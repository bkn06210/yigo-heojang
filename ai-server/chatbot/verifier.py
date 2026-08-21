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
from typing import List, Set

logger = logging.getLogger(__name__)

# 검사 대상은 단위가 붙은 수다. "카드 3장"처럼 세는 말까지 잡으면 오탐이 는다 —
# 세는 말은 조회 결과에 숫자로 안 적혀 있어도 문장을 만들며 나올 수 있다.
_MEASURED_NUMBER = re.compile(r"([\d,]+(?:\.\d+)?)\s*(억|만)?\s*(원|%|퍼센트|포인트|점|회|건)")
_ANY_NUMBER = re.compile(r"([\d,]+(?:\.\d+)?)\s*(억|만)?")

# 답변 길이는 막지 않고 재기만 한다. 자르면 문장이 중간에 끊겨 더 나빠진다.
# 프롬프트를 손볼 때 무엇이 나아졌는지 보려면 먼저 재고 있어야 한다.
_LONG_ANSWER_CHARS = 400


@dataclass(frozen=True)
class Verdict:
    """검증 결과. ok 가 False 면 그 답변은 내보내지 않는다."""

    ok: bool
    reason: str = ""
    unsupported_numbers: List[str] = field(default_factory=list)


def _scale(unit: str) -> int:
    if unit == "억":
        return 100_000_000
    if unit == "만":
        return 10_000
    return 1


def _to_number(raw: str, unit: str) -> float:
    return float(raw.replace(",", "")) * _scale(unit)


def _numbers_in_context(context: str) -> Set[float]:
    """조회 결과에 실재하는 수. 단위가 없는 수도 담는다 — 표에 그냥 적힌 값이 많다."""
    found: Set[float] = set()
    for raw, unit in _ANY_NUMBER.findall(context):
        if not raw.strip(","):
            continue
        try:
            found.add(_to_number(raw, unit))
        except ValueError:
            continue
    return found


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

    allowed = _numbers_in_context(context)
    unsupported: List[str] = []
    for raw, unit, suffix in _MEASURED_NUMBER.findall(text):
        try:
            value = _to_number(raw, unit)
        except ValueError:
            continue
        if value in allowed:
            continue
        # 백분율은 소수 표기가 갈린다(1.2% ↔ 1.20). 정수화해 한 번 더 본다
        if value == int(value) and float(int(value)) in allowed:
            continue
        unsupported.append(f"{raw}{unit}{suffix}")

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
