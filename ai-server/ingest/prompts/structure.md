# 카드 약관 → 혜택 규칙 구조화 지침

카드 약관 원문을 읽고, 계산 엔진이 쓸 수 있는 형태로 옮긴다.

## 가장 중요한 규칙

**약관에 없는 값을 만들지 않는다.** 흔한 값이라서, 그럴 것 같아서 채우면 안 된다.
모르면 `null`로 두고 `_needs_review`에 필드명을 적는다. 빈칸은 나중에 사람이 채울 수 있지만,
그럴듯하게 채워진 틀린 값은 아무도 찾아내지 못한다.

**표현하지 못한 조항을 버리지 않는다.** 아래 스키마로 담기지 않는 내용은 반드시
`unmapped` 또는 `_schema_gap`에 원문 그대로 남긴다. 조용히 사라지면 스키마의 한계를 알 수 없다.

## 출력 형식

JSON 하나. 주석이나 설명 문장을 덧붙이지 말고 JSON만 출력한다.

```json
{
  "card": {
    "card_name": "약관에 적힌 카드명",
    "card_type": "CREDIT | CHECK",
    "annual_fee": 20000,
    "annual_fees": [
      { "brand": "LOCAL | VISA | MASTERCARD | AMEX | UNIONPAY | K_WORLD | ANY",
        "issue_type": "PLASTIC | MOBILE | ANY",
        "total_fee": 20000, "base_fee": 7000, "partner_fee": 13000, "_source": "..." }
    ],
    "_source": { "annual_fee": "근거가 된 약관 문장" },
    "_needs_review": []
  },
  "card_exclusions": [
    { "exclusion_type": "CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR",
      "exclusion_value": "INTEREST_FREE", "_source": "..." }
  ],
  "performance_tiers": [
    { "period_type": "MONTH", "min_performance_amount": 0, "shared_monthly_limit": 0, "_source": "..." },
    { "period_type": "MONTH", "min_performance_amount": 500000, "shared_monthly_limit": 7000, "_source": "..." }
  ],
  "performance_exclusions": [
    { "exclusion_type": "CATEGORY | PAYMENT_TYPE | TRANSACTION_ATTR | MIN_TXN_AMOUNT",
      "exclusion_value": "...", "_source": "..." }
  ],
  "benefits": [
    {
      "benefit_name": "...",
      "benefit_kind": "DISCOUNT",
      "calc_method": "RATE",
      "benefit_value": 10,
      "step_count": null,
      "apply_timing": "BILLED",
      "target_type": "CATEGORY",
      "target_category_code": "CAFE",
      "target_merchant_name": null,
      "require_performance": "Y",
      "performance_period": "MONTH",
      "require_payment_type": null,
      "min_txn_amount": null,
      "max_eligible_amount": null,
      "max_benefit_per_txn": null,
      "monthly_limit": 10000,
      "limit_group_code": null,
      "monthly_count_limit": null,
      "daily_count_limit": null,
      "daily_limit": null,
      "yearly_count_limit": null,
      "quarterly_count_limit": null,
      "quarterly_limit": null,
      "yearly_limit": null,
      "count_group_code": null,
      "use_shared_limit": "Y",
      "exclude_from_performance": "N",
      "option_group_code": null,
      "option_key": null,
      "description": "스키마로 못 담은 단서 문장",
      "tier_limits": [
        { "min_performance_amount": 500000, "monthly_limit": 7000,
          "quarterly_limit": null, "yearly_limit": null,
          "benefit_value": null, "_source": "..." }
      ],
      "exclusions": [
        { "exclusion_type": "CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR",
          "exclusion_value": "DELIVERY", "_source": "..." }
      ],
      "_source": { "benefit_value": "...", "monthly_limit": "..." },
      "_needs_review": ["apply_timing"]
    }
  ],
  "unmapped": [
    { "text": "약관 원문 그대로", "why": "왜 위 스키마로 표현할 수 없는지" }
  ],
  "_schema_gap": [
    { "text": "약관 원문 그대로", "missing": "필요해 보이는 컬럼이나 테이블",
      "why": "지금 스키마로 왜 안 되는지" }
  ]
}
```

