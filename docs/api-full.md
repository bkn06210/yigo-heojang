# API 명세서

## 변경 내역 (DB 스키마 대조 후 수정)

`schema.sql`과 대조해 **스키마로 구현할 수 없거나 값이 틀리게 나오는 부분**을 고쳤습니다.
담당 파트를 임의로 바꾼 것이 아니라, 그대로 두면 동작하지 않는 것들입니다.
이견 있으면 알려주세요.

### 소비·결제 파트

| 위치           | 변경                                                                       | 이유                                                                                                             |
| -------------- | -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 결제 처리      | `recommendationId` → `isRecommendBased`                                    | 추천 결과를 저장하지 않기로 해서 참조할 추천번호가 없다                                                          |
| 결제 처리      | 요청에 `merchantId` 추가                                                   | 엔진은 가맹점 혜택을 id로 찾는다. 이름 문자열은 표기 차이로 매칭이 깨진다                                        |
| 결제 처리      | 요청에 `paymentType`·`interestFreeYn` 추가                                 | 카드 약관은 특정 결제수단·무이자할부를 전월실적에서 제외한다. 없으면 실적이 조용히 과다 계산된다                 |
| 결제 처리      | Business Rules에 엔진 호출 2건 명시                                        | 혜택 계산(적용혜택·할인액 저장)과 상태 갱신(실적·한도 가산). **이 갱신이 없으면 결제해도 다음 추천이 안 바뀐다** |
| 결제 처리·조회 | 경로 `/api/mock-payments` → `/api/payments`, `mockPaymentId` → `paymentId` | 이름에 구현 방식(mock)이 박히면 실서비스 전환 시 경로까지 바뀐다. Mock 여부는 `paymentChannel` 값으로 구분       |
| 소비내역       | `inputType` 값 `MOCK_PAYMENT` → `PAYMENT`                                  | 위와 같은 이유                                                                                                   |
| 소비내역 조회  | `merchantId`, `appliedBenefitId`, `appliedBenefitName` 노출                | 거래별로 어떤 혜택을 받았는지 보여주려면 필요하다                                                                |
| 소비내역       | 직접등록·수정·삭제 3개 삭제                                                | 협의 결과. 소비내역은 결제와 마이데이터 동기화로만 생긴다                                                        |
| 소비내역       | **마이데이터 거래 동기화** 신규 추가                                       | 취소는 사용자가 지우는 게 아니라 동기화로 감지되는 사실이다. 이 엔드포인트가 없으면 취소 반영 경로가 없다        |
| 포인트         | 소멸 관련 필드 제거 (`expiringPoint`, `expiredAt`, `expiringSoon`)         | 포인트 소멸은 범위 밖으로 정했고, 스키마에 만료일 컬럼이 없어 값을 만들 수 없다                                  |
| 포인트         | `pointType` 값 `SAVE, USE`로 한정                                          | 스키마의 값 집합과 맞춤                                                                                          |
| 포인트         | `pointBrandId/Name` → `pointProviderId/Name`                               | 테이블이 `point_provider`다                                                                                      |

### 회원·카드 파트

| 위치             | 변경                                                                    | 이유                                                                                                         |
| ---------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| 보유 카드 상세   | 실적·혜택 필드명을 엔진 현황 API와 통일                                 | 같은 값을 두 이름으로 내려주면 프론트가 파서를 두 벌 갖는다                                                  |
| 보유 카드 상세   | "계산 결과를 조회한다" → "엔진 현황 서비스를 호출해 받은 값을 내려준다" | 조회할 저장된 결과가 없다(파생값은 저장하지 않는다). 실적 계산이 두 벌이 되면 화면마다 숫자가 갈린다         |
| 보유 카드 상세   | `calculatedAt`, `available` 삭제                                        | 저장 컬럼이 없고, `available`은 산출 규칙이 정의된 적이 없다                                                 |
| 보유 카드 목록   | `performanceAchieved` → `performanceMet`                                | 위와 같은 이유                                                                                               |
| 카드별 혜택 상세 | `applicableMerchants` 배열 삭제                                         | 혜택 1건은 대상이 하나다. 약관이 여러 대상을 열거하면 대상마다 혜택 행이 따로 있고 `limitGroupCode`로 묶인다 |
| 카드별 혜택 상세 | `requiredPerformanceAmount` → `requirePerformance`(Y/N)                 | 실적 요구 금액은 혜택이 아니라 **카드 단위**(실적구간)라 혜택별로 다를 수 없다                               |
| 카드별 혜택 상세 | `benefitType`/`calculationType` → `benefitKind`/`calcMethod`            | 컬럼명과 맞춤                                                                                                |
| 약관 목록        | 응답 봉투 `result` → `data`                                             | 나머지 45개와 팀 규약이 `data`다                                                                             |
| 대표카드 설정    | 경로에서 `/v1/` 제거                                                    | 이 엔드포인트만 붙어 있었다                                                                                  |
| 알림 단건 삭제   | `/api/notification/{id}` → `/api/notifications/{id}`                    | 나머지 알림 API는 복수형이다                                                                                 |
| 멤버십 해제      | `{membershipId}` → `{membershipRegisterId}`                             | 같은 리소스인데 상세 조회와 이름이 달랐다                                                                    |
| 전체             | 에러 코드 오타 8개                                                      | `CATEGORY_NOT_GOUND`, `uSER_CARD_ACCESS_DENIED` 등. 코드 문자열이 정확해야 프론트가 분기한다                 |
| 전체             | JSON 문법 오류·코드블록 누락                                            | 예시가 한 문단으로 뭉개지거나 파싱이 안 되던 것                                                              |

### 아직 정하지 못한 것

| 항목             | 내용                                                                                                                         |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| BIN 검증         | 보유 카드 등록의 `CARD_BIN_MISMATCH` 규칙 — `card` 테이블에 BIN 컬럼이 없어 구현 불가. 컬럼을 추가할지 규칙을 뺄지 협의 필요 |
| 가맹점 목록 API  | 추천 API가 `merchantId`를 받는데 가맹점 목록을 주는 엔드포인트가 없다                                                        |
| 멤버십 적립처    | `point_usage_place`에 `merchant_id`가 없어 "이 가맹점에서 적립되는 멤버십"을 판정할 수 없다                                  |
| 소유권 위반 응답 | 엔진은 404(리소스 존재 비노출), 다른 파트는 403. 하나로 정해야 한다                                                          |

---

## 엔드포인트 목록

| #   | 그룹             | 메서드   | 경로                                         | 기능명                                  | 담당    | 상태코드       |
| --- | ---------------- | -------- | -------------------------------------------- | --------------------------------------- | ------- | -------------- |
| 1   | Auth             | `GET`    | `/api/terms`                                 | 약관 목록 조회                          | 재혁 이 | 200 OK         |
| 2   | Auth             | `POST`   | `/api/auth/signup`                           | 회원가입                                | 재혁 이 | 201 Created    |
| 3   | Auth             | `POST`   | `/api/auth/login`                            | 로그인                                  | 재혁 이 | 200 OK         |
| 4   | Auth             | `POST`   | `/api/auth/password/reset-link`              | 인증 코드 요청 (비밀번호 찾기)          | 재혁 이 | 202 Accepted   |
| 5   | Auth             | `POST`   | `/api/auth/password/verify-code`             | 인증 번호 검증 (비밀번호 찾기)          | 재혁 이 | 200 OK         |
| 6   | Auth             | `POST`   | `/api/auth/password/resets`                  | 새 비밀번호 설정 (비밀번호 찾기)        | 재혁 이 | 204 No Content |
| 7   | Auth             | `POST`   | `/api/members/check-email`                   | 이메일(아이디) 중복 확인                | 재혁 이 | 200 OK         |
| 8   | Auth             | `GET`    | `/api/members/me`                            | 회원정보 조회                           | 재혁 이 | 200 OK         |
| 9   | Main             | `GET`    | `/api/notifications`                         | 알림 목록 조회                          | 재혁 이 | 200 OK         |
| 10  | Main             | `PATCH`  | `/api/notifications/read`                    | 모든 알림 읽음                          | 재혁 이 | 200 OK         |
| 11  | Main             | `DELETE` | `/api/notifications/{notificationId}`        | 단건 알림 삭제                          | 재혁 이 | 200 OK         |
| 12  | Main             | `DELETE` | `/api/notifications`                         | 모든 알림 삭제                          | 재혁 이 | 200 OK         |
| 13  | Card             | `GET`    | `/api/user-cards`                            | 보유 카드 목록 조회                     | 재혁 이 | 200 OK         |
| 14  | Card             | `DELETE` | `/api/user-cards/{userCardId}`               | 보유 카드 삭제                          | 재혁 이 | 204 No Content |
| 15  | Card             | `GET`    | `/api/user-cards/{userCardId}`               | 보유 카드 상세 조회                     | 재혁 이 | 200 OK         |
| 16  | Card             | `POST`   | `/api/user-cards`                            | 보유 카드 등록                          | 재혁 이 | 201 Created    |
| 17  | Card             | `PATCH`  | `/api/user-cards/{userCardId}/main`          | 대표 카드 설정                          | 재혁 이 | 204 NoContent  |
| 18  | Point/Mem        | `GET`    | `/api/cards/{cardId}/benefits`               | 카드별 혜택 상세 조회                   | 재혁 이 | 200 OK         |
| 19  | Settings         | `GET`    | `/api/members/me`                            | 회원 정보 조회                          | 재혁 이 | 200 OK         |
| 20  | Settings         | `PATCH`  | `/api/members/me`                            | 회원 정보 수정                          | 재혁 이 | 200 OK         |
| 21  | Settings         | `POST`   | `/api/auth/logout`                           | 로그아웃                                | 재혁 이 | 204 No Content |
| 22  | Settings         | `PATCH`  | `/api/members/me/password`                   | 비밀번호 변경                           | 재혁 이 | 204 No Content |
| 23  | Settings         | `GET`    | `/api/notification-settings`                 | 알림 설정 조회                          | 재혁 이 | 200 OK         |
| 24  | Settings         | `PATCH`  | `/api/notification-settings`                 | 알림 설정 수정                          | 재혁 이 | 200 OK         |
| 25  | Settings         | `GET`    | `/api/members/me/personalization`            | 개인화 설정 조회                        | 재혁 이 | 200 OK         |
| 26  | Settings         | `PATCH`  | `/api/members/me/personalization`            | 개인화 설정 수정                        | 재혁 이 | 204 No Content |
| 27  | Settings         | `DELETE` | `/api/members/me`                            | 회원탈퇴                                | 재혁 이 | 204 No Content |
| 28  | -                | `POST`   | `/api/auth/token`                            | 토큰 재발급                             | 재혁 이 | 200 OK         |
| 29  | Transaction      | `GET`    | `/api/transactions`                          | 소비내역 목록 조회                      | 허강상  | 200 OK         |
| 30  | Transaction      | `GET`    | `/api/transactions/{expenseId}`              | 소비내역 상세 조회                      | 허강상  | 200 OK         |
| 31  | Transaction      | `POST`   | `/api/transactions/sync`                     | 마이데이터 거래 동기화                  | 허강상  | 200 OK         |
| 32  | Transaction      | `GET`    | `/api/expense-categories`                    | 소비카테고리 목록 조회                  | 허강상  | 200 OK         |
| 33  | Payment          | `POST`   | `/api/payments`                              | 결제 처리                               | 허강상  | 201 Created    |
| 34  | Payment          | `GET`    | `/api/payments/{paymentId}`                  | 결제 결과 조회                          | 허강상  | 200 OK         |
| 35  | Point/Membership | `GET`    | `/api/points`                                | 포인트 목록 조회                        | 허강상  | 200 OK         |
| 36  | Point/Membership | `GET`    | `/api/points/history`                        | 포인트 내역 조회                        | 허강상  | 200 OK         |
| 37  | Point/Membership | `GET`    | `/api/points/{pointProviderId}/usage-places` | 포인트 사용처 조회                      | 허강상  | 200 OK         |
| 38  | Point/Membership | `GET`    | `/api/memberships/providers`                 | 멤버십 등록 가능 목록 및 기본 추천 조회 | 허강상  | 200 OK         |
| 39  | Point/Membership | `POST`   | `/api/memberships`                           | 멤버십 등록                             | 허강상  | 201 Created    |
| 40  | Point/Membership | `DELETE` | `/api/memberships/{membershipRegisterId}`    | 멤버십 등록 해제                        | 허강상  | 200 OK         |
| 41  | Point/Membership | `GET`    | `/api/memberships/{membershipRegisterId}`    | 멤버십 상세 조회                        | 허강상  | 200 OK         |
| 42  | -                | `POST`   | `/api/recommendations`                       | 결제 직전 최적 카드 추천                | 현준 고 | 200 OK         |
| 43  | -                | `GET`    | `/api/cards/monthly-status`                  | 보유 카드 전체 현황                     | 현준 고 | 200 OK         |
| 44  | -                | `GET`    | `/api/cards/{userCardId}/monthly-status`     | 보유 카드 상세 현황                     | 현준 고 | 200 OK         |
| 45  | -                | `POST`   | `/api/settlements/cancel`                    | 결제 취소 상태 갱신                     | 현준 고 | 200 OK         |
| 46  | -                | `GET`    | `/api/points/recommendations`                | 포인트 추천                             | 현준 고 | 200 OK         |

