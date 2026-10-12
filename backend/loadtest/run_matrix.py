"""
개선 전·후 서버를 같은 조건으로 번갈아 측정한다.

같은 PC에서 다른 작업(빌드·테스트)이 CPU를 나눠 쓰면 측정값이 흔들린다. 한쪽을 다 재고 나서
다른 쪽을 재면 그 흔들림이 전후 차이로 둔갑하므로, 조건 하나마다 전·후를 붙여서 재고 이를
여러 번 반복해 중앙값을 쓴다. 반복마다 먼저 재는 쪽을 바꿔 순서 효과(예열·캐시)도 상쇄한다.

측정 중간에 컨테이너별 CPU 사용률을 한 번 찍어 둔다. 처리량이 막혔을 때 앱 서버·DB·부하 도구 중
어디가 포화됐는지 가르는 근거다.

    python run_matrix.py --members lt-c02,lt-c10,lt-c20 --concurrency 1,10,50,100 --rounds 3 \
        --out results/recommend.jsonl

대상은 이름=주소@컨테이너 로 준다. 기본값은 개선 전 8080, 개선 후 8081 이다.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import threading
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent


def container_ip(container: str, network: str) -> str:
    output = subprocess.run(
        ["docker", "inspect", container, "--format",
         "{{(index .NetworkSettings.Networks \"" + network + "\").IPAddress}}"],
        capture_output=True, text=True, check=True).stdout.strip()
    if not output:
        raise SystemExit(f"{container} 가 {network} 네트워크에 없다")
    return output


def sample_cpu(containers: list[str]) -> dict[str, float]:
    output = subprocess.run(
        ["docker", "stats", "--no-stream", "--format", "{{.Name}}|{{.CPUPerc}}", *containers],
        capture_output=True, text=True).stdout
    usage = {}
    for line in output.splitlines():
        name, _, percent = line.partition("|")
        if percent.endswith("%"):
            usage[name] = float(percent[:-1])
    return usage


def run_once(args: argparse.Namespace, version: str, base_url: str, container: str, app_db_host: str,
             email: str, concurrency: int, round_number: int) -> dict:
    command = [sys.executable, str(HERE / "load_test.py"),
               "--base-url", base_url, "--email", email, "--endpoint", args.endpoint,
               "--concurrency", str(concurrency), "--processes", str(args.processes),
               "--duration", str(args.duration),
               "--warmup", str(args.warmup), "--app-db-host", app_db_host, "--label", version]

    cpu: dict[str, float] = {}

    def sample_mid_run() -> None:
        # 로그인·예열이 끝나고 측정 구간 한가운데쯤
        time.sleep(1 + args.warmup + args.duration * 0.4)
        cpu.update(sample_cpu([container, args.mysql_container]))

    sampler = threading.Thread(target=sample_mid_run, daemon=True)
    sampler.start()
    completed = subprocess.run(command, capture_output=True, text=True)
    sampler.join(timeout=10)
    if completed.returncode != 0:
        raise SystemExit(f"측정 실패: {' '.join(command)}\n{completed.stderr}")

    result = json.loads(completed.stdout.strip().splitlines()[-1])
    result["version"] = version
    result["round"] = round_number
    result["cpuPercent"] = {"app": cpu.get(container), "mysql": cpu.get(args.mysql_container)}
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--targets", default="before=http://127.0.0.1:8080@loadtest-before,"
                                              "after=http://127.0.0.1:8081@loadtest-after")
    parser.add_argument("--members", default="lt-c02,lt-c10,lt-c20",
                        help="측정 회원 이메일 앞부분(@loadtest.local 생략)")
    parser.add_argument("--concurrency", default="1,10,50,100")
    parser.add_argument("--endpoint", choices=["recommend", "status"], default="recommend")
    parser.add_argument("--processes", type=int, default=1, help="부하 도구 프로세스 수")
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--duration", type=float, default=15)
    parser.add_argument("--warmup", type=float, default=5)
    parser.add_argument("--network", default="backend_default")
    parser.add_argument("--mysql-container", default="yigo-mysql")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    targets = []
    for entry in args.targets.split(","):
        version, _, rest = entry.partition("=")
        base_url, _, container = rest.partition("@")
        targets.append((version, base_url, container, container_ip(container, args.network)))

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    concurrencies = [int(value) for value in args.concurrency.split(",")]
    members = [f"{member}@loadtest.local" for member in args.members.split(",")]

    for round_number in range(1, args.rounds + 1):
        # 짝수 회차는 순서를 뒤집는다 — 늘 먼저 재는 쪽이 유리하거나 불리한 것을 상쇄
        ordered = targets if round_number % 2 == 1 else list(reversed(targets))
        for email in members:
            for concurrency in concurrencies:
                for version, base_url, container, app_db_host in ordered:
                    result = run_once(args, version, base_url, container, app_db_host,
                                      email, concurrency, round_number)
                    with out.open("a", encoding="utf-8") as file:
                        file.write(json.dumps(result, ensure_ascii=False) + "\n")
                    queries = result.get("queries", {})
                    print(f"[{round_number}] {version:6} {email:24} c={concurrency:<3} "
                          f"tps={result['tps']:7.1f} p50={result['p50Ms']:7.1f} p95={result['p95Ms']:7.1f} "
                          f"p99={result['p99Ms']:7.1f} err={result['errorRate']} "
                          f"sel/req={queries.get('selectPerRequest')} db/req={queries.get('dbTimeMsPerRequest')}ms "
                          f"cpu={result['cpuPercent']} client={result['clientCpuMaxPercent']}%", flush=True)


if __name__ == "__main__":
    main()
