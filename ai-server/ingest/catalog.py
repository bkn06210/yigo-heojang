"""수집 가능한 약관 목록을 모아 파일로 남긴다.

문서를 내려받기 전에 무엇이 있는지 먼저 본다. 목록 조회는 요청 수십 번이면 끝나는데
문서를 받는 일은 수백 MB라, 무엇을 받을지 정하고 시작하는 편이 훨씬 싸다.

같은 카드의 개정판이 여러 번 올라오는 카드사가 있어 최신 것만 남긴다.
목록이 최신순으로 오므로 먼저 나온 것이 최신이다.
"""

import json
import sys
from pathlib import Path
from typing import Dict, List

from .collect.base import Collector, DocumentRef
from .collect.kb import KbCollector
from .collect.samsung import SamsungCollector
from .collect.shinhan import ShinhanCollector

_OUTPUT_PATH = Path(__file__).resolve().parent / "out" / "catalog.json"


def collectors() -> List[Collector]:
    return [SamsungCollector(), ShinhanCollector(), KbCollector()]


def build_catalog() -> Dict[str, List[dict]]:
    catalog: Dict[str, List[dict]] = {}

    for collector in collectors():
        seen = set()
        rows: List[dict] = []
        total = 0

        try:
            for ref in collector.list_documents():
                total += 1
                key = (ref.card_name, ref.doc_type)
                if key in seen:
                    continue
                seen.add(key)
                rows.append(_to_row(ref))
        except Exception as error:
            # 카드사 한 곳이 막혀도 나머지는 남겨야 한다.
            print(f"[{collector.issuer}] 목록 조회 실패: {error}", file=sys.stderr)

        catalog[collector.issuer] = rows
        print(f"[{collector.issuer}] 전체 {total}건 → 카드 기준 {len(rows)}건")

    return catalog


def _to_row(ref: DocumentRef) -> dict:
    return {
        "card_name": ref.card_name,
        "doc_type": ref.doc_type,
        "doc_key": ref.doc_key,
        "source_url": ref.source_url,
        "revised_at": ref.revised_at,
        "file_name": ref.file_name,
    }


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    catalog = build_catalog()

    _OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    _OUTPUT_PATH.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"저장: {_OUTPUT_PATH}")


if __name__ == "__main__":
    main()