## 공통 에러 코드

### 1) 요청 검증

| **HTTP** | **코드**                  | **설명**                            |
| -------- | ------------------------- | ----------------------------------- |
| 400      | `INPUT_INVALID`           | Request Body 입력값 검증 실패       |
| 400      | `QUERY_PARAMETER_INVALID` | Query Parameter 검증 실패           |
| 400      | `PATH_PARAMETER_INVALID`  | Path Variable 검증 실패             |
| 400      | `ENUM_VALUE_INVALID`      | 지원하지 않는 열거형 값             |
| 400      | `RESOURCE_STATE_INVALID`  | 현재 리소스 상태에서 요청 수행 불가 |

### 2) 인증 및 권한

| **HTTP** | **코드**               | **설명**                                                  |
| -------- | ---------------------- | --------------------------------------------------------- |
| 401      | `ACCESS_TOKEN_INVALID` | Access Token (누락 되었거나 형식·서명 등이 유효하지 않음) |
| 401      | `ACCESS_TOKEN_EXPIRED` | Access Token 만료                                         |
| 401      | `REFRESH_TOKEN_FAILED` | Refresh Token (없음, 만료, 유효하지 않음, 폐기)           |
| 403      | `ACCESS_DENIED`        | 인증됐지만 해당 리소스 접근 권한 없음                     |

### 3) 서버 오류

| **HTTP** | **코드**                | **설명**  |
| -------- | ----------------------- | --------- |
| 500      | `SERVER_INTERNAL_ERROR` | 서버 에러 |

### 4) 리소스 예외

| **HTTP** | **코드**    | **설명**                      |
| -------- | ----------- | ----------------------------- |
| 404      | `NOT_FOUND` | 요청한 리소스가 존재하지 않음 |

> 💡 어떤 데이터가 없는지는 `message`에 세부 내용을 적어서 프론트에 보낸다.

---

## Auth

### 1. 약관 목록 조회

```
GET /api/terms
```

회원가입 또는 약관 동의 화면에서 표시할 현재 유효한 약관 목록과 최신 약관 버전 정보를 조회한다.

