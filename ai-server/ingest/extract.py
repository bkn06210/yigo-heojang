"""PDF 원본에서 텍스트를 뽑는다.

이 단계에는 LLM이 들어가지 않는다. 그래서 몇 번을 다시 돌려도 비용이 없고,
스키마가 바뀌어 구조화를 다시 해야 할 때도 여기까지는 재사용된다.

표를 따로 뽑는 이유가 있다. 줄글로만 추출하면 열 경계가 공백으로 뭉개져서
어디까지가 할인 대상이고 어디부터가 한도 금액인지 구분되지 않는다.
약관의 핵심 수치는 대부분 표에 있으므로 표 구조를 살려야 값이 정확해진다.
"""

import hashlib
from dataclasses import dataclass
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


@dataclass(frozen=True)
class ExtractResult:
    status: str
    page_count: int
    text: Optional[str]  # IMAGE_ONLY면 None
    content_hash: str  # 원본 PDF의 SHA-256. 개정 감지에 쓴다


def extract(pdf_bytes: bytes) -> ExtractResult:
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
