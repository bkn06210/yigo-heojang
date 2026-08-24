"""LLM 어댑터의 공통 계약.

챗봇 코드는 이 두 메서드만 부르고, 뒤에 실제 LLM 이 있는지 개발용 가짜가 있는지
알지 못한다. 그래야 개발 내내 API 호출 없이 전체 흐름을 돌려보고,
시연 때 설정 한 줄로 실제 모델로 바꿀 수 있다.

LLM 을 두 자리로만 제한한 것은 설계 원칙이다.
  classify — 사람 말에서 의도와 표현을 뽑는다 (판단 아님)
  compose  — 서버가 만든 숫자를 문장으로 바꾼다 (계산 아님)
금액 계산·추천 판단은 엔진이 한다. 그 사이 어디에도 LLM 이 끼지 않는다.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Optional


class IntentName:
    """질문 유형.

    엔진이 이미 답할 수 있는 것과 약관 원문이 필요한 것으로 갈린다.
    분류에 실패하면 지어내지 않고 UNKNOWN 으로 두고 되묻는다.
    """

    BENEFIT_SUM = "BENEFIT_SUM"  # 이번 달 얼마나 할인받았나
    CARD_STATUS = "CARD_STATUS"  # 실적 달성률·남은 한도
    RECOMMEND_CARD = "RECOMMEND_CARD"  # 여기서 결제하면 어느 카드가 유리한가
    # 위와 다르다. 위는 "이번 결제에 어느 카드를 쓸까"라 보유 카드로 한 건을 계산하고,
    # 이쪽은 "어떤 카드를 발급할까"라 미보유 카드까지 놓고 한 달치를 계산한다.
    RECOMMEND_NEW_CARD = "RECOMMEND_NEW_CARD"  # 내 소비에 맞는 카드를 발급할까
    TERM_QA = "TERM_QA"  # 약관 질문 (청구 시점·분실 처리 등)
    UNKNOWN = "UNKNOWN"

    ALL = (BENEFIT_SUM, CARD_STATUS, RECOMMEND_CARD, RECOMMEND_NEW_CARD, TERM_QA, UNKNOWN)


@dataclass(frozen=True)
class Intent:
    """분류 결과.

    가맹점·카드를 id 가 아니라 '표현' 그대로 담는다. 카드와 가맹점이 수백 개가 되면
    목록을 프롬프트에 넣을 수 없어서, 이름을 맞추는 일은 서버가 별칭 조회로 한다.
    LLM 은 "스벅"이라는 말이 나왔다는 것까지만 알려준다.
    """

    name: str
    merchant_text: Optional[str] = None
    category_text: Optional[str] = None
    card_text: Optional[str] = None
    period_text: Optional[str] = None
    amount: Optional[int] = None
    # 약관에서 찾을 때 쓸 검색어. 사람이 쓰는 말과 약관에 적힌 말이 달라서 필요하다.
    # "잃어버렸어요"로는 "분실" 조항이 걸리지 않는다(실측: 일치 0건).
    # 뜻이 통하는 말끼리 이어주는 일이라 엔티티 표현 추출과 성격이 같다 — 판단이 아니다.
    term_query: Optional[str] = None
    # 분류 근거로 삼은 표현. 되물을 때 "무엇을 못 알아들었는지" 보여주는 데 쓴다.
    raw: Dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Answer:
    """생성한 답변.

    문장과 함께 근거를 남긴다. 숫자가 어디서 나왔는지 화면에 표시할 수 있어야
    "AI 가 지어낸 값"이라는 의심을 받지 않는다.
    """

    text: str
    sources: List[str] = field(default_factory=list)


class LlmClient(ABC):
    """LLM 한 곳을 감싼 어댑터."""

    provider: str

    @abstractmethod
    def classify(self, question: str) -> Intent:
        """질문에서 의도와 엔티티 표현을 뽑는다. DB 조회·계산은 하지 않는다."""

    @abstractmethod
    def compose(self, question: str, context: str) -> Answer:
        """서버가 조립한 컨텍스트를 문장으로 바꾼다.

        context 안의 숫자는 이미 완성된 값이다. 더하거나 비교하게 시키지 않는다 —
        LLM 이 산수를 하면 화면 숫자와 답변 숫자가 어긋난다.
        """
