"""선정한 카드의 약관과 카드사 개인회원 약관을 내려받아 DB에 적재한다.

수집 대상은 selection.json(카드)과 member_terms.json(개인회원 약관)에 적는다.
목록 전체를 받는 대신 고른 것만 받는 이유는, 카드사가 공시하는 문서가 1,400건이 넘고
그중 대부분이 법인·제휴·단종 카드라 추천에도 스키마 검증에도 쓰이지 않기 때문이다.

두 종류를 함께 받는 이유는 답할 수 있는 질문이 다르기 때문이다. 카드별 상품설명서에는
혜택·연회비·청구 조건이 있지만 분실·도난·해지 조항은 거의 없다. 그쪽은 카드사 단위
개인회원 약관에 있고, 카드가 몇 장이든 카드사마다 문서 수가 그대로다.

문서 하나가 실패해도 나머지는 계속 받는다. 카드사 사이트가 잠깐 느리거나 화면 구조가
바뀌면 그 문서만 못 받으면 되는데, 거기서 멈추면 뒤에 있는 문서가 통째로 안 받아진다.
대신 실패를 조용히 넘기지 않고 끝에 목록으로 모아 알리고, 하나라도 실패하면 종료 코드를
1로 준다 — 사람이 화면을 안 보고 있어도 실패를 알 수 있어야 한다.

이름이 목록에 없는 것도 같은 방식으로 알린다.
카드사가 표기를 바꾸면 수집이 소리 없이 줄어드는 것을 막기 위해서다.
"""

import json
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Set

from .catalog import collectors
from .collect.base import DocumentRef
from .load import LOADED, SKIPPED, connect, load_document

_SELECTION_PATH = Path(__file__).resolve().parent / "selection.json"
_MEMBER_TERMS_PATH = Path(__file__).resolve().parent / "member_terms.json"


@dataclass
class RunReport:
    """수집 한 번의 결과.

    성공 수만 세지 않는다. "몇 건 받았다"만 보면 원래 몇 건이어야 했는지를 알 수 없어,
    조용히 줄어든 수집을 알아챌 수 없다.
    """

    loaded: int = 0
    skipped: int = 0
    #  카드사가 표기를 바꿔 목록에서 못 찾은 것
    missing: List[str] = field(default_factory=list)
    #  받다가 실패한 것. (문서 이름, 사유)
    failed: List[tuple] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return not self.missing and not self.failed

    def merge(self, other: "RunReport") -> None:
        self.loaded += other.loaded
        self.skipped += other.skipped
        self.missing.extend(other.missing)
        self.failed.extend(other.failed)


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
    report = run()
    _print_report(report)
    # 실패가 있으면 0이 아닌 코드로 끝낸다. 자동으로 돌릴 때 이 값으로 성공 여부를 가린다.
    sys.exit(0 if report.ok else 1)


def run() -> RunReport:
    """수집을 한 번 돌리고 결과를 돌려준다.

    main 과 나눠 둔 것은 화면 출력과 종료 코드가 없어야 다른 곳에서 부를 수 있기 때문이다
    (관리 화면·스케줄러가 생기면 이 함수를 부른다).
    """
    selection = load_selection()
    member_terms = load_member_terms()
    conn = connect()
    report = RunReport()

    try:
        for collector in collectors():
            report.merge(
                _collect(collector, collector.list_documents, selection, conn)
            )
            report.merge(
                _collect(collector, collector.list_member_terms, member_terms, conn)
            )
    finally:
        conn.close()

    return report


def _collect(collector, list_refs: Callable[[], Iterable[DocumentRef]],
             selection: Dict[str, Set[str]], conn) -> RunReport:
    """고른 이름에 해당하는 문서만 받아 적재한다.

    문서 하나가 실패해도 나머지를 계속 받는다. 예외를 넓게 잡는 이유는 실패하는 방식이
    한둘이 아니기 때문이다 — 카드사 응답 오류, 화면 구조 변경, 본문 없음, DB 오류.
    무엇이 터지든 그 문서만 실패로 적고 다음으로 넘어가는 편이, 원인별로 잡다가
    빠뜨린 하나 때문에 수집 전체가 멈추는 것보다 낫다.

    목록 조회 자체가 실패하면 그 카드사 문서를 하나도 못 받으므로 따로 적는다.

    목록을 만드는 함수를 받아 여기서 부르는 이유는, 호출을 인자 자리에서 하면 아래 try
    밖에서 실행되기 때문이다. 그러면 카드사 한 곳의 목록 조회가 터질 때 그 카드사만
    실패로 남는 게 아니라 수집 전체가 멈추고, 아직 차례가 오지 않은 카드사는 통째로 빠진다.
    """
    wanted = selection.get(collector.issuer)
    if not wanted:
        return RunReport()

    report = RunReport()
    found: Set[str] = set()

    try:
        document_refs = list(list_refs())
    except Exception as error:
        report.failed.append((f"{collector.issuer} / 목록 조회", _reason(error)))
        return report

    for ref in document_refs:
        if ref.card_name not in wanted:
            continue
        found.add(ref.card_name)

        label = f"{collector.issuer} / {ref.card_name} ({ref.doc_type})"
        try:
            status = load_document(collector, ref, conn)
        except Exception as error:
            report.failed.append((label, _reason(error)))
            print(f"[{collector.issuer}] 실패 {ref.card_name} ({ref.doc_type}) — {_reason(error)}")
            continue

        if status == LOADED:
            report.loaded += 1
        else:
            report.skipped += 1
        mark = "적재" if status == LOADED else "건너뜀"
        print(f"[{collector.issuer}] {mark} {ref.card_name} ({ref.doc_type})")

    report.missing.extend(f"{collector.issuer} / {name}" for name in sorted(wanted - found))
    return report


def _reason(error: Exception) -> str:
    """실패 사유 한 줄. 예외 종류를 함께 남긴다 — 메시지만으로는 무엇이 터졌는지 모른다."""
    message = str(error).strip().splitlines()[0] if str(error).strip() else ""
    return f"{type(error).__name__}: {message}" if message else type(error).__name__


def _print_report(report: RunReport) -> None:
    print(f"\n적재 {report.loaded}건 · 건너뜀 {report.skipped}건 "
          f"· 실패 {len(report.failed)}건 · 못 찾음 {len(report.missing)}건")

    if report.failed:
        print("\n받다가 실패한 문서:")
        for label, reason in report.failed:
            print(f"  - {label} — {reason}")

    if report.missing:
        print("\n목록에서 찾지 못한 문서:")
        for name in report.missing:
            print(f"  - {name}")


if __name__ == "__main__":
    main()
