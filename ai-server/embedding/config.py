"""임베딩 설정.

값은 ai-server/.env 에서 읽는다. 이 파일은 저장소에 올라가지 않으므로 API 키가
코드·커밋·캡처에 남지 않는다. 필요한 키 목록은 .env.example 에 있다.

챗봇 설정과 따로 둔 이유는 읽는 쪽이 둘이기 때문이다. 수집 파이프라인이
챗봇 서버 설정을 가져다 쓰게 되면, 배치 도구가 서버 코드에 묶인다.
"""

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"

PROVIDER_STUB = "stub"
PROVIDER_VOYAGE = "voyage"

# 같은 모델이라도 키를 어디서 만들었느냐에 따라 주소가 다르다. 요청·응답 모양은 같으므로
# 구현을 나누지 않고 주소만 설정으로 받는다.
#   Voyage 대시보드에서 만든 키 → api.voyageai.com
#   MongoDB Atlas 에서 만든 키   → ai.mongodb.com
_DEFAULT_API_URL = "https://api.voyageai.com/v1/embeddings"

# 저장 자리가 차원에 정비례한다(조각 하나당 4바이트 × 차원). 대신 줄일수록 뜻을 담는
# 자리가 줄어 검색이 무뎌지므로 기본값은 모델 권장값을 쓴다.
_DEFAULT_DIMENSION = 1024


@dataclass(frozen=True)
class EmbeddingSettings:
    provider: str
    model: str
    dimension: int
    api_key: str
    api_url: str


@lru_cache(maxsize=1)
def get_embedding_settings() -> EmbeddingSettings:
    load_dotenv(_ENV_PATH)
    return EmbeddingSettings(
        provider=os.getenv("EMBEDDING_PROVIDER", PROVIDER_STUB),
        model=os.getenv("EMBEDDING_MODEL", "voyage-4-lite"),
        dimension=int(os.getenv("EMBEDDING_DIMENSION", str(_DEFAULT_DIMENSION))),
        api_key=os.getenv("VOYAGE_API_KEY", ""),
        api_url=os.getenv("EMBEDDING_API_URL", _DEFAULT_API_URL),
    )
