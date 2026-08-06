"""챗봇 API 동작.

LLM 은 stub 이라 호출도 비용도 없고, 엔진은 가짜로 갈아끼워 백엔드가 안 떠 있어도 돈다.
어댑터를 분리한 실질적인 값이 이것이다.

표현 해석은 진짜 DB 를 쓴다. 별칭 조회가 SQL 안에 있어 가짜로 바꾸면 정작 확인하려는
것을 못 본다. DB 가 없으면 그 테스트만 건너뛴다.
"""

import pytest
from fastapi.testclient import TestClient

from chatbot import main
from chatbot.db import connection
from chatbot.llm import IntentName

client = TestClient(main.app)


def _db_available() -> bool:
    try:
        with connection():
            return True
    except Exception:
        return False


needs_db = pytest.mark.skipif(not _db_available(), reason="MySQL 연결 불가")


class FakeEngine:
    """엔진 응답을 흉내 낸다. 형식은 API 명세와 같다."""

    def __init__(self, **_kwargs):
        pass

    def monthly_status(self, year_month=None):
        return {
            "cards": [
                {
                    "userCardId": 1,
                    "cardName": "신한카드 핏(Fit)",
                    "yearMonth": "2026-08",
                    "currentPerformanceAmount": 330400,
                    "targetPerformance": 400000,
                    "remainingPerformance": 69600,
                    "achievementRate": 82.6,
                    "performanceMet": True,
                    "sharedLimit": 20000,
                    "sharedLimitUsed": 8000,
                    "benefitsSummary": [
                        {"benefitId": 31, "benefitName": "편의점 10% 할인", "remainingLimit": 5000},
                        {"benefitId": 70, "benefitName": "전 가맹점 0.5% 적립", "remainingLimit": None},
                        # 스탬프 혜택은 브랜드마다 행이 따로라 실제로 열댓 개까지 늘어난다.
                        {"benefitId": 71, "benefitName": "커피 Stamp 적립 (스타벅스)", "remainingLimit": None},
                        {"benefitId": 72, "benefitName": "커피 Stamp 적립 (투썸플레이스)", "remainingLimit": None},
                        {"benefitId": 73, "benefitName": "커피 Stamp 적립 (커피빈)", "remainingLimit": None},
                        {"benefitId": 74, "benefitName": "커피 Stamp 적립 (이디야)", "remainingLimit": None},
                        {"benefitId": 75, "benefitName": "커피 Stamp 적립 (폴바셋)", "remainingLimit": None},
                    ],
                }
            ]
        }

    def benefit_report(self, year_month=None):
        return {
            "yearMonth": "2026-08",
            "totalBenefitAmount": 3500,
            "topCategoryId": 1,
            "topCategoryName": "외식",
            "topCategoryBenefitAmount": 3000,
            "categories": [
                {
                    "categoryId": 1, "categoryName": "외식", "benefitAmount": 3000,
                    "details": [{
                        "expenseId": 1, "merchantName": "스타벅스", "cardName": "ALL point 카드",
                        "paymentAmount": 10000, "benefitAmount": 2000,
                        "benefitName": "커피 10% 할인", "paymentDate": "2026-08-10T12:00:00",
                    }],
                },
                {
                    "categoryId": 2, "categoryName": "쇼핑", "benefitAmount": 500,
                    "details": [{
                        "expenseId": 3, "merchantName": "GS25", "cardName": "ALL point 카드",
                        "paymentAmount": 3000, "benefitAmount": 500,
                        "benefitName": "편의점 적립", "paymentDate": "2026-08-11T12:00:00",
                    }],
                },
            ],
        }

    def applicable_benefits(self, merchant_id=None, category_id=None):
        return {
            "cards": [
                {
                    "userCardId": 1,
                    "cardName": "ALL point 카드",
                    "prevPerformanceAmount": 36600,
                    "requiredPerformanceAmount": 300000,
                    "performanceMet": False,
                    "benefits": [{
                        "benefitId": 10, "benefitName": "커피전문점 포인트리 적립",
                        "benefitKind": "POINT", "calcMethod": "RATE", "benefitValue": "1.20",
                        "requirePerformance": True, "available": False,
                        "unavailableReason": "전월 실적 미달",
                        "minTxnAmount": None, "monthlyLimit": None, "stepCount": None,
                    }],
                },
                {
                    "userCardId": 2,
                    "cardName": "신한카드 핏(Fit)",
                    "prevPerformanceAmount": 537200,
                    "requiredPerformanceAmount": 400000,
                    "performanceMet": True,
                    "benefits": [{
                        "benefitId": 20, "benefitName": "커피 Stamp 적립 (스타벅스)",
                        "benefitKind": "POINT", "calcMethod": "COUNT_STEP", "benefitValue": "2000",
                        "requirePerformance": True, "available": True, "unavailableReason": None,
                        "minTxnAmount": 5000, "monthlyLimit": None, "stepCount": 5,
                    }],
                },
                {
                    "userCardId": 3, "cardName": "YOU Wish 카드",
                    "prevPerformanceAmount": 0, "requiredPerformanceAmount": None,
                    "performanceMet": False, "benefits": [],
                },
            ]
        }

    def recommend(self, expected_amount, merchant_id=None, category_id=None):
        self.last_call = {"merchantId": merchant_id, "categoryId": category_id}
        return {
            "recommendations": [
                {
                    "rank": 1,
                    "userCardId": 1,
                    "cardName": "신한카드 핏(Fit)",
                    "expectedBenefit": 800,
                    "isEstimate": True,
                    "reason": "카페 10% 할인 예상 800원",
                }
            ]
        }


