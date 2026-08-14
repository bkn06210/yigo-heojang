-- ============================================================
-- 간편비밀번호 기능 마이그레이션
--
-- schema.sql은 DROP 후 전부 새로 만드는 스크립트라, 이미 데이터가 들어있는 DB에는
-- 그대로 돌릴 수 없다. 이 파일은 그런 DB에 간편비밀번호 기능에 필요한 변경만 얹기 위한 것이다.
-- 새로 세팅하는 DB라면 이 파일 대신 schema.sql을 쓰면 된다 (이미 반영되어 있다).
--
-- 여러 번 실행해도 안전하도록 작성했다.
-- 실행: mysql -u root -p {DB이름} < db/migration_simple_password.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. member에 간편비밀번호 관련 컬럼 3개 추가
--    simple_password_hash는 기존 회원이 아직 설정하지 않은 상태라 NULL을 허용한다.
--    나머지 둘은 검증 실패 횟수 제한(5회 실패 시 5분 잠금)에 쓴다.
-- ------------------------------------------------------------
SET @has_hash = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member'
      AND COLUMN_NAME = 'simple_password_hash'
);

SET @sql = IF(@has_hash = 0,
    'ALTER TABLE member
        ADD COLUMN simple_password_hash VARCHAR(255) NULL
            COMMENT ''간편비밀번호 해시'' AFTER password_hash,
        ADD COLUMN simple_password_failed_attempt_count INT NOT NULL DEFAULT 0
            COMMENT ''간편비밀번호 연속 검증 실패 횟수'' AFTER simple_password_hash,
        ADD COLUMN simple_password_locked_until DATETIME NULL
            COMMENT ''간편비밀번호 검증 잠금 만료일시'' AFTER simple_password_failed_attempt_count',
    'SELECT ''member의 간편비밀번호 컬럼이 이미 존재합니다'' AS result');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ------------------------------------------------------------
-- 2. simple_password_verification: 간편비밀번호 설정·변경 전 이메일 인증 테이블.
--    인증 코드와 변경 토큰은 원문 대신 SHA-256 해시만 저장한다.
--    코드(5분)와 토큰(10분)의 유효기간이 다르므로 만료일시를 각각 관리한다.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS simple_password_verification (
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


-- ------------------------------------------------------------
-- 3. 데모 계정(active@example.com)에 간편비밀번호 '123456'을 설정한다.
--    결제 화면이 실제 검증을 하므로, 설정된 계정이 하나도 없으면 결제를 눌러볼 수 없다.
--    이미 설정한 회원의 값은 덮어쓰지 않는다.
-- ------------------------------------------------------------
UPDATE member
SET simple_password_hash = '$2a$10$ecygc6FyzyxHoUi8twQNJe8Qk1sXaIX7.Vpc5wPe6cjpHXa0WvWMy'
WHERE email = 'active@example.com'
  AND simple_password_hash IS NULL;


-- ------------------------------------------------------------
-- 4. 확인용 조회
-- ------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'member'
  AND COLUMN_NAME LIKE 'simple_password%'
ORDER BY ORDINAL_POSITION;
