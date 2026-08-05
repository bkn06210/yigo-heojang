"""챗봇 API 기본 동작.

LLM 을 부르지 않는 stub 으로 돌므로 테스트에 비용도 네트워크도 들지 않는다.
어댑터를 분리한 덕에 가능한 일이고, 이게 분리의 실질적인 값이다.
"""

from fastapi.testclient import TestClient

from chatbot.llm import IntentName
from chatbot.main import app

client = TestClient(app)


def _ask(question: str) -> dict:
    response = client.post("/chat", json={"memberId": 1, "question": question})
    assert response.status_code == 200
    return response.json()


def test_헬스체크는_현재_LLM_제공자를_알려준다():
    body = client.get("/health").json()

    assert body["status"] == "UP"
    # stub 인 채로 시연에 들어가는 사고를 이 필드로 잡는다.
    assert body["llmProvider"] == "stub"


def test_질문_유형별로_의도가_갈린다():
    assert _ask("이번 달 스벅에서 얼마 아꼈어?")["intent"] == IntentName.BENEFIT_SUM
    assert _ask("실적 채웠어?")["intent"] == IntentName.CARD_STATUS
    assert _ask("어느 카드로 결제할까?")["intent"] == IntentName.RECOMMEND_CARD
    assert _ask("카드 잃어버리면 어떻게 해?")["intent"] == IntentName.TERM_QA


def test_못_알아들으면_지어내지_않고_되묻는다():
    body = _ask("오늘 날씨 어때?")

    assert body["intent"] == IntentName.UNKNOWN
    assert body["followUpQuestion"] is not None


def test_엔티티는_사용자가_말한_표현_그대로_넘어온다():
    # 이름을 맞추는 일은 서버가 별칭 조회로 한다. LLM 이 '스벅'을 '스타벅스'로
    # 고쳐버리면 서버가 원래 표현을 잃는다.
    body = _ask("이번 달 스벅에서 얼마 아꼈어?")

    assert "스벅" in body["answer"]


def test_금액은_원_단위_정수로_해석된다():
    assert "30000" in _ask("3만원 결제할건데 어느 카드가 좋아?")["answer"]


def test_빈_질문은_거절한다():
    response = client.post("/chat", json={"memberId": 1, "question": ""})

    assert response.status_code == 422
