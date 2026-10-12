"""
엔진 조회 API 부하 측정 — 처리량·응답시간 분위수·에러율·요청당 쿼리 수.

닫힌 루프(closed loop) 방식이다. 가상 사용자 N명이 각자 연결 하나를 쥐고, 응답을 받자마자
다음 요청을 보낸다(생각 시간 없음). 그래서 동시 사용자 수 = 서버에 동시에 걸려 있는 요청 수이고,
그 동시성에서 서버가 낼 수 있는 최대 처리량을 잰다.

HTTP 클라이언트를 표준 라이브러리(asyncio 스트림)로 직접 쓴다. 범용 클라이언트(httpx)는 동시 연결이
100개쯤 되면 요청마다 연결 풀을 관리하는 비용이 커져, 서버보다 부하 도구가 먼저 한 코어를 다 쓰고
처리량을 막았다. 그 상태의 숫자는 서버 성능이 아니라 도구 성능이다. 그래서 요청을 한 번의 쓰기로
보내고 응답만 읽는 최소 구현을 쓰고, 가상 사용자를 여러 프로세스로 나눌 수 있게 했다.
결과의 clientCpuMaxPercent 가 100에 가까우면 --processes 를 늘린다.

요청당 쿼리 수는 MySQL performance_schema 의 호스트별 문장 집계를 측정 구간 앞뒤로 읽어 뺀다.
호스트(=앱 서버의 IP)로 거르는 것은 같은 DB를 쓰는 다른 프로세스(테스트 등)의 문장이
섞이지 않게 하려는 것이다. 앞뒤를 읽는 동안에는 모든 가상 사용자가 멈춰 있어 경계에 걸친 요청이 없다.

    python load_test.py --email lt-c10@loadtest.local --concurrency 50 --duration 15 \
        --app-db-host 172.18.0.3 --label before --out results.jsonl

필요 패키지: pymysql (ai-server 가상환경에 있다). 쿼리 수를 재지 않으면 표준 라이브러리만 쓴다.
"""
from __future__ import annotations

import argparse
import asyncio
import json
import math
import multiprocessing
import socket
import time
import urllib.request
from datetime import datetime
from urllib.parse import urlsplit

DEFAULT_PASSWORD = "LoadTest!2026"


# ── HTTP ────────────────────────────────────────────────────────────────────

class Connection:
    """가상 사용자 한 명이 쥐는 keep-alive 연결. 끊기면 다음 요청 때 다시 연다."""

    def __init__(self, host: str, port: int, token: str, timeout: float) -> None:
        self.host = host
        self.port = port
        self.timeout = timeout
        self.fixed_headers = f"Host: {host}:{port}\r\nAuthorization: Bearer {token}\r\n"
        self.reader: asyncio.StreamReader | None = None
        self.writer: asyncio.StreamWriter | None = None

    async def request(self, method: str, path: str, body: bytes | None) -> tuple[int, bytes]:
        try:
            return await asyncio.wait_for(self._request(method, path, body), self.timeout)
        except BaseException:
            # 응답을 덜 읽은 연결은 다음 요청의 응답과 섞이므로 버린다
            self.close()
            raise

    async def _request(self, method: str, path: str, body: bytes | None) -> tuple[int, bytes]:
        if self.writer is None:
            self.reader, self.writer = await asyncio.open_connection(self.host, self.port)
            self.writer.get_extra_info("socket").setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

        head = f"{method} {path} HTTP/1.1\r\n{self.fixed_headers}"
        if body is not None:
            head += f"Content-Type: application/json\r\nContent-Length: {len(body)}\r\n"
        # 헤더와 본문을 한 번에 쓴다. 나눠 쓰면 TCP 지연 ACK 와 맞물려 요청마다 수십 ms 가 붙을 수 있다
        self.writer.write((head + "\r\n").encode() + (body or b""))
        await self.writer.drain()

        header_block = await self.reader.readuntil(b"\r\n\r\n")
        status = int(header_block[9:12])
        lowered = header_block.lower()
        if b"transfer-encoding: chunked" in lowered:
            chunks = []
            while True:
                size = int((await self.reader.readuntil(b"\r\n")).split(b";", 1)[0], 16)
                if size == 0:
                    await self.reader.readuntil(b"\r\n")
                    break
                chunks.append((await self.reader.readexactly(size + 2))[:-2])
            content = b"".join(chunks)
        else:
            marker = lowered.find(b"content-length:")
            length = int(lowered[marker + 15:lowered.find(b"\r\n", marker)]) if marker >= 0 else 0
            content = await self.reader.readexactly(length)
        if b"connection: close" in lowered:
            self.close()
        return status, content

    def close(self) -> None:
        if self.writer is not None:
            self.writer.close()
        self.reader = self.writer = None


