-- ============================================================
-- schema.sql — 카드 혜택 최적화 전자지갑 전체 스키마 (MySQL 8.0)
-- ------------------------------------------------------------
-- 통합 ERD와 1:1로 대응한다. 이 파일이 스키마의 원본이다.
-- 실행: mysql -uroot -p wallet < schema.sql
-- 시드 데이터는 이 파일에 넣지 않는다 (90_seed_*.sql 참조).
--   구조와 내용을 분리해야 "데이터만 갈아끼우기"가 가능하다.
--
-- 테이블 생성 순서 = FK 의존 순서. 임의로 옮기면 실행이 깨진다.
--
-- 공통 규약
--   · ID는 BIGINT AUTO_INCREMENT (타입 불일치로 인한 FK 오류 방지)
--   · 금액은 BIGINT (원 단위 정수). API 계약이 "원 미만 절사"라 타입이 계약을 강제한다
--   · 비율·혜택값만 DECIMAL(10,2) — RATE에 1.5% 같은 소수가 실제로 존재
--   · 카드·혜택·소비 도메인의 enum 성격 컬럼은 VARCHAR + COMMENT
--     (MySQL ENUM은 값 추가 시 ALTER가 필요하고 Java enum과 이중 관리가 된다)
--   · 회원·알림 도메인은 MySQL ENUM을 쓴다. 도메인 간 컬럼이 서로 조인·비교되지
--     않으므로 스타일 차이가 실제 동작에 영향을 주지 않는다
--   · Y/N 플래그는 CHAR(1), 회원 도메인의 boolean은 TINYINT(1)
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────────────────────
-- ⚠ 초기화 — 아래 DROP은 기존 테이블과 데이터를 전부 지운다.
--   스키마를 반복 적용하기 위한 것이다. 개발 환경 전용이며 운영에서는 쓰지 않는다.
--
-- FOREIGN_KEY_CHECKS를 잠시 끄는 이유:
--   FK가 걸린 테이블은 참조하는 쪽을 먼저 지워야 해서, 켜둔 채로는 삭제 순서를
--   의존 관계의 역순으로 정확히 맞춰야 한다. 테이블이 하나 늘 때마다 순서를 다시
--   따져야 하고 틀리면 실행이 깨진다. 검사를 끄면 순서와 무관하게 삭제된다.
--   (CREATE는 순서가 그대로 중요하다 — 참조 대상이 먼저 있어야 하므로)
-- ────────────────────────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification_setting;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS member_preferred_merchant;
DROP TABLE IF EXISTS member_preferred_category;
DROP TABLE IF EXISTS membership_register;
DROP TABLE IF EXISTS point_usage_place;
DROP TABLE IF EXISTS point_history;
DROP TABLE IF EXISTS point_wallet;
DROP TABLE IF EXISTS point_provider;
DROP TABLE IF EXISTS user_benefit_usage;
DROP TABLE IF EXISTS user_card_monthly_state;
DROP TABLE IF EXISTS recommend_input;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS expense;
DROP TABLE IF EXISTS benefit_exclusion;
DROP TABLE IF EXISTS benefit_tier_limit;
DROP TABLE IF EXISTS benefit;
DROP TABLE IF EXISTS performance_exclusion;
DROP TABLE IF EXISTS performance_tier;
DROP TABLE IF EXISTS user_card;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS card;
DROP TABLE IF EXISTS member_term_agreement;
DROP TABLE IF EXISTS term_version;
DROP TABLE IF EXISTS term;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS password_reset_verification;
DROP TABLE IF EXISTS member_withdrawal;
DROP TABLE IF EXISTS member;

SET FOREIGN_KEY_CHECKS = 1;

-- ════════════════════════════════════════════════════════════
-- 1. 회원 · 인증
-- ════════════════════════════════════════════════════════════

