"""구조화 산출물이 시드 생성기가 받아들일 모양인지 검사한다.

<b>여기서 걸러내지 않으면 조용히 사라진다.</b> seed.py 는 카테고리·가맹점을 못 찾으면
경고 한 줄을 남기고 그 행을 건너뛴다. 카드 한 장에 혜택이 수십 개라 경고가 묻히고,
결과 SQL 은 정상으로 보이며, 그 카드에서만 혜택이 빠진 채 추천이 돌아간다.

가맹점은 이름이 마스터와 글자까지 같아야 한다. 표기가 조금 다르면(S-Oil / S-OIL)
못 찾은 것으로 끝나지 않고 마스터에 같은 브랜드가 두 개 생기는 쪽으로 번진다.

    python -m ingest.check_structured            전체 검사
    python -m ingest.check_structured 롯데_      이름이 이 문자열로 시작하는 것만
"""

import json
import sys
from pathlib import Path
from typing import Dict, List, Set

from .load import connect

_STRUCTURED_DIR = Path(__file__).resolve().parent / "out" / "structured"
_MERCHANTS_PATH = Path(__file__).resolve().parent / "out" / "merchants.json"


def _category_codes() -> Set[str]:
    conn = connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT category_code FROM category")
            return {row[0] for row in cursor.fetchall()}
    finally:
        conn.close()


def _merchant_names() -> Set[str]:
    rows = json.loads(_MERCHANTS_PATH.read_text(encoding="utf-8"))
    return {row["merchant_name"] for row in rows}


def _targets_of(benefit: dict) -> List[str]:
    """대상 이름 목록. targets 가 있으면 그쪽이 대상이고, 없으면 단일 필드다."""
    targets = benefit.get("targets")
    if targets:
        return list(targets)

    if benefit.get("target_type") == "CATEGORY":
        return [benefit.get("target_category_code")]
    if benefit.get("target_type") == "MERCHANT":
        return [benefit.get("target_merchant_name")]
    return []


def _exclusion_problems(exclusions, label, categories: Set[str], merchants: Set[str]) -> List[str]:
    """제외 목록이 가리키는 카테고리·가맹점이 실재하는지."""
    problems: List[str] = []
    for exclusion in exclusions or []:
        if not isinstance(exclusion, dict):
            continue
        kind = exclusion.get("exclusion_type")
        value = exclusion.get("exclusion_value")
        if kind == "CATEGORY" and value not in categories:
            problems.append(f"제외 카테고리 「{value}」 — {label}")
        elif kind == "MERCHANT" and value not in merchants:
            problems.append(f"제외 가맹점 「{value}」 — {label}")
    return problems


def check(path: Path, categories: Set[str], merchants: Set[str]) -> Dict[str, object]:
    data = json.loads(path.read_text(encoding="utf-8"))
    problems: List[str] = []
    rows = 0

    for benefit in data.get("benefits", []):
        target_type = benefit.get("target_type")
        names = _targets_of(benefit)
        rows += max(len(names), 1)

        for name in names:
            label = benefit.get("benefit_name")
            if target_type == "CATEGORY" and name not in categories:
                problems.append(f"카테고리 「{name}」 — {label}")
            elif target_type == "MERCHANT" and name not in merchants:
                problems.append(f"가맹점 「{name}」 — {label}")

        problems.extend(_exclusion_problems(
            benefit.get("exclusions"), benefit.get("benefit_name"), categories, merchants))

    # 카드 전체에 걸리는 제외도 같은 값을 참조한다. 대상만 검사하면
    # "제외하려던 가맹점을 못 찾아 제외가 통째로 빠지는" 경우를 놓친다 —
    # 그러면 약관이 배제한 곳에서 혜택이 나간다.
    for key in ("card_exclusions", "performance_exclusions"):
        problems.extend(_exclusion_problems(data.get(key), key, categories, merchants))

    return {
        "benefits": len(data.get("benefits", [])),
        "rows": rows,
        "tiers": len(data.get("performance_tiers", [])),
        "gaps": len(data.get("_schema_gap", [])),
        "problems": problems,
    }


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    prefix = sys.argv[1] if len(sys.argv) > 1 else ""

    categories = _category_codes()
    merchants = _merchant_names()
    paths = sorted(p for p in _STRUCTURED_DIR.glob("*.json") if p.stem.startswith(prefix))

    total_rows = total_problems = 0
    for path in paths:
        result = check(path, categories, merchants)
        total_rows += result["rows"]
        total_problems += len(result["problems"])

        mark = "OK" if not result["problems"] else "!!"
        print(f'[{mark}] {path.stem[:34]:36} {result["benefits"]:>3}묶음 {result["rows"]:>4}행'
              f' {result["tiers"]:>2}구간 갭{result["gaps"]:>3}')
        for problem in result["problems"]:
            print(f"       {problem}")

    print(f"\n카드 {len(paths)}장 · {total_rows}행 · 미매핑 {total_problems}건")
    if total_problems:
        sys.exit(1)


if __name__ == "__main__":
    main()