`_source`에는 값을 넣은 필드만 담는다. `_needs_review`에는 약관에 근거가 없어 비워둔 필드명을 적는다.

## 필드 설명

### benefit_kind — 다섯 중 하나

| 값 | 뜻 |
|---|---|
| `DISCOUNT` | 할인 (즉시·청구·캐시백 모두) |
| `POINT` | 적립 (포인트·마일리지) |
| `SPECIAL_PRICE` | 특가·정액 제공가 |
| `GIFT` | 증정·무료 이용 (라운지, 쿠폰 등) |
| `INSTALLMENT_FREE` | 무이자할부 |
| `RETROACTIVE` | 사후정산 (많이 쓴 영역 추가적립 등) |

`GIFT`·`INSTALLMENT_FREE`·`RETROACTIVE`도 **행으로 만든다.** 계산에는 쓰지 않지만 카드 상세 화면에 표시한다.
단 결제와 무관한 혜택이라 `target_type`을 정할 수 없으면 `unmapped`로 보낸다.

**금액이 약관에 명시된 것은 `GIFT`가 아니다.** "넷플릭스 월 이용료 지원 17,000원"은
`DISCOUNT` + `calc_method: FIXED` + `benefit_value: 17000`이다.

### calc_method / benefit_value / step_count

- `RATE`: 퍼센트. `10` = 10%
- `FIXED`: 금액(원). `3000` = 3천원
- `COUNT_STEP`: **N회 이용할 때마다** 정액 지급. `step_count`에 N을 적고 `benefit_value`에 금액을 적는다

"5회 이용 시마다 3천 포인트"를 `FIXED`로 넣으면 결제할 때마다 지급되어 **실제의 5배**가 된다.
`step_count`는 `COUNT_STEP`일 때만 값을 넣고 나머지는 `null`이다.

### 기본 적립·할인에 추가분이 얹히는 구조

"전 가맹점 0.7% + 카페 0.5% 추가 적립"처럼 합산되는 약관이 적립형 카드에 흔하다.
엔진은 **한 결제에 혜택 하나**만 고르므로, 추가분(0.5%)만 적으면 기본(0.7%)에 밀려
실제보다 낮게 계산된다.

→ **추가 대상 행에는 합산값을 적는다.**

```
전 가맹점 0.7%   →  benefit_value 0.7,  target_type ALL
카페 0.5% 추가   →  benefit_value 1.2,  target_type CATEGORY(CAFE)   ← 합산값
```

`description`에 "기본 0.7% + 추가 0.5%"라고 원문 근거를 남긴다.

### apply_timing

`IMMEDIATE`(즉시할인) / `BILLED`(청구할인) / `CASHBACK`(결제계좌로 입금).
**`benefit_kind`가 `DISCOUNT`일 때만 값을 넣고, 나머지는 반드시 `null`이다.**
약관에 "결제일할인"으로 적혀 있으면 `BILLED`, "캐시백"이면 `CASHBACK`이다.

### target_type과 대상

- `CATEGORY`: `target_category_code`만 채운다
- `MERCHANT`: `target_merchant_name`만 채운다 (브랜드명 그대로)
- `ALL`: 둘 다 `null`

셋 중 하나만 채워야 한다. 카테고리 코드는 아래 표준에서 고른다. 맞는 게 없으면
`target_category_code`를 `null`로 두고 `_schema_gap`에 적는다. **비슷한 걸로 넣지 않는다.**

