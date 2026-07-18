# 카드 혜택 추천 엔진 API 명세

> 추천·계산 엔진 API. 상태: **초안(draft)** — 요청(입력) 필드는 프론트·카드 관리와 합의 필요. `⚠️합의필요` 표시 참고.

## 공통 규약

- **응답 봉투**: 모든 응답은 아래 형태로 감싼다.
  ```json
  { "success": true, "code": "SUCCESS", "data": { ... }, "message": null }
  ```
- **code**: 기계 판독용 결과 코드. 성공은 `SUCCESS`, 실패는 에러 코드(하단 공통 에러 응답 참고). **프론트 분기는 message(문구)가 아니라 code로 한다** — 문구는 자유롭게 바뀔 수 있다.
- **인증**: `Authorization: Bearer <JWT>` 헤더. 회원 id는 토큰에서 추출(요청 body에 안 넣음).
- **필드 표기**: DB는 snake_case, API JSON은 camelCase (MyBatis `mapUnderscoreToCamelCase`로 변환).
- **금액 단위**: 원(정수).
- **화면 표기**: "AI 추천/AI 브리핑" 대신 **"추천 결과/추천 근거"**로 표기. 계산·판단은 엔진이 하고 LLM은 표현만 담당하므로, 금액·추천을 AI가 만든 것처럼 라벨링하지 않는다.

## 엔드포인트 목록

| #   | 메서드 | 경로                                   | 용도                                                            |
| --- | ------ | -------------------------------------- | --------------------------------------------------------------- |
| 1   | POST   | /api/recommendations                   | 결제 직전 최적 카드 추천 (동적전환·미래최적화·포인트/적립 안내) |
| 2   | GET    | /api/cards/monthly-status              | **전체** 보유 카드 현황 (대시보드 홈·보유카드 목록)             |
| 3   | GET    | /api/cards/{userCardId}/monthly-status | **개별** 카드 상세 현황                                         |
| 4   | POST   | /api/settlements/cancel                | 결제 취소 시 상태 차감 (결제 가산은 결제 처리에 내부 연동)      |
| 5   | GET    | /api/points/recommendations            | 포인트 추천 (금융포인트 사용처 + 미등록 멤버십 가입)            |

---

## 1. 결제 직전 최적 카드 추천

`POST /api/recommendations`

가맹점(또는 카테고리)과 결제 예상금액을 받아, 보유 카드별 예상 혜택을 계산해 이득이 큰 순서로 정렬해 반환. 동적 전환(A 소진→B), 미래 최적화 경고, 결제 직전 포인트 안내(금융포인트 잔액 + 등록 멤버십 적립)를 함께 담는다.

### 요청

| 필드           | 타입 | 필수 | 설명                                               |
| -------------- | ---- | ---- | -------------------------------------------------- |
| merchantId     | int  | X    | 가맹점 id. 있으면 가맹점 직접 혜택까지 계산        |
| categoryId     | int  | X    | 카테고리 id (merchantId 없을 때 폴백)              |
| expectedAmount | int  | O    | 결제 예상금액. 5천원 단위 구간 대표값(중간값) 권장 |

> 가맹점은 `merchantId`로 받는다(결정). 프론트가 merchant 목록에서 뿌리므로 선택 시점에 id를 확정할 수 있다.

**입력 조합별 계산 범위** (입력이 구체적일수록 추천이 정밀해진다):

| 입력              | 계산 범위                                                                                               |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| merchantId 있음   | 가맹점 직접 혜택 + 해당 카테고리 혜택 + 전체(ALL) 혜택                                                  |
| categoryId만 있음 | 카테고리 혜택 + 전체(ALL) 혜택                                                                          |
| 둘 다 없음        | 전체(ALL) 혜택 + 실적 진행 상황 기반 추천 — 장소 미정 상태에서 "지금 어느 카드를 쓰는 게 유리한지" 안내 |

**요청 예시**

```json
{ "merchantId": 205, "expectedAmount": 11900 }
```

