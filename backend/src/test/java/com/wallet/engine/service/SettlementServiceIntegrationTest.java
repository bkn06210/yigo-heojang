package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 정산(가산·취소) 경로의 MySQL 통합 테스트.
 *
 * 실 MySQL(docker-compose의 yigo-mysql, schema.sql 적용 필요)에 연결한다. H2는 쓰지 않는다 —
 * ON DUPLICATE KEY UPDATE·GREATEST 같은 MySQL 구문과 CHECK 제약이 방언 차이로 뜨지 않기 때문이다.
 *
 * 검증하는 것은 계산 공식이 아니라(그건 단위 테스트가 덮는다) '상태 쓰기 SQL과 역산의 정확성'이다:
 *   ① 결제 가산 — 실적·통합한도·혜택별 소진이 더해지고 applied_benefit_id·discount가 반환된다
 *   ② 월 롤오버 — 기준월 행이 없으면 만들며 prev를 이월한다
 *   ③ 취소 차감 — 저장된 값을 역산해 빼고, 차감 반영된 현황을 돌려준다
 *   ④ 할인받은 거래 취소 — 실적 제외 규칙에 걸린 건은 실적을 되돌리지 않는다(0을 뺀다)
 *   ⑤ 없음/남의 것 → 404, 이미 취소 → 409, 전월 거래 → 범위 밖(400)
 *
 * 클래스 이름이 *Test라 surefire가 실행한다. @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class SettlementServiceIntegrationTest {

    /** 기준일 고정 — 기준월 2026-08, 직전월 2026-07 */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);
    private static final String BASE_MONTH = "2026-08";
    private static final String PREV_MONTH = "2026-07";

    @Autowired
    private SettlementService settlementService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long otherMemberId;
    private long cafeCategoryId;
    private long starbucksMerchantId;
    private long cardId;
    private long userCardId;
    private long cafeBenefitId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    // ── 가산 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("결제 가산: 실적·통합한도·혜택별 소진이 더해지고 적용 혜택·혜택액이 반환된다")
    void 결제하면_상태가_가산되고_혜택결과가_반환된다() {
        insertState(BASE_MONTH, 350_000, 0, 0);

        PaymentSettlementResult result = settlementService.applyPayment(
                command(10_000, "2026-08-15T12:00:00"));

        // 카페 10% × 1만원 = 1,000원
        assertThat(result.appliedBenefitId()).isEqualTo(cafeBenefitId);
        assertThat(result.discountAmount()).isEqualTo(1_000L);

        // 할인 제외 규칙이 없는 카드라 승인액 전액이 실적에 잡힌다
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(10_000L);
        assertThat(stateVal("shared_limit_used", BASE_MONTH)).isEqualTo(1_000L);
        assertThat(usageVal("used_amount")).isEqualTo(1_000L);
        assertThat(usageVal("used_count")).isEqualTo(1L);
        assertThat(usageVal("daily_used_amount")).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("월 롤오버: 기준월 행이 없으면 만들며 직전월 누적을 전월실적으로 이월한다")
    void 기준월_행이_없으면_생성하며_전월실적을_이월한다() {
        // 직전월(2026-07) 행만 있다. 당월누적 35만이 새 행의 prev로 이월돼야 실적 조건 혜택이 산다
        insertState(PREV_MONTH, 0, 350_000, 0);

        PaymentSettlementResult result = settlementService.applyPayment(
                command(10_000, "2026-08-15T12:00:00"));

        assertThat(result.discountAmount()).isEqualTo(1_000L);
        // 새로 만들어진 기준월 행: prev 이월 + 이번 결제분 가산
        assertThat(stateVal("prev_performance_amount", BASE_MONTH)).isEqualTo(350_000L);
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(10_000L);
        assertThat(stateVal("shared_limit_used", BASE_MONTH)).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("결제 가산: 혜택 한도가 소진돼 0원이면 횟수·기록을 남기지 않는다")
    void 한도_소진으로_혜택이_0원이면_횟수를_올리지_않는다() {
        // 월 한도 1만원을 이미 다 쓴 상태 — 이번 결제 혜택은 0원으로 깎인다
        insertState(BASE_MONTH, 350_000, 20_000, 5_000);
        insertUsage(10_000, 1, "2026-08-01");

        PaymentSettlementResult result = settlementService.applyPayment(
                command(10_000, "2026-08-15T12:00:00"));

        // 실제로 받은 혜택이 없으므로 적용 혜택·혜택액이 비어서 나간다
        assertThat(result.appliedBenefitId()).isNull();
        assertThat(result.discountAmount()).isZero();

        // 실적은 여전히 인정된다(할인 안 받은 일반 거래) — current += 10,000
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(30_000L);
        // 통합한도·혜택별 횟수/사용액은 그대로 — 0원짜리로 헛소비하지 않는다
        assertThat(stateVal("shared_limit_used", BASE_MONTH)).isEqualTo(5_000L);
        assertThat(usageVal("used_count")).isEqualTo(1L);
        assertThat(usageVal("used_amount")).isEqualTo(10_000L);
    }

    // ── 취소 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("취소 차감: 저장된 값을 역산해 실적·통합한도·혜택별 소진을 빼고 현황을 돌려준다")
    void 취소하면_기여분이_역산_차감된다() {
        insertState(BASE_MONTH, 350_000, 60_000, 1_000);
        insertUsage(1_000, 1, "2026-08-10");
        long expenseId = insertExpense(memberId, cafeBenefitId, 10_000, 1_000,
                "2026-08-10 13:00:00", "APPROVED");

        CardMonthlyStatus status = settlementService.cancelPayment(memberId, expenseId, TODAY);

        // 실적 60,000 − 10,000, 통합한도 1,000 − 1,000, 혜택별 1,000 − 1,000
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(50_000L);
        assertThat(stateVal("shared_limit_used", BASE_MONTH)).isEqualTo(0L);
        assertThat(usageVal("used_amount")).isEqualTo(0L);
        assertThat(usageVal("used_count")).isEqualTo(0L);

        // 응답은 차감 반영된 현황이다
        assertThat(status.currentPerformanceAmount()).isEqualTo(50_000L);
        assertThat(status.sharedLimitUsed()).isEqualTo(0L);
        assertThat(status.performanceMet()).isTrue();
        assertThat(status.benefits()).anySatisfy(benefit -> {
            assertThat(benefit.benefitId()).isEqualTo(cafeBenefitId);
            assertThat(benefit.usedAmount()).isEqualTo(0L);
            assertThat(benefit.monthlyLimit()).isEqualTo(10_000L);
            assertThat(benefit.remainingLimit()).isEqualTo(10_000L);
        });

        // 소비내역은 CANCELED로 전환된다
        assertThat(expenseStatus(expenseId)).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("할인받은 거래 취소: 실적 제외 규칙에 걸린 건은 실적을 되돌리지 않는다")
    void 할인받은_거래는_취소해도_실적을_빼지_않는다() {
        // 이 카드는 '할인받은 거래'를 실적에서 제외한다
        jdbc.update("INSERT INTO performance_exclusion (card_id, exclusion_type, exclusion_value) "
                + "VALUES (?, 'TRANSACTION_ATTR', 'DISCOUNTED')", cardId);
        insertState(BASE_MONTH, 350_000, 50_000, 1_000);
        insertUsage(1_000, 1, "2026-08-10");
        long expenseId = insertExpense(memberId, cafeBenefitId, 10_000, 1_000,
                "2026-08-10 13:00:00", "APPROVED");

        settlementService.cancelPayment(memberId, expenseId, TODAY);

        // 할인받은 거래라 애초에 실적에 안 잡혔으므로 실적은 그대로, 혜택·통합한도만 되돌린다
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(50_000L);
        assertThat(stateVal("shared_limit_used", BASE_MONTH)).isEqualTo(0L);
        assertThat(usageVal("used_amount")).isEqualTo(0L);
    }

    @Test
    @DisplayName("없는 소비내역·남의 소비내역 취소는 404다")
    void 없거나_남의_소비내역은_404다() {
        assertThatThrownBy(() -> settlementService.cancelPayment(memberId, 9_999_999L, TODAY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        // 남의 소비내역 — 존재를 노출하지 않으려 소유권 불일치도 404
        long othersExpense = insertExpense(otherMemberId, null, 10_000, 0,
                "2026-08-10 13:00:00", "APPROVED");
        assertThatThrownBy(() -> settlementService.cancelPayment(memberId, othersExpense, TODAY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("이미 취소된 소비내역 취소는 409다")
    void 이미_취소된_건은_409다() {
        long expenseId = insertExpense(memberId, cafeBenefitId, 10_000, 1_000,
                "2026-08-10 13:00:00", "CANCELED");

        assertThatThrownBy(() -> settlementService.cancelPayment(memberId, expenseId, TODAY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CANCELED));
    }

    @Test
    @DisplayName("전월 이전 거래 취소는 범위 밖(400)이다 — 상태를 건드리지 않는다")
    void 전월_거래_취소는_범위_밖이다() {
        insertState(BASE_MONTH, 350_000, 60_000, 1_000);
        // 결제일이 직전월(2026-07)이다
        long expenseId = insertExpense(memberId, cafeBenefitId, 10_000, 1_000,
                "2026-07-20 13:00:00", "APPROVED");

        assertThatThrownBy(() -> settlementService.cancelPayment(memberId, expenseId, TODAY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_STATE_INVALID));

        // 범위 밖이라 상태도 소비내역 상태도 그대로여야 한다
        assertThat(stateVal("current_performance_amount", BASE_MONTH)).isEqualTo(60_000L);
        assertThat(expenseStatus(expenseId)).isEqualTo("APPROVED");
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 시드와 겹치지 않도록 IT 전용 코드('IT_STL_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        memberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "settle-it@test.local", "x", "정산테스트회원", "정산IT별명");
        otherMemberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "settle-it-other@test.local", "x", "정산타인", "정산타인별명");

        long parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_STL_FOOD", "IT정산대분류");
        cafeCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_STL_CAFE", "IT정산카페", parentCategoryId);
        starbucksMerchantId = insert(
                "INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
                "IT_STL_STARBUCKS", "IT정산스타벅스", cafeCategoryId);

        cardId = insert("INSERT INTO card (card_name, issuer, card_type) VALUES (?, ?, ?)",
                "정산IT_카페카드", "TEST", "CREDIT");
        userCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, cardId, "3333-****-****-3333");

        // 0원 구간 + 30만 구간(통합한도 5만). 실적 35만이면 30만 구간으로 판정돼 실적 조건이 충족된다
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, 0)", cardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 50000)", cardId);

        // 카페 10% 할인 — 실적 조건 있음, 통합한도 사용, 월 한도 1만원
        cafeBenefitId = insert(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, apply_timing, "
                        + "target_type, target_category_id, require_performance, use_shared_limit, monthly_limit, is_active) "
                        + "VALUES (?, '카페 10% 할인', 'DISCOUNT', 'RATE', 10.00, 'BILLED', 'CATEGORY', ?, 'Y', 'Y', 10000, 'Y')",
                cardId, cafeCategoryId);
    }

    private void insertState(String yearMonth, long prev, long current, long sharedUsed) {
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) VALUES (?, ?, ?, ?, ?)",
                userCardId, yearMonth, prev, current, sharedUsed);
    }

    private void insertUsage(long usedAmount, int usedCount, String lastAppliedDate) {
        jdbc.update("INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, "
                        + "used_amount, used_count, last_applied_date, daily_used_count, daily_used_amount) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userCardId, cafeBenefitId, BASE_MONTH, usedAmount, usedCount, lastAppliedDate, usedCount, usedAmount);
    }

    private long insertExpense(long owner, Long benefitId, long amount, long discount,
                               String paymentDateTime, String status) {
        return insert("INSERT INTO expense (member_id, user_card_id, category_id, merchant_id, merchant_name, "
                        + "amount, payment_date, input_type, applied_benefit_id, discount_amount, payment_type, "
                        + "is_interest_free, payment_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PAYMENT', ?, ?, 'CARD', 'N', ?)",
                owner, userCardId, cafeCategoryId, starbucksMerchantId, "IT정산스타벅스",
                amount, paymentDateTime, benefitId, discount, status);
    }

    private SettlementCommand command(long amount, String paymentDateTime) {
        return new SettlementCommand(userCardId, cardId, cafeCategoryId, starbucksMerchantId,
                amount, "CARD", false, LocalDateTime.parse(paymentDateTime));
    }

    private long stateVal(String column, String yearMonth) {
        return jdbc.queryForObject("SELECT " + column + " FROM user_card_monthly_state "
                + "WHERE user_card_id = ? AND base_year_month = ?", Long.class, userCardId, yearMonth);
    }

    private long usageVal(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM user_benefit_usage "
                + "WHERE user_card_id = ? AND benefit_id = ? AND base_year_month = ?",
                Long.class, userCardId, cafeBenefitId, BASE_MONTH);
    }

    private String expenseStatus(long expenseId) {
        return jdbc.queryForObject("SELECT payment_status FROM expense WHERE expense_id = ?",
                String.class, expenseId);
    }

    /** INSERT 후 AUTO_INCREMENT PK를 돌려준다. @Transactional 안에서 같은 커넥션이라 LAST_INSERT_ID가 유효하다. */
    private long insert(String sql, Object... args) {
        jdbc.update(sql, args);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("생성 키를 가져오지 못했다: " + sql);
        }
        return id;
    }
}
