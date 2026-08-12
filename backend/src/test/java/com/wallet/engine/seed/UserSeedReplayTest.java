package com.wallet.engine.seed;

import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.seed.UserSeedScenario.Holding;
import com.wallet.engine.seed.UserSeedScenario.Selection;
import com.wallet.engine.seed.UserSeedScenario.Txn;
import com.wallet.engine.service.SettlementService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 사용자 시드 생성기 — 거래를 엔진에 흘려보내 계산 결과까지 채운 뒤 INSERT 문으로 뽑는다.
 *
 * <b>테스트가 아니라 도구다.</b> 검증하는 것이 없어 평소에는 실행하지 않는다(@Disabled).
 * 시드를 다시 만들 때 이 클래스만 골라 실행한다:
 * <pre>
 *   ./mvnw -o test -Dtest=UserSeedReplayTest -DfailIfNoTests=false  *          -Dsurefire.failIfNoSpecifiedTests=false  *          -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.engine.extension.DisabledCondition
 * </pre>
 * 마지막 옵션이 없으면 클래스를 골라 지정해도 &#64;Disabled 때문에 건너뛴다(Skipped 1).
 *
 * <b>선행 조건</b>: DB에 schema → data → 91 까지만 들어 있어야 한다. 이 도구는 보유카드·소비내역을
 * id를 직접 매겨 넣으므로, 이전 92가 적용된 DB에서 돌리면 중복 키로 실패한다.
 *
 * <b>왜 손으로 안 적고 리플레이하나.</b> 소비내역의 적용 혜택·할인액, 월 실적, 한도 소진은
 * 사람이 맞출 수 없다 — 묶음 한도 합산, 구간별 한도 상속, 일 소진 리셋, 스탬프 진행이 서로 얽힌다.
 * 예전에 손으로 넣은 시드는 이용률 116%, 통합한도 0인데 소진 7,500 같은 상태가 남아 있었다.
 * 엔진이 만든 값은 정의상 엔진 규칙과 어긋날 수 없다.
 *
 * <b>DB를 더럽히지 않는다.</b> @Transactional 롤백이라 넣은 행은 사라진다.
 * 롤백 전에 SELECT해서 파일로 뽑고, 그 파일을 시드로 쓴다.
 *
 * 이 리플레이 기계는 <b>미보유 카드 추천에 그대로 재사용된다</b> —
 * "지난달 내 거래를 이 카드로 전부 다시 결제했다면?"이 정확히 같은 계산이다.
 * 그때도 금액을 월 단위로 합산하면 안 된다. 거래를 하나씩 넣으며 한도를 소진시켜야 맞는다.
 */
