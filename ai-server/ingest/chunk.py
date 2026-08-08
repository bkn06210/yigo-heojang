"""약관 원문을 검색 단위로 자른다.

검색은 문서 하나를 통째로 넘기지 않는다. 약관 한 장이 수만 자라 그대로 넣으면
프롬프트에 안 들어가고, 들어가더라도 답과 무관한 조항이 대부분이라 정확도가 떨어진다.
그래서 조각으로 나눠 두고 질문에 가까운 조각만 골라 넣는다.

자르는 기준은 문서에 실린 구조를 따른다. 개인회원 약관은 '제N조(제목)'로 나뉘어 있어
그 경계가 곧 의미의 경계다. 글자 수로 자르면 한 조문이 두 조각으로 갈려
"카드사는 회원이" 에서 끊긴 조각이 검색에 걸린다.

상품설명서에는 조문 구조가 없다. 그때만 글자 수로 자르고, 잘린 자리에서 문맥이
끊기지 않게 앞뒤를 조금씩 겹친다.
"""

import re
from dataclasses import dataclass
from typing import List, Optional

# 조문 제목. 표기가 카드사·약관마다 갈린다. 실측한 것만 네 가지다.
#   제4조(카드의 관리)  /  제 1 조(회원)  /  제1조 회원  /  제5조의2 계약해지에 따른 연회비 반환
# 괄호형만 받으면 체크카드 약관이 통째로 안 잡힌다 — 그쪽은 괄호 없이 제목을 붙인다.
_ARTICLE_HEADING = re.compile(
    r"^제\s*\d+\s*조(?:의\s*\d+)?\s*(?:\([^)\n]{0,60}\)|[^\n]{0,40})?$", re.M
)

# 조문 제목처럼 생겼지만 실제로는 다른 조문을 가리키는 문장이 있다("제30조에 따라 …합니다").
# 길이만으로는 안 갈리므로 문장 종결을 함께 본다. 이걸 경계로 잡으면 조문 하나가
# 여러 조각으로 부서져 중간이 잘린 조각이 검색에 걸린다.
_SENTENCE_TAIL = re.compile(r"(?:니다|한다|된다|같다)\s*[.]?\s*$|[.]\s*$")

# 조문 구조를 신뢰할 최소 개수. 긴 문서에서 한두 개만 잡혔다면 표기가 우리 규칙과
# 달라 대부분을 놓친 것이므로, 그 상태로 자르면 안 잡힌 부분이 한 조각에 뭉쳐 들어간다.
_MIN_ARTICLES = 3

# 장 제목. 조문의 상위 묶음이라 조각에 함께 붙여 어느 장의 조문인지 남긴다.
_CHAPTER_HEADING = re.compile(r"^제\s*\d+\s*장\s+.{0,40}$", re.M)

# 추출기가 붙인 페이지 머리말. 원문이 아니라 우리가 만든 표시라 검색 대상이 아니다.
_PAGE_MARKER = re.compile(r"^\[\d+페이지\]\s*$", re.M)

# 표 블록 한 덩어리. 머리말 한 줄과 뒤따르는 파이프 줄들로 이뤄진다.
_TABLE_BLOCK = re.compile(r"^\[표\]\n(?:\|.*(?:\n|$))+", re.M)

# 표 줄에서 셀 내용만 남길 때 쓴다.
_TABLE_CELL_SPLIT = re.compile(r"\s*\|\s*")

# 중복 판정에서 무시할 짧은 조각. '①' '및' 같은 토막까지 대조하면
# 본문 어딘가에 늘 들어 있어 멀쩡한 표가 통째로 지워진다.
_MIN_DUPLICATE_CHARS = 12

# 조문 하나가 이보다 길면 더 쪼갠다. 한 조각이 너무 길면 검색이 걸려도
# 정작 필요한 문장이 조각 안에 묻힌다.
_MAX_CHUNK_CHARS = 1200

# 조문 구조가 없는 문서를 자를 때의 길이와 겹침.
_FALLBACK_CHUNK_CHARS = 700
_CHUNK_OVERLAP_CHARS = 120

# 이보다 짧은 조각은 버린다. 제목만 남은 조각은 검색에 걸려도 답이 되지 않는다.
_MIN_CHUNK_CHARS = 30


@dataclass(frozen=True)
class Chunk:
    index: int
    content: str
    heading: Optional[str]  # 이 조각이 속한 조문 제목. 구조가 없으면 None


def split_document(text: str) -> List[Chunk]:
    """원문 하나를 조각 목록으로 만든다."""
    cleaned = preprocess(text)
    pieces = _split_by_article(cleaned) or _split_by_length(cleaned)
    return _number(_drop_duplicates(pieces))


def preprocess(text: str) -> str:
    """검색에 방해가 되는 것을 걷어낸다.

    표를 지우는 조건을 '본문에 이미 있는 내용일 때'로 둔 것이 핵심이다.
    표를 무조건 지우면 상품설명서의 혜택 수치가 사라지고, 무조건 남기면
    약관 PDF에서 같은 조항이 두 번 들어간다. 문서 종류로 가르지 않는 이유는
    한 문서 안에서도 진짜 표와 오인식된 표가 섞여 있기 때문이다.
    """
    without_pages = _PAGE_MARKER.sub("", text)
    return _drop_restated_tables(without_pages)


