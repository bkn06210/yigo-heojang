"""실제 모델이 무엇을 내놓는지 재는 도구.

두 가지를 확인한다.

  1) 규칙 기반 stub 이 못 알아듣던 표현을 실제 모델이 알아듣는가
  2) 별칭 테이블에 등록한 표현 중 **불필요한 것이 무엇인가**
     — 모델이 알아서 정식 명칭으로 바꿔 준다면 그 별칭은 등록할 이유가 없다.
       반대로 우리 DB 표기가 특이한 것(써브웨이·농협 하나로마트)은 모델이 알 수 없어 남아야 한다.

토큰을 쓰므로 평소에는 돌리지 않는다. 프롬프트를 고쳤을 때만 다시 잰다.

    cd ai-server
    LLM_PROVIDER=openai .venv/Scripts/python.exe -m chatbot.tools.measure_llm
"""

import logging
import sys
from typing import List, Optional, Tuple

from ..config import get_settings
from ..db import connection
from ..llm import create_llm_client
from ..llm.stub import StubLlmClient
from ..resolver import MATCH_NAME, resolve_merchant

# (질문, 확인하려는 것)
CASES: List[Tuple[str, str]] = [
    # stub 이 규칙으로 못 잡던 표현
    ("이번 달 목표 얼마나 채웠지?", "실적 질문의 다른 표현"),
    ("지금 카드 얼마나 썼어?", "실적 질문의 다른 표현"),
    ("커피 마시려는데 뭐로 긁을까", "추천 질문의 다른 표현"),
    ("이번달에 카드로 아낀 돈 알려줘", "혜택 합계"),
    ("카드 잃어버렸을 때 어떻게 하지", "약관"),
    # 별칭 등록분 — 모델이 정식 명칭으로 바꿔 주는가
    ("스벅에서 5000원 쓸건데 뭐가 좋아", "축약어 (등록: 스벅)"),
    ("배민으로 2만원 시킬건데", "축약어 (등록: 배민)"),
    ("올영에서 3만원 결제", "축약어 (등록: 올영)"),
    ("파바에서 빵 살건데", "축약어 (등록: 파바)"),
    ("쓱배송으로 5만원", "축약어 (등록: 쓱)"),
    # 우리 DB 표기가 특이한 것 — 모델이 알 수 없어야 정상
    ("서브웨이에서 1만원", "DB 표기는 '써브웨이'"),
    ("하나로마트에서 3만원", "DB 표기는 '농협 하나로마트'"),
    ("엔젤리너스에서 5천원", "DB 표기는 '엔제리너스'"),
    # 모호한 것 — 되물어야 정상
    ("마이핏카드 실적 채웠어?", "적립형·할인형 두 장"),
    ("GS에서 만원 결제", "GS25 / GS SHOP / GS칼텍스"),
    # 무관한 질문
    ("오늘 서울 날씨 알려줘", "UNKNOWN 이어야 정상"),
]


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    logging.basicConfig(level=logging.INFO, format="    %(message)s")

    settings = get_settings()
    if settings.llm_provider != "openai":
        print("LLM_PROVIDER=openai 로 실행할 것. 지금은:", settings.llm_provider)
        return

    real = create_llm_client(settings)
    stub = StubLlmClient()
    print(f"모델: {settings.openai_model}\n")

    unnecessary: List[str] = []
    with connection() as conn:
        for question, note in CASES:
            print(f"Q: {question}   ({note})")
            real_intent = real.classify(question)
            stub_intent = stub.classify(question)

            print(f"    모델: {real_intent.name:<16} 가맹점={real_intent.merchant_text!r}"
                  f" 카드={real_intent.card_text!r} 금액={real_intent.amount}")
            print(f"    stub: {stub_intent.name:<16} 가맹점={stub_intent.merchant_text!r}")

            verdict = _alias_verdict(conn, real_intent.merchant_text)
            if verdict:
                print(f"    별칭: {verdict}")
                if verdict.startswith("불필요"):
                    unnecessary.append(f"{question} → {real_intent.merchant_text}")
            print()

    if unnecessary:
        print("모델이 정식 명칭으로 바꿔 주는 표현 (별칭 등록 불필요):")
        for line in unnecessary:
            print("  -", line)


def _alias_verdict(conn, merchant_text: Optional[str]) -> Optional[str]:
    """모델이 내놓은 표현이 별칭 없이도 찾아지는지."""
    if not merchant_text:
        return None

    result = resolve_merchant(conn, merchant_text)
    if not result.found:
        return f"못 찾음 — 별칭 추가 대상 ({merchant_text!r})"
    if result.match.matched_by == MATCH_NAME:
        return f"불필요 — 정식 명칭으로 바로 찾음 ({result.match.name})"
    return f"필요 — {result.match.matched_by} 로만 찾음 ({result.match.name})"


if __name__ == "__main__":
    main()
