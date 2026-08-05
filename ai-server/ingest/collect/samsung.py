"""삼성카드 약관 수집 어댑터.

상품공시실은 통합게시판의 한 유형으로 구현돼 있다. 게시판 유형 코드 19번이
'신용카드상품약관공시'이고, 목록 조회 서비스 하나로 전체를 훑을 수 있다.

목록 응답에 첨부파일 정보까지 들어 있어 상세 조회가 필요 없다.
게시글마다 상세를 부르면 요청이 게시글 수만큼 늘어나는데, 목록만 쓰면
페이지 수만큼으로 줄어든다.
"""

import time
from typing import Iterator

from .base import Collector, DocType, DocumentRef, clean_text, make_session

_BOARD_URL = "https://www.samsungcard.com/company/IR/announce/product-conditions/UHPPCI0261M0.jsp"
_LIST_API = "https://www.samsungcard.com/service/SHPPCC0247S01"
_DOWNLOAD_URL = "https://www.samsungcard.com/filedownload.do"

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

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=60)
        response.raise_for_status()
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
