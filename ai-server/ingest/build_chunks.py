"""적재된 약관 원문을 조각으로 잘라 card_term_chunk 에 넣는다.

수집(run.py)과 분리한 이유는 다시 도는 조건이 다르기 때문이다. 수집은 카드사 사이트를
때리므로 자주 돌릴 것이 아니고, 조각내기는 자르는 규칙을 고칠 때마다 다시 돌게 된다.
원문이 DB에 있으니 이 단계는 네트워크 없이 몇 초면 끝난다.

몇 번을 돌려도 결과가 같다. 현행본의 조각을 지우고 다시 넣고, 현행본이 아닌 문서의
조각은 남김없이 지운다. 지난 시행본의 조각이 남아 있으면 "연회비 반환 기준"에
옛 조항과 현행 조항이 나란히 검색돼 어느 쪽이 맞는지 가릴 수 없다.
"""

import sys
from typing import List, Tuple

from .chunk import Chunk, split_document
from .load import connect

# heading 칸 길이. 넘치면 잘라 넣는다 — 장 제목과 조 제목을 붙이면 길어지는 문서가 있다.
_HEADING_LIMIT = 200

# 카드사·문서종류·문서이름이 같은 것들 중 현행본 하나만 고른다.
# 개정일이 없는 카드사가 있어(본문 안에만 있는 경우) 수집 시각과 행 번호로 이어서 가른다.
_CURRENT_DOCUMENTS = """
SELECT card_term_document_id, issuer, source_card_name, doc_type, content_text
FROM (
    SELECT card_term_document_id, issuer, source_card_name, doc_type, content_text,
           ROW_NUMBER() OVER (
               PARTITION BY issuer, doc_type, source_card_name
               ORDER BY COALESCE(revised_at, '') DESC,
                        fetched_at DESC,
                        card_term_document_id DESC
           ) AS recency
    FROM card_term_document
    WHERE content_text IS NOT NULL
) ranked
WHERE recency = 1
"""

# 현행본이 아닌 문서에 달린 조각. 개정 전에 만들어져 남아 있는 것들이다.
_STALE_CHUNKS = f"""
DELETE FROM card_term_chunk
WHERE card_term_document_id NOT IN (
    SELECT card_term_document_id FROM ({_CURRENT_DOCUMENTS}) current_documents
)
"""


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    conn = connect()

    try:
        stale = _drop_stale_chunks(conn)
        if stale:
            print(f"지난 시행본 조각 {stale}개 삭제")

        total = 0
        for document in _current_documents(conn):
            document_id, issuer, name, _doc_type, text = document
            chunks = split_document(text)
            _replace_chunks(conn, document_id, chunks)
            total += len(chunks)
            print(f"[{issuer}] {name} → 조각 {len(chunks)}개")

        conn.commit()
        print(f"\n합계 조각 {total}개")
    finally:
        conn.close()


def _current_documents(conn) -> List[Tuple]:
    with conn.cursor() as cursor:
        cursor.execute(_CURRENT_DOCUMENTS)
        return cursor.fetchall()


def _drop_stale_chunks(conn) -> int:
    with conn.cursor() as cursor:
        return cursor.execute(_STALE_CHUNKS)


def _replace_chunks(conn, document_id: int, chunks: List[Chunk]) -> None:
    """이 문서의 조각을 통째로 갈아 끼운다.

    지우고 넣는 이유는 자르는 규칙이 바뀌면 조각 수가 달라지기 때문이다.
    덮어쓰기만 하면 규칙이 조각을 줄였을 때 예전 뒷부분이 남는다.
    임베딩도 함께 사라지는데, 본문이 달라지면 옛 임베딩은 어차피 맞지 않는다.
    """
    with conn.cursor() as cursor:
        cursor.execute(
            "DELETE FROM card_term_chunk WHERE card_term_document_id = %s",
            (document_id,),
        )
        cursor.executemany(
            "INSERT INTO card_term_chunk"
            " (card_term_document_id, chunk_index, heading, content)"
            " VALUES (%s, %s, %s, %s)",
            [
                (
                    document_id,
                    chunk.index,
                    (chunk.heading or "")[:_HEADING_LIMIT] or None,
                    chunk.content,
                )
                for chunk in chunks
            ],
        )


if __name__ == "__main__":
    main()
