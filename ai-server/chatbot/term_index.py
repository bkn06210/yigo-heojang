"""약관 조각 색인 — 질문 벡터에 가까운 조각을 찾는다.

찾는 방식이 둘이고 설정(TERM_INDEX)으로 고른다.

    numpy   벡터를 한 번 읽어 메모리에 두고 행렬곱으로 전수 계산한다. 조각 1,550개에 0.4ms.
    qdrant  별도 서버가 근사 최근접 이웃(HNSW)으로 찾는다. 조각이 수십만 건이면 이쪽이 낫다.

둘 다 두는 이유는 규모에 따라 답이 다르기 때문이다. 지금 규모에서는 전수 계산이
더 정확하고 더 빠르다 — 계산이 조각 수에 정비례하므로 국내 카드를 전부 넣어도 40ms 다.
벡터 서버는 그 다음 단계이고, 검색하는 자리를 이 파일 하나로 모아 두어 그때 여기만 바꾼다.

**어느 쪽이든 원문은 MySQL 에만 있다.** Qdrant 에는 벡터와 id 만 두고 본문·제목·카드사는
MySQL 에서 다시 읽는다. 두 곳에 두면 약관을 고쳤을 때 한쪽만 바뀌어 조용히 어긋난다.

처음 검색할 때 읽는다(기동 시점이 아니라). 컨테이너로 띄우면 챗봇이 MySQL 보다 먼저 뜨는
일이 있어, 기동 중에 읽으면 DB 가 아직 안 떠 있다는 이유로 서버가 통째로 죽는다.
numpy 쪽은 **색인을 다시 만들면 챗봇을 재기동해야 한다** — 메모리에 담아둔 것이 옛것이 된다.
"""

import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Dict, List, Optional, Sequence

import numpy as np

from embedding import create_embedding_client, unpack_vector
from vectorstore import INDEX_NUMPY, INDEX_QDRANT, get_vectorstore_settings

logger = logging.getLogger(__name__)

# 같은 모델로 만든 조각만 읽는다. 좌표계가 다른 값이 한 표에 섞이면 어느 쪽이 가까운지가
# 무의미해지는데, 값이 그럴듯해서 결과만 봐서는 알아채기 어렵다.
_LOAD_SQL = """
SELECT d.card_company_id,
       COALESCE(cc.company_name, d.issuer) AS company_name,
       d.source_card_name,
       c.heading,
       c.content,
       c.embedding
FROM card_term_chunk c
JOIN card_term_document d ON d.card_term_document_id = c.card_term_document_id
LEFT JOIN card_company cc ON cc.card_company_id = d.card_company_id
WHERE c.embedding IS NOT NULL
  AND c.embedding_model = %s
ORDER BY c.card_term_chunk_id
"""

# Qdrant 가 돌려준 id 로 본문을 다시 읽는다. 30건 IN 조회라 몇 ms 다.
_FETCH_SQL = """
SELECT c.card_term_chunk_id,
       d.card_company_id,
       COALESCE(cc.company_name, d.issuer) AS company_name,
       d.source_card_name,
       c.heading,
       c.content
FROM card_term_chunk c
JOIN card_term_document d ON d.card_term_document_id = c.card_term_document_id
LEFT JOIN card_company cc ON cc.card_company_id = d.card_company_id
WHERE c.card_term_chunk_id IN %s
"""


class TermIndex(ABC):
    """조각 색인의 계약. 부르는 쪽은 어느 구현인지 모른다."""

    model_tag: str

    @abstractmethod
    def rank(self, conn, query_vector: Sequence[float], limit: int) -> List[dict]:
        """질문과 가까운 순으로 조각을 돌려준다. score(코사인 유사도)를 얹어서 준다.

        conn 을 받는 이유는 Qdrant 쪽이 본문을 MySQL 에서 다시 읽기 때문이다.
        numpy 쪽은 이미 메모리에 있어 쓰지 않는다.
        """


@dataclass(frozen=True)
class NumpyTermIndex(TermIndex):
    """조각의 좌표와 설명을 같은 순서로 메모리에 들고 있는 표."""

    # (조각 수, 차원). 길이를 미리 1로 맞춰 둔다 — 그러면 유사도가 곱셈 한 번으로 끝난다.
    matrix: np.ndarray
    rows: List[dict]
    model_tag: str

    def rank(self, conn, query_vector: Sequence[float], limit: int) -> List[dict]:
        if not len(self.rows):
            return []

        query = np.asarray(query_vector, dtype=np.float32)
        norm = np.linalg.norm(query)
        if norm <= 0:
            return []

        # 길이를 1로 맞춰 두었으므로 내적이 곧 코사인 유사도다.
        scores = self.matrix @ (query / norm)

        # 전체를 줄 세우지 않고 상위만 골라낸다. 조각이 수만 개가 되면 정렬 비용이
        # 유사도 계산보다 커진다.
        top = np.argpartition(-scores, min(limit, len(scores) - 1))[:limit]
        top = top[np.argsort(-scores[top])]
        return [{**self.rows[index], "score": float(scores[index])} for index in top]


