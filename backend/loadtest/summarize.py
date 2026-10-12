"""
run_matrix.py 결과(JSON 줄)를 조건별 중앙값 표로 묶는다.

중앙값을 쓰는 것은 같은 PC의 다른 작업이 한 회차만 흔들어도 평균은 끌려가지만 중앙값은 버티기
때문이다. 흔들림의 크기를 함께 보이려고 TPS 는 회차별 최소~최대도 적는다.

배경 부하가 회차마다 다르면 중앙값끼리 나눈 비율도 흔들린다. 그래서 변화 표에는 같은 회차에
붙여 잰 전·후 한 쌍의 비율을 회차마다 구해 그 중앙값을 함께 적는다(짝 비율). 붙여 잰 두 측정은
비슷한 배경 부하를 받으므로, 회차 사이의 흔들림이 비율에서 상쇄된다.

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


def cpu_ms(result: dict, label: str):
    return (result.get("cpuMsPerRequest") or {}).get(label)


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
        "rows": median(r.get("queries", {}).get("rowsSentPerRequest") for r in results),
        "dbMs": median(r.get("queries", {}).get("dbTimeMsPerRequest") for r in results),
        "appCpuMs": median(cpu_ms(r, "app") for r in results),
        "mysqlCpuMs": median(cpu_ms(r, "mysql") for r in results),
        "appCpu": median((r.get("cpuPercent") or {}).get("app") for r in results),
        "mysqlCpu": median((r.get("cpuPercent") or {}).get("mysql") for r in results),
        "othersCpu": median((r.get("cpuPercent") or {}).get("others") for r in results),
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


def paired_ratio(base_results: list[dict], new_results: list[dict], value) -> float | None:
    """같은 회차의 전·후 한 쌍마다 후/전 비율을 구해 그 중앙값을 낸다."""
    base_by_round = {r.get("round"): value(r) for r in base_results}
    ratios = [value(r) / base_by_round[r.get("round")] for r in new_results
              if base_by_round.get(r.get("round")) and value(r) is not None]
    return statistics.median(ratios) if ratios else None


def number(value, digits: int = 1) -> str:
    return "-" if value is None else f"{value:.{digits}f}"


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
        print("| 카드 | 동시 | 버전 | 회차 | SELECT/요청 | 행/요청 | DB ms/요청 | 톰캣 CPU ms/요청 | MySQL CPU ms/요청 "
              "| TPS (최소~최대) | p50 ms | p95 ms | p99 ms | 에러율 | 톰캣 CPU% | MySQL CPU% | 타 컨테이너 CPU% "
              "| 도구 CPU% | 타 호스트 문장/s |")
        print("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        keys = sorted({(key[1], key[2]) for key in summaries if key[0] == endpoint})
        for cards, concurrency in keys:
            for version in (args.base, args.new):
                s = summaries.get((endpoint, cards, concurrency, version))
                if s is None:
                    continue
                print(f"| {cards} | {concurrency} | {version} | {s['n']} | {s['select']} | {s['rows']} | {s['dbMs']} "
                      f"| {number(s['appCpuMs'], 2)} | {number(s['mysqlCpuMs'], 2)} "
                      f"| {s['tps']:.1f} ({s['tpsMin']:.0f}~{s['tpsMax']:.0f}) | {s['p50']:.1f} | {s['p95']:.1f} "
                      f"| {s['p99']:.1f} | {s['errorRate']:.2%} | {s['appCpu']} | {s['mysqlCpu']} "
                      f"| {s['othersCpu']} | {s['clientCpu']} | {s['otherHosts']} |")
        print("\n| 카드 | 동시 | TPS 변화 | TPS 짝 비율 | p50 변화 | p95 변화 | p99 변화 | 톰캣 CPU/요청 | MySQL CPU/요청 "
              "| SELECT/요청 | 행/요청 |")
        print("|---|---|---|---|---|---|---|---|---|---|---|")
        for cards, concurrency in keys:
            b = summaries.get((endpoint, cards, concurrency, args.base))
            a = summaries.get((endpoint, cards, concurrency, args.new))
            if b is None or a is None:
                continue
            paired = paired_ratio(groups[(endpoint, cards, concurrency, args.base)],
                                  groups[(endpoint, cards, concurrency, args.new)], lambda r: r["tps"])
            print(f"| {cards} | {concurrency} | {change(b['tps'], a['tps'], True)} "
                  f"| {'-' if paired is None else f'{paired:.2f}배'} "
                  f"| {change(b['p50'], a['p50'], False)} | {change(b['p95'], a['p95'], False)} "
                  f"| {change(b['p99'], a['p99'], False)} "
                  f"| {change(b['appCpuMs'], a['appCpuMs'], False)} | {change(b['mysqlCpuMs'], a['mysqlCpuMs'], False)} "
                  f"| {b['select']} → {a['select']} | {b['rows']} → {a['rows']} |")


if __name__ == "__main__":
    main()