def login(base_url: str, email: str, password: str) -> str:
    request = urllib.request.Request(
        f"{base_url}/api/auth/login", method="POST",
        data=json.dumps({"email": email, "password": password}).encode(),
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read())["data"]["accessToken"]


def build_request(options: dict, sequence: int) -> tuple[str, str, bytes | None]:
    """요청 하나. 가맹점을 순서대로 돌려 전후 측정이 같은 요청 분포를 받게 한다."""
    if options["endpoint"] == "status":
        return "GET", "/api/cards/monthly-status", None
    merchant_id = options["merchant_ids"][sequence % len(options["merchant_ids"])]
    body = json.dumps({"merchantId": merchant_id, "expectedAmount": options["amount"]}).encode()
    return "POST", "/api/recommendations", body


# ── 부하 발생(자식 프로세스) ────────────────────────────────────────────────

async def run_phase(connections: list[Connection], options: dict, seconds: float,
                    sequence_offset: int, record: bool) -> dict:
    """seconds 동안 가상 사용자들이 요청을 반복한다. 모든 가상 사용자가 끝나야 돌아온다."""
    deadline = time.perf_counter() + seconds
    counter = {"sent": sequence_offset}
    latencies: list[float] = []
    errors = {"count": 0, "samples": []}

    async def virtual_user(connection: Connection) -> None:
        while time.perf_counter() < deadline:
            method, path, body = build_request(options, counter["sent"])
            counter["sent"] += 1
            started = time.perf_counter()
            try:
                status, content = await connection.request(method, path, body)
                error = None if status == 200 and b'"success":true' in content \
                    else f"{status} {content[:120].decode(errors='replace')}"
            except (OSError, asyncio.IncompleteReadError, asyncio.LimitOverrunError,
                    asyncio.TimeoutError, ValueError) as exception:
                error = f"{type(exception).__name__}: {exception}"
            elapsed_ms = (time.perf_counter() - started) * 1000
            if not record:
                continue
            # 실패 요청도 응답시간에 넣는다 — 빼면 느려서 실패한 요청이 분위수에서 사라진다
            latencies.append(elapsed_ms)
            if error is not None:
                errors["count"] += 1
                if len(errors["samples"]) < 5:
                    errors["samples"].append(error)

    await asyncio.gather(*(virtual_user(connection) for connection in connections))
    return {"latencies": latencies, "errors": errors["count"], "errorSamples": errors["samples"]}


def worker_main(index: int, users: int, options: dict, token: str, barrier, results) -> None:
    results.put(asyncio.run(worker_async(index, users, options, token, barrier)))