**화면** W_Auth_Terms · **라우트** /auth/terms · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 약관 상태가 ACTIVE인 약관만 조회한다.
- 현재 시점에 유효한 약관 버전을 조회한다.
- 필수 약관과 선택 약관을 함께 응답한다.
- 프론트는 required 값으로 필수/선택 여부를 구분한다.
- “전체 동의하기”는 프론트에서 처리한다.
- 약관 상세 화면이 필요한 경우,
- content를 응답에 포함하거나 별도 상세 조회 API로 분리할 수 있다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "약관 목록 조회에 성공했습니다.",
  "data": {
    "terms": [
      {
        "termsId": 1,
        "termsCode": "SERVICE_TERMS",
        "termsName": "서비스 이용약관",
        "required": true,
        "termsStatus": "ACTIVE",
        "termsVersionId": 10,
        "version": "2026-07-16",
        "effectiveStartedAt": "2026-07-16T00:00:00"
      },
      {
        "termsId": 2,
        "termsCode": "PRIVACY_POLICY",
        "termsName": "개인정보 수집 및 이용 동의",
        "required": true,
        "termsStatus": "ACTIVE",
        "termsVersionId": 11,
        "version": "2026-07-16",
        "effectiveStartedAt": "2026-07-16T00:00:00"
      },
      {
        "termsId": 3,
        "termsCode": "MARKETING_CONSENT",
        "termsName": "마케팅 정보 수신 동의",
        "required": false,
        "termsStatus": "ACTIVE",
        "termsVersionId": 12,
        "version": "2026-07-16",
        "effectiveStartedAt": "2026-07-16T00:00:00"
      }
    ]
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "TERMS_READ_FAILED",
  "message": "약관 목록 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

### 2. 회원가입

```
POST /api/auth/signup
```

이름, 이메일, 비밀번호를 입력받아 신규 회원 계정을 생성한다.

**화면** W_Auth_Signup · **라우트** /auth/signup · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 201 Created

**Business Rules**

- 이메일은 다른 회원과 중복될 수 없다.
- 비밀번호는 암호화가 아닌 단방향 해시로 저장한다.
- 회원 상태의 기본값은 ACTIVE이다.
- 회원가입 성공 후, 로그인 인증 성공 시 Access Token을 발급한다.
- 회원가입 성공 후, 로그인을 하려면 별도로 로그인을 수행해야 한다.
- 회원 생성과 약관 동의 저장은 하나의 트랜잭션으로 처리한다.

**Request**

```json
{
  "email": "user@example.com",
  "password": "1234",
  "name": "이재혁",
  "termsAgreements": [
    {
      "termsVersionId": 10,
      "agreed": true
    },
    {
      "termsVersionId": 11,
      "agreed": true
    },
    {
      "termsVersionId": 12,
      "agreed": false
    }
  ]
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "이재혁",
    "memberStatus": "ACTIVE",
    "createdAt": "2026-07-18T11:00:00"
  }
}
```

**고유 에러**

`EMAIL_ALREADY_EXISTS(409)`

**Error Response Format**

```json
{
  "success": false,
  "code": "INPUT_INVALID",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    {
      "field": "email",
      "message": "이메일 형식이 아닙니다."
    },
    {
      "field": "password",
      "message": "비밀번호는 8자 이상이어야 합니다."
    }
  ]
}
```

### 3. 로그인

```
POST /api/auth/login
```

이메일과 비밀번호를 검증한 후, Access Token과 Refresh Token을 발급한다.

**화면** W_05_Login · **라우트** /auth/login · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 이메일과 비밀번호가 모두 일치해야 한다.
- ACTIVE 상태인 회원만 로그인할 수 있다.
- 비밀번호 불일치 시 이메일 존재 여부를 구분해서 노출하지 않는다.
- 로그인 성공 시, 기존 Refresh Token 처리 정책을 결정한다.
  -> (back) 기존 토큰 만료 방식(로그인 할 때마다 기존 Refresh Token을 삭제)
  -> (front) 중복 로그인 발생 시, 기존 세션 로그아웃을 어떻게 처리할지 (에러코드 규격, 연출 방식 등)
  -> (front) 인터셉터(백엔드 요청 보내기 전, 응답 박은 직후에 자동 실행되는 코드) 설정 필요
- 단일 기기 로그인만 허용하면 기존 Refresh Token을 폐기한다.

**Request**

```json
{
  "email": "user@example.com",
  "password": "1234"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "member": {
      "memberId": 1,
      "email": "user@example.com",
      "name": "이재혁"
    }
  }
}
```

**고유 에러**

`LOGIN_CREDENTIAL_MISMATCH(401) MEMBER_SUSPENDED(403) MEMBER_WITHDRAWN(403)`

**Error Response Format**

```json
{
  "success": false,
  "code": "LOGIN_CREDENTIAL_MISMATCH",
  "message": "아이디 또는 비밀번호가 일치하지 않습니다.",
  "errors": [
    {
      "field": "loginForm",
      "message": "이메일 또는 비밀번호를 다시 확인해주세요."
    }
  ]
}
```

### 4. 인증 코드 요청 (비밀번호 찾기)

```
POST /api/auth/password/reset-link
```

회원 이메일을 입력받아, 비밀번호 초기화 인증 코드를 발급한다.

**화면** W_05_Login -팝업 · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 202 Accepted

**Business Rules**

- 가입된 이메일이라면 인증 코드 또는 초기화 링크를 발송한다.
- 존재하지 않는 이메일이어도 동일한 성공 응답을 반환한다.
- 인증 코드는 제한된 시간 동안만 유효하다.
- 동일 이메일의 반복 요청에는 횟수 또는 시간 제한을 둘 수 있다.
- 새 요청을 발급하면 기존 미사용 인증 코드는 폐기한다.

**Request**

```json
{
  "email": "user@example.com"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "입력한 이메일로 비밀번호 초기화 안내를 전송했습니다.",
  "data": null
}
```

**고유 에러**

`PASSWORD_RESET_LIMIT_EXCEEDED(429)`

**Error Response Format**

```json
{
  "success": false,
  "code": "PASSWORD_RESET_LIMIT_EXCEEDED",
  "message": "비밀번호 재설정 요청 횟수를 초과했습니다. 24시간 후에 다시 시도해 주세요.",
  "errors": []
}
```

### 5. 인증 번호 검증 (비밀번호 찾기)

```
POST /api/auth/password/verify-code
```

비밀번호 재설정을 위해 이메일로 발송된 인증 번호를 검증한다.

**화면** W_05_Login -팝업 · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 이메일과 인증 번호가 일치해야 한다.
- 인증 번호는 만료되지 않아야 한다.
- 이미 사용 완료 처리된 인증 번호는 다시 사용할 수 없다.
- 인증 번호 검증 성공 시, 새 비밀번호 설정에 사용할 임시 토큰을 발급한다.
- 인증 번호 검증 성공 시점에 인증 번호를 바로 사용 완료 처리할지,
- 비밀번호 변경 성공 시 사용 완료 처리할지는 정책으로 결정할 수 있다.
- 인증 실패 횟수가 일정 횟수를 초과하면 추가 검증을 제한한다.
- 비밀번호 재설정 토큰은 짧은 유효시간(5~10분 정도)을 가진다.

**Request**

```json
{
  "email": "user@example.com",
  "verificationCode": "482913"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "인증 번호 검증에 성공했습니다.",
  "data": {
    "passwordResetToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 500
  }
}
```

**고유 에러**

`PASSWORD_RESET_CODE_INVALID(400) PASSWORD_RESET_CODE_EXPIRED(400) PASSWORD_RESET_CODE_ALREADY_USED(409) MEMBER_NOT_FOUND(404) PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED(429)`

**Error Response Format**

```json
{
  "success": false,
  "code": "PASSWORD_RESET_CODE_INVALID",
  "message": "인증 번호가 일치하지 않습니다.",
  "errors": []
}
```

### 6. 새 비밀번호 설정

(비밀번호 찾기)

```
POST /api/auth/password/resets
```

인증 번호 검증 후 발급받은 비밀번호 재설정 토큰을 검증하고, 새로운 비밀번호로 변경한다.

**화면** W_05_PasswordChange · **라우트** /auth/password-change · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 비밀번호 재설정 토큰이 유효해야 한다.
- 비밀번호 재설정 토큰은 만료되지 않아야 한다.
- 이미 사용된 비밀번호 재설정 토큰은 다시 사용할 수 없다.
- 새 비밀번호는 비밀번호 정책을 충족해야 한다.
- 새 비밀번호는 기존 비밀번호와 같지 않도록 제한할 수 있다.
- 비밀번호 변경 성공 시 인증 번호와 비밀번호 재설정 토큰을 사용 완료 처리한다.
- 비밀번호 변경 성공 시 해당 회원의 기존 Refresh Token을 모두 폐기한다.
- 비밀번호 변경 성공 후 사용자는 다시 로그인해야 한다.

**Request**

```json
{
  "passwordResetToken": "eyJhbGciOiJIUzI1NiJ9...",
  "newPassword": "NewPassword456!"
}
```

**고유 에러**

`NEW_PASSWORD_INVALID(400) PASSWORD_RESET_TOKEN_INVALID(400) PASSWORD_RESET_TOKEN_EXPIRED(400) PASSWORD_RESET_TOKEN_ALREADY_USED(409) NEW_PASSWORD_SAME_AS_OLD_PASSWORD(409) MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "PASSWORD_RESET_TOKEN_EXPIRED",
  "message": "비밀번호 재설정 시간이 만료되었습니다. 다시 인증을 진행해 주세요.",
  "errors": []
}
```

### 7. 이메일(아이디) 중복 확인

```
POST /api/members/check-email
```

입력한 이메일이 회원가입에 사용 가능한지 확인한다.

**화면** W_Auth_Signup · **라우트** /auth/signup · **권한** GUEST · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 이메일 형식이 올바른 경우에만 중복 여부를 조회한다.
- 탈퇴 회원의 이메일 재사용 정책을 결정해야 한다.
  -> 30일 동안 같은 이메일로 재가입 불가
  -> 30일이 지나면 탈퇴한 사용자의 이메일을 더미 주소(e.g. withdrawn_1234@dumy.com)으로 바꿈
- => 30일이 지나면 이메일 중복이 아니므로, 재가입 가능

**Request**

```json
{
  "data": {
    "email": "user@example.com"
  }
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "이메일 중복 확인 과정이 완료되었습니다.",
  "data": {
    "email": "user@example.com",
    "available": true
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "EMAIL_FORMAT_INVALID",
  "message": "이메일 형식이 올바르지 않습니다.",
  "errors": [
    {
      "field": "email",
      "message": "이메일 형식이 아닙니다."
    }
  ]
}
```

### 8. 회원정보 조회

```
GET /api/members/me
```

Access Token으로 인증된 현재 회원의 정보를 조회한다.

**화면** W_05_Login · **라우트** /auth/login · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 회원 ID는 요청값으로 받지 않고 인증 정보에서 추출한다.
- 비밀번호 해시와 Refresh Token 등 민감정보는 반환하지 않는다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "회원정보 조회에 성공했습니다.",
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "이재혁",
    "memberStatus": "ACTIVE",
    "createdAt": "2026-07-18T11:00:00",
    "updatedAt": "2026-07-18T11:00:00"
  }
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBER_NOT_FOUND",
  "message": "인증된 회원 정보를 찾을 수 없습니다.",
  "errors": []
}
```

## Main

### 9. 알림 목록 조회

```
GET /api/notifications
```

현재 로그인한 회원에게 생성된 알림 목록을 조회한다.

**화면** W_Notification · **라우트** /notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원 자신의 알림만 조회한다.
- 회원 ID는 Access Token에서 추출한다.
- 기본 정렬은 최신 생성 순인 createdAt DESC이다.
- 알림이 없으면 오류가 아니라 빈 목록을 반환한다.
- read=false는 readAt이 null인 알림을 의미한다.
- 사용자 화면에는 일반적으로 SENT 상태의 알림만 노출한다.
- PENDING, FAILED 상태를 사용자에게 보여줄 필요가 없다면 서버에서 제외한다.
- 알림 내용과 연결된 카드 또는 혜택이 삭제되더라도 기존 알림 이력은 유지한다.
- 목록 응답은 화면에 필요한 요약 정보만 반환한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "알림 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "notificationId": 101,
        "notificationType": "PERFORMANCE_SHORTAGE",
        "title": "카드 실적이 부족해요",
        "content": "생활 할인 카드의 실적까지 80,000원이 남았습니다.",
        "deliveryStatus": "SENT",
        "read": false,
        "scheduledAt": "2026-07-25T09:00:00",
        "sentAt": "2026-07-25T09:00:03",
        "readAt": null,
        "createdAt": "2026-07-25T09:00:00",
        "userCardId": 15,
        "cardName": "생활 할인 카드"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "unreadCount": 1
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_TYPE_INVALID",
  "message": "알림 유형이 올바르지 않습니다.",
  "errors": [
    {
      "field": "notificationType",
      "message": "지원하지 않는 알림 유형입니다."
    }
  ]
}
```

### 10. 모든 알림 읽음

```
PATCH /api/notifications/read
```

현재 로그인한 회원의 읽지 않은 알림을 모두 읽음 상태로 변경한다.

**화면** W_Notification · **라우트** /notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 알림만 처리한다.
- 회원 ID는 Access Token에서 추출한다.
- 현재 읽지 않은 알림만 처리한다.
- 이미 읽은 알림의 readAt은 변경하지 않는다.
- 삭제된 알림은 읽음 처리 대상에서 제외한다.
- 발송이 완료된 SENT 상태 알림만 읽음 처리한다.
- 읽지 않은 알림이 없어도 오류가 아닌 정상 응답을 반환한다.
- 모든 알림은 동일한 처리 시각으로 읽음 처리한다.
- 처리 작업은 하나의 트랜잭션으로 수행한다.
- 읽음 처리는 알림을 삭제하지 않는다.
- 읽음 처리 후에도 알림 목록에서 계속 조회된다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "모든 알림 읽음 처리에 성공했습니다.",
  "data": {
    "processedCount": 5,
    "readAt": "2026-07-25T14:20:00",
    "unreadCount": 0
  }
}
```

이미 모든 알림이 읽음 상태인 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "모든 알림 읽음 처리에 성공했습니다.",
  "data": {
    "processedCount": 0,
    "readAt": "2026-07-25T14:20:00",
    "unreadCount": 0
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_READ_FAILED",
  "message": "모든 알림 읽음 처리에 실패했습니다.",
  "errors": []
}
```

### 11. 단건 알림 삭제

```
DELETE /api/notifications/{notificationId}
```

현재 로그인한 회원의 특정 알림 하나를 삭제한다.

**화면** W_Notification · **라우트** /notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원이 소유한 알림만 삭제할 수 있다.
- 회원 ID는 Access Token에서 추출하며 요청값으로 받지 않는다.
- notificationId에 해당하는 알림이 현재 회원의 알림인지 확인한다.
- 존재하지 않거나 다른 회원이 소유한 알림은 보안상 동일하게 404 Not Found로 처리한다.
- 실제 DB 행은 삭제하지 않고 deletedAt에 현재 일시를 저장한다.
- 삭제된 알림은 알림 목록 및 상세 조회 결과에서 제외한다.
- 삭제하더라도 발송 상태, 발송 일시, 알림 내용, 중복 제거 키 등의 기존 정보는 유지한다.
- 삭제 시 읽음 여부는 변경하지 않는다.
- 이미 삭제된 알림에 다시 요청하면 404 Not Found로 처리한다.
- 단건 삭제는 다른 알림에 영향을 주지 않는다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "알림 삭제에 성공했습니다.",
  "data": {
    "notificationId": 101,
    "deletedAt": "2026-07-25T15:10:00"
  }
}
```

**고유 에러**

`NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_ID_INVALID",
  "message": "알림 ID가 올바르지 않습니다.",
  "errors": [
    {
      "field": "notificationId",
      "message": "notificationId는 1 이상의 정수여야 합니다."
    }
  ]
}
```

### 12. 모든 알림 삭제

```
DELETE /api/notifications
```

현재 로그인한 회원의, 모든 알림을 한 번에 삭제한다.

**화면** W_Notification · **라우트** /notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 회원 본인의 알림만 처리한다.
- 회원 ID는 Access Token에서 추출하며 요청값으로 받지 않는다.
- 현재 회원에게 노출되는 모든 알림을 삭제한다.
- 이미 삭제된 알림은 처리 대상에서 제외한다.
- 알림이 하나도 없어도 오류가 아니라 정상 처리한다.
- 삭제된 알림은 알림 목록과 상세 조회 결과에서 제외한다.
- 알림 삭제 후에도 알림 중복 방지 키와 발송 이력은 DB에 유지된다.
- 모든 알림 삭제는 하나의 트랜잭션으로 처리한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "모든 알림 삭제에 성공했습니다.",
  "data": {
    "deletedCount": 12,
    "deletedAt": "2026-07-25T14:30:00"
  }
}
```

