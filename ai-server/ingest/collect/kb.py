"""KB국민카드 약관 수집 어댑터.

다른 두 카드사와 달리 게시판이 아니다. 카드 목록 화면과 카드 상세 화면이 있고,
약관은 상세 화면에 걸린 링크로 내려받는다. 그래서 목록 → 상세 두 단계를 거친다.

카드 목록 화면은 서버가 완성된 HTML을 주므로 주소에 분류 번호만 바꿔가며 읽으면 된다.
카드 코드는 카드 이미지 주소에 들어 있다.
"""

import re
import time
from typing import Dict, Iterator, List

from .base import Collector, DocType, DocumentRef, clean_text, decode_html, make_session

_SCREEN_BASE = "https://card.kbcard.com/CRD/DVIEW/"
_LIST_URL = _SCREEN_BASE + "HCAMCXPRICAC0047"
_DETAIL_URL = _SCREEN_BASE + "HCAMCXPRICAC0076"

# 카드 목록이 분류별로 나뉘어 있고 한 번에 전체를 주는 주소가 없다.
# 분류를 모두 돌면서 모으고 카드 코드로 중복을 제거한다.
#
# <b>화면 하나가 전부를 주지 않는다.</b> 신용카드 화면과 체크카드 화면이 따로 있고,
# 어느 쪽 분류 탭에도 안 걸리는 카드(VVIP·플래티늄·공공기관·신속발급)는 각자 자기 화면에만 있다.
# 신용카드 화면만 보면 체크카드가 통째로 빠지는데, 목록이 정상으로 돌아오기 때문에
# 수집이 성공한 것처럼 보인다.
#
# 뒤쪽 네 화면은 분류를 받지 않고 자기 목록만 준다. 그래서 분류 자리가 비어 있다.
_LIST_SCREENS = (
    ("HCAMCXPRICAC0047", tuple(range(1, 13))),   # 신용카드
    ("HCAMCXPRICAC0056", tuple(range(1, 19))),   # 체크카드 (13~18이 체크 전용 분류)
    ("HCAMCXPRICAC0054", ()),                    # VVIP
    ("HCAMCXPRICAC0055", ()),                    # 플래티늄
    ("HCAMCXPRICAC0062", ()),                    # 공공·특수 목적
    ("HCAMCXPRICAC0031", ()),                    # 신속발급
)

# 카드 이미지 태그에 카드 코드와 카드명이 함께 들어 있다.
#
# 태그를 통째로 잡은 뒤 안에서 코드와 이름을 따로 꺼낸다. 화면마다 속성 순서가 달라
# (신용카드 화면은 src가 먼저, VVIP·플래티늄 화면은 alt가 먼저) 순서를 고정한 정규식을 쓰면
# 한쪽 화면이 통째로 0종이 된다. 응답은 정상으로 오기 때문에 실패로 보이지도 않는다.
_IMAGE_TAG = re.compile(r"<img[^>]*>", re.I)
_CARD_CODE = re.compile(r"/product/(\d{5})_img")
_CARD_ALT = re.compile(r"alt=\"([^\"]{2,60})\"")
# 약관은 별도 도메인에 올라간다.
_PDF_PATTERN = re.compile(r"https://img\d*\.kbcard\.com/obj/[^\"'\s>)]+?\.pdf")
# 상품설명서 파일명에만 개정일이 붙는다.
_REVISED_PATTERN = re.compile(r"_(\d{8})\.pdf$")

# 상세 화면에는 카드 약관 말고도 국제브랜드 기본서비스 약관 같은 공통 문서가 함께 걸린다.
# 그런 문서는 카드가 달라도 내용이 같아 받아봐야 중복이므로 아는 유형만 통과시킨다.
# 모르는 파일을 임의로 상품설명서로 넘기면 카드 혜택이 아닌 내용이 구조화 대상에 섞인다.
_DOC_TYPE_BY_MARKER = (
    ("prdctOpmn", DocType.PRODUCT_GUIDE),
    ("Terms_sheet", DocType.KEY_TERMS),
)

_REQUEST_INTERVAL_SEC = 1.0

# 개인회원 약관은 카드 상세가 아니라 고객센터 이용약관 화면에 모여 있다.
_TERMS_URL = "https://m.kbcard.com/CXHIACSC0013.cms"

# 약관 이름과 내려받기 링크가 나란히 붙어 있다. 이름을 함께 읽어야
# 어느 약관인지 알 수 있다 — 파일명만으로는 구분되지 않는다.
_TERMS_ENTRY = re.compile(r"<span>([^<]{2,60})</span>\s*<a href=\"(https://[^\"]+?\.pdf)\"")

