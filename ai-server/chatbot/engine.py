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

    def benefit_report(self, year_month: Optional[str] = None) -> Dict[str, Any]:
        """한 달 동안 실제로 받은 혜택. 총액·부문별 합계·거래 목록이 함께 온다.

        혜택액은 결제 시점에 엔진이 확정해 소비내역에 적어 둔 값이라 여기서 다시 구하지 않는다.
        다시 계산하면 그때의 한도 상태를 재현할 수 없어 실제와 달라진다.
        """
        params = {"yearMonth": year_month} if year_month else None
        return self._request("GET", "/api/benefits/report", params=params)

    def card_recommendations(self) -> Dict[str, Any]:
        """소비 내역을 근거로 한 카드 추천. 입력이 없다 — 근거가 회원의 직전월 소비다.

        금액은 전부 엔진이 계산한 값이다. 순증(monthlyGainAmount)은 지금 카드로 받는 금액과의
        차액이라 이 서버가 다시 더하거나 빼면 안 된다.
        """
        return self._request("GET", "/api/cards/recommendations")

    def applicable_benefits(self, merchant_id: Optional[int] = None,
                            category_id: Optional[int] = None) -> Dict[str, Any]:
        """이 가맹점·업종에 걸린 혜택과 그 조건. 금액이 없어도 답할 수 있다.

        추천과 답하는 질문이 다르다. 추천은 "지금 8,000원이면 어느 카드가 유리한가"이고
        이쪽은 "여기서 뭐가 좋아?"다. 뒤쪽은 금액이 없는 것이 정상이라 되물을 일이 아니다.
        """
        params: Dict[str, Any] = {}
        if merchant_id is not None:
            params["merchantId"] = merchant_id
        if category_id is not None:
            params["categoryId"] = category_id
        return self._request("GET", "/api/cards/applicable-benefits", params=params)

    def recommend(self, expected_amount: int, merchant_id: Optional[int] = None,
                  category_id: Optional[int] = None) -> Dict[str, Any]:
        """결제 직전 최적 카드. 보유 카드 전부를 이득 순으로 돌려준다.

        가맹점을 특정하면 그 가맹점 혜택까지 보고, 업종만 알면 업종 혜택까지만 본다.
        입력이 구체적일수록 계산 범위가 넓어지는 것은 엔진 쪽 규칙이다.
        """
        payload: Dict[str, Any] = {"expectedAmount": expected_amount}
        if merchant_id is not None:
            payload["merchantId"] = merchant_id
        if category_id is not None:
            payload["categoryId"] = category_id
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
