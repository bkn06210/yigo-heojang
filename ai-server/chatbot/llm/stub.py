"""개발용 가짜 LLM.

API 를 한 번도 부르지 않고 챗봇 전체 흐름을 돌리기 위한 것이다.
분류 → 엔티티 해석 → 조회 → 답변 조립까지 전부 진짜로 돌고, LLM 두 자리만 가짜다.

규칙 기반이라 등록해둔 표현만 알아듣는다. 실제 사용자는 아무 말이나 하므로
시연 전에는 반드시 실제 모델로 한 번 돌려봐야 한다.

가짜라는 사실을 답변에 남긴다. 개발용 응답이 그대로 화면에 나가는 것을
눈으로 알아챌 수 있어야 한다.
"""

import re
from typing import Dict, Optional, Tuple

from .base import Answer, Intent, IntentName, LlmClient

STUB_SOURCE = "개발용 응답 (LLM 미호출)"

# 의도 판정 규칙. 위에서부터 먼저 걸리는 것을 쓴다.
# 순서가 의미를 갖는다 — "이번 달 얼마 아꼈어"는 금액 질문이지 현황 질문이 아니다.
_INTENT_RULES: Tuple[Tuple[str, str], ...] = (
    (IntentName.BENEFIT_SUM, r"얼마|할인받|혜택.*받|아꼈|절약"),
    (IntentName.RECOMMEND_CARD, r"어느 ?카드|어떤 ?카드|무슨 ?카드|뭘로|추천"),
    (IntentName.CARD_STATUS, r"실적|한도|남은|달성|채웠|채워"),
    (IntentName.TERM_QA, r"약관|청구|언제 빠져|분실|잃어버|도난|해지|없애|연회비|재발급|수수료"),
)

# LLM 이라면 문맥으로 뽑아낼 표현들. 가짜라서 목록으로 대신한다.
# 여기 없는 말은 UNKNOWN 이 되고, 서버가 되묻는다.
_MERCHANT_WORDS: Tuple[str, ...] = ("스벅", "스타벅스", "GS25", "CU", "이마트", "쿠팡", "배민")

# 업종은 표준 이름으로 바꿔서 넘긴다. 실제 모델도 프롬프트의 목록에서 골라 오므로
# 뒤쪽(해석·조회)이 두 경로에서 같은 값을 받는다.
_CATEGORY_WORDS: Dict[str, str] = {
    "편의점": "편의점",
    "카페": "카페",
    "커피": "카페",
    "마트": "대형마트",
    "주유": "주유",
    "대중교통": "대중교통",
    "지하철": "대중교통",
    "배달": "배달앱",
}
_PERIOD_WORDS: Tuple[str, ...] = ("이번 달", "이번달", "지난달", "저번 달", "이번 주", "오늘", "어제")

# 일상어 → 약관 낱말. 실제 모델은 문맥을 보고 만들지만 가짜는 표로 대신한다.
# 이 표가 짧은 것은 의도한 것이다 — 늘려서 진짜처럼 보이게 하면 실제 모델로 바꿨을 때
# 무엇이 달라지는지 알 수 없게 된다.
_TERM_WORDS: Dict[str, str] = {
    "잃어버": "분실 도난 신고",
    "분실": "분실 도난 신고",
    "도난": "분실 도난 신고",
    "해지": "카드 해지",
    "없애": "카드 해지",
    "재발급": "재발급 갱신발급",
    "연회비": "연회비 반환",
    "청구": "대금결제 결제일",
    "언제 빠져": "대금결제 결제일 출금",
    "수수료": "수수료",
}

_AMOUNT_PATTERN = re.compile(r"(\d[\d,]*)\s*(만원|원)")


class StubLlmClient(LlmClient):
    provider = "stub"

    def classify(self, question: str) -> Intent:
        name = self._match_intent(question)
        return Intent(
            name=name,
            merchant_text=_first_hit(question, _MERCHANT_WORDS),
            category_text=_first_category(question),
            period_text=_first_hit(question, _PERIOD_WORDS),
            amount=_parse_amount(question),
            term_query=_term_query(question) if name == IntentName.TERM_QA else None,
            raw={"matchedBy": "stub-rule"},
        )

    def compose(self, question: str, context: str) -> Answer:
        # 문장을 다듬는 일이 LLM 의 몫이라 가짜는 컨텍스트를 그대로 돌려준다.
        # 지어내지 않는 편이 낫다 — 숫자가 맞는지 눈으로 확인할 수 있다.
        return Answer(text=context, sources=[STUB_SOURCE])

    def _match_intent(self, question: str) -> str:
        for name, pattern in _INTENT_RULES:
            if re.search(pattern, question):
                return name
        return IntentName.UNKNOWN


def _first_hit(question: str, words: Tuple[str, ...]) -> Optional[str]:
    for word in words:
        if word in question:
            return word
    return None


def _term_query(question: str) -> Optional[str]:
    """질문에 든 말을 약관 낱말로 바꾼다. 여러 개가 걸리면 모두 이어 붙인다."""
    matched = [
        term for word, term in _TERM_WORDS.items() if word in question
    ]
    if not matched:
        return None

    # 같은 낱말이 여러 규칙에서 나오므로 순서를 지키며 중복을 없앤다.
    words = dict.fromkeys(word for term in matched for word in term.split())
    return " ".join(words)


def _first_category(question: str) -> Optional[str]:
    for word, standard_name in _CATEGORY_WORDS.items():
        if word in question:
            return standard_name
    return None


def _parse_amount(question: str) -> Optional[int]:
    match = _AMOUNT_PATTERN.search(question)
    if not match:
        return None
    value = int(match.group(1).replace(",", ""))
    return value * 10000 if match.group(2) == "만원" else value