삭제할 알림이 없는 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "모든 알림 삭제에 성공했습니다.",
  "data": {
    "deletedCount": 0,
    "deletedAt": "2026-07-25T14:30:00"
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_DELETE_FAILED",
  "message": "모든 알림 삭제에 실패했습니다.",
  "errors": []
}
```

## Card

### 13. 보유 카드 목록 조회

```
GET /api/user-cards
```

현재 로그인한 사용자가 등록한 보유 카드 목록을 조회한다.

**화면** W_CardList · **라우트** /cards · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 인증된 사용자의 보유 카드만 조회한다.
- 회원 ID는 Access Token에서 추출한다.
- 다른 회원의 보유 카드는 반환하지 않는다.
- cardStatus가 ACTIVE인 카드만 조회한다.
- 삭제된 카드는 목록에서 제외한다.
- 기본 정렬은 최근 등록 순인 registeredAt DESC로 한다. (정렬 기준을 바꿔도 됨)
- 등록한 보유 카드가 없으면 오류가 아니라 빈 배열을 반환한다.
- 실적 값은 사용자 카드 월별 실적 현황 데이터를 사용하여 계산한다.

**Request**

없음

**Response**

보유 카드가 있는 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "보유 카드 목록 조회에 성공했습니다.",
  "data": {
    "userCards": [
      {
        "userCardId": 15,
        "cardId": 1,
        "cardName": "신한카드 Mr.Life",
        "issuerName": "신한카드",
        "cardType": "CREDIT",
        "maskedCardNumber": "****-****-****-5678",
        "imageUrl": "https://example.com/images/cards/1.png",
        "achievementRate": 80.0,
        "performanceMet": false,
        "registeredAt": "2026-07-18T12:30:00"
      }
    ],
    "totalCount": 1
  }
}
```

보유 카드가 없는 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "보유 카드 목록 조회에 성공했습니다.",
  "data": {
    "userCards": [],
    "totalCount": 0
  }
}
```

**Error Response Format**

```json
{
  "success": false,
  "code": "USER_CARD_LIST_FAILED",
  "message": "보유 카드 목록 조회에 실패했습니다.",
  "errors": []
}
```

### 14. 보유 카드 삭제

```
DELETE /api/user-cards/{userCardId}
```

현재 로그인한 사용자가 등록한 보유 카드를 삭제한다.

**화면** W_CardList · **라우트** /cards · **권한** USER · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 인증된 사용자가 소유한 보유 카드만 삭제할 수 있다.
- 다른 사용자의 보유 카드 또는 존재하지 않는 보유 카드는 404 Not Found로 처리한다.
- 물리적으로 행을 삭제하지 않고 상태를 DELETED로 변경한다.
- 삭제한 카드의 기존 소비 내역, 실적 현황, 혜택 이용 이력은 삭제하지 않는다.
- 이미 DELETED 상태인 카드에 다시 삭제 요청이 오면,
- 리소스 상태 확인을 명확하게 하기 위 404로 처리한다.
- 삭제된 동일 카드를 나중에 재등록하면 기존 행을 ACTIVE로 복구한다.

**Request**

없음

**고유 에러**

`NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "USER_CARD_NOT_FOUND",
  "message": "보유 카드를 찾을 수 없습니다.",
  "errors": []
}
```

### 15. 보유 카드 상세 조회

```
GET /api/user-cards/{userCardId}
```

현재 로그인한 사용자가 등록한 특정 보유 카드의 상세 정보를 조회한다.

**화면** W_CardDetail · **라우트** /cards/:id · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 인증된 사용자가 소유한 보유 카드만 조회할 수 있다.
- URL의 userCardId가 로그인한 사용자의 카드인지 확인해야 한다.
- 다른 회원의 카드이거나 존재하지 않는 카드인 경우 보안상 동일하게 404 Not Found를 반환한다.
- DELETED 상태의 카드는 조회하지 않는다.
- yearMonth를 생략하면 현재 연월을 기준으로 조회한다.
- **실적과 혜택 이용 상태는 이 API에서 계산하지 않는다. 엔진 현황 서비스를 호출해 받은 값을 그대로 내려준다.** 실적 제외 규칙·구간 판정·묶음 한도·구간별 한도 상속이 모두 엔진에 있어, 여기서 다시 계산하면 화면마다 숫자가 달라진다.
- 응답의 실적·혜택 필드명은 엔진 현황 API와 동일하게 맞춘다. 프론트가 같은 값을 두 이름으로 받지 않게 하기 위함이다.
- 주요 혜택은 화면에 표시할 일정 개수만 반환한다. 전체는 엔진 현황 API를 호출한다.
- 해당 월 상태 행이 없으면 엔진이 그 시점에 생성한다(전월 행에서 전월실적을 가져오고, 없으면 소비내역 합산). 0을 기본값으로 내려보내면 실적 구간이 0원으로 판정되어 실적 조건부 혜택이 전부 사라진다.
- 카드 상세 화면 하단의 소비 내역은 별도 소비 내역 API를 호출한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "보유 카드 상세 조회에 성공했습니다.",
  "data": {
    "userCardId": 15,
    "card": {
      "cardId": 1,
      "cardName": "신한카드 Mr.Life",
      "issuerName": "신한카드",
      "cardType": "CREDIT",
      "annualFee": 15000,
      "imageUrl": "https://example.com/images/cards/1.png",
      "maskedCardNumber": "****-****-****-5678"
    },
    "performance": {
      "yearMonth": "2026-07",
      "prevPerformanceAmount": 520000,
      "targetPerformance": 500000,
      "currentPerformanceAmount": 400000,
      "remainingPerformance": 100000,
      "achievementRate": 80.0,
      "performanceMet": true,
      "sharedLimit": 20000,
      "sharedLimitUsed": 8000
    },
    "majorBenefits": [
      {
        "benefitId": 101,
        "benefitName": "편의점·약국 All Day 10% 할인",
        "limitGroupCode": null,
        "monthlyLimit": 10000,
        "usedAmount": 8000,
        "remainingLimit": 2000,
        "usageRate": 80.0
      }
    ],
    "registeredAt": "2026-07-10T10:00:00"
  }
}
```

**고유 에러**

`NOT_FOUND(404)`

### 16. 보유 카드 등록

```
POST /api/user-cards
```

현재 로그인한 사용자가 서비스에 등록된 카드 상품을 자신의 보유 카드로 등록한다.

**화면** W_CardRegister · **라우트** /cards/register · **권한** USER · **담당** 재혁 이 · **상태 코드** 201 Created

**Business Rules**

- 인증된 사용자만 보유 카드를 등록할 수 있다.
- 회원 ID는 Access Token에서 추출하며 Request Body로 받지 않는다.
- 서비스에 등록된 카드 상품만 보유 카드로 등록할 수 있다.
- 동일 회원은 같은 카드 상품을 중복 등록할 수 없다.
- 동일 카드가 ACTIVE 상태로 존재하면 409 Conflict를 반환한다.
- 동일 카드가 DELETED 상태로 존재하면 해당 데이터를 ACTIVE로 복구한다.
- 카드번호는 전체 번호를 저장하지 않고 마지막 4자리만 입력받아 마스킹하여 저장한다.
- 카드번호 마지막 4자리는 숫자 4자리여야 한다.
- 입력된 카드번호 16자리 전체를 룬 알고리즘을 돌려 검증한다.
- 카드번호 앞 6자리를 잘라서, 카드 상품의 BIN과 일치하는지 검증한다.

**Request**

```json
{
  "cardId": 1,
  "cardNumbers": "1234567812345678",
  "expirationDate": "2031-05",
  "cvc": "123"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "보유 카드 등록에 성공했습니다.",
  "data": {
    "userCardId": 15,
    "cardId": 1,
    "cardName": "신한카드 Mr.Life",
    "issuerName": "신한카드",
    "cardType": "CREDIT",
    "maskedCardNumber": "****-****-****-5678",
    "imageUrl": "https://example.com/images/cards/1.png",
    "cardStatus": "ACTIVE",
    "registeredAt": "2026-07-18T12:30:00"
  }
}
```

**고유 에러**

`CARD_BIN_MISMATCH(400) NOT_FOUND(404) USER_CARD_ALREADY_EXISTS(409)`

**Error Response Format**

```json
{
  "success": false,
  "code": "USER_CARD_ALREADY_EXISTS",
  "message": "이미 등록된 카드입니다.",
  "errors": []
}
```

### 17. 대표 카드 설정

```
PATCH /api/user-cards/{userCardId}/main
```

현재 로그인한 회원의 특정 보유 카드를 홈 화면에 표시할 대표 카드로 설정한다.

**화면** W*Main*홈 · **라우트** /home · **권한** USER · **담당** 재혁 이 **상태 코드** 204 No Content

**Business Rules**

- 로그인한 회원만 대표 카드를 설정할 수 있다.
- 본인이 보유한 카드만 대표 카드로 설정할 수 있다.
- 카드 상태가 `ACTIVE`인 보유 카드만 대표 카드로 설정할 수 있다.
- 회원당 대표 카드는 최대 1개만 허용한다.
- 새로운 카드를 대표 카드로 설정하면, 같은 회원의 기존 대표 카드는 자동 해제한다.
- 이미 대표 카드로 설정된 카드를 다시 설정해도 성공 처리할 수 있다.
- 새로운 대표 카드 설정 시 `user_cards.is_representative_card`를 `true`로 변경한다.
  -> 기존 대표 카드는 `user_cards.is_representative_card`를 `false`로 변경한다.
- 대표 카드 설정 처리는 트랜잭션으로 처리한다.

**Request**

없음

**고유 에러**

`USER_CARD_ACCESS_DENIED(403) USER_CARD_NOT_FOUND(404) USER_CARD_DELETED(409) MEMBER_STATUS_INVALID(409)`

**Error Response Format**

```json
{
  "success": false,
  "code": "USER_CARD_ACCESS_DENIED",
  "message": "해당 보유 카드에 접근할 수 없습니다.",
  "errors": []
}
```

## Point/Mem

### 18. 카드별 혜택 상세 조회

```
GET /api/cards/{cardId}/benefits
```

특정 카드 상품이 제공하는 전체 혜택과 각 혜택의 상세 적용 조건을 조회한다.

**화면** W_BenefitDetail · **라우트** /benefits/:id · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 존재하는 카드 상품의 혜택만 조회할 수 있다.
- 로그인 여부와 관계없이 조회할 수 있다.
- 카드 상품에 연결된 전체 혜택을 반환한다.
- 혜택은 화면 표시 기준에 따라 카테고리별로 구분해서 반환한다.
- 혜택 1건은 대상이 하나다(가맹점 또는 카테고리 또는 전 가맹점). 약관이 여러 대상을 열거하면 대상마다 혜택 행이 따로 있고, 같은 묶음 한도를 쓰면 limitGroupCode가 같다.
- 대상이 가맹점이면 merchantName, 카테고리면 categoryCode·categoryName을 채운다. 전 가맹점(ALL)이면 둘 다 null이다.
- 실적 조건은 requirePerformance(Y/N)로만 반환한다. **요구 금액은 혜택이 아니라 카드 단위(실적구간)라 혜택별로 다를 수 없다.** 금액이 필요하면 카드 실적구간을 함께 조회한다.
- 월 한도가 없는 혜택은 monthlyLimit을 null로 반환한다. limitGroupCode가 있으면 그 값은 묶음 그룹의 공유 한도다.
- 조회 결과가 없으면 오류가 아니라 빈 혜택 목록을 반환한다.
- 사용자 개인의 현재 실적, 혜택 사용액, 잔여 한도는 포함하지 않는다.

