"""구조화 입력에서 혜택과 무관한 공통 문구를 걷어낸다.

추출된 약관에는 카드마다 똑같이 붙는 글이 많다. 연체이자율, 부가서비스 변경 사유,
개인신용평점 경고, 해외 수수료 계산식 같은 것들이다. 법령이 요구해 모든 카드에 싣는
글이라 카드가 달라도 한 글자도 다르지 않고, 혜택 규칙을 뽑는 데는 한 줄도 쓰이지 않는다.

그런데도 구조화할 때마다 이 글이 통째로 실려 나간다. KB 문서는 이런 공통 문구가
본문의 6~8할을 차지한다. 카드를 수십 장 늘리면 같은 글을 수십 번 되풀이해 보내는 셈이다.

<b>원문을 고치는 것이 아니다.</b> 걷어낸 결과는 구조화 입력으로만 쓰고,
약관 Q&A가 읽는 card_term_document 에는 원문 그대로 들어간다. 여기서 지우는 연체·해지·
민원 조항은 오히려 약관 질문의 답이 되는 자리라, 양쪽을 같은 기준으로 자르면 안 된다.

<b>지우는 기준은 "반복되느냐"가 아니라 "혜택과 무관하냐"다.</b> 이 둘을 같게 보면
전월 실적 산정 기준이나 공과금 분류처럼 카드사가 모든 카드에 똑같이 싣는 <b>계산에 필요한</b>
문장까지 사라진다. 실제로 반복 문구를 기계로 추려보면 그런 줄이 섞여 나온다.
그래서 지울 문구는 자동 판정이 아니라 아래 목록으로 고정하고, 목록에 없는 것은 남긴다.

    python -m ingest.prune          out/terms → out/pruned (걷어낸 부분은 out/pruned/_removed)
"""

import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Tuple

_ROOT = Path(__file__).resolve().parent
_SOURCE_DIR = _ROOT / "out" / "terms"
_TARGET_DIR = _ROOT / "out" / "pruned"
_REMOVED_DIR = _TARGET_DIR / "_removed"

# ---------------------------------------------------------------------------
# 1. 섹션 단위로 걷어낼 것
#
# KB 문서는 「금융소비자 보호에 관한 법률」 설명서를 카드마다 통째로 싣는다.
# 번호가 붙은 조항 열여섯 개 중 혜택 계산에 쓰이는 것은 연회비 하나뿐이라,
# 줄 단위로 고르는 것보다 조항째 들어내는 쪽이 정확하다.
# ---------------------------------------------------------------------------

# 조항 경계. "1. 카드상품의 개요" 또는 "■ 유사 금융상품과 구별되는 특징" 형태다.
#
# <b>문서 머리글도 경계로 잡는다.</b> 한 카드의 파일에는 상품설명서와 주요거래조건이
# 이어 붙어 있는데, 앞 문서 마지막 조항이 삭제 대상이면 경계를 못 찾아 다음 문서의
# 첫 번호줄까지 통째로 지워진다. 지워지는 쪽이 혜택 상세라 요율·한도·대상이 전부
# 사라지는데, 남은 요약표만으로도 구조화가 되기 때문에 결과가 정상으로 보인다.
_SECTION_BOUNDARY = re.compile(
    r"^\s*(?:###\s*문서|\d{1,2}\.\s*\S|[■◈▶]\s*\S).*$", re.M)

