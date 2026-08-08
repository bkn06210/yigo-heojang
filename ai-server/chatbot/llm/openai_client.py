"""실제 LLM 호출.

프롬프트를 코드에 문자열로 박지 않고 prompts/*.md 에서 읽는다.
프롬프트는 사실상 이 서버의 규칙이라 diff 로 변경 이력이 보여야 하고,
고칠 때 파이썬을 몰라도 되어야 한다.

분류 응답은 JSON 으로 강제한다. 자유 문장으로 받으면 파싱이 매번 다르게 깨진다.
그래도 형식이 어긋날 수 있으므로, 실패하면 예외를 던지지 않고 UNKNOWN 으로 떨어뜨린다 —
분류 실패는 서버가 되물으면 되는 일이지 500 이 날 일이 아니다.

토큰 사용량을 로그로 남긴다. 호출 한 번이 얼마나 드는지 눈으로 봐야
프롬프트를 키울지 줄일지 판단할 수 있다.
"""

import json
import logging
from pathlib import Path
from typing import Any, Dict, Optional

from openai import BadRequestError, OpenAI

from .base import Answer, Intent, IntentName, LlmClient

logger = logging.getLogger(__name__)

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
        # 일부 모델은 temperature 를 기본값 외로 받지 않는다. 첫 호출에서 확인하고 이후 생략한다.
        self._supports_temperature = True

    def classify(self, question: str) -> Intent:
        content = self._complete(
            system=self._classify_prompt,
            user=question,
            temperature=0,  # 같은 질문에 같은 분류가 나와야 재현이 된다
            response_format={"type": "json_object"},
            label="classify",
        )
        return _to_intent(content)

    def compose(self, question: str, context: str) -> Answer:
        content = self._complete(
            system=self._compose_prompt,
            user=f"[질문]\n{question}\n\n[조회 결과]\n{context}",
            temperature=0.2,
            response_format=None,
            label="compose",
        )
        return Answer(text=(content or "").strip())

    def _complete(self, system: str, user: str, temperature: float,
                  response_format: Optional[dict], label: str) -> Optional[str]:
        params: Dict[str, Any] = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
        }
        if response_format:
            params["response_format"] = response_format
        if self._supports_temperature:
            params["temperature"] = temperature

        try:
            response = self._client.chat.completions.create(**params)
        except BadRequestError as error:
            if not self._supports_temperature or "temperature" not in str(error):
                raise
            # 모델이 temperature 를 안 받는 경우다. 빼고 한 번만 다시 시도한다.
            logger.info("temperature 미지원 모델로 판단해 생략한다: %s", self._model)
            self._supports_temperature = False
            params.pop("temperature", None)
            response = self._client.chat.completions.create(**params)

        _log_usage(label, response)
        return response.choices[0].message.content


def _log_usage(label: str, response) -> None:
    usage = getattr(response, "usage", None)
    if usage is None:
        return
    logger.info(
        "LLM %s: 입력 %s토큰 / 출력 %s토큰",
        label,
        getattr(usage, "prompt_tokens", "?"),
        getattr(usage, "completion_tokens", "?"),
    )


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
        term_query=parsed.get("termQuery"),
    )
