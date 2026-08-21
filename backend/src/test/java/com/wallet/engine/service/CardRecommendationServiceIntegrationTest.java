package com.wallet.engine.service;

import com.wallet.engine.dto.CardRecommendation;
import com.wallet.engine.dto.CardRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소비 기반 카드 추천의 MySQL 통합 테스트.
 *
 * 확인하는 것은 넷이다.
 *   ① 지난달 거래를 읽어 시뮬레이션까지 도는가 (쿼리·매핑·조립)
 *   ② 순증이 실제 계산값과 맞는가
 *   ③ 이길 데가 없는 카드는 목록에서 빠지는가
 *   ④ 소비가 없으면 빈 응답인가 (에러가 아니라)
 *
 * 카테고리·가맹점·카드사까지 전부 직접 넣고 롤백한다. 시드에서 찾아 쓰면 시드가 없는
 * 환경에서 깨진다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class CardRecommendationServiceIntegrationTest {

    /** 기준일 2026-08-15 → 추천 근거는 직전월(2026-07) 소비 */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Autowired
    private CardRecommendationService cardRecommendationService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long cafeCategoryId;
    private long heldCardId;
    private long strongCafeCardId;

    @Autowired
    void setDataSource(javax.sql.DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void setUp() {
        insertFixture();
    }

    @Test
    @DisplayName("카페에 강한 미보유 카드가 순증과 함께 추천된다")
    void 미보유_카드가_순증과_함께_추천된다() {
        CardRecommendationResponse response = cardRecommendationService.recommend(memberId, TODAY);

        assertThat(response.baseYearMonth()).isEqualTo("2026-07");
        // 보유 카드 카페 1% × 10,000원 × 10건 = 1,000원
        assertThat(response.currentMonthlyBenefitAmount()).isEqualTo(1_000L);

        assertThat(response.items()).isNotEmpty();
        CardRecommendation top = response.items().get(0);
        assertThat(top.cardId()).isEqualTo(strongCafeCardId);
        // 카페 10%로 바뀌어 10,000원. 기존 1,000원과의 차액이 순증이다
        assertThat(top.monthlyGainAmount()).isEqualTo(9_000L);
        assertThat(top.cardName()).isEqualTo("추천IT_카페강자");
        assertThat(top.cardCompanyName()).isEqualTo("추천IT카드사");
    }

    @Test
    @DisplayName("연회비가 있으면 손익분기 개월을 함께 내려준다")
    void 손익분기_개월을_내려준다() {
        CardRecommendationResponse response = cardRecommendationService.recommend(memberId, TODAY);

        CardRecommendation top = response.items().get(0);
        // 연회비 30,000원 ÷ 월 순증 9,000원 = 3.33 → 4개월
        assertThat(top.annualFee()).isEqualTo(30_000L);
        assertThat(top.breakEvenMonths()).isEqualTo(4);
    }

    @Test
    @DisplayName("이길 데가 없는 카드는 목록에 담지 않는다")
    void 순증이_없으면_빠진다() {
        CardRecommendationResponse response = cardRecommendationService.recommend(memberId, TODAY);

        // 보유 카드보다 약한 카페 카드(0.5%)는 어느 결제에서도 이기지 못한다
        assertThat(response.items())
                .noneSatisfy(item -> assertThat(item.cardName()).isEqualTo("추천IT_카페약자"));
        assertThat(response.items()).allSatisfy(
                item -> assertThat(item.monthlyGainAmount()).isPositive());
    }

    @Test
    @DisplayName("소비 내역이 없으면 빈 응답이다")
    void 소비가_없으면_빈_응답이다() {
        jdbc.update("DELETE FROM expense WHERE member_id = ?", memberId);

        CardRecommendationResponse response = cardRecommendationService.recommend(memberId, TODAY);

        assertThat(response.items()).isEmpty();
        assertThat(response.currentMonthlyBenefitAmount()).isZero();
    }

    private void insertFixture() {
        jdbc.update("INSERT INTO category (category_code, category_name, parent_category_id)"
                + " VALUES ('IT_REC_DINING', 'IT추천외식', NULL)");
        long parentCategoryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update("INSERT INTO category (category_code, category_name, parent_category_id)"
                + " VALUES ('IT_REC_CAFE', 'IT추천카페', ?)", parentCategoryId);
        cafeCategoryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update("INSERT INTO member (email, password_hash, name, nickname, member_status)"
                + " VALUES ('card-rec-it@example.com', 'x', '추천IT', '추천IT', 'ACTIVE')");
        memberId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update("INSERT INTO card_company (company_code, company_name)"
                + " VALUES ('IT_REC_COMPANY', '추천IT카드사')");
        long companyId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        heldCardId = insertCard(companyId, "추천IT_보유카드");
        strongCafeCardId = insertCard(companyId, "추천IT_카페강자");
        long weakCafeCardId = insertCard(companyId, "추천IT_카페약자");

        // 실적 조건을 쓰지 않는 카드들이다. 0원 구간만 둔다 —
        // 모든 카드가 0원 구간을 갖는다는 전제가 깨지면 실적 판정이 예외를 던진다.
        insertTier(heldCardId, 0L);
        insertTier(strongCafeCardId, 0L);
        insertTier(weakCafeCardId, 0L);

        insertCafeBenefit(heldCardId, "추천IT_보유카페", "1.00");
        insertCafeBenefit(strongCafeCardId, "추천IT_강자카페", "10.00");
        insertCafeBenefit(weakCafeCardId, "추천IT_약자카페", "0.50");

        // 연회비는 브랜드별로 여러 행이다. 최저값이 쓰이는지 함께 확인한다.
        jdbc.update("INSERT INTO card_annual_fee (card_id, brand, issue_type, variant, total_fee)"
                + " VALUES (?, 'LOCAL', 'PLASTIC', 'ANY', 30000)", strongCafeCardId);
        jdbc.update("INSERT INTO card_annual_fee (card_id, brand, issue_type, variant, total_fee)"
                + " VALUES (?, 'VISA', 'PLASTIC', 'ANY', 50000)", strongCafeCardId);

        long userCardId = insertUserCard(heldCardId);
        for (int day = 1; day <= 10; day++) {
            insertCafeExpense(userCardId, day);
        }
    }

    private long insertCard(long companyId, String name) {
        jdbc.update("INSERT INTO card (card_company_id, card_name, card_type, annual_fee, is_active)"
                + " VALUES (?, ?, 'CREDIT', 0, 'Y')", companyId, name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertCafeBenefit(long cardId, String name, String rate) {
        jdbc.update("INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method,"
                + " benefit_value, target_type, target_category_id, require_performance, is_active)"
                + " VALUES (?, ?, 'DISCOUNT', 'RATE', ?, 'CATEGORY', ?, 'N', 'Y')",
                cardId, name, new java.math.BigDecimal(rate), cafeCategoryId);
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

    /** 하루 한 건씩 흩어 넣는다 — 하루에 몰면 일 한도가 걸리는 혜택에서 결과가 달라진다 */
    private void insertCafeExpense(long userCardId, int day) {
        jdbc.update("INSERT INTO expense (member_id, user_card_id, category_id, merchant_name,"
                + " amount, payment_date, input_type, payment_status, discount_amount, payment_type)"
                + " VALUES (?, ?, ?, 'IT추천카페가맹점', 10000, ?, 'MANUAL', 'APPROVED', 0, 'CARD')",
                memberId, userCardId, cafeCategoryId,
                java.sql.Timestamp.valueOf(LocalDate.of(2026, 7, day).atTime(12, 0)));
    }
}
