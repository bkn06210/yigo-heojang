"""보관해 둔 원본과 추출 텍스트를 카드사 접속 없이 DB에 적재한다.

수집(run.py)은 카드사 사이트를 때리므로 자주 돌릴 것이 아니고, 사이트 개편이나 장애로
실패하기도 한다. 그런데 DB를 다시 만들면 약관 원문이 사라져 챗봇의 약관 질의응답이
조용히 죽는다 — 오류가 아니라 "찾지 못했다"로 답해서 시연 중에야 알게 된다.
원본(raw/)과 추출 결과(out/terms/)가 이미 있으면 네트워크 없이 몇 초 만에 되살릴 수 있다.

<b>수집을 대신하는 도구가 아니다.</b> 새 카드를 받아오는 것은 run.py 의 몫이고,
이쪽은 이미 받아 둔 것을 다시 넣기만 한다. 그래서 파일이 없는 문서는 건너뛴다.

원본 해시를 그대로 계산하므로 나중에 run.py 를 돌려도 같은 문서로 인식되어 중복이 생기지 않는다.
텍스트를 해시하면 값이 달라져 같은 약관이 두 행으로 쌓인다.

    python -m ingest.load_local          보관분 적재
    python -m ingest.build_chunks        조문 단위로 자름
"""

import hashlib
import json
import sys
from pathlib import Path
from typing import Dict, List, Optional

from .companies import CARD_COMPANIES
from .load import connect

_ROOT = Path(__file__).resolve().parent
_RAW_DIR = _ROOT / "raw"
_TERMS_DIR = _ROOT / "out" / "terms"
_CATALOG_PATH = _ROOT / "out" / "catalog.json"

# 추출 텍스트 파일 이름은 "{카드사}_{문서이름}.txt" 다. 카드명에 밑줄이 들어갈 수 있어
# 첫 밑줄에서만 자른다.
_TERMS_NAME_SEPARATOR = "_"

# 같은 카드에 여러 문서가 보관돼 있을 때 고르는 순서.
# 추출 텍스트(out/terms/)는 카드당 하나뿐이고 상품설명서에서 나온 것이므로 그것부터 찾는다.
# 핵심설명서(KEY_TERMS)를 집으면 본문이 어긋난 채로 들어간다.
_DOC_TYPE_PRIORITY = ("PRODUCT_GUIDE", "MEMBER_TERMS", "KEY_TERMS")


def _company_id(conn, issuer: str) -> Optional[int]:
    """카드사 표기 → 카드사 마스터 ID. 마스터가 아직 없으면 NULL."""
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


def _card_id(conn, card_name: str) -> Optional[int]:
    """카드명 → 카드 마스터 ID.

    이어 두면 혜택 값의 근거를 원문으로 되짚을 수 있다. 카드사 단위 회원약관처럼
    특정 카드의 문서가 아닌 것은 이름이 맞지 않아 자연히 NULL 이 된다.
    """
    with conn.cursor() as cursor:
        cursor.execute("SELECT card_id FROM card WHERE card_name = %s", (card_name,))
        row = cursor.fetchone()
        return row[0] if row else None


def _catalog_index() -> Dict[tuple, dict]:
    """(카드사, 문서이름) → 수집 목록 항목. 출처 주소와 개정일을 되살리는 데 쓴다."""
    if not _CATALOG_PATH.exists():
        return {}
    catalog = json.loads(_CATALOG_PATH.read_text(encoding="utf-8"))
    index = {}
    for issuer, entries in catalog.items():
        if not isinstance(entries, list):
            continue
        for entry in entries:
            name = entry.get("card_name")
            if name:
                index[(issuer, name)] = entry
    return index


def _parse_raw_name(raw_path: Path, doc_name: str) -> Optional[str]:
    """원본 파일 이름에서 문서 종류를 꺼낸다.

    이름은 "{문서이름}_{문서종류}_{개정일}.확장자" 인데 <b>문서 종류 자체에 밑줄이 있다</b>
    (PRODUCT_GUIDE, MEMBER_TERMS). 그래서 밑줄로 나눠 가운데를 집으면 "GUIDE" 처럼 잘린다.
    앞의 문서 이름과 뒤의 개정일을 떼어 내는 방식으로 읽는다.
    """
    stem = raw_path.stem
    prefix = doc_name + "_"
    if not stem.startswith(prefix):
        return None
    rest = stem[len(prefix):]
    if "_" not in rest:
        return None
    return rest.rsplit("_", 1)[0]


