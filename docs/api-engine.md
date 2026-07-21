# 카드 혜택 추천 엔진 API 명세

> 추천·계산 엔진 API. 상태: **초안(draft)** — 요청(입력) 필드는 프론트·카드 관리와 합의 필요. `⚠️합의필요` 표시 참고.

## 공통 규약

- **응답 봉투**: 성공과 실패의 필드 구성이 다르다.
  ```json
  // 성공
  { "success": true, "code": "SUCCESS", "data": { ... }, "message": null }
  ```
  ```json
  // 실패
  {
    "success": false,
    "code": "ACCESS_TOKEN_INVALID",
    "message": "인증이 필요합니다.",
    "errors": []
  }
  ```
  `data`는 성공 응답에만, `errors`는 실패 응답에만 존재한다.
  `errors`는 유효성 검증 오류가 여러 건일 때 담는 배열. 엔진 API는 대부분 빈 배열이지만 형태는 팀 규약에 맞춘다.
- **code**: 기계 판독용 결과 코드. 성공은 `SUCCESS`, 실패는 에러 코드(하단 공통 에러 응답 참고). **프론트 분기는 message(문구)가 아니라 code로 한다** — 문구는 자유롭게 바뀔 수 있다.
- **code 명명 규칙**: 공통 코드(`INPUT_INVALID`, `ACCESS_TOKEN_INVALID`, `ACCESS_TOKEN_EXPIRED`, `ACCESS_DENIED`, `NOT_FOUND`, `SERVER_INTERNAL_ERROR`)를 기본으로 사용한다.
  도메인 고유 코드는 공통 코드로 표현이 불가능하고 프론트가 별도 분기를 해야 할 때만 추가한다.
  (예: `ALREADY_CANCELED` — "이미 취소됨"은 공통 코드로 표현 불가하고 프론트 처리가 달라 정당한 추가)
  기능명 기반 코드(`XXX_READ_FAILED` 등)를 API마다 새로 만들지 않는다. code 목록은 팀 공동 문서로 관리한다.
- **인증**: 모든 엔드포인트는 `Authorization: Bearer <JWT>` 헤더 필수. 회원 id는 토큰에서 추출(요청 body에 안 넣음).
- **필드 표기**: DB는 snake_case, API JSON은 camelCase (MyBatis `mapUnderscoreToCamelCase`로 변환).
- **금액 단위**: 원(정수). 혜택/할인액 계산 시 원 미만은 **절사(버림)** — 표시 규칙이 아니라 계산·저장 값 기준(테스트 기댓값 포함).
  DB 컬럼은 `BIGINT`, DTO는 `Long`. 아래 표의 `int` 표기는 "정수"라는 뜻이며 자바 `int`가 아니다.
- **비율 표기**: 달성률·이용률 등 계산 비율(%)은 소수 첫째 자리까지, **반올림**.
- **소유권 검증**: 모든 리소스 접근은 토큰의 회원 id로 소유권을 검증한다. 타인 소유 리소스 요청은 403이 아니라 **404 NOT_FOUND**로 응답한다 (리소스 존재 여부 비노출).
- **화면 표기**: "AI 추천/AI 브리핑" 대신 **"추천 결과/추천 근거"**로 표기. 계산·판단은 엔진이 하고 LLM은 표현만 담당하므로, 금액·추천을 AI가 만든 것처럼 라벨링하지 않는다.
- **문서 규칙**: 해당 없는 섹션(Path Variables 등)은 생략한다. 각 API에는 고유 에러만 적고, 공통 에러(401 등)는 하단 공통 에러 응답 한 곳에서 관리한다.

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

### 📌 기능 설명

- **사용 목적**: 가맹점(또는 카테고리)과 결제 예상금액을 받아, 보유 카드별 예상 혜택을 계산해 이득이 큰 순서로 정렬해 반환한다. 동적 전환(A 소진→B), 미래 최적화 경고, 결제 직전 포인트 안내(금융포인트 잔액 + 등록 멤버십 적립)를 함께 담는다.
- **주의사항**:
  - 가맹점은 `merchantId`로 받는다(결정). 프론트가 merchant 목록에서 뿌리므로 선택 시점에 id를 확정할 수 있다.
  - `expectedAmount`는 5천원 단위 구간 대표값(중간값) 권장 — 이때 정률 혜택은 예상치(`isEstimate=true`)가 된다.
  - 응답은 보유 카드 **전부**를 담는다(표시 개수는 프론트가 자름). `expectedBenefit` 동점 시 `userCardId` 오름차순 정렬 (테스트 재현성).

