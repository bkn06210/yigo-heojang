"""답변 검증 테스트.

지키려는 것은 하나다 — **조회 결과에 없는 금액을 말한 답변은 나가지 않는다.**
문장이 자연스러워서 읽는 사람이 틀렸다는 것을 알 수 없기 때문이다.
"""

from chatbot.verifier import fallback_answer, verify

CONTEXT = """[결제 대상] 스타벅스 (카페)
[보유 카드별 혜택]
- ALL point 카드: 전월실적 36,600원 / 300,000원 필요 (미충족)
  · 커피전문점 포인트리 적립 - 1.2% 적립
- 신한카드 핏(Fit): 전월실적 537,200원 / 400,000원 필요 (충족)
  · 커피 Stamp 적립 - 5회마다 2,000원 적립 (건당 5,000원 이상)
"""


class TestNumbersMustExist:

    def test_조회_결과에_있는_금액은_통과한다(self):
        answer = "신한카드 핏(Fit)은 커피 5회마다 2,000원 적립이고 건당 5,000원 이상 결제해야 합니다."

        assert verify(answer, CONTEXT).ok

    def test_쉼표_표기가_달라도_같은_수로_본다(self):
        answer = "전월실적 300000원을 채우셔야 합니다."

        assert verify(answer, CONTEXT).ok

    def test_만원_단위_표기도_같은_수로_본다(self):
        answer = "전월실적 30만원을 채우셔야 합니다."

        assert verify(answer, CONTEXT).ok

    def test_없는_금액을_말하면_막는다(self):
        # 조회 결과에 없는 값이다. LLM 이 만들어 냈거나 계산한 것이다
        answer = "이번 달 4,800원을 아끼실 수 있습니다."

        verdict = verify(answer, CONTEXT)

        assert not verdict.ok
        assert "4,800원" in verdict.unsupported_numbers

    def test_합산한_금액도_막는다(self):
        # 36,600 + 537,200 을 더한 값. 계산은 엔진이 하고 LLM 은 옮겨 쓰기만 해야 한다
        answer = "두 카드의 전월실적 합계는 573,800원입니다."

        assert not verify(answer, CONTEXT).ok

    def test_세는_말은_검사하지_않는다(self):
        # "카드 2장"은 문장을 만들며 나오는 말이라 조회 결과에 숫자로 없을 수 있다.
        # 여기까지 막으면 정상 답변이 걸린다
        answer = "카드 2장 중 신한카드 핏(Fit)을 쓰시면 됩니다."

        assert verify(answer, CONTEXT).ok


class TestEdges:

    def test_빈_답변은_막는다(self):
        verdict = verify("   ", CONTEXT)

        assert not verdict.ok
        assert verdict.reason == "빈 답변"

    def test_대조할_조회_결과가_없으면_통과한다(self):
        # 되묻기 흐름처럼 근거가 없는 경우다. 막을 기준이 없다
        assert verify("어느 가맹점인지 알려주시겠어요?", "").ok

    def test_폴백은_조회_결과를_그대로_보여준다(self):
        text = fallback_answer(CONTEXT)

        # 지어낸 문장 대신 투박한 사실을 보여준다
        assert "스타벅스" in text
        assert "2,000원" in text

    def test_조회_결과도_없으면_안내만_한다(self):
        assert "다시" in fallback_answer("")