@pytest.fixture
def fake_engine(monkeypatch):
    monkeypatch.setattr(main, "EngineClient", FakeEngine)


def _ask(question: str) -> dict:
    response = client.post("/chat", json={"memberId": 1, "question": question})
    assert response.status_code == 200
    return response.json()


def test_헬스체크는_현재_LLM_제공자를_알려준다():
    body = client.get("/health").json()

    assert body["status"] == "UP"
    # stub 인 채로 시연에 들어가는 사고를 이 필드로 잡는다.
    assert body["llmProvider"] == "stub"


def test_못_알아들으면_지어내지_않고_되묻는다():
    body = _ask("오늘 날씨 어때?")

    assert body["intent"] == IntentName.UNKNOWN
    assert body["followUpQuestion"] is not None


def test_빈_질문은_거절한다():
    response = client.post("/chat", json={"memberId": 1, "question": ""})

    assert response.status_code == 422


@needs_db
def test_현황_질문은_엔진이_준_숫자를_그대로_쓴다(fake_engine):
    body = _ask("실적 채웠어?")

    assert body["intent"] == IntentName.CARD_STATUS
    # 천 단위 쉼표까지 서버가 찍어서 넘긴다. LLM 에게 자릿수를 맞추게 하지 않는다.
    assert "330,400원" in body["answer"]
    assert "82.6%" in body["answer"]


@needs_db
def test_한도가_없는_혜택은_소진과_구분해_적는다(fake_engine):
    body = _ask("실적 채웠어?")

    # remainingLimit 이 null 인 것은 "제약 없음"이지 "다 씀"이 아니다.
    assert "전 가맹점 0.5% 적립 (한도 없음)" in body["answer"]
    assert "편의점 10% 할인 (잔여 5,000원)" in body["answer"]


@needs_db
def test_혜택이_많으면_줄이되_몇_건을_뺐는지_밝힌다(fake_engine):
    body = _ask("실적 채웠어?")

    # 7건 중 5건만 싣는다. 조용히 자르면 그게 전부인 것처럼 읽힌다.
    assert "외 2건" in body["answer"]
    assert "커피 Stamp 적립 (폴바셋)" not in body["answer"]


@needs_db
def test_추천은_어느_가맹점으로_봤는지_밝힌다(fake_engine):
    # 이마트(대형마트)와 이마트24(편의점)처럼 혜택이 전혀 다른 곳이 있어
    # 잘못 잡혔으면 사용자가 바로 알아채야 한다.
    body = _ask("스벅에서 8000원 결제할건데 어느 카드가 좋아?")

    assert body["intent"] == IntentName.RECOMMEND_CARD
    assert "스타벅스" in body["answer"]
    assert "카페" in body["answer"]
    assert "800원" in body["answer"]


@needs_db
def test_금액이_없으면_혜택_구조를_알려준다(fake_engine):
    # "여기서 뭐가 좋아?"는 금액이 없는 것이 정상인 질문이다. 금액을 정해 묻는 사람은
    # 결제 화면에서 추천을 받는다. 되묻지 말고 혜택의 조건으로 답한다.
    body = _ask("스벅에서 어느 카드가 좋아?")

    assert "스타벅스 (카페)" in body["answer"]
    assert "1.2% 적립" in body["answer"]
    assert "5회마다 2,000원 적립" in body["answer"]
    assert body["followUpQuestion"] is None