### 응답 (data)

| 필드                                    | 타입         | 설명                                                     |
| --------------------------------------- | ------------ | -------------------------------------------------------- |
| recommendations[].rank                  | int          | 추천 순위(1이 최적)                                      |
| recommendations[].userCardId            | int          | 보유 카드 id                                             |
| recommendations[].cardName              | string       | 카드명                                                   |
| recommendations[].expectedBenefit       | int          | 예상 혜택액(원)                                          |
| recommendations[].isEstimate            | bool         | true=예상(정률+구간), false=확정(정액/상한도달)          |
| recommendations[].benefitKind           | string       | DISCOUNT / SPECIAL_PRICE / GIFT / RETROACTIVE            |
| recommendations[].reason                | string       | 추천 근거(엔진 생성)                                     |
| recommendations[].dynamicSwitch         | bool         | 동적 전환으로 올라온 카드면 true                         |
| futureOptimization                      | object\|null | 실적 미달 경고. 없으면 null                              |
| futureOptimization.userCardId           | int          | 대상 보유 카드 id                                        |
| futureOptimization.cardName             | string       | 카드명                                                   |
| futureOptimization.remainingPerformance | int          | 실적까지 남은 금액                                       |
| futureOptimization.message              | string       | 경고 문구                                                |
| pointGuide                              | object\|null | 결제 직전 **금융포인트 잔액** 안내. 없으면 null          |
| pointGuide.pointBrandName               | string       | 금융포인트사명 (예: 마이신한포인트)                      |
| pointGuide.usablePoint                  | int          | 보유 잔액(이 결제에 사용 가능)                           |
| pointGuide.message                      | string       | 안내 문구                                                |
| membershipEarn[]                        | array        | 결제 가맹점에서 적립되는 **등록 멤버십** 안내. 없으면 [] |
| membershipEarn[].pointBrandName         | string       | 멤버십명 (예: CJ ONE)                                    |
| membershipEarn[].message                | string       | 적립 안내 문구                                           |

> 포인트 안내 구분: `pointGuide`는 **금융포인트**(카드사, 잔액 조회 가능) 기준. **멤버십**(CJ ONE 등)은 잔액 미연동 → 잔액 대신 `membershipEarn`(등록 시 적립 가능 안내)만.

