"""MySQL 연결.

챗봇이 DB를 직접 읽는 범위는 좁다. 별칭·가맹점·카드 이름 조회와 약관 원문뿐이다.
할인액·달성률 같은 계산값은 엔진 API로 받는다 — 규칙이 두 곳에 생기지 않게 하려는 것이다.

수집 파이프라인(ingest)과 접속 정보를 공유한다. 같은 DB라 키를 나눌 이유가 없다.
"""

from contextlib import contextmanager
import os
from pathlib import Path

import pymysql
from dotenv import load_dotenv

_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"


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
    )
    try:
        yield conn
    finally:
        conn.close()