def _find_raw(issuer: str, doc_name: str) -> Optional[tuple]:
    """보관된 원본 파일과 그 문서 종류. 여러 개면 우선순위가 높은 것을 고른다."""
    directory = _RAW_DIR / issuer
    if not directory.is_dir():
        return None

    candidates = []
    for path in sorted(directory.glob(f"{doc_name}_*")):
        doc_type = _parse_raw_name(path, doc_name)
        if doc_type is not None:
            candidates.append((path, doc_type))
    if not candidates:
        return None

    def rank(item):
        doc_type = item[1]
        if doc_type in _DOC_TYPE_PRIORITY:
            return _DOC_TYPE_PRIORITY.index(doc_type)
        return len(_DOC_TYPE_PRIORITY)

    return min(candidates, key=rank)


def _exists(conn, issuer: str, doc_type: str, content_hash: str) -> bool:
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT 1 FROM card_term_document"
            " WHERE issuer = %s AND doc_type = %s AND content_hash = %s",
            (issuer, doc_type, content_hash),
        )
        return cursor.fetchone() is not None


def _insert(conn, issuer: str, doc_name: str, doc_type: str, text: str,
            raw_path: Path, content_hash: str, entry: dict) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            "INSERT INTO card_term_document"
            " (card_id, card_company_id, source_card_name, issuer, doc_type, source_url,"
            "  storage_path, extract_status, page_count, content_text, content_hash,"
            "  revised_at, fetched_at)"
            " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NULL, %s, %s, %s, NOW())",
            (
                _card_id(conn, doc_name),
                _company_id(conn, issuer),
                doc_name,
                issuer,
                doc_type,
                # 출처 주소는 원본을 다시 받을 수 있는 유일한 실마리라 되도록 채운다.
                entry.get("source_url") or "",
                str(raw_path),
                "TEXT_OK",
                text,
                content_hash,
                entry.get("revised_at"),
            ),
        )
    conn.commit()


def run() -> List[str]:
    """보관분을 적재하고 사람이 읽을 결과 줄을 돌려준다."""
    if not _TERMS_DIR.is_dir():
        return ["추출 텍스트 폴더가 없다: out/terms/"]

    catalog = _catalog_index()
    conn = connect()
    lines, loaded, skipped, missing = [], 0, 0, 0

    try:
        for path in sorted(_TERMS_DIR.glob("*.txt")):
            if _TERMS_NAME_SEPARATOR not in path.stem:
                lines.append(f"  이름 형식 아님(건너뜀): {path.name}")
                continue
            issuer, doc_name = path.stem.split(_TERMS_NAME_SEPARATOR, 1)

            found = _find_raw(issuer, doc_name)
            if found is None:
                # 원본이 없으면 해시를 만들 수 없다. 텍스트로 해시하면 수집을 다시 돌릴 때
                # 같은 약관이 두 행으로 쌓이므로, 넣지 않고 알린다.
                lines.append(f"  원본 없음(건너뜀): {issuer} / {doc_name}")
                missing += 1
                continue

            raw_path, doc_type = found
            entry = catalog.get((issuer, doc_name), {})
            content_hash = hashlib.sha256(raw_path.read_bytes()).hexdigest()

            if _exists(conn, issuer, doc_type, content_hash):
                skipped += 1
                continue

            text = path.read_text(encoding="utf-8", errors="replace")
            _insert(conn, issuer, doc_name, doc_type, text, raw_path, content_hash, entry)
            lines.append(f"  적재 {issuer} / {doc_name} ({doc_type}, {len(text):,}자)")
            loaded += 1
    finally:
        conn.close()

    lines.append("")
    lines.append(f"적재 {loaded}건 · 이미 있음 {skipped}건 · 원본 없음 {missing}건")
    if loaded:
        lines.append("다음: python -m ingest.build_chunks")
    return lines


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    for line in run():
        print(line)


if __name__ == "__main__":
    main()
