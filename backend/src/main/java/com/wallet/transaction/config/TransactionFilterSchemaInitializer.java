package com.wallet.transaction.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 개발 DB에서도 조회조건 컬럼을 자동 보강한다. */
@Component
public class TransactionFilterSchemaInitializer implements InitializingBean {
    private final JdbcTemplate jdbcTemplate;

    public TransactionFilterSchemaInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void afterPropertiesSet() {
        addColumnIfMissing(
            "approval_status",
            "ALTER TABLE expense ADD COLUMN approval_status VARCHAR(20) "
                + "NOT NULL DEFAULT 'APPROVED' AFTER payment_status"
        );
        addColumnIfMissing(
            "transaction_type",
            "ALTER TABLE expense ADD COLUMN transaction_type VARCHAR(30) "
                + "NOT NULL DEFAULT 'LUMP_SUM' AFTER approval_status"
        );
        addColumnIfMissing(
            "region",
            "ALTER TABLE expense ADD COLUMN region VARCHAR(20) "
                + "NOT NULL DEFAULT 'DOMESTIC' AFTER transaction_type"
        );

        Integer indexCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = 'expense' "
                + "AND index_name = 'idx_expense_member_filters'",
            Integer.class
        );
        if (indexCount != null && indexCount == 0) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_expense_member_filters ON expense "
                    + "(member_id, payment_date, payment_status, approval_status, region)"
            );
        }

        jdbcTemplate.update(
            "UPDATE expense SET approval_status = 'CONFIRMED' "
                + "WHERE input_type = 'PAYMENT' AND approval_status = 'APPROVED'"
        );
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'expense' AND column_name = ?",
            Integer.class,
            columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
