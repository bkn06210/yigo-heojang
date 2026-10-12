package com.wallet.engine.service;

import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.model.DiscrepancyKind;
import com.wallet.engine.model.ReconciliationDiscrepancy;
import com.wallet.engine.model.ReconciliationReport;
import com.wallet.engine.model.ReconciliationTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 상태 대사의 MySQL 통합 테스트 — 실제 정산이 만든 상태를 대사가 어떻게 읽는지 본다.
 *
 * 비교 규칙 자체는 StateReconcilerTest가 덮는다. 여기서 검증하는 것은 그 앞단이다:
 *   ① 정산을 거친 결제·취소는 일치로 나온다 (대사가 정상 상태를 불일치로 오판하지 않는다)
 *   ② 상태를 임의로 틀어 놓으면 어긋난 값과 차이를 정확히 집어낸다
 *   ③ 혜택 0원 결제·스탬프 결제가 의도한 분류로 나온다
 *   ④ 조회 SQL — 월 묶음, 취소 건 처리, 회원 범위
 *
 * 결제는 정산 서비스를 직접 호출하고 그 결과로 소비내역을 넣는다. 운영에서 결제 도메인이 하는 일을
 * 테스트가 대신하는 것이며, 상태를 손으로 만들지 않으므로 "엔진이 만든 상태"를 그대로 대조하게 된다.
 *
 * 공용 DB에는 다른 데이터가 섞여 있으므로 모든 대조를 픽스처 회원으로 좁힌다.
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class StateReconciliationServiceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);
    private static final YearMonth JULY = YearMonth.of(2026, 7);
    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    @Autowired
    private StateReconciliationService reconciliationService;

    @Autowired
    private SettlementService settlementService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long cardId;
    private long userCardId;
    private long cafeCategoryId;
    private long utilityCategoryId;
    private long storeCategoryId;
    private long starbucksMerchantId;
    private long cafeBenefitId;
    private long stampBenefitId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    // ── 정상 경로: 정산을 거친 상태는 일치한다 ─────────────────────────────

    @Test
    @DisplayName("정상 결제: 혜택·통합한도·실적이 소비내역과 전부 일치한다")
    void 정상_결제는_전부_일치한다() {
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");
        pay(cafeCategoryId, starbucksMerchantId, 5_000, "2026-08-11T12:00:00");
        // 실적 제외 업종 — 혜택도 없고 실적에도 안 잡힌다
        pay(utilityCategoryId, null, 30_000, "2026-08-12T12:00:00");

        ReconciliationReport report = reconcile(AUGUST, AUGUST);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.unverifiables()).isEmpty();
        assertThat(report.stateRowCount()).isEqualTo(1);
        assertThat(report.usageRowCount()).isEqualTo(1);
        assertThat(report.expenseCount()).isEqualTo(3);
        // 카드 월 상태 3건(당월실적·통합한도·전월실적) + 혜택 소진 2건(금액·횟수)
        assertThat(report.comparisonCount()).isEqualTo(5);
        assertThat(report.matchedCount()).isEqualTo(5);

        // 대사가 맞춘 값이 실제로 0이 아닌 값인지 — 0끼리 맞은 것이면 아무것도 검증하지 못한 것이다
        assertThat(stateValue("current_performance_amount", AUGUST)).isEqualTo(15_000L);
        assertThat(stateValue("shared_limit_used", AUGUST)).isEqualTo(1_500L);
        assertThat(usageValue(cafeBenefitId, "used_amount")).isEqualTo(1_500L);
    }

    @Test
    @DisplayName("결제 후 취소: 역산이 끝난 상태는 남은 소비내역과 일치한다")
    void 결제_후_취소해도_일치한다() {
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");
        long canceledExpenseId = pay(cafeCategoryId, starbucksMerchantId, 5_000, "2026-08-11T12:00:00");

        settlementService.cancelPayment(memberId, canceledExpenseId, TODAY);

        ReconciliationReport report = reconcile(AUGUST, AUGUST);
        assertThat(report.discrepancies()).isEmpty();
        // 취소 건도 원본으로 읽되 합계에서는 빠진다
        assertThat(report.expenseCount()).isEqualTo(2);
        assertThat(usageValue(cafeBenefitId, "used_amount")).isEqualTo(1_000L);
        assertThat(usageValue(cafeBenefitId, "used_count")).isEqualTo(1L);
    }

    @Test
    @DisplayName("전부 취소: 0으로 되돌아간 채 남은 행은 불일치가 아니다")
    void 전부_취소해_0이_된_행은_일치한다() {
        long expenseId = pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");

        settlementService.cancelPayment(memberId, expenseId, TODAY);

        ReconciliationReport report = reconcile(AUGUST, AUGUST);
        assertThat(report.discrepancies()).isEmpty();
        assertThat(usageValue(cafeBenefitId, "used_amount")).isZero();
    }

    @Test
    @DisplayName("월 롤오버: 이월된 전월실적이 직전 월 누적과 일치한다")
    void 월이_바뀌어도_이월이_일치한다() {
        pay(cafeCategoryId, starbucksMerchantId, 350_000, "2026-07-20T12:00:00");
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");

        ReconciliationReport report = reconcile(JULY, AUGUST);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.stateRowCount()).isEqualTo(2);
        assertThat(stateValue("prev_performance_amount", AUGUST)).isEqualTo(350_000L);
    }

    // ── 분류: 혜택 0원 결제 · 스탬프 ───────────────────────────────────────

    @Test
    @DisplayName("혜택 0원 결제: 소진도 기록도 남지 않으므로 대조에서 걸리지 않는다")
    void 한도_소진으로_혜택이_0원인_결제는_일치한다() {
        // 카페 10%, 월 한도 3,000원 — 2,000 → 1,000(잔여) → 0(소진)
        pay(cafeCategoryId, starbucksMerchantId, 20_000, "2026-08-10T12:00:00");
        pay(cafeCategoryId, starbucksMerchantId, 20_000, "2026-08-11T12:00:00");
        PaymentSettlementResult exhausted = settle(
                cafeCategoryId, starbucksMerchantId, 20_000, "2026-08-12T12:00:00");
        insertExpense(cafeCategoryId, starbucksMerchantId, 20_000, "2026-08-12 12:00:00", exhausted);

        assertThat(exhausted.appliedBenefitId()).isNull();
        assertThat(exhausted.discountAmount()).isZero();

        ReconciliationReport report = reconcile(AUGUST, AUGUST);
        assertThat(report.discrepancies()).isEmpty();
        // 횟수는 혜택을 실제로 받은 두 번만 — 0원 결제가 횟수를 올렸다면 여기서 3 대 2로 걸린다
        assertThat(usageValue(cafeBenefitId, "used_count")).isEqualTo(2L);
        assertThat(stateValue("current_performance_amount", AUGUST)).isEqualTo(60_000L);
    }

    @Test
    @DisplayName("스탬프 결제: 지급액은 일치하고, 횟수는 원본에 흔적이 없어 대조 불가로 분류된다")
    void 스탬프_횟수는_대조_불가로_분류된다() {
        // 3회마다 2,000점 — 1·2회째는 0원이라 소비내역에 혜택이 남지 않는다
        pay(storeCategoryId, null, 4_000, "2026-08-10T12:00:00");
        pay(storeCategoryId, null, 4_000, "2026-08-11T12:00:00");
        pay(storeCategoryId, null, 4_000, "2026-08-12T12:00:00");

        ReconciliationReport report = reconcile(AUGUST, AUGUST);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.unverifiables()).singleElement().satisfies(unverifiable -> {
            assertThat(unverifiable.target()).isEqualTo(ReconciliationTarget.BENEFIT_USED_COUNT);
            assertThat(unverifiable.benefitId()).isEqualTo(stampBenefitId);
            // 상태는 3회를 세었지만 소비내역에서 보이는 것은 지급된 3회째 한 건뿐이다
            assertThat(unverifiable.storedValue()).isEqualTo(3L);
            assertThat(unverifiable.tracedValue()).isEqualTo(1L);
        });
        assertThat(usageValue(stampBenefitId, "used_amount")).isEqualTo(2_000L);
    }

    @Test
    @DisplayName("스탬프 결제 취소: 찍히기만 한 결제를 취소해도 금액 대조는 일치한다")
    void 스탬프만_찍힌_결제를_취소해도_일치한다() {
        long stampOnlyExpenseId = pay(storeCategoryId, null, 4_000, "2026-08-10T12:00:00");
        pay(storeCategoryId, null, 4_000, "2026-08-11T12:00:00");

        settlementService.cancelPayment(memberId, stampOnlyExpenseId, TODAY);

        ReconciliationReport report = reconcile(AUGUST, AUGUST);
        assertThat(report.discrepancies()).isEmpty();
        assertThat(usageValue(stampBenefitId, "used_count")).isEqualTo(1L);
    }

    // ── 검출: 어긋난 상태를 집어낸다 ───────────────────────────────────────

    @Test
    @DisplayName("상태 변조: 틀어 놓은 값마다 저장값·원본값·차이를 정확히 보고한다")
    void 상태를_틀어_놓으면_어긋난_값을_검출한다() {
        // 변조 전 상태가 일치한다는 것은 여기서 다시 대조하지 않는다(위의 정상 결제 테스트가 본다).
        // 한 트랜잭션에서 같은 조회를 두 번 하면 MyBatis가 앞 결과를 그대로 돌려주는데, 아래 변조는
        // JdbcTemplate으로 넣어 그 캐시를 비우지 않는다 — 두 번째 대조가 변조 전 값을 읽게 된다
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");

        jdbc.update("UPDATE user_benefit_usage SET used_amount = used_amount + 500, used_count = used_count + 2 "
                + "WHERE user_card_id = ? AND benefit_id = ?", userCardId, cafeBenefitId);
        jdbc.update("UPDATE user_card_monthly_state SET shared_limit_used = shared_limit_used + 700, "
                        + "current_performance_amount = current_performance_amount - 3000, "
                        + "prev_performance_amount = 123 WHERE user_card_id = ? AND base_year_month = ?",
                userCardId, AUGUST.toString());

        ReconciliationReport report = reconcile(AUGUST, AUGUST);

        assertThat(report.discrepancies())
                .extracting(ReconciliationDiscrepancy::target, ReconciliationDiscrepancy::kind,
                        ReconciliationDiscrepancy::storedValue, ReconciliationDiscrepancy::sourceValue,
                        ReconciliationDiscrepancy::difference)
                .containsExactlyInAnyOrder(
                        tuple(ReconciliationTarget.BENEFIT_USED_AMOUNT,
                                DiscrepancyKind.VALUE_MISMATCH, 1_500L, 1_000L, 500L),
                        tuple(ReconciliationTarget.BENEFIT_USED_COUNT,
                                DiscrepancyKind.VALUE_MISMATCH, 3L, 1L, 2L),
                        tuple(ReconciliationTarget.SHARED_LIMIT_USED,
                                DiscrepancyKind.VALUE_MISMATCH, 1_700L, 1_000L, 700L),
                        tuple(ReconciliationTarget.CURRENT_PERFORMANCE,
                                DiscrepancyKind.VALUE_MISMATCH, 7_000L, 10_000L, -3_000L),
                        // 7월 행이 없으므로 이월될 것이 없다 — 전월실적은 0이어야 한다
                        tuple(ReconciliationTarget.PREV_PERFORMANCE,
                                DiscrepancyKind.STATE_WITHOUT_SOURCE, 123L, null, 123L));
        assertThat(report.discrepancies()).allSatisfy(discrepancy -> {
            assertThat(discrepancy.userCardId()).isEqualTo(userCardId);
            assertThat(discrepancy.baseYearMonth()).isEqualTo(AUGUST.toString());
        });
    }

    @Test
    @DisplayName("취소 미역산: 소비내역만 취소되고 상태가 그대로면 검출한다")
    void 취소가_상태에_반영되지_않으면_검출한다() {
        long expenseId = pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");

        // 정산을 거치지 않고 소비내역 상태만 바꾼 경우
        jdbc.update("UPDATE expense SET payment_status = 'CANCELED' WHERE expense_id = ?", expenseId);

        ReconciliationReport report = reconcile(AUGUST, AUGUST);
        assertThat(report.discrepancies())
                .extracting(ReconciliationDiscrepancy::target, ReconciliationDiscrepancy::kind,
                        ReconciliationDiscrepancy::difference)
                .containsExactlyInAnyOrder(
                        tuple(ReconciliationTarget.BENEFIT_USED_AMOUNT,
                                DiscrepancyKind.VALUE_MISMATCH, 1_000L),
                        tuple(ReconciliationTarget.BENEFIT_USED_COUNT,
                                DiscrepancyKind.VALUE_MISMATCH, 1L),
                        tuple(ReconciliationTarget.SHARED_LIMIT_USED,
                                DiscrepancyKind.VALUE_MISMATCH, 1_000L),
                        tuple(ReconciliationTarget.CURRENT_PERFORMANCE,
                                DiscrepancyKind.VALUE_MISMATCH, 10_000L));
    }

    @Test
    @DisplayName("정산 누락: 소비내역은 있는데 상태 행이 없으면 검출한다")
    void 정산을_거치지_않은_소비내역을_검출한다() {
        // 혜택이 적용됐다고 적힌 소비내역이 상태 갱신 없이 들어온 경우
        insertExpense(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10 12:00:00",
                new PaymentSettlementResult(cafeBenefitId, 1_000L));

        ReconciliationReport report = reconcile(AUGUST, AUGUST);

        assertThat(report.discrepancies()).hasSize(4).allSatisfy(discrepancy -> {
            assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.SOURCE_WITHOUT_STATE);
            assertThat(discrepancy.storedValue()).isNull();
        });
        assertThat(report.stateRowCount()).isZero();
    }

    @Test
    @DisplayName("근거 없는 상태: 소비내역 없이 상태 행만 있으면 검출한다")
    void 소비내역_없이_쌓인_상태를_검출한다() {
        insertState(AUGUST, 0, 120_000, 2_000);
        jdbc.update("INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, used_amount, "
                + "used_count, last_applied_date, daily_used_count, daily_used_amount) "
                + "VALUES (?, ?, ?, 1500, 1, '2026-08-10', 1, 1500)", userCardId, cafeBenefitId, AUGUST.toString());

        ReconciliationReport report = reconcile(AUGUST, AUGUST);

        assertThat(report.discrepancies()).hasSize(4).allSatisfy(discrepancy -> {
            assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.STATE_WITHOUT_SOURCE);
            assertThat(discrepancy.sourceValue()).isNull();
        });
    }

    /**
     * 정산은 넘겨받은 결제 시각으로 기준월을 정하고, 소비내역의 결제일시는 따로 기록된다.
     * 두 시각이 월 경계를 사이에 두고 갈리면 결제는 8월로 기록되는데 실적·소진은 7월 행에 쌓인다.
     * 합계는 같아서 화면 어디에도 오류가 뜨지 않지만 두 달의 실적이 모두 틀린다.
     */
    @Test
    @DisplayName("월 귀속 불일치: 정산 기준월과 소비내역의 결제월이 다르면 양쪽 달에서 검출한다")
    void 정산_기준월과_소비내역_결제월이_갈리면_검출한다() {
        PaymentSettlementResult result = settle(
                cafeCategoryId, starbucksMerchantId, 10_000, "2026-07-31T15:30:00");
        insertExpense(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-01 00:30:00", result);

        ReconciliationReport report = reconcile(JULY, AUGUST);

        assertThat(report.discrepancies())
                .filteredOn(discrepancy -> discrepancy.target() == ReconciliationTarget.CURRENT_PERFORMANCE)
                .extracting(ReconciliationDiscrepancy::baseYearMonth, ReconciliationDiscrepancy::kind,
                        ReconciliationDiscrepancy::difference)
                .containsExactly(
                        tuple(
                                JULY.toString(), DiscrepancyKind.STATE_WITHOUT_SOURCE, 10_000L),
                        tuple(
                                AUGUST.toString(), DiscrepancyKind.SOURCE_WITHOUT_STATE, -10_000L));
    }

    // ── 조회 범위 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원 범위: 다른 회원의 어긋난 상태는 이 회원의 대조 결과에 섞이지 않는다")
    void 회원으로_좁히면_다른_회원의_불일치는_나오지_않는다() {
        long otherMemberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "reconcile-it-other@test.local", "x", "대사타인", "대사IT타인");
        long otherUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                otherMemberId, cardId, "5555-****-****-5556");
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) "
                        + "VALUES (?, ?, 0, 99000, 0)", otherUserCardId, AUGUST.toString());
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");

        assertThat(reconciliationService.reconcile(AUGUST, AUGUST, memberId).discrepancies()).isEmpty();
        assertThat(reconciliationService.reconcile(AUGUST, AUGUST, otherMemberId).discrepancies())
                .singleElement()
                .satisfies(discrepancy -> assertThat(discrepancy.userCardId()).isEqualTo(otherUserCardId));
    }

    @Test
    @DisplayName("월 범위: 구간 밖 달의 상태·소비내역은 대조하지 않는다")
    void 구간_밖의_달은_대조하지_않는다() {
        pay(cafeCategoryId, starbucksMerchantId, 350_000, "2026-07-20T12:00:00");
        pay(cafeCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");
        // 7월 상태를 틀어 놓는다 — 8월만 대조하면 보이지 않아야 한다
        jdbc.update("UPDATE user_benefit_usage SET used_amount = used_amount + 999 "
                + "WHERE user_card_id = ? AND base_year_month = ?", userCardId, JULY.toString());

        assertThat(reconcile(AUGUST, AUGUST).discrepancies()).isEmpty();
        assertThat(reconcile(JULY, AUGUST).discrepancies()).singleElement().satisfies(discrepancy -> {
            assertThat(discrepancy.baseYearMonth()).isEqualTo(JULY.toString());
            assertThat(discrepancy.difference()).isEqualTo(999L);
        });
    }

    /**
     * 결제 가산은 가맹점이 속한 업종으로 실적 제외를 판정하고, 취소 역산과 재합산은 소비내역에 적힌
     * 업종으로 판정한다. 결제 요청의 업종이 가맹점의 업종과 다르면 두 판정이 갈려,
     * 가산 때 인정된 실적이 취소 때 빠지지 않는다(취소했는데 실적이 남는다).
     *
     * 어느 업종을 기준으로 삼을지 정해지면 이 테스트를 켠다 — 그때부터 회귀 테스트가 된다.
     */
    @Test
    @Disabled("가산과 취소가 서로 다른 업종으로 실적을 판정한다 — 기준 업종이 정해지면 켠다")
    @DisplayName("업종 불일치 결제: 가맹점 업종과 소비내역 업종이 달라도 결제·취소 후 일치해야 한다")
    void 가맹점_업종과_소비내역_업종이_달라도_일치해야_한다() {
        // 가맹점은 카페인데 소비내역에는 실적 제외 업종으로 기록된다
        long expenseId = pay(utilityCategoryId, starbucksMerchantId, 10_000, "2026-08-10T12:00:00");
        assertThat(reconcile(AUGUST, AUGUST).discrepancies()).isEmpty();

        settlementService.cancelPayment(memberId, expenseId, TODAY);
        assertThat(reconcile(AUGUST, AUGUST).discrepancies()).isEmpty();
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 시드·다른 통합 테스트와 겹치지 않도록 전용 코드('IT_RCN_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        memberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "reconcile-it@test.local", "x", "대사테스트회원", "대사IT별명");

        long parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_RCN_FOOD", "IT대사대분류");
        cafeCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_RCN_CAFE", "IT대사카페", parentCategoryId);
        utilityCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_RCN_UTILITY", "IT대사공과금");
        storeCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_RCN_STORE", "IT대사편의점");
        starbucksMerchantId = insert(
                "INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
                "IT_RCN_STARBUCKS", "IT대사스타벅스", cafeCategoryId);

        long cardCompanyId = insert(
                "INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
                "IT_RCN_COMPANY", "대사테스트카드사");
        cardId = insert("INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "대사IT_카드", cardCompanyId, "CREDIT");
        userCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, cardId, "5555-****-****-5555");

        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) "
                + "VALUES (?, 0, 50000)", cardId);
        // 공과금은 실적에서 뺀다 — 재계산이 제외 규칙을 타는지 보려는 것이다
        jdbc.update("INSERT INTO performance_exclusion (card_id, exclusion_type, exclusion_value) "
                + "VALUES (?, 'CATEGORY', 'IT_RCN_UTILITY')", cardId);

        // 카페 10% 할인 — 통합한도 사용, 월 한도 3,000원(0원 결제를 만들기 위한 낮은 한도)
        cafeBenefitId = insert(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, apply_timing, "
                        + "target_type, target_category_id, require_performance, use_shared_limit, monthly_limit, "
                        + "is_active) VALUES (?, 'IT대사 카페 10% 할인', 'DISCOUNT', 'RATE', 10.00, 'BILLED', "
                        + "'CATEGORY', ?, 'N', 'Y', 3000, 'Y')",
                cardId, cafeCategoryId);
        // 편의점 스탬프 — 3회마다 2,000점. 통합한도를 쓰지 않는다
        stampBenefitId = insert(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, step_count, "
                        + "target_type, target_category_id, require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, 'IT대사 편의점 스탬프', 'POINT', 'COUNT_STEP', 2000.00, 3, "
                        + "'CATEGORY', ?, 'N', 'N', 'Y')",
                cardId, storeCategoryId);
    }

    private ReconciliationReport reconcile(YearMonth fromMonth, YearMonth toMonth) {
        return reconciliationService.reconcile(fromMonth, toMonth, memberId);
    }

    /** 결제 한 건 — 정산으로 상태를 가산하고 그 결과를 소비내역에 적는다. 소비내역 ID를 돌려준다. */
    private long pay(long categoryId, Long merchantId, long amount, String paymentDateTime) {
        PaymentSettlementResult result = settle(categoryId, merchantId, amount, paymentDateTime);
        return insertExpense(categoryId, merchantId, amount, paymentDateTime.replace('T', ' '), result);
    }

    private PaymentSettlementResult settle(long categoryId, Long merchantId, long amount,
                                           String paymentDateTime) {
        return settlementService.applyPayment(new SettlementCommand(
                userCardId, cardId, categoryId, merchantId, amount, "CARD", false,
                LocalDateTime.parse(paymentDateTime)));
    }

    private long insertExpense(long categoryId, Long merchantId, long amount, String paymentDateTime,
                               PaymentSettlementResult result) {
        return insert("INSERT INTO expense (member_id, user_card_id, category_id, merchant_id, merchant_name, "
                        + "amount, payment_date, input_type, applied_benefit_id, discount_amount, payment_type, "
                        + "is_interest_free, payment_status) "
                        + "VALUES (?, ?, ?, ?, 'IT대사가맹점', ?, ?, 'PAYMENT', ?, ?, 'CARD', 'N', 'APPROVED')",
                memberId, userCardId, categoryId, merchantId, amount, paymentDateTime,
                result.appliedBenefitId(), result.discountAmount());
    }

    private void insertState(YearMonth month, long prev, long current, long sharedUsed) {
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userCardId, month.toString(), prev, current, sharedUsed);
    }

    private long stateValue(String column, YearMonth month) {
        return jdbc.queryForObject("SELECT " + column + " FROM user_card_monthly_state "
                + "WHERE user_card_id = ? AND base_year_month = ?", Long.class, userCardId, month.toString());
    }

    private long usageValue(long benefitId, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM user_benefit_usage "
                        + "WHERE user_card_id = ? AND benefit_id = ? AND base_year_month = ?",
                Long.class, userCardId, benefitId, AUGUST.toString());
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
