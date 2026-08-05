"""챗봇 서버 진입점.

프론트가 이 서버를 직접 부르지 않는다. Spring 이 JWT 를 검증한 뒤 넘겨주는 요청만 받는다.
그래서 기본 바인딩이 127.0.0.1 이다 — 밖으로 열리면 아무도 토큰을 확인하지 않은 채
memberId 만 바꿔 남의 소비내역을 물어볼 수 있다.

실행:
    uvicorn chatbot.main:app --reload   (ai-server 폴더에서)
"""

from fastapi import FastAPI

from .config import get_settings
from .llm import IntentName, LlmClient, create_llm_client
from .schema import ChatRequest, ChatResponse, HealthResponse

app = FastAPI(title="카드 혜택 챗봇", docs_url="/docs")

_settings = get_settings()
_llm: LlmClient = create_llm_client(_settings)

# 분류는 됐지만 아직 조회 경로가 없는 의도에 쓰는 안내.
# 지어낸 답을 내보내는 것보다 "아직 못 한다"고 말하는 편이 낫다.
_NOT_WIRED = "아직 조회 경로가 연결되지 않은 질문입니다."

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
def chat(request: ChatRequest) -> ChatResponse:
    intent = _llm.classify(request.question)

    if intent.name == IntentName.UNKNOWN:
        return ChatResponse(
            answer=_UNKNOWN_FOLLOW_UP,
            intent=intent.name,
            follow_up_question=_UNKNOWN_FOLLOW_UP,
        )

    context = _describe(intent)
    answer = _llm.compose(request.question, context)
    return ChatResponse(answer=answer.text, intent=intent.name, sources=answer.sources)


def _describe(intent) -> str:
    """조회 결과 자리에 임시로 넣는 문자열.

    조회·엔진 호출이 붙기 전까지 무엇을 알아들었는지만 보여준다.
    """
    parts = [_NOT_WIRED, f"의도: {intent.name}"]
    for label, value in (
        ("가맹점 표현", intent.merchant_text),
        ("카테고리 표현", intent.category_text),
        ("카드 표현", intent.card_text),
        ("기간 표현", intent.period_text),
        ("금액", intent.amount),
    ):
        if value is not None:
            parts.append(f"{label}: {value}")
    return "\n".join(parts)
