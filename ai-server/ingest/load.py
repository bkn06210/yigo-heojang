"""수집한 약관을 내려받아 텍스트로 만들고 DB에 적재한다.

원문은 SQL 파일이 아니라 DB에 바로 넣는다. 카드 한 장이 1만~2만 자라
파일로 만들면 수 MB가 되는데, 수집이 자동이라 누구든 다시 만들 수 있는 데이터다.
반대로 구조화 결과(benefit)는 작고 공유해야 하므로 시드 SQL로 뽑는다.

같은 문서를 다시 받지 않도록 두 단계로 거른다.
  1) 시행일이 있으면 내려받기 전에 판정한다 — 요청 자체가 없다
  2) 시행일이 없는 카드사는 받아서 해시로 판정한다 — 적재만 막는다
"""

import os
import re
from datetime import datetime
from pathlib import Path
from typing import Optional

import pymysql
from dotenv import load_dotenv

from .collect.base import Collector, DocumentRef
from .extract import ExtractResult, extract
from .companies import CARD_COMPANIES

# 원본 PDF 보관 위치. 카드사 주소는 사이트 개편으로 죽으므로 받은 파일을 남겨둔다.
_STORAGE_ROOT = Path(__file__).resolve().parent / "raw"

# 파일 이름에 쓸 수 없는 문자. 카드명에 슬래시나 콜론이 들어가는 경우가 있다.
_UNSAFE_FILENAME = re.compile(r'[\\/:*?"<>|]')

LOADED = "LOADED"
SKIPPED = "SKIPPED"


def connect() -> pymysql.connections.Connection:
    load_dotenv(Path(__file__).resolve().parent.parent / ".env")
    return pymysql.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", ""),
        database=os.getenv("DB_NAME", "wallet"),
        charset="utf8mb4",
    )


def load_document(collector: Collector, ref: DocumentRef, conn) -> str:
    if ref.revised_at and _exists_by_revision(conn, ref):
        return SKIPPED

    pdf_bytes = collector.fetch(ref)
    result = extract(pdf_bytes)

    if _exists_by_hash(conn, ref, result.content_hash):
        return SKIPPED

    storage_path = _store(ref, pdf_bytes)
    _insert(conn, ref, result, storage_path)
    return LOADED


def _exists_by_revision(conn, ref: DocumentRef) -> bool:
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT 1 FROM card_term_document"
            " WHERE issuer = %s AND source_card_name = %s"
            "   AND doc_type = %s AND revised_at = %s"
            " LIMIT 1",
            (ref.issuer, ref.card_name, ref.doc_type, ref.revised_at),
        )
        return cursor.fetchone() is not None


def _exists_by_hash(conn, ref: DocumentRef, content_hash: str) -> bool:
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT 1 FROM card_term_document"
            " WHERE issuer = %s AND doc_type = %s AND content_hash = %s"
            " LIMIT 1",
            (ref.issuer, ref.doc_type, content_hash),
        )
        return cursor.fetchone() is not None


def _store(ref: DocumentRef, content: bytes) -> str:
    directory = _STORAGE_ROOT / ref.issuer
    directory.mkdir(parents=True, exist_ok=True)

    safe_name = _UNSAFE_FILENAME.sub("_", ref.card_name).strip()
    suffix = ref.revised_at or "unknown"
    path = directory / f"{safe_name}_{ref.doc_type}_{suffix}{_extension(content)}"
    path.write_bytes(content)

    # 프로젝트 기준 상대 경로로 남긴다. 구분자를 슬래시로 맞추는 이유는
    # 윈도우에서 만든 경로가 다른 환경에서 그대로 읽히게 하기 위해서다.
    return path.relative_to(_STORAGE_ROOT.parent.parent).as_posix()


def _extension(content: bytes) -> str:
    """보관 파일의 확장자.

    카드사가 준 주소가 아니라 받은 내용으로 정한다. 개인회원 약관은 카드사에 따라
    웹페이지로 오는데, 주소만 보고 .pdf 로 저장하면 열리지 않는 파일이 쌓인다.
    """
    return ".pdf" if content.startswith(b"%PDF-") else ".html"


def _company_id(conn, issuer: str) -> Optional[int]:
    """수집기의 카드사 표기를 카드사 마스터 ID로 바꾼다.

    표기 대응은 시드 생성기가 이미 갖고 있는 것을 그대로 쓴다. 여기에 또 적으면
    카드사를 추가할 때 한쪽만 고쳐져 어긋난다.

    마스터가 아직 없으면(카드 시드를 넣기 전) NULL 로 둔다. 약관 수집이 카드 시드보다
    먼저 도는 것을 막을 이유가 없고, 나중에 채워 넣을 수 있다.
    """
    company = CARD_COMPANIES.get(issuer)
    if company is None:
        return None

    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_company_id FROM card_company WHERE company_code = %s",
            (company[1],),
        )
        row = cursor.fetchone()
        return row[0] if row else None


def _insert(conn, ref: DocumentRef, result: ExtractResult, storage_path: str) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            "INSERT INTO card_term_document"
            " (card_id, card_company_id, source_card_name, issuer, doc_type, source_url,"
            "  storage_path, extract_status, page_count, content_text, content_hash,"
            "  revised_at, fetched_at)"
            " VALUES (NULL, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
            (
                _company_id(conn, ref.issuer),
                ref.card_name,
                ref.issuer,
                ref.doc_type,
                ref.source_url,
                storage_path,
                result.status,
                result.page_count,
                result.text,
                result.content_hash,
                ref.revised_at,
                datetime.now(),
            ),
        )
    conn.commit()
