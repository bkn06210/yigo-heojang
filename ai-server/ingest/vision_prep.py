"""텍스트가 안 나온 약관을 비전으로 읽을 수 있게 준비한다.

카드사 상품설명서 중에는 스캔 이미지로만 된 것이 있다. 텍스트 추출이 0자로 끝나
`extract_status='IMAGE_ONLY'`로 적재된 문서들이다. 이 상태로 두면 그 카드만
혜택 데이터가 비는데, 목록에는 정상으로 보여서 알아채기 어렵다.

여기서는 그런 문서를 찾아 페이지를 이미지로 만들어 둔다.
실제로 읽는 일(비전 모델 호출)은 다음 단계다.

같은 카드의 다른 문서로 내용이 커버되는 경우가 있어(KB는 주요거래조건에
혜택이 실리기도 한다) 대체 문서 유무도 함께 보여준다.
"""

import sys
from pathlib import Path
from typing import List, Tuple

from .extract import ExtractStatus, render_pages
from .load import connect

_OUTPUT_DIR = Path(__file__).resolve().parent / "out" / "vision"
_PROJECT_ROOT = Path(__file__).resolve().parent.parent


def find_image_only() -> List[Tuple[int, str, str, str, str, int]]:
    """이미지 PDF로 판정된 문서와, 같은 카드에 쓸 만한 대체 문서가 있는지."""
    conn = connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT d.card_term_document_id, d.issuer, d.source_card_name,"
                "       d.doc_type, d.storage_path,"
                "       (SELECT COUNT(*) FROM card_term_document o"
                "         WHERE o.issuer = d.issuer"
                "           AND o.source_card_name = d.source_card_name"
                "           AND o.extract_status = %s) AS usable_siblings"
                "  FROM card_term_document d"
                " WHERE d.extract_status = %s"
                " ORDER BY d.issuer, d.source_card_name",
                (ExtractStatus.TEXT_OK, ExtractStatus.IMAGE_ONLY),
            )
            return list(cursor.fetchall())
    finally:
        conn.close()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    rows = find_image_only()

    if not rows:
        print("이미지 PDF로 판정된 문서가 없다.")
        return

    for doc_id, issuer, card_name, doc_type, storage_path, siblings in rows:
        pdf_path = _PROJECT_ROOT / storage_path
        if not pdf_path.exists():
            print(f"[{issuer}] {card_name}: 원본 파일 없음 — {storage_path}")
            continue

        stem = f"{doc_id}_{doc_type}"
        pages = render_pages(pdf_path.read_bytes(), _OUTPUT_DIR, stem)
        # 대체 문서가 있으면 급하지 않고, 없으면 이 카드는 이걸 읽어야만 데이터가 생긴다.
        urgency = "대체 문서 있음" if siblings else "대체 문서 없음 — 필수"
        print(f"[{issuer}] {card_name} ({doc_type}) {len(pages)}쪽 → {urgency}")
        for page in pages:
            print(f"    {page.relative_to(_PROJECT_ROOT).as_posix()}")


if __name__ == "__main__":
    main()