class QdrantTermIndex(TermIndex):
    """Qdrant 에서 가까운 id 를 받고, 본문은 MySQL 에서 읽는다."""

    def __init__(self, client, collection: str, model_tag: str, available: bool):
        self._client = client
        self._collection = collection
        self.model_tag = model_tag
        # 컬렉션이 없으면 빈 결과다. 있는 척하다 예외로 죽는 것보다 "못 찾았다"가 낫다.
        self.available = available

    def rank(self, conn, query_vector: Sequence[float], limit: int) -> List[dict]:
        if not self.available:
            return []

        from vectorstore import qdrant

        hits = qdrant.search(self._client, self._collection, query_vector, limit)
        if not hits:
            return []

        rows = _fetch_rows(conn, [point_id for point_id, _ in hits])
        # Qdrant 순서를 지킨다. MySQL 에 없는 id 는 조각이 지워진 뒤 동기화가 안 된 것이라 뺀다.
        return [{**rows[point_id], "score": score}
                for point_id, score in hits if point_id in rows]


_index: Optional[TermIndex] = None


def get_index(conn) -> TermIndex:
    """색인. 처음 부를 때 만든다."""
    global _index
    if _index is None:
        _index = _build(conn)
    return _index


def reset() -> None:
    """다음 검색 때 다시 만들게 한다. 조각을 넣고 확인하는 테스트에서 쓴다."""
    global _index
    _index = None


def _build(conn) -> TermIndex:
    settings = get_vectorstore_settings()
    model_tag = create_embedding_client().model_tag

    if settings.term_index == INDEX_QDRANT:
        return _build_qdrant(settings.qdrant_url, model_tag)
    if settings.term_index == INDEX_NUMPY:
        return _load_numpy(conn, model_tag)
    raise ValueError(f"알 수 없는 TERM_INDEX: {settings.term_index}")


def _build_qdrant(url: str, model_tag: str) -> QdrantTermIndex:
    # Qdrant 는 실제로 쓸 때만 불러온다. numpy 로 도는 동안 그 패키지가 끼어들지 않는다.
    from vectorstore import qdrant

    client = qdrant.connect(url)
    collection = qdrant.collection_name(model_tag)
    available = qdrant.has_collection(client, collection)
    if not available:
        logger.warning(
            "Qdrant 컬렉션이 없다 (%s). python -m ingest.sync_qdrant 로 동기화할 것", collection
        )
    else:
        logger.info("Qdrant 색인 연결: %s @ %s", collection, url)
    return QdrantTermIndex(client, collection, model_tag, available)


def _load_numpy(conn, model_tag: str) -> NumpyTermIndex:
    with conn.cursor() as cursor:
        cursor.execute(_LOAD_SQL, (model_tag,))
        rows = cursor.fetchall()

    if not rows:
        # 색인을 안 돌렸거나, 다른 모델로 만들어 두고 설정만 바꾼 경우다. 약관 질문에
        # 아무것도 못 찾는 상태가 되므로 조용히 넘기지 않는다.
        logger.warning(
            "약관 임베딩이 없다 (모델 %s). python -m ingest.build_embeddings 로 색인할 것",
            model_tag,
        )
        return NumpyTermIndex(matrix=np.empty((0, 0), dtype=np.float32), rows=[],
                              model_tag=model_tag)

    matrix = np.array([unpack_vector(row.pop("embedding")) for row in rows], dtype=np.float32)
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    # 길이가 0인 벡터는 나눌 수 없다. 1로 두면 값이 그대로 0 벡터로 남아 아무것과도 안 가깝다.
    norms[norms == 0] = 1.0
    matrix /= norms

    logger.info("약관 색인 적재: 조각 %d개 · 모델 %s", len(rows), model_tag)
    return NumpyTermIndex(matrix=matrix, rows=list(rows), model_tag=model_tag)


def _fetch_rows(conn, ids: Sequence[int]) -> Dict[int, dict]:
    if not ids:
        return {}
    with conn.cursor() as cursor:
        cursor.execute(_FETCH_SQL, (tuple(ids),))
        rows = cursor.fetchall()
    return {int(row["card_term_chunk_id"]): row for row in rows}