**입력 조합별 계산 범위** (입력이 구체적일수록 추천이 정밀해진다):

| 입력              | 계산 범위                                                                                               |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| merchantId 있음   | 가맹점 직접 혜택 + 해당 카테고리 혜택 + 전체(ALL) 혜택                                                  |
| categoryId만 있음 | 카테고리 혜택 + 전체(ALL) 혜택                                                                          |
| 둘 다 없음        | 전체(ALL) 혜택 + 실적 진행 상황 기반 추천 — 장소 미정 상태에서 "지금 어느 카드를 쓰는 게 유리한지" 안내 |

### 🔽 엔드포인트

```
POST /api/recommendations
```

### 📌 Request

#### ✔ Headers

```bash
Authorization: Bearer <JWT>
Content-Type: application/json
```

#### ✔ Request Body

```json
{ "merchantId": 205, "expectedAmount": 11900, "paymentType": "CARD" }
```

#### ✔ Request 필드 설명

| 필드명         | 타입 | 필수 | 설명                                               |
| -------------- | ---- | ---- | -------------------------------------------------- |
| merchantId     | int  | N    | 가맹점 id. 있으면 가맹점 직접 혜택까지 계산        |
| categoryId     | int  | N    | 카테고리 id (merchantId 없을 때 폴백)              |
| expectedAmount | int  | Y    | 결제 예상금액. 5천원 단위 구간 대표값(중간값) 권장 |
| paymentType    | string | N | 결제수단(CARD, SIMPLE_PAY 등). `require_payment_type`이 걸린 혜택 판정용 |

- `paymentType`은 선택값이다. `require_payment_type`이 걸린 혜택(예: 간편결제 전용)을 판정하는 데 쓴다.
  **미입력이면 결제수단 조건이 걸린 혜택을 제외**하고 계산한다. 받을 수 있을지 확실하지 않은 혜택을
  추천에 넣어 실제보다 큰 금액을 보여주면 안 되기 때문이다. 결제 확정 시에는 실제 수단으로 다시 계산한다.

### 📌 Response

