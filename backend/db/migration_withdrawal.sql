-- ============================================================
-- 회원 탈퇴 기능 마이그레이션
--
-- schema.sql / data.sql은 DROP 후 전부 새로 만드는 스크립트라, 이미 데이터가 들어있는
-- DB에는 그대로 돌릴 수 없다. 이 파일은 그런 DB에 탈퇴 기능에 필요한 변경만 얹기 위한 것이다.
-- 새로 세팅하는 DB라면 이 파일 대신 schema.sql + data.sql을 쓰면 된다 (이미 반영되어 있다).
--
-- 여러 번 실행해도 안전하도록 작성했다.
-- 실행: mysql -u root -p {DB이름} < db/migration_withdrawal.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. term.term_scope: 약관이 가입 화면용인지 탈퇴 화면용인지 구분한다.
--    DEFAULT 'SIGNUP'이라 기존 약관은 전부 자동으로 가입용으로 남는다.
-- ------------------------------------------------------------
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'term'
      AND COLUMN_NAME = 'term_scope'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE term
        ADD COLUMN term_scope VARCHAR(20) NOT NULL DEFAULT ''SIGNUP''
        COMMENT ''약관 노출 시점: SIGNUP(가입 시) | WITHDRAWAL(탈퇴 시)''
        AFTER term_code',
    'SELECT ''term.term_scope 컬럼이 이미 존재합니다'' AS result');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ------------------------------------------------------------
-- 2. member_withdrawal_archive: 탈퇴 회원의 원본 개인정보 보관 테이블.
--    member의 email·name은 탈퇴 즉시 마스킹되므로 원본을 여기로 옮겨 담는다.
--    회원 삭제 후에도 남아야 하므로 물리 FK를 걸지 않는다 (논리 참조).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS member_withdrawal_archive (
    member_withdrawal_archive_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '보관 ID',
    member_id                    BIGINT       NOT NULL COMMENT '회원 ID (논리 참조)',
    email                        VARCHAR(255) NOT NULL COMMENT '탈퇴 시점 이메일 원본',
    email_hash                   CHAR(64)     NOT NULL COMMENT '이메일 SHA-256 해시. 재가입 제한 등 향후 정책 조회용',
    name                         VARCHAR(50)  NOT NULL COMMENT '탈퇴 시점 회원명 원본',
    withdrawn_at                 DATETIME     NOT NULL COMMENT '탈퇴일시',
    retention_reason             VARCHAR(100) NOT NULL COMMENT '보관 근거',
    created_at                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_withdrawal_archive_id),
    UNIQUE KEY uk_withdrawal_archive_member (member_id),
    KEY idx_withdrawal_archive_withdrawn_at (withdrawn_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '탈퇴회원 개인정보 보관';


-- ------------------------------------------------------------
-- 3. 탈퇴 고지 약관.
--    is_required = 1이라 이 고지에 동의하지 않으면 탈퇴를 진행할 수 없다.
--    term_id/term_version_id는 data.sql과 같은 5번을 쓴다.
-- ------------------------------------------------------------
INSERT INTO term (term_id, term_code, term_scope, term_name, is_required, term_status)
VALUES (5, 'WITHDRAWAL_NOTICE', 'WITHDRAWAL', '회원 탈퇴 안내 및 동의', 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    term_scope  = VALUES(term_scope),
    term_name   = VALUES(term_name),
    is_required = VALUES(is_required),
    term_status = VALUES(term_status);

INSERT INTO term_version (term_version_id, term_id, version, content, effective_started_at, effective_ended_at)
VALUES (5, 5, '2026-07-01',
        '1. 탈퇴 시 보유카드, 소비내역, 결제내역, 포인트 정보는 즉시 삭제되며 복구할 수 없습니다.
2. 이메일, 성명 등 회원정보는 소비자 분쟁 처리를 위해 탈퇴일로부터 3년간 보관 후 파기됩니다.
3. 탈퇴 후 동일 이메일로 재가입할 수 있으나, 이전 데이터는 복원되지 않습니다.',
        '2026-07-01 00:00:00', NULL)
ON DUPLICATE KEY UPDATE
    content              = VALUES(content),
    effective_started_at = VALUES(effective_started_at),
    effective_ended_at   = VALUES(effective_ended_at);


-- ------------------------------------------------------------
-- 4. 확인용 조회
-- ------------------------------------------------------------
SELECT term_id, term_code, term_scope, term_name, is_required, term_status FROM term ORDER BY term_id;
