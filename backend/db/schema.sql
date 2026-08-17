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
DROP TABLE IF EXISTS member_personalization_brand;
DROP TABLE IF EXISTS member_personalization_category;
DROP TABLE IF EXISTS membership_register;
DROP TABLE IF EXISTS point_usage_place;
DROP TABLE IF EXISTS point_history;
DROP TABLE IF EXISTS point_wallet;
DROP TABLE IF EXISTS point_provider;
DROP TABLE IF EXISTS user_benefit_usage;
DROP TABLE IF EXISTS user_card_benefit_selection;
DROP TABLE IF EXISTS user_card_monthly_state;
DROP TABLE IF EXISTS recommend_input;
DROP TABLE IF EXISTS payment_qr;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS expense;
DROP TABLE IF EXISTS card_benefit_exclusion;
DROP TABLE IF EXISTS benefit_exclusion;
DROP TABLE IF EXISTS benefit_tier_limit;
DROP TABLE IF EXISTS benefit;
DROP TABLE IF EXISTS performance_exclusion;
DROP TABLE IF EXISTS performance_tier;
DROP TABLE IF EXISTS user_card;
DROP TABLE IF EXISTS mock_card;
DROP TABLE IF EXISTS card_alias;
DROP TABLE IF EXISTS merchant_alias;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS card_annual_fee;
DROP TABLE IF EXISTS card_term_document;
DROP TABLE IF EXISTS card;
DROP TABLE IF EXISTS card_bin;
DROP TABLE IF EXISTS card_company;
DROP TABLE IF EXISTS member_term_agreement;
DROP TABLE IF EXISTS term_version;
DROP TABLE IF EXISTS term;
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS signup_email_verification;
DROP TABLE IF EXISTS password_reset_verification;
DROP TABLE IF EXISTS simple_password_verification;
DROP TABLE IF EXISTS member_withdrawal_archive;
DROP TABLE IF EXISTS member_withdrawal;
DROP TABLE IF EXISTS member;

SET FOREIGN_KEY_CHECKS = 1;

-- ════════════════════════════════════════════════════════════
-- 1. 회원 · 인증
-- ════════════════════════════════════════════════════════════