#### ✔ 성공 응답

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
      "pointProviderName": "마이신한포인트",
      "usablePoint": 97842,
      "message": "마이신한포인트 97,842P 보유 중. 이 결제에 사용할 수 있습니다."
    },
    "membershipEarn": [
      {
        "pointProviderName": "CJ ONE",
        "message": "CJ ONE 멤버십이 등록되어 있어요. 올리브영 결제 시 적립됩니다. 결제 후 적립 여부를 확인해 보세요."
      }
    ]
  },
  "message": null
}
```

#### ✔ Response 필드 설명

| 필드명                                  | 타입         | 설명                                                                                                                      |
| --------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------- |
| recommendations[].rank                  | int          | 추천 순위(1이 최적)                                                                                                       |
| recommendations[].userCardId            | int          | 보유 카드 id                                                                                                              |
| recommendations[].cardName              | string       | 카드명                                                                                                                    |
| recommendations[].expectedBenefit       | int          | 예상 혜택액(원). **카드당 혜택 1개만 적용한 값**(합산 아님 — 매칭된 혜택 중 최댓값, 동점이면 benefit_id 오름차순). DISCOUNT/POINT=계산값(원 미만 절사), SPECIAL_PRICE=혜택값(정가−특가) 데이터 사용, GIFT=0(금액 비교 제외) |
| recommendations[].isEstimate            | bool         | true=예상(정률+구간), false=확정(정액/상한도달)                                                                           |
| recommendations[].benefitKind           | string       | DISCOUNT(할인) / POINT(적립) / SPECIAL_PRICE(특가) / GIFT(증정) / RETROACTIVE(사후정산, 추천 계산 제외)                    |
| recommendations[].reason                | string       | 추천 근거(엔진 생성)                                                                                                      |
| recommendations[].dynamicSwitch         | bool         | 동적 전환이면 true — 한도·횟수 제한이 없다고 가정한 1위와 실제 1위가 다를 때, 실제 1위 카드에 표시                        |
| futureOptimization                      | object\|null | 실적 미달 경고. 없으면 null                                                                                               |
| futureOptimization.userCardId           | int          | 대상 보유 카드 id                                                                                                         |
| futureOptimization.cardName             | string       | 카드명                                                                                                                    |
| futureOptimization.remainingPerformance | int          | 실적까지 남은 금액                                                                                                        |
| futureOptimization.message              | string       | 경고 문구                                                                                                                 |
| pointGuide                              | object\|null | 결제 직전 **금융포인트 잔액** 안내. 없으면 null. 여러 브랜드 보유 시 **잔액 최대** 1개 선택 (포인트 소멸은 범위 밖)      |
| pointGuide.pointProviderName               | string       | 금융포인트사명 (예: 마이신한포인트)                                                                                       |
| pointGuide.usablePoint                  | int          | 보유 잔액(이 결제에 사용 가능)                                                                                            |
| pointGuide.message                      | string       | 안내 문구                                                                                                                 |
| membershipEarn[]                        | array        | 결제 가맹점에서 적립되는 **등록 멤버십** 안내. **merchantId가 있을 때만** 산출, 없으면(카테고리만/장소 미정) 항상 []      |
| membershipEarn[].pointProviderName         | string       | 멤버십명 (예: CJ ONE)                                                                                                     |
| membershipEarn[].message                | string       | 적립 안내 문구                                                                                                            |

> 포인트 안내 구분: `pointGuide`는 **금융포인트**(카드사, 잔액 조회 가능) 기준. **멤버십**(CJ ONE 등)은 잔액 미연동 → 잔액 대신 `membershipEarn`(등록 시 적립 가능 안내)만.

> GIFT/RETROACTIVE 한계: GIFT(사은품)는 금전 가치 환산이 불가해 **금액 비교(rank)에서 제외**하고 reason으로만 안내한다 — 실제 가치는 금액 혜택보다 클 수 있으나 시연 범위의 트레이드오프로 수용. RETROACTIVE(사후정산)는 추천 계산에서 제외, 정보 표시만.

### 📌 이 API 고유 에러

| 에러코드          | 설명                                                          | HTTP Status |
| ----------------- | ------------------------------------------------------------- | ----------- |
| INPUT_INVALID | expectedAmount 누락                                           | 400         |
| NOT_FOUND         | 존재하지 않는 merchantId/categoryId (무시하지 않고 명시 실패) | 404         |

---

## 2. 전체 보유 카드 현황 (대시보드 홈·보유카드 목록)

### 📌 기능 설명

- **사용 목적**: 대시보드 홈, 보유 카드 목록 화면용. 로그인 사용자의 **보유 카드 전부**를 한 번에 반환한다. 홈 상단 브리핑(`briefing`)과 카드별 실적 요약·남은 혜택(`benefitsSummary`)을 담는다.
- **주의사항**:
  - 카드가 여러 장이라도 한 번의 호출로 처리한다 (개별 호출 반복 금지).
  - 보유 카드가 0장이면 에러가 아니라 `cards: []`, `briefing: null`을 반환한다 (정상 상태).
  - 3번(상세)과의 분리 근거: 목록에서 카드마다 전체 혜택 상세를 반복 전송하면 응답이 커지므로, 목록은 요약(`benefitsSummary`)만, 혜택 상세(`benefits`, 이용률 포함)는 3번 개별 호출로 나눈다.
  - `briefing`은 실적 달성이 가장 임박한 카드 안내 — 판단·문구 모두 엔진 생성 (화면 표기는 "추천", "AI 브리핑" 라벨 지양).

### 🔽 엔드포인트

```
GET /api/cards/monthly-status
```

### 📌 Request

#### ✔ Headers

```bash
Authorization: Bearer <JWT>
```

#### ✔ Query Parameters

| 파라미터  | 타입   | 필수 | 설명                                |
| --------- | ------ | ---- | ----------------------------------- |
| yearMonth | string | N    | 기준 연월(YYYY-MM). 생략 시 이번 달 |

### 📌 Response

#### ✔ 성공 응답

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
        "currentPerformanceAmount": 450000,
        "targetPerformance": 500000,
        "remainingPerformance": 50000,
        "achievementRate": 90.0,
        "performanceMet": true,
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

#### ✔ Response 필드 설명

| 필드명                                   | 타입         | 설명                                                                                                                                      |
| ---------------------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| briefing                                 | object\|null | 홈 상단 브리핑 — 실적 달성이 가장 임박한 카드 안내. 판단·문구 모두 엔진 생성. 실적 조건 없는 카드(target=0)는 후보 제외. 대상 없으면 null |
| briefing.userCardId                      | int          | 가장 임박한 보유 카드 id                                                                                                                  |
| briefing.cardName                        | string       | 카드명                                                                                                                                    |
| briefing.achievementRate                 | float        | 해당 카드 달성률(%)                                                                                                                       |
| briefing.remainingPerformance            | int          | 남은 실적 금액                                                                                                                            |
| briefing.message                         | string       | 브리핑 문구 (엔진 템플릿 생성)                                                                                                            |
| cards[].userCardId                       | int          | 보유 카드 id                                                                                                                              |
| cards[].cardName                         | string       | 카드명                                                                                                                                    |
| cards[].yearMonth                        | string       | 기준 연월                                                                                                                                 |
| cards[].currentPerformanceAmount             | int          | 이번 달 누적 실적 인정액                                                                                                                  |
| cards[].targetPerformance                | int          | 실적 목표 금액 — 당월 누적으로 아직 도달하지 못한 가장 낮은 구간의 최소실적금액. 전 구간 도달 시 최고 구간 금액(달성률 ≥100%)             |
| cards[].remainingPerformance             | int          | 남은 실적 금액(target − current). 초과 달성 시 음수가 아니라 **0으로 클램프**                                                             |
| cards[].achievementRate                  | float\|null  | 실적 달성률(%), 계산값. 실적 조건 없는 카드(target=0)는 **null** — 화면은 "실적 조건 없음" 표기                                           |
| cards[].performanceMet                   | bool         | 현재 실적 충족 여부 — 전월실적으로 판정된 구간의 `min_performance_amount > 0`이면 true. false면 `require_performance=Y` 혜택은 이번 달 적용되지 않는다 |
| cards[].sharedLimit                      | int\|null     | 현재 구간의 월 통합할인한도. **null=통합한도 없는 카드**(혜택별 개별한도만 적용), 0=혜택 없음                                             |
| cards[].sharedLimitUsed                  | int          | 통합한도 소진액                                                                                                                           |
| cards[].benefitsSummary[]                | array        | 홈 위젯 "남은 혜택" 표시용 — 잔여 한도가 남은 혜택 요약. 상세는 3번                                                                       |
| cards[].benefitsSummary[].benefitId      | int          | 혜택 id                                                                                                                                   |
| cards[].benefitsSummary[].benefitName    | string       | 혜택명 (예: 교통 10% 할인)                                                                                                                |
| cards[].benefitsSummary[].remainingLimit | int\|null    | 잔여 한도(한도 없으면 null). **묶음 한도 소속이면 그룹 기준 잔여액** — 3번의 `limitGroupCode` 참조                                        |

---

## 3. 개별 카드 상세 현황

### 📌 기능 설명

- **사용 목적**: 카드 상세 화면용. 특정 보유 카드 한 장의 실적 현황과 **혜택별 이용 현황(잔여 한도 포함)**까지 상세 반환한다.
- **주의사항**: 달성률·이용률·잔여 한도는 저장값이 아니라 계산값이다 (원본만 저장, 조회 시 계산).

### 🔽 엔드포인트

```
GET /api/cards/{userCardId}/monthly-status
```

### 📌 Request

#### ✔ Headers

```bash
Authorization: Bearer <JWT>
```

#### ✔ Path Variables

| 변수명     | 타입 | 설명         |
| ---------- | ---- | ------------ |
| userCardId | int  | 보유 카드 id |

#### ✔ Query Parameters

| 파라미터  | 타입   | 필수 | 설명                                |
| --------- | ------ | ---- | ----------------------------------- |
| yearMonth | string | N    | 기준 연월(YYYY-MM). 생략 시 이번 달 |

### 📌 Response

#### ✔ 성공 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "userCardId": 12,
    "cardName": "삼성 ID ON",
    "yearMonth": "2026-07",
    "prevPerformanceAmount": 520000,
    "targetPerformance": 500000,
    "currentPerformanceAmount": 400000,
    "remainingPerformance": 100000,
    "achievementRate": 80.0,
    "performanceMet": true,
    "sharedLimit": 20000,
    "sharedLimitUsed": 8000,
    "benefits": [
      {
        "benefitId": 55,
        "benefitName": "편의점/약국 All Day 10% 할인",
        "limitGroupCode": null,
        "usedAmount": 8000,
        "monthlyLimit": 10000,
        "remainingLimit": 2000,
        "usageRate": 80.0
      },
      {
        "benefitId": 61,
        "benefitName": "통신요금 10% 할인",
        "limitGroupCode": "LIVING3",
        "usedAmount": 3000,
        "monthlyLimit": 5000,
        "remainingLimit": 2000,
        "usageRate": 60.0
      },
      {
        "benefitId": 62,
        "benefitName": "공과금 10% 할인",
        "limitGroupCode": "LIVING3",
        "usedAmount": 3000,
        "monthlyLimit": 5000,
        "remainingLimit": 2000,
        "usageRate": 60.0
      }
    ]
  },
  "message": null
}
```