**응답 예시**

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "recommendations": [
      {
        "rank": 1,
        "userCardId": 12,
        "cardName": "신한 Deep Dream",
        "expectedBenefit": 1000,
        "isEstimate": true,
        "benefitKind": "DISCOUNT",
        "reason": "쇼핑 할인 혜택 적용, 최근 소비 패턴 반영",
        "dynamicSwitch": false
      }
    ],
    "futureOptimization": null,
    "pointGuide": {
      "pointBrandName": "마이신한포인트",
      "usablePoint": 97842,
      "message": "마이신한포인트 97,842P 보유 중. 이 결제에 사용할 수 있습니다."
    },
    "membershipEarn": [
      {
        "pointBrandName": "CJ ONE",
        "message": "CJ ONE 멤버십이 등록되어 있어요. 올리브영 결제 시 적립됩니다. 결제 후 적립 여부를 확인해 보세요."
      }
    ]
  },
  "message": null
}
```

---

## 2. 전체 보유 카드 현황 (대시보드 홈·보유카드 목록)

`GET /api/cards/monthly-status`

대시보드 홈, 보유 카드 목록 화면용. 로그인 사용자의 **보유 카드 전부**를 한 번에 반환. 카드가 여러 장이라도 한 번의 호출로 처리(개별 호출 반복 금지).

> 2번(목록)과 3번(상세)의 분리 근거: 목록에서 카드마다 전체 혜택 상세를 반복 전송하면 응답이 커지므로, 목록은 요약(`benefitsSummary`)만, 혜택 상세(`benefits`, 이용률 포함)는 3번 개별 호출로 나눈다.

### 요청

| 위치  | 필드      | 타입   | 필수 | 설명                                |
| ----- | --------- | ------ | ---- | ----------------------------------- |
| query | yearMonth | string | X    | 기준 연월(YYYY-MM). 생략 시 이번 달 |

**요청 예시**

```
GET /api/cards/monthly-status?yearMonth=2026-07
```

### 응답 (data)

`cards` 배열(실적 진행 요약, 혜택 상세는 3번에서) + `briefing`(홈 상단 브리핑).

| 필드                                     | 타입         | 설명                                                                                           |
| ---------------------------------------- | ------------ | ---------------------------------------------------------------------------------------------- |
| briefing                                 | object\|null | 홈 상단 브리핑 — 실적 달성이 가장 임박한 카드 안내. 판단·문구 모두 엔진 생성. 대상 없으면 null |
| briefing.userCardId                      | int          | 가장 임박한 보유 카드 id                                                                       |
| briefing.cardName                        | string       | 카드명                                                                                         |
| briefing.achievementRate                 | float        | 해당 카드 달성률(%)                                                                            |
| briefing.remainingPerformance            | int          | 남은 실적 금액                                                                                 |
| briefing.message                         | string       | 브리핑 문구 (엔진 템플릿 생성 — 화면 표기는 "추천"으로, "AI 브리핑" 라벨 지양)                 |
| cards[].userCardId                       | int          | 보유 카드 id                                                                                   |
| cards[].cardName                         | string       | 카드명                                                                                         |
| cards[].yearMonth                        | string       | 기준 연월                                                                                      |
| cards[].currentMonthSpending             | int          | 이번 달 누적 실적 인정액                                                                       |
| cards[].targetPerformance                | int          | 실적 목표 금액(혜택 유지/달성 기준)                                                            |
| cards[].remainingPerformance             | int          | 남은 실적 금액(target − current)                                                               |
| cards[].achievementRate                  | float        | 실적 달성률(%), 계산값                                                                         |
| cards[].sharedLimit                      | int          | 현재 구간의 월 통합할인한도                                                                    |
| cards[].sharedLimitUsed                  | int          | 통합한도 소진액                                                                                |
| cards[].benefitsSummary[]                | array        | 홈 위젯 "남은 혜택" 표시용 — 잔여 한도가 남은 혜택 요약. 상세는 3번                            |
| cards[].benefitsSummary[].benefitId      | int          | 혜택 id                                                                                        |
| cards[].benefitsSummary[].benefitName    | string       | 혜택명 (예: 교통 10% 할인)                                                                     |
| cards[].benefitsSummary[].remainingLimit | int\|null    | 잔여 한도(한도 없으면 null)                                                                    |

**응답 예시**

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "briefing": {
      "userCardId": 12,
      "cardName": "삼성 ID ON",
      "achievementRate": 90.0,
      "remainingPerformance": 50000,
      "message": "보유하신 카드 3장 중 삼성 ID ON 카드 실적이 90%로 가장 임박했어요. 이번 달은 이 카드부터 채우는 걸 추천드려요."
    },
    "cards": [
      {
        "userCardId": 12,
        "cardName": "삼성 ID ON",
        "yearMonth": "2026-07",
        "currentMonthSpending": 450000,
        "targetPerformance": 500000,
        "remainingPerformance": 50000,
        "achievementRate": 90.0,
        "sharedLimit": 20000,
        "sharedLimitUsed": 8000,
        "benefitsSummary": [
          {
            "benefitId": 31,
            "benefitName": "교통 10% 할인",
            "remainingLimit": 5000
          },
          {
            "benefitId": 55,
            "benefitName": "편의점 2천원 할인",
            "remainingLimit": 2000
          }
        ]
      }
    ]
  },
  "message": null
}
```

---

## 3. 개별 카드 상세 현황

`GET /api/cards/{userCardId}/monthly-status`

카드 상세 화면용. 특정 보유 카드 한 장의 실적 현황과 **혜택별 이용 현황(잔여 한도 포함)**까지 상세 반환. 달성률·이용률·잔여 한도는 저장값이 아니라 계산값.

