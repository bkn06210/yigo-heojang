"""서버 설정.

값은 전부 ai-server/.env 에서 읽는다. 이 파일은 저장소에 올라가지 않으므로
API 키가 코드·커밋·캡처에 남지 않는다. 필요한 키 목록은 .env.example 에 있다.

설정을 한 곳에 모으는 이유는 "지금 어떤 LLM으로 도는지"를 한 군데서만 보면
되게 하기 위해서다. 개발 중에는 stub, 시연 때만 openai 로 바꾼다.
"""

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"

PROVIDER_STUB = "stub"
PROVIDER_OPENAI = "openai"


@dataclass(frozen=True)
class Settings:
    llm_provider: str
    openai_api_key: str
    openai_model: str
    # 엔진 계산값(추천·현황)을 받아오는 곳. 금액 계산은 이 서버가 하지 않는다.
    backend_base_url: str
    # 이 서버는 Spring 이 검증을 마친 요청만 받는다. 밖에서 직접 열리면
    # 아무도 토큰을 확인하지 않은 채 남의 소비내역을 물어볼 수 있다.
    host: str
    port: int


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    load_dotenv(_ENV_PATH)
    return Settings(
        llm_provider=os.getenv("LLM_PROVIDER", PROVIDER_STUB),
        openai_api_key=os.getenv("OPENAI_API_KEY", ""),
        openai_model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
        backend_base_url=os.getenv("BACKEND_BASE_URL", "http://localhost:8080"),
        host=os.getenv("CHATBOT_HOST", "127.0.0.1"),
        port=int(os.getenv("CHATBOT_PORT", "8000")),
    )
