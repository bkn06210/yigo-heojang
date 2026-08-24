"""분류 프롬프트에 예시로 적어 둔 질문이 stub 에서도 도는지.

stub 은 낱말을 글자 그대로 맞춰볼 뿐이라 "스벅 가는데 뭐 쓸까"처럼 '카드'라는 말이
없는 질문을 놓친다. 개발 중에는 stub 으로 돌리므로, 대표 질문이 UNKNOWN 으로 빠지면
멀쩡한 기능을 고장 난 것으로 오해하게 된다.

프롬프트(진짜 모델용)와 stub 규칙은 따로 관리되는 두 벌이라 한쪽만 고쳐지기 쉽다.
사람이 기억하지 말고 테스트가 잡게 한다.

이 테스트가 보장하지 않는 것: stub 이 실제 사용자의 아무 말이나 알아듣는다는 것.
그건 규칙 기반으로 불가능하고, 그래서 시연은 실제 모델로 한다.
"""

import pytest

from chatbot.llm import IntentName
from chatbot.llm.stub import StubLlmClient


@pytest.fixture(scope="module")
def stub():
    return StubLlmClient()


# 분류 프롬프트의 RECOMMEND_CARD 예시. '카드'라는 낱말이 없는 것이 오히려 자연스럽다
@pytest.mark.parametrize("question", [
    "스벅에서 5000원 쓸건데 뭐가 좋아",
    "배민으로 2만원 시킬건데",
    "올영에서 3만원 결제",
    "하나로마트에서 3만원",
    "커피 마시려는데 뭐로 긁을까",
    "지하철 탈 건데",
    "스벅 가는데 뭐 쓸까",
])
def test_결제_직전_질문은_카드_추천으로_분류된다(stub, question):
    assert stub.classify(question).name == IntentName.RECOMMEND_CARD


# 새로 넣은 규칙이 다른 의도를 가로채면 안 된다. 업종·금액은 어느 질문에나 섞여 나온다
@pytest.mark.parametrize("question,expected", [
    ("이번 달 커피에 얼마 썼어", IntentName.BENEFIT_SUM),
    ("편의점 실적 채웠어?", IntentName.CARD_STATUS),
    ("연회비 3만원 언제 빠져나가?", IntentName.TERM_QA),
    ("내 소비에 맞는 카드 추천해줘", IntentName.RECOMMEND_NEW_CARD),
])
def test_업종_금액이_섞여도_원래_의도를_지킨다(stub, question, expected):
    assert stub.classify(question).name == expected
