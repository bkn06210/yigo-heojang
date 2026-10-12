"""답변 검증 — LLM 이 낸 문장을 내보내기 전에 한 번 거른다.

이 서버의 원칙은 "계산은 엔진, LLM 은 표현"인데, **지킨 것을 확인하는 층이 없었다.**
프롬프트로 "계산하지 마라"라고 적어 두는 것은 지시일 뿐 보장이 아니다.

가장 위험한 실패는 **없는 금액을 말하는 것**이다. 문장이 자연스러워서 읽는 사람이
틀렸다는 것을 알 방법이 없고, 그 숫자로 카드를 고르게 된다.

그래서 확인한다: **답변에 나온 금액·횟수가 조회 결과에 실재하는가.**
조회 결과는 서버가 DB·엔진에서 받아 만든 문자열이라 여기 있는 숫자는 전부 근거가 있다.
거기 없는 숫자가 답변에 있으면 LLM 이 만들어 냈거나 계산한 것이다 — 둘 다 하면 안 되는 일이다.
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass, field
from decimal import Decimal
from typing import Dict, List, Optional, Set, Tuple

logger = logging.getLogger(__name__)

# 검사 대상은 단위가 붙은 수다. "카드 3장"처럼 세는 말까지 잡으면 오탐이 는다 —
# 세는 말은 조회 결과에 숫자로 안 적혀 있어도 문장을 만들며 나올 수 있다.
#
# 값만 같으면 통과시키지 않고 단위의 종류까지 맞춘다. 조회 결과의 8,000원을 근거로
# "8,000회"를 말하면 숫자는 실재해도 사실은 지어낸 것이다.
# 포인트·점은 원과 같은 금액으로 본다 — 적립 혜택도 조회 결과에는 원으로 적어 넘긴다.
_AMOUNT, _RATE, _COUNT = "AMOUNT", "RATE", "COUNT"
_UNIT_KINDS: Dict[str, str] = {
    "원": _AMOUNT, "포인트": _AMOUNT, "점": _AMOUNT,
    "%": _RATE, "퍼센트": _RATE,
    "회": _COUNT, "건": _COUNT,
}

# ── 수 읽기 ────────────────────────────────────────────────
#
# 같은 값을 "15,000원"·"1만 5천원"·"만오천 원"으로 쓸 수 있다. 숫자 표기만 검사하면
# 표기만 바꿔 그대로 빠져나가므로, 만·천·백과 한글 숫자를 섞은 표기도 정수로 환산한다.
# 조회 결과도 같은 규칙으로 읽는다 — 약관 원문은 "월 5천원 한도"처럼 적혀 있다.
#
# 못 읽는 표기는 검사하지 않고 통과시킨다. 억지로 읽다가 낱말을 수로 잘못 읽으면
# 멀쩡한 답변이 막히고, 그런 검사는 결국 꺼진다.
_ARABIC = r"\d[\d,]*(?:\.\d+)?"
_KOREAN_DIGITS = "일이삼사오육칠팔구"
_COEFFICIENT = rf"(?:{_ARABIC}|[{_KOREAN_DIGITS}])"


def _place_term(place: str) -> str:
    # 붙여 쓰면 앞 수를 생략할 수 있다(천오백). 띄어 쓴 뒤에는 앞 수가 있어야 잇는다 —
    # "30만 백화점"의 '백'을 100으로 읽어 30만을 300,100으로 바꾸지 않으려고.
    return rf"(?:{_COEFFICIENT}?{place}|\s+{_COEFFICIENT}{place})"


# 만 아래 네 자리. 천·백·십을 이 순서로만 읽는다.
# 끝의 아라비아 숫자는 붙여 쓴 것만 잇는다(2천500). 띄어 쓴 것은 금액일 때만 잇는다 —
# "53만 7,200원"은 한 금액이지만 "30만 5회"를 300,005회로 읽으면 멀쩡한 "5회"가 막힌다.
# 끝의 한글 숫자는 만·억 바로 앞에서만 읽는다(오만, 이십오만). 단위 앞의 한글 숫자는
# 조사·낱말과 겹친다 — "오천이 원래"의 '이', "만일 원하시면"의 '일'.
_BELOW_MAN = (
    rf"{_place_term('천')}?{_place_term('백')}?{_place_term('십')}?"
    rf"(?:{_ARABIC}"
    rf"|[{_KOREAN_DIGITS}](?=[만억])"
    rf"|(?<=[십백천만억])\s+{_ARABIC}(?=\s*(?:원|포인트|점)))?"
)

# 한글 숫자는 낱말 첫머리에서만 읽는다. "회사원"의 '사원', "일회용"의 '일회'를 4원·1회로
# 읽지 않으려고. 한글 숫자만으로 된 수는 자릿값(십·백·천·만·억)이 있어야 수로 본다.
# 억 뒤에 띄어 쓴 아라비아 숫자는 만이 따라올 때만 잇는다(1억 5000만).
_NUMERAL = rf"""
    (?P<numeral>
        (?:(?<![\d.])(?=\d)|(?<![\d.가-힣])(?=[{_KOREAN_DIGITS}십백천만억]))
        (?:(?P<eok>{_BELOW_MAN})억)?
        (?:(?P<man>{_BELOW_MAN}|\s+{_ARABIC})만)?
        (?P<rest>{_BELOW_MAN})
    )
