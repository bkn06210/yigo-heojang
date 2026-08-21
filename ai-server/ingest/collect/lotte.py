"""롯데카드 약관 수집 어댑터.

상품공시 화면이 검색 엔진 위에 올라가 있어, 화면을 긁는 대신 그 검색 엔드포인트를 부른다.
한 번에 백 건씩 받을 수 있고 응답에 PDF 파일명이 그대로 들어 있어, 카드 상세 화면을
거치지 않고 목록 → 내려받기 두 단계로 끝난다(상세 화면에는 애초에 약관 링크가 없다).

주의할 지점이 둘이다.

<b>응답이 JSON 안에 JSON이다.</b> 바깥 껍데기의 `Content`가 객체가 아니라 문자열이라
한 번 더 풀어야 목록이 나온다.

<b>파일명을 규칙으로 만들 수 없다.</b> 실제로 세 가지 형식이 섞여 있고 한글 파일명도 있다.
카드 코드로 주소를 조립하려 들면 대부분 404가 되므로 반드시 목록에서 받은 이름을 쓴다.
"""

import json
import time
import re
from typing import Dict, Iterator, List

from .base import Collector, DocType, DocumentRef, clean_text, make_session

_SEARCH_URL = "https://www.lottecard.co.kr/app/LPSCHAA_V100.lc"
_FILE_BASE = "https://image.lottecard.co.kr/UploadFiles/cardProvisionPath/"
_REFERER = "https://www.lottecard.co.kr/"

# 상품설명서가 담긴 검색 묶음. 회원약관은 여기 들어 있지 않다.
_COLLECTION_DISCLOSURE = "disclosure"

# 한 번에 받을 건수. 기본이 열 건이라 그대로 두면 요청이 쉰 번을 넘어간다.
_PAGE_SIZE = 100

# 응답이 총 건수를 주지만 그 값만 믿고 끝내지 않기 위한 상한.
_MAX_PAGES = 30

_REQUEST_INTERVAL_SEC = 0.5

# 검색어를 넣으면 결과 문자열에 일치 구간 표시가 섞여 온다. 카드명에 그대로 두면
# 카드 마스터와 이름이 맞지 않아 매칭이 실패한다.
_HIGHLIGHT = re.compile(r"<!H[SE]>")


class LotteCollector(Collector):
    issuer = "롯데"

    def __init__(self) -> None:
        self._session = make_session(referer=_REFERER)

    def list_documents(self) -> Iterator[DocumentRef]:
        for row in self._list_rows():
            file_name = clean_text(row.get("OCY_FILE_NM"))
            card_name = _clean_name(row.get("VT_CD_KND_NM"))
            if not (file_name and card_name):
                # 파일이 안 걸린 항목이 섞여 온다. 주소를 지어내면 없는 문서를 받으러 간다.
                continue

            yield DocumentRef(
                issuer=self.issuer,
                card_name=card_name,
                doc_type=DocType.PRODUCT_GUIDE,
                doc_key=file_name,
                source_url=_FILE_BASE + file_name,
                revised_at=_revised_at(file_name),
                file_name=file_name,
            )

    def list_member_terms(self) -> Iterator[DocumentRef]:
        """카드사 단위 회원약관.

        <b>아직 경로를 찾지 못했다.</b> 상품공시 검색 묶음에는 상품설명서만 있고
        회원약관은 들어 있지 않다("회원약관"·"개인회원"으로 검색하면 0건이다).

        빈 목록을 조용히 돌려주면 이 카드사 카드에서만 분실·해지 질문에 답하지 못하는
        상태가 되므로, 찾기 전까지는 부르는 쪽이 알 수 있게 막아 둔다.
        상품설명서 수집은 이것과 무관하게 동작한다.
        """
        raise NotImplementedError(
            "롯데카드 개인회원 약관 경로를 아직 찾지 못했다. "
            "상품공시 검색(collection=disclosure)에는 상품설명서만 들어 있다."
        )

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=60)
        response.raise_for_status()
        return response.content

    # ── 내부 ────────────────────────────────────────────────

    def _list_rows(self) -> Iterator[Dict[str, str]]:
        start = 0
        for _ in range(_MAX_PAGES):
            payload = {
                "collection": _COLLECTION_DISCLOSURE,
                "listcount": _PAGE_SIZE,
                "startcount": start,
                "query": "",
            }
            response = self._session.post(_SEARCH_URL, data=payload, timeout=60)
            response.raise_for_status()

            rows = _rows_of(response.json())
            if not rows:
                return
            for row in rows:
                yield row

            start += len(rows)
            if len(rows) < _PAGE_SIZE:
                return
            time.sleep(_REQUEST_INTERVAL_SEC)


def _rows_of(outer: Dict) -> List[Dict[str, str]]:
    """이중으로 싸인 응답에서 목록을 꺼낸다."""
    content = outer.get("Content")
    inner = json.loads(content) if isinstance(content, str) else outer
    collections = ((inner.get("result") or {}).get("collection")) or []
    if not collections:
        return []
    docs = collections[0].get("docs")
    return docs if isinstance(docs, list) else []


def _clean_name(value) -> str:
    return clean_text(_HIGHLIGHT.sub("", value or ""))


def _revised_at(file_name: str) -> "str | None":
    """파일명에 섞인 개정일. 형식이 여러 가지라 여덟 자리 날짜만 집는다."""
    match = re.search(r"(20\d{6})", file_name)
    return match.group(1) if match else None
