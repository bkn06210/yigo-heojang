"""Voyage 어댑터 테스트 — 키 없이 요청 모양과 방어 장치만 확인한다.

값이 그럴듯하게 나와도 틀릴 수 있는 자리를 본다.
  · 응답 순서가 어긋나면 다른 조각의 좌표가 붙는데, 검색 결과가 그럴듯해 알아채기 어렵다
  · 차원이 다른 값이 섞이면 유사도가 무의미해진다
"""

from typing import Any, Dict, List

import httpx
import pytest

from deadline import request_deadline
from embedding.voyage import VoyageEmbeddingClient


class FakeResponse:
    def __init__(self, payload: Dict[str, Any]):
        self._payload = payload

    def raise_for_status(self) -> None:
        return None

    def json(self) -> Dict[str, Any]:
        return self._payload


class FakeApi:
    """호출을 기록하고 정해둔 값을 돌려준다."""

    def __init__(self, dimension: int = 4):
        self.calls: List[Dict[str, Any]] = []
        self._dimension = dimension
        self.reverse = False
        self.drop_one = False
        self.wrong_dimension = False

    def __call__(self, url, headers=None, json=None, timeout=None):
        self.calls.append({"url": url, "headers": headers, "json": json, "timeout": timeout})
        texts = json["input"]
        if self.drop_one:
            texts = texts[:-1]

        width = 1 if self.wrong_dimension else self._dimension
        data = [
            {"index": position, "embedding": [float(position)] * width}
            for position in range(len(texts))
        ]
        if self.reverse:
            data.reverse()
        return FakeResponse({"data": data, "usage": {"total_tokens": 10}})


@pytest.fixture
def api(monkeypatch):
    fake = FakeApi()
    monkeypatch.setattr("embedding.voyage.httpx.post", fake)
    return fake


_API_URL = "https://api.voyageai.com/v1/embeddings"


def _client(dimension: int = 4, api_url: str = _API_URL) -> VoyageEmbeddingClient:
    return VoyageEmbeddingClient(
        api_key="test-key", model="voyage-4-lite", dimension=dimension, api_url=api_url
    )


class TestRequest:

    def test_문서와_질문에_다른_힌트를_보낸다(self, api):
        client = _client()

        client.embed_documents(["약관 본문"])
        client.embed_query("연회비 얼마야")

        assert api.calls[0]["json"]["input_type"] == "document"
        assert api.calls[1]["json"]["input_type"] == "query"

    def test_모델과_차원을_함께_보낸다(self, api):
        _client().embed_query("분실 신고")

        payload = api.calls[0]["json"]
        assert payload["model"] == "voyage-4-lite"
        assert payload["output_dimension"] == 4

    def test_설정한_주소로_보낸다(self, api):
        """키를 발급받은 곳에 따라 주소가 다르다. 어긋나면 인증 단계에서 떨어진다."""
        _client(api_url="https://ai.mongodb.com/v1/embeddings").embed_query("분실 신고")

        assert api.calls[0]["url"] == "https://ai.mongodb.com/v1/embeddings"

    def test_키를_헤더에_담는다(self, api):
        _client().embed_query("분실 신고")

        assert api.calls[0]["headers"]["Authorization"] == "Bearer test-key"

    def test_질문은_색인보다_짧게_기다린다(self, api):
        """사람이 답을 기다리는 자리라 오래 끌 수 없다."""
        client = _client()

        client.embed_documents(["약관 본문"])
        client.embed_query("연회비 얼마야")

        assert api.calls[1]["timeout"] < api.calls[0]["timeout"]

    def test_많은_조각은_나눠_보낸다(self, api):
        """한 요청에 담을 수 있는 양에 상한이 있다."""
        vectors = _client().embed_documents([f"조각 {number}" for number in range(300)])

        assert len(api.calls) == 3
        assert len(vectors) == 300


class TestDefenses:

    def test_응답_순서가_어긋나도_보낸_순서로_되돌린다(self, api):
        api.reverse = True

        vectors = _client().embed_documents(["첫째", "둘째", "셋째"])

        assert vectors == [[0.0] * 4, [1.0] * 4, [2.0] * 4]

    def test_개수가_모자라면_실패로_본다(self, api):
        api.drop_one = True

        with pytest.raises(RuntimeError):
            _client().embed_documents(["첫째", "둘째"])

    def test_차원이_다르면_저장하지_않는다(self, api):
        api.wrong_dimension = True

        with pytest.raises(RuntimeError):
            _client().embed_query("분실 신고")

    def test_키가_없으면_만들_때_바로_알려준다(self):
        with pytest.raises(ValueError):
            VoyageEmbeddingClient(
                api_key="", model="voyage-4-lite", dimension=1024, api_url=_API_URL
            )


class TimingOutApi:
    """매번 시간 초과로 실패한다. 받은 타임아웃을 적어 둔다."""

    def __init__(self):
        self.timeouts: List[float] = []
        # 재시도 사이에 실제로 쉬면 테스트가 몇 초씩 걸린다. 쉰 시간만 적어 둔다.
        self.slept: List[float] = []

    def __call__(self, url, headers=None, json=None, timeout=None):
        self.timeouts.append(timeout)
        raise httpx.ReadTimeout("응답 없음", request=httpx.Request("POST", url))


class TestDeadline:
    """질문 임베딩은 챗봇 요청의 마감 안에서만 시도한다. 색인 배치는 마감이 없어 그대로다."""

    @pytest.fixture
    def timing_out(self, monkeypatch):
        fake = TimingOutApi()
        monkeypatch.setattr("embedding.voyage.httpx.post", fake)
        monkeypatch.setattr("embedding.voyage.time.sleep", fake.slept.append)
        return fake

    def test_마감이_없으면_원래_타임아웃과_재시도를_그대로_쓴다(self, timing_out):
        with pytest.raises(httpx.ReadTimeout):
            _client().embed_documents(["약관 본문"])

        assert timing_out.timeouts == [60.0, 60.0, 60.0]
        assert timing_out.slept == [2.0, 4.0]

    def test_마감이_있으면_남은_시간만큼만_기다린다(self, timing_out):
        with request_deadline(2.5), pytest.raises(httpx.ReadTimeout):
            _client().embed_query("분실 신고")

        # 원래 10초를 기다릴 자리지만 마감까지 2.5초뿐이다.
        assert timing_out.timeouts[0] <= 2.5

    def test_쉬고_나서_다시_시도할_시간이_없으면_재시도하지_않는다(self, timing_out):
        # 2초 쉬고 나면 0.5초가 남는데, 그 시간에 새로 보낸 요청은 대개 끝나지 못한다.
        with request_deadline(2.5), pytest.raises(httpx.ReadTimeout):
            _client().embed_query("분실 신고")

        assert len(timing_out.timeouts) == 1
        assert timing_out.slept == []