CREATE TABLE member (
    member_id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    email                VARCHAR(255) NOT NULL COMMENT '이메일(로그인 ID)',
    password_hash        VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    -- 기존 회원은 간편비밀번호를 설정하지 않았으므로 NULL을 허용한다.
    -- 6자리 원문은 저장하지 않고, 서버에서 BCrypt로 만든 해시만 저장한다.
    simple_password_hash VARCHAR(255) NULL COMMENT '간편비밀번호 해시',
    simple_password_failed_attempt_count INT NOT NULL DEFAULT 0 COMMENT '간편비밀번호 연속 검증 실패 횟수',
    simple_password_locked_until DATETIME NULL COMMENT '간편비밀번호 검증 잠금 만료일시',
    name                 VARCHAR(50)  NOT NULL COMMENT '회원명',
    nickname             VARCHAR(50)  NOT NULL COMMENT '닉네임',
    member_status        ENUM('ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    withdrawn_at         DATETIME     NULL COMMENT '탈퇴일시. 상태가 WITHDRAWN이면 필수',
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

-- member_withdrawal_archive: 탈퇴 회원의 개인정보를 보관 기간 동안 담아두는 테이블이다.
-- member 테이블의 email·name은 탈퇴 즉시 마스킹 값으로 바뀌므로(회원 서비스 코드에서 처리),
-- 원본 값을 잃어버리기 전에 이 테이블로 옮겨 담는다.
-- member_withdrawal과 성격이 다르다: member_withdrawal은 "탈퇴 사유"라는 통계성 정보라
-- 기간 제한 없이 보관해도 되지만, 이 테이블은 실제 개인정보라 배치가 물리 삭제해야 한다.
-- 두 정보를 한 테이블에 같이 두면, 파기 배치가 탈퇴 사유 통계까지 함께 지워버리게 되어
-- 테이블을 분리했다.
-- member_withdrawal과 마찬가지로 회원 삭제 후에도 이 행 자체는 남아야 하므로 물리 FK를 걸지 않는다(논리 참조).
CREATE TABLE member_withdrawal_archive (
    member_withdrawal_archive_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '보관 ID',
    member_id                    BIGINT       NOT NULL COMMENT '회원 ID (논리 참조)',
    email                        VARCHAR(255) NOT NULL COMMENT '탈퇴 시점 이메일 원본',
    -- 이메일 원본을 그대로 비교하면 조회 시 매번 문자열 비교라 느리고,
    -- "이 이메일로 예전에 탈퇴한 적 있는지" 같은 정책이 나중에 추가될 때 인덱스로 바로 조회하기 위해
    -- SHA-256 해시값을 별도로 저장해둔다. 지금 당장 쓰는 곳은 없지만, 나중에 추가하면
    -- 이미 보관된 예전 데이터에는 소급 적용을 할 수 없으므로 처음부터 함께 저장한다.
    email_hash                   CHAR(64)     NOT NULL COMMENT '이메일 SHA-256 해시. 재가입 제한 등 향후 정책 조회용',
    name                         VARCHAR(50)  NOT NULL COMMENT '탈퇴 시점 회원명 원본',
    withdrawn_at                 DATETIME     NOT NULL COMMENT '탈퇴일시',
    retention_reason             VARCHAR(100) NOT NULL COMMENT '보관 근거 (예: 전자상거래법 제6조 소비자 불만·분쟁 처리 기록)',
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_withdrawal_archive_id),
    -- 회원 한 명당 보관 행은 하나여야 한다. (탈퇴 → 재가입 → 재탈퇴 시나리오는 범위 밖으로 남겨둔다)
    UNIQUE KEY uk_withdrawal_archive_member (member_id),
    -- 파기 배치(추후 구현)가 "탈퇴한 지 오래된" 행을 찾을 때 쓸 인덱스다.
    -- 몇 년을 기준으로 삼을지는 이 시점에 정하지 않는다 — 배치가 실제로
    -- 만들어질 때(WHERE withdrawn_at <= NOW() - INTERVAL n YEAR) 그 값을
    -- 그 코드가 정한다.
    KEY idx_withdrawal_archive_withdrawn_at (withdrawn_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '탈퇴회원 개인정보 보관';

CREATE TABLE signup_email_verification (
    signup_email_verification_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원가입 이메일 인증 ID',
    email                        VARCHAR(255) NOT NULL COMMENT '인증 대상 이메일',
    verification_code_hash       VARCHAR(255) NOT NULL COMMENT '인증 코드 단방향 해시',
    verification_status          ENUM('PENDING', 'VERIFIED', 'USED', 'EXPIRED') NOT NULL DEFAULT 'PENDING' COMMENT '인증 상태',
    failed_attempt_count         INT          NOT NULL DEFAULT 0 COMMENT '인증 코드 검증 실패 횟수',
    verification_code_expires_at DATETIME     NOT NULL COMMENT '인증 코드 만료일시',
    signup_token_hash            VARCHAR(255) NULL COMMENT '회원가입 인증 토큰 해시',
    signup_token_expires_at      DATETIME     NULL COMMENT '회원가입 인증 토큰 만료일시',
    verified_at                  DATETIME     NULL COMMENT '인증 완료일시',
    used_at                      DATETIME     NULL COMMENT '회원가입에 사용된 일시',
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (signup_email_verification_id),
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_signup_email_verification_token (signup_token_hash),
    KEY idx_signup_email_verification_email_status (email, verification_status),
    KEY idx_signup_email_verification_code_expiry (verification_code_expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '회원가입 이메일 인증';

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

-- 이메일 인증 코드와 변경 토큰은 유효 기간이 다르므로 만료일시를 따로 관리한다.
-- 코드와 토큰의 원문을 DB에 남기지 않아 DB 노출 시 즉시 악용되는 것을 방지한다.
CREATE TABLE simple_password_verification (
    simple_password_verification_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '간편비밀번호 변경 인증 ID',
    member_id                       BIGINT       NOT NULL COMMENT '회원 ID',
    verification_code_hash          VARCHAR(255) NOT NULL COMMENT '이메일 인증 코드 단방향 해시',
    verification_status             ENUM('PENDING','VERIFIED','USED','EXPIRED') NOT NULL DEFAULT 'PENDING' COMMENT '인증 상태',
    failed_attempt_count            INT          NOT NULL DEFAULT 0 COMMENT '인증 코드 검증 실패 횟수',
    verification_code_expires_at    DATETIME     NOT NULL COMMENT '인증 코드 만료일시',
    change_token_hash               VARCHAR(255) NULL COMMENT '간편비밀번호 변경 토큰 단방향 해시',
    change_token_expires_at         DATETIME     NULL COMMENT '변경 토큰 만료일시',
    verified_at                     DATETIME     NULL COMMENT '이메일 인증 완료일시',
    used_at                         DATETIME     NULL COMMENT '간편비밀번호 설정·변경에 사용된 일시',
    created_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at                      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (simple_password_verification_id),
    UNIQUE KEY uk_simple_password_change_token (change_token_hash),
    KEY idx_simple_password_verification_member_status (member_id, verification_status),
    KEY idx_simple_password_verification_code_expiry (verification_code_expires_at),
    CONSTRAINT fk_simple_password_verification_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '간편비밀번호 변경 이메일 인증';

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
    -- 이 약관이 어느 화면(시점)에 노출되는지 구분하는 값이다.
    -- SIGNUP = 회원가입 화면에서 동의를 받는 약관 (기존 약관 전부 여기 해당)
    -- WITHDRAWAL = 회원 탈퇴 화면에서 고지하고 동의를 받는 약관
    -- DEFAULT 'SIGNUP'으로 둔 이유: 기존 4개 약관 행이 전부 회원가입용이므로,
    -- 값을 안 넣어도 자동으로 기존 동작(회원가입 필수 약관 검사)이 그대로 유지된다.
    term_scope  VARCHAR(20)  NOT NULL DEFAULT 'SIGNUP' COMMENT '약관 노출 시점: SIGNUP(가입 시) | WITHDRAWAL(탈퇴 시)',
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

CREATE TABLE card_company (
    card_company_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카드사 ID',
    company_code    VARCHAR(30) NOT NULL COMMENT '카드사 코드',
    company_name    VARCHAR(50) NOT NULL COMMENT '카드사명',
    is_active       CHAR(1)     NOT NULL DEFAULT 'Y' COMMENT '사용 여부: Y | N',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (card_company_id),
    UNIQUE KEY uk_card_company_code (company_code),
    UNIQUE KEY uk_card_company_name (company_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드사 마스터';

-- 카드번호 앞자리로 카드사를 판별한다. 카드 등록 화면이 입력값을 검증할 때 쓴다.
CREATE TABLE card_bin (
    card_bin_id     BIGINT     NOT NULL AUTO_INCREMENT COMMENT '카드 BIN ID',
    card_company_id BIGINT     NOT NULL COMMENT '카드사 ID',
    bin_prefix      VARCHAR(8) NOT NULL COMMENT '6자리 또는 8자리 BIN',
    bin_length      TINYINT    NOT NULL COMMENT 'BIN 길이: 6 또는 8',
    is_active       CHAR(1)    NOT NULL DEFAULT 'Y' COMMENT '사용 여부: Y | N',
    created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (card_bin_id),
    UNIQUE KEY uk_card_bin_prefix (bin_prefix),
    KEY idx_card_bin_company (card_company_id),
    CONSTRAINT fk_card_bin_company FOREIGN KEY (card_company_id) REFERENCES card_company (card_company_id),
    CONSTRAINT chk_card_bin_length CHECK (bin_length IN (6, 8))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 BIN과 카드사 매핑';

CREATE TABLE card (
    card_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    card_company_id BIGINT       NOT NULL COMMENT '카드사 ID',
    card_name       VARCHAR(100) NOT NULL COMMENT '카드명 (예: 나라사랑카드)',
    card_type       VARCHAR(20)  NOT NULL COMMENT '카드 종류: CREDIT(신용) | CHECK(체크)',
    annual_fee      INT          NOT NULL DEFAULT 0 COMMENT '연회비(원). 체크카드는 0. 브랜드·발급형태별 금액은 card_annual_fee',
    image_url       VARCHAR(255) NULL COMMENT '카드 이미지 URL',
    description     VARCHAR(500) NULL COMMENT '카드 한줄 소개',
    is_active       CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '판매중 여부: Y | N',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (card_id),
    KEY idx_card_company_active (card_company_id, is_active),
    CONSTRAINT fk_card_company FOREIGN KEY (card_company_id) REFERENCES card_company (card_company_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 마스터';

-- 시연 환경에서 전체 카드번호를 카드 상품에 정확히 연결하기 위한 Mock 카드다.
-- 실제 발급 카드번호가 아닌 개발·시연 목적으로 만든 가상 번호만 저장한다.
CREATE TABLE mock_card (
    mock_card_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Mock 카드 ID',
    card_id      BIGINT      NOT NULL COMMENT '연결할 카드 상품 ID',
    -- 사용자가 입력한 카드번호에서 공백과 하이픈을 제거한 값이다.
    -- 실제 발급 카드번호가 아니라 개발 및 시연 목적으로 만든 번호만 저장한다.
    card_number  VARCHAR(19) NOT NULL COMMENT '정규화된 전체 카드번호',
    is_active    CHAR(1)     NOT NULL DEFAULT 'Y' COMMENT '등록 허용 여부: Y | N',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (mock_card_id),
    UNIQUE KEY uk_mock_card_number (card_number),
    KEY idx_mock_card_card_active (card_id, is_active),
    CONSTRAINT fk_mock_card_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    CONSTRAINT chk_mock_card_number CHECK (card_number REGEXP '^[0-9]{13,19}$'),
    CONSTRAINT chk_mock_card_active CHECK (is_active IN ('Y', 'N'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '시연용 카드번호와 카드 상품 매핑';

-- 연회비는 하나가 아니다. 국제브랜드(국내전용/VISA/Mastercard)와 발급 형태(실물/모바일단독)에
-- 따라 갈리고, 기본연회비와 제휴연회비가 따로 청구된다.
-- card.annual_fee는 대표값으로 남겨 목록·비교 화면이 쓰고, 정확한 금액이 필요하면 이 표를 본다.
CREATE TABLE card_annual_fee (
    card_annual_fee_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카드 연회비 ID',
    card_id            BIGINT      NOT NULL COMMENT '카드 ID',
    -- ANY는 약관이 브랜드를 구분하지 않고 한 금액만 제시할 때 쓴다.
    -- NULL로 두면 UNIQUE 제약이 중복을 못 막으므로 값으로 표현한다.
    brand              VARCHAR(30) NOT NULL COMMENT '국제브랜드: LOCAL(국내전용) | VISA | MASTERCARD | AMEX | UNIONPAY | K_WORLD | ANY(브랜드 무관)',
    issue_type         VARCHAR(20) NOT NULL DEFAULT 'PLASTIC' COMMENT '발급 형태: PLASTIC(실물) | MOBILE(모바일단독) | ANY',
    -- 같은 카드가 리워드 종류로 갈리며 연회비가 다른 경우가 있다.
    -- 약관 예) The BEST-XO는 마이신한포인트형과 스카이패스형의 연회비가 2만원 차이난다.
    -- 브랜드·발급형태와는 다른 축이라 그 컬럼에 넣으면 의미가 어긋나고, 빼면 같은 (카드,브랜드)에
    -- 금액이 둘이 되어 UNIQUE 제약에 걸린다. 갈리지 않는 카드는 ANY다.
    variant            VARCHAR(30) NOT NULL DEFAULT 'ANY' COMMENT '상품형(리워드 종류). 갈리지 않으면 ANY',
    -- 실제로 청구되는 금액이라 항상 있다.
    total_fee          INT         NOT NULL COMMENT '총 연회비(원)',
    -- 약관이 "20,000원(기본 7천 + 제휴 13천)"처럼 나눠 적을 때만 채운다.
    -- 나누지 않은 약관에 임의로 배분하면 없는 값을 만드는 것이라 NULL로 둔다.
    base_fee           INT         NULL COMMENT '기본연회비(원). 약관이 분리 표기할 때만',
    partner_fee        INT         NULL COMMENT '제휴연회비(원). 약관이 분리 표기할 때만',
    PRIMARY KEY (card_annual_fee_id),
    UNIQUE KEY uk_card_annual_fee (card_id, brand, issue_type, variant),
    CONSTRAINT fk_card_annual_fee_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    -- 나눠 적었으면 합이 총액과 맞아야 한다. 안 맞으면 추출이 틀린 것이다.
    CONSTRAINT ck_card_annual_fee_total CHECK (
        base_fee IS NULL OR partner_fee IS NULL OR base_fee + partner_fee = total_fee
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 연회비 (브랜드·발급형태별)';

-- 카테고리 표준 (대분류 7 / 중분류 33, 계층 깊이 2단계 고정).
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

-- 사람이 부르는 이름을 정식 명칭에 잇는다. 챗봇이 "스벅에서 얼마 썼어?" 같은 질문을
-- 처리하려면 필요하다.
-- 이름 유사도로 대신할 수 없다. "스벅"과 "스타벅스"는 글자가 '스' 하나만 겹쳐
-- 어떤 문자열 거리로도 가까워지지 않는다. 축약·별명은 등록해두는 것 말고 방법이 없다.
-- alias를 UNIQUE로 잡은 것은 판단이다. 한 별칭이 두 가맹점을 가리키면 어느 쪽인지
-- 알 수 없어 조용히 틀린 답이 나간다. 모호한 별칭은 아예 등록하지 않고, 못 찾은 것으로
-- 두어 되묻게 한다. 후보를 여럿 보여주고 고르게 하려면 이 제약을 풀고 복수 반환으로 바꾼다.
CREATE TABLE merchant_alias (
    merchant_alias_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '가맹점 별칭 ID',
    merchant_id       BIGINT      NOT NULL COMMENT '가맹점 ID',
    alias             VARCHAR(50) NOT NULL COMMENT '사용자가 부르는 표현 (예: 스벅)',
    PRIMARY KEY (merchant_alias_id),
    UNIQUE KEY uk_merchant_alias (alias),
    KEY idx_merchant_alias_merchant (merchant_id),
    CONSTRAINT fk_merchant_alias_merchant FOREIGN KEY (merchant_id) REFERENCES merchant (merchant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '가맹점 별칭';

-- 카드명은 길고 사람은 줄여 부른다("신한카드 핏(Fit)" → "핏카드", "신한 핏").
-- merchant_alias와 같은 이유로 필요하고 같은 규칙을 따른다.
CREATE TABLE card_alias (
    card_alias_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카드 별칭 ID',
    card_id       BIGINT      NOT NULL COMMENT '카드 ID',
    alias         VARCHAR(50) NOT NULL COMMENT '사용자가 부르는 표현 (예: 핏카드)',
    PRIMARY KEY (card_alias_id),
    UNIQUE KEY uk_card_alias (alias),
    KEY idx_card_alias_card (card_id),
    CONSTRAINT fk_card_alias_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 별칭';

-- 카드사가 공시한 약관 원문. benefit으로 구조화하기 전의 원본이다.
-- 원문을 DB에 두는 이유: 혜택 규칙과 약관이 같은 곳에서 관리돼야 카드를 추가할 때
-- 한쪽만 갱신되는 일이 없다. 임베딩 같은 파생물은 이 원문에서 언제든 다시 만든다.
-- benefit이 계산용 규칙만 담는 것과 달리, 청구 시점·분실 처리처럼 컬럼으로 만들 수 없는
-- 내용이 여기 남는다. 서로 대체할 수 없다.
CREATE TABLE card_term_document (
    card_term_document_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '약관 문서 ID',
    card_id               BIGINT       NULL COMMENT '카드 ID. 카드 마스터와 매칭되기 전이면 NULL',
    -- 수집 시점에는 대응하는 card 행이 없을 수 있다. 카드사가 표기한 이름을 그대로 남겨야
    -- card_id가 NULL인 문서를 나중에 어느 카드에 붙일지 판단할 수 있다.
    source_card_name      VARCHAR(200) NOT NULL COMMENT '카드사 표기 카드명. card_id 매칭의 근거',
    -- 수집기가 쓰는 카드사 표기와 카드사 마스터의 정식 명칭이 다르다(KB국민 / KB국민카드).
    -- 문자열로 이으면 표기가 갈리는 순간 조용히 조인이 비므로 ID로 잇는다.
    -- 원래 표기는 그대로 남긴다 — 매칭이 틀렸을 때 무엇을 보고 이었는지 근거가 된다.
    card_company_id       BIGINT       NULL COMMENT '카드사 ID. 마스터와 매칭되기 전이면 NULL',
    issuer                VARCHAR(50)  NOT NULL COMMENT '수집기가 쓴 카드사 표기 (예: KB국민, 삼성)',
    -- 카드사마다 문서 구성이 다르다. 유형을 ENUM으로 고정하면 카드사를 추가할 때마다
    -- ALTER가 필요하므로 값으로 흡수한다.
    doc_type              VARCHAR(30)  NOT NULL COMMENT '문서 유형: PRODUCT_GUIDE(상품설명서) | KEY_TERMS(주요거래조건)',
    source_url            VARCHAR(500) NOT NULL COMMENT '수집 출처 URL',
    -- 카드사 URL은 사이트 개편으로 죽는다. 원본을 따로 보관해야 재추출이 가능하다.
    storage_path          VARCHAR(500) NOT NULL COMMENT '보관한 원본 PDF 경로',
    -- 스캔 이미지로만 된 PDF는 텍스트가 거의 나오지 않는다. 상태를 남겨야
    -- 재처리 대상을 골라낼 수 있다. 상태를 안 남기면 빈 원문이 정상처럼 섞인다.
    extract_status        VARCHAR(20)  NOT NULL COMMENT '추출 상태: TEXT_OK | VISION_OK(이미지 PDF를 비전으로 읽음) | IMAGE_ONLY(텍스트 추출 불가)',
    page_count            INT          NULL COMMENT '페이지 수',
    content_text          LONGTEXT     NULL COMMENT '추출된 원문 텍스트. IMAGE_ONLY면 NULL',
    -- 같은 문서를 다시 받았을 때 개정 여부를 판정한다. 해시가 같으면 구조화를 건너뛴다.
    content_hash          CHAR(64)     NOT NULL COMMENT '원본 PDF의 SHA-256. 개정 감지용',
    -- 카드사가 목록에 함께 주는 값이다. 이 값이 있으면 내려받기 전에 이미 가진 문서인지
    -- 가려낼 수 있다. 없으면 판정할 방법이 해시뿐이라 매번 다시 받아야 한다.
    revised_at            CHAR(8)      NULL COMMENT '약관 시행일 YYYYMMDD. 카드사가 주지 않으면 NULL',
    fetched_at            DATETIME     NOT NULL COMMENT '수집 일시',

    PRIMARY KEY (card_term_document_id),
    KEY idx_card_term_document_revision (issuer, source_card_name, doc_type, revised_at),
    -- 같은 문서를 여러 번 수집해도 행이 늘지 않게 막는다.
    -- 약관이 개정되면 해시가 달라지므로 새 행으로 쌓여 이력이 남는다.
    UNIQUE KEY uk_card_term_document (issuer, doc_type, content_hash),
    KEY idx_card_term_document_card (card_id),
    KEY idx_card_term_document_company (card_company_id),
    CONSTRAINT fk_card_term_document_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    CONSTRAINT fk_card_term_document_company FOREIGN KEY (card_company_id)
        REFERENCES card_company (card_company_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 약관 원문';

-- 약관 원문을 검색 단위로 자른 조각.
-- 원문 한 장이 수만 자라 통째로는 프롬프트에 들어가지 않고, 들어가더라도 질문과
-- 무관한 조항이 대부분이라 답이 흐려진다. 조문 단위로 잘라 두고 질문에 가까운
-- 몇 개만 골라 넣는다.
--
-- 개정된 약관이 새 행으로 쌓이는 원문과 달리, 조각은 현행본만 남긴다.
-- 옛 조각을 함께 두면 "연회비 반환 기준"에 지난 시행본과 현행본이 나란히 검색돼
-- 어느 쪽이 지금 맞는 답인지 가릴 수 없다. 원문 이력은 위 테이블에 그대로 남으므로
-- 조각은 언제든 다시 만들 수 있다.
CREATE TABLE card_term_chunk (
    card_term_chunk_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '약관 조각 ID',
    card_term_document_id BIGINT       NOT NULL COMMENT '약관 문서 ID',
    chunk_index           INT          NOT NULL COMMENT '문서 안에서의 순서. 0부터',
    -- 조각만 따로 읽는 검색 단계에서 무엇에 관한 규정인지 드러나게 한다.
    -- 조문 구조가 없는 문서(상품설명서)는 글자 수로 자르므로 값이 없다.
    heading               VARCHAR(200) NULL COMMENT '속한 장·조 제목. 조문 구조가 없으면 NULL',
    content               TEXT         NOT NULL COMMENT '조각 본문',
    -- 임베딩은 float32 배열을 그대로 담는다. JSON 문자열로 두면 숫자 하나가
    -- 열 바이트 남짓을 먹어 같은 값이 두 배 넘는 자리를 차지한다.
    -- 파생물이라 값이 없어도 검색은 동작한다(키워드 검색으로만 돌아간다).
    embedding             BLOB         NULL COMMENT '임베딩 벡터(float32 이진). 미생성이면 NULL',
    embedding_model       VARCHAR(50)  NULL COMMENT '임베딩을 만든 모델. 섞이면 유사도가 무의미해진다',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (card_term_chunk_id),
    -- 같은 문서를 다시 자르면 조각이 두 벌로 쌓인다. 순서 번호로 막는다.
    UNIQUE KEY uk_card_term_chunk (card_term_document_id, chunk_index),
    -- 한글은 띄어쓰기로 단어가 갈리지 않아 기본 파서로는 색인이 되지 않는다.
    -- ngram 파서는 글자 두 개씩 잘라 색인하므로 '분실신고' 같은 말도 걸린다.
    FULLTEXT KEY ft_card_term_chunk_content (content) WITH PARSER ngram,
    -- 문서가 지워지면 조각도 함께 지운다. 남으면 원문 없는 조각이 검색에 걸린다.
    CONSTRAINT fk_card_term_chunk_document FOREIGN KEY (card_term_document_id)
        REFERENCES card_term_document (card_term_document_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '약관 원문 검색 조각';

-- ════════════════════════════════════════════════════════════
-- 4. 보유카드
--    엔진의 상태 테이블은 전부 user_card_id 기준이다 (card_id 아님).
-- ════════════════════════════════════════════════════════════

-- 대표 카드는 회원당 최대 3개까지 허용한다.
-- DB에서는 대표 카드 여부만 저장하고, "최대 3개" 규칙은 서비스 트랜잭션에서 검증한다.
-- 이유: 일반적인 UNIQUE 제약만으로 "회원당 최대 3개" 같은 개수 제한을 표현하기 어렵기 때문이다.
CREATE TABLE user_card (
    user_card_id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '보유카드 ID',
    member_id                BIGINT      NOT NULL COMMENT '회원 ID',
    card_id                  BIGINT      NOT NULL COMMENT '카드 ID',
    masked_card_number       VARCHAR(30) NOT NULL COMMENT '마스킹된 카드번호',
    is_representative        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '대표카드 여부',
    registered_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    card_status              ENUM('ACTIVE','DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '카드 상태',
    PRIMARY KEY (user_card_id),

    UNIQUE KEY uk_user_card_member_card (member_id, card_id),

    -- 보유 카드 목록 조회와 대표 카드 개수 조회에서 함께 사용할 인덱스다.
    -- member_id, card_status 조건만 사용하는 기존 목록 조회도 이 인덱스를 활용할 수 있다.
    -- 대표 카드 설정 시에는 member_id + ACTIVE + is_representative = 1 조건으로 현재 개수를 빠르게 조회한다.
    KEY idx_user_card_member_status_representative (member_id, card_status, is_representative),

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
    -- 한 카드가 기간이 다른 구간표를 둘 가지는 경우가 있다.
    -- 약관 예) 일상 영역은 전월 40만원, 특정 영역은 전분기 100/200/300만원.
    -- benefit.performance_period가 어느 구간표를 볼지 가리킨다.
    period_type            VARCHAR(10) NOT NULL DEFAULT 'MONTH' COMMENT '구간 기준 기간: MONTH(전월) | QUARTER(전분기)',
    min_performance_amount BIGINT NOT NULL COMMENT '이 구간의 최소 실적(원). 조건 없으면 0',
    shared_monthly_limit   BIGINT NULL COMMENT '통합할인한도(월, 원). NULL = 통합한도 없음(개별한도만 적용), 0 = 혜택 없음',
    PRIMARY KEY (tier_id),
    UNIQUE KEY uk_performance_tier (card_id, period_type, min_performance_amount),
    CONSTRAINT fk_performance_tier_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 통합할인한도';

-- 발급 초기 실적 유예. 카드 50장 중 30장에 있는 조항이다.
--   "최초 카드 사용등록일로부터 다음달 말일까지는 전월 실적이 없어도
--    '40만원 이상~80만원 미만' 구간의 서비스가 적용됩니다"
--
-- 실적을 0으로 보는 것이 아니라 특정 구간에 있는 것으로 친다. 어느 구간으로 쳐주는지가
-- 카드마다 다르므로(30만원 구간·40만원 구간·50만원 구간) 구간을 직접 가리킨다.
-- 실적을 0으로 두면 실적 조건이 붙은 혜택이 전부 꺼져 신규 발급 회원의 혜택이 계산되지 않는다.
--
-- 월 축과 분기 축이 따로 유예되는 카드가 있어(월은 40만원 구간, 분기는 100만원 구간)
-- period_type이 PK에 들어간다.
--
-- 유예를 구간으로 표현할 수 없는 카드는 행을 만들지 않는다. "구간 한도의 50%까지",
-- "영역별 월 2,500원 한도까지"처럼 구간이 아니라 한도를 깎는 형태가 있는데,
-- 구간으로 적으면 한도가 약관의 두 배가 된다. 유예 없음으로 두면 그 카드만 보수적으로 계산된다.
CREATE TABLE card_performance_grace (
    card_id       BIGINT      NOT NULL COMMENT '카드 ID',
    period_type   VARCHAR(10) NOT NULL COMMENT '실적 축: MONTH | QUARTER (performance_tier.period_type과 짝)',
    tier_id       BIGINT      NOT NULL COMMENT '유예 기간에 적용할 실적 구간',
    -- 조사한 카드는 전부 1이다("등록월 + 다음달 말일까지", "발급월+1개월까지").
    -- 그래도 컬럼으로 두는 것은 60일·3개월로 적은 카드가 있어 값이 하나라고 단정할 수 없기 때문이다.
    grace_periods TINYINT     NOT NULL COMMENT '사용등록 기간 이후 몇 기간까지 유예되는가 (MONTH면 개월, QUARTER면 분기)',
    PRIMARY KEY (card_id, period_type),
    CONSTRAINT fk_card_performance_grace_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    CONSTRAINT fk_card_performance_grace_tier FOREIGN KEY (tier_id) REFERENCES performance_tier (tier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '발급 초기 실적 유예';

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
--                       'CASH_ADVANCE'          현금서비스
--                       'CARD_LOAN'             카드론(장기카드대출)
--                       'GIFT_CARD'             상품권·선불카드 구매/충전
--                       'TAX'                   국세·지방세
--                       'SOCIAL_INSURANCE'      4대 사회보험료
--                       'FEE_INTEREST'          수수료·이자·연체료
--                       'ANNUAL_FEE'            연회비
--                       'GOV_SUBSIDY'           정부지원금(보육료·바우처 등)
--                       'POSTPAID_TRANSIT'      후불교통요금
--                       'UNAPPROVED'            무승인전표 전반(자판기·무인정산 등)
--                       'TOLL'                  고속도로 통행료
--                       'CANCELED'              취소·부분취소 거래
--                       'LEVY'                  부담금·준조세(장애인 고용부담금 등)
--                       'POINT_USED'            포인트로 결제한 금액
--                       'INSTALLMENT_CONVERTED' 일시불을 할부로 전환한 거래
--                       'RECURRING'             정기결제·자동이체 등록 건
--                       'CARD_SERVICE_FEE'      카드사 부가서비스 이용료(문자알림 등)
--   MIN_TXN_AMOUNT      '10000'                 건당 1만원 미만 제외
--
-- 위 목록은 카드사 약관에 반복해서 나오는 항목을 표준화한 것이다.
-- 다만 판정하려면 소비내역에 그 거래가 어떤 종류인지 남아 있어야 한다.
-- 지금 expense가 구분할 수 있는 것은 무이자할부·할인적용 여부뿐이므로,
-- 나머지 값은 표현만 가능하고 엔진이 해석하지 못한다. 시드에 넣으면 조용히 무시되므로 넣지 않는다.
--
-- '특정 혜택을 받은 거래만 실적 제외'는 여기 적지 않는다. 'DISCOUNTED'로 적으면
-- 다른 혜택을 받은 거래까지 빠져 실적이 실제보다 낮아진다. benefit.exclude_from_performance를 쓴다.
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

    -- INSTALLMENT_FREE는 금액으로 환산할 수 없어 GIFT·RETROACTIVE와 함께 계산에서 빠진다.
    -- 카드 상세 화면이 정보로만 보여준다.
    benefit_kind         VARCHAR(20)   NOT NULL COMMENT '혜택 종류: DISCOUNT(할인) | POINT(적립) | SPECIAL_PRICE(특가) | GIFT(증정) | INSTALLMENT_FREE(무이자할부) | RETROACTIVE(사후정산). 뒤 셋은 계산 제외',
    calc_method          VARCHAR(15)   NOT NULL COMMENT '계산 방식: RATE(정률) | FIXED(정액) | COUNT_STEP(N회마다 정액)',
    benefit_value        DECIMAL(10,2) NOT NULL COMMENT 'RATE면 퍼센트(10.00 = 10%), FIXED·COUNT_STEP이면 금액(원, 소수부 미사용)',
    -- "5회 이용할 때마다 3천 포인트" 같은 누적 횟수 기반 혜택용.
    -- FIXED로 넣으면 결제마다 지급되어 실제의 step_count배가 된다(에러 없이 금액만 틀림).
    step_count           INT           NULL COMMENT 'COUNT_STEP일 때 몇 회마다 지급하는지',
    -- CASHBACK은 청구서에서 빼는 대신 결제계좌로 돈이 들어온다. 금액 계산은 청구할인과 같다.
    apply_timing         VARCHAR(20)   NULL COMMENT '할인 시점: IMMEDIATE(즉시) | BILLED(청구) | CASHBACK(결제계좌 입금). benefit_kind=DISCOUNT일 때만 값',

    target_type          VARCHAR(20)   NOT NULL COMMENT '대상 유형: CATEGORY | MERCHANT | ALL(전 가맹점)',
    target_category_id   BIGINT        NULL COMMENT 'target_type=CATEGORY일 때만. 대분류를 넣으면 하위 중분류까지 적용',
    target_merchant_id   BIGINT        NULL COMMENT 'target_type=MERCHANT일 때만',

    -- require_performance='Y'의 충족 기준을 여기서 못 박는다.
    -- 0원 구간 필수 규칙 때문에 "구간 못 찾음 = 미충족"이 성립하지 않는다.
    require_performance  CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '전월실적 조건 필요 여부: Y | N. 충족 기준 = 판정된 구간의 min_performance_amount > 0',
    -- 한 카드가 월 실적과 분기 실적을 함께 쓰는 경우가 있다(일상 영역은 전월, 특정 영역은 전분기).
    -- 분기 실적은 따로 저장하지 않고 user_card_monthly_state의 직전 3개월을 합산해 구한다.
    performance_period   VARCHAR(10)   NOT NULL DEFAULT 'MONTH' COMMENT '실적 기준 기간: MONTH(전월) | QUARTER(전분기)',
    -- 요일·시간대·채널 조건이 필요해지면 benefit_condition(benefit_id, condition_type,
    -- condition_value) 테이블로 확장하고 이 컬럼을 흡수한다 (benefit_exclusion과 대칭).
    -- 표준값을 정해두지 않으면 같은 조건이 카드마다 다른 문자열이 되어 엔진이 매칭하지 못한다.
    -- (실제로 "자동납부"가 자동납부/자동이체/AUTO_TRANSFER/AUTO_PAYMENT 넷으로 갈렸다.)
    --   AUTO_TRANSFER  자동이체·자동납부 등록 건
    --   SIMPLE_PAY     간편결제 일반 (브랜드를 가리지 않을 때)
    --   브랜드 지정이 필요하면 아래 값을 쓴다. 약관이 여러 개를 열거하면 대상마다 행을 쪼갠다.
    --   SAMSUNG_PAY / LG_PAY / KB_PAY / NAVER_PAY / KAKAO_PAY / PAYCO
    --   SSG_PAY / L_PAY / SOL_PAY / COUPAY / SMILE_PAY
    -- expense.payment_type에 같은 값이 들어와야 판정된다. 브랜드가 안 들어오는 환경에서는
    -- SIMPLE_PAY로만 판정되고, 그러면 열거되지 않은 간편결제까지 혜택을 받는다.
    require_payment_type VARCHAR(30)   NULL COMMENT '특정 결제수단에서만 적용. NULL이면 수단 무관. 표준값은 위 주석 참조',
    min_txn_amount       BIGINT        NULL COMMENT '건당 최소 결제금액(원). 미만이면 혜택 없음',

    max_eligible_amount  BIGINT        NULL COMMENT '혜택 대상 금액 상한(원). 이 금액까지만 율을 곱한다',
    max_benefit_per_txn  BIGINT        NULL COMMENT '건당 최대 혜택액(원)',
    monthly_limit        BIGINT        NULL COMMENT '이 혜택의 월 개별 한도(원). NULL=한도 없음',
    -- "통신·공과금·마트 각 10%, 합쳐서 월 5천원" 같은 묶음 한도용.
    -- 안 묶으면 한도가 혜택 수만큼 배로 샌다(에러 없이 금액만 틀림).
    -- 금액뿐 아니라 횟수도 함께 묶는다. "렌탈 5건 / 생활월납 3건"처럼 여러 대상이
    -- 건수를 공유하는 약관이 흔하고, 금액만 묶으면 횟수가 대상 수만큼 배로 샌다.
    limit_group_code     VARCHAR(30)   NULL COMMENT '묶음 한도 코드. 같은 카드 내 같은 코드끼리 monthly_limit·횟수 한도를 함께 공유',
    monthly_count_limit  INT           NULL COMMENT '월 최대 적용 횟수',
    daily_count_limit    INT           NULL COMMENT '일 최대 적용 횟수',
    -- 월·일과 축이 다르다. "영화 연 12회"처럼 연 단위로만 걸리는 한도가 실제로 있다.
    -- 연 소진량은 user_benefit_usage의 같은 해 월별 행을 합산해 구한다(별도 상태 없음).
    yearly_count_limit   INT           NULL COMMENT '연 최대 적용 횟수',
    quarterly_count_limit INT          NULL COMMENT '분기 최대 적용 횟수',
    -- 금액 한도도 월·일 말고 분기·연으로 걸리는 약관이 있다.
    -- 분기 한도를 monthly_limit에 넣으면 매달 리셋되어 실제의 세 배가 나간다(에러 없이 금액만 틀림).
    -- 소진량은 횟수와 마찬가지로 user_benefit_usage의 해당 기간 월별 행을 합산해 구한다.
    quarterly_limit      BIGINT        NULL COMMENT '분기 최대 혜택액(원)',
    yearly_limit         BIGINT        NULL COMMENT '연 최대 혜택액(원)',
    -- 금액을 묶는 범위와 횟수를 묶는 범위가 다를 때 쓴다.
    -- 약관 예) "택시·커피·영화관 합쳐 월 5천원"(금액) + "영화관 3사 합쳐 연 4회"(횟수).
    -- 하나로 묶으면 좁은 쪽 한도가 대상 수만큼 배로 샌다. NULL이면 limit_group_code를 따른다.
    count_group_code     VARCHAR(30)   NULL COMMENT '횟수 묶음 코드. NULL이면 limit_group_code 기준',
    -- 일 단위 금액 한도. 횟수(daily_count_limit)와 축이 다르다.
    -- 약관 예) "월 적립한도 3만점, 일 적립한도 1만점" — 횟수로는 표현할 수 없다.
    daily_limit          BIGINT        NULL COMMENT '일 최대 혜택액(원). NULL=일 한도 없음',
    use_shared_limit     CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '카드 통합할인한도를 함께 소진하는가: Y | N',

    -- 이 혜택을 받은 거래만 실적에서 빼는 약관이 흔하다.
    -- performance_exclusion의 TRANSACTION_ATTR='DISCOUNTED'로 적으면 다른 혜택을 받은 거래까지
    -- 실적에서 빠져 실적이 실제보다 낮게 잡힌다. 그래서 혜택 쪽에 둔다.
    -- 판정은 expense.applied_benefit_id로 한다.
    exclude_from_performance CHAR(1)   NOT NULL DEFAULT 'N' COMMENT '이 혜택이 적용된 거래를 전월실적에서 제외하는가: Y | N',
    -- 회원이 매월 하나를 고르는 혜택 묶음. 같은 코드끼리는 그달에 선택된 하나만 적용된다.
    -- 선택 상태는 user_card_benefit_selection에 있고, 코드가 NULL이면 선택과 무관하게 항상 적용된다.
    option_group_code    VARCHAR(30)   NULL COMMENT '선택형 혜택 묶음 코드. 같은 코드 중 선택된 하나만 적용',
    -- 선택지 하나가 혜택 여러 개로 이뤄지는 경우가 있다("배달팩을 고르면 4개 혜택이 함께 켜진다").
    -- 회원은 팩을 고르는 것이지 혜택 행을 고르는 게 아니므로 선택 단위를 따로 둔다.
    option_key           VARCHAR(30)   NULL COMMENT '선택지 식별자. 같은 option_group_code 안에서 이 혜택이 속한 선택지',

    -- 이 규칙이 어느 약관에서 나왔는지. 카드가 늘면 값의 근거를 사람이 기억할 수 없어
    -- 검수할 때 원문으로 되짚을 경로가 필요하다.
    source_document_id   BIGINT        NULL COMMENT '출처 약관 문서. 수기로 넣은 혜택이면 NULL',

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
    KEY idx_benefit_option_group (card_id, option_group_code),

    CONSTRAINT fk_benefit_card     FOREIGN KEY (card_id)            REFERENCES card (card_id),
    CONSTRAINT fk_benefit_category FOREIGN KEY (target_category_id) REFERENCES category (category_id),
    CONSTRAINT fk_benefit_merchant FOREIGN KEY (target_merchant_id) REFERENCES merchant (merchant_id),
    CONSTRAINT fk_benefit_source_document FOREIGN KEY (source_document_id) REFERENCES card_term_document (card_term_document_id),

    -- 대상 컬럼은 target_type에 맞는 것 정확히 하나만 채운다
    CONSTRAINT ck_benefit_target CHECK (
        (target_type = 'CATEGORY' AND target_category_id IS NOT NULL AND target_merchant_id IS NULL)
     OR (target_type = 'MERCHANT' AND target_merchant_id IS NOT NULL AND target_category_id IS NULL)
     OR (target_type = 'ALL'      AND target_category_id IS NULL     AND target_merchant_id IS NULL)
    ),
    -- 몇 회마다 주는지 모르면 계산할 수 없다.
    CONSTRAINT ck_benefit_step_count CHECK (
        (calc_method = 'COUNT_STEP' AND step_count IS NOT NULL)
     OR (calc_method <> 'COUNT_STEP' AND step_count IS NULL)
    ),
    -- 할인 시점은 할인 혜택에만 있다. 적립·특가·증정·사후정산은 NULL이어야 한다.
    -- 할인인데 NULL인 것은 허용한다 — 약관이 즉시/청구를 밝히지 않는 경우가 실제로 많고
    -- (카드 15장 중 33개 혜택), 기본값을 정해 채우면 약관에 없는 값을 만드는 것이 된다.
    -- 엔진은 이 값을 계산에 쓰지 않는다(표시용).
    CONSTRAINT ck_benefit_apply_timing CHECK (
        benefit_kind = 'DISCOUNT' OR apply_timing IS NULL
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 규칙';

-- 약관 예) "전월 30만원 이상 1%, 60만원 이상 2%, 한도도 각각 5천원/1만원"
--   → benefit 1행 + 이 테이블 2행. 혜택을 구간 수만큼 쪼개지 않는다.
-- NULL의 의미: 그 구간에서는 benefit의 기본값을 그대로 쓴다(상속).
CREATE TABLE benefit_tier_limit (
    benefit_id         BIGINT        NOT NULL COMMENT '혜택 ID',
    tier_id            BIGINT        NOT NULL COMMENT '실적구간 ID',
    tier_monthly_limit BIGINT        NULL COMMENT '이 구간에서의 월 개별 한도(원). NULL이면 benefit.monthly_limit 사용',
    -- 구간별 한도도 기간 축이 갈린다. 분기 실적을 쓰는 혜택은 한도도 분기로 걸린다
    -- ("전분기 100만원 이상 분기 1만점, 200만원 이상 1만5천점").
    -- 이 값을 tier_monthly_limit에 넣으면 매달 리셋되어 실제의 세 배가 나간다.
    tier_quarterly_limit BIGINT      NULL COMMENT '이 구간에서의 분기 개별 한도(원). NULL이면 benefit.quarterly_limit 사용',
    tier_yearly_limit  BIGINT        NULL COMMENT '이 구간에서의 연 개별 한도(원). NULL이면 benefit.yearly_limit 사용',
    tier_benefit_value DECIMAL(10,2) NULL COMMENT '이 구간에서의 혜택값(율 또는 정액). NULL이면 benefit.benefit_value 사용',
    PRIMARY KEY (benefit_id, tier_id),
    KEY idx_benefit_tier_limit_tier (tier_id),
    CONSTRAINT fk_benefit_tier_limit_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id),
    CONSTRAINT fk_benefit_tier_limit_tier    FOREIGN KEY (tier_id)    REFERENCES performance_tier (tier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 개별한도·혜택값';

-- 약관 예) "외식 5% (단, 배달앱 제외)"
--   → benefit은 대분류 외식을 겨냥하고, 여기에 CATEGORY 'DELIVERY' 한 행.
-- performance_exclusion과 동일한 (유형, 값) 패턴이다.
-- MERCHANT_LOCATION은 "백화점 입점 매장 제외"처럼 같은 브랜드 안에서 매장 위치로 가르는 조건이다.
--   IN_DEPARTMENT_STORE  백화점 입점        IN_LARGE_MART    대형마트·할인점 입점
--   IN_SHOPPING_MALL     쇼핑몰 임대매장     IN_TRANSIT_HUB   기차역·지하철역·공항 입점
-- 표현은 가능하되 지금은 판정할 수 없다 — merchant가 브랜드 단위라 지점을 구분하지 않고,
-- expense에도 매장 위치가 남지 않는다. 시드에 넣으면 조용히 무시되므로 넣지 않는다.
-- 마이데이터로 "GS25 신세계백화점점" 같은 가맹점명이 들어오면 그때 판정 경로가 생긴다.
CREATE TABLE benefit_exclusion (
    benefit_id      BIGINT      NOT NULL COMMENT '혜택 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR | MERCHANT_LOCATION',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값 (CATEGORY면 category_code, MERCHANT면 merchant_code)',
    PRIMARY KEY (benefit_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_benefit_exclusion_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 적용 예외';

-- 약관의 "할인서비스 제외 대상"처럼 카드의 모든 혜택에 공통으로 걸리는 예외다.
-- benefit_exclusion은 혜택 하나에 걸리는 예외라, 카드 공통 예외를 거기 넣으면
-- 혜택 수만큼 같은 행이 복제된다(카드 한 장에서 수백 행까지 늘어난다).
-- 그보다 나쁜 것은 혜택을 새로 추가할 때 그 복제를 빠뜨리면 그 혜택만 조용히 예외가 안 걸리는 것이다.
-- 엔진은 혜택을 적용하기 전에 이 표와 benefit_exclusion을 함께 확인한다.
CREATE TABLE card_benefit_exclusion (
    card_id         BIGINT      NOT NULL COMMENT '카드 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR | MERCHANT_LOCATION',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값 (CATEGORY면 category_code, MERCHANT면 merchant_code)',
    PRIMARY KEY (card_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_card_benefit_exclusion_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 전체 혜택 적용 예외';

-- 회원이 매월 고르는 혜택. 약관 예) "의료 20% 또는 생활 10% 중 택 1, 매월 변경 가능"
-- 고른 값은 회원마다 다르고 달마다 바뀌므로 카드 쪽(benefit)이 아니라 여기에 둔다.
-- 선택 기록이 없는 달은 그 묶음의 혜택이 하나도 적용되지 않는다.
CREATE TABLE user_card_benefit_selection (
    user_card_id      BIGINT  NOT NULL COMMENT '보유카드 ID',
    -- 다른 월 상태 테이블(user_card_monthly_state·user_benefit_usage)과 같은 형식이다.
    -- 같은 개념에 형식이 둘이면 조회 키가 에러 없이 안 맞아 선택이 없는 것으로 읽힌다.
    base_year_month   CHAR(7) NOT NULL COMMENT '기준 연월 (YYYY-MM)',
    option_group_code VARCHAR(30) NOT NULL COMMENT '선택형 혜택 묶음 코드',
    -- 혜택 ID가 아니라 선택지를 저장한다. 선택지 하나가 혜택 여러 개로 이뤄지는 경우가 있어
    -- ("배달팩을 고르면 4개 혜택이 함께 켜진다") 혜택 ID로는 고른 것을 담지 못한다.
    -- 엔진은 benefit.option_key가 이 값과 같은 혜택만 그달에 적용한다.
    selected_option_key VARCHAR(30) NOT NULL COMMENT '그달에 선택한 선택지 (benefit.option_key와 대응)',
    -- 한 묶음에서 한 달에 하나만 고를 수 있다는 규칙을 PK로 못 박는다.
    PRIMARY KEY (user_card_id, base_year_month, option_group_code),
    CONSTRAINT fk_user_card_benefit_selection_card FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '선택형 혜택의 월별 선택 상태';

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
    approval_status    VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT '승인 단계: APPROVED | CONFIRMED',
    transaction_type   VARCHAR(30)  NOT NULL DEFAULT 'LUMP_SUM' COMMENT '거래 구분: LUMP_SUM | INSTALLMENT | CASH_ADVANCE',
    region             VARCHAR(20)  NOT NULL DEFAULT 'DOMESTIC' COMMENT '이용 지역: DOMESTIC | OVERSEAS',
    applied_benefit_id BIGINT       NULL COMMENT '적용된 혜택 ID. 엔진이 채운다. 카드당 1개만 적용되므로 단수',
    discount_amount    BIGINT       NOT NULL DEFAULT 0 COMMENT '실제 받은 할인/적립액. 엔진이 채운다',
    -- 전월실적 계산에 필요하다. 카드 약관은 특정 결제수단과 무이자할부를
    -- 실적에서 빼는 게 기본이라, 이 두 값이 없으면 실적이 과다 계산된다.
    payment_type       VARCHAR(30)  NULL COMMENT '결제수단 (CARD, SIMPLE_PAY 등). 실적 제외 판정용',
    is_interest_free   CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '무이자할부 여부 Y/N. 실적 제외 판정용',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (expense_id),
    KEY idx_expense_member_date (member_id, payment_date),
    KEY idx_expense_member_filters (member_id, payment_date, payment_status, approval_status, region),
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
-- QR 결제 요청의 발급·만료·사용·실패 상태를 저장한다.
CREATE TABLE payment_qr (
    payment_qr_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'QR 결제 ID',
    qr_token      VARCHAR(100) NOT NULL COMMENT '외부에 노출하는 QR 식별 토큰',
    member_id     BIGINT       NOT NULL COMMENT '회원 ID',
    user_card_id  BIGINT       NOT NULL COMMENT '결제에 사용할 보유카드 ID',
    payment_amount BIGINT      NOT NULL COMMENT 'QR 발급 시 확정한 결제금액',
    status        VARCHAR(20)  NOT NULL DEFAULT 'READY' COMMENT 'QR 상태: READY | USED | EXPIRED | FAILED',
    expires_at    DATETIME     NOT NULL COMMENT 'QR 만료 일시',
    used_at       DATETIME     NULL COMMENT 'QR 사용 일시',
    failed_at     DATETIME     NULL COMMENT '결제 실패 일시',
    fail_reason   VARCHAR(255) NULL COMMENT '결제 실패 사유',
    payment_id    BIGINT       NULL COMMENT '완료된 결제 ID',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (payment_qr_id),
    UNIQUE KEY uk_payment_qr_token (qr_token),
    KEY idx_payment_qr_token (qr_token),
    KEY idx_payment_qr_member_status (member_id, status),
    KEY idx_payment_qr_card (user_card_id),
    KEY idx_payment_qr_payment (payment_id),
    CONSTRAINT fk_payment_qr_member  FOREIGN KEY (member_id)    REFERENCES member (member_id),
    CONSTRAINT fk_payment_qr_card    FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id),
    CONSTRAINT fk_payment_qr_payment FOREIGN KEY (payment_id)   REFERENCES payment (payment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT 'QR 결제 상태';

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
    -- 멤버십 상세의 "공식 사이트 이동" 버튼이 쓰는 주소.
    -- 제휴사 로그인 주소는 OAuth 파라미터가 통째로 붙는 경우가 있어 255자로는 잘린다
    -- (현재 최장은 뷰티포인트 1157자). 잘린 URL은 열리지 않으므로 넉넉히 잡는다.
    official_site_url    VARCHAR(1500) NULL COMMENT '공식 사이트 URL',
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

-- 개인화 설정 화면에서 직접 입력한 브랜드 문자열을 저장한다.
CREATE TABLE member_personalization_category (
    member_id    BIGINT      NOT NULL COMMENT '회원 ID',
    category_key VARCHAR(30) NOT NULL COMMENT '프론트 개인화 카테고리 키',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (member_id, category_key),
    CONSTRAINT fk_personalization_category_member
        FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 개인화 카테고리';

CREATE TABLE member_personalization_brand (
    member_id    BIGINT       NOT NULL COMMENT '회원 ID',
    category_key VARCHAR(30)  NOT NULL COMMENT '프론트 개인화 카테고리 키',
    priority     TINYINT      NOT NULL COMMENT '브랜드 표시 순서 1~3',
    brand_name   VARCHAR(100) NOT NULL COMMENT '관심 브랜드명',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    PRIMARY KEY (member_id, category_key, priority),
    UNIQUE KEY uk_personalization_brand_name (member_id, category_key, brand_name),
    CONSTRAINT fk_personalization_brand_category
        FOREIGN KEY (member_id, category_key)
        REFERENCES member_personalization_category (member_id, category_key)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 개인화 관심 브랜드';

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
