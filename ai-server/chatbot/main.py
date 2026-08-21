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

import logging
from typing import Optional

from fastapi import FastAPI, Header

from .config import get_settings
from .db import connection
from .engine import EngineClient
from .llm import Intent, IntentName, LlmClient, create_llm_client
from .router import route
from .schema import ChatRequest, ChatResponse, HealthResponse, PendingContext
from .verifier import fallback_answer, verify

logger = logging.getLogger(__name__)

app = FastAPI(title="카드 혜택 챗봇", docs_url="/docs")

_settings = get_settings()
_llm: LlmClient = create_llm_client(_settings)

_UNKNOWN_FOLLOW_UP = (
    "무엇을 알려드릴지 잘 모르겠습니다. "
    "받은 혜택 금액, 실적 현황, 결제할 카드 추천, 약관 중 어떤 것이 궁금하신가요?"
)

# LLM 이 아예 응답하지 못할 때(타임아웃·한도 초과·장애) 보여줄 말.
# 대화가 끊기지 않게 200 으로 내려준다 — 500 을 던지면 화면에 아무것도 안 남는다.
_LLM_DOWN_MESSAGE = (
    "지금은 답변을 만들지 못했습니다. 잠시 후 다시 물어봐 주세요."
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
    try:
        intent = _merge(_llm.classify(request.question), request.pending_context)
    except Exception:
        # 무엇을 물었는지조차 알 수 없으므로 조회로 넘어갈 수 없다
        logger.exception("의도 분류 실패")
        return ChatResponse(answer=_LLM_DOWN_MESSAGE, intent=IntentName.UNKNOWN)

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

    try:
        answer = _llm.compose(request.question, result.context)
    except Exception:
        # 조회는 끝났으니 값은 있다. 문장만 못 만든 것이라 조회 결과를 그대로 보여준다
        logger.exception("답변 생성 실패 (intent=%s)", intent.name)
        return ChatResponse(
            answer=fallback_answer(result.context),
            intent=intent.name,
            sources=result.sources,
            follow_up_question=result.follow_up,
            pending_context=pending,
        )

    verdict = verify(answer.text, result.context)
    if not verdict.ok:
        # 조회 결과에 없는 금액을 말한 답변은 내보내지 않는다. 문장이 자연스러워서
        # 읽는 사람이 틀렸다는 것을 알 방법이 없고, 그 숫자로 카드를 고르게 된다
        logger.warning("답변 검증 실패 (intent=%s): %s %s",
                       intent.name, verdict.reason, verdict.unsupported_numbers)
        return ChatResponse(
            answer=fallback_answer(result.context),
            intent=intent.name,
            sources=result.sources,
            follow_up_question=result.follow_up,
            pending_context=pending,
        )

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
