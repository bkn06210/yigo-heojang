"""비전으로 읽은 약관 본문을 DB에 넣는다.

스캔 이미지로만 된 PDF는 기계 추출이 0자로 끝나 `extract_status='IMAGE_ONLY'`로 남는다.
`vision_prep`이 그런 문서의 페이지를 그림으로 만들어 두면, 비전 모델이 그림을 읽어
`out/vision/<문서ID>.txt`에 본문을 적는다. 이 스크립트는 그 파일을 원문 칸에 넣는다.

<b>읽은 결과를 저장하지 않으면 같은 문서를 계속 다시 읽게 된다.</b> 그림을 읽는 일은
자동으로 돌릴 수 없어 사람이 붙어야 하는데, 결과가 DB에 남지 않으면 DB를 다시 만들 때마다
그 카드만 조용히 비고 목록에는 정상으로 보인다. 원문 칸에 넣어 두면 덤프(dump_terms)에
함께 담겨 SQL 한 번으로 복구되고, 약관 질의응답도 그 카드를 답할 수 있게 된다.

상태를 `TEXT_OK`가 아니라 `VISION_OK`로 남기는 이유는 근거의 성질이 다르기 때문이다.
기계 추출은 원문 글자를 그대로 옮긴 것이지만 비전은 읽어 옮긴 것이라 원문 대조가 안 된다.
같은 값으로 적으면 나중에 어느 쪽을 다시 확인해야 하는지 가릴 수 없다.

    python -m ingest.vision_prep          이미지 PDF의 페이지를 그림으로
    (비전 모델이 그림을 읽어 out/vision/<문서ID>.txt 로 저장)
    python -m ingest.load_vision          그 본문을 DB에 반영
"""

import sys
from pathlib import Path
from typing import List, Tuple

from .load import connect

_VISION_DIR = Path(__file__).resolve().parent / "out" / "vision"

VISION_OK = "VISION_OK"


def _pending(conn) -> List[Tuple[int, str, str]]:
    """비전으로 읽어야 할 문서. 이미 반영한 것은 다시 넣지 않는다."""
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_term_document_id, issuer, source_card_name"
            "  FROM card_term_document"
            " WHERE extract_status = 'IMAGE_ONLY'"
            " ORDER BY card_term_document_id"
        )
        return list(cursor.fetchall())


def _apply(conn, document_id: int, text: str) -> None:
    with conn.cursor() as cursor:
        cursor.execute(
            "UPDATE card_term_document"
            "   SET content_text = %s, extract_status = %s"
            " WHERE card_term_document_id = %s",
            (text, VISION_OK, document_id),
        )
    conn.commit()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    conn = connect()

    try:
        rows = _pending(conn)
        if not rows:
            print("비전으로 읽어야 할 문서가 없다.")
            return

        applied = 0
        for document_id, issuer, card_name in rows:
            path = _VISION_DIR / f"{document_id}.txt"
            if not path.exists():
                print(f"[{issuer}] {card_name} — 읽은 본문 없음 ({path.name})")
                continue

            text = path.read_text(encoding="utf-8").strip()
            if not text:
                print(f"[{issuer}] {card_name} — 본문이 비어 있어 건너뜀")
                continue

            _apply(conn, document_id, text)
            applied += 1
            print(f"[{issuer}] {card_name} — {len(text):,}자 반영")

        print(f"\n반영 {applied}건 · 남은 이미지 문서 {len(rows) - applied}건")
        if applied < len(rows):
            sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
