"""챗봇 서버 진입점.

프론트가 이 서버를 직접 부르지 않는다. Spring 이 JWT 를 검증한 뒤 넘겨주는 요청만 받는다.
그래서 기본 바인딩이 127.0.0.1 이다 — 밖으로 열리면 아무도 토큰을 확인하지 않은 채
memberId 만 바꿔 남의 소비내역을 물어볼 수 있다.

한 질문이 지나가는 길은 이렇다.

    질문 → classify(LLM) → 직전 맥락과 병합 → 표현 해석(DB) → 값 조회(엔진 API)
         → compose(LLM) → 답변

판단과 계산은 그 사이 어디에도 없다. LLM 은 말을 알아듣고 문장을 쓰는 두 자리에만 있다.

실행:
    uvicorn chatbot.main:app --reload   (ai-server 폴더에서)
"""

from typing import Optional

from fastapi import FastAPI, Header

from .config import get_settings
from .db import connection
from .engine import EngineClient
from .llm import Intent, IntentName, LlmClient, create_llm_client
from .router import route
from .schema import ChatRequest, ChatResponse, HealthResponse, PendingContext

app = FastAPI(title="카드 혜택 챗봇", docs_url="/docs")

_settings = get_settings()
_llm: LlmClient = create_llm_client(_settings)

_UNKNOWN_FOLLOW_UP = (
    "무엇을 알려드릴지 잘 모르겠습니다. "
    "받은 혜택 금액, 실적 현황, 결제할 카드 추천, 약관 중 어떤 것이 궁금하신가요?"
)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """서버가 살아 있는지와 지금 어떤 LLM 으로 도는지.

    provider 를 같이 내려주는 이유는 stub 인 채로 시연에 들어가는 사고를 막기 위해서다.
    """
    return HealthResponse(status="UP", llm_provider=_llm.provider)


@app.post("/chat", response_model=ChatResponse)
def chat(
    request: ChatRequest,
    # Spring 이 원래 요청의 헤더를 그대로 넘겨준다. 챗봇은 열어보지 않고 엔진 호출에 다시 쓴다.
    authorization: Optional[str] = Header(default=None),
) -> ChatResponse:
    intent = _merge(_llm.classify(request.question), request.pending_context)

    if intent.name == IntentName.UNKNOWN:
        return ChatResponse(
            answer=_UNKNOWN_FOLLOW_UP,
            intent=intent.name,
            follow_up_question=_UNKNOWN_FOLLOW_UP,
        )

    engine = EngineClient(base_url=_settings.backend_base_url, authorization=authorization)
    with connection() as conn:
        result = route(intent, conn, engine)

    pending = _to_pending(intent) if result.follow_up else None

    if result.context is None:
        # 조회할 값을 못 모은 경우다. 지어내지 않고 되묻는다.
        message = result.follow_up or _UNKNOWN_FOLLOW_UP
        return ChatResponse(
            answer=message,
            intent=intent.name,
            follow_up_question=message,
            pending_context=pending,
        )

    answer = _llm.compose(request.question, result.context)
    return ChatResponse(
        answer=answer.text,
        intent=intent.name,
        sources=result.sources + answer.sources,
        follow_up_question=result.follow_up,
        pending_context=pending,
    )


def _merge(fresh: Intent, pending: Optional[PendingContext]) -> Intent:
    """직전에 되물은 맥락과 이번 분류를 합친다.

    "스벅에서 어느 카드가 좋아?" → "얼마를 결제하실 예정인가요?" → "8000원"
    마지막 답만 보면 무엇을 묻는지 알 수 없다. 직전 의도와 가맹점을 이어받아야 한다.

    맥락을 이어받는 경우를 둘로 제한한다 — 이번 분류가 UNKNOWN 이거나(답만 말한 경우),
    직전과 같은 의도일 때(같은 주제를 이어가는 경우)다. 주제가 바뀌면 통째로 버린다.
    안 그러면 "스벅"이 다음 질문까지 따라붙는다.
    """
    if pending is None:
        return fresh
    if fresh.name != IntentName.UNKNOWN and fresh.name != pending.intent:
        return fresh

    return Intent(
        name=pending.intent,
        merchant_text=fresh.merchant_text or pending.merchant_text,
        category_text=fresh.category_text or pending.category_text,
        card_text=fresh.card_text or pending.card_text,
        period_text=fresh.period_text or pending.period_text,
        amount=fresh.amount or pending.amount,
        raw=fresh.raw,
    )


def _to_pending(intent: Intent) -> PendingContext:
    return PendingContext(
        intent=intent.name,
        merchant_text=intent.merchant_text,
        category_text=intent.category_text,
        card_text=intent.card_text,
        period_text=intent.period_text,
        amount=intent.amount,
    )
