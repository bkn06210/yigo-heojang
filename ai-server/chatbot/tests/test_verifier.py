"""답변 검증 테스트.

지키려는 것은 하나다 — **조회 결과에 없는 금액을 말한 답변은 나가지 않는다.**
문장이 자연스러워서 읽는 사람이 틀렸다는 것을 알 수 없기 때문이다.
"""

import pytest

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


class TestKoreanNumerals:
    """같은 값을 "15,000원"·"1만 5천원"·"만오천 원"으로 쓸 수 있다.

    숫자 표기만 검사하면 LLM 이 표기만 바꿔도 없는 금액이 그대로 나간다.
    """

    @pytest.mark.parametrize("amount", [
        "4천원",          # 천
        "2천5백원",       # 천·백 혼합
        "4천 5백원",      # 천·백 사이 띄어쓰기
        "1만 5천원",      # 만 + 천, 띄어 씀
        "1만5천원",       # 만 + 천, 붙여 씀
        "1.5만원",        # 소수 + 만
        "1억 2천만원",    # 억 + 천만
        "사천 원",        # 한글 숫자
        "이천오백 원",    # 한글 숫자 혼합
        "일만 오천 원",   # 한글 숫자 + 만
    ])
    def test_한국어_수사로_쓴_없는_금액도_막는다(self, amount):
        # 모두 조회 결과에 없는 값이다. 숫자로 쓴 "15,000원"만 막으면 표기만 바꿔 빠져나간다
        verdict = verify(f"이번 달 {amount}을 아끼실 수 있습니다.", CONTEXT)

        assert not verdict.ok
        assert amount in verdict.unsupported_numbers

    @pytest.mark.parametrize("amount", [
        "5천원",          # 5,000원
        "5천 원",
        "오천 원",
        "이천 원",        # 2,000원
        "30만 원",        # 300,000원
        "삼십만 원",
        "53만 7,200원",   # 537,200원 — 만 뒤에 띄어 쓴 금액도 한 금액으로 잇는다
        "53만7200원",
        "3만6천6백원",    # 36,600원
    ])
    def test_조회_결과에_있는_값은_한국어_수사로_말해도_통과한다(self, amount):
        assert verify(f"기준 금액은 {amount}입니다.", CONTEXT).ok

    def test_조회_결과가_한국어_수사로_적혀_있어도_대조한다(self):
        # 약관 원문은 "월 5천원 한도"처럼 적혀 있다. 조회 결과를 숫자 표기로만 읽으면
        # 5천원을 5로 읽어, 값을 그대로 옮긴 "5,000원"이 막힌다
        context = "[약관 조항]\n· 커피 할인\n1만원 이상 결제 시 10% 할인, 월 5천원 한도"

        assert verify("1만 원 이상 결제하시면 10% 할인되고 월 5,000원까지입니다.", context).ok
        assert not verify("월 6천원까지 할인됩니다.", context).ok

    def test_소수와_만_단위를_섞어도_정확히_비교한다(self):
        # 실수 계산이면 1.12만이 11200.000000000002 가 되어 11,200 과 어긋난다
        assert verify("1.12만원 할인됩니다.", "[추천 결과]\n1위 핏 — 11,200원").ok

    def test_소수_자릿수가_달라도_같은_수로_본다(self):
        assert verify("커피전문점에서 1.20% 적립됩니다.", CONTEXT).ok


class TestUnitKinds:
    """값이 같아도 단위가 다르면 다른 사실이다."""

    @pytest.mark.parametrize("answer", [
        "8,000회 결제하시면 됩니다.",
        "8,000건까지 적립됩니다.",
        "8,000% 할인됩니다.",
    ])
    def test_값이_같아도_단위_종류가_다르면_막는다(self, answer):
        # 조회 결과에 8,000원이 있다고 "8,000회"가 근거를 갖는 것은 아니다
        context = "[결제 예정] 스타벅스 (카페) 8,000원"

        verdict = verify(answer, context)

        assert not verdict.ok

    def test_포인트와_점은_원과_같은_금액으로_본다(self):
        # 적립 혜택도 조회 결과에는 원으로 적어 넘긴다. 이걸 포인트로 옮겨 말하는 건 정상이다
        assert verify("5회마다 2,000포인트, 즉 2천점이 적립됩니다.", CONTEXT).ok

    def test_회와_건은_같은_횟수로_본다(self):
        assert verify("5건 결제할 때마다 2,000원이 적립됩니다.", CONTEXT).ok

    def test_퍼센트를_글자로_써도_같은_비율로_본다(self):
        assert verify("1.2퍼센트 적립됩니다.", CONTEXT).ok

    def test_단위를_알_수_없는_조회_값은_어느_단위로_말해도_인정한다(self):
        # 표에는 단위를 머리글에만 적고 칸에는 수만 둔다. 칸의 수를 한 단위로 정해 버리면
        # 표를 그대로 옮겨 말한 답변이 막힌다
        context = "[약관 조항]\n· 할인 서비스\n구분 | 할인율(%) | 월 한도(원)\n커피 | 10 | 5,000"

        assert verify("커피는 10% 할인되고 월 5,000원까지입니다.", context).ok


class TestNoFalseAlarms:
    """수처럼 보이지만 수가 아닌 말. 여기서 막히기 시작하면 사람이 검사를 끈다."""

    @pytest.mark.parametrize("answer", [
        "혜택 만점인 카드입니다. 5회마다 2,000원이 적립됩니다.",   # 만점 ≠ 1만 점
        "전국 3천 점포에서 5회마다 적립됩니다.",                    # 점포 수
        "1천만 회원이 쓰는 카드로, 5회마다 적립됩니다.",            # 회원 수
        "회사원도 일회용 컵 사건 없이 5회마다 적립됩니다.",         # 사원·일회·사건
        "만일 원하시면 5회마다 적립되는 핏을 쓰세요.",              # 만일 ≠ 10,001
        "전월실적 30만 넘기면 5회마다 적립됩니다.",                 # 30만 5회 ≠ 300,005회
    ])
    def test_수가_아닌_말은_검사하지_않는다(self, answer):
        assert verify(answer, CONTEXT).ok

    def test_띄어_쓴_낱말을_앞_수에_붙여_읽지_않는다(self):
        # "30만 백화점"의 '백'을 100으로 읽으면 조회 결과의 30만이 300,100이 되어
        # 값을 그대로 옮긴 답변이 막힌다
        context = "[약관 조항]\n월 30만 백화점 이용 시 할인"

        assert verify("백화점에서 30만원 이상 쓰시면 할인됩니다.", context).ok


class TestUnsupportedNotations:
    """읽지 않기로 한 표기. 수로 읽으면 멀쩡한 낱말까지 걸려서 검사하지 않고 통과시킨다.

    조회 결과에 없는 값이어도 통과한다는 것을 알고 쓰려고 적어 둔다.
    """

    @pytest.mark.parametrize("answer", [
        "만 원 이상 결제하시면 됩니다.",     # 앞 수 생략 한 글자 — "만점"·"백점"과 모양이 같다
        "천 원 할인됩니다.",
        "월이천원 할인됩니다.",              # 앞 글자에 붙은 한글 숫자 — "회사원"의 '사원'과 같은 모양
        "이천오 원 할인됩니다.",             # 한글 숫자로 끝나는 일의 자리 — "오천이 원래"의 조사와 겹친다
        "5조원 한도입니다.",                 # 조 — "조건"과 겹친다
    ])
    def test_읽지_않는_표기는_검사하지_않는다(self, answer):
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