# 파일명 끝에 붙는 개정일. 여섯 자리(YYMMDD)와 여덟 자리(YYYYMMDD)가 섞여 있다.
_TERMS_REVISED = re.compile(r"_(\d{8}|\d{6})\.pdf$")


class KbCollector(Collector):
    issuer = "KB국민"

    def __init__(self) -> None:
        self._session = make_session(referer=_LIST_URL)

    def list_documents(self) -> Iterator[DocumentRef]:
        for code, card_name in self._list_cards().items():
            # 카드마다 문서 구성이 다르고, 아예 약관이 걸리지 않은 카드도 있다.
            # 파일명을 규칙으로 추측하면 없는 주소를 만들게 되므로 상세에서 실제 링크를 읽는다.
            for url in self._detail_pdf_urls(code):
                doc_type = _classify(url)
                if doc_type is None:
                    continue
                yield DocumentRef(
                    issuer=self.issuer,
                    card_name=card_name,
                    doc_type=doc_type,
                    doc_key=f"{code}:{url.rsplit('/', 1)[-1]}",
                    source_url=url,
                    revised_at=_revised_at(url),
                    file_name=url.rsplit("/", 1)[-1],
                )
            time.sleep(_REQUEST_INTERVAL_SEC)

    def list_member_terms(self) -> Iterator[DocumentRef]:
        response = self._session.get(_TERMS_URL, timeout=60)
        response.raise_for_status()

        for name, url in _TERMS_ENTRY.findall(decode_html(response.content)):
            term_name = clean_text(name)
            if not term_name:
                continue

            yield DocumentRef(
                issuer=self.issuer,
                card_name=term_name,
                doc_type=DocType.MEMBER_TERMS,
                doc_key=url.rsplit("/", 1)[-1],
                source_url=url,
                revised_at=_terms_revised_at(url),
                file_name=url.rsplit("/", 1)[-1],
            )

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=120)
        response.raise_for_status()
        return response.content

    def _list_cards(self) -> Dict[str, str]:
        """카드 코드 → 카드명. 목록 화면을 돌며 모은다."""
        cards: Dict[str, str] = {}
        for screen, categories in _LIST_SCREENS:
            url = _SCREEN_BASE + screen
            # 분류를 받지 않는 화면은 한 번만 부른다. 그런 화면에 분류를 넣으면
            # 무시하고 같은 목록을 되돌려주므로 요청만 헛돈다.
            for params in ([{"pageNo": 1, "cateIdx": c} for c in categories] or [{"pageNo": 1}]):
                response = self._session.get(url, params=params, timeout=60)
                response.raise_for_status()
                for code, name in _cards_in(response.text):
                    cards.setdefault(code, name)
                time.sleep(_REQUEST_INTERVAL_SEC)
        return cards

    def _detail_pdf_urls(self, code: str) -> List[str]:
        response = self._session.get(
            _DETAIL_URL, params={"mainCC": "a", "cooperationcode": code}, timeout=60
        )
        response.raise_for_status()
        # 같은 링크가 화면에 여러 번 나오므로 순서를 지키면서 중복을 없앤다.
        return list(dict.fromkeys(_PDF_PATTERN.findall(response.text)))


def _cards_in(html: str) -> List[tuple]:
    """목록 화면에서 (카드 코드, 카드명)을 뽑는다."""
    found = []
    for tag in _IMAGE_TAG.findall(html):
        code = _CARD_CODE.search(tag)
        name = _CARD_ALT.search(tag)
        if code and name:
            found.append((code.group(1), clean_text(name.group(1))))
    return found


def _classify(url: str) -> "str | None":
    for marker, doc_type in _DOC_TYPE_BY_MARKER:
        if marker in url:
            return doc_type
    return None


def _revised_at(url: str) -> "str | None":
    match = _REVISED_PATTERN.search(url)
    return match.group(1) if match else None


def _terms_revised_at(url: str) -> "str | None":
    """개인회원 약관 파일명에서 개정일을 읽어 YYYYMMDD로 맞춘다.

    같은 화면 안에서도 표기가 갈린다(standardagreement_260402 / attach_20201218).
    여섯 자리를 그대로 두면 다른 카드사와 자릿수가 달라 개정 판정이 어긋난다.
    """
    match = _TERMS_REVISED.search(url)
    if match is None:
        return None

    digits = match.group(1)
    return digits if len(digits) == 8 else f"20{digits}"
