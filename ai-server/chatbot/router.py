"""의도별로 필요한 값을 모아 컨텍스트 문자열을 만든다.

여기서 숫자를 전부 완성해 넘긴다. 천 단위 쉼표까지 찍어서 준다.
LLM 에게 계산은 물론 자릿수 맞추기도 시키지 않는다 — 화면 숫자와 답변 숫자가
어긋나는 원인이 되고, 어긋나면 어느 쪽이 맞는지 사용자가 알 수 없다.

조회에 실패하거나 대상을 못 찾으면 지어내지 않고 되묻는다.
"""

from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from typing import Any, Dict, List, Optional, Tuple

from . import terms
from .engine import EngineClient, EngineError
from .llm import Intent, IntentName
from .resolver import Resolution, resolve_card, resolve_category, resolve_merchant


@dataclass(frozen=True)
class RouteResult:
    """조회 결과. 답변 문장으로 바꾸는 일은 이 다음 단계다."""

    context: Optional[str] = None
    follow_up: Optional[str] = None
    sources: List[str] = field(default_factory=list)


# 혜택 종류·계산방식을 사람 말로.
_KIND_LABELS = {
    "DISCOUNT": "할인",
    "POINT": "적립",
    "SPECIAL_PRICE": "특가",
}

# 현황 답변에 나열할 혜택 수. 넘으면 "외 N건"으로 줄인다.
#
# 스탬프 혜택은 브랜드마다 행이 따로라(편의점 4사·커피 6사) 카드 한 장에 열댓 개가 나온다.
# 그대로 넣으면 프롬프트가 부풀고 답변도 목록 낭독이 된다. "실적 채웠어?"라는 질문에
# 혜택 전체가 필요하지도 않다.
#
# 자른 사실은 밝힌다. 조용히 줄이면 그게 전부인 것처럼 읽힌다.
# 근본 해법은 엔진이 count_group_code 로도 묶어 내려주는 것이다(홈 화면도 같은 문제를 겪는다).
_MAX_BENEFITS_IN_ANSWER = 5

# 리포트 답변에 나열할 부문 수. 상위 몇 개면 "어디서 많이 받았나"에 답이 된다.
_MAX_CATEGORIES_IN_ANSWER = 3

# 추천 카드를 몇 장까지 말할지. 더 넣으면 답변이 목록 낭독이 된다
_MAX_CARDS_IN_ANSWER = 3

# 약관에서 무엇을 찾을지 알아내지 못했을 때. 질문을 그대로 검색어로 쓰지 않는다 —
# 일상어와 약관의 낱말이 달라 무관한 조항이 낮은 점수로 걸리고, 그걸 근거로
# 그럴듯한 답이 만들어지면 사용자가 틀렸다는 것을 알 방법이 없다.
_TERM_QUERY_MISSING = "약관에서 어떤 내용을 찾아드릴지 조금 더 구체적으로 말씀해 주세요."

# 찾았지만 쓸 만한 조항이 없을 때. 지어내지 않고 없다고 말한다.
_TERM_NOT_FOUND = (
    "등록된 약관에서 관련 조항을 찾지 못했습니다. 다른 표현으로 물어봐 주세요."
)


