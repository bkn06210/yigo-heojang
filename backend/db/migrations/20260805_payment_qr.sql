-- QR 결제 API용 상태 테이블. 기존 데이터와 테이블은 유지한다.
CREATE TABLE IF NOT EXISTS payment_qr (
    payment_qr_id BIGINT       NOT NULL AUTO_INCREMENT,
    qr_token      VARCHAR(100) NOT NULL,
    member_id     BIGINT       NOT NULL,
    user_card_id  BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    expires_at    DATETIME     NOT NULL,
    used_at       DATETIME     NULL,
    failed_at     DATETIME     NULL,
    fail_reason   VARCHAR(255) NULL,
    payment_id    BIGINT       NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_qr_id),
    UNIQUE KEY uk_payment_qr_token (qr_token),
    KEY idx_payment_qr_token (qr_token),
    KEY idx_payment_qr_member_status (member_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'QR 결제 상태';