# 이 제목으로 시작하는 조항은 다음 조항 제목이 나올 때까지 통째로 뺀다.
_DROP_SECTIONS = (
    r"\d+\.\s*카드상품의 개요",
    r"\d+\.\s*결제일자에 따른",
    r"\d+\.\s*할부, ?단기카드대출",
    r"\d+\.\s*계약해지 시 불이익",
    r"\d+\.\s*신용카드 이용의 제한",
    r"\d+\.\s*신용 ?점수에 미치는",
    r"\d+\.\s*기한의 이익",
    r"\d+\.\s*일부결제금액이월약정",
    r"\d+\.\s*위법계약해지권",
    r"\d+\.\s*카드이용 등에 관한 이의제기",
    r"\d+\.\s*고객의 이해여부",
    r"\d+\.\s*단기카드대출\(현금서비스\) 금리인하",
    r"\d+\.\s*자료열람요구권",
    r"\d+\.\s*신용카드 해지 신청",
    r"\d+\.\s*개인신용평가대응권",
    r"■\s*유사 금융상품과",
    r"■\s*민원·상담이 빈번",
    r"■\s*신용카드 이용대금 연체",
    r"■\s*민원을 제기하거나",
)

# 삭제 대상처럼 보이지만 남겨야 하는 조항. 연회비 <b>금액</b>이 이 조항의 표에 실린다.
_KEEP_SECTIONS = (r"\d+\.\s*연회비 청구",)

# ---------------------------------------------------------------------------
# 2. 줄 단위로 걷어낼 것
#
# 상품설명서에는 조항 번호가 없어 섹션으로 자를 수 없다. 대신 문구가 카드사 안에서
# 글자 하나 다르지 않게 반복되므로 그 문구로 집는다.
#
# 넓은 낱말(예: "연회비", "수수료")로 잡지 않는다. 그렇게 하면 "총 연회비 기본 연회비
# 제휴 연회비" 같은 <b>금액표 머리글</b>과 "각종 수수료 및 이자 … 이용금액에 해당되지
# 않음" 같은 <b>실적 제외 기준</b>이 함께 걸려 계산 근거가 사라진다.
# 반드시 그 문장에서만 나오는 표현으로 좁힌다.
# ---------------------------------------------------------------------------

_DROP_LINES = (
    # -- 부가서비스 변경 고지 (여신전문금융업 감독규정이 요구하는 문구) --
    "다음과 같은 사유가 발생한 경우 카드사는 부가서비스를 변경",
    "카드사의 휴업",
    "제휴업체의 휴업",
    "제휴업체가 카드사의 의사에 반하여",
    "부가서비스를 3년 이상 제공한 상태에서",
    "수익성이 현저히 낮아진 경우",
    "부가서비스 변경 사유, 변경 내용",
    "고지방법 중 2가지 이상의 방법",
    "고지 방법 : 서면 교부",
    "동종의 유사한 부가서비스 제공이 불가",
    "제공이 불가한 경우 : 사유발생 즉시",
    "회원의 권익을 증진하거나 부담을 완화",
    # -- 금융소비자보호법 고지 --
    "금융소비자보호법 제 19조",
    "설명을 받을 권리가 있습니다",
    "설명받을 권리가 있으며",
    "내부통제기준에 따른 절차를 거쳐 제공됩니다",
    "금융상품설명서 및 약관을 확인하시기 바랍니다",
    "여신금융협회 심의필",
    # -- 연체·신용도 경고 --
    "정상이자율이 없는 경우 아래와 같이 적용",
    "약정금리가 없는 경우 약정금리는",
    "무이자 할부 거래 연체 시",
    "일시불 거래 연체 시",
    "상법상 상사법정이율",
    "가중평균대출금리",
    "상환능력에 비해 신용카드 사용액이 과도",
    "상 환 능력에 비해 신용카드 사용액이 과도",
    "개인신용평점이 하락할 수 있습니다",
    "개인신용평점 하락 시 금융거래",
    "개 인신용평점 하락 시",
    "개인신용평점, 이용한도 등에 영향",
    "원리금을 연체할 경우",
    "이용대금을 연체할 경우",
    "의무가 발생할 수 있습니다",
    "카드 발급이 제한될 수 있습니다",
    "카드발급이 제한 될 수 있습니다",
    "지정하신 결제일에 상환하여야 합니다",
    "당일 출금되지 못하여 연체로 처리",
    # -- 해외 이용 (해외 거래는 추천 계산 대상이 아니다) --
    "전신환매도율",
    "해외이용수수료=",
    "해외 이용 시 청구금액 산출방법",
    "원화결제(DCC)",
    "해외원화결제(DCC)",
    "해외 이용 잠금 서비스",
    # -- 연회비 청구·반환 규정 (금액이 아니라 규정 서술이라 계산에 쓰이지 않는다) --
    "연회비는 카드 발급일(발급 첫 해에는",
    "카드 발급일(발급 첫 해에는 카드 사용등록일)에 연회비가 청구되지",
    "매년 카드 발급일을 기준으로 직전 1년 이내 실적이 없는 경우",
    "재발급, 갱신 시에는 이전 카드의 카드 발급일",
    "연회비는 기본 연회비와 제휴 연회비로 구분",
    "연회비(기본 연회비+제휴 연회비)는 카드별로 청구",
    "카드 중도 해지 시, 연회비 반환 금액",
    "유효기간이 도래하기 전에 카드를 해지하는 경우, 연회비 반환",
    "해지한 날부터 일 단위로 나누어 계산",
    "날을 기준으로 계산)하여 반환됨",
    "소요된 비용(신규 가입연도에 해당)은 반환 금액에서 제외",
    "추가적으로 반환 금액에서 제외됩니다",
    "제휴 연회비가 있는 경우에는 카드 이용 시 제공되는 추가적인 혜택",
    "제휴연회비가 있는 경우에는 카드 이용 시 제공되는 추가적인 혜택",
    "계약을 해지한 날로부터 3개월 이내에 반환",
    # -- 안내·연락처 --
    "고객센터 1544-7000",
    "카드신청 1661-8599",
    "오토다이렉트 센터",
    "홈페이지(www.shinhancard.com)를 참조",
    "www.samsungcard.com",
    "앱 → 전체메뉴 → 대금결제",
    "전체메뉴 → 고객센터 → 해외 이용",
)

