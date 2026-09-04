"""Qdrant 컬렉션을 다루는 얇은 층.

Qdrant 는 벡터를 저장하고 가까운 것을 빨리 찾는 전용 서버다. 전부 재지 않고
HNSW 그래프를 타며 이웃으로 점프하므로 조각이 수백만 건이어도 밀리초다.
대신 근사라 정답을 100% 보장하지 않는다 — 조각 1,550개에서 numpy 전수 계산이
더 정확하고 더 빠른 이유다.

**여기에는 벡터와 id 만 둔다.** 본문·제목·카드사는 MySQL 에서 다시 읽는다.
두 곳에 두면 약관을 고쳤을 때 한쪽만 바뀌어 조용히 어긋난다. payload 에는
카드사 id 만 넣는다 — 나중에 카드사로 좁혀 찾을 때 필터에 쓸 값이다.

컬렉션 이름에 임베딩 모델 태그를 붙인다. 모델이나 차원을 바꾸면 다른 컬렉션이 되어
좌표계가 다른 값이 한 곳에 섞이지 않는다.
"""

from typing import Iterable, List, Sequence, Set, Tuple

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PointIdsList, PointStruct, VectorParams

_COLLECTION_PREFIX = "card_term__"

# 챗봇이 사람을 기다리게 하는 자리라 길게 잡지 않는다. 동기화는 배치라 같은 값이어도 괜찮다.
_TIMEOUT_SECONDS = 10

# 한 요청에 담을 점 수. 하나씩 보내면 왕복이 조각 수만큼 곱해진다.
_UPSERT_BATCH = 256
_SCROLL_BATCH = 1000


def collection_name(model_tag: str) -> str:
    return _COLLECTION_PREFIX + model_tag


def connect(url: str) -> QdrantClient:
    return QdrantClient(url=url, timeout=_TIMEOUT_SECONDS)


def has_collection(client: QdrantClient, name: str) -> bool:
    return client.collection_exists(collection_name=name)


def ensure_collection(client: QdrantClient, name: str, dimension: int) -> bool:
    """없으면 만든다. 만들었으면 True."""
    if has_collection(client, name):
        return False
    client.create_collection(
        collection_name=name,
        # 코사인으로 두면 Qdrant 가 길이를 맞춰 저장해, numpy 쪽과 같은 척도의 점수가 나온다.
        vectors_config=VectorParams(size=dimension, distance=Distance.COSINE),
    )
    return True


def upsert(
    client: QdrantClient,
    name: str,
    points: Iterable[Tuple[int, Sequence[float], dict]],
) -> int:
    """(id, 벡터, payload) 를 넣는다. 같은 id 면 덮어쓰므로 몇 번 돌려도 결과가 같다."""
    batch: List[PointStruct] = []
    written = 0
    for point_id, vector, payload in points:
        batch.append(PointStruct(id=point_id, vector=list(vector), payload=payload))
        if len(batch) >= _UPSERT_BATCH:
            client.upsert(collection_name=name, points=batch)
            written += len(batch)
            batch = []
    if batch:
        client.upsert(collection_name=name, points=batch)
        written += len(batch)
    return written


def point_ids(client: QdrantClient, name: str) -> Set[int]:
    """컬렉션에 든 id 전부. 지워야 할 것을 가려내는 데 쓴다."""
    ids: Set[int] = set()
    offset = None
    while True:
        points, offset = client.scroll(
            collection_name=name,
            limit=_SCROLL_BATCH,
            offset=offset,
            with_payload=False,
            with_vectors=False,
        )
        ids.update(int(point.id) for point in points)
        if offset is None:
            break
    return ids


def delete_points(client: QdrantClient, name: str, ids: Sequence[int]) -> None:
    if not ids:
        return
    client.delete(collection_name=name, points_selector=PointIdsList(points=list(ids)))


def search(
    client: QdrantClient, name: str, vector: Sequence[float], limit: int
) -> List[Tuple[int, float]]:
    """가까운 순으로 (id, 코사인 유사도)."""
    response = client.query_points(
        collection_name=name,
        query=list(vector),
        limit=limit,
        with_payload=False,
        with_vectors=False,
    )
    return [(int(point.id), float(point.score)) for point in response.points]
