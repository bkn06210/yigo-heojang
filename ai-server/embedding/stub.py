"""개발용 가짜 임베딩.

API 키 없이 색인 → 저장 → 검색 전 구간을 실제로 돌리기 위한 것이다. 벡터 경로를
아예 꺼두면 그 코드가 한 번도 실행되지 않은 채로 있다가 키를 받은 날 처음 도는데,
색인 도구는 조각 수천 개를 한 번에 다루므로 그 자리에서 처음 터지면 되돌리기 번거롭다.

만드는 방법은 글자 조각을 해시해 고정 자리에 쌓는 것이다. 글자가 많이 겹치는 문장끼리
값이 비슷해지므로 유사도 정렬이 "그럴듯하게" 동작한다. 다만 이것은 글자를 세는 것이지
뜻을 읽는 것이 아니다 — "잃어버렸어요"와 "분실"은 겹치는 글자가 없어 여전히 못 찾는다.
임베딩을 쓰는 이유가 바로 그 자리라, **stub 으로는 임베딩의 효과를 확인할 수 없다.**
시연 전에는 반드시 실제 공급자로 한 번 돌려 검색 품질을 확인해야 한다.

같은 글에 항상 같은 값이 나온다. 값이 매번 흔들리면 검색 결과가 재현되지 않아
테스트로 고정할 수 없다.
"""

import hashlib
import re
from typing import List, Sequence

from .base import EmbeddingClient, Vector
from .vector import normalize

# 이 이름이 card_term_chunk.embedding_model 에 남는다. 실제 공급자로 바꾸면 이름이
# 달라지므로, 색인 도구가 "다른 좌표계로 만든 값"으로 보고 전부 다시 만든다.
STUB_MODEL = "stub-hash"


class StubEmbeddingClient(EmbeddingClient):
    provider = "stub"

    def __init__(self, dimension: int):
        self.model = STUB_MODEL
        self.dimension = dimension

    def embed_documents(self, texts: Sequence[str]) -> List[Vector]:
        return [self._vector(text) for text in texts]

    def embed_query(self, text: str) -> Vector:
        # 문서와 똑같이 만든다. 뜻을 읽는 모델이 아니라 역할을 구분할 근거가 없고,
        # 다르게 만들면 같은 글을 넣어도 자기 자신을 못 찾는다.
        return self._vector(text)

    def _vector(self, text: str) -> Vector:
        """글자 조각을 해시해 고정 자리에 쌓는다.

        조각을 글자 두 개로 자르는 것은 한국어가 띄어쓰기로 단어가 갈리지 않기 때문이다.
        낱말 단위로 자르면 '분실신고'가 통째로 하나가 되어 '분실'과 겹치는 조각이 없다.
        약관 원문 색인이 같은 이유로 ngram 파서를 쓰고 있다.
        """
        vector = [0.0] * self.dimension

        for token in _tokens(text):
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
            # 자리와 부호를 다른 바이트에서 뽑는다. 한 값에서 둘 다 꺼내면 자리가 정해지는
            # 순간 부호도 함께 정해져, 서로 다른 조각이 같은 자리에서 상쇄되지 않는다.
            index = int.from_bytes(digest[:4], "big") % self.dimension
            sign = 1.0 if digest[4] & 1 else -1.0
            vector[index] += sign

        # 길이를 1로 맞춰 둔다. 긴 글일수록 값이 커지므로, 맞추지 않으면 글이 길다는
        # 이유만으로 유사도가 높게 나온다.
        return normalize(vector)


def _tokens(text: str) -> List[str]:
    cleaned = re.sub(r"\s+", "", text).lower()
    if len(cleaned) < 2:
        return [cleaned] if cleaned else []
    return [cleaned[index : index + 2] for index in range(len(cleaned) - 1)]