```
DINING(외식) > RESTAURANT(음식점) CAFE(카페) DELIVERY(배달앱) FAST_FOOD(패스트푸드)
SHOPPING(쇼핑) > CONVENIENCE_STORE(편의점) LARGE_MART(대형마트) DEPARTMENT_STORE(백화점)
                 ONLINE_SHOPPING(온라인쇼핑) BEAUTY(뷰티) BOOKSTORE(서점) SUPERMARKET(슈퍼마켓)
TRANSPORT(교통) > PUBLIC_TRANSPORT(대중교통) TAXI(택시) FUEL(주유)
                  PARKING_MAINTENANCE(주차정비) RAILWAY(철도) EXPRESS_BUS(고속시외버스)
LIVING(생활) > TELECOM(이동통신) UTILITY(공과금) APARTMENT_FEE(아파트관리비)
               INSURANCE(보험료) LIFE_SERVICE(세탁생활서비스) RENT(임대료)
               PERSONAL_CARE(개인관리서비스 — 미용실·피부관리·사진관·마사지)
CULTURE_LEISURE(문화여가) > MOVIE(영화) SUBSCRIPTION_STREAMING(구독스트리밍)
                            SPORTS_LEISURE(스포츠레저) GOLF(골프)
MEDICAL(의료) > HOSPITAL(병원) PHARMACY(약국) ANIMAL_HOSPITAL(동물병원)
EDUCATION(교육) > ACADEMY(학원) TUITION(학교납입금)
```

대분류를 넣으면 하위 중분류 결제까지 적용된다.

### 여러 대상을 겨냥한 혜택은 행을 쪼갠다

"OTT 10% — 넷플릭스/유튜브/웨이브" 처럼 브랜드가 나열되면 **브랜드마다 행을 따로** 만들고
같은 `limit_group_code`를 준다. 카테고리 하나로 뭉개면 안 된다 — 열거된 브랜드만 대상인데
카테고리 전체에 적용되어 버린다.

### limit_group_code — 묶음 한도

"통신·공과금·마트 각 10%, **합쳐서** 월 5천원"처럼 여러 혜택이 한도를 공유하면
같은 코드를 준다(예: `LIVING_5000`). 안 묶으면 한도가 혜택 수만큼 배로 새고,
에러 없이 금액만 틀린다. 한도를 공유하지 않으면 `null`.

**금액뿐 아니라 횟수도 이 코드로 묶인다.** "렌탈 5건 / 생활월납 3건"처럼 여러 대상이
건수를 나눠 쓰는 약관이면 같은 코드를 주고 `monthly_count_limit`에 그 건수를 적는다.

### count_group_code — 금액 묶음과 횟수 묶음이 다를 때

금액을 나눠 쓰는 범위와 횟수를 나눠 쓰는 범위가 다르면 이 코드를 따로 준다.

```
"택시·커피·영화관 합쳐 월 5천원"     → limit_group_code: PICK_DAILY_5000  (셋 다)
"영화관 3사 합쳐 연 4회"            → count_group_code: MOVIE_YEAR_4     (영화관만)
```

하나로 묶으면 좁은 쪽 한도가 대상 수만큼 배로 샌다(위 예에서 연 4회가 연 12회가 된다).
범위가 같으면 `null`로 두고 `limit_group_code`를 따른다.

### performance_period — 전월이냐 전분기냐

한 카드가 기간이 다른 실적 조건을 함께 쓰는 경우가 있다.
약관 예) 일상 영역은 전월 40만원, 특정 영역은 전분기 100만원.

이때 `performance_tiers`에 `period_type`이 다른 구간표를 각각 만들고,
각 혜택의 `performance_period`가 어느 쪽을 보는지 가리킨다. 기본값은 `MONTH`다.

### 한도의 기간 축 — 월·일이 전부가 아니다

같은 "한도"라도 기간이 다르면 다른 필드에 적는다. 환산하지 마라.

| 기간 | 금액 | 횟수 |
|---|---|---|
| 일 | `daily_limit` | `daily_count_limit` |
| 월 | `monthly_limit` | `monthly_count_limit` |
| 분기 | `quarterly_limit` | `quarterly_count_limit` |
| 연 | `yearly_limit` | `yearly_count_limit` |

"영화 연 12회"를 월 1회로 바꾸거나, "분기 2만 5천원"을 `monthly_limit`에 넣으면 안 된다.
분기 한도를 월 필드에 넣으면 매달 리셋되어 **실제의 세 배**가 나간다.

### exclude_from_performance — 이 혜택을 받은 거래를 실적에서 뺄 때

"○○ 할인을 받은 이용금액은 전월 실적에서 제외"처럼 **특정 혜택에만 걸리는 실적 제외**는
여기에 `Y`로 적는다. `performance_exclusions`에 `TRANSACTION_ATTR: DISCOUNTED`로 적으면
다른 혜택을 받은 거래까지 실적에서 빠져 실적이 실제보다 낮아진다.

