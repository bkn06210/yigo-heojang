"""벡터 색인 — 조각 벡터를 어디에 두고 어떻게 찾을지.

챗봇(검색)과 수집 파이프라인(동기화)이 같이 쓰므로 어느 한쪽에 넣지 않았다.
Qdrant 구현은 여기서 바로 불러오지 않는다 — numpy 로 도는 동안 그 패키지가 끼어들지 않게.
"""

from .config import (
    INDEX_NUMPY,
    INDEX_QDRANT,
    VectorStoreSettings,
    get_vectorstore_settings,
)

__all__ = [
    "INDEX_NUMPY",
    "INDEX_QDRANT",
    "VectorStoreSettings",
    "get_vectorstore_settings",
]