# 위 목록에 걸려도 남길 줄. 이 낱말이 하나라도 있으면 지우지 않는다.
#
# 줄 단위로 지우는 것이 위험한 이유가 여기 있다. 상품설명서는 2단 조판이라 추출하면
# 좌우 단이 한 줄에 붙는다. 그래서 오른쪽 단의 공통 문구를 집어 줄을 통째로 버리면
# 왼쪽 단의 혜택까지 함께 사라진다. 실제로 확인된 것만 해도 이렇다.
#
#   · 전기요금, 도시가스요금, 통신요금 10% 할인 | - 고지 방법 : 서면 교부, 우편 또는 …
#   [전월/ 전분기 이용금액 산정기준]            | ② 제휴업체의 휴업 · 파산 …
#
# 오른쪽만 보면 버려야 할 줄이지만 버리면 혜택과 실적 산정기준이 없어진다. 오류도 나지
# 않고 혜택 하나가 조용히 빠질 뿐이라, 시드를 다 만들고 나서도 알아채기 어렵다.
#
# 그래서 <b>지우는 쪽이 아니라 남기는 쪽에 유리하게 판정한다.</b> 공통 문구가 조금 남는 것은
# 토큰을 조금 더 쓰는 일이지만, 혜택이 하나 빠지는 것은 계산이 틀리는 일이다.
_PROTECT_LINES = (
    # 혜택·실적을 가리키는 낱말. 이 줄은 공통 문구가 섞여 있어도 손대지 않는다.
    "할인",
    "적립",
    "한도",
    "실적",
    "전월",
    "전분기",
    "포인트",
    "캐시백",
    "캐쉬백",              # 같은 말인데 카드사마다 표기가 갈린다
    "이용금액",
    "이용 금액",
    # "…건에 한해 서비스 제공" 처럼 혜택 낱말 없이 적용 범위를 좁히는 문장이 있다.
    # 이런 줄이 빠지면 조건이 사라져 혜택이 실제보다 넓게 계산된다.
    "서비스 제공",
    "한하여",
    "한해",
    # 낱말로는 안 잡히는 계산 근거들.
    "공과금으로 분류",       # 카테고리 정의
    "가맹점 업종 분류 기준",  # 혜택 적용 범위
    "중복 적용",             # 혜택 중복 규칙
    "총 연회비",             # 연회비 금액표 머리글
)