async def worker_async(index: int, users: int, options: dict, token: str, barrier) -> dict:
    connections = [Connection(options["host"], options["port"], token, options["timeout"])
                   for _ in range(users)]
    # 프로세스마다 가맹점 순서를 어긋나게 시작해 같은 요청이 한꺼번에 몰리지 않게 한다
    offset = index * 7919
    if options["warmup"] > 0:
        await run_phase(connections, options, options["warmup"], offset, record=False)

    barrier.wait()  # 모두 예열을 마쳤다 — 부모가 DB 카운터를 읽는다
    barrier.wait()  # 측정 시작
    cpu_started = time.process_time()
    started = time.perf_counter()
    phase = await run_phase(connections, options, options["duration"], offset, record=True)
    phase["cpuSeconds"] = time.process_time() - cpu_started
    phase["wallSeconds"] = time.perf_counter() - started
    barrier.wait()  # 모두 끝났다 — 부모가 DB 카운터를 다시 읽는다

    for connection in connections:
        connection.close()
    return phase


# ── DB 카운터 ────────────────────────────────────────────────────────────────

def mysql_connection(args: argparse.Namespace):
    import pymysql

    return pymysql.connect(host=args.mysql_host, port=args.mysql_port, user=args.mysql_user,
                           password=args.mysql_password, database="performance_schema")


def read_statement_counts(args: argparse.Namespace) -> dict[str, tuple[int, int]]:
    """앱 서버 호스트가 지금까지 실행한 문장 수와 누적 실행 시간(피코초), 이벤트 종류별."""
    connection = mysql_connection(args)
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT EVENT_NAME, COUNT_STAR, SUM_TIMER_WAIT"
                "  FROM events_statements_summary_by_host_by_event_name"
                " WHERE HOST = %s AND COUNT_STAR > 0",
                (args.app_db_host,))
            return {name: (int(count), int(wait)) for name, count, wait in cursor.fetchall()}
    finally:
        connection.close()


def read_other_hosts_statements(args: argparse.Namespace) -> int:
    """
    앱 서버가 아닌 호스트들이 지금까지 실행한 문장 수 합계.

    측정 구간에 이 값이 크게 늘었다면 같은 DB에 다른 부하(테스트 실행 등)가 겹친 것이라,
    그 회차의 숫자는 흔들렸다고 보고 해석한다.
    """
    connection = mysql_connection(args)
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT COALESCE(SUM(COUNT_STAR), 0)"
                "  FROM events_statements_summary_by_host_by_event_name"
                " WHERE HOST IS NOT NULL AND HOST <> %s",
                (args.app_db_host,))
            return int(cursor.fetchone()[0])
    finally:
        connection.close()


def read_digests(args: argparse.Namespace) -> dict[str, tuple[str, int, int, int]]:
    """
    쿼리 유형(digest)별 누적 실행 수·시간·읽은 행 수.

    이 표는 호스트 구분이 없어 같은 DB를 쓰는 다른 프로세스의 쿼리가 섞일 수 있다.
    그래서 요청당 수치는 호스트별 표로 내고, 이 표는 "어떤 쿼리가 무거운가"를 보는 데만 쓴다.
    """
    connection = mysql_connection(args)
    try:
        with connection.cursor() as cursor:
            cursor.execute(
                "SELECT DIGEST, LEFT(DIGEST_TEXT, 90), COUNT_STAR, SUM_TIMER_WAIT, SUM_ROWS_EXAMINED"
                "  FROM events_statements_summary_by_digest"
                " WHERE SCHEMA_NAME = 'wallet'")
            return {digest: (text, int(count), int(wait), int(examined))
                    for digest, text, count, wait, examined in cursor.fetchall()}
    finally:
        connection.close()


def top_digests(before: dict, after: dict, requests: int, limit: int) -> list[dict]:
    rows = []
    for digest, (text, count, wait, examined) in after.items():
        _, count0, wait0, examined0 = before.get(digest, (text, 0, 0, 0))
        if count - count0 <= 0:
            continue
        rows.append({
            "query": text,
            "perRequest": round((count - count0) / requests, 2),
            "dbMsPerRequest": round((wait - wait0) / 1e9 / requests, 3),
            "rowsExaminedPerCall": round((examined - examined0) / (count - count0), 1),
        })
    rows.sort(key=lambda row: row["dbMsPerRequest"], reverse=True)
    return rows[:limit]