#### ✔ Response 필드 설명 — `CardMonthlyStatus`

| 필드명                    | 타입        | 설명                                                                                                                          |
| ------------------------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------- |
| userCardId                | int         | 보유 카드 id                                                                                                                  |
| cardName                  | string      | 카드명                                                                                                                        |
| yearMonth                 | string      | 기준 연월                                                                                                                     |
| prevPerformanceAmount      | int         | 전월 실적(현재 구간·한도 판정 기준, 엔진 계산)                                                                                |
| targetPerformance         | int         | 실적 목표 금액 — 당월 누적으로 아직 도달하지 못한 가장 낮은 구간의 최소실적금액. 전 구간 도달 시 최고 구간 금액(달성률 ≥100%) |
| currentPerformanceAmount      | int         | 이번 달 누적 실적 인정액                                                                                                      |
| remainingPerformance      | int         | 남은 실적 금액(target − current). 초과 달성 시 음수가 아니라 **0으로 클램프**                                                 |
| achievementRate           | float\|null | 실적 달성률(%), 계산값. 실적 조건 없는 카드(target=0)는 **null** — 화면은 "실적 조건 없음" 표기                               |
| performanceMet            | bool        | 현재 실적 충족 여부 — 전월실적으로 판정된 구간의 `min_performance_amount > 0`이면 true. false면 `require_performance=Y` 혜택은 이번 달 적용되지 않는다 |
| sharedLimit               | int\|null    | 현재 구간의 월 통합할인한도. **null=통합한도 없는 카드**(혜택별 개별한도만 적용), 0=혜택 없음                                 |
| sharedLimitUsed           | int         | 통합한도 소진액                                                                                                               |
| benefits[].benefitId      | int         | 혜택 id                                                                                                                       |
| benefits[].benefitName    | string      | 혜택명                                                                                                                        |
| benefits[].usedAmount      | int         | 이번 달 누적 혜택액. 묶음 소속이면 **그룹 전체 누적액**                                                                        |
| benefits[].limitGroupCode | string\|null | 묶음 한도 코드. **같은 코드를 가진 혜택들은 한도를 공유한다** — 아래 세 값이 전부 그룹 기준으로 내려간다. null이면 이 혜택 단독 |
| benefits[].monthlyLimit   | int\|null   | 혜택 월 한도(없으면 null). 묶음 소속이면 **그룹 공유 한도**. 실적구간별 한도가 있으면(`benefit_tier_limit`) **판정된 구간의 한도**를 내려준다 |
| benefits[].remainingLimit | int\|null   | 잔여 한도(monthlyLimit − usedAmount, 한도 없으면 null). 묶음 소속이면 **그룹 기준 잔여액** — 묶인 혜택들이 같은 값을 갖는다     |
| benefits[].usageRate      | float\|null | 혜택 이용률(%), 계산값. 한도 없으면(monthlyLimit=null) **null**. 묶음 소속이면 그룹 기준 이용률                                |

