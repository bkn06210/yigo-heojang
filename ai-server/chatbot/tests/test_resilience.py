"""의존성이 실패하거나 느려질 때의 챗봇 응답.

두 가지를 본다.

1. 어느 의존성이 실패해도 500 이 아니라 200 + 대체 답변이 나간다. LLM 만이 아니라
   DB·임베딩·약관 색인·엔진 실패까지다. 500 이 나가면 화면에 아무것도 안 남는다.
2. 의존성이 멈추거나 느려도 요청 마감시간 안에 돌아온다. Spring 이 기다려 주는 시간
   (읽기 타임아웃)보다 먼저 돌아와야 그 답이 사용자에게 닿는다. 늦게 만든 답은 버려지는데
   그사이 부른 LLM 은 토큰을 쓴다.

외부 호출은 하나도 없다. 2 에서는 LLM·임베딩의 실제 어댑터를 쓰되 네트워크 자리에 가짜를
끼워 응답 없음·느린 응답을 흉내 낸다. 타임아웃을 줄이고 재시도를 끊는 코드가 어댑터 안에
있어서, 어댑터를 통째로 가짜로 바꾸면 정작 확인하려는 것을 못 본다.

시간은 1/20 로 줄였다. 마감 20초 → 1초, LLM 한 번 20초 → 1초, 질문 임베딩 10초 → 0.5초,
Spring 읽기 타임아웃 25초 → 1.25초. 횟수(재시도 2회)는 그대로다.
측정값은 `pytest -s` 로 돌리면 표준 출력에 남는다.
"""

import dataclasses
import json
import time
from contextlib import contextmanager
from typing import Callable, Dict, List, Optional, Tuple

import httpx
import numpy as np
import pymysql
import pytest
from fastapi.testclient import TestClient

import deadline
from chatbot import main, term_index, terms
from chatbot import engine as engine_module
from chatbot.engine import EngineClient
from chatbot.llm import IntentName, openai_client
from chatbot.llm.openai_client import OpenAiLlmClient
from deadline import DeadlineExceeded, request_deadline
from embedding import voyage
from embedding.voyage import EmbeddingApiError, VoyageEmbeddingClient

client = TestClient(main.app)

TERM_QUESTION = "카드 잃어버리면 어떻게 해?"
STATUS_QUESTION = "실적 채웠어?"


@contextmanager
def _no_db():
    """DB 없이 도는 흐름용. 이 테스트들이 지나는 길에서는 연결을 열어 쓰지 않는다."""
    yield object()


def _ask(question: str):
    return client.post("/chat", json={"memberId": 1, "question": question})


# ── 1. 대체 답변의 범위 ────────────────────────────────────────


class TestFallbackScope:
    """LLM 밖의 의존성이 실패해도 200 + 안내 문구가 나간다."""

    def test_DB_에_못_붙으면_안내_문구로_답한다(self, monkeypatch):
        def broken_connection():
            raise pymysql.err.OperationalError(2003, "Can't connect to MySQL server")

        monkeypatch.setattr(main, "connection", broken_connection)

        response = _ask(STATUS_QUESTION)

        assert response.status_code == 200
        assert response.json()["answer"] == main._LOOKUP_DOWN_MESSAGE
        assert response.json()["intent"] == IntentName.CARD_STATUS

    def test_질문_임베딩이_실패하면_안내_문구로_답한다(self, monkeypatch):
        class BrokenEmbedding:
            def embed_query(self, text):
                raise EmbeddingApiError(503, "service unavailable")

        monkeypatch.setattr(main, "connection", _no_db)
        monkeypatch.setattr(terms, "_client", BrokenEmbedding)

        response = _ask(TERM_QUESTION)

        assert response.status_code == 200
        assert response.json()["answer"] == main._LOOKUP_DOWN_MESSAGE
        assert response.json()["intent"] == IntentName.TERM_QA

    def test_약관_색인을_못_읽으면_안내_문구로_답한다(self, monkeypatch):
        # 색인은 첫 검색 때 DB 에서 읽는다. 벡터 저장소를 따로 두어도 실패하는 자리는 여기다.
        def broken_index(conn):
            raise RuntimeError("약관 색인을 읽지 못했다")

        monkeypatch.setattr(main, "connection", _no_db)
        monkeypatch.setattr(term_index, "get_index", broken_index)

        response = _ask(TERM_QUESTION)

        assert response.status_code == 200
        assert response.json()["answer"] == main._LOOKUP_DOWN_MESSAGE

    def test_엔진이_뜻밖의_응답을_주면_안내_문구로_답한다(self, monkeypatch):
        # 401·5xx 같은 예상된 실패는 라우터가 되묻는 문장으로 바꾼다. 여기는 그 밖의 경우다
        # — 본문이 JSON 이 아니거나(프록시 오류 페이지) 모양이 달라 꺼내다 터지는 경우.
        class BrokenEngine:
            def __init__(self, **_kwargs):
                pass

            def monthly_status(self, year_month=None):
                raise ValueError("Expecting value: line 1 column 1 (char 0)")

        monkeypatch.setattr(main, "connection", _no_db)
        monkeypatch.setattr(main, "EngineClient", BrokenEngine)

        response = _ask(STATUS_QUESTION)

        assert response.status_code == 200
        assert response.json()["answer"] == main._LOOKUP_DOWN_MESSAGE

    def test_실패를_남길_때_질문_원문은_남기지_않는다(self, monkeypatch, caplog):
        def broken_connection():
            raise pymysql.err.OperationalError(2003, "Can't connect to MySQL server")

        monkeypatch.setattr(main, "connection", broken_connection)

        _ask(STATUS_QUESTION)

        assert "조회 실패" in caplog.text
        assert STATUS_QUESTION not in caplog.text


