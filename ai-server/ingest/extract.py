"""원본 문서에서 텍스트를 뽑는다.

이 단계에는 LLM이 들어가지 않는다. 그래서 몇 번을 다시 돌려도 비용이 없고,
스키마가 바뀌어 구조화를 다시 해야 할 때도 여기까지는 재사용된다.

표를 따로 뽑는 이유가 있다. 줄글로만 추출하면 열 경계가 공백으로 뭉개져서
어디까지가 할인 대상이고 어디부터가 한도 금액인지 구분되지 않는다.
약관의 핵심 수치는 대부분 표에 있으므로 표 구조를 살려야 값이 정확해진다.

원본이 PDF만은 아니다. 개인회원 약관은 카드사에 따라 웹페이지 본문으로만
제공되고, 오히려 그쪽이 조문 구조(제N장·제N조)를 그대로 갖고 있어 다루기 쉽다.
형식은 호출자가 알려주는 대신 바이트 앞머리로 판정한다. 카드사가 PDF 주소라고
해놓고 오류 페이지를 HTML로 돌려주는 일이 있어, 선언된 형식을 믿으면
빈 원문이 정상처럼 저장된다.
"""

import hashlib
import re
from dataclasses import dataclass
from html.parser import HTMLParser
from io import BytesIO
from pathlib import Path
from typing import List, Optional

import pdfplumber


class ExtractStatus:
    TEXT_OK = "TEXT_OK"
    IMAGE_ONLY = "IMAGE_ONLY"  # 스캔 이미지로만 된 PDF. 텍스트가 나오지 않는다


# 페이지당 이 글자 수에 못 미치면 스캔 이미지로 본다.
# 총 글자 수가 아니라 페이지당으로 재는 이유는, 총량은 문서 길이에 따라 달라져
# 짧은 정상 문서와 긴 이미지 문서를 가르지 못하기 때문이다.
# 실측값: 이미지 PDF 105자 / 정상 약관 1,052~2,152자. 열 배 차이라 경계가 넓다.
_MIN_CHARS_PER_PAGE = 300

# 글자 사이가 이 값보다 벌어지면 공백으로 본다.
# 기본값(3)은 한글 약관에서 공백을 놓쳐 "전월이용실적50만원이상시제공되며"처럼 붙어 나온다.
# 실측: KB 문서는 공백이 65개에서 295개로 늘어 문장이 복원되고, 삼성 문서는 거의 영향이 없다.
_X_TOLERANCE = 1.5

# 이미지 PDF를 비전으로 읽을 때 쓰는 렌더링 해상도(dpi).
_VISION_RESOLUTION = 160

# HTML 문서가 이 글자 수에 못 미치면 본문을 못 받았다고 본다.
# 넉넉히 잡으면 안 된다. 조문이 둘뿐인 부속약관이 612자로 정상인데,
# 분량으로 정상 여부를 가르면 짧은 약관이 통째로 버려진다.
# 걸러내려는 것은 넘김 전용 페이지(본문 0자)와 오류 화면이다.
_MIN_HTML_CHARS = 200

# 본문이 아닌 태그. 안에 든 글자를 그대로 뽑으면 자바스크립트 코드가 원문에 섞인다.
_HTML_SKIP_TAGS = frozenset({"script", "style", "noscript", "head"})

# 줄바꿈이 필요한 태그. 구분하지 않으면 조문 제목과 본문이 한 줄로 붙어
# "제3조(카드의 발급)카드를 발급받고자 하는"처럼 경계가 사라진다.
_HTML_BLOCK_TAGS = frozenset(
    {"p", "div", "br", "li", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "table", "section"}
)

# 세 줄 이상 연속된 빈 줄. 태그 사이 공백이 그대로 줄바꿈이 되어 생긴다.
_EXCESS_BLANK_LINES = re.compile(r"\n{3,}")


@dataclass(frozen=True)
class ExtractResult:
    status: str
    page_count: Optional[int]  # HTML은 페이지 개념이 없어 None
    text: Optional[str]  # IMAGE_ONLY면 None
    content_hash: str  # 원본의 SHA-256. 개정 감지에 쓴다


def extract(content: bytes) -> ExtractResult:
    """원본 바이트에서 텍스트를 뽑는다. PDF·HTML 모두 받는다."""
    if content.startswith(b"%PDF-"):
        return _extract_pdf(content)
    return _extract_html(content)


def _extract_pdf(pdf_bytes: bytes) -> ExtractResult:
    content_hash = hashlib.sha256(pdf_bytes).hexdigest()
    pages: List[str] = []
    body_length = 0

    with pdfplumber.open(BytesIO(pdf_bytes)) as pdf:
        page_count = len(pdf.pages)
        for index, page in enumerate(pdf.pages, start=1):
            body = page.extract_text(x_tolerance=_X_TOLERANCE) or ""
            # 판정은 본문 길이로만 한다. 페이지 머리말이나 표 구분자 같은
            # 우리가 붙인 마크업까지 세면 빈 문서가 정상으로 넘어간다.
            body_length += len(body.strip())
            pages.append(_render_page(page, index, body))

    text = "\n\n".join(pages)

    if page_count == 0 or body_length < page_count * _MIN_CHARS_PER_PAGE:
        # 원문을 남기지 않는다. 몇 글자 잡힌 조각을 정상 원문처럼 저장하면
        # 나중에 "이 카드만 답을 못 하는" 상태가 조용히 생긴다.
        return ExtractResult(ExtractStatus.IMAGE_ONLY, page_count, None, content_hash)

    return ExtractResult(ExtractStatus.TEXT_OK, page_count, text, content_hash)