def per_request_statements(before: dict[str, tuple[int, int]], after: dict[str, tuple[int, int]],
                           requests: int) -> dict:
    """
    측정 구간의 문장 수·DB 실행 시간을 요청 수로 나눈다.

    DB 실행 시간은 MySQL 이 문장을 처리한 시간의 합이다. 응답시간에서 이 값을 빼면
    앱(자바)·네트워크·커넥션 대기에 쓴 시간이 남는다 — 병목이 DB 쪽인지 앱 쪽인지 가르는 기준이다.
    """
    if not requests:
        return {}
    zero = (0, 0)
    diff = {name: (after[name][0] - before.get(name, zero)[0], after[name][1] - before.get(name, zero)[1])
            for name in after}
    diff = {name: value for name, value in diff.items() if value[0] > 0}
    selects = diff.get("statement/sql/select", zero)[0]
    total_count = sum(count for count, _ in diff.values())
    total_wait_ps = sum(wait for _, wait in diff.values())
    return {
        "selectPerRequest": round(selects / requests, 2),
        "statementsPerRequest": round(total_count / requests, 2),
        "dbTimeMsPerRequest": round(total_wait_ps / 1e9 / requests, 3),
        "byEvent": {name.split("/", 1)[1]: round(count / requests, 2)
                    for name, (count, _) in sorted(diff.items())},
    }


# ── 측정 진행(부모 프로세스) ─────────────────────────────────────────────────

def percentile(sorted_values: list[float], ratio: float) -> float:
    """최근접 순위(nearest-rank) 분위수. 보간하지 않아 실제로 관측된 값만 나온다."""
    if not sorted_values:
        return float("nan")
    # round 는 0.95 × 2000 = 1900.0000000000002 같은 부동소수 오차가 올림에 끼지 않게 한다
    rank = max(1, math.ceil(round(ratio * len(sorted_values), 9)))
    return sorted_values[min(rank, len(sorted_values)) - 1]