카드 전체에 걸리는 제외(무이자할부 전체, 상품권 구매 전체 등)만 `performance_exclusions`에 적는다.

### option_group_code — 매월 하나를 고르는 혜택

"의료 20% 또는 생활 10% 중 택 1, 매월 변경 가능"처럼 회원이 골라야 하는 혜택이다.
같은 묶음에 속하는 혜택들에 **같은 코드**를 주면(예: `SELECT_SERVICE`) 그달에 선택된
하나만 적용된다. 선택과 무관하게 항상 적용되는 혜택은 `null`이다.

몇 개 중에 고르는지, 언제 바꿀 수 있는지는 `description`에 남긴다.

**선택지 하나가 혜택 여러 개로 이뤄지면 `option_key`로 묶는다.**
회원은 "팩"을 고르는 것이지 혜택 행을 고르는 게 아니다.

```
"위시 픽 3팩 중 택 1 — 배달팩 / 일상팩 / 관리팩"

배달팩의 혜택 4행   option_group_code: WISH_PICK,  option_key: DELIVERY
일상팩의 혜택 5행   option_group_code: WISH_PICK,  option_key: DAILY
관리팩의 혜택 6행   option_group_code: WISH_PICK,  option_key: CARE
```

선택지가 혜택 하나뿐이어도 `option_key`를 채운다.

### exclusions — 이 혜택만의 적용 예외

"외식 5% (단, 배달앱 제외)"처럼 혜택 대상 안에서 일부를 빼는 조건이다.
`exclusion_type`은 `CATEGORY` / `MERCHANT` / `PAYMENT_TYPE` / `TRANSACTION_ATTR` /
`MERCHANT_LOCATION` 중 하나다. 카드 전체가 아니라 **이 혜택에만** 걸리는 것을 적는다.

`MERCHANT_LOCATION`은 "백화점 입점 매장 제외"처럼 같은 브랜드 안에서 매장 위치로 가르는 조건이다.

```
IN_DEPARTMENT_STORE  백화점 입점       IN_LARGE_MART   대형마트·할인점 입점
IN_SHOPPING_MALL     쇼핑몰 임대매장    IN_TRANSIT_HUB  기차역·지하철역·공항 입점
```

이 조건은 지금 엔진이 판정하지 못한다(가맹점이 브랜드 단위라 지점을 구분하지 않는다).
그래도 적어둔다 — 나중에 가맹점명이 들어오면 판정 경로가 생긴다. `_schema_gap`에는 넣지 마라.

### card_exclusions — 카드 전체에 걸리는 적용 예외

약관의 "할인서비스 제외 대상" 절처럼 **모든 혜택에 공통으로** 걸리는 예외다.
카드 최상위의 `card_exclusions` 배열에 **한 번만** 적는다.

혜택마다 `exclusions`에 복사하지 마라. 같은 내용이 혜택 수만큼 늘어나고,
나중에 혜택을 추가할 때 복사를 빠뜨리면 그 혜택만 예외가 안 걸린다.

```
"할인서비스 제외 대상: 무이자할부, 상품권 구입…"   → card_exclusions (한 번)
"카페 5% (단, 백화점 입점 매장 제외)"              → 그 혜택의 exclusions
```

### 연회비 — 하나가 아니다

브랜드(국내전용/VISA/Mastercard)와 발급 형태(실물/모바일단독)에 따라 갈린다.
조합마다 `annual_fees`에 한 행씩 적는다.

- `total_fee`는 **항상** 채운다. 실제로 청구되는 금액이다.
- `base_fee`·`partner_fee`는 약관이 "20,000원(기본 7천 + 제휴 13천)"처럼 **나눠 적을 때만** 채운다.
  나누지 않은 약관에 임의로 배분하지 마라. 없는 값을 만드는 것이다. 그럴 땐 둘 다 `null`이다.
- 표의 금액 칸이 여러 브랜드에 걸쳐 병합돼 있어 브랜드를 구분하지 않으면 `brand: "ANY"`로 한 행만 적는다.
  브랜드마다 행을 복제하면 약관에 없는 구분을 만드는 셈이다.