**Request**

없음

**Response**

혜택이 있는 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "카드 혜택 상세 조회에 성공했습니다.",
  "data": {
    "cardId": 1,
    "cardName": "생활 할인 카드",
    "benefits": [
      {
        "benefitId": 101,
        "benefitName": "카페 10% 청구할인",
        "targetType": "CATEGORY",
        "categoryCode": "CAFE",
        "categoryName": "카페",
        "merchantName": null,
        "benefitKind": "DISCOUNT",
        "calcMethod": "RATE",
        "benefitValue": 10,
        "requirePerformance": "Y",
        "limitGroupCode": null,
        "monthlyLimit": 5000
      },
      {
        "benefitId": 102,
        "benefitName": "GS25 5% 적립",
        "targetType": "MERCHANT",
        "categoryCode": null,
        "categoryName": null,
        "merchantName": "GS25",
        "benefitKind": "POINT",
        "calcMethod": "RATE",
        "benefitValue": 5,
        "requirePerformance": "Y",
        "limitGroupCode": "CVS_GROUP",
        "monthlyLimit": 3000
      }
    ],
    "totalCount": 2
  }
}
```

혜택이 없는 경우:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "카드 혜택 상세 조회에 성공했습니다.",
  "data": {
    "cardId": 1,
    "cardName": "생활 할인 카드",
    "benefits": [],
    "totalCount": 0
  }
}
```

**고유 에러**

`NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "CARD_ID_INVALID",
  "message": "카드 상품 ID가 올바르지 않습니다.",
  "errors": [
    {
      "field": "cardId",
      "message": "cardId는 1 이상의 정수여야 합니다."
    }
  ]
}
```

## Settings

### 19. 회원 정보 조회

```
GET /api/members/me
```

Access Token으로 인증된 현재 회원의 정보를 조회한다.

**화면** W_Settings W_Profile W_Accountinfo · **라우트** /settings /settings/profile /settings/account · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 회원 ID는 요청값으로 받지 않고 인증 정보에서 추출한다.
- 비밀번호 해시와 Refresh Token 등 민감정보는 반환하지 않는다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "회원정보 조회에 성공했습니다.",
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "이재혁",
    "memberStatus": "ACTIVE",
    "createdAt": "2026-07-18T11:00:00",
    "updatedAt": "2026-07-18T11:00:00"
  }
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBER_NOT_FOUND",
  "message": "인증된 회원 정보를 찾을 수 없습니다.",
  "errors": []
}
```

> 불필요한 회원정보 조회 API 중복 호출을 방지하기 위해, 앱 최초 실행(Mount) 및 로그인 시점에 API를 1회 호출하여 Pinia 전역 스토어에 캐싱한다. 해당 화면들은 API 통신 없이 Pinia 데이터를 즉시 참조하여 렌더링한다.

### 20. 회원 정보 수정

```
PATCH /api/members/me
```

현재 로그인한 회원의 수정 가능한 일반 정보를 부분 변경한다.

**화면** W_Profile · **라우트** /settings/profile · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 현재 회원의 정보만 수정할 수 있다.
- 이메일과 비밀번호는 이 API에서 수정하지 않는다.
- 전달되지 않은 필드는 기존 값을 유지한다.

**Request**

```json
{
  "name": "새로운 이름"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "새로운이름",
    "updatedAt": "2026-07-18T12:00:00"
  }
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "INPUT_INVALID",
  "message": "입력값 검증에 실패했습니다.",
  "errors": [
    {
      "field": "name",
      "message": "이름은 한글과 영문만 2~20자 이내로 입력 가능합니다."
    }
  ]
}
```

### 21. 로그아웃

```
POST /api/auth/logout
```

현재 로그인 세션의 Refresh Token을 서버에서 폐기하고, Refresh Token 쿠키를 삭제한다.

**화면** W_SecuritySetting · **라우트** /settings/security · **권한** USER · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 서버에 저장된 현재 Refresh Token을 삭제한다.
- 클라이언트의 Refresh Token 쿠키를 즉시 만료시킨다.
- 기존 Access Token은 짧은 만료시간까지 유효할 수 있다.
- Access Token 블랙리스트(MVP 구현 이후 고민)

**Request**

없음

**Error Response Format**

```json
{
  "success": false,
  "code": "ACCESS_TOKEN_INVALID",
  "message": "유효하지 않거나 변조된 인증 토큰입니다.",
  "errors": []
}
```

### 22. 비밀번호 변경

```
PATCH /api/members/me/password
```

로그인한 회원이 현재 비밀번호를 확인한 후, 새로운 비밀번호로 변경한다.

**화면** W_PasswordChange · **라우트** /settings/security/password · **권한** USER · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 현재 비밀번호가 일치해야 한다.
- 새 비밀번호는 비밀번호 정책을 충족해야 한다.
- 새 비밀번호는 현재 비밀번호와 달라야 한다.
- 변경된 비밀번호는 단방향 해시로 저장한다.
- 비밀번호 변경 후 기존 Refresh Token을 모두 폐기하고 재로그인을 요청한다.

**Request**

```json
{
  "currentPassword": "1234",
  "newPassword": "5678"
}
```

**고유 에러**

`PASSWORD_SAME_AS_CURRENT(400) PASSWORD_MISMATCH(401)  MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "NEW_PASSWORD_INVALID",
  "message": "새 비밀번호가 보안 정책에 맞지 않습니다.",
  "errors": [
    {
      "field": "newPassword",
      "message": "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상 20자 이하로 입력해야 합니다."
    }
  ]
}
```

### 23. 알림 설정 조회

```
GET /api/notification-settings
```

현재 로그인한 회원의 알림 수신 설정을 조회한다.

**화면** W_NotificationSetting · **라우트** /settings/notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 회원은 자신의 알림 설정만 조회할 수 있다.
- 회원 ID는 Access Token에서 추출한다.
- 회원가입 시 알림 설정을 기본값으로 생성한다.
- 설정 행이 없으면 기본 설정을 생성하거나 기본값을 반환할 수 있다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "알림 설정 조회에 성공했습니다.",
  "data": {
    "performanceShortageEnabled": true,
    "benefitLimitEnabled": true,
    "updatedAt": "2026-07-25T10:00:00"
  }
}
```

**고유 에러**

`NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_SETTING_NOT_FOUND",
  "message": "알림 설정을 찾을 수 없습니다.",
  "errors": []
}
```

### 24. 알림 설정 수정

```
PATCH /api/notification-settings
```

현재 로그인한 회원의 알림 수신 설정을 부분 수정한다.

**화면** W_NotificationSetting · **라우트** /settings/notifications · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 회원은 자신의 알림 설정만 수정할 수 있다.
- 부분 수정이므로 전달되지 않은 필드는 기존 값을 유지한다.
- 최소 하나 이상의 설정 필드를 전달해야 한다.
- 모든 설정값은 Boolean이어야 한다.
- 알림 설정을 끄더라도 이미 생성되거나 발송된 알림은 삭제하지 않는다.
  -> 설정 변경 이후 새로 생성되는 알림부터 적용한다.
- 여러 필드 변경은 하나의 트랜잭션으로 처리한다.

**Request**

```json
{
  "performanceShortageEnabled": false,
  "benefitLimitEnabled": true
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "알림 설정 수정에 성공했습니다.",
  "data": {
    "performanceShortageEnabled": false,
    "benefitLimitEnabled": true,
    "updatedAt": "2026-07-25T12:00:00"
  }
}
```

**고유 에러**

`NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "NOTIFICATION_SETTING_REQUEST_INVALID",
  "message": "수정할 알림 설정이 없습니다.",
  "errors": [
    {
      "field": "requestBody",
      "message": "최소 하나 이상의 알림 설정을 입력해야 합니다."
    }
  ]
}
```

### 25. 개인화 설정 조회

```
GET /api/members/me/personalization
```

현재 로그인한 회원의 개인화 설정 정보를 조회한다.