def route(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    if intent.name == IntentName.CARD_STATUS:
        return _card_status(intent, conn, engine)
    if intent.name == IntentName.RECOMMEND_CARD:
        return _recommend(intent, conn, engine)
    if intent.name == IntentName.RECOMMEND_NEW_CARD:
        return _recommend_new_card(engine)
    if intent.name == IntentName.BENEFIT_SUM:
        return _benefit_sum(intent, conn, engine)
    if intent.name == IntentName.TERM_QA:
        return _term_qa(intent, conn)
    return RouteResult()


# ── 소비 기반 카드 추천 ────────────────────────────────────


def _recommend_new_card(engine: EngineClient) -> RouteResult:
    """"내 소비에 맞는 카드 추천해줘" — 지금 카드에 무엇을 더하면 좋아지는가.

    금액은 전부 엔진이 계산한 값을 옮긴다. 순증은 지금 카드로 받는 금액과의 **차액**이라
    여기서 다시 더하거나 지금 금액과 합쳐 말하면 실제와 달라진다.

    전제를 문맥에 함께 넣는다 — 이 계산은 "그 카드를 주로 쓴다면"을 가정한 값이라,
    밝히지 않으면 "카드만 만들면 받는 돈"으로 읽힌다.
    """
    try:
        data = engine.card_recommendations()
    except EngineError as error:
        return RouteResult(context=f"[카드 추천]\n조회하지 못했습니다. ({error})")

    items = data.get("items") or []
    period = data.get("baseYearMonth") or "지난달"
    if not items:
        return RouteResult(
            context=(f"[카드 추천] {period} 소비 기준\n"
                     "지금 쓰시는 카드보다 나은 카드를 찾지 못했습니다."),
            sources=["소비 기반 카드 추천"],
        )

    lines = [
        f"[카드 추천] {period} 소비 기준",
        f"[지금 카드로 받는 혜택] 월 {_won(data.get('currentMonthlyBenefitAmount'))}",
        "[전제] 아래 금액은 그 카드를 주로 쓴다고 볼 때의 값입니다",
        "[추천 카드]",
    ]
    for item in items[:_MAX_CARDS_IN_ANSWER]:
        line = (f"- {item.get('cardCompanyName')} {item.get('cardName')}:"
                f" 월 {_won(item.get('monthlyGainAmount'))} 더 받음")
        annual_fee = item.get("annualFee") or 0
        if annual_fee > 0:
            line += (f" (최저 연회비 {_won(annual_fee)},"
                     f" {item.get('breakEvenMonths')}개월이면 연회비를 넘어섬)")
        else:
            line += " (연회비 없음)"
        lines.append(line)

    hidden = len(items) - len(items[:_MAX_CARDS_IN_ANSWER])
    if hidden > 0:
        lines.append(f"- 외 {hidden}장")

    return RouteResult(context="\n".join(lines), sources=["소비 기반 카드 추천"])


# ── 실적·한도 현황 ─────────────────────────────────────────


def _card_status(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    card = resolve_card(conn, intent.card_text) if intent.card_text else Resolution()
    if intent.card_text and not card.found:
        return RouteResult(follow_up=_ask_again("카드", intent.card_text, card))

    period = _to_period(intent.period_text)
    if not period.supported:
        return RouteResult(follow_up=_period_not_supported(intent.period_text))

    year_month = period.year_month
    try:
        data = engine.monthly_status(year_month)
    except EngineError as error:
        return RouteResult(follow_up=_engine_failed(error))

    cards = data.get("cards") or []
    if card.found:
        cards = [row for row in cards if row.get("cardName") == card.match.name]
        if not cards:
            return RouteResult(follow_up=f"{card.match.name}은(는) 보유 카드에 없습니다.")
    if not cards:
        return RouteResult(context="[보유 카드 현황]\n보유한 카드가 없습니다.")

    lines = [f"[보유 카드 현황] {cards[0].get('yearMonth') or year_month or '이번 달'}"]
    lines.extend(_status_line(row) for row in cards)
    return RouteResult(context="\n".join(lines), sources=["보유 카드 월별 현황"])


def _status_line(row: Dict[str, Any]) -> str:
    parts = [
        f"- {row.get('cardName')}: "
        f"실적 {_won(row.get('currentPerformanceAmount'))} / {_won(row.get('targetPerformance'))}"
        f" ({row.get('achievementRate')}%)"
        f", 남은 실적 {_won(row.get('remainingPerformance'))}"
        f", 전월 실적 조건 {'충족' if row.get('performanceMet') else '미충족'}"
    ]

    shared_limit = row.get("sharedLimit")
    if shared_limit is None:
        # null 은 "통합한도가 없는 카드"다. 0(혜택 없음)과 뜻이 다르므로 뭉개지 않는다.
        parts.append("  통합 한도: 없음 (혜택별 개별 한도만 적용)")
    else:
        used = row.get("sharedLimitUsed") or 0
        parts.append(
            f"  통합 한도 {_won(shared_limit)} 중 {_won(used)} 사용"
            f" (잔여 {_won(shared_limit - used)})"
        )

    parts.append("  지금 쓸 수 있는 혜택: " + _benefits_text(row.get("benefitsSummary") or []))
    return "\n".join(parts)


def _benefits_text(summaries: List[Dict[str, Any]]) -> str:
    if not summaries:
        return "없음"

    shown = [_benefit_text(benefit) for benefit in summaries[:_MAX_BENEFITS_IN_ANSWER]]
    hidden = len(summaries) - len(shown)
    if hidden > 0:
        shown.append(f"외 {hidden}건")
    return " / ".join(shown)


def _benefit_text(benefit: Dict[str, Any]) -> str:
    remaining = benefit.get("remainingLimit")
    # null 은 "한도 제약이 없다"는 뜻이지 "다 썼다"가 아니다.
    suffix = "한도 없음" if remaining is None else f"잔여 {_won(remaining)}"
    return f"{benefit.get('benefitName')} ({suffix})"


# ── 받은 혜택 (리포트) ─────────────────────────────────────


def _benefit_sum(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    period = _to_period(intent.period_text)
    if not period.supported:
        return RouteResult(follow_up=_period_not_supported(intent.period_text))

    year_month = period.year_month
    try:
        data = engine.benefit_report(year_month)
    except EngineError as error:
        return RouteResult(follow_up=_engine_failed(error))

    period = data.get("yearMonth") or year_month or "이번 달"
    categories = data.get("categories") or []

    # 가맹점을 짚어 물었으면("스벅에서 얼마 아꼈어?") 그 가맹점 거래만 추린다.
    if intent.merchant_text:
        merchant = resolve_merchant(conn, intent.merchant_text)
        if not merchant.found:
            return RouteResult(follow_up=_ask_again("가맹점", intent.merchant_text, merchant))
        return _merchant_benefit(period, merchant, categories)

    if not categories:
        return RouteResult(
            context=f"[받은 혜택] {period}\n받은 혜택이 없습니다.",
            sources=["혜택 리포트"],
        )

    lines = [
        f"[받은 혜택] {period} 총 {_won(data.get('totalBenefitAmount'))}",
        f"[가장 많이 받은 부문] {data.get('topCategoryName')}"
        f" {_won(data.get('topCategoryBenefitAmount'))}",
        "[부문별]",
    ]
    shown = categories[:_MAX_CATEGORIES_IN_ANSWER]
    lines.extend(
        f"- {category.get('categoryName')} {_won(category.get('benefitAmount'))}"
        f" ({len(category.get('details') or [])}건)"
        for category in shown
    )
    hidden = len(categories) - len(shown)
    if hidden > 0:
        lines.append(f"- 외 {hidden}개 부문")

    return RouteResult(context="\n".join(lines), sources=["혜택 리포트"])


def _merchant_benefit(period: str, merchant: Resolution, categories: List[Dict[str, Any]]) -> RouteResult:
    name = merchant.match.name
    details = [
        detail
        for category in categories
        for detail in (category.get("details") or [])
        if detail.get("merchantName") == name
    ]
    if not details:
        return RouteResult(
            context=f"[받은 혜택] {period} · {name}\n이 가맹점에서 받은 혜택이 없습니다.",
            sources=["혜택 리포트"],
        )

    total = sum(detail.get("benefitAmount") or 0 for detail in details)
    lines = [f"[받은 혜택] {period} · {name} 총 {_won(total)} ({len(details)}건)"]
    lines.extend(
        f"- {detail.get('paymentDate', '')[:10]} {_won(detail.get('paymentAmount'))} 결제"
        f" → {_won(detail.get('benefitAmount'))} ({detail.get('benefitName')}, {detail.get('cardName')})"
        for detail in details
    )
    return RouteResult(context="\n".join(lines), sources=["혜택 리포트"])


# ── 결제 직전 추천 ─────────────────────────────────────────


def _recommend(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    # 브랜드를 말했으면 그쪽이 우선이다. 업종만 말했으면("커피 마시려는데") 업종으로 계산한다 —
    # 엔진은 가맹점 없이 업종만으로도 계산할 수 있고, 그때는 업종·전체 혜택까지만 본다.
    place, kind = _resolve_place(intent, conn)
    if not place.found:
        text = intent.merchant_text or intent.category_text
        if not text:
            # 장소를 아예 말하지 않은 경우다. 못 찾았다고 하면 말한 적 없는 것을 되묻는 꼴이 된다.
            return RouteResult(follow_up="어디에서 결제하실 예정인가요?")
        return RouteResult(follow_up=_ask_again(kind, text, place))

    ids = {"merchant_id" if kind == "가맹점" else "category_id": place.match.target_id}

    # 금액이 없으면 되묻지 않는다. "여기서 뭐가 좋아?"는 금액이 없는 것이 정상인 질문이고,
    # 금액을 정해 묻는 사람은 결제 화면에서 추천을 받는다. 챗봇에 오는 질문은 대개 앞쪽이라
    # 답도 순위가 아니라 혜택의 조건이어야 한다.
    if not intent.amount:
        return _benefit_lookup(place, engine, ids)

    try:
        data = engine.recommend(intent.amount, **ids)
    except EngineError as error:
        return RouteResult(follow_up=_engine_failed(error))

    recommendations = data.get("recommendations") or []
    if not recommendations:
        return RouteResult(context="[추천 결과]\n계산할 보유 카드가 없습니다.")

    # 어디로 봤는지 반드시 남긴다. 이마트(대형마트)와 이마트24(편의점)처럼
    # 이름이 비슷하고 혜택이 전혀 다른 곳이 있어, 잘못 잡혔으면 사용자가 바로 알아야 한다.
    lines = [
        f"[결제 예정] {_where(place)} {_won(intent.amount)}",
        "[카드별 예상 혜택] 한 결제에 적용되는 혜택은 카드당 1개입니다.",
    ]
    lines.extend(_recommendation_line(row) for row in recommendations)

    point_guide = data.get("pointGuide")
    if point_guide and point_guide.get("message"):
        lines.append(f"[보유 포인트] {point_guide['message']}")

    return RouteResult(context="\n".join(lines), sources=["결제 직전 카드 추천"])


# ── 혜택 구조 안내 (금액 없는 질문) ────────────────────────


def _benefit_lookup(place: Resolution, engine: EngineClient, ids: Dict[str, int]) -> RouteResult:
    try:
        data = engine.applicable_benefits(**ids)
    except EngineError as error:
        return RouteResult(follow_up=_engine_failed(error))

    cards = data.get("cards") or []
    if not cards:
        return RouteResult(context="[혜택 조회]\n보유한 카드가 없습니다.")

    with_benefit = [card for card in cards if card.get("benefits")]
    without_benefit = [card.get("cardName") for card in cards if not card.get("benefits")]

    lines = [f"[결제 대상] {_where(place)}"]
    if with_benefit:
        lines.append("[보유 카드별 혜택]")
        for card in with_benefit:
            lines.append(_benefit_card_block(card))
    else:
        lines.append("[보유 카드별 혜택] 이 대상에 걸린 혜택이 있는 카드가 없습니다.")
    if without_benefit:
        lines.append("[혜택 없는 카드] " + ", ".join(without_benefit))

    return RouteResult(context="\n".join(lines), sources=["가맹점별 카드 혜택 조회"])


def _benefit_card_block(card: Dict[str, Any]) -> str:
    lines = [f"- {card.get('cardName')}: {_performance_text(card)}"]
    lines.extend(f"  · {_benefit_detail(benefit)}" for benefit in card.get("benefits") or [])
    return "\n".join(lines)


def _performance_text(card: Dict[str, Any]) -> str:
    required = card.get("requiredPerformanceAmount")
    if required is None:
        return "실적 조건 없는 카드"
    current = _won(card.get("prevPerformanceAmount"))
    met = "충족" if card.get("performanceMet") else "미충족"
    return f"전월실적 {current} / {_won(required)} 필요 ({met})"


def _benefit_detail(benefit: Dict[str, Any]) -> str:
    kind = _KIND_LABELS.get(benefit.get("benefitKind"), benefit.get("benefitKind"))
    value = benefit.get("benefitValue")
    calc_method = benefit.get("calcMethod")

    if calc_method == "RATE":
        amount_text = f"{_trim(value)}% {kind}"
    elif calc_method == "COUNT_STEP":
        amount_text = f"{benefit.get('stepCount')}회마다 {_won(value)} {kind}"
    else:
        amount_text = f"{_won(value)} {kind}"

    conditions = []
    if benefit.get("minTxnAmount"):
        conditions.append(f"건당 {_won(benefit['minTxnAmount'])} 이상")
    monthly_limit = benefit.get("monthlyLimit")
    if monthly_limit is not None:
        # null 은 "한도 제약 없음"이라 조건에 적지 않는다. 0 은 혜택 없음이라 뜻이 다르다.
        conditions.append(f"월 {_won(monthly_limit)} 한도")
    if not benefit.get("available"):
        conditions.append(f"{benefit.get('unavailableReason')}로 지금은 적용 안 됨")

    detail = f"{benefit.get('benefitName')} — {amount_text}"
    return detail + (f" ({', '.join(conditions)})" if conditions else "")


def _trim(value) -> str:
    """10.00 을 10 으로. 소수점 자리가 의미 없을 때 붙어 있으면 읽기 나쁘다."""
    text = str(value)
    return text.rstrip("0").rstrip(".") if "." in text else text


def _where(place: Resolution) -> str:
    name = place.match.name
    return f"{name} ({place.match.detail})" if place.match.detail else name


def _resolve_place(intent: Intent, conn) -> Tuple[Resolution, str]:
    """결제할 곳. 브랜드가 먼저이고, 없으면 업종으로 본다."""
    if intent.merchant_text:
        return resolve_merchant(conn, intent.merchant_text), "가맹점"
    return resolve_category(conn, intent.category_text), "업종"


def _recommendation_line(row: Dict[str, Any]) -> str:
    estimate = " (예상)" if row.get("isEstimate") else ""
    return (
        f"{row.get('rank')}위 {row.get('cardName')} — "
        f"{_won(row.get('expectedBenefit'))}{estimate} · {row.get('reason')}"
    )


# ── 약관 질문 ──────────────────────────────────────────────


def _term_qa(intent: Intent, conn) -> RouteResult:
    """약관 원문에서 관련 조항을 찾아 컨텍스트로 만든다.

    다른 의도와 달리 엔진을 부르지 않는다. 약관은 계산 규칙이 아니라 텍스트라
    두 곳에서 규칙이 갈릴 위험이 없고, 엔진이 내려줄 수 있는 모양도 아니다.

    찾은 조항을 그대로 싣고 요약하지 않는다. 줄이는 순간 그 요약이 맞는지
    아무도 확인할 수 없게 되는데, 약관은 단서 한 줄이 결론을 뒤집는다.
    """
    if not intent.term_query:
        return RouteResult(follow_up=_TERM_QUERY_MISSING)

    card = resolve_card(conn, intent.card_text) if intent.card_text else Resolution()
    if intent.card_text and not card.found:
        return RouteResult(follow_up=_ask_again("카드", intent.card_text, card))

    company_ids = None
    if card.found:
        company_id = terms.company_id_of_card(conn, card.match.target_id)
        # 카드사가 안 붙은 카드면 좁히지 않는다. 좁히면 결과가 0건이 되어
        # "약관이 없다"고 답하게 되는데, 실제로는 있다.
        company_ids = [company_id] if company_id else None

    passages = terms.search(conn, intent.term_query, company_ids)
    if not passages:
        return RouteResult(follow_up=_TERM_NOT_FOUND)

    lines = ["[약관 조항]"]
    for passage in passages:
        lines.append(f"\n· {passage.label()}\n{passage.content}")

    return RouteResult(
        context="\n".join(lines),
        sources=[passage.label() for passage in passages],
    )


# ── 공통 ───────────────────────────────────────────────────


def _ask_again(label: str, text: Optional[str], resolution: Resolution) -> str:
    if resolution.candidates:
        options = ", ".join(resolution.candidates)
        return f"{label}이(가) 여럿입니다. 어느 쪽인가요? ({options})"
    return f"'{text}'이(가) 어느 {label}인지 찾지 못했습니다. 정확한 이름으로 다시 알려주세요."


def _engine_failed(error: EngineError) -> str:
    if error.status_code == 401:
        return "로그인 정보가 확인되지 않아 조회하지 못했습니다. 다시 로그인해 주세요."
    return "카드 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."


# 일·주 단위 기간 표현. 엔진 조회가 월 단위(yearMonth)라 이 표현들은 답할 수 없다.
#
# 조용히 이번 달로 바꾸면 "오늘 얼마 아꼈어?"에 한 달치 금액이 나간다. 사용자는 그 값이
# 오늘 것인 줄 알고, 틀린 답인지 알 방법이 없다. 못 하는 것은 못 한다고 말한다.
_SUB_MONTH_PERIODS = (
    "오늘", "금일", "어제", "그제", "그저께", "엊그제", "내일",
    "이번주", "금주", "지난주", "저번주", "이번한주",
)


@dataclass(frozen=True)
class Period:
    """기간 표현 해석 결과."""

    year_month: Optional[str] = None
    """조회할 연월(YYYY-MM). None 이면 이번 달."""

    supported: bool = True
    """월 단위로 답할 수 있는 기간인가. False 면 되묻는다."""


def _to_period(period_text: Optional[str]) -> Period:
    """기간 표현을 조회 연월로. 월 단위로 못 담는 표현은 supported=False 로 돌려준다."""
    if not period_text:
        return Period()

    normalized = period_text.replace(" ", "")
    if normalized in _SUB_MONTH_PERIODS:
        return Period(supported=False)
    if normalized in ("지난달", "저번달"):
        today = date.today()
        year, month = (today.year - 1, 12) if today.month == 1 else (today.year, today.month - 1)
        return Period(year_month=f"{year:04d}-{month:02d}")
    # 모르는 표현은 이번 달로 본다. "요즘"·"최근" 같은 말은 월 단위로 읽어도 어긋나지 않는다.
    return Period()


def _period_not_supported(period_text: str) -> str:
    return f"'{period_text}' 기준으로는 아직 조회할 수 없습니다. 이번 달·지난달처럼 월 단위로 물어봐 주세요."


def _won(amount) -> str:
    """금액 문자열. LLM 에게 자릿수 맞추기를 시키지 않으려고 여기서 완성한다.

    혜택값은 소수 자리를 가진 값이라 JSON 에서 문자열로 올 수 있다("2000.00").
    원 단위로 끊어 쓴다 — 카드 혜택은 원 미만을 버리고 지급한다.
    """
    if amount is None:
        return "0원"
    return f"{int(Decimal(str(amount))):,}원"
