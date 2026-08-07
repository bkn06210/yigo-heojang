"""신한카드 약관 수집 어댑터.

PC 상품공시실과 모바일 상품공시실이 같은 데이터를 다르게 노출한다.
PC는 파일 키가 조회할 때마다 새로 발급되는 암호화 값이고 앞단에 웹방화벽이 있어
브라우저 밖에서는 다운로드가 되지 않는다. 모바일은 같은 목록을 그대로 주면서
다운로드도 주소 한 번으로 끝난다. 그래서 모바일 경로를 쓴다.

목록은 커서 방식이다. 페이지 번호가 아니라 응답이 준 다음 키를 다시 넣어야
그다음 묶음이 온다.
"""

import time
import urllib.parse
from typing import Iterator

from .base import Collector, DocType, DocumentRef, clean_text, make_session

_BOARD_URL = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R01.shc?page=CRE"
_LIST_API = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R01C.ajax"
_DOWNLOAD_URL = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R03.shc"

_CARD_TYPE_CREDIT = "0"  # 0=신용, 1=체크

# 모바일 화면이라 모바일 단말로 접근한다.
_MOBILE_USER_AGENT = (
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
    "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
)

_REQUEST_INTERVAL_SEC = 1.0


class ShinhanCollector(Collector):
    issuer = "신한"

    def __init__(self) -> None:
        self._session = make_session(referer=_BOARD_URL)
        self._session.headers["User-Agent"] = _MOBILE_USER_AGENT
        self._session_ready = False

    def list_documents(self) -> Iterator[DocumentRef]:
        self._ensure_session()
        next_key = ""

        while True:
            body = self._fetch_page(next_key)
            for item in body.get("crdPdPbnList") or []:
                ref = self._to_ref(item)
                if ref is not None:
                    yield ref

            # 커서가 비면 마지막 묶음이다.
            next_key = (body.get("data") or {}).get("nxtQyKey") or ""
            if not next_key:
                break
            time.sleep(_REQUEST_INTERVAL_SEC)

    def fetch(self, ref: DocumentRef) -> bytes:
        self._ensure_session()
        response = self._session.get(ref.source_url, timeout=90)
        response.raise_for_status()
        return response.content

    def _ensure_session(self) -> None:
        """목록 화면을 먼저 열어 세션 쿠키를 받는다.

        이 쿠키가 없으면 목록도 다운로드도 거부된다.
        """
        if self._session_ready:
            return
        self._session.get(_BOARD_URL, timeout=60).raise_for_status()
        self._session_ready = True

    def _fetch_page(self, next_key: str) -> dict:
        # JSON 본문으로 보내면 서버가 파라미터를 비운 채 받아 결과가 0건이 된다.
        # 화면이 form 형식으로 보내므로 같은 형식을 쓴다.
        payload = {
            "crdTcd": _CARD_TYPE_CREDIT,
            "nxtQyKey": next_key,
            "crdPdGuiNm": "",
        }
        response = self._session.post(_LIST_API, data=payload, timeout=60)
        response.raise_for_status()
        # 실제 내용은 한 겹 안에 들어 있다.
        return response.json().get("mbw_json") or {}

    def _to_ref(self, item: dict) -> "DocumentRef | None":
        card_name = clean_text(item.get("CRD_PD_GUI_NM"))
        file_key = clean_text(item.get("CRD_PD_GUI_FIL_NM"))
        if not card_name or not file_key:
            return None

        return DocumentRef(
            issuer=self.issuer,
            card_name=card_name,
            doc_type=DocType.PRODUCT_GUIDE,
            doc_key=file_key,
            source_url=self._download_url(card_name, file_key),
            revised_at=_normalize_date(item.get("CRD_PD_GUI_BUL_D")),
            file_name=f"{card_name}.pdf",
        )

    @staticmethod
    def _download_url(card_name: str, file_key: str) -> str:
        # 카드명이 주소에 들어가고 한글이라 인코딩이 필요하다.
        return (
            f"{_DOWNLOAD_URL}?pbnNm={urllib.parse.quote(card_name)}&aFilenm={file_key}"
        )


def _normalize_date(value: "str | None") -> "str | None":
    """'2026.07.06' 형태를 다른 카드사와 같은 YYYYMMDD로 맞춘다."""
    if not value:
        return None
    digits = value.replace(".", "").replace("-", "").strip()
    return digits if len(digits) == 8 and digits.isdigit() else None
