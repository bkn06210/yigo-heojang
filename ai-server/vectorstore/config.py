"""벡터 색인 설정.

값은 ai-server/.env 에서 읽는다. 임베딩 설정과 나눈 이유는 층이 다르기 때문이다 —
임베딩은 문장을 숫자로 바꾸는 모델이고, 여기는 그 숫자를 어디에 두고 어떻게 찾을지다.
모델을 바꾸지 않고 저장소만 바꾸는 일이 있고, 그 반대도 있다.
"""

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"

# 챗봇 메모리에서 numpy 로 전수 계산한다. 조각 1,550개에 0.4ms 라 지금 규모의 기본값이다.
INDEX_NUMPY = "numpy"
# 별도 서버(Qdrant)가 근사 최근접 이웃으로 찾는다. 조각이 수십만 건이 되면 이쪽이 낫다.
INDEX_QDRANT = "qdrant"


@dataclass(frozen=True)
class VectorStoreSettings:
    term_index: str
    qdrant_url: str


@lru_cache(maxsize=1)
def get_vectorstore_settings() -> VectorStoreSettings:
    load_dotenv(_ENV_PATH)
    return VectorStoreSettings(
        term_index=os.getenv("TERM_INDEX", INDEX_NUMPY),
        qdrant_url=os.getenv("QDRANT_URL", "http://localhost:6333"),
    )