class TestEngineDeadline:
    """엔진 호출도 요청 마감 안에서만 기다린다."""

    def test_남은_시간이_모자라면_요청을_보내지_않는다(self, monkeypatch):
        def must_not_send(**_kwargs):
            pytest.fail("마감이 지났는데 엔진에 요청을 보냈다")

        monkeypatch.setattr(engine_module.httpx, "Client", must_not_send)

        with request_deadline(deadline.MIN_ATTEMPT_SECONDS / 2), pytest.raises(DeadlineExceeded):
            EngineClient(base_url="http://engine.test").monthly_status()

    def test_남은_시간이_원래_타임아웃보다_짧으면_그만큼만_기다린다(self, monkeypatch):
        opened_with: Dict[str, float] = {}
        real_client = httpx.Client

        def recording_client(**kwargs):
            opened_with["timeout"] = kwargs["timeout"]
            return real_client(transport=httpx.MockTransport(
                lambda request: httpx.Response(200, json={"data": {"cards": []}})), **kwargs)

        monkeypatch.setattr(engine_module.httpx, "Client", recording_client)

        with request_deadline(3.0):
            EngineClient(base_url="http://engine.test").monthly_status()

        assert opened_with["timeout"] <= 3.0


# ── 2. 마감시간 ────────────────────────────────────────────────

TIME_SCALE = 20
DEADLINE_SECONDS = 20.0 / TIME_SCALE
SPRING_READ_TIMEOUT_SECONDS = 25.0 / TIME_SCALE
# 마감이 사실상 없는 상태 = 마감을 넣기 전의 동작. 각 호출이 제 타임아웃과 재시도를 다 쓴다.
NO_DEADLINE_SECONDS = 3600.0
# 경과 시간 단언의 여유. 테스트 클라이언트·스레드 전환 몫이다. Spring 과의 차이(0.25초)보다 작다.
TOLERANCE_SECONDS = 0.15

HANG = None  # 응답을 주지 않는다

QUERY_VECTOR = [1.0] + [0.0] * 7
TERM_ROW = {
    "card_company_id": 1,
    "company_name": "삼성카드",
    "source_card_name": "개인회원 표준약관",
    "heading": "제40조(카드의 분실·도난)",
    "content": "회원은 카드를 분실하거나 도난당한 경우 지체 없이 카드사에 신고하여야 합니다.",
}


class FakeDependencies:
    """가짜 LLM·임베딩 서버. 단계마다 응답 시간을 정하고, 호출이 언제 시작됐는지 적어 둔다.

    받은 타임아웃보다 오래 걸릴 응답이면 그 타임아웃만큼 기다렸다가 시간 초과를 낸다.
    실제 HTTP 클라이언트가 하는 일과 같다.
    """

    def __init__(self, classify: Optional[float], embed: Optional[float], compose: Optional[float]):
        self.delays = {"classify": classify, "embed": embed, "compose": compose}
        self.calls: List[Tuple[str, float]] = []
        self.started_at = time.monotonic()

    def respond(self, stage: str, timeout: float, request: httpx.Request,
                make_response: Callable[[], httpx.Response]) -> httpx.Response:
        self.calls.append((stage, time.monotonic() - self.started_at))
        delay = self.delays[stage]
        if delay is HANG or delay >= timeout:
            time.sleep(timeout)
            raise httpx.ReadTimeout(f"{stage} 응답 없음", request=request)
        time.sleep(delay)
        return make_response()

    def llm_calls(self) -> List[float]:
        return [started for stage, started in self.calls if stage != "embed"]

    def count(self, stage: str) -> int:
        return sum(1 for called, _ in self.calls if called == stage)