def measure(args: argparse.Namespace) -> dict:
    token = login(args.base_url, args.email, args.password)
    target = urlsplit(args.base_url)
    options = {
        "host": target.hostname, "port": target.port or 80, "timeout": args.timeout,
        "endpoint": args.endpoint, "amount": args.amount, "merchant_ids": args.merchant_ids,
        "warmup": args.warmup, "duration": args.duration,
    }
    processes = min(args.processes, args.concurrency)
    users = [args.concurrency // processes + (1 if index < args.concurrency % processes else 0)
             for index in range(processes)]

    context = multiprocessing.get_context("spawn")
    barrier = context.Barrier(processes + 1)
    results = context.Queue()
    workers = [context.Process(target=worker_main, args=(index, count, options, token, barrier, results))
               for index, count in enumerate(users)]
    for worker in workers:
        worker.start()

    barrier.wait()  # 예열 끝
    counts_before = read_statement_counts(args) if args.app_db_host else None
    others_before = read_other_hosts_statements(args) if args.app_db_host else None
    digests_before = read_digests(args) if args.digest_top else None
    barrier.wait()  # 측정 시작
    started = time.perf_counter()
    barrier.wait()  # 측정 끝
    elapsed = time.perf_counter() - started
    counts_after = read_statement_counts(args) if args.app_db_host else None
    others_after = read_other_hosts_statements(args) if args.app_db_host else None
    digests_after = read_digests(args) if args.digest_top else None

    phases = [results.get() for _ in workers]
    for worker in workers:
        worker.join()

    latencies = sorted(latency for phase in phases for latency in phase["latencies"])
    errors = sum(phase["errors"] for phase in phases)
    completed = len(latencies)
    result = {
        "at": datetime.now().isoformat(timespec="seconds"),
        "label": args.label,
        "endpoint": args.endpoint,
        "email": args.email,
        "concurrency": args.concurrency,
        "processes": processes,
        "durationSec": round(elapsed, 2),
        "requests": completed,
        "tps": round((completed - errors) / elapsed, 1),
        "p50Ms": round(percentile(latencies, 0.50), 2),
        "p95Ms": round(percentile(latencies, 0.95), 2),
        "p99Ms": round(percentile(latencies, 0.99), 2),
        "maxMs": round(latencies[-1], 2) if latencies else None,
        "errorRate": round(errors / completed, 4) if completed else None,
        "errorSamples": [sample for phase in phases for sample in phase["errorSamples"]][:5],
        # 한 프로세스가 한 코어를 다 쓰면 서버가 아니라 부하 도구가 처리량을 막고 있는 것이다
        "clientCpuMaxPercent": round(max(phase["cpuSeconds"] / phase["wallSeconds"] * 100
                                         for phase in phases), 1),
    }
    if counts_before is not None:
        result["queries"] = per_request_statements(counts_before, counts_after, completed)
        # 측정 PC의 쿼리 도구(이 스크립트)가 카운터를 읽은 문장도 들어가므로 0이 아니라 수 건이 기본이다
        result["otherHostsStatementsPerSec"] = round((others_after - others_before) / elapsed, 1)
    if digests_before is not None and completed:
        result["topDigests"] = top_digests(digests_before, digests_after, completed, args.digest_top)
    return result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    # localhost 가 아니라 127.0.0.1 이다. Windows 에서 localhost 는 IPv6(::1) 경로로 Docker Desktop
    # 포트 프록시를 타는데, 헤더와 본문을 나눠 쓰는 클라이언트의 POST 에서 요청마다 약 45ms 가 붙었다
    # (본문 없는 GET 은 그대로). 서버와 무관한 지연이 응답시간의 대부분을 차지해 비교가 무의미해진다.
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--email", required=True)
    parser.add_argument("--password", default=DEFAULT_PASSWORD)
    parser.add_argument("--endpoint", choices=["recommend", "status"], default="recommend")
    parser.add_argument("--concurrency", type=int, default=10, help="동시 가상 사용자 수")
    parser.add_argument("--processes", type=int, default=1,
                        help="가상 사용자를 나눠 맡을 프로세스 수. 부하 도구가 병목이 되지 않게")
    parser.add_argument("--duration", type=float, default=15, help="측정 구간(초)")
    parser.add_argument("--warmup", type=float, default=5, help="측정 전 버리는 구간(초). JIT·커넥션 예열")
    parser.add_argument("--timeout", type=float, default=30)
    parser.add_argument("--amount", type=int, default=15000, help="예상 결제금액")
    parser.add_argument("--merchant-ids", default="1-225",
                        help="돌려 쓸 가맹점 id. '1-225' 또는 '1,5,9'")
    parser.add_argument("--app-db-host", default=None,
                        help="MySQL이 보는 앱 서버 주소(컨테이너 IP). 주면 요청당 쿼리 수를 잰다")
    parser.add_argument("--mysql-host", default="127.0.0.1")
    parser.add_argument("--mysql-port", type=int, default=3306)
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-password", default="root")
    parser.add_argument("--digest-top", type=int, default=0,
                        help="DB 시간이 큰 쿼리 유형 상위 N개를 함께 낸다(0이면 생략)")
    parser.add_argument("--label", default="")
    parser.add_argument("--out", default=None, help="결과를 JSON 한 줄로 덧붙일 파일")
    args = parser.parse_args()

    if "-" in args.merchant_ids:
        low, high = (int(value) for value in args.merchant_ids.split("-"))
        args.merchant_ids = list(range(low, high + 1))
    else:
        args.merchant_ids = [int(value) for value in args.merchant_ids.split(",")]
    return args


def main() -> None:
    args = parse_args()
    result = measure(args)
    line = json.dumps(result, ensure_ascii=False)
    print(line)
    if args.out:
        with open(args.out, "a", encoding="utf-8") as file:
            file.write(line + "\n")


if __name__ == "__main__":
    main()
