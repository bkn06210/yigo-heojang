"""약관 조각의 임베딩을 한 번만 읽어 메모리에 둔다.

질문마다 DB 에서 다시 읽으면 6MB 를 옮기고 이진을 숫자로 되돌리는 일이 매번 반복된다.
그런데 이 데이터는 질문에 따라 바뀌지 않는다 — 약관은 카드를 추가할 때만 달라진다.
실측으로 질문 한 건에 읽기 66ms + 되돌리기 87ms 가 계산도 시작하기 전에 들었다.

**색인을 다시 만들면 챗봇을 재기동해야 한다.** 메모리에 담아둔 것이 옛것이 되기 때문이다.
카드 추가는 수집 파이프라인을 돌리는 배치 작업이라 그 김에 재기동하면 된다.

처음 검색할 때 읽는다(기동 시점이 아니라). 컨테이너로 띄우면 챗봇이 MySQL 보다 먼저 뜨는
일이 있어, 기동 중에 읽으면 DB 가 아직 안 떠 있다는 이유로 서버가 통째로 죽는다.
"""

import logging
from dataclasses import dataclass
from typing import List, Optional

import numpy as np

from embedding import create_embedding_client, unpack_vector

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


@dataclass(frozen=True)
class TermIndex:
    """조각의 좌표와 설명을 같은 순서로 들고 있는 표."""

    # (조각 수, 차원). 길이를 미리 1로 맞춰 둔다 — 그러면 유사도가 곱셈 한 번으로 끝난다.
    matrix: np.ndarray
    rows: List[dict]
    model_tag: str

    def rank(self, query_vector: List[float], limit: int) -> List[dict]:
        """질문과 가까운 순으로 조각을 돌려준다. score 를 얹어서 준다."""
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


_index: Optional[TermIndex] = None


def get_index(conn) -> TermIndex:
    """메모리에 올려둔 색인. 없으면 이때 읽는다."""
    global _index
    if _index is None:
        _index = _load(conn)
    return _index


def reset() -> None:
    """다음 검색 때 다시 읽게 한다. 조각을 넣고 확인하는 테스트에서 쓴다."""
    global _index
    _index = None


def _load(conn) -> TermIndex:
    model_tag = create_embedding_client().model_tag

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
        return TermIndex(matrix=np.empty((0, 0), dtype=np.float32), rows=[], model_tag=model_tag)

    matrix = np.array([unpack_vector(row.pop("embedding")) for row in rows], dtype=np.float32)
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    # 길이가 0인 벡터는 나눌 수 없다. 1로 두면 값이 그대로 0 벡터로 남아 아무것과도 안 가깝다.
    norms[norms == 0] = 1.0
    matrix /= norms

    logger.info("약관 색인 적재: 조각 %d개 · 모델 %s", len(rows), model_tag)
    return TermIndex(matrix=matrix, rows=list(rows), model_tag=model_tag)
