"""실제 LLM 호출.

프롬프트를 코드에 문자열로 박지 않고 prompts/*.md 에서 읽는다.
프롬프트는 사실상 이 서버의 규칙이라 diff 로 변경 이력이 보여야 하고,
고칠 때 파이썬을 몰라도 되어야 한다.

분류 응답은 JSON 으로 강제한다. 자유 문장으로 받으면 파싱이 매번 다르게 깨진다.
그래도 형식이 어긋날 수 있으므로, 실패하면 예외를 던지지 않고 UNKNOWN 으로 떨어뜨린다 —
분류 실패는 서버가 되물으면 되는 일이지 500 이 날 일이 아니다.
"""

import json
from pathlib import Path
from typing import Any, Dict, Optional

from openai import OpenAI

from .base import Answer, Intent, IntentName, LlmClient

_PROMPT_DIR = Path(__file__).resolve().parent.parent / "prompts"


def _read_prompt(name: str) -> str:
    return (_PROMPT_DIR / name).read_text(encoding="utf-8")


class OpenAiLlmClient(LlmClient):
    provider = "openai"

    def __init__(self, api_key: str, model: str):
        if not api_key:
            raise ValueError("OPENAI_API_KEY 가 비어 있다. ai-server/.env 를 확인할 것")
        self._client = OpenAI(api_key=api_key)
        self._model = model
        self._classify_prompt = _read_prompt("classify.md")
        self._compose_prompt = _read_prompt("compose.md")

    def classify(self, question: str) -> Intent:
        response = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": self._classify_prompt},
                {"role": "user", "content": question},
            ],
            response_format={"type": "json_object"},
            # 분류는 같은 질문에 같은 답이 나와야 한다. 흔들리면 재현이 안 된다.
            temperature=0,
        )
        return _to_intent(response.choices[0].message.content)

    def compose(self, question: str, context: str) -> Answer:
        response = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": self._compose_prompt},
                {"role": "user", "content": f"[질문]\n{question}\n\n[조회 결과]\n{context}"},
            ],
            temperature=0.2,
        )
        return Answer(text=(response.choices[0].message.content or "").strip())


def _to_intent(raw: Optional[str]) -> Intent:
    try:
        parsed: Dict[str, Any] = json.loads(raw or "")
    except (TypeError, ValueError):
        return Intent(name=IntentName.UNKNOWN, raw={"parseError": (raw or "")[:200]})

    name = parsed.get("name")
    if name not in IntentName.ALL:
        # 모델이 없는 의도를 만들어낸 경우다. 조용히 통과시키면 라우팅이 엉뚱한 데로 간다.
        return Intent(name=IntentName.UNKNOWN, raw={"unknownIntent": str(name)})

    return Intent(
        name=name,
        merchant_text=parsed.get("merchantText"),
        category_text=parsed.get("categoryText"),
        card_text=parsed.get("cardText"),
        period_text=parsed.get("periodText"),
        amount=parsed.get("amount"),
    )
