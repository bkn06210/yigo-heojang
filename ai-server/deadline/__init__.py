"""요청 한 건이 쓸 수 있는 시간(마감시간).

챗봇 요청은 Spring 이 기다려 주는 시간(읽기 타임아웃) 안에 끝나야 의미가 있다. 그 뒤에 만든
답은 받을 사람이 없는데, 그사이 LLM 을 부르면 토큰만 쓴다.

호출마다 제 타임아웃을 따로 가지면 합이 그 시간을 넘는다. LLM 20초 × 3회, 임베딩 10초 × 3회,
다시 LLM 20초 × 3회 — 하나하나는 짧아 보여도 모두 느려지면 2분이 넘는다.

그래서 요청이 들어올 때 마감 시각을 하나 정하고, 바깥으로 나가는 호출(LLM·임베딩·엔진·DB)은
모두 **남은 시간**만큼만 기다린다. 남은 시간으로 한 번 더 시도할 수 없으면 재시도하지 않는다.

마감은 함수 인자가 아니라 요청 범위 변수(ContextVar)에 둔다. 자바의 ThreadLocal 과 같은 것이다.
인자로 넘기면 LLM·임베딩 어댑터의 공통 계약과 그 사이 모든 함수의 모양이 바뀌는데, 임베딩
어댑터는 마감이 없는 색인 배치도 함께 쓴다. 마감이 걸리지 않은 곳(배치)에서는 각 호출이
원래 타임아웃과 재시도를 그대로 쓴다.

챗봇·임베딩 어느 쪽에도 속하지 않아 따로 둔다. 임베딩이 챗봇을 가져다 쓰면 색인 배치가
서버 코드에 묶인다.
"""

import time
from contextlib import contextmanager
from contextvars import ContextVar
from dataclasses import dataclass
from typing import Iterator, Optional

# 이보다 적게 남았으면 새로 시도하지 않는다. 응답이 올 수 없는 시간에 요청을 보내면 상대 서버에
# 일만 시키고(LLM 이면 토큰까지 쓰고) 결과는 버려진다. 실측으로 LLM 한 번이 1~2초, 질문 임베딩이
# 0.5초 남짓이라 1초 미만으로 남았을 때 새로 시작한 호출은 대개 끝나지 못한다.
MIN_ATTEMPT_SECONDS = 1.0


class DeadlineExceeded(TimeoutError):
    """마감까지 남은 시간으로는 이 호출을 시도할 수 없다."""


@dataclass(frozen=True)
class Deadline:
    # 벽시계(time.time)가 아니라 단조 시계다. 벽시계는 시각 동기화로 앞뒤로 움직일 수 있다.
    expires_at: float

    @classmethod
    def after(cls, seconds: float) -> "Deadline":
        return cls(time.monotonic() + seconds)

    def remaining(self) -> float:
        return self.expires_at - time.monotonic()

    def timeout(self, preferred: float) -> float:
        """이번 호출에 줄 타임아웃. 원래 값과 남은 시간 중 짧은 쪽이다."""
        remaining = self.remaining()
        if remaining < MIN_ATTEMPT_SECONDS:
            raise DeadlineExceeded(f"마감까지 {max(remaining, 0.0):.2f}초 남아 시도하지 않는다")
        return min(preferred, remaining)

    def allows_retry(self, backoff: float) -> bool:
        """backoff 만큼 쉬고도 한 번 더 시도할 시간이 남는가."""
        return self.remaining() - backoff >= MIN_ATTEMPT_SECONDS


_current: ContextVar[Optional[Deadline]] = ContextVar("request_deadline", default=None)


@contextmanager
def request_deadline(seconds: float) -> Iterator[Deadline]:
    """이 블록 안에서 나가는 호출은 지금부터 seconds 안에 끝나야 한다."""
    deadline = Deadline.after(seconds)
    token = _current.set(deadline)
    try:
        yield deadline
    finally:
        # 되돌려 놓지 않으면 같은 스레드가 다음 요청을 받을 때 지난 요청의 마감이 남는다.
        _current.reset(token)


def current() -> Optional[Deadline]:
    """지금 걸려 있는 마감. 없으면(배치·테스트 준비 코드) None."""
    return _current.get()


def timeout_for(preferred: float) -> float:
    """호출 한 번에 줄 타임아웃. 마감이 없으면 원래 값을 그대로 쓴다."""
    deadline = _current.get()
    return preferred if deadline is None else deadline.timeout(preferred)


def can_retry(backoff: float) -> bool:
    """쉬었다가 다시 시도해도 되는가. 마감이 없으면 원래 재시도 규칙만 따른다."""
    deadline = _current.get()
    return True if deadline is None else deadline.allows_retry(backoff)