@dataclass
class PruneResult:
    kept: str
    removed_sections: List[Tuple[str, int]] = field(default_factory=list)
    removed_lines: List[str] = field(default_factory=list)

    @property
    def removed_chars(self) -> int:
        return sum(size for _, size in self.removed_sections) + sum(
            len(line) for line in self.removed_lines
        )


def prune(text: str) -> PruneResult:
    """구조화에 쓸 본문만 남긴다."""
    result = PruneResult(kept="")
    body = _drop_sections(text, result)
    result.kept = _drop_lines(body, result)
    return result


def _drop_sections(text: str, result: PruneResult) -> str:
    """조항 제목으로 경계를 잡아 통째로 뺀다."""
    marks = [(m.start(), m.group(0).strip()) for m in _SECTION_BOUNDARY.finditer(text)]
    if not marks:
        return text

    kept: List[str] = []
    cursor = 0
    for index, (start, heading) in enumerate(marks):
        end = marks[index + 1][0] if index + 1 < len(marks) else len(text)
        if start > cursor:
            kept.append(text[cursor:start])
        segment = text[start:end]
        if _matches(heading, _KEEP_SECTIONS) or not _matches(heading, _DROP_SECTIONS):
            kept.append(segment)
        else:
            result.removed_sections.append((heading, len(segment)))
        cursor = end
    return "".join(kept)


def _drop_lines(text: str, result: PruneResult) -> str:
    """정해진 문구가 든 줄을 뺀다. 보호 문구가 함께 있으면 남긴다."""
    kept: List[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if stripped and _contains(stripped, _DROP_LINES) and not _contains(stripped, _PROTECT_LINES):
            result.removed_lines.append(stripped)
            continue
        kept.append(line)
    return "\n".join(kept)


def _matches(value: str, patterns: Tuple[str, ...]) -> bool:
    return any(re.match(pattern, value) for pattern in patterns)


def _contains(value: str, phrases: Tuple[str, ...]) -> bool:
    return any(phrase in value for phrase in phrases)


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    sources = sorted(_SOURCE_DIR.glob("*.txt"))
    if not sources:
        print(f"{_SOURCE_DIR} 에 추출 텍스트가 없다. ingest.extract 를 먼저 돌린다")
        return

    _TARGET_DIR.mkdir(parents=True, exist_ok=True)
    _REMOVED_DIR.mkdir(parents=True, exist_ok=True)

    before = after = 0
    for source in sources:
        text = source.read_text(encoding="utf-8")
        result = prune(text)
        (_TARGET_DIR / source.name).write_text(result.kept, encoding="utf-8")

        # 걷어낸 것도 남긴다. 혜택 하나가 빠졌을 때 무엇이 사라졌는지 대조할 데가 없으면
        # 필터를 의심할 수도 믿을 수도 없다.
        removed = ["## 걷어낸 조항"]
        removed += [f"- {head} ({size:,}자)" for head, size in result.removed_sections]
        removed += ["", "## 걷어낸 줄"]
        removed += [f"- {line}" for line in result.removed_lines]
        (_REMOVED_DIR / source.name).write_text("\n".join(removed), encoding="utf-8")

        before += len(text)
        after += len(result.kept)
        rate = 100 - len(result.kept) * 100 // len(text) if text else 0
        print(
            f"{source.stem:<30}{len(text):>8,} → {len(result.kept):>8,}  ({rate:>2}% 감축, "
            f"조항 {len(result.removed_sections)}개·줄 {len(result.removed_lines)}개)"
        )

    print("-" * 78)
    print(f"{'합계':<30}{before:>8,} → {after:>8,}  ({100 - after * 100 // before}% 감축)")


if __name__ == "__main__":
    main()
