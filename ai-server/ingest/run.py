"""선정한 카드의 약관과 카드사 개인회원 약관을 내려받아 DB에 적재한다.

수집 대상은 selection.json(카드)과 member_terms.json(개인회원 약관)에 적는다.
목록 전체를 받는 대신 고른 것만 받는 이유는, 카드사가 공시하는 문서가 1,400건이 넘고
그중 대부분이 법인·제휴·단종 카드라 추천에도 스키마 검증에도 쓰이지 않기 때문이다.

두 종류를 함께 받는 이유는 답할 수 있는 질문이 다르기 때문이다. 카드별 상품설명서에는
혜택·연회비·청구 조건이 있지만 분실·도난·해지 조항은 거의 없다. 그쪽은 카드사 단위
개인회원 약관에 있고, 카드가 몇 장이든 카드사마다 문서 수가 그대로다.

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
_MEMBER_TERMS_PATH = Path(__file__).resolve().parent / "member_terms.json"


def load_selection() -> Dict[str, Set[str]]:
    return _load_names(_SELECTION_PATH)


def load_member_terms() -> Dict[str, Set[str]]:
    return _load_names(_MEMBER_TERMS_PATH)


def _load_names(path: Path) -> Dict[str, Set[str]]:
    """카드사별 수집 대상 이름. 밑줄로 시작하는 키는 설명이라 건너뛴다."""
    raw = json.loads(path.read_text(encoding="utf-8"))
    return {key: set(value) for key, value in raw.items() if not key.startswith("_")}


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    selection = load_selection()
    member_terms = load_member_terms()
    conn = connect()
    missing: List[str] = []

    try:
        for collector in collectors():
            missing.extend(
                _collect(collector, collector.list_documents(), selection, conn)
            )
            missing.extend(
                _collect(collector, collector.list_member_terms(), member_terms, conn)
            )
    finally:
        conn.close()

    if missing:
        print("\n목록에서 찾지 못한 문서:")
        for name in missing:
            print(f"  - {name}")


def _collect(collector, refs, selection: Dict[str, Set[str]], conn) -> List[str]:
    """고른 이름에 해당하는 문서만 받아 적재하고, 못 찾은 이름을 돌려준다."""
    wanted = selection.get(collector.issuer)
    if not wanted:
        return []

    found: Set[str] = set()
    for ref in refs:
        if ref.card_name not in wanted:
            continue
        found.add(ref.card_name)
        status = load_document(collector, ref, conn)
        mark = "적재" if status == LOADED else "건너뜀"
        print(f"[{collector.issuer}] {mark} {ref.card_name} ({ref.doc_type})")

    return [f"{collector.issuer} / {name}" for name in sorted(wanted - found)]


if __name__ == "__main__":
    main()