> **묶음 한도 표시 주의** — `limitGroupCode`가 같은 혜택들은 한도를 공유하므로 `monthlyLimit`·`usedAmount`·`remainingLimit`·`usageRate`가 모두 같은 값으로 내려간다.
> 화면에서 혜택마다 따로 더하면 한도가 실제보다 몇 배로 보인다(위 예시: 5,000원 지갑 하나인데 10,000원으로 보임).
> 프론트는 같은 코드끼리 묶어 한 줄로 표시하거나, 그룹 잔여액을 한 번만 노출해야 한다.

### 📌 이 API 고유 에러

| 에러코드  | 설명                                                                                       | HTTP Status |
| --------- | ------------------------------------------------------------------------------------------ | ----------- |
| NOT_FOUND | 해당 보유 카드를 찾을 수 없음 — **타인 소유 카드 포함** (소유권 불일치도 404, 존재 비노출) | 404         |

---

## 4. 결제·취소 시 상태 갱신

### 📌 기능 설명

- **사용 목적**: 결제·취소 발생 시 카드의 실적·혜택 소진 상태를 갱신해 **다음 추천에 반영**되게 한다.
- **주의사항**:
  - 결제(가산)와 취소(차감)의 처리 경로가 다르다 — 아래 4-A / 4-B 참고.
  - 놓친 혜택(최적 대비 차액)은 계산·저장하지 않는다. (범위 밖)

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