### 요청

| 위치  | 필드       | 타입   | 필수 | 설명                                |
| ----- | ---------- | ------ | ---- | ----------------------------------- |
| path  | userCardId | int    | O    | 보유 카드 id                        |
| query | yearMonth  | string | X    | 기준 연월(YYYY-MM). 생략 시 이번 달 |

**요청 예시**

```
GET /api/cards/12/monthly-status?yearMonth=2026-07
```

### 응답 (data) — CardMonthlyStatus

| 필드                      | 타입      | 설명                                                  |
| ------------------------- | --------- | ----------------------------------------------------- |
| userCardId                | int       | 보유 카드 id                                          |
| cardName                  | string    | 카드명                                                |
| yearMonth                 | string    | 기준 연월                                             |
| prevMonthPerformance      | int       | 전월 실적(현재 구간·한도 판정 기준, 엔진 계산)        |
| targetPerformance         | int       | 실적 목표 금액(혜택 유지/달성 기준, 예: 50만)         |
| currentMonthSpending      | int       | 이번 달 누적 실적 인정액                              |
| remainingPerformance      | int       | 남은 실적 금액(target − current)                      |
| achievementRate           | float     | 실적 달성률(%), 계산값                                |
| sharedLimit               | int       | 현재 구간의 월 통합할인한도                           |
| sharedLimitUsed           | int       | 통합한도 소진액                                       |
| benefits[].benefitId      | int       | 혜택 id                                               |
| benefits[].benefitName    | string    | 혜택명                                                |
| benefits[].usedValue      | int       | 이번 달 누적 혜택액                                   |
| benefits[].monthlyLimit   | int\|null | 혜택 월 한도(없으면 null)                             |
| benefits[].remainingLimit | int\|null | 잔여 한도(monthlyLimit − usedValue, 한도 없으면 null) |
| benefits[].usageRate      | float     | 혜택 이용률(%), 계산값                                |

**응답 예시**

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "userCardId": 12,
    "cardName": "삼성 ID ON",
    "yearMonth": "2026-07",
    "prevMonthPerformance": 520000,
    "targetPerformance": 500000,
    "currentMonthSpending": 400000,
    "remainingPerformance": 100000,
    "achievementRate": 80.0,
    "sharedLimit": 20000,
    "sharedLimitUsed": 8000,
    "benefits": [
      {
        "benefitId": 55,
        "benefitName": "편의점/약국 All Day 10% 할인",
        "usedValue": 8000,
        "monthlyLimit": 10000,
        "remainingLimit": 2000,
        "usageRate": 80.0
      }
    ]
  },
  "message": null
}
```

---

## 4. 결제·취소 시 상태 갱신

### 4-A. 결제 성공 → 내부 연동 (REST 아님)

결제(Mock) 처리는 소비내역 도메인 담당이며, 그 **결제 트랜잭션 안에서 엔진 정산 서비스를 자바 메서드로 직접 호출**한다 (같은 WAR·같은 DB — 자기 자신에게 HTTP를 쏠 이유가 없음).

```
결제 처리 (@Transactional, 소비내역 도메인)
 ├ consumption INSERT
 └ 엔진 정산 서비스 내부 호출
    ├ 혜택 계산 → applied_benefit_id, discount_amount 기록
    └ 상태 가산: 당월누적실적 +, 통합한도사용 +, 혜택별 사용액·횟수 +
 → 전부 성공 시 커밋 (원자적 — "결제됐는데 혜택 미기록" 상태가 존재할 수 없음)