def _extract_html(html_bytes: bytes) -> ExtractResult:
    """웹페이지 본문에서 글자만 뽑는다.

    짧으면 IMAGE_ONLY로 넘기지 않고 예외를 던진다. 스캔 PDF는 나중에 비전으로
    다시 읽을 수 있는 정상 문서지만, 짧은 HTML은 받아오기가 실패한 것이라
    성격이 다르다. 상태로 남기면 실패가 정상 데이터에 섞여 조용히 잊힌다.
    """
    content_hash = hashlib.sha256(html_bytes).hexdigest()
    text = _html_to_text(_decode(html_bytes))

    if len(text) < _MIN_HTML_CHARS:
        raise ValueError(f"HTML 본문이 너무 짧다({len(text)}자). 받아오기가 실패했을 수 있다")

    return ExtractResult(ExtractStatus.TEXT_OK, None, text, content_hash)


def _decode(html_bytes: bytes) -> str:
    """바이트를 문자열로 되돌린다.

    응답 헤더의 charset을 쓰지 않는다. 카드사 약관 페이지 중에는 charset을
    주지 않는 곳이 있어, 라이브러리가 ISO-8859-1로 추측해 한글이 통째로 깨진다.
    국내 카드사 문서는 UTF-8 아니면 EUC-KR 둘 중 하나다.
    """
    for encoding in ("utf-8-sig", "utf-8", "euc-kr"):
        try:
            return html_bytes.decode(encoding)
        except UnicodeDecodeError:
            continue
    # 어느 것으로도 안 되면 읽히는 글자만 남긴다. 여기까지 오면 본문 길이 검사에 걸린다.
    return html_bytes.decode("utf-8", errors="ignore")


class _TextCollector(HTMLParser):
    """태그를 걷어내고 글자만 모은다.

    외부 파서 라이브러리를 쓰지 않는 것은 이 단계가 표준 라이브러리만으로
    충분하기 때문이다. 약관 페이지는 문단·목록·표로만 되어 있어 복잡한
    DOM 조작이 필요 없고, 의존성이 늘면 수집을 돌릴 환경 준비가 그만큼 무거워진다.
    """

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self._parts: List[str] = []
        self._skip_depth = 0

    def handle_starttag(self, tag: str, attrs) -> None:
        if tag in _HTML_SKIP_TAGS:
            self._skip_depth += 1
        elif tag in _HTML_BLOCK_TAGS:
            self._parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in _HTML_SKIP_TAGS:
            # 여는 태그를 못 본 채 닫는 태그만 나오는 문서가 있다. 음수로 내려가면
            # 그 뒤 본문이 통째로 건너뛰기 상태에 갇힌다.
            self._skip_depth = max(0, self._skip_depth - 1)
        elif tag in _HTML_BLOCK_TAGS:
            self._parts.append("\n")

    def handle_data(self, data: str) -> None:
        if self._skip_depth == 0:
            self._parts.append(data)

    def text(self) -> str:
        return "".join(self._parts)


def _html_to_text(html: str) -> str:
    collector = _TextCollector()
    collector.feed(html)

    lines = [line.strip() for line in collector.text().splitlines()]
    joined = "\n".join(line for line in lines if line)
    return _EXCESS_BLANK_LINES.sub("\n\n", joined).strip()


def render_pages(pdf_bytes: bytes, output_dir: Path, stem: str) -> List[Path]:
    """이미지 PDF의 페이지를 PNG로 저장한다.

    비전 모델에 넘기기 전 단계다. 텍스트 추출이 실패한 문서에만 쓴다.
    모든 문서에 태우면 비용이 페이지 수만큼 곱해진다.

    해상도는 실측으로 정했다. 160dpi에서 KB 상품설명서의 본문·표·각주가
    모두 판독되고 페이지당 40~90KB에 머문다. 더 올리면 용량만 늘고 읽히는 양은 같다.
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    saved: List[Path] = []

    with pdfplumber.open(BytesIO(pdf_bytes)) as pdf:
        for index, page in enumerate(pdf.pages, start=1):
            path = output_dir / f"{stem}_p{index}.png"
            page.to_image(resolution=_VISION_RESOLUTION).save(str(path))
            saved.append(path)

    return saved


def _render_page(page, page_number: int, body: str) -> str:
    parts = [f"[{page_number}페이지]"]

    if body.strip():
        parts.append(body)

    # 표 안의 글자에도 같은 기준을 적용한다. 안 맞추면 본문은 띄어쓰기가 살아 있는데
    # 표 셀만 붙어 나와 같은 문서 안에서 표기가 갈린다.
    for table in page.extract_tables({"text_x_tolerance": _X_TOLERANCE}):
        rendered = _render_table(table)
        if rendered:
            parts.append(rendered)

    return "\n".join(parts)


def _render_table(table: List[List[Optional[str]]]) -> str:
    """표를 파이프로 구분한 형태로 만든다.

    셀 안의 줄바꿈은 공백으로 바꾼다. 그대로 두면 한 행이 여러 줄로 쪼개져
    열 정렬이 깨지고, 표로 뽑은 의미가 없어진다.
    """
    lines = []
    for row in table:
        cells = [(cell or "").replace("\n", " ").strip() for cell in row]
        if not any(cells):
            continue
        lines.append("| " + " | ".join(cells) + " |")

    if not lines:
        return ""
    return "[표]\n" + "\n".join(lines)
