"""카드사 약관 수집 어댑터의 공통 계약.

카드사마다 사이트 구조가 전혀 다르다. 실제로 확인한 것만 해도
응답 형식(JSON / form-encoded / HTML), 인코딩(UTF-8 / EUC-KR),
페이징(페이지 번호 / 커서 / 없음), 세션 필요 여부가 모두 갈린다.

그 차이를 전부 어댑터 안에 가두고, 밖으로는 DocumentRef 목록과
PDF 바이트만 내보낸다. 그래야 카드사를 추가할 때 이 파일 아래쪽
(extract / structure / load)이 한 줄도 바뀌지 않는다.
"""

import html
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Iterator, Optional

import requests


class DocType:
    """문서 유형.

    카드사마다 문서 구성이 다르다. 혜택·실적 조건이 어느 문서에 실리는지도
    카드사마다 달라서, 유형을 구분해두지 않으면 어떤 문서를 구조화 대상으로
    삼을지 판단할 수 없다.
    """

    PRODUCT_GUIDE = "PRODUCT_GUIDE"  # 상품설명서·이용안내장
    KEY_TERMS = "KEY_TERMS"  # 주요거래조건


@dataclass(frozen=True)
class DocumentRef:
    """수집 대상 문서 하나를 가리키는 값.

    목록 조회의 결과이자 다운로드의 입력이다. 목록은 싸고 다운로드는 비싸므로
    두 단계를 분리해, 목록을 다 받아본 뒤 무엇을 내려받을지 고를 수 있게 한다.
    """

    issuer: str
    card_name: str  # 카드사가 표기한 이름 그대로. 카드 마스터 매칭의 근거가 된다
    doc_type: str
    doc_key: str  # 카드사 내부 식별자. 형식은 카드사마다 다르다
    source_url: str
    revised_at: Optional[str] = None  # YYYYMMDD. 개정 감지에 쓴다
    file_name: Optional[str] = None


class Collector(ABC):
    """카드사 한 곳의 수집 어댑터.

    카드사를 추가하는 일은 이 클래스를 상속한 파일 하나를 더 만드는 것으로 끝난다.
    """

    issuer: str

    @abstractmethod
    def list_documents(self) -> Iterator[DocumentRef]:
        """수집 가능한 문서 목록. 다운로드는 하지 않는다."""

    @abstractmethod
    def fetch(self, ref: DocumentRef) -> bytes:
        """문서 원본(PDF) 바이트."""


# 브라우저와 같은 값을 쓴다. 일부 카드사는 이 헤더가 없으면 응답을 주지 않는다.
_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
)


def clean_text(value: Optional[str]) -> str:
    """카드사가 준 문자열을 사람이 읽는 형태로 되돌린다.

    화면에 뿌릴 목적으로 만들어진 값이라 '&amp;'나 '&#x27;' 같은 표기가 섞여 온다.
    그대로 두면 '삼성카드 &amp; POINT'가 되어 카드 마스터와 이름이 맞지 않는다.
    """
    return html.unescape((value or "").strip())


def make_session(referer: str) -> requests.Session:
    """카드사 사이트용 세션.

    Session을 쓰는 이유는 쿠키가 유지되어야 하는 카드사가 있기 때문이다.
    목록 페이지를 먼저 열어 발급받은 세션으로만 다운로드가 허용되는 곳이 있다.
    """
    session = requests.Session()
    session.headers.update({"User-Agent": _USER_AGENT, "Referer": referer})
    return session