```

- 결제 응답에 적용 혜택·갱신 현황을 담을지는 결제 API(소비내역 담당) 명세에서 정한다.
- 엔진 정산 서비스의 메서드 시그니처는 소비내역 담당과 협의. (미결 안건)

> 놓친 혜택(최적 대비 차액)은 계산·저장하지 않는다. (범위 밖)

### 4-B. 결제 취소 → 상태 차감

`POST /api/settlements/cancel`

결제 취소는 앱 기능이 아니라 외부(카드사/가맹점)에서 발생해 데이터로 유입되는 사실이다. 마이데이터 동기화·Mock 취소로 취소 건이 유입될 때 호출한다. 4-A(가산)의 짝.

**취소 처리 원칙** (실제 카드사 동작 기준):

| 원칙 | 내용 |
|---|---|
| 소급 재계산 금지 | 취소돼도 **다른 거래**의 applied_benefit_id·discount_amount는 불변. 이미 승인된 거래에 할인이 뒤늦게 붙지 않는다(실제 카드사 동작과 동일). 과거 기록은 불변 |
| 취소 건 기여분만 역산 차감 | 당월누적실적 −, 통합한도사용 −, 혜택별 사용액 −, 적용횟수 − (월 전체 재계산 아님) |
| 복원 정책: **복원함** | 실적·통합한도·개별한도·횟수 모두 차감(복원). 상시 혜택은 취소 시 사용액 차감이 카드사 공식 기준(BC 부가서비스 기준). 미복원 사례는 프로모션 특약 영역 → 범위 밖 |
| 복원분은 이후부터 | 복원된 여유는 **이후 추천부터** 반영 (소급 없음) |
| 전액 취소만 | 부분 취소는 범위 밖 |
| 당월 취소만 | 전월 거래 취소는 범위 밖 — 전월실적 변경 시 실적구간·통합한도 전체 재판정이 필요하므로 별도 판단 전까지 미처리 |

> 각주: `최종적용일시`는 이력이 없어 되돌리지 않는다 — 취소 당일의 일1회 판정만 보수적으로 동작(허용).
> 표기: 청구할인은 월말 확정이므로 월중 값은 전부 예상치다. 취소로 숫자가 바뀌는 게 정상이며, 화면은 `isEstimate`로 예상 표기한다.

### 요청

| 필드          | 타입 | 필수 | 설명               |
| ------------- | ---- | ---- | ------------------ |
| consumptionId | int  | O    | 취소된 소비내역 id |

**요청 예시**

```json
{ "consumptionId": 3001 }
```

### 처리

1. 소비내역 결제상태 → CANCELED 반영 (상태 변경 주체는 소비내역 담당과 협의 — 미결 안건)
2. 취소 처리 원칙대로 해당 건의 기여분을 상태에서 역산 차감

### 응답 (data)

차감 반영된 카드 현황 — 3번 `CardMonthlyStatus`와 동일 구조.

**응답 예시**

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "userCardId": 12,
    "cardName": "삼성 ID ON",
    "currentMonthSpending": 386100,
    "sharedLimitUsed": 9110,
    "...": "..."
  },
  "message": null
}
```

---

## 5. 포인트 추천 (금융포인트 사용처 + 미등록 멤버십 가입)

`GET /api/points/recommendations`

소비내역 + 포인트/멤버십 정보를 분석해 두 가지를 추천한다. 포인트 잔액 단순 조회가 아니라 **소비 패턴 해석 → 추천 도출**이다.

1. **금융포인트 사용처 추천**(`usage`): 보유 금융포인트(잔액 조회 가능)를 소비 이력 기반으로 어디서 쓸지.
2. **미등록 멤버십 가입 권유**(`unregistered`): 소비는 많은데 미등록인 멤버십사 (예: 파리바게트 소비 많은데 해피포인트 미등록 → 등록 권유).

### 요청

인증 토큰만 필요. body 없음.

**요청 예시**

```
GET /api/points/recommendations
```

### 응답 (data)

