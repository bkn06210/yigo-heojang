"""요청 마감시간.

마감이 없는 곳(색인 배치)은 원래 동작이 그대로여야 하고, 마감이 있으면 남은 시간이
타임아웃과 재시도를 함께 묶어야 한다.
"""

import pytest

import deadline
from deadline import DeadlineExceeded, can_retry, request_deadline, timeout_for


def test_마감이_없으면_원래_타임아웃과_재시도를_그대로_쓴다():
    assert deadline.current() is None
    assert timeout_for(10.0) == 10.0
    assert can_retry(2.0)


def test_남은_시간이_더_짧으면_그쪽을_타임아웃으로_준다():
    with request_deadline(3.0):
        assert timeout_for(10.0) <= 3.0
        # 원래 값이 더 짧으면 원래 값을 쓴다. 마감은 줄이기만 하지 늘리지 않는다.
        assert timeout_for(1.5) == 1.5


def test_남은_시간이_모자라면_보내지_않는다():
    # 응답이 올 수 없는 시간에 보내면 상대 서버에 일만 시키고 결과는 버려진다.
    with request_deadline(deadline.MIN_ATTEMPT_SECONDS / 2), pytest.raises(DeadlineExceeded):
        timeout_for(10.0)


def test_쉬고_나서_한_번_더_시도할_시간이_없으면_재시도하지_않는다():
    with request_deadline(2.5):
        assert can_retry(1.0)  # 쉬고 나도 1.5초 남는다
        assert not can_retry(2.0)  # 쉬고 나면 0.5초뿐이다


def test_블록을_나오면_마감이_풀린다():
    # 풀리지 않으면 같은 스레드가 받는 다음 요청에 지난 요청의 마감이 남는다.
    with request_deadline(0.1):
        pass

    assert deadline.current() is None
    assert timeout_for(10.0) == 10.0
