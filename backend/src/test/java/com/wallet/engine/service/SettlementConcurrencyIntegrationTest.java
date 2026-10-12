package com.wallet.engine.service;

import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 카드로 결제 두 건이 동시에 정산될 때 월 한도를 넘지 않는지 검증하는 MySQL 통합 테스트.
 *
 * 두 트랜잭션이 실제로 겹쳐야 하므로 @Transactional 롤백을 쓰지 않는다 — 각 스레드가 자기 트랜잭션을
 * 커밋하고, 픽스처는 @AfterEach에서 지운다.
 *
 * 순서를 래치로 고정해 매번 같은 경합을 만든다:
 * <pre>
 * A: 소비내역 INSERT → 조회 → 정산(잔여 2,000원 전부 적용) → [커밋 대기]
 * B:                          소비내역 INSERT → 조회(스냅샷 고정) → 정산
 * A: 커밋
 * </pre>
 * B의 스냅샷은 A가 커밋하기 전에 고정된다. 잠금이 없거나 일반 SELECT로 소진을 읽으면
 * B는 A의 소진을 못 보고 같은 2,000원을 한 번 더 준다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
class SettlementConcurrencyIntegrationTest {

    private static final String BASE_MONTH = "2026-08";
    private static final long MONTHLY_LIMIT = 10_000L;
    private static final long USED_BEFORE = 8_000L;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;