| 필드                          | 타입   | 설명                       |
| ----------------------------- | ------ | -------------------------- |
| usage[].pointBrandId          | int    | 보유 금융포인트사 id       |
| usage[].pointBrandName        | string | 보유 금융포인트사명        |
| usage[].usablePoint           | int    | 사용 가능 포인트(잔액)     |
| usage[].suggestedMerchant     | string | 추천 사용처                |
| usage[].reason                | string | 추천 근거 (소비 이력 기반) |
| usage[].expiringSoon          | bool   | 소멸 임박 여부             |
| unregistered[].pointBrandId   | int    | 미등록 멤버십사 id         |
| unregistered[].pointBrandName | string | 멤버십사명                 |
| unregistered[].reason         | string | 추천 근거 (소비 분석 결과) |

**응답 예시**

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "usage": [
      {
        "pointBrandId": 1,
        "pointBrandName": "마이신한포인트",
        "usablePoint": 97842,
        "suggestedMerchant": "올리브영",
        "reason": "최근 올리브영 방문 이력이 있어 사용을 추천합니다.",
        "expiringSoon": false
      }
    ],
    "unregistered": [
      {
        "pointBrandId": 4,
        "pointBrandName": "해피포인트",
        "reason": "최근 파리바게트 결제가 잦은데 해피포인트가 미등록입니다. 등록 시 적립 가능."
      }
    ]
  },
  "message": null
}
```

> 구분: `usage`는 **금융포인트**(잔액 조회 O), `unregistered`는 **멤버십**(등록 여부 기반). 멤버십은 잔액 미연동이라 멤버십 사용처 추천은 없다.

> ⚠️합의필요: 포인트/멤버십 데이터(포인트사·잔액·등록 여부)와, 그 데이터+소비내역을 **분석해 추천을 생성**하는 로직의 담당 경계를 팀에서 확정 필요.

---

## 공통 에러 응답

| 상태 | code              | 상황                         | message 예시                           |
| ---- | ----------------- | ---------------------------- | -------------------------------------- |
| 400  | INVALID_PARAMETER | 필수값 누락/형식 오류        | `"expectedAmount는 필수입니다."`       |
| 401  | UNAUTHORIZED      | 인증 실패(토큰 없음/만료)    | `"인증이 필요합니다."`                 |
| 404  | NOT_FOUND         | 대상 없음(보유카드/소비내역) | `"해당 보유 카드를 찾을 수 없습니다."` |

```json
{ "success": false, "code": "UNAUTHORIZED", "data": null, "message": "인증이 필요합니다." }
```

> 에러 code 목록은 팀 공동 문서로 관리한다(각자 임의 추가 금지). 프론트는 message가 아닌 code로 분기한다.

---

## 미결 안건 (팀 합의 필요)

- **소비내역 `merchant_id` 병행**: 소비내역에 `merchant_id`(NULL 허용)를 `가맹점명`과 함께 두어야 함 — 엔진 혜택 매칭용, 소비내역 담당과 반영 확인.
- **포인트 담당 경계**: 포인트/멤버십 데이터 담당과 포인트 추천 로직 담당의 경계 확정.
- **전체 카드 현황(2번) 응답 범위**: `briefing`·`benefitsSummary` 구성이 홈 화면에 충분한지 프론트와 확인.
- **요청 필드 전반**: 프론트가 실제로 보낼 수 있는 형태인지 프론트와 확인.
- **인증**: JWT Bearer 방식이 팀 인증 설계와 일치하는지 인증 담당과 확인.
- **엔진 정산 인터페이스(4번)**: 결제·취소 처리(소비내역 도메인)가 호출할 엔진 내부 서비스의 메서드 시그니처, 취소 시 소비내역 상태 변경 주체(소비내역 도메인 vs 엔진), cancel의 호출 경로(내부 호출 vs REST) 협의.

## 보류 (연동 필요, V2)

- **멤버십 포인트 잔액 기반 기능**: 제휴 멤버십(CJ ONE 등)은 **잔액 조회 미연동** → 멤버십 잔액 안내·멤버십 사용처 추천은 제외. 멤버십은 **가입 권유 + 등록 시 적립 안내**까지만. (금융포인트는 잔액 연동되어 사용처 추천·pointGuide 가능.)
