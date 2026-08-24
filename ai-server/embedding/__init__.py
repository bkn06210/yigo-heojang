"""임베딩 어댑터.

어느 구현을 쓸지는 설정 한 곳에서만 정한다. 부르는 코드에는 provider 분기가 없다.

    색인   ingest/build_embeddings.py  — 조각 수천 개를 한 번에
    검색   chatbot                     — 질문 한 건을 사람이 기다리는 동안

둘이 같은 모델·같은 차원을 써야 거리 비교가 성립하므로, 고르는 자리를 하나로 둔다.
"""

from typing import Optional

from .base import EmbeddingClient, Vector
from .config import (
    PROVIDER_STUB,
    PROVIDER_VOYAGE,
    EmbeddingSettings,
    get_embedding_settings,
)
from .stub import STUB_MODEL, StubEmbeddingClient
from .vector import cosine_similarity, normalize, pack_vector, unpack_vector

__all__ = [
    "STUB_MODEL",
    "EmbeddingClient",
    "EmbeddingSettings",
    "PROVIDER_STUB",
    "PROVIDER_VOYAGE",
    "StubEmbeddingClient",
    "Vector",
    "cosine_similarity",
    "create_embedding_client",
    "get_embedding_settings",
    "normalize",
    "pack_vector",
    "unpack_vector",
]


def create_embedding_client(settings: Optional[EmbeddingSettings] = None) -> EmbeddingClient:
    settings = settings or get_embedding_settings()

    if settings.provider == PROVIDER_VOYAGE:
        # httpx 는 챗봇이 이미 쓰고 있어 따로 받을 것이 없지만, 공급자별 구현은
        # 실제로 쓸 때만 불러온다. stub 으로 개발하는 동안 그 코드가 끼어들지 않는다.
        from .voyage import VoyageEmbeddingClient

        return VoyageEmbeddingClient(
            settings.api_key, settings.model, settings.dimension, settings.api_url
        )

    if settings.provider == PROVIDER_STUB:
        return StubEmbeddingClient(settings.dimension)

    raise ValueError(f"알 수 없는 EMBEDDING_PROVIDER: {settings.provider}")