@Disabled("시드 생성 도구 — 시드를 다시 만들 때만 수동 실행")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class UserSeedReplayTest {

    private static final Path OUTPUT = Path.of("db", "92_seed_user_data.sql");

    /** MySQL DATETIME 리터럴 형식. LocalDateTime의 기본 toString()은 ISO-8601이라 'T'가 들어간다 */
    private static final java.time.format.DateTimeFormatter DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SettlementService settlementService;

    private JdbcTemplate jdbc;

    @Autowired
    void setDataSource(javax.sql.DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void 사용자_시드_생성() throws IOException {
        Map<String, Long> cardIds = lookup("SELECT card_name, card_id FROM card");
        Map<String, Long> merchantIds = lookup("SELECT merchant_code, merchant_id FROM merchant");
        Map<String, Long> categoryIds = lookup("SELECT category_code, category_id FROM category");

        Map<String, Long> userCardIds = insertHoldings(cardIds);
        // 선택은 반드시 리플레이 앞이다 — 엔진이 결제를 계산하면서 그달의 선택을 읽는다.
        // 뒤에 넣으면 선택이 없는 상태로 계산돼 선택형 혜택이 한 건도 적용되지 않은 시드가 나온다.
        insertSelections(userCardIds);
        int applied = replay(cardIds, merchantIds, categoryIds, userCardIds);

        String sql = dump();
        Files.writeString(OUTPUT, sql, StandardCharsets.UTF_8);
        System.out.printf("생성: %s (%,d자) — 거래 %d건, 혜택 적용 %d건%n",
                OUTPUT.toAbsolutePath(), sql.length(), UserSeedScenario.transactions().size(), applied);
    }

    /** 코드 → id 사전. 이름으로 지정한 시나리오를 실제 id로 옮긴다 */
    private Map<String, Long> lookup(String sql) {
        Map<String, Long> map = new HashMap<>();
        jdbc.query(sql, rs -> {
            map.put(rs.getString(1), rs.getLong(2));
        });
        return map;
    }

    /**
     * 보유 카드를 넣고 (회원, 카드명) → user_card_id 를 돌려준다.
     *
     * id를 AUTO_INCREMENT에 맡기지 않고 직접 매긴다. 이 도구를 여러 번 돌리면 롤백을 해도
     * AUTO_INCREMENT는 되돌아가지 않아, 돌릴 때마다 다른 id로 시드가 만들어진다.
     */
    private Map<String, Long> insertHoldings(Map<String, Long> cardIds) {
        Map<String, Long> userCardIds = new HashMap<>();
        long userCardId = 0;
        for (Holding holding : UserSeedScenario.holdings()) {
            Long cardId = require(cardIds, holding.cardName(), "카드");
            userCardId++;
            // 마스킹 번호는 카드마다 다르게 두되 규칙적으로 만든다 — 화면에서 카드를 구분하는 값이다
            String masked = "****-****-****-%04d".formatted(1000 + cardId);
            jdbc.update("""
                    INSERT INTO user_card (user_card_id, member_id, card_id,
                                           masked_card_number, is_representative)
                    VALUES (?, ?, ?, ?, ?)
                    """, userCardId, holding.memberId(), cardId, masked,
                    holding.representative() ? 1 : 0);
            userCardIds.put(key(holding.memberId(), holding.cardName()), userCardId);
        }
        return userCardIds;
    }

    /**
     * 선택형 혜택의 월별 선택을 넣는다 — 시나리오의 선택 하나를 거래가 있는 다섯 달에 모두 깐다.
     *
     * 달마다 행이 필요한 것은 선택 상태의 PK가 (보유카드, 연월, 묶음)이기 때문이다.
     * 한 달치만 넣으면 나머지 달은 "고르지 않음"이 되어 그 카드의 묶음 혜택이 통째로 빠진다.
     */
    private void insertSelections(Map<String, Long> userCardIds) {
        for (Selection selection : UserSeedScenario.selections()) {
            Long userCardId = require(userCardIds,
                    key(selection.memberId(), selection.cardName()), "보유 카드");
            for (int month : UserSeedScenario.MONTHS) {
                jdbc.update("""
                        INSERT INTO user_card_benefit_selection (user_card_id, base_year_month,
                                                                 option_group_code, selected_option_key)
                        VALUES (?, ?, ?, ?)
                        """, userCardId, "%d-%02d".formatted(UserSeedScenario.YEAR, month),
                        selection.optionGroupCode(), selection.optionKey());
            }
        }
    }

    /**
     * 거래를 시간순으로 엔진에 흘려보낸다.
     *
     * 순서가 중요하다 — 전월실적 이월(월 롤오버)과 한도 소진이 앞선 거래의 결과에 달려 있어,
     * 뒤섞어 넣으면 실제로 일어날 수 없는 상태가 만들어진다.
     */
    private int replay(Map<String, Long> cardIds, Map<String, Long> merchantIds,
                       Map<String, Long> categoryIds, Map<String, Long> userCardIds) {
        int applied = 0;
        long expenseId = 0;
        for (Txn txn : UserSeedScenario.transactions()) {
            Long cardId = require(cardIds, txn.cardName(), "카드");
            Long userCardId = userCardIds.get(key(txn.memberId(), txn.cardName()));
            if (userCardId == null) {
                throw new IllegalStateException(
                        "보유하지 않은 카드로 결제한 거래가 있다: 회원 %d / %s"
                                .formatted(txn.memberId(), txn.cardName()));
            }
            Long categoryId = require(categoryIds, txn.categoryCode(), "카테고리");
            Long merchantId = txn.merchantCode() == null
                    ? null
                    : require(merchantIds, txn.merchantCode(), "가맹점");

            LocalDateTime paidAt = LocalDateTime.of(
                    UserSeedScenario.YEAR, txn.month(), txn.day(), txn.hour(), 0);

            PaymentSettlementResult result = settlementService.applyPayment(new SettlementCommand(
                    userCardId, cardId, categoryId, merchantId, txn.amount(),
                    txn.paymentType(), txn.interestFree(), paidAt));

            // 엔진은 expense에 쓰지 않는다 — 계산 결과를 돌려주면 소비내역 쪽이 저장한다.
            // 여기서는 그 소비내역 역할을 이 도구가 대신한다.
            expenseId++;
            jdbc.update("""
                    INSERT INTO expense (expense_id, member_id, user_card_id, category_id,
                                         merchant_id, merchant_name,
                                         amount, payment_date, input_type, payment_status,
                                         applied_benefit_id, discount_amount, payment_type, is_interest_free)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAYMENT', 'APPROVED', ?, ?, ?, ?)
                    """,
                    expenseId, txn.memberId(), userCardId, categoryId, merchantId, merchantName(merchantId),
                    txn.amount(), paidAt, result.appliedBenefitId(), result.discountAmount(),
                    txn.paymentType(), txn.interestFree() ? "Y" : "N");

            if (result.appliedBenefitId() != null) {
                applied++;
            }
        }
        return applied;
    }

    private String merchantName(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        return jdbc.queryForObject(
                "SELECT merchant_name FROM merchant WHERE merchant_id = ?", String.class, merchantId);
    }

    private Long require(Map<String, Long> ids, String key, String what) {
        Long id = ids.get(key);
        if (id == null) {
            // 조용히 넘기면 그 거래만 빠진 시드가 만들어지고, 왜 혜택이 안 붙는지 알 수 없게 된다
            throw new IllegalStateException("%s를 찾을 수 없다: %s".formatted(what, key));
        }
        return id;
    }

    private String key(long memberId, String cardName) {
        return memberId + "/" + cardName;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 덤프 — 롤백되기 전에 읽어 INSERT 문으로 옮긴다
    // ─────────────────────────────────────────────────────────────────────

    private String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                -- ============================================================
                -- 92_seed_user_data.sql — 보유카드·소비내역·계산 상태
                --
                -- 손으로 고치지 마라. UserSeedReplayTest가 만든다.
                -- 거래를 엔진(SettlementService)에 시간순으로 흘려보내 나온 결과라,
                -- 적용 혜택·할인액·월 실적·한도 소진이 엔진 규칙과 어긋날 수 없다.
                -- 값을 손으로 바꾸면 그 보장이 사라진다 — 시나리오를 고치고 다시 생성해라.
                --
                -- 실행 순서: schema.sql → data.sql → 91_seed_card_benefit.sql → 이 파일
                -- ============================================================

                SET NAMES utf8mb4;

                START TRANSACTION;

                """);

        sb.append(table("user_card",
                "SELECT user_card_id, member_id, card_id, masked_card_number, is_representative, card_status "
                        + "FROM user_card ORDER BY user_card_id",
                "user_card_id, member_id, card_id, masked_card_number, is_representative, card_status"));

        // user_card 바로 뒤에 둔다 — 보유카드를 참조하는 FK라 순서가 뒤바뀌면 적재가 실패한다
        sb.append(table("user_card_benefit_selection",
                "SELECT user_card_id, base_year_month, option_group_code, selected_option_key "
                        + "FROM user_card_benefit_selection "
                        + "ORDER BY user_card_id, base_year_month, option_group_code",
                "user_card_id, base_year_month, option_group_code, selected_option_key"));

        sb.append(table("expense",
                "SELECT expense_id, member_id, user_card_id, category_id, merchant_id, merchant_name, "
                        + "amount, payment_date, input_type, payment_status, applied_benefit_id, "
                        + "discount_amount, payment_type, is_interest_free FROM expense ORDER BY expense_id",
                "expense_id, member_id, user_card_id, category_id, merchant_id, merchant_name, "
                        + "amount, payment_date, input_type, payment_status, applied_benefit_id, "
                        + "discount_amount, payment_type, is_interest_free"));

        sb.append(table("user_card_monthly_state",
                "SELECT user_card_id, base_year_month, prev_performance_amount, "
                        + "current_performance_amount, shared_limit_used FROM user_card_monthly_state "
                        + "ORDER BY user_card_id, base_year_month",
                "user_card_id, base_year_month, prev_performance_amount, "
                        + "current_performance_amount, shared_limit_used"));

        sb.append(table("user_benefit_usage",
                "SELECT user_card_id, benefit_id, base_year_month, used_amount, used_count, "
                        + "last_applied_date, daily_used_count, daily_used_amount FROM user_benefit_usage "
                        + "ORDER BY user_card_id, base_year_month, benefit_id",
                "user_card_id, benefit_id, base_year_month, used_amount, used_count, "
                        + "last_applied_date, daily_used_count, daily_used_amount"));

        sb.append("COMMIT;\n");
        return sb.toString();
    }

    private String table(String tableName, String selectSql, String columns) {
        List<String> rows = jdbc.query(selectSql, (rs, rowNum) -> {
            int count = rs.getMetaData().getColumnCount();
            StringJoiner values = new StringJoiner(", ", "    (", ")");
            for (int i = 1; i <= count; i++) {
                values.add(literal(rs.getObject(i)));
            }
            return values.toString();
        });
        if (rows.isEmpty()) {
            return "";
        }
        return "INSERT INTO %s (%s) VALUES\n%s;\n\n".formatted(tableName, columns, String.join(",\n", rows));
    }

    /**
     * SQL 리터럴.
     *
     * Boolean과 날짜/시간을 따로 다룬다. 그냥 toString()을 쓰면 TINYINT(1) 칸에 {@code 'true'}가,
     * DATETIME 칸에 ISO-8601의 {@code '2026-04-02T08:00'}이 들어가 적재가 깨진다.
     */
    private String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean flag) {
            return flag ? "1" : "0";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return "'" + dateTime.format(DATE_TIME) + "'";
        }
        if (value instanceof java.time.LocalDate date) {
            return "'" + date + "'";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
