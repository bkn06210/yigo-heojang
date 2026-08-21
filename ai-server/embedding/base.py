"""임베딩 어댑터의 공통 계약.

임베딩은 문장을 숫자 배열로 바꾼 것이다. 뜻이 가까운 문장끼리 값이 비슷해지므로
글자가 겹치지 않아도 찾을 수 있다. 낱말 검색은 "잃어버렸어요"로 "분실" 조항을
찾지 못하는데(실측 일치 0건), 이 값이 그 자리를 메운다.

부르는 쪽은 이 두 메서드만 알고, 뒤에 실제 모델이 있는지 개발용 가짜가 있는지
알지 못한다. 한국어 성능이 아직 검증되지 않아 공급자를 갈아탈 가능성이 있고,
그때 색인 도구와 검색 코드를 함께 고쳐야 하는 상황을 피하기 위한 층이다.

소비자가 둘이라 챗봇·수집 어느 쪽에도 넣지 않고 따로 두었다.
    문서 색인 — 조각 수천 개를 한 번에 (수집 파이프라인)
    질문 임베딩 — 한 건을 사람이 기다리는 동안 (챗봇 런타임)
"""

from abc import ABC, abstractmethod
from typing import List, Sequence

Vector = List[float]


class EmbeddingClient(ABC):
    """임베딩 공급자 한 곳을 감싼 어댑터."""

    provider: str
    model: str
    dimension: int

    @property
    def model_tag(self) -> str:
        """어떤 좌표계로 만든 값인지 나타내는 이름. card_term_chunk.embedding_model 에 저장한다.

        모델 이름에 차원을 붙이는 이유는 차원이 좌표계의 일부이기 때문이다.
        같은 모델이라도 1024 로 만든 값과 512 로 만든 값은 길이가 달라 견줄 수 없다.
        모델 이름만 저장하면 차원을 바꿔도 "이미 색인된 조각"으로 보여 다시 만들지 않고,
        길이가 다른 벡터끼리 유사도를 구하다 검색이 조용히 어긋난다.
        """
        return f"{self.model}-{self.dimension}"

    @abstractmethod
    def embed_documents(self, texts: Sequence[str]) -> List[Vector]:
        """색인할 문서 조각을 임베딩한다. 입력 순서와 결과 순서가 같아야 한다."""

    @abstractmethod
    def embed_query(self, text: str) -> Vector:
        """질문을 임베딩한다.

        문서와 나눈 것은 공급자가 둘을 다르게 다루기 때문이다. 문서는 "찾아지는 쪽",
        질문은 "찾는 쪽"이라 역할이 달라, 힌트를 주면 같은 모델이라도 검색이 더 맞는다.
        좌표계는 하나여야 하므로 모델은 반드시 같은 것을 쓴다.
        """
