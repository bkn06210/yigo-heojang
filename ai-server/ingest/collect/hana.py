"""하나카드 약관 수집 어댑터.

상품공시 화면이 목록을 HTML로 그리지 않고 같은 주소의 `.ajax` 엔드포인트에서 JSON으로
받아 채운다. 그래서 화면을 긁지 않고 그 엔드포인트를 직접 부른다 — 카드명·카드코드·
시행일·PDF 주소가 한 항목에 다 들어 있어 KB처럼 목록에서 상세로 한 번 더 들어갈 필요가 없다.

다루기 까다로운 지점이 셋이다.

<b>페이징이 페이지 번호가 아니라 커서다.</b> `page` 파라미터는 서버가 받기는 하지만 값을
그대로 돌려줄 뿐 언제나 첫 열 건을 준다. 응답의 다음 커서를 받아 다시 넣는 것을 되풀이해야
하고, 커서가 빈 문자열이 되면 끝이다. 첫 요청은 실재하는 커서를 모르므로 아주 큰 값을 넣어
맨 앞부터 받는다.

<b>응답이 EUC-KR인데 Content-Type은 text/html이라고 말한다.</b> 인코딩 자동 판별에 맡기면
카드명이 통째로 깨진다. 오류가 나지 않고 글자만 깨지므로 수집이 끝난 뒤에야 알게 된다.

<b>목록이 한 겹 더 싸여 있다.</b> `dataMap.RESULT_LIST.data` 안에 항목 배열이 들어 있다.
"""

import json
import time
from typing import Dict, Iterator, List

from .base import Collector, DocType, DocumentRef, clean_text, make_session

_LIST_URL = "https://www.hanacard.co.kr/OSA95000000D.ajax"
_REFERER = (
    "https://www.hanacard.co.kr/OSA95000000D.web"
    "?schID=pcd&mID=OSA95000000D&CT_ID=CDPD"
)

# 화면의 탭 구분. 상품설명서와 회원약관이 같은 엔드포인트를 쓰고 이 값으로만 갈린다.
_TAB_PRODUCT_GUIDE = "CDPD"
_TAB_MEMBER_TERMS = "TERMS"

# 첫 요청에 넣을 커서. 실재하는 값보다 크기만 하면 맨 앞부터 받는다.
_CURSOR_SEED = "9999999999999"

# 커서가 돌지 않고 같은 값이 계속 오는 경우를 대비한 상한.
# 한 번에 열 건씩이므로 카드가 수천 장이 되어도 여유가 있다.
_MAX_PAGES = 300

_REQUEST_INTERVAL_SEC = 0.5


class HanaCollector(Collector):
    issuer = "하나"

    def __init__(self) -> None:
        self._session = make_session(referer=_REFERER)

    def list_documents(self) -> Iterator[DocumentRef]:
        for row in self._list_rows(_TAB_PRODUCT_GUIDE):
            ref = self._to_ref(row, DocType.PRODUCT_GUIDE)
            if ref is not None:
                yield ref

    def list_member_terms(self) -> Iterator[DocumentRef]:
        """카드사 단위 회원약관. 카드가 아니라 카드사 전체에 걸리는 문서다."""
        for row in self._list_rows(_TAB_MEMBER_TERMS):
            ref = self._to_ref(row, DocType.MEMBER_TERMS)
            if ref is not None:
                yield ref

    def fetch(self, ref: DocumentRef) -> bytes:
        response = self._session.get(ref.source_url, timeout=60)
        response.raise_for_status()
        return response.content

    # ── 내부 ────────────────────────────────────────────────

    def _list_rows(self, tab: str) -> Iterator[Dict[str, str]]:
        """커서를 따라가며 목록을 끝까지 읽는다."""
        cursor = _CURSOR_SEED
        seen_cursors = {cursor}

        for _ in range(_MAX_PAGES):
            payload = {"CT_ID": tab, "AMM_NEXT_KEY": cursor, "SEARCHKEY": ""}
            response = self._session.post(_LIST_URL, data=payload, timeout=60)
            response.raise_for_status()
            # 헤더가 text/html이라 자동 판별에 맡기면 한글이 깨진다.
            response.encoding = "euc-kr"

            data = json.loads(response.text).get("dataMap") or {}
            for row in _rows_of(data):
                yield row

            cursor = (data.get("AMM_NEXT_KEY") or "").strip()
            if not cursor or cursor in seen_cursors:
                # 빈 값이면 끝이다. 같은 커서가 다시 오면 서버가 진행하지 않는 것이므로
                # 여기서 멈춘다 — 그대로 두면 같은 열 건을 끝없이 되받는다.
                return
            seen_cursors.add(cursor)
            time.sleep(_REQUEST_INTERVAL_SEC)

    def _to_ref(self, row: Dict[str, str], doc_type: str) -> "DocumentRef | None":
        file_name = clean_text(row.get("APN_FILE_NM"))
        base_path = clean_text(row.get("APN_FILE_PH_NM"))
        card_name = clean_text(row.get("AN_TIT_NM"))
        if not (file_name and base_path and card_name):
            # 파일이 안 걸린 항목이 섞여 온다. 주소를 지어내면 없는 문서를 받으러 간다.
            return None

        return DocumentRef(
            issuer=self.issuer,
            card_name=card_name,
            doc_type=doc_type,
            doc_key=clean_text(row.get("ADD_VAR3")) or file_name,
            source_url=base_path + file_name,
            revised_at=clean_text(row.get("AN_SDT")) or None,
            file_name=file_name,
        )


def _rows_of(data: Dict) -> List[Dict[str, str]]:
    """목록 항목을 꺼낸다.

    `RESULT_LIST`가 배열이 아니라 `{"data": [...]}` 로 한 겹 싸여 있고,
    항목이 없을 때는 키 자체가 오지 않는다.
    """
    result_list = data.get("RESULT_LIST")
    if isinstance(result_list, dict):
        rows = result_list.get("data")
    else:
        rows = result_list
    return rows if isinstance(rows, list) else []