**화면** W_PersonalizationSetting · **라우트** /settings/personalization · **권한** USER · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원만 조회할 수 있다.
- 현재 로그인한 회원의 개인화 설정만 조회할 수 있다.
- 카테고리는 ACTIVE 상태 또는 사용 가능한 카테고리만 조회한다.
- 가맹점은 각 카테고리에 속한 사용 가능한 가맹점만 조회한다.
- 회원이 선택한 관심 카테고리는 selected = true로 응답한다.
- 회원이 선택한 선호 가맹점은 selected = true로 응답한다.
- 개인화 설정이 없는 경우 모든 카테고리의 selected는 false로 응답한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "개인화 설정 조회에 성공했습니다.",
  "data": {
    "categories": [
      {
        "categoryId": 1,
        "categoryCode": "CAFE",
        "categoryName": "카페",
        "selected": true,
        "merchants": [
          {
            "merchantId": 1,
            "merchantName": "스타벅스",
            "selected": true,
            "priority": 1
          },
          {
            "merchantId": 2,
            "merchantName": "투썸",
            "selected": true,
            "priority": 2
          },
          {
            "merchantId": 3,
            "merchantName": "폴바셋",
            "selected": true,
            "priority": 3
          }
        ]
      },
      {
        "categoryId": 2,
        "categoryCode": "CONVENIENCE_STORE",
        "categoryName": "편의점",
        "selected": true,
        "merchants": [
          {
            "merchantId": 4,
            "merchantName": "GS25",
            "selected": true,
            "priority": 1
          },
          {
            "merchantId": 5,
            "merchantName": "CU",
            "selected": true,
            "priority": 2
          },
          {
            "merchantId": 6,
            "merchantName": "이마트24",
            "selected": true,
            "priority": 3
          }
        ]
      },
      {
        "categoryId": 3,
        "categoryCode": "TRANSPORT",
        "categoryName": "교통",
        "selected": false,
        "merchants": []
      }
    ]
  }
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404)`

### 26. 개인화 설정 수정

```
PATCH /api/members/me/personalization
```

현재 로그인한 회원의 관심 소비 카테고리와 카테고리별 선호 가맹점 설정을 저장한다.

**화면** W_PersonalizationSetting · **라우트** /settings/personalization · **권한** USER · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 로그인한 회원만 수정할 수 있다.
- 현재 로그인한 회원의 개인화 설정만 수정할 수 있다.
- 요청한 카테고리 ID는 존재해야 한다.
- 요청한 가맹점 ID는 존재해야 한다.
- 선택한 가맹점은 해당 카테고리에 속해야 한다.
- 선택한 가맹점이 있다면 해당 카테고리는 선택된 것으로 간주한다.
- 선택한 카테고리별 가맹점은 최대 3개까지 허용한다.
- 같은 카테고리 안에서 동일한 가맹점을 중복 선택할 수 없다.
- 같은 카테고리 안에서 같은 우선순위를 중복 사용할 수 없다.
- 요청에 포함되지 않은 기존 개인화 설정은 삭제 또는 해제 처리한다.
- 전체 수정은 하나의 트랜잭션으로 처리한다.

**Request**

```json
{
  "categories": [
    {
      "categoryId": 1,
      "merchantIds": [1, 2, 3]
    },
    {
      "categoryId": 2,
      "merchantIds": [4, 5, 6]
    },
    {
      "categoryId": 3,
      "merchantIds": []
    }
  ]
}
```

**고유 에러**

`MERCHANT_INVALID(400)
MERCHANT_NOT_IN_CATEGORY(400)
MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "PREFERRED_MERCHANT_LIMIT_EXCEEDED",
  "message": "카테고리별 선호 가맹점은 최대 3개까지 선택할 수 있습니다.",
  "errors": []
}
```

### 27. 회원탈퇴

```
DELETE /api/members/me
```

현재 로그인한 회원을 탈퇴 처리한다.

**화면** W_Withdrawal · **라우트** /settings/withdrawal · **권한** USER · **담당** 재혁 이 · **상태 코드** 204 No Content

**Business Rules**

- 로그인한 회원만 탈퇴할 수 있다.
- 본인 계정만 탈퇴할 수 있다.
- 이미 탈퇴한 회원은 다시 탈퇴할 수 없다.
- 회원탈퇴 시 members.member_status를 WITHDRAWN으로 변경한다.
- 회원탈퇴 시 members.withdrawn_at에 탈퇴 일시를 저장한다.
- 탈퇴 사유가 입력된 경우 member_withdrawal에 저장한다.
- 탈퇴 성공 시 해당 회원의 기존 Refresh Token을 모두 폐기한다.
- 탈퇴 성공 후 현재 Access Token은 더 이상 유효한 사용자로 처리되지 않아야 한다.
- 탈퇴한 회원은 로그인할 수 없다.
- 탈퇴 회원의 보유 카드, 소비 내역, 포인트 내역 등은 실제 삭제하지 않고 보존할 수 있다.
- 화면에서는 탈퇴 회원의 개인 데이터를 노출하지 않도록 처리한다.

**Request**

```json
{
  "reasonCode": "INCONVENIENT",
  "reasonDetail": "사용 방법이 복잡해서 탈퇴합니다."
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404) MEMBER_ALREADY_WITHDRAWN(409)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBER_ALREADY_WITHDRAWN",
  "message": "이미 탈퇴한 회원입니다.",
  "errors": []
}
```

## 기타

### 28. 토큰 재발급

```
POST /api/auth/token
```

HttpOnly Cookie로 전달된 Refresh Token을 검증하고, 새로운 Access Token을 발급한다.

**권한** REFRESH_TOKEN · **담당** 재혁 이 · **상태 코드** 200 OK

**Business Rules**

- Refresh Token이 존재하고 유효해야 한다.
- 서버에 저장된 Refresh Token과 일치해야 한다.
- 폐기되었거나 만료된 Refresh Token은 사용할 수 없다.
- Rotation 적용 시 기존 Refresh Token은 즉시 폐기한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "accessToken": "new-access-token",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

**고유 에러**

`MEMBER_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "REFRESH_TOKEN_MISSING",
  "message": "인증 정보가 누락되었습니다. 다시 로그인해 주세요.",
  "errors": []
}
```

> 호출 시점: API 통신 중 Access Token 만료 에러 발생 시, 자동 호출
> • 성공 시 처리: 새 토큰을 발급받아 Pinia/로컬 스토리지에 저장 후, 실패했던 원래 API 요청을 재시도한다.
> • 실패 시 처리(RTT 미적용시): Refresh Token마저 만료되어 해당 API가 실패할 경우, 클라이언트 데이터를 초기화하고 로그인 화면으로 강제 리다이렉트 한다.

### 29. 결제 직전 최적 카드 추천

```
POST /api/recommendations
```

• 사용 목적: 가맹점(또는 카테고리)과 결제 예상금액을 받아, 보유 카드별 예상 혜택을 계산해 이득이 큰 순서로 정렬해 반환한다. 동적 전환(A 소진→B), 미래 최적화 경고, 결제 직전 포인트 안내(금융포인트 잔액 + 등록 멤버십 적립)를 함께 담는다.
• 주의사항:
◦ 가맹점은 merchantId로 받는다(결정). 프론트가 merchant 목록에서 뿌리므로 선택 시점에 id를 확정할 수 있다.
◦ expectedAmount는 5천원 단위 구간 대표값(중간값) 권장 — 이때 정률 혜택은 예상치(isEstimate=true)가 된다.
◦ 응답은 보유 카드 전부를 담는다(표시 개수는 프론트가 자름). expectedBenefit 동점 시 userCardId 오름차순 정렬 (테스트 재현성).
◦ expectedBenefit은 **카드당 혜택 1개만 적용한 값**이다(합산 아님). 한 결제에 여러 혜택이 매칭되면 혜택액이 가장 큰 1개만 적용하고, 동점이면 benefitId 오름차순.
◦ benefitKind: DISCOUNT(할인) / POINT(적립) / SPECIAL_PRICE(특가) / GIFT(증정) / RETROACTIVE(사후정산, 추천 계산 제외).

**화면** W_CardRecommend W_CardRecommendResult · **라우트** /cards/recommend /cards/recommend/result · **권한** USER · **담당** 현준 고 · **상태 코드** 200 OK

**Request**

```json
{ "merchantId": 205, "expectedAmount": 11900 }
```

**Response**

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

**고유 에러**

`NOT_FOUND(404) INPUT_INVALID(400)`

### 30. 보유 카드 전체 현황

```
GET /api/cards/monthly-status
```

• 사용 목적: 대시보드 홈, 보유 카드 목록 화면용. 로그인 사용자의 보유 카드 전부를 한 번에 반환한다. 홈 상단 브리핑(briefing)과 카드별 실적 요약·남은 혜택(benefitsSummary)을 담는다.
• 주의사항:
◦ 카드가 여러 장이라도 한 번의 호출로 처리한다 (개별 호출 반복 금지).
◦ 보유 카드가 0장이면 에러가 아니라 cards: [], briefing: null을 반환한다 (정상 상태).
◦ 3번(상세)과의 분리 근거: 목록에서 카드마다 전체 혜택 상세를 반복 전송하면 응답이 커지므로, 목록은 요약(benefitsSummary)만, 혜택 상세(benefits, 이용률 포함)는 3번 개별 호출로 나눈다.
◦ briefing은 실적 달성이 가장 임박한 카드 안내 — 판단·문구 모두 엔진 생성 (화면 표기는 "추천", "AI 브리핑" 라벨 지양).
◦ performanceMet: 현재 실적 충족 여부(bool). false면 전월실적 조건이 걸린 혜택은 이번 달 적용되지 않는다.
◦ sharedLimit은 `int|null`. **null = 통합한도가 없는 카드**(혜택별 개별한도만 적용), 0 = 혜택 없음. 둘을 뭉개면 안 된다.
◦ benefitsSummary[].remainingLimit은 묶음 한도에 속한 혜택이면 **그룹 기준 잔여액**이다 (31번의 limitGroupCode 참조).

**화면** W*CardList W_Main*홈 · **라우트** /home /cards · **담당** 현준 고 · **상태 코드** 200 OK

**Request**

쿼리 파라미터 `yearMonth`(선택, `YYYY-MM`). 생략하면 현재 연월.

**Response**

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

**고유 에러**

없음 (인증 실패는 공통 에러)

### 31. 보유 카드 상세 현황

```
GET /api/cards/{userCardId}/monthly-status
```

• 사용 목적: 카드 상세 화면용. 특정 보유 카드 한 장의 실적 현황과 **혜택별 이용 현황(잔여 한도 포함)**까지 상세 반환한다.
• 주의사항:
◦ 달성률·이용률·잔여 한도는 저장값이 아니라 계산값이다 (원본만 저장, 조회 시 계산).
◦ **limitGroupCode가 같은 혜택들은 한도를 공유한다.** monthlyLimit·usedAmount·remainingLimit·usageRate가 모두 같은 값으로 내려가므로, 화면에서 혜택마다 따로 더하면 한도가 실제보다 몇 배로 보인다. 같은 코드끼리 묶어 한 줄로 표시하거나 그룹 잔여액을 한 번만 노출한다. null이면 이 혜택 단독.
◦ performanceMet: 현재 실적 충족 여부(bool). sharedLimit은 `int|null` (null = 통합한도 없는 카드).

**화면** W_CardDetail · **라우트** /cards/:id · **담당** 현준 고 · **상태 코드** 200 OK

**Request**

쿼리 파라미터 `yearMonth`(선택, `YYYY-MM`). 생략하면 현재 연월.

**Response**

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
      }
    ]
  },
  "message": null
}
```

**고유 에러**

`NOT_FOUND(404)`

### 32. 결제 취소 상태 갱신

```
POST /api/settlements/cancel
```

- 사용 목적: 결제·취소 발생 시 카드의 실적·혜택 소진 상태를 갱신해 다음 추천에 반영되게 한다.
- 주의사항:
- 결제(가산)와 취소(차감)의 처리 경로가 다르다 — 아래 4-A / 4-B 참고.
- 놓친 혜택(최적 대비 차액)은 계산·저장하지 않는다. (범위 밖)

**담당** 현준 고 · **상태 코드** 200 OK

**Request**

```json
{ "expenseId": 3001 }
```

**Response**

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

**고유 에러**

`NOT_FOUND(404) ALREADY_CANCELED(409)`

### 33. 포인트 추천

```
GET /api/points/recommendations
```

