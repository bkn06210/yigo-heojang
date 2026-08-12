"""LLM 어댑터.

어느 구현을 쓸지는 설정 한 곳에서만 정한다. 챗봇 코드에는 provider 분기가 없다.
"""

from ..config import PROVIDER_OPENAI, PROVIDER_STUB, Settings
from .base import Answer, Intent, IntentName, LlmClient
from .stub import StubLlmClient

__all__ = [
    "Answer",
    "Intent",
    "IntentName",
    "LlmClient",
    "StubLlmClient",
    "create_llm_client",
]


def create_llm_client(settings: Settings) -> LlmClient:
    if settings.llm_provider == PROVIDER_OPENAI:
        # openai 패키지는 실제로 쓸 때만 불러온다. stub 으로만 개발하는 동안
        # 패키지가 없어도 서버가 뜬다.
        from .openai_client import OpenAiLlmClient

        return OpenAiLlmClient(settings.openai_api_key, settings.openai_model)

    if settings.llm_provider == PROVIDER_STUB:
        return StubLlmClient()

    raise ValueError(f"알 수 없는 LLM_PROVIDER: {settings.llm_provider}")
