ALTER TABLE expense
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
        COMMENT '승인 단계: APPROVED | CONFIRMED' AFTER payment_status,
    ADD COLUMN transaction_type VARCHAR(30) NOT NULL DEFAULT 'LUMP_SUM'
        COMMENT '거래 구분: LUMP_SUM | INSTALLMENT | CASH_ADVANCE' AFTER approval_status,
    ADD COLUMN region VARCHAR(20) NOT NULL DEFAULT 'DOMESTIC'
        COMMENT '이용 지역: DOMESTIC | OVERSEAS' AFTER transaction_type;

CREATE INDEX idx_expense_member_filters
    ON expense (member_id, payment_date, payment_status, approval_status, region);

UPDATE expense SET approval_status = 'CONFIRMED' WHERE input_type = 'PAYMENT';