• 사용 목적: 소비내역 + 포인트/멤버십 정보를 분석해 두 가지를 추천한다. 포인트 잔액 단순 조회가 아니라 소비 패턴 해석 → 추천 도출이다. 1. 금융포인트 사용처 추천(usage): 보유 금융포인트(잔액 조회 가능)를 소비 이력 기반으로 어디서 쓸지. 2. 미등록 멤버십 가입 권유(unregistered): 소비는 많은데 미등록인 멤버십사 (예: 파리바게트 소비 많은데 해피포인트 미등록 → 등록 권유).
• 주의사항:
◦ usage는 금융포인트(잔액 조회 O), unregistered는 멤버십(등록 여부 기반). 멤버십은 잔액 미연동이라 멤버십 사용처 추천은 없다.
◦ 구현 우선순위: 엔진 핵심(추천 #1 → 정산 #4 → 현황 #2·#3) 구현 이후 후순위로 진행한다.
🔽 엔드포인트

**화면** W_FinancialPointDetail · **라우트** /points · **담당** 현준 고 · **상태 코드** 200 OK

**Request**

없음

**Response**

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

**고유 에러**

`UNAUTHORIZED`

## Transaction

### 34. 소비내역 목록 조회

```
GET /api/transactions
```

회원의 소비내역 목록을 조회한다.
월별, 카테고리별, 보유카드별 조건으로 필터링할 수 있으며, 소비내역 화면에서 사용된다.

**화면** W_TransactionList · **라우트** /transactions · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 소비내역만 조회한다.
- 다른 회원의 소비내역은 조회할 수 없다.
- yearMonth가 전달되면 해당 월의 소비내역만 조회한다.
- categoryId가 전달되면 해당 카테고리의 소비내역만 조회한다.
- userCardId가 전달되면 해당 보유카드의 소비내역만 조회한다.
- paymentStatus가 전달되지 않으면 APPROVED 상태의 소비내역을 기본 조회한다.
- 소비내역은 결제일시 기준 최신순으로 정렬한다.
- 결제로 생성된 소비내역과 마이데이터 동기화로 수집된 소비내역을 함께 조회한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "소비내역 목록 조회에 성공했습니다.",
  "data": {
    "transactions": [
      {
        "expenseId": 1,
        "userCardId": 3,
        "cardName": "KB 청춘대로카드",
        "categoryId": 1,
        "categoryName": "카페",
        "merchantName": "스타벅스",
        "paymentAmount": 12000,
        "discountAmount": 1200,
        "paymentDate": "2026-07-18T13:20:00",
        "paymentStatus": "APPROVED",
        "inputType": "PAYMENT"
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) INPUT_INVALID(400) TRANSACTION_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "TRANSACTION_READ_FAILED",
  "message": "소비내역 목록 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 로그인한 회원의 소비내역 목록 조회

### 35. 소비내역 상세 조회

```
GET /api/transactions/{expenseId}
```

선택한 소비내역의 상세 정보를 조회한다.

소비내역 상세 화면에서 카드명, 카테고리, 가맹점명, 결제금액, 할인금액, 결제일시 등을 표시할 때 사용한다.

**화면** W_TransactionDetail · **라우트** /transactions/:id · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 소비내역만 조회할 수 있다.
- 존재하지 않는 소비내역 ID인 경우 오류를 반환한다.
- 다른 회원의 소비내역을 조회하려는 경우 접근을 제한한다.
- 취소된 소비내역도 상세 화면에서 상태 확인을 위해 조회할 수 있다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "소비내역 상세 조회에 성공했습니다.",
  "data": {
    "expenseId": 1,
    "userCardId": 3,
    "cardName": "KB 청춘대로카드",
    "categoryId": 1,
    "categoryName": "카페",
    "merchantId": 7,
    "merchantName": "스타벅스",
    "paymentAmount": 12000,
    "appliedBenefitId": 55,
    "appliedBenefitName": "카페 10% 청구할인",
    "discountAmount": 1200,
    "paymentType": "CARD",
    "interestFreeYn": "N",
    "paymentDate": "2026-07-18T13:20:00",
    "paymentStatus": "APPROVED",
    "inputType": "PAYMENT",
    "createdAt": "2026-07-18T13:20:10"
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) INPUT_INVALID(400) TRANSACTION_READ_FAILED(500) TRANSACTION_NOT_FOUND(404)`

**Error Response Format**

```json
{
  "success": false,
  "code": "TRANSACTION_NOT_FOUND",
  "message": "소비내역을 찾을 수 없습니다.",
  "errors": []
}
```

> 특정 소비내역 상세 조회

### 36. 마이데이터 거래 동기화

```
POST /api/transactions/sync
```

카드사에서 최신 거래 내역을 다시 받아 소비내역에 반영한다. 소비내역 화면에서 당겨서 새로고침할 때 호출한다.

**화면** W_TransactionList · **라우트** /transactions · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 보유카드 거래만 동기화한다.
- 이미 저장된 거래는 다시 만들지 않는다(중복 방지).
- **새로 수집된 거래는 엔진의 혜택 계산을 호출해 appliedBenefitId·discountAmount를 채우고, 같은 트랜잭션에서 실적·한도 상태를 가산한다.**
- **카드사에서 취소로 확인된 거래는 paymentStatus를 CANCELED로 바꾸고, 엔진의 상태 갱신을 호출해 실적·한도를 차감한다.** 취소는 사용자가 앱에서 지우는 것이 아니라 동기화로 감지된 사실이다.
- 취소 반영은 당월 건만 한다. 전월 이후 건은 이미 확정된 실적이라 소급 재계산하지 않는다.
- 동기화 중 일부 거래가 실패해도 나머지는 반영한다. 실패 건수를 응답에 담는다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "거래 동기화가 완료되었습니다.",
  "data": {
    "syncedAt": "2026-07-21T16:40:00",
    "addedCount": 3,
    "canceledCount": 1,
    "failedCount": 0
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) MYDATA_SYNC_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MYDATA_SYNC_FAILED",
  "message": "거래 동기화 중 오류가 발생했습니다.",
  "errors": []
}
```

> 카드사 최신 거래 수집 및 취소 반영

### 37. 소비카테고리 목록 조회

```
GET /api/expense-categories
```

소비내역 등록, 소비내역 필터, 결제 추천 입력 화면에서 사용할 소비카테고리 목록을 조회한다.

**화면** W_TransactionList, W_CardRecommend · **라우트** /transactions, /cards/recommend · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원만 조회할 수 있다.
- 사용 가능한 소비카테고리만 조회한다.
- 카테고리코드는 카드 혜택 계산, 소비내역 분류, 포인트 사용처 매칭에 공통으로 사용한다.
- 카테고리 목록은 화면 표시 순서 또는 카테고리번호 기준으로 정렬한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "소비카테고리 목록 조회에 성공했습니다.",
  "data": {
    "categories": [
      {
        "categoryId": 1,
        "categoryName": "카페",
        "categoryCode": "CAFE"
      },
      {
        "categoryId": 2,
        "categoryName": "편의점",
        "categoryCode": "CONVENIENCE_STORE"
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHROIZED(401) CATEGORY_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "CATEGORY_READ_FAILED",
  "message": "소비카테고리 목록 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 소비내역 등록/필터/추천 입력에서 사용하는 카테고리 목록 조회

### 38. 결제 처리

```
POST /api/payments
```

결제를 처리한다. 시연 환경에서는 실제 PG 연동 없이 처리하며, 그 구분은 paymentChannel 값으로 남긴다(MOCK | PG). 실서비스 전환 시 이 값만 바뀌고 스키마·경로는 그대로다.

결제 성공 시 소비내역을 자동 생성하고, 포인트 적립 내역을 생성하며, 포인트지갑 잔액을 갱신한다.

**화면** W_Payment · **라우트** /payment · **권한** USER · **담당** 허강상 · **상태 코드** 201 Created

**Business Rules**

- 로그인한 회원만 결제를 요청할 수 있다.
- 보유카드는 로그인한 회원의 카드여야 한다.
- 결제금액은 1원 이상이어야 한다.
- 추천 결과로 결제한 경우 isRecommendBased를 Y로 저장한다. 추천 결과 자체는 저장하지 않으므로 참조할 추천번호는 없다.
- 결제 성공 시 결제상태는 SUCCESS로 저장한다.
- 결제 성공 시 소비내역이 자동 생성된다.
- 생성된 소비내역의 입력구분은 PAYMENT로 저장한다.
- **결제 처리 트랜잭션 안에서 엔진의 혜택 계산을 호출한다.** 적용 혜택 1개와 할인액을 받아 소비내역의 appliedBenefitId·discountAmount에 저장한다.
- **같은 트랜잭션에서 엔진의 상태 갱신을 호출한다.** 당월 누적 실적, 통합한도 사용액, 혜택별 소진액·적용횟수를 가산한다. 이 갱신이 있어야 다음 추천이 바뀐다(한도 소진 시 다른 카드로 전환).
- merchantId는 등록된 가맹점이면 채운다. 없으면 카테고리 혜택만 매칭된다.
- **paymentType·interestFreeYn을 함께 저장한다.** 카드 약관은 특정 결제수단과 무이자할부를 전월실적에서 제외하는 것이 기본이라, 이 두 값이 없으면 실적이 조용히 과다 계산된다.
- 포인트 적립 대상인 경우 포인트내역을 생성한다.
- 포인트 적립 후 포인트지갑의 총보유포인트를 갱신한다.
- 결제 실패 시 소비내역·포인트내역을 생성하지 않고 엔진 상태도 갱신하지 않는다.

**Request**

```json
{
  "userCardId": 3,
  "isRecommendBased": "Y",
  "categoryId": 1,
  "merchantId": 7,
  "merchantName": "스타벅스",
  "paymentAmount": 12000,
  "paymentType": "CARD",
  "interestFreeYn": "N"
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "결제가 완료되었습니다.",
  "data": {
    "paymentId": 1,
    "expenseId": 15,
    "paymentStatus": "SUCCESS",
    "paymentChannel": "MOCK",
    "appliedBenefitId": 55,
    "appliedBenefitName": "카페 10% 청구할인",
    "discountAmount": 1200,
    "savedPoint": 120,
    "totalPoint": 63620,
    "completedAt": "2026-07-18T13:30:00"
  }
}
```

**고유 에러**

`INPUT_INVALID(400) INVALID_PAYMENT_AMOUNT(400) USER_CARD_ACCESS_DENIED(403) CATEGORY_NOT_FOUND(404) PAYMENT_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "PAYMENT_FAILED",
  "message": "결제 처리 중 오류가 발생했습니다.",
  "errors": []
}
```

> 결제 처리 (시연 환경은 paymentChannel=MOCK)

### 39. 결제 결과 조회

```
GET /api/payments/{paymentId}
```

결제 완료 후 결제 결과, 생성된 소비내역, 적립 포인트 정보를 조회한다.

**화면** W_Payment · **라우트** /payment · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 결제 결과만 조회할 수 있다.
- 다른 회원의 결제 결과는 조회할 수 없다.
- 존재하지 않는 결제 ID인 경우 오류를 반환한다.
- 결제 성공 시 생성된 소비내역 ID를 함께 응답한다.
- 포인트가 적립된 경우 적립 포인트와 결제 후 총 포인트를 함께 응답한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "결제 결과 조회에 성공했습니다.",
  "data": {
    "paymentId": 1,
    "expenseId": 15,
    "cardName": "KB 청춘대로카드",
    "merchantName": "스타벅스",
    "paymentAmount": 12000,
    "paymentStatus": "SUCCESS",
    "savedPoint": 120,
    "totalPoint": 63620,
    "completedAt": "2026-07-18T13:30:00"
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) MOCK_+PYAMENT_NOT_FOUND(404) MOCK_PAYMENT_ACCESS_DENIED(403) MOCK_PAYMNENT_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MOCK_PAYMENT_NOT_FOUND",
  "message": "결제 정보를 찾을 수 없습니다.",
  "errors": []
}
```

> 결제 완료 결과 조회

## Point/Membership

### 40. 포인트 목록 조회

```
GET /api/points
```

회원이 보유한 포인트 목록을 조회한다.

금융 포인트와 멤버십 포인트를 함께 조회하며 포인트 목록 화면에서 사용한다.

**화면** W_FinancialPointDetail · **라우트** /points · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 포인트지갑만 조회한다.
- 사용여부가 Y인 포인트사만 조회한다.
- 포인트사별 총보유포인트를 응답한다. 포인트 소멸은 범위 밖이다.
- 금융 포인트와 멤버십 포인트는 providerType으로 구분한다.
- 포인트 목록은 총보유포인트가 높은 순으로 정렬한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "포인트 목록 조회에 성공했습니다.",
  "data": {
    "points": [
      {
        "pointWalletId": 1,
        "pointProviderId": 1,
        "providerName": "포인트리",
        "providerType": "FINANCIAL_POINT",
        "logoImage": "/images/pointree.png",
        "totalPoint": 63500
      },
      {
        "pointWalletId": 2,
        "pointProviderId": 2,
        "providerName": "CJ ONE",
        "providerType": "MEMBERSHIP",
        "logoImage": "/images/cjone.png",
        "totalPoint": 1500
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) POINT_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "POINT_READ_FAILED",
  "message": "포인트 목록 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 회원이 보유한 포인트 목록 조회

### 41. 포인트 내역 조회

```
GET /api/points/history
```

회원의 포인트 적립·사용 내역을 조회한다.

포인트지갑별, 포인트유형별, 기준연월별로 필터링할 수 있다.

**화면** W_PointList · **라우트** /points/history · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 포인트내역만 조회한다.
- pointWalletId가 전달되면 해당 포인트지갑의 내역만 조회한다.
- pointType이 전달되면 유형(SAVE, USE)별로 조회한다. 포인트 소멸·조정은 범위 밖이다.
- yearMonth가 전달되면 해당 월의 포인트내역만 조회한다.
- 포인트내역은 발생일시 기준 최신순으로 정렬한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "포인트 내역 조회에 성공했습니다.",
  "data": {
    "histories": [
      {
        "pointHistoryId": 1,
        "pointWalletId": 2,
        "providerName": "CJ ONE",
        "pointType": "SAVE",
        "pointAmount": 120,
        "content": "스타벅스 결제 포인트 적립",
        "occurredAt": "2026-07-18T13:30:00"
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) INPUT_INVALID(400) POINT_HISTORY_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "POINT_HISTORY_READ_FAILED",
  "message": "포인트 내역 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 적립·사용 포인트 내역 조회

### 42. 포인트 사용처 조회

```
GET /api/points/{pointProviderId}/usage-places
```

특정 포인트사 또는 멤버십 포인트를 사용할 수 있는 제휴처 목록을 조회한다.

**화면** W_FinancialPoinDetail, W_MembershipDetail · **라우트** /points, /memberships/:id · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원만 조회할 수 있다.
- 존재하는 포인트사 ID만 조회할 수 있다.
- 사용가능여부가 Y인 사용처를 기본 조회한다.
- categoryId가 전달되면 해당 카테고리의 사용처만 조회한다.
- 포인트사별 사용처는 포인트 상세 화면과 멤버십 상세 화면에서 함께 사용할 수 있다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "포인트 사용처 조회에 성공했습니다.",
  "data": {
    "usagePlaces": [
      {
        "usagePlaceId": 1,
        "pointProviderId": 2,
        "providerName": "CJ ONE",
        "placeName": "올리브영",
        "categoryId": 5,
        "categoryName": "쇼핑",
        "useYn": "Y",
        "description": "CJ ONE 포인트 사용 가능"
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) POINT_PROVIDER_NOT_FOUND(404) INPUT_INVALID(400) POINT_USAGE_PLACE_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "POINT_USAGE_PLACE_READ_FAILED",
  "message": "포인트 사용처 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 포인트사별 사용 가능한 제휴처 조회

### 43. 멤버십 등록 가능 목록 및 기본 추천 조회

```
GET /api/memberships/providers
```

멤버십 등록 화면에서 사용자가 등록할 수 있는 멤버십 포인트사 목록을 조회한다.

**화면** W_MembershipRegister · **라우트** /memberships/register · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원만 조회할 수 있다.
- 포인트구분이 MEMBERSHIP인 포인트사만 조회한다.
- 사용여부가 Y인 포인트사만 조회한다.
- 이미 등록된 멤버십은 isRegistered 값으로 구분한다.
- 등록 해제된 멤버십은 재등록 가능 상태로 응답할 수 있다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "멤버십 등록 가능 목록 조회에 성공했습니다.",
  "data": {
    "providers": [
      {
        "pointProviderId": 1,
        "providerName": "CJ ONE",
        "providerType": "MEMBERSHIP",
        "logoImageUrl": "/images/cjone.png",
        "isRegistered": false,
        "defaultRecommendYn": "Y",
        "recommendPriority": 1,
        "recommendMessage": "쇼핑, 문화, 외식 사용처가 많아 처음 등록하기 좋은 멤버십입니다."
      },
      {
        "pointProviderId": 2,
        "providerName": "HappyPoint",
        "providerType": "MEMBERSHIP",
        "logoImageUrl": "/images/happypoint.png",
        "isRegistered": false,
        "defaultRecommendYn": "Y",
        "recommendPriority": 2,
        "recommendMessage": "카페와 디저트 사용처가 많아 자주 쓰기 좋은 멤버십입니다."
      },
      {
        "pointProviderId": 3,
        "providerName": "L.POINT",
        "providerType": "MEMBERSHIP",
        "logoImageUrl": "/images/lpoint.png",
        "isRegistered": false,
        "defaultRecommendYn": "Y",
        "recommendPriority": 3,
        "recommendMessage": "마트와 쇼핑 사용처가 많아 생활 소비에 유용합니다."
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) MEMBERSHIP_PROIVDER_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBERSHIP_PROVIDER_READ_FAILED",
  "message": "멤버십 등록 가능 목록 조회 중 오류가 발생했습니다.",
  "errors": []
}
```

> 등록 가능한 멤버십 포인트사 목록 조회

### 44. 멤버십 등록

```
POST /api/memberships
```

회원이 멤버십 포인트를 등록한다.

멤버십 등록 화면에서 CJ ONE, HappyPoint 등 포인트사를 선택하여 등록할 때 사용한다.

**화면** W_MembershipRegister · **라우트** /memberships/register · **권한** USER · **담당** 허강상 · **상태 코드** 201 Created

**Business Rules**

- 로그인한 회원만 멤버십을 등록할 수 있다.
- 포인트구분이 MEMBERSHIP인 포인트사만 멤버십으로 등록할 수 있다.
- 이미 등록된 멤버십은 중복 등록할 수 없다.
- 등록상태가 CANCELED인 멤버십은 REGISTERED로 재등록할 수 있다.
- 멤버십 등록 시 포인트지갑이 없으면 함께 생성할 수 있다.
- 등록상태는 REGISTERED로 저장한다.

**Request**

```json
{
  "pointProviderId": 2
}
```

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "멤버십 등록에 성공했습니다.",
  "data": {
    "membershipRegisterId": 1,
    "pointProviderId": 2,
    "providerName": "CJ ONE",
    "registerStatus": "REGISTERED",
    "registeredAt": "2026-07-18T14:00:00"
  }
}
```

**고유 에러**

`INPUT_INVALID(400) POINT_PROVIDER_NOT_FOUND(404) NOT_MEMBERSHIP_PROVIDER(400) MEMBERSHIP_ALREADY_REGISTERED(409) MEMBERSHIP_REGISTER_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBERSHIP_ALREADY_REGISTERED",
  "message": "이미 등록된 멤버십입니다.",
  "errors": []
}
```

> 멤버십 포인트사 등록

### 45. 멤버십 등록 해제

```
DELETE /api/memberships/{membershipRegisterId}
```

회원의 멤버십 등록을 해제한다.

실제 데이터를 삭제하지 않고 등록상태를 CANCELED로 변경한다.

**화면** W_MembershipDetail · **라우트** /memberships/:id · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 멤버십만 해제할 수 있다.
- 존재하지 않는 멤버십인 경우 오류를 반환한다.
- 다른 회원의 멤버십은 해제할 수 없다.
- 이미 해제된 멤버십은 중복 해제할 수 없다.
- 등록 해제 시 REGISTER_STATUS는 CANCELED로 변경한다.
- CANCELED_AT에 해제 일시를 저장한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "멤버십 등록 해제에 성공했습니다.",
  "data": {
    "membershipRegisterId": 1,
    "registerStatus": "CANCELED",
    "canceledAt": "2026-07-18T14:00:00"
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) MEMBERSHIP_ACCESS_DENIED(403) MEMBERSHIP_NOT_FOUND(404) MEMBERSHIP_ALREADY_CANCELED(409) MEMBERSHIP_CANCEL_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBERSHIP_NOT_FOUND",
  "message": "멤버십 정보를 찾을 수 없습니다.",
  "errors": []
}
```

> 멤버십 상태를 CANCELED로 변경

### 46. 멤버십 상세 조회

```
GET /api/memberships/{membershipRegisterId}
```

회원이 등록한 멤버십의 상세 정보, 보유 포인트, 사용처 정보를 조회한다.

**화면** W_MembershipDetail · **라우트** /memberships/:id · **권한** USER · **담당** 허강상 · **상태 코드** 200 OK

**Business Rules**

- 로그인한 회원의 멤버십만 조회할 수 있다.
- 다른 회원의 멤버십은 조회할 수 없다.
- 존재하지 않는 멤버십 ID인 경우 오류를 반환한다.
- 등록상태, 포인트 잔액, 사용처 정보를 함께 응답한다.

**Request**

없음

**Response**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "멤버십 상세 조회에 성공했습니다.",
  "data": {
    "membershipRegisterId": 1,
    "pointProviderId": 2,
    "providerName": "CJ ONE",
    "logoImage": "/images/cjone.png",
    "registerStatus": "REGISTERED",
    "totalPoint": 1500,
    "registeredAt": "2026-07-18T14:00:00",
    "usagePlaces": [
      {
        "usagePlaceId": 1,
        "placeName": "올리브영",
        "categoryName": "쇼핑",
        "useYn": "Y",
        "description": "CJ ONE 포인트 사용 가능"
      }
    ]
  }
}
```

**고유 에러**

`UNAUTHORIZED(401) MEMBERSHIP_ACCESS_DENIED(403) MEMBERSHIP_NOT_FOUND(404) MEMBERSHIP_READ_FAILED(500)`

**Error Response Format**

```json
{
  "success": false,
  "code": "MEMBERSHIP_NOT_FOUND",
  "message": "멤버십 정보를 찾을 수 없습니다.",
  "errors": []
}
```

> 등록된 멤버십 상세 조회