class _LlmTransport(httpx.BaseTransport):
    """OpenAI 대신 받는다. 요청 본문으로 분류인지 답변 생성인지 가른다."""

    def __init__(self, dependencies: FakeDependencies):
        self._dependencies = dependencies

    def handle_request(self, request: httpx.Request) -> httpx.Response:
        body = json.loads(request.read())
        stage = "classify" if "response_format" in body else "compose"
        content = (json.dumps({"name": IntentName.TERM_QA, "termQuery": "분실 도난 신고"})
                   if stage == "classify" else "분실하면 카드사에 바로 신고하시면 됩니다.")
        return self._dependencies.respond(
            stage, request.extensions["timeout"]["read"], request,
            lambda: httpx.Response(200, json=_completion(content), request=request))


def _completion(content: str) -> dict:
    return {
        "id": "test", "object": "chat.completion", "created": 0, "model": "gpt-test",
        "choices": [{"index": 0, "finish_reason": "stop",
                     "message": {"role": "assistant", "content": content}}],
        "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2},
    }


@pytest.fixture
def scaled_time(monkeypatch):
    monkeypatch.setattr(openai_client, "_REQUEST_TIMEOUT_SECONDS", 20.0 / TIME_SCALE)
    monkeypatch.setattr(openai_client, "_RETRY_BACKOFF_SECONDS", 0.5 / TIME_SCALE)
    monkeypatch.setattr(voyage, "_QUERY_TIMEOUT_SECONDS", 10.0 / TIME_SCALE)
    monkeypatch.setattr(voyage, "_RETRY_BACKOFF_SECONDS", 2.0 / TIME_SCALE)
    monkeypatch.setattr(deadline, "MIN_ATTEMPT_SECONDS", 1.0 / TIME_SCALE)


@dataclasses.dataclass
class Measurement:
    status_code: int
    answer: str
    elapsed: float
    dependencies: FakeDependencies

    def late_llm_calls(self) -> int:
        """Spring 이 이미 끊은 뒤에 시작한 LLM 호출. 그 답은 받을 사람이 없다."""
        return sum(1 for started in self.dependencies.llm_calls() if started >= SPRING_READ_TIMEOUT_SECONDS)

    def delivered(self) -> str:
        """사용자가 실제로 받는 것."""
        if self.elapsed > SPRING_READ_TIMEOUT_SECONDS:
            return "503(Spring이 먼저 끊음)"
        return f"{self.status_code} {_kind(self.answer)}"


def _kind(answer: str) -> str:
    if answer == main._LLM_DOWN_MESSAGE:
        return "안내(분류 실패)"
    if answer == main._LOOKUP_DOWN_MESSAGE:
        return "안내(조회 실패)"
    if answer.startswith("답변을 다듬지 못해"):
        return "조회 결과 그대로"
    return "정상 답변"


def _run(monkeypatch, delays: Dict[str, Optional[float]], deadline_seconds: float) -> Measurement:
    dependencies = FakeDependencies(**delays)

    llm = OpenAiLlmClient("test-key", "gpt-test")
    llm._client = llm._client.with_options(http_client=httpx.Client(transport=_LlmTransport(dependencies)))

    def fake_embedding_post(url, headers=None, json=None, timeout=None):
        request = httpx.Request("POST", url)
        return dependencies.respond("embed", timeout, request, lambda: httpx.Response(
            200, json={"data": [{"index": 0, "embedding": QUERY_VECTOR}], "usage": {"total_tokens": 1}},
            request=request))

    embedding_client = VoyageEmbeddingClient("test-key", "voyage-test", len(QUERY_VECTOR), "http://embedding.test")
    index = term_index.TermIndex(matrix=np.array([QUERY_VECTOR], dtype=np.float32),
                                 rows=[dict(TERM_ROW)], model_tag=embedding_client.model_tag)

    monkeypatch.setattr(main, "_llm", llm)
    monkeypatch.setattr(main, "connection", _no_db)
    monkeypatch.setattr(main, "_settings", dataclasses.replace(main._settings, chat_deadline_seconds=deadline_seconds))
    monkeypatch.setattr(terms, "_client", lambda: embedding_client)
    monkeypatch.setattr(term_index, "get_index", lambda conn: index)
    monkeypatch.setattr(voyage.httpx, "post", fake_embedding_post)

    dependencies.started_at = time.monotonic()
    response = _ask(TERM_QUESTION)
    elapsed = time.monotonic() - dependencies.started_at
    return Measurement(response.status_code, response.json()["answer"], elapsed, dependencies)


