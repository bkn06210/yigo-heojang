"""MySQL 에 있는 조각 벡터를 Qdrant 로 옮긴다.

    python -m ingest.sync_qdrant

벡터의 원본은 MySQL(card_term_chunk.embedding)이고 Qdrant 는 그 복사본이다.
그래서 여기서 임베딩을 새로 만들지 않는다 — 이미 있는 숫자를 옮길 뿐이라 API 키가 필요 없고
네트워크도 Qdrant 하나만 탄다. build_embeddings 로 벡터를 만든 뒤에 돌린다.

몇 번을 돌려도 결과가 같다. 같은 id 는 덮어쓰고, MySQL 에서 사라진 조각은 Qdrant 에서도 지운다.
조각을 다시 자르면 id 가 새로 매겨지므로, 지우지 않으면 옛 id 가 남아 검색 상위를 차지한다 —
본문을 못 찾아 결과에서는 빠지지만 그만큼 자리를 낭비한다.

컬렉션은 임베딩 모델 태그별로 따로다. 모델을 바꾸면 새 컬렉션이 생기고, 챗봇도 같은 태그를
보므로 옛 컬렉션과 섞이지 않는다.
"""

import sys
from typing import Iterator, List, Tuple

from embedding import create_embedding_client, unpack_vector
from vectorstore import get_vectorstore_settings, qdrant

from .load import connect

_LOAD_SQL = """
SELECT c.card_term_chunk_id, d.card_company_id, c.embedding
FROM card_term_chunk c
JOIN card_term_document d ON d.card_term_document_id = c.card_term_document_id
WHERE c.embedding IS NOT NULL
  AND c.embedding_model = %s
ORDER BY c.card_term_chunk_id
"""


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")

    embedding = create_embedding_client()
    settings = get_vectorstore_settings()

    rows = _load_rows(embedding.model_tag)
    if not rows:
        raise SystemExit(
            f"옮길 벡터가 없다 (모델 {embedding.model_tag}). "
            "python -m ingest.build_embeddings 로 먼저 색인할 것"
        )

    client = qdrant.connect(settings.qdrant_url)
    collection = qdrant.collection_name(embedding.model_tag)
    if qdrant.ensure_collection(client, collection, embedding.dimension):
        print(f"컬렉션 생성: {collection} ({embedding.dimension}차원, cosine)")
    else:
        print(f"컬렉션: {collection}")

    written = qdrant.upsert(client, collection, _points(rows))
    print(f"  {written}개 반영 ← {settings.qdrant_url}")

    current = {chunk_id for chunk_id, _, _ in rows}
    stale = sorted(qdrant.point_ids(client, collection) - current)
    if stale:
        qdrant.delete_points(client, collection, stale)
        print(f"  {len(stale)}개 삭제 (MySQL 에 더는 없는 조각)")

    print()
    print("챗봇이 이 컬렉션을 쓰게 하려면 .env 에 TERM_INDEX=qdrant 를 두고 재기동한다.")


def _load_rows(model_tag: str) -> List[Tuple[int, int, bytes]]:
    conn = connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute(_LOAD_SQL, (model_tag,))
            return list(cursor.fetchall())
    finally:
        conn.close()


def _points(rows) -> Iterator[Tuple[int, List[float], dict]]:
    for chunk_id, company_id, blob in rows:
        # payload 는 카드사 id 뿐이다. 본문은 MySQL 이 진실 원천이라 여기에 복사하지 않는다.
        yield int(chunk_id), unpack_vector(blob), {"company_id": company_id}


if __name__ == "__main__":
    main()