- 표의 칸이 아예 비어 있으면 그 조합은 행을 만들지 않는다. 다른 행 값을 복사하지 마라.

`annual_fee`(단일 값)는 목록 화면용 대표값이다. 가장 흔한 조합의 `total_fee`를 넣고,
고를 근거가 없으면 `null`로 두고 `_needs_review`에 적는다.

### 상한 필드에서 null과 0은 다르다

`null`은 "제약 없음", `0`은 "혜택 없음"이다. 약관에 한도 언급이 없으면 `null`이다.
0으로 적으면 혜택이 통째로 사라진다.

### use_shared_limit

카드 전체 통합할인한도를 함께 소진하는지. 약관에 "통합 월 할인한도"가 있고 이 혜택이
거기 포함되면 `Y`.

### require_performance

전월실적 조건이 필요하면 `Y`. 충족 기준은 "판정된 구간의 최소실적금액 > 0"이다.

### performance_tiers

실적 구간표. **모든 카드는 0원 구간 행을 가진다** — 실적을 못 채웠을 때도 판정이 되어야 한다.
0원 구간의 `shared_monthly_limit`은 보통 0이다.
`shared_monthly_limit`은 그 구간의 카드 통합 할인한도다. 통합한도 개념이 없는 카드면 `null`.

### tier_limits — 구간별 개별 한도

"전월 30만원 이상 5천원, 60만원 이상 1만원"처럼 혜택 한도가 구간마다 다르면 여기에 적는다.
혜택을 구간 수만큼 쪼개지 않는다. 어떤 구간에 값이 없으면 그 구간은 위쪽 한도 값을 그대로 쓴다.

**한도의 기간 축은 여기서도 지킨다.** 분기 실적을 쓰는 혜택은 한도도 분기로 걸리는 경우가 많다.

```
"전분기 100만원 이상 분기 1만점, 200만원 이상 1만5천점"
  → { min_performance_amount: 1000000, quarterly_limit: 10000 }
     monthly_limit에 넣으면 매달 리셋되어 세 배가 나간다
```

### performance_exclusions — 실적 제외

| exclusion_type | exclusion_value |
|---|---|
| `CATEGORY` | 위 카테고리 코드 (`UTILITY`, `TUITION` 등) |
| `PAYMENT_TYPE` | 결제수단명 |
| `MIN_TXN_AMOUNT` | `1000` (이 금액 미만은 실적 제외) |
| `TRANSACTION_ATTR` | 아래 표준값 중에서 고른다 |

`TRANSACTION_ATTR` 표준값은 이것뿐이다. 새로 만들지 말고, 맞는 게 없으면 `_schema_gap`에 적는다.

```
INTEREST_FREE     무이자할부          DISCOUNTED      할인받은 거래
OVERSEAS          해외 이용분         CASH_ADVANCE    현금서비스
CARD_LOAN         카드론              GIFT_CARD       상품권·선불카드 구매/충전
TAX               국세·지방세         SOCIAL_INSURANCE 4대 사회보험료
FEE_INTEREST      수수료·이자·연체료   ANNUAL_FEE      연회비
GOV_SUBSIDY       정부지원금          POSTPAID_TRANSIT 후불교통요금
UNAPPROVED        무승인전표(자판기·통행료 등)
CANCELED          취소·부분취소 거래   LEVY            부담금·준조세(장애인 고용부담금 등)
POINT_USED        포인트로 결제한 금액  RECURRING       정기결제·자동이체 등록 건
INSTALLMENT_CONVERTED  일시불을 할부로 전환한 거래
CARD_SERVICE_FEE       카드사 부가서비스 이용료(문자알림 등)
```

### require_payment_type — 결제수단 조건도 표준값에서 고른다

같은 조건이 카드마다 다른 문자열이 되면 엔진이 매칭하지 못한다. 아래에서만 고른다.

