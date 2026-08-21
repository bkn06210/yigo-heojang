"""약관 조각에 임베딩을 붙인다.

임베딩은 문장을 숫자 배열로 바꾼 것이다. 뜻이 가까운 문장끼리 값이 비슷해지므로,
글자가 겹치지 않아도 찾을 수 있다. 키워드 검색은 '잃어버렸어요'로 '분실' 조항을
찾지 못하는데(실측 0건), 이 값이 그 자리를 메운다.

조각을 만드는 단계와 나눈 이유는 비용과 조건이 다르기 때문이다. 조각내기는 공짜라
규칙을 고칠 때마다 돌리지만, 이쪽은 외부 호출이라 API 키가 있어야 하고 요금이 붙는다.
그래서 아직 값이 없는 조각만 골라 부른다 — 중간에 끊겨도 다시 돌리면 이어서 채운다.

임베딩이 없어도 검색은 동작한다. 키워드 검색만으로 돌고 정확도만 떨어진다.
"""

import os
import struct
import sys
from typing import List, Sequence, Tuple

import requests
from dotenv import load_dotenv

from .load import connect

_ENV_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), ".env")
_API_URL = "https://api.openai.com/v1/embeddings"

# 문장을 숫자로 바꾸는 모델. 글을 쓰는 모델과 달리 값만 내주므로 훨씬 싸다.
# 100만 토큰에 $0.02 수준이라 약관 전체를 넣어도 십원대다.
_MODEL = "text-embedding-3-small"

# 한 번에 보낼 조각 수. 하나씩 부르면 왕복 시간이 조각 수만큼 곱해지고,
# 너무 많이 담으면 요청 크기 상한에 걸린다.
_BATCH_SIZE = 64

_TIMEOUT_SEC = 60


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    api_key = _api_key()
    conn = connect()

    try:
        pending = _pending_chunks(conn)
        if not pending:
            print("임베딩이 필요한 조각이 없다")
            return

        print(f"대상 조각 {len(pending)}개 · 모델 {_MODEL}")
        done = 0

        for batch in _batched(pending, _BATCH_SIZE):
            vectors = _embed([content for _, content in batch], api_key)
            _store(conn, [(chunk_id, vector) for (chunk_id, _), vector in zip(batch, vectors)])
            conn.commit()
            done += len(batch)
            print(f"  {done}/{len(pending)}")

        print(f"완료. 조각 {done}개에 임베딩을 붙였다")
    finally:
        conn.close()


def _api_key() -> str:
    load_dotenv(_ENV_PATH)
    key = os.getenv("OPENAI_API_KEY", "")
    if not key:
        raise SystemExit("OPENAI_API_KEY 가 ai-server/.env 에 없다")
    return key


def _pending_chunks(conn) -> List[Tuple[int, str]]:
    """아직 임베딩이 없거나 다른 모델로 만든 조각.

    모델이 섞이면 유사도 값이 서로 비교할 수 없는 것이 된다. 같은 문장이라도
    모델마다 좌표계가 달라, 한 표 안에 섞이면 어느 쪽이 가까운지가 무의미해진다.
    """
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_term_chunk_id, content FROM card_term_chunk"
            " WHERE embedding IS NULL OR embedding_model <> %s"
            " ORDER BY card_term_chunk_id",
            (_MODEL,),
        )
        return list(cursor.fetchall())


def _batched(items: Sequence, size: int):
    for start in range(0, len(items), size):
        yield items[start : start + size]


def _embed(texts: List[str], api_key: str) -> List[List[float]]:
    response = requests.post(
        _API_URL,
        headers={"Authorization": f"Bearer {api_key}"},
        json={"model": _MODEL, "input": texts},
        timeout=_TIMEOUT_SEC,
    )
    response.raise_for_status()

    # 응답 순서가 보낸 순서와 같다고 가정하지 않는다. 어긋나면 다른 조각의 좌표가
    # 붙어 검색이 엉뚱한 조문을 집어오는데, 값이 그럴듯해서 알아채기 어렵다.
    items = sorted(response.json()["data"], key=lambda item: item["index"])
    return [item["embedding"] for item in items]


def _store(conn, rows: List[Tuple[int, List[float]]]) -> None:
    with conn.cursor() as cursor:
        cursor.executemany(
            "UPDATE card_term_chunk SET embedding = %s, embedding_model = %s"
            " WHERE card_term_chunk_id = %s",
            [(pack_vector(vector), _MODEL, chunk_id) for chunk_id, vector in rows],
        )


def pack_vector(vector: Sequence[float]) -> bytes:
    """숫자 배열을 float32 이진으로 담는다.

    JSON 문자열로 두면 '0.0123456' 처럼 숫자 하나가 열 바이트 남짓을 먹어
    같은 값이 두 배 넘는 자리를 차지한다. 카드가 늘어 조각이 수만 개가 되면
    그 차이가 백 메가 단위로 벌어진다.
    """
    return struct.pack(f"<{len(vector)}f", *vector)


def unpack_vector(blob: bytes) -> List[float]:
    """저장한 이진을 숫자 배열로 되돌린다."""
    return list(struct.unpack(f"<{len(blob) // 4}f", blob))


if __name__ == "__main__":
    main()
