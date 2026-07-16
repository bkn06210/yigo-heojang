# 카드 혜택 추천 엔진 API 명세

> 담당: 고현준 (추천·계산 엔진)
> 상태: **초안(draft)** — 요청(입력) 필드는 프론트·카드관리와 합의 필요. `⚠️합의필요` 표시 참고.

## 공통 규약

- **응답 봉투**: 모든 응답은 아래 형태로 감싼다.
  ```json
  { "success": true, "data": { ... }, "message": null }
  ```
- **인증**: `Authorization: Bearer <JWT>` 헤더. 회원 id는 토큰에서 추출(요청 body에 안 넣음).
- **컬럼/필드**: DB는 snake_case, API JSON은 camelCase (MyBatis `mapUnderscoreToCamelCase`로 변환).
- **금액 단위**: 원(정수).

---

## 1. 결제 직전 최적 카드 추천

`POST /api/recommendations`

가맹점(또는 카테고리)과 결제 예상금액을 받아, 보유 카드별 예상 혜택을 계산해 이득이 큰 순서로 정렬해 반환. 동적 전환(A 소진→B)과 미래 최적화 경고(실적 미달 손실)를 함께 담는다.

### 요청

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| merchantId | int | X | 가맹점 id. 있으면 가맹점 직접 혜택까지 계산 ⚠️합의필요 |
| categoryId | int | X | 카테고리 id (merchantId 없을 때 폴백) |
| expectedAmount | int | O | 결제 예상금액. 5천원 단위 구간 대표값(중간값) 권장 |

> ⚠️합의필요: 가맹점을 `merchantId`(권장) vs 문자열로 받을지 미결. 프론트가 목록을 merchant에서 받아 뿌리므로 선택 시점에 id 확정 가능 → id 권장.

**요청 예시**
```json
{
  "merchantId": 205,
  "expectedAmount": 25000
}
```

### 응답 (data)

| 필드 | 타입 | 설명 |
|---|---|---|
| recommendations[].rank | int | 추천 순위(1이 최적) |
| recommendations[].userCardId | int | 보유 카드 id |
| recommendations[].cardName | string | 카드명 |
| recommendations[].expectedBenefit | int | 예상 혜택액(원) |
| recommendations[].isEstimate | bool | true=예상(정률+구간), false=확정(정액/상한도달) |
| recommendations[].benefitKind | string | DISCOUNT / SPECIAL_PRICE / GIFT / RETROACTIVE |
| recommendations[].reason | string | 추천 근거(엔진 생성) |
| recommendations[].dynamicSwitch | bool | 동적 전환으로 올라온 카드면 true |
| futureOptimization | object\|null | 실적 미달 경고. 없으면 null |
| futureOptimization.userCardId | int | 대상 보유 카드 id |
| futureOptimization.remainingPerformance | int | 실적까지 남은 금액 |
| futureOptimization.message | string | 경고 문구 |

**응답 예시**
```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "rank": 1,
        "userCardId": 12,
        "cardName": "KB 딥드림 카드",
        "expectedBenefit": 2500,
        "isEstimate": true,
        "benefitKind": "DISCOUNT",
        "reason": "카페 카테고리 10% 청구할인, 이번 달 통합한도 여유 있음",
        "dynamicSwitch": false
      },
      {
        "rank": 2,
        "userCardId": 8,
        "cardName": "현대 Z work",
        "expectedBenefit": 1000,
        "isEstimate": false,
        "benefitKind": "DISCOUNT",
        "reason": "생활 카테고리 정액 할인",
        "dynamicSwitch": true
      }
    ],
    "futureOptimization": {
      "userCardId": 8,
      "cardName": "현대 Z work",
      "remainingPerformance": 200000,
      "message": "이번 달 실적 20만원이 남았습니다. 못 채우면 다음 달 혜택을 통째로 놓칩니다."
    }
  },
  "message": null
}
```

---

## 2. 카드별 월 실적·혜택 현황

`GET /api/cards/{userCardId}/monthly-status`

대시보드용. 전월 실적, 현재 실적 구간, 통합한도 소진, 혜택별 이용 현황을 계산해 반환. 달성률·이용률은 저장값이 아니라 계산값.

### 요청

| 위치 | 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| path | userCardId | int | O | 보유 카드 id |
| query | yearMonth | string | X | 기준 연월(YYYY-MM). 생략 시 이번 달 |

**요청 예시**
```
GET /api/cards/12/monthly-status?yearMonth=2026-07
```

### 응답 (data)

| 필드 | 타입 | 설명 |
|---|---|---|
| userCardId | int | 보유 카드 id |
| cardName | string | 카드명 |
| yearMonth | string | 기준 연월 |
| prevMonthPerformance | int | 전월 실적(엔진이 지난달 소비 합산으로 계산) |
| currentMinAmount | int | 현재 적용 실적 구간의 최소실적금액 |
| sharedLimit | int | 현재 구간의 월 통합할인한도 |
| sharedLimitUsed | int | 통합한도 소진액 |
| achievementRate | float | 실적 달성률(%), 계산값 |
| benefits[].benefitId | int | 혜택 id |
| benefits[].benefitName | string | 혜택명 |
| benefits[].usedValue | int | 이번 달 누적 혜택액 |
| benefits[].monthlyLimit | int\|null | 혜택 월 한도(없으면 null) |
| benefits[].usageRate | float | 혜택 이용률(%), 계산값 |

**응답 예시**
```json
{
  "success": true,
  "data": {
    "userCardId": 12,
    "cardName": "KB 딥드림 카드",
    "yearMonth": "2026-07",
    "prevMonthPerformance": 520000,
    "currentMinAmount": 500000,
    "sharedLimit": 20000,
    "sharedLimitUsed": 8000,
    "achievementRate": 104.0,
    "benefits": [
      {
        "benefitId": 55,
        "benefitName": "카페 10% 청구할인",
        "usedValue": 3000,
        "monthlyLimit": 5000,
        "usageRate": 60.0
      }
    ]
  },
  "message": null
}
```

---

## 3. 결제 후 소진 상태 갱신

`POST /api/settlements`

결제(Mock) 성공 후 소비내역이 생기면, 해당 카드의 실적·혜택 소진 상태를 갱신. 갱신 결과는 다음 추천에 반영. 응답은 2번과 동일한 `CardMonthlyStatus` 구조.

### 요청

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| consumptionId | int | O | 갱신 근거가 되는 소비내역 id |

**요청 예시**
```json
{ "consumptionId": 3001 }
```

### 응답 (data)

2번의 `CardMonthlyStatus`와 동일 (갱신 후 상태).

---

## 공통 에러 응답

| 상태 | 상황 | message 예시 |
|---|---|---|
| 400 | 필수값 누락 | `"expectedAmount는 필수입니다."` |
| 401 | 인증 실패(토큰 없음/만료) | `"인증이 필요합니다."` |
| 404 | 대상 없음(보유카드/소비내역) | `"해당 보유 카드를 찾을 수 없습니다."` |

```json
{ "success": false, "data": null, "message": "인증이 필요합니다." }
```

---

## 미결 안건 (팀 합의 필요)

- **가맹점 입력 방식**: `merchantId`(권장) vs 문자열. 소비내역·Mock결제도 동일 원칙으로 통일 필요.
- **요청 필드 전반**: 프론트가 실제로 보낼 수 있는 형태인지 장은영과 확인.
- **인증**: JWT Bearer 방식이 팀 인증 설계와 일치하는지 이재혁과 확인.
