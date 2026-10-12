"""
run_matrix.py 결과(JSON 줄)를 조건별 중앙값 표로 묶는다.

중앙값을 쓰는 것은 같은 PC의 다른 작업이 한 회차만 흔들어도 평균은 끌려가지만 중앙값은 버티기
때문이다. 흔들림의 크기를 함께 보이려고 TPS 는 회차별 최소~최대도 적는다.

    python summarize.py results/recommend.jsonl
    python summarize.py results/pool-size.jsonl --base pool10 --new pool30
"""
from __future__ import annotations

import argparse
import json
import statistics
from collections import defaultdict


def cards_of(email: str) -> int:
    # lt-c20@loadtest.local → 20
    return int(email.split("@")[0].split("-c")[-1])


def median(values):
    values = [value for value in values if value is not None]
    return statistics.median(values) if values else None


def load(paths: list[str]) -> dict:
    groups: dict[tuple, list[dict]] = defaultdict(list)
    for path in paths:
        with open(path, encoding="utf-8") as file:
            for line in file:
                if line.strip():
                    result = json.loads(line)
                    key = (result["endpoint"], cards_of(result["email"]), result["concurrency"], result["version"])
                    groups[key].append(result)
    return groups


def summarize(results: list[dict]) -> dict:
    return {
        "n": len(results),
        "tps": median(r["tps"] for r in results),
        "tpsMin": min(r["tps"] for r in results),
        "tpsMax": max(r["tps"] for r in results),
        "p50": median(r["p50Ms"] for r in results),
        "p95": median(r["p95Ms"] for r in results),
        "p99": median(r["p99Ms"] for r in results),
        "errorRate": max(r["errorRate"] or 0 for r in results),
        "select": median(r.get("queries", {}).get("selectPerRequest") for r in results),
        "statements": median(r.get("queries", {}).get("statementsPerRequest") for r in results),
        "dbMs": median(r.get("queries", {}).get("dbTimeMsPerRequest") for r in results),
        "appCpu": median((r.get("cpuPercent") or {}).get("app") for r in results),
        "mysqlCpu": median((r.get("cpuPercent") or {}).get("mysql") for r in results),
        "clientCpu": max(r.get("clientCpuMaxPercent") or 0 for r in results),
        "otherHosts": max(r.get("otherHostsStatementsPerSec") or 0 for r in results),
    }


def change(before, after, higher_is_better: bool) -> str:
    if before in (None, 0) or after is None:
        return "-"
    ratio = after / before
    if higher_is_better:
        return f"{ratio:.2f}배"
    return f"{(1 - ratio) * 100:+.0f}%".replace("+", "−") if ratio < 1 else f"+{(ratio - 1) * 100:.0f}%"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--base", default="before", help="비교 기준 버전 이름")
    parser.add_argument("--new", default="after", help="비교 대상 버전 이름")
    args = parser.parse_args()

    groups = load(args.paths)
    summaries = {key: summarize(results) for key, results in groups.items()}
    endpoints = sorted({key[0] for key in summaries})
    for endpoint in endpoints:
        print(f"\n## {endpoint}\n")
        print("| 카드 | 동시 | 버전 | 회차 | SELECT/요청 | DB ms/요청 | TPS (최소~최대) | p50 ms | p95 ms | p99 ms "
              "| 에러율 | 톰캣 CPU% | MySQL CPU% | 도구 CPU% | 타 호스트 문장/s |")
        print("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        keys = sorted({(key[1], key[2]) for key in summaries if key[0] == endpoint})
        for cards, concurrency in keys:
            for version in (args.base, args.new):
                s = summaries.get((endpoint, cards, concurrency, version))
                if s is None:
                    continue
                print(f"| {cards} | {concurrency} | {version} | {s['n']} | {s['select']} | {s['dbMs']} "
                      f"| {s['tps']:.1f} ({s['tpsMin']:.0f}~{s['tpsMax']:.0f}) | {s['p50']:.1f} | {s['p95']:.1f} "
                      f"| {s['p99']:.1f} | {s['errorRate']:.2%} | {s['appCpu']} | {s['mysqlCpu']} "
                      f"| {s['clientCpu']} | {s['otherHosts']} |")
        print("\n| 카드 | 동시 | TPS 변화 | p50 변화 | p95 변화 | p99 변화 | SELECT/요청 |")
        print("|---|---|---|---|---|---|---|")
        for cards, concurrency in keys:
            b = summaries.get((endpoint, cards, concurrency, args.base))
            a = summaries.get((endpoint, cards, concurrency, args.new))
            if b is None or a is None:
                continue
            print(f"| {cards} | {concurrency} | {change(b['tps'], a['tps'], True)} "
                  f"| {change(b['p50'], a['p50'], False)} | {change(b['p95'], a['p95'], False)} "
                  f"| {change(b['p99'], a['p99'], False)} | {b['select']} → {a['select']} |")


if __name__ == "__main__":
    main()