"""

# "3천 회원"·"5천 점포"는 단위가 아니라 세는 말이다.
_UNIT = r"\s*(?!회원|회사|점포)(?P<unit>원|%|퍼센트|포인트|점|회|건)"

_MEASURED_NUMBER = re.compile(_NUMERAL + _UNIT, re.VERBOSE)
_ANY_NUMBER = re.compile(_NUMERAL + rf"(?:{_UNIT})?", re.VERBOSE)

_PLACE_VALUES = {"십": 10, "백": 100, "천": 1000}
_PLACE_TERM = re.compile(rf"({_COEFFICIENT})?([십백천])")

# 앞 수를 생략한 한 글자("천 원"·"만 원")는 읽지 않는다. "혜택 만점"·"백점"과
# 모양이 같아 수로 읽으면 멀쩡한 문장이 걸린다.
_IMPLIED_ONE_ONLY = set("십백천만억")

# 답변 길이는 막지 않고 재기만 한다. 자르면 문장이 중간에 끊겨 더 나빠진다.
# 프롬프트를 손볼 때 무엇이 나아졌는지 보려면 먼저 재고 있어야 한다.
_LONG_ANSWER_CHARS = 400


@dataclass(frozen=True)
class Verdict:
    """검증 결과. ok 가 False 면 그 답변은 내보내지 않는다."""

    ok: bool
    reason: str = ""
    unsupported_numbers: List[str] = field(default_factory=list)


def _coefficient(text: Optional[str]) -> Decimal:
    if not text:
        return Decimal(1)
    if len(text) == 1 and text in _KOREAN_DIGITS:
        return Decimal(_KOREAN_DIGITS.index(text) + 1)
    return Decimal(text.replace(",", ""))


def _below_man_value(text: str) -> Decimal:
    """만 아래 네 자리. "2천5백" → 2500, "천오백" → 1500, "2,500" → 2500."""
    compact = "".join(text.split())
    total = Decimal(0)
    end = 0
    for term in _PLACE_TERM.finditer(compact):
        total += _coefficient(term.group(1)) * _PLACE_VALUES[term.group(2)]
        end = term.end()
    if compact[end:]:
        total += _coefficient(compact[end:])
    return total


def _group_value(text: Optional[str], scale: int) -> Decimal:
    if text is None:
        return Decimal(0)
    # "만 5천"처럼 만 앞이 비었으면 1만이다. 0만과 헷갈리지 않게 빈 것만 1로 본다
    coefficient = _below_man_value(text) if text.strip() else Decimal(1)
    return coefficient * scale


def _read(match: re.Match) -> Optional[Tuple[Decimal, Optional[str]]]:
    """(값, 단위 종류). 수로 보지 않는 표기면 None. 단위가 없으면 종류도 None 이다.

    Decimal 로 계산한다. float 로는 1.12만이 11200.000000000002 가 되어 11,200 과 어긋난다.
    소수 표기 차이(1.2 ↔ 1.20)도 Decimal 비교에서 같은 값이 된다.
    """
    numeral = "".join(match.group("numeral").split())
    if not numeral or numeral in _IMPLIED_ONE_ONLY:
        return None
    value = (
        _group_value(match.group("eok"), 100_000_000)
        + _group_value(match.group("man"), 10_000)
        + _below_man_value(match.group("rest"))
    )
    unit = match.group("unit")
    return value, (_UNIT_KINDS[unit] if unit else None)


def _numbers_in_context(context: str) -> Tuple[Set[Tuple[Decimal, str]], Set[Decimal]]:
    """조회 결과에 실재하는 수. (단위가 붙은 수, 단위를 알 수 없는 수)로 나눠 돌려준다.

    단위가 없는 수도 담는다 — 표에 그냥 적힌 값이 많다("할인율(%) | 10").
    이런 수는 어느 단위로 말해도 근거로 인정한다. 단위를 모르는데 한쪽으로 정하면
    표의 값을 옮겨 말한 멀쩡한 답변이 막힌다.
    """
    measured: Set[Tuple[Decimal, str]] = set()
    unit_unknown: Set[Decimal] = set()
    # 답변을 읽는 규칙으로도 한 번 더 읽는다. 단위를 요구하느냐에 따라 수를 끊는 자리가
    # 달라질 수 있는데, 그 차이 때문에 조회 결과를 그대로 옮긴 문장이 막히면 안 된다.
    matches = list(_ANY_NUMBER.finditer(context)) + list(_MEASURED_NUMBER.finditer(context))
    for match in matches:
        read = _read(match)
        if read is None:
            continue
        value, kind = read
        if kind is None:
            unit_unknown.add(value)
        else:
            measured.add((value, kind))
    return measured, unit_unknown


def verify(answer_text: str, context: str) -> Verdict:
    """답변을 내보내도 되는지 판정한다.

    조회 결과가 비어 있으면(되묻기 흐름 등) 대조할 근거가 없으므로 통과시킨다.
    """
    text = (answer_text or "").strip()
    if not text:
        return Verdict(ok=False, reason="빈 답변")

    if len(text) >= _LONG_ANSWER_CHARS:
        # 통과시키되 남긴다. 어느 의도에서 길어지는지 알아야 프롬프트를 고칠 수 있다.
        logger.info("답변이 길다: %d자", len(text))

    if not context:
        return Verdict(ok=True)

    measured, unit_unknown = _numbers_in_context(context)
    unsupported: List[str] = []
    for match in _MEASURED_NUMBER.finditer(text):
        read = _read(match)
        if read is None:
            continue
        if read in measured or read[0] in unit_unknown:
            continue
        unsupported.append(match.group(0))

    if unsupported:
        return Verdict(
            ok=False,
            reason="조회 결과에 없는 숫자를 말했다",
            unsupported_numbers=unsupported,
        )
    return Verdict(ok=True)


def fallback_answer(context: str) -> str:
    """검증에 걸렸거나 LLM 호출이 실패했을 때 대신 내보내는 문장.

    조회 결과를 그대로 보여준다. 문장으로 다듬지는 못해도 **값은 전부 맞다** —
    서버가 DB·엔진에서 받아 만든 것이기 때문이다.

    지어낸 문장을 내보내는 것보다 투박한 사실을 보여주는 편이 낫다.
    """
    body = (context or "").strip()
    if not body:
        return "지금은 답변을 만들지 못했습니다. 잠시 후 다시 물어봐 주세요."
    return "답변을 다듬지 못해 조회한 내용을 그대로 보여드립니다.\n\n" + body