```
AUTO_TRANSFER   자동이체·자동납부 등록 건   ("자동납부", "자동이체" 모두 이 값)
SIMPLE_PAY      간편결제 일반 (브랜드를 가리지 않을 때)
SAMSUNG_PAY  LG_PAY  KB_PAY  NAVER_PAY  KAKAO_PAY  PAYCO
SSG_PAY  L_PAY  SOL_PAY  COUPAY  SMILE_PAY
```

약관이 "삼성페이, LG페이, KB Pay"처럼 여러 개를 열거하면 **대상마다 행을 쪼개고**
`limit_group_code`로 한도를 묶는다. 한글 표기나 원문 그대로 쓰지 마라.
목록에 없는 결제수단이면 `_schema_gap`에 적는다.

**주의**: "이 혜택으로 할인받은 금액만 실적 제외"는 여기 적지 않는다.
`DISCOUNTED`로 적으면 다른 혜택을 받은 거래까지 실적에서 빠진다.
그런 경우는 그 혜택의 `exclude_from_performance`를 `Y`로 한다.

## 범위 밖 — unmapped로 보낼 것

아래는 지금 스키마가 의도적으로 담지 않는다. 발견하면 `unmapped`에 원문을 남긴다.

- 사후정산형(가장 많이 쓴 영역 추가적립)
- 상품 단위 조건(특정 메뉴·세트)
- 전전월 실적, 가족카드 합산, 월분할청구
- 생애 1회 한정(최초 1회, 신규 고객 한정)
- 해외 결제 혜택
- 요일·시간대·채널 조건
- 혜택 유효기간

## _schema_gap — 스키마를 고쳐야 할 것으로 보이는 것

"범위 밖"이 아닌데 표현이 안 되는 조항이다. 이미 알려진 것들은 아래와 같고,
발견하면 다시 적어도 된다. 다만 **원문을 그대로** 넣는다. 요약하지 않는다.

- 통합한도 초과 시 다른 혜택으로 대체되는 규칙
- 발급 초기 실적 면제 (발급일 정보가 없어 판정 불가)
- 한 카드에 통합한도가 둘 이상 (할인용·적립용이 따로인 경우 등)
- 단위당 계산 ("1,500원당 1마일", "리터당 60원")
- 원이 아닌 단위의 한도 (마일·포인트)
- 분기 단위 실적·한도
- 결제금액이 아니라 수수료에 붙는 할인
- 가맹점 안의 입점 위치 조건 ("백화점 내 매장 제외")

**이미 지원하는 것을 갭으로 적지 마라.** 아래는 위에 설명한 필드로 표현된다.

| 이런 조항 | 이 필드로 |
|---|---|
| 이 혜택만의 제외 대상 | `exclusions` 배열 |
| 이 혜택 받은 거래만 실적 제외 | `exclude_from_performance` |
| 매월 택 1 선택형 | `option_group_code` |
| 연 N회 한도 | `yearly_count_limit` |
| 여러 혜택이 건수를 공유 | `limit_group_code` + `monthly_count_limit` |
| 무이자할부 혜택 | `benefit_kind: INSTALLMENT_FREE` |
| 캐시백 | `benefit_kind: DISCOUNT` + `apply_timing: CASHBACK` |
| 카드 전체에 걸리는 제외 대상 | `card_exclusions` (최상위, 한 번만) |
| 브랜드·발급형태별 연회비 | `card.annual_fees` 배열 |
| 실적 구간마다 다른 혜택값 | `tier_limits[].benefit_value` |
| 취소 거래 / 부담금 / 포인트 사용분 | `TRANSACTION_ATTR`의 `CANCELED` `LEVY` `POINT_USED` |
| N회마다 정액 지급 | `calc_method: COUNT_STEP` + `step_count` |
| 전분기 실적 기준 혜택 | `performance_period: QUARTER` + `period_type: QUARTER` 구간표 |
| 분기·연 단위 금액 한도 | `quarterly_limit` / `yearly_limit` |
| 분기 단위 횟수 한도 | `quarterly_count_limit` |
| 금액 묶음과 횟수 묶음의 범위가 다름 | `count_group_code` |
| 선택지 하나가 혜택 여러 개 | `option_key` |
| 기본 적립 + 영역별 추가 적립 | 추가 대상 행에 **합산값** |