CREATE TABLE member (
    member_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    email         VARCHAR(255) NOT NULL COMMENT '이메일(로그인 ID)',
    password_hash VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    name          VARCHAR(50)  NOT NULL COMMENT '회원명',
    member_status ENUM('ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    withdrawn_at  DATETIME     NULL COMMENT '탈퇴일시. 상태가 WITHDRAWN이면 필수',
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원';

-- 탈퇴 사유는 회원 삭제 후에도 남겨야 하므로 물리 FK를 걸지 않는다 (논리 참조).
CREATE TABLE member_withdrawal (
    member_withdrawal_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '탈퇴 이력 ID',
    member_id            BIGINT       NOT NULL COMMENT '회원 ID (논리 참조)',
    reason_type          VARCHAR(50)  NULL COMMENT '탈퇴 사유 유형',
    reason_detail        VARCHAR(500) NULL COMMENT '탈퇴 사유 상세',
    withdrawn_at         DATETIME     NOT NULL COMMENT '탈퇴일시',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_withdrawal_id),
    UNIQUE KEY uk_member_withdrawal_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 탈퇴 이력';

-- reset_token_hash와 reset_token_expires_at은 둘 다 NULL이거나 둘 다 값이 있어야 한다.
CREATE TABLE password_reset_verification (
    password_reset_verification_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '비밀번호 재설정 인증 ID',
    member_id                      BIGINT       NOT NULL COMMENT '회원 ID',
    verification_code_hash         VARCHAR(255) NOT NULL COMMENT '인증코드 해시',
    verification_status            ENUM('PENDING','VERIFIED','USED','EXPIRED') NOT NULL DEFAULT 'PENDING' COMMENT '인증 상태',
    failed_attempt_count           INT          NOT NULL DEFAULT 0 COMMENT '실패 횟수',
    verification_code_expires_at   DATETIME     NOT NULL COMMENT '인증코드 만료일시',
    reset_token_hash               VARCHAR(255) NULL COMMENT '재설정 토큰 해시',
    reset_token_expires_at         DATETIME     NULL COMMENT '재설정 토큰 만료일시',
    verified_at                    DATETIME     NULL COMMENT '인증 완료일시',
    used_at                        DATETIME     NULL COMMENT '사용일시',
    created_at                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (password_reset_verification_id),
    UNIQUE KEY uk_password_reset_token (reset_token_hash),
    KEY idx_password_reset_member_status (member_id, verification_status),
    KEY idx_password_reset_code_expiry (verification_code_expires_at),
    CONSTRAINT fk_password_reset_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '비밀번호 재설정 인증';

-- 원문은 저장하지 않는다. revoked_at과 revoke_reason은 함께 NULL이거나 함께 값이 있어야 한다.
CREATE TABLE refresh_token (
    refresh_token_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Refresh Token ID',
    member_id        BIGINT       NOT NULL COMMENT '회원 ID',
    token_hash       VARCHAR(255) NOT NULL COMMENT '토큰 단방향 해시',
    expires_at       DATETIME     NOT NULL COMMENT '만료일시',
    revoked_at       DATETIME     NULL COMMENT '폐기일시. NULL이면 유효',
    revoke_reason    ENUM('REISSUED', 'LOGOUT','PASSWORD_CHANGED','PASSWORD_RESET','MEMBER_WITHDRAWN') NULL COMMENT '폐기 사유',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일시',
    PRIMARY KEY (refresh_token_id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_token_member_validity (member_id, revoked_at, expires_at),
    CONSTRAINT fk_refresh_token_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT 'Refresh Token';

-- ════════════════════════════════════════════════════════════
-- 2. 약관 (서비스 이용약관 — 카드 약관이 아니다)
-- ════════════════════════════════════════════════════════════

CREATE TABLE term (
    term_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '약관 ID',
    term_code   VARCHAR(50)  NOT NULL COMMENT '약관 코드',
    term_name   VARCHAR(100) NOT NULL COMMENT '약관명',
    is_required TINYINT(1)   NOT NULL COMMENT '필수 동의 여부',
    term_status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '약관 상태',
    PRIMARY KEY (term_id),
    UNIQUE KEY uk_term_code (term_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '약관';

CREATE TABLE term_version (
    term_version_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '약관 버전 ID',
    term_id              BIGINT      NOT NULL COMMENT '약관 ID',
    version              VARCHAR(20) NOT NULL COMMENT '버전',
    content              LONGTEXT    NOT NULL COMMENT '약관 전문',
    effective_started_at DATETIME    NOT NULL COMMENT '시행 시작일시',
    effective_ended_at   DATETIME    NULL COMMENT '시행 종료일시. NULL이면 현재 유효',
    PRIMARY KEY (term_version_id),
    UNIQUE KEY uk_term_version (term_id, version),
    CONSTRAINT fk_term_version_term FOREIGN KEY (term_id) REFERENCES term (term_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '약관 버전';

CREATE TABLE member_term_agreement (
    member_term_agreement_id BIGINT     NOT NULL AUTO_INCREMENT COMMENT '약관 동의 ID',
    member_id                BIGINT     NOT NULL COMMENT '회원 ID',
    term_version_id          BIGINT     NOT NULL COMMENT '약관 버전 ID',
    is_agreed                TINYINT(1) NOT NULL COMMENT '동의 여부',
    agreed_at                DATETIME   NULL COMMENT '동의일시. is_agreed=1이면 필수',
    PRIMARY KEY (member_term_agreement_id),
    UNIQUE KEY uk_member_term_agreement (member_id, term_version_id),
    CONSTRAINT fk_member_term_agreement_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_member_term_agreement_version FOREIGN KEY (term_version_id) REFERENCES term_version (term_version_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 약관 동의';

-- ════════════════════════════════════════════════════════════
-- 3. 카드 마스터
--    카드 혜택을 데이터로 표현하고 계산 엔진 하나가 해석한다.
--    카드별 테이블 분리 금지 — 새 카드는 행 추가로 끝난다.
-- ════════════════════════════════════════════════════════════

CREATE TABLE card (
    card_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    card_name   VARCHAR(100) NOT NULL COMMENT '카드명 (예: 나라사랑카드)',
    issuer      VARCHAR(50)  NOT NULL COMMENT '카드사 (예: KB국민, 현대)',
    card_type   VARCHAR(20)  NOT NULL COMMENT '카드 종류: CREDIT(신용) | CHECK(체크)',
    annual_fee  INT          NOT NULL DEFAULT 0 COMMENT '연회비(원). 체크카드는 0',
    image_url   VARCHAR(255) NULL COMMENT '카드 이미지 URL',
    description VARCHAR(500) NULL COMMENT '카드 한줄 소개',
    is_active   CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '판매중 여부: Y | N',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 마스터';

-- 카테고리 표준 (대분류 6 / 중분류 24, 계층 깊이 2단계 고정).
-- 계층을 두는 이유: 카드사가 대분류 단위로 혜택을 거는 경우가 실제로 있다.
-- 혜택이 대분류를 겨냥하면 엔진이 하위 중분류 결제까지 매칭한다.
-- 깊이를 2로 고정했으므로 혜택 조회는 조인 한 번으로 끝난다
--   WHERE target_category_id IN (결제카테고리, 그_부모)
CREATE TABLE category (
    category_id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    category_code      VARCHAR(30) NOT NULL COMMENT '카테고리 코드 (예: CAFE, PUBLIC_TRANSPORT)',
    category_name      VARCHAR(50) NOT NULL COMMENT '카테고리명 (예: 카페)',
    parent_category_id BIGINT      NULL COMMENT '상위 카테고리 ID. 최상위면 NULL',
    display_order      INT         NOT NULL DEFAULT 0 COMMENT '화면 노출 순서',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_code (category_code),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) REFERENCES category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카테고리 표준';

-- 혜택이 걸린 브랜드 단위 가맹점. 지점·상품 단위는 범위 밖.
-- category_id가 가맹점 → 카테고리 혜택으로 올라가는 경로다.
CREATE TABLE merchant (
    merchant_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '가맹점(브랜드) ID',
    merchant_code VARCHAR(30)  NOT NULL COMMENT '가맹점 코드 (예: STARBUCKS). 제외 규칙에서 이 값으로 참조',
    merchant_name VARCHAR(100) NOT NULL COMMENT '가맹점명 (예: 스타벅스)',
    category_id   BIGINT       NOT NULL COMMENT '소속 카테고리 ID',
    PRIMARY KEY (merchant_id),
    UNIQUE KEY uk_merchant_code (merchant_code),
    KEY idx_merchant_category (category_id),
    CONSTRAINT fk_merchant_category FOREIGN KEY (category_id) REFERENCES category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '가맹점(브랜드)';

-- ════════════════════════════════════════════════════════════
-- 4. 보유카드
--    엔진의 상태 테이블은 전부 user_card_id 기준이다 (card_id 아님).
-- ════════════════════════════════════════════════════════════

-- 대표 카드는 회원당 최대 1개. 생성 컬럼 + UNIQUE로 DB가 강제한다.
-- (활성 대표 카드일 때만 member_id가 채워지고, 그 컬럼에 UNIQUE가 걸려 있다)
CREATE TABLE user_card (
    user_card_id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '보유카드 ID',
    member_id                BIGINT      NOT NULL COMMENT '회원 ID',
    card_id                  BIGINT      NOT NULL COMMENT '카드 ID',
    masked_card_number       VARCHAR(30) NOT NULL COMMENT '마스킹된 카드번호',
    is_representative        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '대표카드 여부',
    registered_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    card_status              ENUM('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '카드 상태',
    representative_member_id BIGINT      GENERATED ALWAYS AS (
        CASE WHEN is_representative = 1 AND card_status = 'ACTIVE' THEN member_id ELSE NULL END
    ) STORED COMMENT '대표카드 유일성 보장용 생성 컬럼',
    PRIMARY KEY (user_card_id),
    UNIQUE KEY uk_user_card_member_card (member_id, card_id),
    UNIQUE KEY uk_user_card_representative (representative_member_id),
    KEY idx_user_card_member_status (member_id, card_status),
    CONSTRAINT fk_user_card_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_user_card_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '보유카드';

-- ════════════════════════════════════════════════════════════
-- 5. 실적
-- ════════════════════════════════════════════════════════════

-- 카드 약관의 "전월 30만원 이상 이용 시 월 1만원 한도"를 데이터로 표현한다.
-- if문의 조건(임계값)이 컬럼, 결과(한도)가 값, 분기가 행이다.
--
-- 판정 규칙: min_performance_amount <= 전월실적 인 행 중 min_performance_amount 최댓값.
-- 규칙 — 모든 카드는 min_performance_amount = 0 행을 반드시 1개 갖는다.
--   그래야 판정이 항상 행 하나를 반환하고 엔진에 "구간 없음" NULL 분기가 생기지 않는다.
--   실적 미달 시 혜택이 없는 카드라면 0원 구간의 shared_monthly_limit을 0으로 넣는다.
CREATE TABLE performance_tier (
    tier_id                BIGINT NOT NULL AUTO_INCREMENT COMMENT '실적구간 ID',
    card_id                BIGINT NOT NULL COMMENT '카드 ID',
    min_performance_amount BIGINT NOT NULL COMMENT '이 구간의 최소 전월실적(원). 조건 없으면 0',
    shared_monthly_limit   BIGINT NULL COMMENT '통합할인한도(월, 원). NULL = 통합한도 없음(개별한도만 적용), 0 = 혜택 없음',
    PRIMARY KEY (tier_id),
    UNIQUE KEY uk_performance_tier (card_id, min_performance_amount),
    CONSTRAINT fk_performance_tier_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 통합할인한도';

-- 제외값이 대분류 코드면 하위 중분류 결제까지 제외한다. 혜택 대상(target_category_id)이
-- 대분류면 하위까지 적용되는 것과 대칭이다. 한쪽만 상향 매칭하면 같은 계층을 두 규칙이
-- 다르게 해석하게 된다.
--
-- 카드사가 전월실적 값을 주지 않으므로 엔진이 직접 계산한다.
--   전월실적 = 지난달 소비내역 합계 − 여기 걸린 제외 항목
--
--   exclusion_type      exclusion_value 예시    의미
--   CATEGORY            'PUBLIC_TRANSPORT'      해당 카테고리 결제 제외
--   PAYMENT_TYPE        'SIMPLE_PAY'            해당 결제수단 제외
--   TRANSACTION_ATTR    'INTEREST_FREE'         무이자할부 건 제외
--                       'DISCOUNTED'            이미 할인받은 건 제외
--                       'OVERSEAS'              해외 이용분 제외
--   MIN_TXN_AMOUNT      '10000'                 건당 1만원 미만 제외
--
-- 값에 FK를 못 거는 게 이 설계의 비용이다. 그래서 id 대신 코드 문자열을 쓴다 —
-- 어차피 무결성 이점이 없으니 시드 가독성을 택했다('301'보다 'PUBLIC_TRANSPORT').
CREATE TABLE performance_exclusion (
    card_id         BIGINT      NOT NULL COMMENT '카드 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | PAYMENT_TYPE | TRANSACTION_ATTR | MIN_TXN_AMOUNT',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값. 유형에 따라 해석이 달라진다',
    PRIMARY KEY (card_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_performance_exclusion_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '전월실적 제외 항목';

-- ════════════════════════════════════════════════════════════
-- 6. 혜택
--
-- ★ 엔진 규칙 — 한 결제에 적용되는 혜택은 카드당 1개다.
--   가맹점 혜택과 카테고리 혜택을 둘 다 조회하므로 여러 혜택이 매칭될 수 있다.
--   이때 합산하지 않고 혜택액이 가장 큰 1개만 적용한다. 동점이면 benefit_id 오름차순.
--   근거: ①실제 약관은 "타 할인과 중복 불가"가 기본 ②expense.applied_benefit_id가
--   단수라 이미 이 전제 ③계단식 정액할인도 자동으로 맞게 동작한다.
-- ════════════════════════════════════════════════════════════

-- 컬럼을 4묶음으로 읽는다.
--   [무엇을] target_type / target_category_id / target_merchant_id
--   [얼마나] calc_method / benefit_value
--   [조건]   require_performance / require_payment_type / min_txn_amount
--   [상한]   max_eligible_amount / max_benefit_per_txn / monthly_limit
--            / limit_group_code / monthly_count_limit / daily_count_limit / use_shared_limit
--
-- 상한 컬럼이 전부 NULL 허용인 이유: 약관에 없는 제약과 0인 제약은 다르다.
--   monthly_limit = NULL → 월 한도 없음 / = 0 → 이번 달 혜택 없음
-- 엔진에서 NULL을 0으로 뭉개면 혜택이 통째로 사라진다.
CREATE TABLE benefit (
    benefit_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '혜택 ID',
    card_id              BIGINT        NOT NULL COMMENT '카드 ID',
    benefit_name         VARCHAR(100)  NOT NULL COMMENT '혜택명 (예: 카페 10% 청구할인)',

    benefit_kind         VARCHAR(20)   NOT NULL COMMENT '혜택 종류: DISCOUNT(할인) | POINT(적립) | SPECIAL_PRICE(특가) | GIFT(증정) | RETROACTIVE(사후정산, 계산 제외)',
    calc_method          VARCHAR(10)   NOT NULL COMMENT '계산 방식: RATE(정률) | FIXED(정액)',
    benefit_value        DECIMAL(10,2) NOT NULL COMMENT 'RATE면 퍼센트(10.00 = 10%), FIXED면 금액(원, 소수부 미사용)',
    apply_timing         VARCHAR(20)   NULL COMMENT '할인 시점: IMMEDIATE(즉시) | BILLED(청구). benefit_kind=DISCOUNT일 때만 값',

    target_type          VARCHAR(20)   NOT NULL COMMENT '대상 유형: CATEGORY | MERCHANT | ALL(전 가맹점)',
    target_category_id   BIGINT        NULL COMMENT 'target_type=CATEGORY일 때만. 대분류를 넣으면 하위 중분류까지 적용',
    target_merchant_id   BIGINT        NULL COMMENT 'target_type=MERCHANT일 때만',

    -- require_performance='Y'의 충족 기준을 여기서 못 박는다.
    -- 0원 구간 필수 규칙 때문에 "구간 못 찾음 = 미충족"이 성립하지 않는다.
    require_performance  CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '전월실적 조건 필요 여부: Y | N. 충족 기준 = 판정된 구간의 min_performance_amount > 0',
    -- 요일·시간대·채널 조건이 필요해지면 benefit_condition(benefit_id, condition_type,
    -- condition_value) 테이블로 확장하고 이 컬럼을 흡수한다 (benefit_exclusion과 대칭).
    require_payment_type VARCHAR(30)   NULL COMMENT '특정 결제수단에서만 적용 (예: SIMPLE_PAY). NULL이면 수단 무관',
    min_txn_amount       BIGINT        NULL COMMENT '건당 최소 결제금액(원). 미만이면 혜택 없음',

    max_eligible_amount  BIGINT        NULL COMMENT '혜택 대상 금액 상한(원). 이 금액까지만 율을 곱한다',
    max_benefit_per_txn  BIGINT        NULL COMMENT '건당 최대 혜택액(원)',
    monthly_limit        BIGINT        NULL COMMENT '이 혜택의 월 개별 한도(원). NULL=한도 없음',
    -- "통신·공과금·마트 각 10%, 합쳐서 월 5천원" 같은 묶음 한도용.
    -- 안 묶으면 한도가 혜택 수만큼 배로 샌다(에러 없이 금액만 틀림).
    limit_group_code     VARCHAR(30)   NULL COMMENT '묶음 한도 코드. 같은 카드 내 같은 코드끼리 monthly_limit 공유',
    monthly_count_limit  INT           NULL COMMENT '월 최대 적용 횟수',
    daily_count_limit    INT           NULL COMMENT '일 최대 적용 횟수',
    -- 일 단위 금액 한도. 횟수(daily_count_limit)와 축이 다르다.
    -- 약관 예) "월 적립한도 3만점, 일 적립한도 1만점" — 횟수로는 표현할 수 없다.
    daily_limit          BIGINT        NULL COMMENT '일 최대 혜택액(원). NULL=일 한도 없음',
    use_shared_limit     CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '카드 통합할인한도를 함께 소진하는가: Y | N',

    -- 계산에 절대 쓰지 않는다. 스키마로 표현하지 못한 약관 단서를 원문 그대로 남기는 칸.
    -- 추천 근거 문구는 여기서 읽지 않고 benefit_name + 계산값으로 엔진이 조립한다.
    description          VARCHAR(500)  NULL COMMENT '스키마로 표현 못 한 약관 단서 원문. 계산 사용 금지 — 표시·챗봇 컨텍스트용',
    is_active            CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '유효 여부: Y | N. 종료된 혜택은 N',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (benefit_id),
    KEY idx_benefit_card (card_id),
    KEY idx_benefit_target_category (target_category_id),
    KEY idx_benefit_target_merchant (target_merchant_id),
    KEY idx_benefit_limit_group (card_id, limit_group_code),

    CONSTRAINT fk_benefit_card     FOREIGN KEY (card_id)            REFERENCES card (card_id),
    CONSTRAINT fk_benefit_category FOREIGN KEY (target_category_id) REFERENCES category (category_id),
    CONSTRAINT fk_benefit_merchant FOREIGN KEY (target_merchant_id) REFERENCES merchant (merchant_id),

    -- 대상 컬럼은 target_type에 맞는 것 정확히 하나만 채운다
    CONSTRAINT ck_benefit_target CHECK (
        (target_type = 'CATEGORY' AND target_category_id IS NOT NULL AND target_merchant_id IS NULL)
     OR (target_type = 'MERCHANT' AND target_merchant_id IS NOT NULL AND target_category_id IS NULL)
     OR (target_type = 'ALL'      AND target_category_id IS NULL     AND target_merchant_id IS NULL)
    ),
    -- 할인 시점은 할인 혜택에만 있다. 적립·특가·증정·사후정산은 NULL이어야 한다.
    CONSTRAINT ck_benefit_apply_timing CHECK (
        (benefit_kind = 'DISCOUNT' AND apply_timing IS NOT NULL)
     OR (benefit_kind <> 'DISCOUNT' AND apply_timing IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 규칙';

-- 약관 예) "전월 30만원 이상 1%, 60만원 이상 2%, 한도도 각각 5천원/1만원"
--   → benefit 1행 + 이 테이블 2행. 혜택을 구간 수만큼 쪼개지 않는다.
-- NULL의 의미: 그 구간에서는 benefit의 기본값을 그대로 쓴다(상속).
CREATE TABLE benefit_tier_limit (
    benefit_id         BIGINT        NOT NULL COMMENT '혜택 ID',
    tier_id            BIGINT        NOT NULL COMMENT '실적구간 ID',
    tier_monthly_limit BIGINT        NULL COMMENT '이 구간에서의 월 개별 한도(원). NULL이면 benefit.monthly_limit 사용',
    tier_benefit_value DECIMAL(10,2) NULL COMMENT '이 구간에서의 혜택값(율 또는 정액). NULL이면 benefit.benefit_value 사용',
    PRIMARY KEY (benefit_id, tier_id),
    KEY idx_benefit_tier_limit_tier (tier_id),
    CONSTRAINT fk_benefit_tier_limit_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id),
    CONSTRAINT fk_benefit_tier_limit_tier    FOREIGN KEY (tier_id)    REFERENCES performance_tier (tier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 개별한도·혜택값';

-- 약관 예) "외식 5% (단, 배달앱 제외)"
--   → benefit은 대분류 외식을 겨냥하고, 여기에 CATEGORY 'DELIVERY' 한 행.
-- performance_exclusion과 동일한 (유형, 값) 패턴이다.
CREATE TABLE benefit_exclusion (
    benefit_id      BIGINT      NOT NULL COMMENT '혜택 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값 (CATEGORY면 category_code, MERCHANT면 merchant_code)',
    PRIMARY KEY (benefit_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_benefit_exclusion_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 적용 예외';

-- ════════════════════════════════════════════════════════════
-- 7. 소비 · 결제
-- ════════════════════════════════════════════════════════════

-- 엔진이 전월실적을 계산할 때 이 테이블을 카드별·월별로 합산한다.
-- 혜택 결과(applied_benefit_id, discount_amount)는 엔진이 채운다.
CREATE TABLE expense (
    expense_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '소비내역 ID',
    member_id          BIGINT       NOT NULL COMMENT '회원 ID',
    user_card_id       BIGINT       NOT NULL COMMENT '보유카드 ID',
    category_id        BIGINT       NOT NULL COMMENT '카테고리 ID',
    -- 엔진은 가맹점 혜택을 id로 찾는다. 이름 문자열로는 표기 차이 때문에 매칭이 깨진다.
    -- 혜택이 걸린 브랜드만 merchant에 등록되므로 미등록 가맹점은 NULL이다.
    merchant_id        BIGINT       NULL COMMENT '가맹점 ID. 미등록이면 NULL',
    merchant_name      VARCHAR(100) NULL COMMENT '가맹점명. 미등록 가맹점은 이름만 남는다',
    -- 카드 승인액(포인트 차감 후) 기준이다. 총 결제금액으로 저장하면
    -- 실적·혜택이 에러 없이 과다 계산된다.
    amount             BIGINT       NOT NULL COMMENT '결제금액 = 카드 승인액(포인트 차감 후)',
    payment_date       DATETIME     NOT NULL COMMENT '결제일시',
    input_type         VARCHAR(30)  NOT NULL COMMENT '입력 구분: MANUAL | PAYMENT',
    -- 취소 건은 실적·혜택 계산에서 제외한다(약관의 실적 제외 대상에 '취소금액'이 있다).
    -- 물리 삭제하지 않고 상태만 바꾸므로 엔진이 이 값을 보고 걸러야 한다.
    payment_status     VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT '결제 상태: APPROVED | CANCELED',
    applied_benefit_id BIGINT       NULL COMMENT '적용된 혜택 ID. 엔진이 채운다. 카드당 1개만 적용되므로 단수',
    discount_amount    BIGINT       NOT NULL DEFAULT 0 COMMENT '실제 받은 할인/적립액. 엔진이 채운다',
    -- 전월실적 계산에 필요하다. 카드 약관은 특정 결제수단과 무이자할부를
    -- 실적에서 빼는 게 기본이라, 이 두 값이 없으면 실적이 과다 계산된다.
    payment_type       VARCHAR(30)  NULL COMMENT '결제수단 (CARD, SIMPLE_PAY 등). 실적 제외 판정용',
    is_interest_free   CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '무이자할부 여부 Y/N. 실적 제외 판정용',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (expense_id),
    KEY idx_expense_member_date (member_id, payment_date),
    KEY idx_expense_card_date (user_card_id, payment_date),
    KEY idx_expense_merchant (merchant_id),
    KEY idx_expense_benefit (applied_benefit_id),
    CONSTRAINT fk_expense_member   FOREIGN KEY (member_id)          REFERENCES member (member_id),
    CONSTRAINT fk_expense_card     FOREIGN KEY (user_card_id)       REFERENCES user_card (user_card_id),
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id)        REFERENCES category (category_id),
    CONSTRAINT fk_expense_merchant FOREIGN KEY (merchant_id)        REFERENCES merchant (merchant_id),
    CONSTRAINT fk_expense_benefit  FOREIGN KEY (applied_benefit_id) REFERENCES benefit (benefit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '소비내역';

-- Mock 결제와 실제 결제를 한 테이블로 다룬다. 구분은 payment_channel 값.
-- 테이블명에 구현 방식(mock)을 넣으면 실서비스 전환 시 테이블을 rename해야 하고
-- 매퍼·DTO·쿼리가 전부 따라 바뀐다. 도메인 개념(결제)은 변하지 않으므로 이름은 payment다.
CREATE TABLE payment (
    payment_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '결제 ID',
    member_id          BIGINT       NOT NULL COMMENT '회원 ID',
    user_card_id       BIGINT       NOT NULL COMMENT '보유카드 ID',
    payment_channel    VARCHAR(20)  NOT NULL DEFAULT 'MOCK' COMMENT '결제 처리 방식: MOCK | PG',
    is_recommend_based CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '추천 결과로 결제했는지 Y/N',
    expense_id         BIGINT       NULL COMMENT '결제 성공 시 생성된 소비내역 ID',
    merchant_id        BIGINT       NULL COMMENT '가맹점 ID',
    merchant_name      VARCHAR(100) NULL COMMENT '가맹점명',
    payment_amount     BIGINT       NOT NULL COMMENT '결제금액',
    payment_status     VARCHAR(20)  NOT NULL COMMENT '결제상태: SUCCESS | FAIL | CANCELED',
    requested_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '결제요청일시',
    completed_at       DATETIME     NULL COMMENT '결제완료일시',
    PRIMARY KEY (payment_id),
    KEY idx_payment_member (member_id, requested_at),
    KEY idx_payment_expense (expense_id),
    KEY idx_payment_merchant (merchant_id),
    CONSTRAINT fk_payment_member   FOREIGN KEY (member_id)    REFERENCES member (member_id),
    CONSTRAINT fk_payment_card     FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id),
    CONSTRAINT fk_payment_expense  FOREIGN KEY (expense_id)   REFERENCES expense (expense_id),
    CONSTRAINT fk_payment_merchant FOREIGN KEY (merchant_id)  REFERENCES merchant (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '결제 (Mock/실결제 공용)';

-- 사용자가 추천을 요청하며 입력한 값의 기록.
-- 추천 "결과"는 저장하지 않는다 — 카드 한도 소진 상태에 따라 매번 달라져
-- 저장하는 순간 낡은 값이 된다. 엔진이 실시간 계산해 응답으로 내려준다.
CREATE TABLE recommend_input (
    recommend_input_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '추천입력 ID',
    member_id          BIGINT       NOT NULL COMMENT '회원 ID',
    category_id        BIGINT       NULL COMMENT '카테고리 ID. merchantId·categoryId 둘 다 없이 요청할 수 있어 NULL 허용',
    merchant_id        BIGINT       NULL COMMENT '가맹점 ID. 요청이 merchantId로 오므로 id로 저장한다',
    merchant_name      VARCHAR(100) NULL COMMENT '가맹점명(표시용)',
    expected_amount    BIGINT       NOT NULL COMMENT '결제예상금액',
    input_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '입력일시',
    PRIMARY KEY (recommend_input_id),
    KEY idx_recommend_input_member (member_id, input_at),
    CONSTRAINT fk_recommend_input_member   FOREIGN KEY (member_id)   REFERENCES member (member_id),
    CONSTRAINT fk_recommend_input_category FOREIGN KEY (category_id) REFERENCES category (category_id),
    CONSTRAINT fk_recommend_input_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '결제추천 입력 기록';

-- ════════════════════════════════════════════════════════════
-- 8. 계산 상태 (결제마다 갱신되고, 그 갱신이 다음 추천을 바꾼다 = 동적 전환)
--
-- 저장 원칙 — 원본만 저장하고 파생값은 저장하지 않는다.
--   저장 O : 전월실적, 당월누적, 한도사용액, 적용횟수
--   저장 X : 달성률, 잔여한도, 이용률, 이용상태, 적용구간 (조회 시 계산)
-- ════════════════════════════════════════════════════════════

-- prev_performance_amount는 전월 행의 current_performance_amount와 같은 값(캐시)이다.
--   원본은 전월 행이고, 전월 행이 없을 때(가입 첫 달, 마이데이터 과거분 적재)만
--   소비내역 합산으로 채운다. 전월 거래를 수정·취소하는 기능이 생기면
--   반드시 다음 달 행의 이 값도 같이 고쳐야 한다.
-- 월 롤오버 — 새 달 행은 조회·추천·결제 어느 시점이든 없으면 그때 만든다(lazy 생성).
--   prev_performance_amount는 전월 행의 current_performance_amount에서 가져오고,
--   전월 행이 없으면 소비내역을 실적제외 규칙 적용해 합산한다.
--   행이 없다고 0으로 두면 실적 구간이 0원으로 판정되어 실적 조건부 혜택이 전부 사라진다.
-- 적용 실적구간(tier)은 저장하지 않는다 — prev_performance_amount로 항상 도출된다.
CREATE TABLE user_card_monthly_state (
    user_card_id               BIGINT   NOT NULL COMMENT '보유카드 ID',
    base_year_month            CHAR(7)  NOT NULL COMMENT '기준 연월 (YYYY-MM)',
    prev_performance_amount    BIGINT   NOT NULL DEFAULT 0 COMMENT '전월실적(원). 실적제외 규칙 적용 후 합산액',
    current_performance_amount BIGINT   NOT NULL DEFAULT 0 COMMENT '당월 누적 실적인정액(원). 다음 달 전월실적이 된다',
    shared_limit_used          BIGINT   NOT NULL DEFAULT 0 COMMENT '통합할인한도 사용액(원)',
    updated_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (user_card_id, base_year_month),
    CONSTRAINT fk_user_card_monthly_state_user_card FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '보유카드 월별 실적·소진 상태';

-- last_applied_date와 daily_used_count가 같이 있는 이유:
--   일 N회 판정에 "오늘 몇 번 썼나"가 필요한데, 날짜별 행을 쌓으면 테이블이 커진다.
--   결제일이 last_applied_date와 다르면 daily_used_count를 0으로 리셋하고 센다.
-- "그 혜택이 그 카드의 혜택인지"는 DB가 검증하지 못하므로 엔진이 보장한다.
CREATE TABLE user_benefit_usage (
    user_card_id      BIGINT   NOT NULL COMMENT '보유카드 ID',
    benefit_id        BIGINT   NOT NULL COMMENT '혜택 ID',
    base_year_month   CHAR(7)  NOT NULL COMMENT '기준 연월 (YYYY-MM)',
    used_amount       BIGINT   NOT NULL DEFAULT 0 COMMENT '이 혜택으로 받은 누적 혜택액(원)',
    used_count        INT      NOT NULL DEFAULT 0 COMMENT '당월 누적 적용 횟수',
    last_applied_date DATE     NULL COMMENT '마지막 적용 일자. 일 단위 횟수 리셋 판정용',
    daily_used_count  INT      NOT NULL DEFAULT 0 COMMENT 'last_applied_date 당일의 적용 횟수',
    daily_used_amount BIGINT   NOT NULL DEFAULT 0 COMMENT 'last_applied_date 당일의 누적 혜택액(원). benefit.daily_limit 판정용',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (user_card_id, benefit_id, base_year_month),
    KEY idx_user_benefit_usage_benefit (benefit_id),
    CONSTRAINT fk_user_benefit_usage_user_card FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id),
    CONSTRAINT fk_user_benefit_usage_benefit   FOREIGN KEY (benefit_id)   REFERENCES benefit (benefit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택별 월 소진·적용횟수';

-- ════════════════════════════════════════════════════════════
-- 9. 포인트
-- ════════════════════════════════════════════════════════════

CREATE TABLE point_provider (
    point_provider_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '포인트사 ID',
    point_provider_name  VARCHAR(100) NOT NULL COMMENT '포인트사명 (포인트리, CJ ONE 등)',
    point_provider_type  VARCHAR(30)  NOT NULL COMMENT '포인트 구분: FINANCIAL_POINT | MEMBERSHIP',
    logo_image_url       VARCHAR(255) NULL COMMENT '로고 이미지',
    use_yn               CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 여부: Y | N',
    default_recommend_yn CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '기본 추천 여부: Y | N',
    recommend_priority   INT          NULL COMMENT '추천 우선순위',
    recommend_message    VARCHAR(255) NULL COMMENT '추천 문구',
    PRIMARY KEY (point_provider_id),
    UNIQUE KEY uk_point_provider_name (point_provider_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '포인트사';

CREATE TABLE point_wallet (
    point_wallet_id   BIGINT   NOT NULL AUTO_INCREMENT COMMENT '포인트지갑 ID',
    member_id         BIGINT   NOT NULL COMMENT '회원 ID',
    point_provider_id BIGINT   NOT NULL COMMENT '포인트사 ID',
    total_point       BIGINT   NOT NULL DEFAULT 0 COMMENT '총 보유 포인트',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (point_wallet_id),
    UNIQUE KEY uk_point_wallet (member_id, point_provider_id),
    CONSTRAINT fk_point_wallet_member   FOREIGN KEY (member_id)         REFERENCES member (member_id),
    CONSTRAINT fk_point_wallet_provider FOREIGN KEY (point_provider_id) REFERENCES point_provider (point_provider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '포인트지갑';

CREATE TABLE point_history (
    point_history_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '포인트내역 ID',
    member_id        BIGINT       NOT NULL COMMENT '회원 ID',
    point_wallet_id  BIGINT       NOT NULL COMMENT '포인트지갑 ID',
    expense_id       BIGINT       NULL COMMENT '관련 소비내역 ID. 없으면 NULL',
    point_type       VARCHAR(20)  NOT NULL COMMENT '포인트 유형: SAVE | USE',
    point_amount     BIGINT       NOT NULL COMMENT '적립/사용 포인트',
    content          VARCHAR(255) NULL COMMENT '내용',
    occurred_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발생일시',
    PRIMARY KEY (point_history_id),
    KEY idx_point_history_member (member_id, occurred_at),
    KEY idx_point_history_wallet (point_wallet_id),
    KEY idx_point_history_expense (expense_id),
    CONSTRAINT fk_point_history_member  FOREIGN KEY (member_id)       REFERENCES member (member_id),
    CONSTRAINT fk_point_history_wallet  FOREIGN KEY (point_wallet_id) REFERENCES point_wallet (point_wallet_id),
    CONSTRAINT fk_point_history_expense FOREIGN KEY (expense_id)      REFERENCES expense (expense_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '포인트 내역';

CREATE TABLE point_usage_place (
    point_usage_place_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '포인트 사용처 ID',
    point_provider_id    BIGINT       NOT NULL COMMENT '포인트사 ID',
    place_name           VARCHAR(100) NOT NULL COMMENT '사용처명',
    category_id          BIGINT       NULL COMMENT '카테고리 ID',
    use_yn               CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '사용 가능 여부: Y | N',
    description          VARCHAR(255) NULL COMMENT '제휴 정보 또는 사용 조건',
    PRIMARY KEY (point_usage_place_id),
    KEY idx_point_usage_place_provider (point_provider_id),
    CONSTRAINT fk_point_usage_place_provider FOREIGN KEY (point_provider_id) REFERENCES point_provider (point_provider_id),
    CONSTRAINT fk_point_usage_place_category FOREIGN KEY (category_id)       REFERENCES category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '포인트 사용처';

CREATE TABLE membership_register (
    membership_register_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '멤버십등록 ID',
    member_id              BIGINT      NOT NULL COMMENT '회원 ID',
    point_provider_id      BIGINT      NOT NULL COMMENT '포인트사 ID',
    register_status        VARCHAR(20) NOT NULL COMMENT '등록 상태: REGISTERED | CANCELED',
    registered_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    canceled_at            DATETIME    NULL COMMENT '해지일시',
    PRIMARY KEY (membership_register_id),
    UNIQUE KEY uk_membership_register (member_id, point_provider_id),
    CONSTRAINT fk_membership_register_member   FOREIGN KEY (member_id)         REFERENCES member (member_id),
    CONSTRAINT fk_membership_register_provider FOREIGN KEY (point_provider_id) REFERENCES point_provider (point_provider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '멤버십 등록';

-- ════════════════════════════════════════════════════════════
-- 10. 선호도
-- ════════════════════════════════════════════════════════════

CREATE TABLE member_preferred_category (
    member_preferred_category_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '선호 카테고리 ID',
    member_id                    BIGINT   NOT NULL COMMENT '회원 ID',
    category_id                  BIGINT   NOT NULL COMMENT '카테고리 ID',
    created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_preferred_category_id),
    UNIQUE KEY uk_member_preferred_category (member_id, category_id),
    CONSTRAINT fk_member_preferred_category_member   FOREIGN KEY (member_id)   REFERENCES member (member_id),
    CONSTRAINT fk_member_preferred_category_category FOREIGN KEY (category_id) REFERENCES category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 선호 카테고리';

CREATE TABLE member_preferred_merchant (
    member_preferred_merchant_id BIGINT   NOT NULL AUTO_INCREMENT COMMENT '선호 가맹점 ID',
    member_id                    BIGINT   NOT NULL COMMENT '회원 ID',
    category_id                  BIGINT   NOT NULL COMMENT '카테고리 ID',
    merchant_id                  BIGINT   NOT NULL COMMENT '가맹점 ID',
    priority                     TINYINT  NOT NULL COMMENT '우선순위 1~3',
    created_at                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_preferred_merchant_id),
    UNIQUE KEY uk_member_preferred_merchant (member_id, category_id, merchant_id),
    UNIQUE KEY uk_member_category_priority (member_id, category_id, priority),
    CONSTRAINT fk_member_preferred_merchant_member   FOREIGN KEY (member_id)   REFERENCES member (member_id),
    CONSTRAINT fk_member_preferred_merchant_category FOREIGN KEY (category_id) REFERENCES category (category_id),
    CONSTRAINT fk_member_preferred_merchant_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 선호 가맹점';

-- ════════════════════════════════════════════════════════════
-- 11. 알림
-- ════════════════════════════════════════════════════════════

CREATE TABLE notification (
    notification_id     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    member_id           BIGINT        NOT NULL COMMENT '회원 ID',
    user_card_id        BIGINT        NULL COMMENT '관련 보유카드 ID',
    benefit_id          BIGINT        NULL COMMENT '관련 혜택 ID',
    point_history_id    BIGINT        NULL COMMENT '관련 포인트내역 ID',
    notification_type   VARCHAR(50)   NOT NULL COMMENT '알림 유형',
    title               VARCHAR(100)  NOT NULL COMMENT '제목',
    content             VARCHAR(1000) NOT NULL COMMENT '내용',
    notification_status ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태',
    scheduled_at        DATETIME      NULL COMMENT '발송 예정일시',
    sent_at             DATETIME      NULL COMMENT '발송일시',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    read_at             DATETIME      NULL COMMENT '읽은 일시',
    deleted_at          DATETIME      NULL COMMENT '삭제일시',
    deduplication_key   VARCHAR(255)  NOT NULL COMMENT '중복 발송 방지 키',
    PRIMARY KEY (notification_id),
    UNIQUE KEY uk_notification_member_dedup (member_id, deduplication_key),
    KEY idx_notification_member_created (member_id, deleted_at, created_at),
    KEY idx_notification_member_read (member_id, read_at),
    KEY idx_notification_delivery (notification_status, scheduled_at),
    KEY idx_notification_card (user_card_id),
    KEY idx_notification_benefit (benefit_id),
    KEY idx_notification_point_history (point_history_id),
    CONSTRAINT fk_notification_member        FOREIGN KEY (member_id)        REFERENCES member (member_id),
    CONSTRAINT fk_notification_user_card     FOREIGN KEY (user_card_id)     REFERENCES user_card (user_card_id),
    CONSTRAINT fk_notification_benefit       FOREIGN KEY (benefit_id)       REFERENCES benefit (benefit_id),
    CONSTRAINT fk_notification_point_history FOREIGN KEY (point_history_id) REFERENCES point_history (point_history_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '알림';

CREATE TABLE notification_setting (
    notification_setting_id      BIGINT     NOT NULL AUTO_INCREMENT COMMENT '알림설정 ID',
    member_id                    BIGINT     NOT NULL COMMENT '회원 ID. 회원당 1개',
    performance_shortage_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '카드 실적 부족 알림 수신 여부',
    benefit_limit_enabled        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '혜택 한도 임박/소진 알림 수신 여부',
    updated_at                   DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (notification_setting_id),
    UNIQUE KEY uk_notification_setting_member (member_id),
    CONSTRAINT fk_notification_setting_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '알림 설정';
