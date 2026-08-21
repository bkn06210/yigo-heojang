"""적재된 약관 원문을 카드별 텍스트 파일로 꺼낸다 — 구조화 입력을 만드는 단계.

수집(run)이 PDF에서 뽑은 글자는 이미 DB(card_term_document.content_text)에 들어 있다.
여기서는 그것을 카드 한 장당 파일 하나로 모아 out/terms 에 쓴다. 이어서 prune 이
공통 문구를 걷어내면 구조화에 넣을 입력이 된다.

<b>PDF를 다시 파싱하지 않는 이유가 있다.</b> 스캔 이미지로만 된 PDF는 기계 추출이 0자로
끝나 비전으로 읽어 넣는데, 그 결과는 원본 파일이 아니라 DB에만 있다. PDF에서 다시 뽑으면
그 카드들만 빈 파일이 되고, 오류 없이 혜택이 통째로 사라진다. DB를 원천으로 삼으면
어떻게 읽었든 같은 경로로 나온다.

카드 하나에 문서가 여럿이라(상품설명서·주요거래조건) 한 파일에 이어 붙이고, 어디서 온
글인지 알 수 있게 문서마다 머리글을 남긴다. 구조화가 값을 잘못 읽었을 때 어느 문서를
다시 봐야 하는지 찾을 수 있어야 한다.

카드사 단위 개인회원 약관은 뽑지 않는다. 카드에 딸린 문서가 아니라 혜택 규칙이 없고,
카드마다 붙이면 같은 글이 카드 수만큼 구조화에 실려 나간다.

    python -m ingest.dump_text            out/terms/<카드사>_<카드명>.txt
    python -m ingest.prune                공통 문구를 걷어내 out/pruned 로
"""

import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple

from .load import connect

_OUTPUT_DIR = Path(__file__).resolve().parent / "out" / "terms"

# 카드에 딸린 문서만 뽑는다. 순서를 고정하는 것은 같은 DB에서 두 번 뽑았을 때
# 파일이 달라지지 않게 하기 위해서다 — 달라지면 구조화 결과 비교가 안 된다.
_CARD_DOC_TYPES = ("PRODUCT_GUIDE", "KEY_TERMS")

# 파일 이름에 쓸 수 없는 글자. 카드명에 슬래시나 콜론이 들어가는 카드사가 있다.
_UNSAFE_IN_FILENAME = re.compile(r'[\\/:*?"<>|]')


def _rows(conn) -> List[Tuple[int, str, str, str, str]]:
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_term_document_id, issuer, source_card_name, doc_type, content_text"
            "  FROM card_term_document"
            " WHERE doc_type IN %s AND content_text IS NOT NULL"
            " ORDER BY issuer, source_card_name, doc_type, card_term_document_id",
            (_CARD_DOC_TYPES,),
        )
        return list(cursor.fetchall())


def _group(rows) -> Dict[Tuple[str, str], List[Tuple[int, str, str]]]:
    grouped: Dict[Tuple[str, str], List[Tuple[int, str, str]]] = {}
    for document_id, issuer, card_name, doc_type, text in rows:
        grouped.setdefault((issuer, card_name), []).append((document_id, doc_type, text))
    return grouped


def _file_name(issuer: str, card_name: str) -> str:
    return _UNSAFE_IN_FILENAME.sub("_", f"{issuer}_{card_name}") + ".txt"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    conn = connect()

    try:
        grouped = _group(_rows(conn))
    finally:
        conn.close()

    if not grouped:
        print("뽑을 원문이 없다. 먼저 ingest.run 으로 약관을 받아라.")
        sys.exit(1)

    _OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    for (issuer, card_name), documents in sorted(grouped.items()):
        blocks = [
            f"### 문서 {document_id} ({doc_type})\n{text.strip()}"
            for document_id, doc_type, text in documents
        ]
        body = "\n\n".join(blocks) + "\n"

        path = _OUTPUT_DIR / _file_name(issuer, card_name)
        path.write_text(body, encoding="utf-8")
        print(f"[{issuer}] {card_name} — 문서 {len(documents)}건 · {len(body):,}자")

    print(f"\n카드 {len(grouped)}장 → {_OUTPUT_DIR}")


if __name__ == "__main__":
    main()
