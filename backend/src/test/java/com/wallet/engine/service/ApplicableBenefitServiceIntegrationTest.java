package com.wallet.engine.service;

import com.wallet.engine.dto.ApplicableBenefitCard;
import com.wallet.engine.dto.ApplicableBenefitItem;
import com.wallet.engine.dto.ApplicableBenefitResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가맹점·업종별 혜택 조회의 MySQL 통합 테스트.
 *
 * 확인하는 것은 금액 계산이 아니라(이 서비스는 금액을 내지 않는다) 다음 넷이다.
 *   ① 결제 대상에 걸리는 혜택만 골라 온다
 *   ② 실적을 못 채워 지금 못 받는 혜택도 사유와 함께 담는다
 *   ③ 판정된 실적구간의 개별한도·혜택값이 반영된다
 *   ④ 혜택이 없는 카드도 목록에서 빠지지 않는다
 *
 * 픽스처를 직접 넣고 롤백하므로 시드 데이터에 의존하지 않는다.
 * 카테고리·가맹점만 팀 표준 시드에서 코드로 찾아 쓴다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class ApplicableBenefitServiceIntegrationTest {

    /** 기준월 2026-08, 직전월 2026-07 */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Autowired
    private ApplicableBenefitService applicableBenefitService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long cafeCategoryId;
    private long cafeMerchantId;
    private long benefitCardUserCardId;
    private long emptyCardUserCardId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @Test
    @DisplayName("결제 대상에 걸리는 혜택만 조건과 함께 내려준다")
    void 대상에_걸리는_혜택을_조건과_함께_반환한다() {
        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, cafeMerchantId, null, TODAY);

        ApplicableBenefitCard card = cardOf(response, benefitCardUserCardId);
        // 가맹점을 특정하면 그 가맹점 혜택과 소속 업종 혜택을 함께 본다.
        assertThat(card.getBenefits()).hasSize(2);

        ApplicableBenefitItem benefit = benefitNamed(card, "혜택조회IT_가맹점할인");
        assertThat(benefit.getCalcMethod()).isEqualTo("RATE");
        assertThat(benefit.getBenefitValue()).isEqualByComparingTo("10.00");
        assertThat(benefit.getMinTxnAmount()).isEqualTo(5_000L);
        assertThat(benefit.isRequirePerformance()).isTrue();
    }

    @Test
    @DisplayName("실적을 채웠으면 받을 수 있는 혜택으로 표시한다")
    void 실적을_채웠으면_사용_가능이다() {
        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, cafeMerchantId, null, TODAY);

        ApplicableBenefitCard card = cardOf(response, benefitCardUserCardId);

        assertThat(card.isPerformanceMet()).isTrue();
        assertThat(card.getPrevPerformanceAmount()).isEqualTo(400_000L);
        assertThat(card.getRequiredPerformanceAmount()).isEqualTo(300_000L);
        assertThat(benefitNamed(card, "혜택조회IT_가맹점할인").isAvailable()).isTrue();
        assertThat(benefitNamed(card, "혜택조회IT_가맹점할인").getUnavailableReason()).isNull();
    }

    @Test
    @DisplayName("실적이 모자라면 혜택을 빼지 않고 사유를 붙인다")
    void 실적_미달_혜택도_사유와_함께_담는다() {
        // 전월실적을 0원 구간으로 떨어뜨린다. 혜택 자체는 그대로 존재한다.
        jdbc.update("UPDATE user_card_monthly_state SET prev_performance_amount = 0"
                + " WHERE user_card_id = ?", benefitCardUserCardId);

        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, cafeMerchantId, null, TODAY);

        ApplicableBenefitCard card = cardOf(response, benefitCardUserCardId);

        assertThat(card.isPerformanceMet()).isFalse();
        // "혜택이 없다"가 아니라 "채우면 받을 수 있다"로 답할 수 있어야 한다.
        assertThat(card.getBenefits()).hasSize(2);
        ApplicableBenefitItem benefit = benefitNamed(card, "혜택조회IT_가맹점할인");
        assertThat(benefit.isAvailable()).isFalse();
        assertThat(benefit.getUnavailableReason()).isEqualTo("전월 실적 미달");
    }

    @Test
    @DisplayName("혜택이 없는 카드도 목록에서 빼지 않는다")
    void 혜택_없는_카드도_담는다() {
        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, cafeMerchantId, null, TODAY);

        // "이 카드는 여기서 혜택이 없다"도 사용자가 알아야 하는 답이다.
        assertThat(cardOf(response, emptyCardUserCardId).getBenefits()).isEmpty();
    }

    @Test
    @DisplayName("업종으로 조회하면 가맹점 전용 혜택은 빠지고 업종 혜택만 나온다")
    void 업종으로_조회하면_업종_혜택만_나온다() {
        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, null, cafeCategoryId, TODAY);

        // 어느 가맹점인지 모르면 가맹점 전용 혜택을 받는다고 말할 수 없다.
        ApplicableBenefitCard card = cardOf(response, benefitCardUserCardId);
        assertThat(card.getBenefits()).hasSize(1);
        assertThat(card.getBenefits().get(0).getBenefitName()).isEqualTo("혜택조회IT_업종적립");
    }

    private ApplicableBenefitItem benefitNamed(ApplicableBenefitCard card, String name) {
        return card.getBenefits().stream()
                .filter(benefit -> name.equals(benefit.getBenefitName()))
                .findFirst().orElseThrow();
    }

    private ApplicableBenefitCard cardOf(ApplicableBenefitResponse response, long userCardId) {
        return response.getCards().stream()
                .filter(card -> card.getUserCardId() == userCardId)
                .findFirst().orElseThrow();
    }

    private void insertFixture() {
        cafeCategoryId = jdbc.queryForObject(
                "SELECT category_id FROM category WHERE category_code = 'CAFE'", Long.class);
        cafeMerchantId = jdbc.queryForObject(
                "SELECT merchant_id FROM merchant WHERE category_id = ? LIMIT 1",
                Long.class, cafeCategoryId);

        jdbc.update("INSERT INTO member (email, password_hash, name, nickname, member_status)"
                + " VALUES ('benefit-lookup-it@example.com', 'x', '혜택조회IT', '혜택조회IT', 'ACTIVE')");
        memberId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        long companyId = jdbc.queryForObject(
                "SELECT card_company_id FROM card_company LIMIT 1", Long.class);

        long benefitCardId = insertCard(companyId, "혜택조회IT_카페카드");
        long emptyCardId = insertCard(companyId, "혜택조회IT_무혜택카드");

        // 두 카드 모두 0원 구간을 갖는다. 모든 카드가 0원 구간을 갖는다는 전제가 깨지면
        // 실적 판정이 예외를 던진다.
        insertTier(benefitCardId, 0L);
        long metTierId = insertTier(benefitCardId, 300_000L);
        insertTier(emptyCardId, 0L);

        jdbc.update("INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method,"
                + " benefit_value, target_type, target_merchant_id, require_performance,"
                + " min_txn_amount, monthly_limit, is_active)"
                + " VALUES (?, '혜택조회IT_가맹점할인', 'DISCOUNT', 'RATE', 10.00, 'MERCHANT', ?, 'Y',"
                + " 5000, 20000, 'Y')", benefitCardId, cafeMerchantId);

        // 업종 전체에 걸리는 혜택. 가맹점으로 조회하면 함께 나오고, 업종으로 조회하면 이것만 나온다.
        jdbc.update("INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method,"
                + " benefit_value, target_type, target_category_id, require_performance, is_active)"
                + " VALUES (?, '혜택조회IT_업종적립', 'POINT', 'RATE', 3.00, 'CATEGORY', ?, 'N', 'Y')",
                benefitCardId, cafeCategoryId);

        benefitCardUserCardId = insertUserCard(benefitCardId);
        emptyCardUserCardId = insertUserCard(emptyCardId);

        // 전월실적 40만원 → 30만원 구간으로 판정되어 실적 조건이 충족된다.
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month,"
                + " current_performance_amount, prev_performance_amount, shared_limit_used)"
                + " VALUES (?, '2026-08', 0, 400000, 0)", benefitCardUserCardId);
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month,"
                + " current_performance_amount, prev_performance_amount, shared_limit_used)"
                + " VALUES (?, '2026-08', 0, 0, 0)", emptyCardUserCardId);

        assertThat(metTierId).isPositive();
    }

    private long insertCard(long companyId, String name) {
        jdbc.update("INSERT INTO card (card_company_id, card_name, card_type, annual_fee, is_active)"
                + " VALUES (?, ?, 'CREDIT', 0, 'Y')", companyId, name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertTier(long cardId, long minPerformanceAmount) {
        jdbc.update("INSERT INTO performance_tier (card_id, period_type, min_performance_amount,"
                + " shared_monthly_limit) VALUES (?, 'MONTH', ?, NULL)", cardId, minPerformanceAmount);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertUserCard(long cardId) {
        jdbc.update("INSERT INTO user_card (member_id, card_id, masked_card_number, card_status)"
                + " VALUES (?, ?, '1234-****-****-5678', 'ACTIVE')", memberId, cardId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