### 4-B. 결제 취소 → 상태 차감

결제 취소는 앱 기능이 아니라 외부(카드사/가맹점)에서 발생해 데이터로 유입되는 사실이다. 마이데이터 동기화·Mock 취소로 취소 건이 유입될 때 호출한다. 4-A(가산)의 짝.

**취소 처리 원칙** (실제 카드사 동작 기준):

| 원칙                       | 내용                                                                                                                                                            |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 소급 재계산 금지           | 취소돼도 **다른 거래**의 applied_benefit_id·discount_amount는 불변. 이미 승인된 거래에 할인이 뒤늦게 붙지 않는다(실제 카드사 동작과 동일). 과거 기록은 불변     |
| 취소 건 기여분만 역산 차감 | 당월누적실적 −, 통합한도사용 −, 혜택별 사용액 −, 적용횟수 − (월 전체 재계산 아님)                                                                               |
| 복원 정책: **복원함**      | 실적·통합한도·개별한도·횟수 모두 차감(복원). 상시 혜택은 취소 시 사용액 차감이 카드사 공식 기준(BC 부가서비스 기준). 미복원 사례는 프로모션 특약 영역 → 범위 밖 |
| 복원분은 이후부터          | 복원된 여유는 **이후 추천부터** 반영 (소급 없음)                                                                                                                |
| 전액 취소만                | 부분 취소는 범위 밖                                                                                                                                             |
| 당월 취소만                | 전월 거래 취소는 범위 밖 — 전월실적 변경 시 실적구간·통합한도 전체 재판정이 필요하므로 별도 판단 전까지 미처리                                                  |

> 각주: `최종적용일시`는 이력이 없어 되돌리지 않는다 — 취소 당일의 일1회 판정만 보수적으로 동작(허용).
> 표기: 청구할인은 월말 확정이므로 월중 값은 전부 예상치다. 취소로 숫자가 바뀌는 게 정상이며, 화면은 `isEstimate`로 예상 표기한다.

### 🔽 엔드포인트

```
POST /api/settlements/cancel
```

### 📌 Request

#### ✔ Headers

```bash
Authorization: Bearer <JWT>
Content-Type: application/json
```

#### ✔ Request Body

```json
{ "expenseId": 3001 }
```

#### ✔ Request 필드 설명

| 필드명        | 타입 | 필수 | 설명               |
| ------------- | ---- | ---- | ------------------ |
| expenseId | int  | Y    | 취소된 소비내역 id |

#### ✔ 처리

1. 소비내역 결제상태 → CANCELED 반영 (상태 변경 주체는 소비내역 담당과 협의 — 미결 안건)
2. 취소 처리 원칙대로 해당 건의 기여분을 상태에서 역산 차감

### 📌 Response

#### ✔ 성공 응답