@dataclasses.dataclass(frozen=True)
class Scenario:
    # 분류·질문 임베딩·답변 생성의 응답 시간(초, 1/20 축척). HANG 은 응답 없음.
    delays: Dict[str, Optional[float]]
    # 마감이 있을 때 사용자가 받아야 하는 것
    expected_answer: str


SCENARIOS = {
    # 무엇을 물었는지 모르니 안내 문구밖에 없다.
    "LLM 먹통(분류 단계)": Scenario(
        dict(classify=HANG, embed=0.02, compose=0.05), "안내(분류 실패)"),
    "임베딩 먹통": Scenario(
        dict(classify=0.05, embed=HANG, compose=0.05), "안내(조회 실패)"),
    # 값은 모았고 문장만 못 만들었다. 안내 문구보다 조회 결과가 낫다 — 값은 전부 맞다.
    "LLM 먹통(답변 생성 단계)": Scenario(
        dict(classify=0.05, embed=0.02, compose=HANG), "조회 결과 그대로"),
    # 하나하나는 제 타임아웃 안에서 답한다. 합이 Spring 이 기다려 주는 시간을 넘는 경우다.
    # 분류가 마감을 거의 다 써서 뒤 단계는 남은 시간 안에 못 끝난다. 답변 생성은 보내지도 않는다.
    "모두 느림(각자 타임아웃 안)": Scenario(
        dict(classify=0.9, embed=0.45, compose=0.9), "안내(조회 실패)"),
}


@pytest.fixture(scope="module")
def warmed_up():
    """첫 호출에만 드는 준비 비용(SDK 가 늦게 불러오는 모듈 등)을 측정에서 뺀다.

    빼지 않으면 처음 도는 시나리오만 2초 남짓 느리게 재어 비교가 어긋난다.
    """
    with pytest.MonkeyPatch.context() as monkeypatch:
        _run(monkeypatch, dict(classify=0.0, embed=0.0, compose=0.0), NO_DEADLINE_SECONDS)


@pytest.mark.parametrize("scenario", list(SCENARIOS))
def test_의존성이_멈추거나_느려도_마감_안에_200으로_돌아온다(scenario, monkeypatch, warmed_up, scaled_time):
    before = _run(monkeypatch, SCENARIOS[scenario].delays, NO_DEADLINE_SECONDS)
    after = _run(monkeypatch, SCENARIOS[scenario].delays, DEADLINE_SECONDS)

    print(f"\n[{scenario}] (시간 1/20 축척 · 괄호는 실제 환산 · Spring 읽기 타임아웃 "
          f"{SPRING_READ_TIMEOUT_SECONDS:.2f}초(25초))")
    for label, measured in (("마감 없음", before), ("마감 20초", after)):
        print(f"  {label}: 경과 {measured.elapsed:.2f}초({measured.elapsed * TIME_SCALE:.0f}초)"
              f" · 사용자가 받는 것 {measured.delivered()}"
              f" · 호출 분류 {measured.dependencies.count('classify')}"
              f"/임베딩 {measured.dependencies.count('embed')}"
              f"/생성 {measured.dependencies.count('compose')}회"
              f" · Spring이 끊은 뒤 시작한 LLM 호출 {measured.late_llm_calls()}회")

    # 마감이 없으면 Spring 이 먼저 끊는다 — 고치려는 문제가 실제로 재현되는지 먼저 본다.
    assert before.elapsed > SPRING_READ_TIMEOUT_SECONDS

    assert after.status_code == 200
    assert _kind(after.answer) == SCENARIOS[scenario].expected_answer
    assert after.elapsed <= DEADLINE_SECONDS + TOLERANCE_SECONDS
    assert after.elapsed < SPRING_READ_TIMEOUT_SECONDS
    # 받을 사람 없는 답에 토큰을 쓰지 않는다
    assert after.late_llm_calls() == 0
