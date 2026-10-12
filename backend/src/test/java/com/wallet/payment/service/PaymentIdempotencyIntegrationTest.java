package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentQrCreateRequest;
import com.wallet.payment.dto.PaymentQrPayRequest;
import com.wallet.payment.dto.PaymentQrPayResponse;
import com.wallet.payment.dto.PaymentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 결제 요청 멱등성 MySQL 통합 테스트 — 같은 요청이 다시 와도 결제는 한 번이고 처음 결과를 돌려준다.
 *
 * 동시 요청은 트랜잭션이 실제로 겹쳐야 하므로 @Transactional 롤백을 쓰지 않는다.
 * 픽스처는 커밋되고 @AfterEach에서 지운다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
class PaymentIdempotencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentQrService paymentQrService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;

    private long memberId;
    private long parentCategoryId;
    private long categoryId;
    private long merchantId;
    private long cardCompanyId;
    private long cardId;
    private long userCardId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM payment_idempotency WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM payment_qr WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM payment WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM expense WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM user_benefit_usage WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM user_card_monthly_state WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM performance_tier WHERE card_id = ?", cardId);
        jdbc.update("DELETE FROM user_card WHERE user_card_id = ?", userCardId);
        jdbc.update("DELETE FROM card WHERE card_id = ?", cardId);
        jdbc.update("DELETE FROM card_company WHERE card_company_id = ?", cardCompanyId);
        jdbc.update("DELETE FROM merchant WHERE merchant_id = ?", merchantId);
        jdbc.update("DELETE FROM category WHERE category_id = ?", categoryId);
        jdbc.update("DELETE FROM category WHERE category_id = ?", parentCategoryId);
        jdbc.update("DELETE FROM member WHERE member_id = ?", memberId);
    }

    // ── 일반 결제: Idempotency-Key ────────────────────────────────────────────

    @Test
    @DisplayName("같은 키로 다시 보내면 결제하지 않고 처음 결과를 돌려준다")
    void 같은_키의_재요청은_처음_결과를_돌려준다() {
        PaymentProcessResponse first = paymentService.processPayment(memberId, paymentRequest(10_000L), "retry-key");
        PaymentProcessResponse retried = paymentService.processPayment(memberId, paymentRequest(10_000L), "retry-key");

        assertThat(retried.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(retried.getExpenseId()).isEqualTo(first.getExpenseId());
        assertThat(paymentCount()).isEqualTo(1);
        assertThat(expenseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 키로 동시에 와도 결제는 한 번이다 — 뒤 요청은 키 잠금에서 기다렸다가 앞 결과를 받는다")
    void 같은_키의_동시_요청도_결제는_한_번이다() throws Exception {
        CountDownLatch firstPaid = new CountDownLatch(1);
        CountDownLatch commitFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        try {
            Future<PaymentProcessResponse> first = executor.submit(() -> tx.execute(status -> {
                PaymentProcessResponse response =
                        paymentService.processPayment(memberId, paymentRequest(10_000L), "concurrent-key");
                firstPaid.countDown();
                await(commitFirst);
                return response;
            }));
            await(firstPaid);

            Future<PaymentProcessResponse> second = executor.submit(() ->
                    paymentService.processPayment(memberId, paymentRequest(10_000L), "concurrent-key"));

            // 뒤 요청이 키 INSERT에서 실제로 잠금 대기에 들어간 것을 확인한 뒤 앞 요청을 커밋한다
            awaitLockWait();
            commitFirst.countDown();

            long firstPaymentId = first.get(10, TimeUnit.SECONDS).getPaymentId();
            long secondPaymentId = second.get(10, TimeUnit.SECONDS).getPaymentId();

            assertThat(secondPaymentId).isEqualTo(firstPaymentId);
            assertThat(paymentCount()).isEqualTo(1);
            assertThat(expenseCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("키 없이 같은 요청을 두 번 보내면 결제가 두 번 된다 — 멱등 키가 막는 문제")
    void 키가_없으면_같은_요청이_두_번_결제된다() {
        paymentService.processPayment(memberId, paymentRequest(10_000L), null);
        paymentService.processPayment(memberId, paymentRequest(10_000L), null);

        assertThat(paymentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 키로 내용이 다른 결제를 보내면 422 — 재시도가 아니라 키 재사용이다")
    void 같은_키에_다른_내용이면_거절한다() {
        paymentService.processPayment(memberId, paymentRequest(10_000L), "reused-key");

        assertThatThrownBy(() -> paymentService.processPayment(memberId, paymentRequest(20_000L), "reused-key"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(paymentCount()).isEqualTo(1);
    }

    // ── QR 결제: QR 토큰이 멱등 키 ───────────────────────────────────────────

    @Test
    @DisplayName("같은 QR로 같은 결제를 다시 보내면 409가 아니라 처음 결과를 돌려준다")
    void 같은_QR_재요청은_처음_결과를_돌려준다() {
        String qrToken = issueQr(15_000L);

        PaymentQrPayResponse first = paymentQrService.payWithQr(memberId, qrToken, qrPayRequest(merchantId));
        PaymentQrPayResponse retried = paymentQrService.payWithQr(memberId, qrToken, qrPayRequest(merchantId));

        assertThat(retried.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(retried.getExpenseId()).isEqualTo(first.getExpenseId());
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 QR로 다른 가맹점 결제를 보내면 재시도가 아니므로 409")
    void 같은_QR로_다른_결제는_거절한다() {
        String qrToken = issueQr(15_000L);
        paymentQrService.payWithQr(memberId, qrToken, qrPayRequest(merchantId));

        PaymentQrPayRequest otherMerchant = qrPayRequest(merchantId);
        otherMerchant.setMerchantName("IT멱등다른가맹점");

        assertThatThrownBy(() -> paymentQrService.payWithQr(memberId, qrToken, otherMerchant))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(paymentCount()).isEqualTo(1);
    }

    // ── 픽스처·헬퍼 ─────────────────────────────────────────────────────────

    private PaymentRequest paymentRequest(long amount) {
        PaymentRequest request = new PaymentRequest();
        request.setUserCardId(userCardId);
        request.setCategoryId(categoryId);
        request.setMerchantId(merchantId);
        request.setMerchantName("IT멱등카페");
        request.setPaymentAmount(amount);
        return request;
    }

    private String issueQr(long amount) {
        PaymentQrCreateRequest request = new PaymentQrCreateRequest();
        request.setUserCardId(userCardId);
        request.setPaymentAmount(amount);
        return paymentQrService.createPaymentQr(memberId, request).getQrToken();
    }

    private PaymentQrPayRequest qrPayRequest(long merchant) {
        PaymentQrPayRequest request = new PaymentQrPayRequest();
        request.setMerchantId(merchant);
        request.setMerchantName("IT멱등카페");
        request.setCategoryId(categoryId);
        return request;
    }

    private void insertFixture() {
        memberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "idempotency-it@test.local", "x", "멱등테스트회원", "멱등IT별명");
        parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_IDEM_FOOD", "IT멱등대분류");
        categoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_IDEM_CAFE", "IT멱등카페", parentCategoryId);
        merchantId = insert("INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
                "IT_IDEM_CAFE_SHOP", "IT멱등카페", categoryId);
        cardCompanyId = insert("INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
                "IT_IDEM_COMPANY", "멱등테스트카드사");
        cardId = insert("INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "멱등IT_카드", cardCompanyId, "CREDIT");
        userCardId = insert("INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, cardId, "5555-****-****-5555");
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, 0)",
                cardId);
    }

    private int paymentCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM payment WHERE member_id = ?", Integer.class, memberId);
    }

    private int expenseCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM expense WHERE member_id = ?", Integer.class, memberId);
    }

    /**
     * 다른 트랜잭션이 행 잠금을 기다리는 상태가 될 때까지 기다린다.
     * information_schema.innodb_trx는 결과를 짧게 캐시해 촘촘히 물으면 대기 상태를 놓친다.
     */
    private void awaitLockWait() throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            Integer waiting = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("뒤 요청이 잠금 대기에 들어가지 않았다");
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