차감 반영된 카드 현황 — 3번 `CardMonthlyStatus`와 **필드까지 완전히 동일** (프론트가 같은 파서 사용).
아래는 3번 예시 상태에서 13,900원 결제(할인 1,390원)가 취소된 뒤의 응답.

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "userCardId": 12,
    "cardName": "삼성 ID ON",
    "yearMonth": "2026-07",
    "prevPerformanceAmount": 520000,
    "targetPerformance": 500000,
    "currentPerformanceAmount": 386100,
    "remainingPerformance": 113900,
    "achievementRate": 77.2,
    "performanceMet": true,
    "sharedLimit": 20000,
    "sharedLimitUsed": 6610,
    "benefits": [
      {
        "benefitId": 55,
        "benefitName": "편의점/약국 All Day 10% 할인",
        "limitGroupCode": null,
        "usedAmount": 6610,
        "monthlyLimit": 10000,
        "remainingLimit": 3390,
        "usageRate": 66.1
      }
    ]
  },
  "message": null
}
```

### 📌 이 API 고유 에러

| 에러코드         | 설명                                                                                 | HTTP Status |
| ---------------- | ------------------------------------------------------------------------------------ | ----------- |
| NOT_FOUND        | 해당 소비내역을 찾을 수 없음 — **타인 소유 포함** (소유권 불일치도 404, 존재 비노출) | 404         |
| ALREADY_CANCELED | 이미 취소된 소비내역                                                                 | 409         |

---

## 5. 포인트 추천 (금융포인트 사용처 + 미등록 멤버십 가입)

### 📌 기능 설명

- **사용 목적**: 소비내역 + 포인트/멤버십 정보를 분석해 두 가지를 추천한다. 포인트 잔액 단순 조회가 아니라 **소비 패턴 해석 → 추천 도출**이다.
  1. **금융포인트 사용처 추천**(`usage`): 보유 금융포인트(잔액 조회 가능)를 소비 이력 기반으로 어디서 쓸지.
  2. **미등록 멤버십 가입 권유**(`unregistered`): 소비는 많은데 미등록인 멤버십사 (예: 파리바게트 소비 많은데 해피포인트 미등록 → 등록 권유).
- **주의사항**:
  - `usage`는 **금융포인트**(잔액 조회 O), `unregistered`는 **멤버십**(등록 여부 기반). 멤버십은 잔액 미연동이라 멤버십 사용처 추천은 없다.
  - 구현 우선순위: 엔진 핵심(추천 #1 → 정산 #4 → 현황 #2·#3) 구현 이후 **후순위**로 진행한다.

### 🔽 엔드포인트

```
GET /api/points/recommendations
```

### 📌 Request

#### ✔ Headers

```bash
Authorization: Bearer <JWT>
```

(그 외 파라미터·body 없음)

### 📌 Response

#### ✔ 성공 응답

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "usage": [
      {
        "pointProviderId": 1,
        "pointProviderName": "마이신한포인트",
        "usablePoint": 97842,
        "suggestedMerchant": "올리브영",
        "reason": "최근 올리브영 방문 이력이 있어 사용을 추천합니다."
      }
    ],
    "unregistered": [
      {
        "pointProviderId": 4,
        "pointProviderName": "해피포인트",
        "reason": "최근 파리바게트 결제가 잦은데 해피포인트가 미등록입니다. 등록 시 적립 가능."
      }
    ]
  },
  "message": null
}
```

#### ✔ Response 필드 설명

| 필드명                        | 타입   | 설명                       |
| ----------------------------- | ------ | -------------------------- |
| usage[].pointProviderId          | int    | 보유 금융포인트사 id       |
| usage[].pointProviderName        | string | 보유 금융포인트사명        |
| usage[].usablePoint           | int    | 사용 가능 포인트(잔액)     |
| usage[].suggestedMerchant     | string | 추천 사용처                |
| usage[].reason                | string | 추천 근거 (소비 이력 기반) |
| unregistered[].pointProviderId   | int    | 미등록 멤버십사 id         |
| unregistered[].pointProviderName | string | 멤버십사명                 |
| unregistered[].reason         | string | 추천 근거 (소비 분석 결과) |

> ⚠️합의필요: 포인트/멤버십 데이터(포인트사·잔액·등록 여부)와, 그 데이터+소비내역을 **분석해 추천을 생성**하는 로직의 담당 경계를 팀에서 확정 필요.

---

## 공통 에러 응답

| 상태 | code              | 상황                         | message 예시                           |
| ---- | ----------------- | ---------------------------- | -------------------------------------- |
| 400  | INPUT_INVALID | 필수값 누락/형식 오류        | `"expectedAmount는 필수입니다."`       |
| 401  | ACCESS_TOKEN_INVALID / ACCESS_TOKEN_EXPIRED | 인증 실패(토큰 없음·만료) | `"인증이 필요합니다."` |
| 404  | NOT_FOUND         | 대상 없음(보유카드/소비내역) | `"해당 보유 카드를 찾을 수 없습니다."` |

```json
{
  "success": false,
  "code": "ACCESS_TOKEN_INVALID",
  "message": "인증이 필요합니다.",
  "errors": []
}
```

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