    private long memberId;
    private long parentCategoryId;
    private long cafeCategoryId;
    private long merchantId;
    private long cardCompanyId;
    private long cardId;
    private long userCardId;
    private long benefitId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM expense WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM user_benefit_usage WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM user_card_monthly_state WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM benefit WHERE card_id = ?", cardId);
        jdbc.update("DELETE FROM performance_tier WHERE card_id = ?", cardId);
        jdbc.update("DELETE FROM user_card WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM card WHERE card_id = ?", cardId);
        jdbc.update("DELETE FROM card_company WHERE card_company_id = ?", cardCompanyId);
        jdbc.update("DELETE FROM merchant WHERE merchant_id = ?", merchantId);
        // 한 문장으로 지우면 행 단위 외래키 검사에서 부모가 먼저 걸릴 수 있어 자식부터 지운다
        jdbc.update("DELETE FROM category WHERE category_id = ?", cafeCategoryId);
        jdbc.update("DELETE FROM category WHERE category_id = ?", parentCategoryId);
        jdbc.update("DELETE FROM member WHERE member_id = ?", memberId);
    }

    @Test
    @DisplayName("동시 결제: 잔여 한도 2,000원을 두 결제가 나눠 가져도 합계가 월 한도를 넘지 않는다")
    void 동시_결제에도_월_한도를_넘지_않는다() throws Exception {
        CountDownLatch paymentAApplied = new CountDownLatch(1);
        CountDownLatch paymentBSnapshotTaken = new CountDownLatch(1);
        CountDownLatch commitA = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        try {
            Future<PaymentSettlementResult> paymentA = executor.submit(() -> tx.execute(status -> {
                PaymentSettlementResult result = payLikePaymentService();
                paymentAApplied.countDown();
                await(commitA);
                return result;
            }));

            Future<PaymentSettlementResult> paymentB = executor.submit(() -> {
                await(paymentAApplied);
                return tx.execute(status -> payLikePaymentService(paymentBSnapshotTaken));
            });

            // B의 스냅샷이 A 커밋 전에 고정된 뒤에 A를 커밋시킨다 — 경합을 매번 재현하기 위한 순서
            await(paymentBSnapshotTaken);
            commitA.countDown();

            long discountA = paymentA.get(10, TimeUnit.SECONDS).discountAmount();
            long discountB = paymentB.get(10, TimeUnit.SECONDS).discountAmount();

            // 잔여 2,000원은 한 번만 나간다. 먼저 정산한 A가 전부 받고 B는 0원
            assertThat(discountA).isEqualTo(2_000L);
            assertThat(discountB).isZero();
            assertThat(discountA + discountB).isEqualTo(MONTHLY_LIMIT - USED_BEFORE);
            assertThat(usedAmount()).isEqualTo(MONTHLY_LIMIT);
        } finally {
            executor.shutdownNow();
        }
    }

    /** 결제 트랜잭션과 같은 순서: 소비내역 INSERT → 일반 조회 → 정산. */
    private PaymentSettlementResult payLikePaymentService() {
        return payLikePaymentService(null);
    }

    private PaymentSettlementResult payLikePaymentService(CountDownLatch snapshotTaken) {
        // 외래키 검사로 user_card 행에 공유 잠금이 걸리는 것까지 실제 결제 흐름과 맞춘다
        jdbc.update("INSERT INTO expense (member_id, user_card_id, category_id, merchant_id, merchant_name, "
                        + "amount, payment_date, input_type, payment_type, is_interest_free, payment_status) "
                        + "VALUES (?, ?, ?, ?, 'IT동시성카페', 30000, '2026-08-15 12:00:00', 'PAYMENT', 'CARD', 'N', 'APPROVED')",
                memberId, userCardId, cafeCategoryId, merchantId);
        // 결제 서비스의 소유권 확인과 같은 일반 SELECT — 이 시점에 REPEATABLE READ 스냅샷이 고정된다
        jdbc.queryForObject("SELECT COUNT(*) FROM user_card WHERE user_card_id = ?", Long.class, userCardId);
        if (snapshotTaken != null) {
            snapshotTaken.countDown();
        }
        // 3만원 × 10% = 3,000원이지만 잔여 한도 2,000원으로 깎인다
        return settlementService.applyPayment(new SettlementCommand(userCardId, cardId, cafeCategoryId,
                merchantId, 30_000L, "CARD", false, LocalDateTime.parse("2026-08-15T12:00:00")));
    }

    private void insertFixture() {
        memberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "concurrency-it@test.local", "x", "동시성테스트회원", "동시성IT별명");
        parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_CC_FOOD", "IT동시성대분류");
        cafeCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_CC_CAFE", "IT동시성카페", parentCategoryId);
        merchantId = insert("INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
                "IT_CC_CAFE_SHOP", "IT동시성카페", cafeCategoryId);
        cardCompanyId = insert("INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
                "IT_CC_COMPANY", "동시성테스트카드사");
        cardId = insert("INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "동시성IT_카페카드", cardCompanyId, "CREDIT");
        userCardId = insert("INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, cardId, "4444-****-****-4444");

        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, 0)", cardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 50000)", cardId);
        // 카페 10% 할인, 월 한도 1만원
        benefitId = insert("INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, "
                        + "apply_timing, target_type, target_category_id, require_performance, use_shared_limit, "
                        + "monthly_limit, is_active) "
                        + "VALUES (?, '카페 10% 할인', 'DISCOUNT', 'RATE', 10.00, 'BILLED', 'CATEGORY', ?, 'Y', 'Y', ?, 'Y')",
                cardId, cafeCategoryId, MONTHLY_LIMIT);

        // 실적 35만(30만 구간 충족), 이번 달 이미 8,000원 사용 — 잔여 2,000원
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month, prev_performance_amount, "
                + "current_performance_amount, shared_limit_used) VALUES (?, ?, 350000, 0, ?)",
                userCardId, BASE_MONTH, USED_BEFORE);
        jdbc.update("INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, used_amount, "
                        + "used_count, last_applied_date, daily_used_count, daily_used_amount) "
                        + "VALUES (?, ?, ?, ?, 4, '2026-08-10', 0, 0)",
                userCardId, benefitId, BASE_MONTH, USED_BEFORE);
    }

    private long usedAmount() {
        return jdbc.queryForObject("SELECT used_amount FROM user_benefit_usage "
                        + "WHERE user_card_id = ? AND benefit_id = ? AND base_year_month = ?",
                Long.class, userCardId, benefitId, BASE_MONTH);
    }

    /** 커밋되는 INSERT의 생성 키. 트랜잭션이 없어 LAST_INSERT_ID는 다른 커넥션을 볼 수 있다. */
    private long insert(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 순서 대기 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
