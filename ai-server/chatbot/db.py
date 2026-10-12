"""MySQL 연결.

챗봇이 DB를 직접 읽는 범위는 좁다. 별칭·가맹점·카드 이름 조회와 약관 원문뿐이다.
할인액·달성률 같은 계산값은 엔진 API로 받는다 — 규칙이 두 곳에 생기지 않게 하려는 것이다.

수집 파이프라인(ingest)과 접속 정보를 공유한다. 같은 DB라 키를 나눌 이유가 없다.
"""

from contextlib import contextmanager
import os
from pathlib import Path
from typing import Dict

import deadline
import pymysql
from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"

# 같은 기기·같은 네트워크의 DB 라 정상이면 바로 붙는다.
_CONNECT_TIMEOUT_SECONDS = 3.0
# 챗봇의 조회는 별칭·이름 찾기와 약관 색인 적재뿐이라 밀리초~수십 밀리초에 끝난다.
_QUERY_TIMEOUT_SECONDS = 5.0


@contextmanager
def connection():
    """요청 하나에 연결 하나. 끝나면 반드시 닫는다.

    커넥션 풀을 두지 않는 것은 규모 판단이다. 챗봇은 사람이 말을 입력하는 속도로
    요청이 오고, 한 질문에 조회가 몇 번뿐이라 연결 비용이 문제가 되지 않는다.
    """
    load_dotenv(_ENV_PATH)
    conn = pymysql.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", ""),
        database=os.getenv("DB_NAME", "wallet"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        **_timeouts(),
    )
    try:
        yield conn
    finally:
        conn.close()


def _timeouts() -> Dict[str, float]:
    """요청 마감이 걸려 있으면 DB 를 기다리는 시간도 그 안으로 묶는다.

    pymysql 은 읽기 타임아웃 기본값이 없어(무한) DB 가 멈추면 요청이 끝나지 않는다.
    읽기 타임아웃은 연결할 때 한 번 정해지므로 마감까지 남은 시간이 아니라 조회 한 번의
    상한으로 준다. 마감이 없는 곳(테스트 준비 코드 등)은 원래 동작을 그대로 둔다.
    """
    current = deadline.current()
    if current is None:
        return {}
    query_timeout = current.timeout(_QUERY_TIMEOUT_SECONDS)
    return {
        "connect_timeout": current.timeout(_CONNECT_TIMEOUT_SECONDS),
        "read_timeout": query_timeout,
        "write_timeout": query_timeout,
    }
