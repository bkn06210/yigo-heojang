"""Voyage 임베딩 호출.

호출 주소를 설정으로 받는다. 키를 발급받은 곳에 따라 주소가 달라지는데
(api.voyageai.com / ai.mongodb.com) 요청·응답 모양은 같아서 구현을 나눌 이유가 없다.
주소가 어긋나면 키 문제처럼 401 로 떨어지므로, 예외에 응답 본문을 함께 실어 보낸다.
"""

import logging
import time
from typing import Any, Dict, List, Sequence

import httpx

from .base import EmbeddingClient, Vector

logger = logging.getLogger(__name__)


class EmbeddingApiError(RuntimeError):
    """공급자가 요청을 거절했다.

    상태 코드를 함께 담는 이유는 부르는 쪽이 무엇을 안내할지 갈리기 때문이다.
    한도 초과는 기다리면 되고, 인증 실패는 설정을 고쳐야 한다.
    """

    def __init__(self, status_code: int, detail: str):
        super().__init__(f"{status_code} {detail}")
        self.status_code = status_code
        self.detail = detail

# 한 요청에 담을 조각 수. 공급자 상한은 1,000개지만 토큰 상한에도 함께 걸리므로
# 조각 하나가 길어져도 안전한 선에서 끊는다. 하나씩 부르면 왕복 시간이 조각 수만큼 곱해진다.
_REQUEST_BATCH = 128

# 색인은 배치라 오래 걸려도 되지만, 질문은 사람이 답을 기다리는 자리라 짧게 끊는다.
_DOCUMENT_TIMEOUT_SECONDS = 60.0
_QUERY_TIMEOUT_SECONDS = 10.0

# 일시적인 429·5xx 만 넘기면 되므로 짧게 잡는다.
_MAX_RETRIES = 2
_RETRY_BACKOFF_SECONDS = 2.0


class VoyageEmbeddingClient(EmbeddingClient):
    provider = "voyage"

    def __init__(self, api_key: str, model: str, dimension: int, api_url: str):
        if not api_key:
            raise ValueError("VOYAGE_API_KEY 가 비어 있다. ai-server/.env 를 확인할 것")
        self._api_key = api_key
        self._api_url = api_url
        self.model = model
        self.dimension = dimension

    def embed_documents(self, texts: Sequence[str]) -> List[Vector]:
        vectors: List[Vector] = []
        # 부르는 쪽의 묶음과 다른 기준이다. 여기서 끊는 것은 요청 하나에 담을 수 있는
        # 양의 문제이고, 부르는 쪽이 끊는 것은 얼마나 자주 저장해 이어서 돌 수 있게 할지의 문제다.
        for start in range(0, len(texts), _REQUEST_BATCH):
            batch = texts[start : start + _REQUEST_BATCH]
            vectors.extend(self._embed(batch, "document", _DOCUMENT_TIMEOUT_SECONDS))
        return vectors

    def embed_query(self, text: str) -> Vector:
        return self._embed([text], "query", _QUERY_TIMEOUT_SECONDS)[0]

    def _embed(self, texts: Sequence[str], input_type: str, timeout: float) -> List[Vector]:
        payload: Dict[str, Any] = {
            "model": self.model,
            "input": list(texts),
            # 문서와 질문을 다르게 다루라는 힌트다. 좌표계는 같고 역할만 알려준다.
            "input_type": input_type,
            "output_dimension": self.dimension,
        }

        data = self._post(payload, timeout)

        # 응답 순서가 보낸 순서와 같다고 가정하지 않는다. 어긋나면 다른 조각의 좌표가
        # 붙어 검색이 엉뚱한 조문을 집어오는데, 값이 그럴듯해서 알아채기 어렵다.
        items = sorted(data["data"], key=lambda item: item["index"])
        if len(items) != len(texts):
            raise RuntimeError(f"임베딩 개수 불일치: 요청 {len(texts)}개, 응답 {len(items)}개")

        vectors = [item["embedding"] for item in items]
        for vector in vectors:
            if len(vector) != self.dimension:
                raise RuntimeError(
                    f"임베딩 차원 불일치: 기대 {self.dimension}, 응답 {len(vector)}"
                )

        _log_usage(data, len(texts))
        return vectors

    def _post(self, payload: Dict[str, Any], timeout: float) -> Dict[str, Any]:
        headers = {"Authorization": f"Bearer {self._api_key}"}

        for attempt in range(_MAX_RETRIES + 1):
            try:
                response = httpx.post(self._api_url, headers=headers, json=payload, timeout=timeout)
                response.raise_for_status()
                return response.json()
            except httpx.HTTPStatusError as error:
                # 상태 코드만으로는 무엇을 고쳐야 할지 알 수 없다. 한도 초과인지 키 문제인지
                # 모델명 오타인지가 전부 본문에 적혀 오므로 함께 남긴다.
                detail = _detail(error.response)
                # 요청 자체가 잘못된 경우(모델명 오타·차원 미지원)는 다시 보내도 같다.
                if not _retryable(error.response.status_code) or attempt == _MAX_RETRIES:
                    raise EmbeddingApiError(error.response.status_code, detail) from error
                logger.warning("임베딩 호출 %s, 재시도 %d회차 — %s",
                               error.response.status_code, attempt + 1, detail)
            except httpx.TransportError:
                if attempt == _MAX_RETRIES:
                    raise
                logger.warning("임베딩 호출 실패(연결), 재시도 %d회차", attempt + 1)

            time.sleep(_RETRY_BACKOFF_SECONDS * (attempt + 1))

        raise RuntimeError("임베딩 호출에 실패했다")


def _detail(response: httpx.Response) -> str:
    """응답 본문에서 사람이 읽을 설명을 꺼낸다."""
    try:
        body = response.json()
    except ValueError:
        return response.text[:300]
    return str(body.get("detail") or body)[:300]


def _retryable(status_code: int) -> bool:
    return status_code == 429 or status_code >= 500


def _log_usage(data: Dict[str, Any], count: int) -> None:
    """쓴 토큰을 남긴다. 무료 한도 안에서 도는지 눈으로 확인할 수 있어야 한다."""
    total = (data.get("usage") or {}).get("total_tokens")
    logger.info("임베딩 %d건 / %s토큰", count, total if total is not None else "?")
