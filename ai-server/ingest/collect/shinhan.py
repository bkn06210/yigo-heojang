"""신한카드 약관 수집 어댑터.

PC 상품공시실과 모바일 상품공시실이 같은 데이터를 다르게 노출한다.
PC는 파일 키가 조회할 때마다 새로 발급되는 암호화 값이고 앞단에 웹방화벽이 있어
브라우저 밖에서는 다운로드가 되지 않는다. 모바일은 같은 목록을 그대로 주면서
다운로드도 주소 한 번으로 끝난다. 그래서 모바일 경로를 쓴다.

목록은 커서 방식이다. 페이지 번호가 아니라 응답이 준 다음 키를 다시 넣어야
그다음 묶음이 온다.
"""

import re
import time
import urllib.parse
from typing import Iterator

from .base import Collector, DocType, DocumentRef, clean_text, decode_html, make_session

_BOARD_URL = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R01.shc?page=CRE"
_LIST_API = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R01C.ajax"
_DOWNLOAD_URL = "https://www.shinhancard.com/mob/MOBFM12051N/MOBFM12051R03.shc"

_CARD_TYPE_CREDIT = "0"  # 0=신용, 1=체크

_SITE_ORIGIN = "https://www.shinhancard.com"
_TERMS_INDEX_URL = f"{_SITE_ORIGIN}/pconts/html/helpdesk/terms/MOBFM170C01.html"

# 약관 목록은 a 태그가 아니라 버튼의 onclick 으로 이동한다. 링크로 뽑히지 않으므로
# 이동 함수에 들어간 경로와 버튼에 적힌 약관 이름을 함께 읽는다.
_TERMS_ENTRY = re.compile(
    r"go\('(/pconts/html/helpdesk/terms/[^']+/index\.html)'\)"
    r".*?listcard__text--title[^>]*>([^<]+)<",
    re.S,
)

# 목록이 가리키는 index.html 에는 본문이 없고 최신 시행본으로 넘기는 한 줄만 들어 있다.
# 개정되면 이 주소가 바뀌므로 최신본 주소를 코드에 적어두지 않고 매번 따라간다.
_TERMS_REDIRECT = re.compile(r"location\.replace\('([^']+)'")

# 넘김이 한 번으로 안 끝나는 경우를 대비한 상한. 잘못 만들어진 페이지가
# 자기 자신을 가리키면 상한이 없을 때 무한히 돈다.
_MAX_REDIRECT_HOPS = 3

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

    def list_member_terms(self) -> Iterator[DocumentRef]:
        """약관 목록 화면에 걸린 항목을 그대로 낸다.

        여기서 최신본 주소까지 알아내지 않는 것은 항목이 90개가 넘기 때문이다.
        항목마다 넘김을 따라가면 목록 한 번에 90번을 요청하게 되는데, 실제로
        받는 것은 그중 서너 개다. 최신본 판별은 다운로드하는 문서에만 한다.
        """
        response = self._session.get(_TERMS_INDEX_URL, timeout=60)
        response.raise_for_status()

        for path, name in _TERMS_ENTRY.findall(decode_html(response.content)):
            term_name = clean_text(name)
            if not term_name:
                continue

            yield DocumentRef(
                issuer=self.issuer,
                card_name=term_name,
                doc_type=DocType.MEMBER_TERMS,
                doc_key=path,
                source_url=f"{_SITE_ORIGIN}{path}",
                # 시행일은 최신본으로 넘어가 봐야 알 수 있다. 목록 단계에서는 비운다.
                revised_at=None,
                file_name=f"{term_name}.html",
            )

    def fetch(self, ref: DocumentRef) -> bytes:
        if ref.doc_type == DocType.MEMBER_TERMS:
            return self._fetch_member_terms(ref)

        self._ensure_session()
        response = self._session.get(ref.source_url, timeout=90)
        response.raise_for_status()
        return response.content

    def _fetch_member_terms(self, ref: DocumentRef) -> bytes:
        """최신 시행본까지 넘김을 따라가 본문 HTML을 받는다.

        상품공시 게시판과 달리 세션 쿠키가 필요 없는 정적 화면이라
        목록 화면을 먼저 여는 절차를 타지 않는다.
        """
        url = ref.source_url

        for _ in range(_MAX_REDIRECT_HOPS):
            response = self._session.get(url, timeout=60)
            response.raise_for_status()
            content = response.content

            target = _TERMS_REDIRECT.search(decode_html(content))
            if target is None:
                return content
            url = urllib.parse.urljoin(url, target.group(1))

        raise ValueError(f"약관 페이지 넘김이 끝나지 않는다: {ref.source_url}")

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
