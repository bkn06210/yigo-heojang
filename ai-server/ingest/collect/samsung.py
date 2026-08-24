"""삼성카드 약관 수집 어댑터.

상품공시실은 통합게시판의 한 유형으로 구현돼 있다. 게시판 유형 코드 19번이
'신용카드상품약관공시'이고, 목록 조회 서비스 하나로 전체를 훑을 수 있다.

목록 응답에 첨부파일 정보까지 들어 있어 상세 조회가 필요 없다.
게시글마다 상세를 부르면 요청이 게시글 수만큼 늘어나는데, 목록만 쓰면
페이지 수만큼으로 줄어든다.
"""

import time
from typing import Iterator

from .base import Collector, DocType, DocumentRef, clean_text, decode_html, make_session

_BOARD_URL = "https://www.samsungcard.com/company/IR/announce/product-conditions/UHPPCI0261M0.jsp"
_LIST_API = "https://www.samsungcard.com/service/SHPPCC0247S01"
_DOWNLOAD_URL = "https://www.samsungcard.com/filedownload.do"

_TERMS_BASE_URL = "https://www.samsungcard.com/personal/customer-service/conditions/"

# 개인회원 약관은 상품공시 게시판이 아니라 고객센터 화면에 있고, 목록이
# 자바스크립트로 그려져 링크를 읽어낼 수 없다. 그래서 주소를 직접 적는다.
# 대신 받은 화면에 약관 이름이 실제로 들어 있는지 확인한다 — 사이트가 개편돼
# 다른 문서가 오면 조용히 엉뚱한 약관이 적재되는 것을 막는다.
_MEMBER_TERMS_PAGES = (
    ("개인회원 약관", "personal/UHPPCC0379M0.jsp"),
    ("체크카드 개인회원 약관", "check-card/UHPPCC0340M0.jsp"),
)

# 한 화면에 지난 시행본이 함께 들어 있다. 최신본만 펼쳐져 있고 나머지는 숨겨져 있다.
# 통째로 넣으면 몇 해 전 조항이 검색에 걸려 지금은 틀린 답이 나간다.
_TERM_BLOCK_MARKER = '<div class="box696 ui_info_content"'
_VISIBLE_BLOCK_START = ">"  # 숨겨진 블록은 여기에 style="display: none;" 이 붙는다

_CHANNEL_PERSONAL = "01"  # 게시판 채널: 개인
_BOARD_TYPE_CARD_TERMS = "19"  # 통합게시판유형: 신용카드상품약관공시
_PAGE_SIZE = 10

# 한 게시글에 카드 약관과 브랜드 공통 약관(Mastercard 등)이 함께 붙는다.
# 브랜드 약관은 카드가 달라도 내용이 같아 받아봐야 중복이므로 이름으로 가른다.
_CARD_TERMS_MARKER = "이용안내장"

_REQUEST_INTERVAL_SEC = 1.0


class SamsungCollector(Collector):
    issuer = "삼성"

    def __init__(self) -> None:
        self._session = make_session(referer=_BOARD_URL)

    def list_documents(self) -> Iterator[DocumentRef]:
        page = 1
        total = None

        while True:
            body = self._fetch_page(page)
            if total is None:
                total = int(body.get("totInqrCt", 0))

            posts = body.get("blbdInqrRsList") or []
            if not posts:
                break

            for post in posts:
                yield from self._to_refs(post)

            if page * _PAGE_SIZE >= total:
                break
            page += 1
            time.sleep(_REQUEST_INTERVAL_SEC)

    def list_member_terms(self) -> Iterator[DocumentRef]:
        for term_name, path in _MEMBER_TERMS_PAGES:
            yield DocumentRef(
                issuer=self.issuer,
                card_name=term_name,
                doc_type=DocType.MEMBER_TERMS,
                doc_key=path,
                source_url=f"{_TERMS_BASE_URL}{path}",
                # 시행일이 본문 안에만 있어 목록 단계에서는 알 수 없다.
                # 개정 여부는 내려받은 뒤 해시로 판정한다.
                revised_at=None,
                file_name=f"{term_name}.html",
            )

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=60)
        response.raise_for_status()

        if ref.doc_type == DocType.MEMBER_TERMS:
            return _latest_term_block(decode_html(response.content), ref.card_name)
        return response.content

    def _fetch_page(self, page: int) -> dict:
        payload = {
            "cndt": {
                "no1PgeSize": str(_PAGE_SIZE),
                "pgeNo": str(page),
                "itgBlbdSn": "",
                "itgBlbdChnlDvC": _CHANNEL_PERSONAL,
                "itgBlbdTpDvC": _BOARD_TYPE_CARD_TERMS,
                "bltnbmTitNm": "",
                "bltnbmCn": "",
                "seaKeywCn": "",
                "seaKeywNm": "",
                "aryCriCn": "sysFstRgTs",  # 등록일 기준
                "aryDvCn": "DESC",  # 최신순. 같은 카드의 개정판이 앞에 온다
            }
        }
        response = self._session.post(_LIST_API, json=payload, timeout=60)
        response.raise_for_status()
        return response.json()

    def _to_refs(self, post: dict) -> Iterator[DocumentRef]:
        card_name = clean_text(post.get("bltnbmTitNm"))

        for file_info in post.get("uploadFileList") or []:
            file_name = clean_text(file_info.get("apnFileNm"))
            if _CARD_TERMS_MARKER not in file_name:
                continue

            yield DocumentRef(
                issuer=self.issuer,
                card_name=card_name,
                doc_type=DocType.PRODUCT_GUIDE,
                doc_key=f"{post.get('itgBlbdSn')}:{file_info.get('apnFileSn')}",
                source_url=self._download_url(file_info),
                revised_at=post.get("bltnbmWrteDt"),
                file_name=file_name,
            )

    @staticmethod
    def _download_url(file_info: dict) -> str:
        # apnFileGrpNoE는 이미 URL 인코딩된 값(%2F, %3D 포함)으로 내려온다.
        # 쿼리 파라미터로 넘기면 라이브러리가 한 번 더 인코딩해 키가 깨지므로
        # URL을 직접 조립한다.
        group_no = file_info.get("apnFileGrpNoE")
        file_sn = file_info.get("apnFileSn")
        return f"{_DOWNLOAD_URL}?grpNo={group_no}&sn={file_sn}"


def _latest_term_block(html: str, term_name: str) -> bytes:
    """지난 시행본을 걷어내고 지금 시행 중인 약관만 남긴다.

    화면에 펼쳐져 있는 블록이 최신본이고, 지난 시행본은 숨김 처리로 함께 실려 온다.
    '숨겨져 있으면 지금 약관이 아니다'는 화면이 이미 지키고 있는 규칙이라
    시행일을 직접 비교하는 것보다 어긋날 여지가 적다.
    """
    if term_name not in html:
        raise ValueError(f"'{term_name}' 화면이 아니다. 주소가 바뀌었을 수 있다")

    blocks = html.split(_TERM_BLOCK_MARKER)
    for block in blocks[1:]:
        if block.lstrip().startswith(_VISIBLE_BLOCK_START):
            return block.encode("utf-8")

    raise ValueError(f"'{term_name}' 화면에서 펼쳐진 약관 본문을 찾지 못했다")
