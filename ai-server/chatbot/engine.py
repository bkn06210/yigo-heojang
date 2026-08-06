"""엔진 API 호출.

할인액·실적 달성률·잔여 한도는 여기서 받아온다. 챗봇이 SQL 로 직접 구하지 않는 이유는
묶음 한도·구간별 개별 한도·횟수 묶음 규칙이 파이썬에 다시 생기기 때문이다. 규칙이 두 곳에
있으면 한쪽만 고쳐졌을 때 화면 숫자와 챗봇 숫자가 갈리고, 어느 쪽이 맞는지 알 수 없게 된다.

인증은 원래 요청의 Authorization 헤더를 그대로 넘겨 쓴다. 챗봇은 토큰을 열어보지 않는다 —
검증은 Spring 필터가 이미 했고, 여기서 또 하면 검증 로직이 두 벌이 된다.
"""

from dataclasses import dataclass
from typing import Any, Dict, Optional

import httpx

# 사람이 답을 기다리는 화면이라 오래 붙잡지 않는다. 엔진 조회는 밀리초 단위로 끝난다.
_TIMEOUT_SECONDS = 5.0


class EngineError(Exception):
    """엔진 호출 실패.

    상태 코드를 남긴다. 401·404 는 사용자에게 설명할 수 있는 상황이고
    5xx 는 그렇지 않아, 답변 문구가 갈린다.
    """

    def __init__(self, status_code: Optional[int], message: str):
        super().__init__(message)
        self.status_code = status_code


@dataclass(frozen=True)
class EngineClient:
    base_url: str
    authorization: Optional[str] = None

    def monthly_status(self, year_month: Optional[str] = None) -> Dict[str, Any]:
        """보유 카드 전체 현황. 카드가 여러 장이어도 한 번만 부른다."""
        params = {"yearMonth": year_month} if year_month else None
        return self._request("GET", "/api/cards/monthly-status", params=params)

    def recommend(self, merchant_id: int, expected_amount: int) -> Dict[str, Any]:
        """결제 직전 최적 카드. 보유 카드 전부를 이득 순으로 돌려준다."""
        payload = {"merchantId": merchant_id, "expectedAmount": expected_amount}
        return self._request("POST", "/api/recommendations", json=payload)

    def _request(self, method: str, path: str, **kwargs) -> Dict[str, Any]:
        headers = {"Authorization": self.authorization} if self.authorization else {}
        try:
            with httpx.Client(base_url=self.base_url, timeout=_TIMEOUT_SECONDS) as client:
                response = client.request(method, path, headers=headers, **kwargs)
        except httpx.RequestError as error:
            # 백엔드가 안 떠 있는 경우다. 사용자에게는 조회 실패로만 보인다.
            raise EngineError(None, f"엔진에 연결하지 못했습니다: {error}") from error

        if response.status_code >= 400:
            raise EngineError(response.status_code, _error_message(response))

        body = response.json()
        # 팀 공통 응답 봉투를 벗겨 data 만 넘긴다. 봉투는 프론트가 보는 형식이라
        # 챗봇 안까지 끌고 다니면 접근 코드가 전부 body["data"]["..."] 가 된다.
        return body.get("data") or {}


def _error_message(response: httpx.Response) -> str:
    try:
        return response.json().get("message") or response.text
    except ValueError:
        return response.text
