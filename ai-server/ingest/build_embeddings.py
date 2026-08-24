"""약관 조각에 임베딩을 붙인다.

조각을 만드는 단계(build_chunks)와 나눈 이유는 비용과 조건이 다르기 때문이다.
조각내기는 공짜라 자르는 규칙을 고칠 때마다 돌리지만, 이쪽은 외부 호출이라 키가 필요하다.
그래서 아직 값이 없는 조각만 골라 부른다 — 중간에 끊겨도 다시 돌리면 이어서 채운다.

임베딩이 없어도 검색은 동작한다. 낱말 검색만으로 돌고 정확도만 떨어진다.

어느 공급자로 만들지는 여기서 정하지 않는다. EMBEDDING_PROVIDER 를 stub 으로 두면
키 없이도 전 구간이 돌아, 색인 도구 자체는 키를 받기 전에 확인할 수 있다.
"""

import sys
from typing import List, Sequence, Tuple

from embedding import create_embedding_client, pack_vector

from .load import connect

# 몇 개마다 저장할지. 요청 하나에 담는 양(어댑터가 정한다)과는 다른 기준이다 —
# 이 값은 중간에 끊겼을 때 얼마나 되돌아가는지를 정한다.
_COMMIT_BATCH = 128


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    client = create_embedding_client()
    conn = connect()

    try:
        pending = _pending_chunks(conn, client.model_tag)
        if not pending:
            print(f"임베딩이 필요한 조각이 없다 (모델 {client.model_tag})")
            return

        print(f"대상 조각 {len(pending)}개 · {client.provider} / {client.model_tag}")
        if client.provider == "stub":
            print("  ※ 개발용 임베딩이다. 글자만 보므로 뜻으로는 찾지 못한다")

        done = 0
        for batch in _batched(pending, _COMMIT_BATCH):
            vectors = client.embed_documents([content for _, content in batch])
            _store(conn, client.model_tag,
                   [(chunk_id, vector) for (chunk_id, _), vector in zip(batch, vectors)])
            conn.commit()
            done += len(batch)
            print(f"  {done}/{len(pending)}")

        print(f"완료. 조각 {done}개에 임베딩을 붙였다")
    finally:
        conn.close()


def _pending_chunks(conn, model_tag: str) -> List[Tuple[int, str]]:
    """아직 임베딩이 없거나 다른 모델로 만든 조각.

    모델이 섞이면 유사도 값이 서로 비교할 수 없는 것이 된다. 같은 문장이라도
    모델마다 좌표계가 달라, 한 표 안에 섞이면 어느 쪽이 가까운지가 무의미해진다.
    """
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_term_chunk_id, content FROM card_term_chunk"
            " WHERE embedding IS NULL OR embedding_model <> %s"
            " ORDER BY card_term_chunk_id",
            (model_tag,),
        )
        return list(cursor.fetchall())


def _batched(items: Sequence, size: int):
    for start in range(0, len(items), size):
        yield items[start : start + size]


def _store(conn, model_tag: str, rows: List[Tuple[int, List[float]]]) -> None:
    with conn.cursor() as cursor:
        cursor.executemany(
            "UPDATE card_term_chunk SET embedding = %s, embedding_model = %s"
            " WHERE card_term_chunk_id = %s",
            [(pack_vector(vector), model_tag, chunk_id) for chunk_id, vector in rows],
        )


if __name__ == "__main__":
    main()
