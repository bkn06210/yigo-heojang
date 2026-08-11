"""선정한 카드의 약관을 내려받아 DB에 적재한다.

수집 대상은 selection.json에 적는다. 목록 전체를 받는 대신 고른 것만 받는 이유는,
카드사가 공시하는 문서가 1,400건이 넘고 그중 대부분이 법인·제휴·단종 카드라
추천에도 스키마 검증에도 쓰이지 않기 때문이다.

이름이 목록에 없으면 조용히 넘기지 않고 끝에 모아서 알린다.
카드사가 표기를 바꾸면 수집이 소리 없이 줄어드는 것을 막기 위해서다.
"""

import json
import sys
from pathlib import Path
from typing import Dict, List, Set

from .catalog import collectors
from .load import LOADED, SKIPPED, connect, load_document

_SELECTION_PATH = Path(__file__).resolve().parent / "selection.json"


def load_selection() -> Dict[str, Set[str]]:
    raw = json.loads(_SELECTION_PATH.read_text(encoding="utf-8"))
    return {key: set(value) for key, value in raw.items() if not key.startswith("_")}


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    selection = load_selection()
    conn = connect()
    missing: List[str] = []

    try:
        for collector in collectors():
            wanted = selection.get(collector.issuer)
            if not wanted:
                continue

            found: Set[str] = set()
            for ref in collector.list_documents():
                if ref.card_name not in wanted:
                    continue
                found.add(ref.card_name)
                status = load_document(collector, ref, conn)
                mark = "적재" if status == LOADED else "건너뜀"
                print(f"[{collector.issuer}] {mark} {ref.card_name} ({ref.doc_type})")

            missing.extend(
                f"{collector.issuer} / {name}" for name in sorted(wanted - found)
            )
    finally:
        conn.close()

    if missing:
        print("\n목록에서 찾지 못한 카드:")
        for name in missing:
            print(f"  - {name}")


if __name__ == "__main__":
    main()