def _drop_restated_tables(text: str) -> str:
    """본문을 되풀이하는 표 블록만 지운다.

    레이아웃이 상자로 짜인 PDF에서 추출기가 같은 내용을 본문으로 한 번,
    표로 한 번 뽑는다. 표 쪽은 셀 너비에서 잘려 '카드의 남' 처럼 단어 중간에서
    끊기므로, 줄 단위로 같은지 보지 않고 본문에 들어 있는지로 판정한다.
    """
    body = _normalize(_TABLE_BLOCK.sub("", text))

    def replace(match: re.Match) -> str:
        cells = _table_cells(match.group(0))
        if cells and all(cell in body for cell in cells):
            return ""
        return match.group(0)

    return _TABLE_BLOCK.sub(replace, text)


def _table_cells(block: str) -> List[str]:
    """표 블록에서 대조할 만한 길이의 셀 내용만 뽑는다."""
    cells = []
    for line in block.splitlines():
        for cell in _TABLE_CELL_SPLIT.split(line):
            normalized = _normalize(cell)
            if len(normalized) >= _MIN_DUPLICATE_CHARS:
                cells.append(normalized)
    return cells


def _normalize(value: str) -> str:
    """대조용 형태. 공백과 표 구분자를 지운다.

    추출기가 넣은 줄바꿈이 문장 중간에 들어가 있어("카드의 남\n은 유효기한"),
    공백을 남겨두면 같은 문장인데도 다르다고 판정된다.
    """
    return re.sub(r"[\s|]+", "", value)


def _split_by_article(text: str) -> List[Chunk]:
    """조문 경계로 자른다. 조문 구조가 없으면 빈 목록을 낸다."""
    headings = [m for m in _ARTICLE_HEADING.finditer(text) if _is_heading(m.group(0))]
    if len(headings) < _MIN_ARTICLES:
        return []

    chapters = [(m.start(), m.group(0).strip()) for m in _CHAPTER_HEADING.finditer(text)]
    pieces: List[Chunk] = []

    # 첫 조문 앞에도 내용이 있다. 목적 조항 앞의 안내문이나 약관 이름·시행일이 그것이고,
    # 버리면 문서 앞부분이 조용히 사라진다. 조각 길이 검사가 알아서 짧은 것은 걸러낸다.
    preamble = text[: headings[0].start()].strip()
    for part in _cut_to_length(preamble, _MAX_CHUNK_CHARS):
        pieces.append(Chunk(index=0, content=part, heading=None))

    for order, heading in enumerate(headings):
        end = headings[order + 1].start() if order + 1 < len(headings) else len(text)
        title = heading.group(0).strip()
        body = text[heading.end() : end].strip()
        if not body:
            continue

        # 어느 장의 조문인지 함께 남긴다. 조문 번호만으로는 무엇에 관한 규정인지
        # 드러나지 않아, 조각만 읽는 검색 단계에서 판단 근거가 부족하다.
        chapter = _chapter_of(chapters, heading.start())
        label = f"{chapter} {title}" if chapter else title

        for part in _cut_to_length(body, _MAX_CHUNK_CHARS):
            pieces.append(Chunk(index=0, content=f"{label}\n{part}", heading=label))

    return pieces


def _is_heading(line: str) -> bool:
    """조문 제목인지, 다른 조문을 언급하는 문장인지 가른다."""
    return _SENTENCE_TAIL.search(line.strip()) is None


def _chapter_of(chapters: List[tuple], position: int) -> Optional[str]:
    """이 위치보다 앞에 나온 마지막 장 제목."""
    found = None
    for start, title in chapters:
        if start > position:
            break
        found = title
    return found


def _split_by_length(text: str) -> List[Chunk]:
    """조문 구조가 없는 문서를 글자 수로 자른다."""
    return [
        Chunk(index=0, content=part, heading=None)
        for part in _cut_to_length(
            text, _FALLBACK_CHUNK_CHARS, overlap=_CHUNK_OVERLAP_CHARS
        )
    ]


def _cut_to_length(text: str, limit: int, overlap: int = 0) -> List[str]:
    """길이 상한에 맞춰 자른다. 상한 안에 들어가면 그대로 하나로 둔다.

    자를 자리는 줄바꿈을 먼저 찾는다. 글자 수만 세어 자르면 문장 한가운데가
    끊겨 조각 끝이 말이 되지 않는다.
    """
    text = text.strip()
    if len(text) <= limit:
        return [text] if text else []

    parts: List[str] = []
    start = 0

    while start < len(text):
        end = min(start + limit, len(text))
        if end < len(text):
            boundary = text.rfind("\n", start + limit // 2, end)
            if boundary != -1:
                end = boundary

        part = text[start:end].strip()
        if part:
            parts.append(part)

        if end >= len(text):
            break
        start = max(end - overlap, end) if overlap == 0 else end - overlap

    return parts


def _drop_duplicates(pieces: List[Chunk]) -> List[Chunk]:
    """내용이 같은 조각을 하나만 남긴다.

    같은 약관이 화면에 두 번 실리거나 표로 되풀이된 경우, 앞 단계에서 다 걸러지지
    않은 나머지가 여기서 걸린다. 검색 상위가 같은 내용으로 채워지면 프롬프트
    자리만 차지하고 답에 보태는 것이 없다.
    """
    seen = set()
    kept: List[Chunk] = []

    for piece in pieces:
        if len(_normalize(piece.content)) < _MIN_CHUNK_CHARS:
            continue
        key = _normalize(piece.content)
        if key in seen:
            continue
        seen.add(key)
        kept.append(piece)

    return kept


def _number(pieces: List[Chunk]) -> List[Chunk]:
    """조각 번호를 매긴다. 저장할 때 문서 안에서의 순서가 된다."""
    return [
        Chunk(index=order, content=piece.content, heading=piece.heading)
        for order, piece in enumerate(pieces)
    ]
