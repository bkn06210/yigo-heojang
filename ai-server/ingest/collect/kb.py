"""KB국민카드 약관 수집 어댑터.

다른 두 카드사와 달리 게시판이 아니다. 카드 목록 화면과 카드 상세 화면이 있고,
약관은 상세 화면에 걸린 링크로 내려받는다. 그래서 목록 → 상세 두 단계를 거친다.

카드 목록 화면은 서버가 완성된 HTML을 주므로 주소에 분류 번호만 바꿔가며 읽으면 된다.
카드 코드는 카드 이미지 주소에 들어 있다.
"""

import re
import time
from typing import Dict, Iterator, List

from .base import Collector, DocType, DocumentRef, clean_text, make_session

_LIST_URL = "https://card.kbcard.com/CRD/DVIEW/HCAMCXPRICAC0047"
_DETAIL_URL = "https://card.kbcard.com/CRD/DVIEW/HCAMCXPRICAC0076"

# 카드 목록이 분류별로 나뉘어 있고 한 번에 전체를 주는 주소가 없다.
# 분류를 모두 돌면서 모으고 카드 코드로 중복을 제거한다.
_CATEGORY_INDEXES = range(1, 13)

# 카드 이미지 주소에 카드 코드와 카드명이 함께 들어 있다.
_CARD_PATTERN = re.compile(r"/product/(\d{5})_img[^>]*?alt=\"([^\"]{2,60})\"")
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

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=120)
        response.raise_for_status()
        return response.content

    def _list_cards(self) -> Dict[str, str]:
        """카드 코드 → 카드명. 분류를 돌며 모은다."""
        cards: Dict[str, str] = {}
        for category in _CATEGORY_INDEXES:
            response = self._session.get(
                _LIST_URL, params={"pageNo": 1, "cateIdx": category}, timeout=60
            )
            response.raise_for_status()
            for code, name in _CARD_PATTERN.findall(response.text):
                cards.setdefault(code, clean_text(name))
            time.sleep(_REQUEST_INTERVAL_SEC)
        return cards

    def _detail_pdf_urls(self, code: str) -> List[str]:
        response = self._session.get(
            _DETAIL_URL, params={"mainCC": "a", "cooperationcode": code}, timeout=60
        )
        response.raise_for_status()
        # 같은 링크가 화면에 여러 번 나오므로 순서를 지키면서 중복을 없앤다.
        return list(dict.fromkeys(_PDF_PATTERN.findall(response.text)))


def _classify(url: str) -> "str | None":
    for marker, doc_type in _DOC_TYPE_BY_MARKER:
        if marker in url:
            return doc_type
    return None


def _revised_at(url: str) -> "str | None":
    match = _REVISED_PATTERN.search(url)
    return match.group(1) if match else None
