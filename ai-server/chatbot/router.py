"""의도별로 필요한 값을 모아 컨텍스트 문자열을 만든다.

여기서 숫자를 전부 완성해 넘긴다. 천 단위 쉼표까지 찍어서 준다.
LLM 에게 계산은 물론 자릿수 맞추기도 시키지 않는다 — 화면 숫자와 답변 숫자가
어긋나는 원인이 되고, 어긋나면 어느 쪽이 맞는지 사용자가 알 수 없다.

조회에 실패하거나 대상을 못 찾으면 지어내지 않고 되묻는다.
"""

from dataclasses import dataclass, field
from datetime import date
from typing import Any, Dict, List, Optional, Tuple

from .engine import EngineClient, EngineError
from .llm import Intent, IntentName
from .resolver import Resolution, resolve_card, resolve_category, resolve_merchant


@dataclass(frozen=True)
class RouteResult:
    """조회 결과. 답변 문장으로 바꾸는 일은 이 다음 단계다."""

    context: Optional[str] = None
    follow_up: Optional[str] = None
    sources: List[str] = field(default_factory=list)


# 현황 답변에 나열할 혜택 수. 넘으면 "외 N건"으로 줄인다.
#
# 스탬프 혜택은 브랜드마다 행이 따로라(편의점 4사·커피 6사) 카드 한 장에 열댓 개가 나온다.
# 그대로 넣으면 프롬프트가 부풀고 답변도 목록 낭독이 된다. "실적 채웠어?"라는 질문에
# 혜택 전체가 필요하지도 않다.
#
# 자른 사실은 밝힌다. 조용히 줄이면 그게 전부인 것처럼 읽힌다.
# 근본 해법은 엔진이 count_group_code 로도 묶어 내려주는 것이다(홈 화면도 같은 문제를 겪는다).
_MAX_BENEFITS_IN_ANSWER = 5

# 아직 데이터 경로가 없는 의도. 못 하는 것을 못 한다고 말한다.
_NOT_READY = {
    IntentName.BENEFIT_SUM: "받은 혜택 금액은 소비내역 조회가 연결된 뒤에 답할 수 있습니다.",
    IntentName.TERM_QA: "약관 원문이 아직 등록되지 않아 약관 질문에는 답할 수 없습니다.",
}


def route(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    if intent.name == IntentName.CARD_STATUS:
        return _card_status(intent, conn, engine)
    if intent.name == IntentName.RECOMMEND_CARD:
        return _recommend(intent, conn, engine)

    message = _NOT_READY.get(intent.name)
    if message:
        return RouteResult(follow_up=message)
    return RouteResult()


# ── 실적·한도 현황 ─────────────────────────────────────────


def _card_status(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    card = resolve_card(conn, intent.card_text) if intent.card_text else Resolution()
    if intent.card_text and not card.found:
        return RouteResult(follow_up=_ask_again("카드", intent.card_text, card))

    year_month = _to_year_month(intent.period_text)
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


# ── 결제 직전 추천 ─────────────────────────────────────────


def _recommend(intent: Intent, conn, engine: EngineClient) -> RouteResult:
    # 브랜드를 말했으면 그쪽이 우선이다. 업종만 말했으면("커피 마시려는데") 업종으로 계산한다 —
    # 엔진은 가맹점 없이 업종만으로도 계산할 수 있고, 그때는 업종·전체 혜택까지만 본다.
    place, kind = _resolve_place(intent, conn)
    if not place.found:
        text = intent.merchant_text or intent.category_text
        return RouteResult(follow_up=_ask_again(kind, text, place))
    if not intent.amount:
        return RouteResult(follow_up=f"{place.match.name}에서 얼마를 결제하실 예정인가요?")

    ids = {"merchant_id" if kind == "가맹점" else "category_id": place.match.target_id}
    try:
        data = engine.recommend(intent.amount, **ids)
    except EngineError as error:
        return RouteResult(follow_up=_engine_failed(error))

    recommendations = data.get("recommendations") or []
    if not recommendations:
        return RouteResult(context="[추천 결과]\n계산할 보유 카드가 없습니다.")

    # 어디로 봤는지 반드시 남긴다. 이마트(대형마트)와 이마트24(편의점)처럼
    # 이름이 비슷하고 혜택이 전혀 다른 곳이 있어, 잘못 잡혔으면 사용자가 바로 알아야 한다.
    where = place.match.name
    if place.match.detail:
        where += f" ({place.match.detail})"
    lines = [
        f"[결제 예정] {where} {_won(intent.amount)}",
        "[카드별 예상 혜택] 한 결제에 적용되는 혜택은 카드당 1개입니다.",
    ]
    lines.extend(_recommendation_line(row) for row in recommendations)

    point_guide = data.get("pointGuide")
    if point_guide and point_guide.get("message"):
        lines.append(f"[보유 포인트] {point_guide['message']}")

    return RouteResult(context="\n".join(lines), sources=["결제 직전 카드 추천"])


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


def _to_year_month(period_text: Optional[str]) -> Optional[str]:
    """기간 표현을 YYYY-MM 으로. 모르는 표현이면 None(이번 달)."""
    if not period_text:
        return None
    if period_text.replace(" ", "") in ("지난달", "저번달"):
        today = date.today()
        year, month = (today.year - 1, 12) if today.month == 1 else (today.year, today.month - 1)
        return f"{year:04d}-{month:02d}"
    return None


def _won(amount: Optional[int]) -> str:
    """금액 문자열. LLM 에게 자릿수 맞추기를 시키지 않으려고 여기서 완성한다."""
    if amount is None:
        return "0원"
    return f"{amount:,}원"
