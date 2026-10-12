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
import time
from pathlib import Path
from typing import Any, Dict, Optional

from deadline import can_retry, timeout_for
from openai import APIConnectionError, BadRequestError, InternalServerError, OpenAI, RateLimitError

from .base import Answer, Intent, IntentName, LlmClient

logger = logging.getLogger(__name__)

_PROMPT_DIR = Path(__file__).resolve().parent.parent / "prompts"


def _read_prompt(name: str) -> str:
    return (_PROMPT_DIR / name).read_text(encoding="utf-8")


# 사람이 답을 기다리는 시간이다. 더 늘리면 화면이 멈춘 것처럼 보이고,
# 더 줄이면 정상 응답까지 잘라 버린다. 요청 마감이 걸려 있으면 남은 시간이 더 짧을 때 그쪽을 쓴다.
_REQUEST_TIMEOUT_SECONDS = 20.0
# 일시적인 429·5xx 만 넘기면 되므로 짧게 잡는다. 여기서 오래 끌면 타임아웃보다 더 기다리게 된다.
_MAX_RETRIES = 2
_RETRY_BACKOFF_SECONDS = 0.5

# 다시 보내면 나을 수 있는 실패. 연결 실패·타임아웃(APITimeoutError 가 APIConnectionError 의
# 하위 형이다)·한도 초과·서버 오류다. 요청이 잘못된 400 계열은 다시 보내도 같다.
_RETRYABLE_ERRORS = (APIConnectionError, RateLimitError, InternalServerError)


class OpenAiLlmClient(LlmClient):
    provider = "openai"

    def __init__(self, api_key: str, model: str):
        if not api_key:
            raise ValueError("OPENAI_API_KEY 가 비어 있다. ai-server/.env 를 확인할 것")
        # 타임아웃과 재시도를 명시한다. 기본값에 맡기면 한 번 느린 호출에 화면이 붙잡히고,
        # 일시적인 429·5xx 에도 그대로 실패한다. 챗봇은 사람이 기다리는 자리라 오래 끌 수 없다.
        #
        # 재시도는 SDK 에 맡기지 않고(max_retries=0) _create 에서 한다. SDK 는 요청 마감을 몰라
        # 시도마다 타임아웃을 새로 받으므로, 세 번이면 20초 × 3 = 60초를 기다린다.
        self._client = OpenAI(api_key=api_key, timeout=_REQUEST_TIMEOUT_SECONDS, max_retries=0)
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
            response = self._create(params, label)
        except BadRequestError as error:
            if not self._supports_temperature or "temperature" not in str(error):
                raise
            # 모델이 temperature 를 안 받는 경우다. 빼고 한 번만 다시 시도한다.
            logger.info("temperature 미지원 모델로 판단해 생략한다: %s", self._model)
            self._supports_temperature = False
            params.pop("temperature", None)
            response = self._create(params, label)

        _log_usage(label, response)
        return response.choices[0].message.content

    def _create(self, params: Dict[str, Any], label: str):
        """요청 마감 안에서만 호출하고, 남은 시간이 있을 때만 다시 시도한다.

        시도마다 타임아웃을 남은 시간으로 줄인다. 남은 시간이 너무 짧으면 보내지 않고
        DeadlineExceeded 를 던진다 — 답이 올 수 없는 요청에 토큰을 쓰지 않는다.
        """
        for attempt in range(_MAX_RETRIES + 1):
            try:
                return self._client.chat.completions.create(
                    **params, timeout=timeout_for(_REQUEST_TIMEOUT_SECONDS))
            except _RETRYABLE_ERRORS as error:
                backoff = _RETRY_BACKOFF_SECONDS * (attempt + 1)
                if attempt == _MAX_RETRIES or not can_retry(backoff):
                    raise
                # 질문·프롬프트는 남기지 않는다. 어느 단계에서 무슨 오류였는지만 있으면 된다.
                logger.warning("LLM %s 실패(%s), 재시도 %d회차",
                               label, type(error).__name__, attempt + 1)
                time.sleep(backoff)
        raise RuntimeError("LLM 호출에 실패했다")


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