@needs_db
def test_실적_미달로_못_받는_혜택도_사유와_함께_알려준다(fake_engine):
    body = _ask("스벅에서 어느 카드가 좋아?")

    # "혜택이 없다"가 아니라 "채우면 받을 수 있다"로 답할 수 있어야 한다.
    assert "전월실적 36,600원 / 300,000원 필요 (미충족)" in body["answer"]
    assert "전월 실적 미달로 지금은 적용 안 됨" in body["answer"]


@needs_db
def test_혜택_없는_카드는_묶어서_알려준다(fake_engine):
    body = _ask("스벅에서 어느 카드가 좋아?")

    assert "[혜택 없는 카드] YOU Wish 카드" in body["answer"]


@needs_db
def test_되묻고_받은_답으로_대화가_이어진다(fake_engine):
    # 장소를 모르면 되묻고, 그 답을 직전 맥락과 합쳐 처리한다.
    first = _ask("5000원 결제할건데 어느 카드가 좋아?")
    assert first["pendingContext"]["intent"] == IntentName.RECOMMEND_CARD

    second = client.post("/chat", json={
        "memberId": 1,
        "question": "스벅",
        "pendingContext": first["pendingContext"],
    }).json()

    # "스벅"만으로는 무엇을 묻는지 알 수 없다. 직전 의도와 금액을 이어받아야 한다.
    assert second["intent"] == IntentName.RECOMMEND_CARD
    assert "스타벅스" in second["answer"]
    assert "5,000원" in second["answer"]
    assert second["followUpQuestion"] is None


@needs_db
def test_주제가_바뀌면_직전_맥락을_버린다(fake_engine):
    first = _ask("5000원 결제할건데 어느 카드가 좋아?")

    second = client.post("/chat", json={
        "memberId": 1,
        "question": "실적 채웠어?",
        "pendingContext": first["pendingContext"],
    }).json()

    # 앞의 '스벅'이 따라붙어 추천으로 새면 안 된다.
    assert second["intent"] == IntentName.CARD_STATUS
    assert "결제 예정" not in second["answer"]
    assert "보유 카드 현황" in second["answer"]


@needs_db
def test_결제할_곳을_모르면_지어내지_않고_되묻는다(fake_engine):
    body = _ask("5000원 결제할건데 어느 카드가 좋아?")

    assert body["intent"] == IntentName.RECOMMEND_CARD
    # 말한 적 없는 것을 "못 찾았다"고 하면 안 된다.
    assert body["followUpQuestion"] == "어디에서 결제하실 예정인가요?"
    assert "None" not in body["followUpQuestion"]


@needs_db
def test_업종만_말해도_추천한다(fake_engine):
    # 브랜드 없이 업종만 말하는 경우가 흔하다("커피 마시려는데"). 엔진은 가맹점 없이
    # 업종만으로도 계산할 수 있으므로 되묻지 않는다.
    body = _ask("커피 마시려는데 5000원으로 어느 카드가 좋아?")

    assert body["intent"] == IntentName.RECOMMEND_CARD
    assert "카페" in body["answer"]
    assert body["followUpQuestion"] is None


@needs_db
def test_받은_혜택은_총액과_최대_부문을_알려준다(fake_engine):
    body = _ask("이번 달 얼마 아꼈어?")

    assert body["intent"] == IntentName.BENEFIT_SUM
    assert "총 3,500원" in body["answer"]
    assert "외식 3,000원" in body["answer"]


@needs_db
def test_가맹점을_짚어_물으면_그_가맹점_거래만_합산한다(fake_engine):
    body = _ask("스벅에서 얼마 아꼈어?")

    assert body["intent"] == IntentName.BENEFIT_SUM
    assert "스타벅스 총 2,000원" in body["answer"]
    # 다른 가맹점 거래가 섞이면 안 된다.
    assert "GS25" not in body["answer"]


@needs_db
def test_약관_질문은_아직_못_한다고_말한다(fake_engine):
    body = _ask("카드 잃어버리면 어떻게 해?")

    assert body["intent"] == IntentName.TERM_QA
    assert "약관 원문" in body["answer"]
