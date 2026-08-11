package com.wallet.engine.service;

import com.wallet.engine.dto.RecommendationItem;
import com.wallet.engine.dto.RecommendationRequest;
import com.wallet.engine.dto.RecommendationResponse;
import com.wallet.engine.model.BenefitKind;
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
 * 추천 경로(조회 매퍼 → 조립 → 계산기 → 순위)의 MySQL 통합 테스트.
 *
 * 실 MySQL(docker-compose의 yigo-mysql, schema.sql 적용 필요)에 연결한다. H2는 쓰지 않는다 —
 * benefit의 CHECK 제약·user_card의 생성 컬럼이 방언 차이로 뜨지 않기 때문이다.
 *
 * 검증하는 것은 계산 공식이 아니라(그건 단위 테스트가 덮는다) '매퍼 SQL과 조립의 정확성'이다:
 *   ① 카드별 계산 결과가 혜택액 내림차순으로 정렬되고 순위가 매겨진다
 *   ② 기준월 상태 행이 없으면 직전월 당월누적을 전월실적으로 쓴다(폴백)
 *   ③ 일 소진은 last_applied_date가 기준일과 다르면 0으로 접힌다
 *   ④ 구간별 개별한도(benefit_tier_limit)가 판정된 구간 기준으로 적용된다
 *   ⑤ 증정(GIFT)은 조회 단계에서 빠져 추천 대상이 되지 않는다
 *
 * 클래스 이름이 *Test라 surefire가 실행한다(*IT는 failsafe 몫이라 안 돈다).
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백하므로 시드 데이터에 의존·오염되지 않는다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class RecommendationServiceIntegrationTest {

    /** 기준일 고정 — 시계에 의존하지 않게 서비스에 직접 넘긴다. 기준월 2026-08, 직전월 2026-07 */
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Autowired
    private RecommendationService recommendationService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long cafeCategoryId;
    private long starbucksMerchantId;

    private long highCardId;      // 카페 10% 할인 카드
    private long lowCardId;       // 카페 5% 할인 카드
    private long highUserCardId;
    private long lowUserCardId;
    private long highBenefitId;
    private long highTierIdOfHighCard;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @Test
    @DisplayName("보유카드별 혜택을 계산해 혜택액 내림차순으로 순위를 매긴다")
    void 추천_순위는_혜택액_내림차순이다() {
        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        assertThat(response.recommendations()).hasSize(2);

        // 1만원 결제: 10% 카드 = 1,000원, 5% 카드 = 500원
        RecommendationItem first = response.recommendations().get(0);
        assertThat(first.rank()).isEqualTo(1);
        assertThat(first.userCardId()).isEqualTo(highUserCardId);
        assertThat(first.expectedBenefit()).isEqualTo(1_000L);
        assertThat(first.benefitKind()).isEqualTo(BenefitKind.DISCOUNT);
        assertThat(first.cardName()).isEqualTo("추천IT_10퍼센트카드");
        // 근거는 혜택명 + 계산 금액으로 조립된다. 정률+구간금액이라 예상 표기
        assertThat(first.isEstimate()).isTrue();
        assertThat(first.reason()).isEqualTo("카페 10% 할인 예상 1,000원");

        // 소진이 없으므로 "소진 전이었다면"의 1위와 실제 1위가 같다 → 동적 전환 아님
        assertThat(first.dynamicSwitch()).isFalse();

        RecommendationItem second = response.recommendations().get(1);
        assertThat(second.rank()).isEqualTo(2);
        assertThat(second.userCardId()).isEqualTo(lowUserCardId);
        assertThat(second.expectedBenefit()).isEqualTo(500L);

        // 미구현 필드는 명세대로 비어서 나간다
        assertThat(response.futureOptimization()).isNull();
        assertThat(response.pointGuide()).isNull();
        assertThat(response.membershipEarn()).isEmpty();
    }

    @Test
    @DisplayName("기준월 상태 행이 없으면 직전월 당월누적을 전월실적으로 쓴다")
    void 기준월_행이_없으면_직전월_누적으로_실적을_판정한다() {
        // 5% 카드는 2026-08 행이 없고 2026-07 행의 current_performance_amount가 40만이다.
        // 폴백이 동작해야 상위 구간(30만↑)으로 판정되어 실적 조건이 걸린 혜택이 적용된다.
        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        RecommendationItem lowCard = response.recommendations().stream()
                .filter(item -> item.userCardId() == lowUserCardId)
                .findFirst().orElseThrow();

        // 폴백이 없으면 전월실적 0 → 0원 구간 → require_performance='Y' 미충족 → 혜택 없음
        assertThat(lowCard.expectedBenefit()).isEqualTo(500L);
        assertThat(lowCard.reason()).isNotEqualTo("적용 가능한 혜택 없음");
    }

    @Test
    @DisplayName("일 소진은 마지막 적용일이 기준일과 다르면 0으로 본다")
    void 어제_소진액은_오늘_한도_판정에_반영되지_않는다() {
        // 10% 카드 혜택에 일 한도 1,000원을 걸고, '어제' 그 한도를 다 쓴 것으로 기록한다.
        jdbc.update("UPDATE benefit SET daily_limit = 1000 WHERE benefit_id = ?", highBenefitId);
        jdbc.update(
                "INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, "
                        + "used_amount, used_count, last_applied_date, daily_used_count, daily_used_amount) "
                        + "VALUES (?, ?, '2026-08', 1000, 1, ?, 1, 1000)",
                highUserCardId, highBenefitId, TODAY.minusDays(1));

        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        RecommendationItem highCard = response.recommendations().stream()
                .filter(item -> item.userCardId() == highUserCardId)
                .findFirst().orElseThrow();

        // 어제 소진분을 그대로 반영하면 잔여 0원이 되어 혜택이 사라진다
        assertThat(highCard.expectedBenefit()).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("구간별 개별한도가 있으면 판정된 구간의 한도로 깎인다")
    void 구간별_개별한도가_적용된다() {
        // 판정될 상위 구간에 월 한도 300원을 지정한다(benefit.monthly_limit은 NULL이라 이 값이 유효값)
        jdbc.update(
                "INSERT INTO benefit_tier_limit (benefit_id, tier_id, tier_monthly_limit) VALUES (?, ?, 300)",
                highBenefitId, highTierIdOfHighCard);

        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        RecommendationItem highCard = response.recommendations().stream()
                .filter(item -> item.userCardId() == highUserCardId)
                .findFirst().orElseThrow();

        // 1,000원 계산값이 구간 한도 300원으로 깎이고, 상한에 걸렸으므로 확정으로 표기된다
        assertThat(highCard.expectedBenefit()).isEqualTo(300L);
        assertThat(highCard.isEstimate()).isFalse();

        // 한도에 깎여 5% 카드(500원)에 순위가 밀린다
        assertThat(response.recommendations().get(0).userCardId()).isEqualTo(lowUserCardId);
    }

    @Test
    @DisplayName("소진 때문에 1위가 바뀌면 실제 1위에 동적 전환을 표시한다")
    void 소진으로_순위가_뒤집히면_동적_전환이다() {
        // 10% 카드 혜택에 월 한도 2,000원을 걸고 그 한도를 이미 다 쓴 것으로 기록한다
        jdbc.update("UPDATE benefit SET monthly_limit = 2000 WHERE benefit_id = ?", highBenefitId);
        jdbc.update(
                "INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, "
                        + "used_amount, used_count, last_applied_date, daily_used_count, daily_used_amount) "
                        + "VALUES (?, ?, '2026-08', 2000, 1, ?, 1, 0)",
                highUserCardId, highBenefitId, TODAY);

        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        // 실제: 10% 카드는 잔여 0원 → 5% 카드(500원)가 1위로 올라선다
        RecommendationItem first = response.recommendations().get(0);
        assertThat(first.userCardId()).isEqualTo(lowUserCardId);
        assertThat(first.expectedBenefit()).isEqualTo(500L);

        // 소진 전이었다면 10% 카드가 1,000원으로 1위였으므로 순위가 뒤집힌 것이다
        assertThat(first.dynamicSwitch()).isTrue();

        // 표시는 실제 1위에만 붙는다
        assertThat(response.recommendations().get(1).dynamicSwitch()).isFalse();
    }

    @Test
    @DisplayName("한도 자체가 낮아 순위가 바뀐 것은 동적 전환이 아니다")
    void 구간_한도로_밀린_것은_동적_전환이_아니다() {
        // 소진이 아니라 '구간 한도' 때문에 깎이는 경우 — 소진을 되돌려도 결과가 같다
        jdbc.update(
                "INSERT INTO benefit_tier_limit (benefit_id, tier_id, tier_monthly_limit) VALUES (?, ?, 300)",
                highBenefitId, highTierIdOfHighCard);

        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        // 10% 카드가 300원으로 깎여 5% 카드에 밀리지만, 이건 '평소에도' 그런 것이다
        RecommendationItem first = response.recommendations().get(0);
        assertThat(first.userCardId()).isEqualTo(lowUserCardId);
        assertThat(first.dynamicSwitch()).isFalse();
    }

    @Test
    @DisplayName("증정 혜택은 조회 단계에서 빠져 추천 대상이 되지 않는다")
    void 증정_혜택은_추천에_잡히지_않는다() {
        // 5% 카드의 할인 혜택을 지우고 증정 혜택만 남긴다
        jdbc.update("UPDATE benefit SET is_active = 'N' WHERE card_id = ?", lowCardId);
        jdbc.update(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, "
                        + "target_type, require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, '공항 라운지 무료 이용', 'GIFT', 'FIXED', 0, 'ALL', 'N', 'N', 'Y')",
                lowCardId);

        RecommendationResponse response = recommendationService.recommend(
                memberId, request(starbucksMerchantId, 10_000L), TODAY);

        RecommendationItem lowCard = response.recommendations().stream()
                .filter(item -> item.userCardId() == lowUserCardId)
                .findFirst().orElseThrow();

        // 증정이 적용된 것으로 잡히면 "결제와 무관한 혜택"이 기록된다
        assertThat(lowCard.benefitKind()).isNull();
        assertThat(lowCard.expectedBenefit()).isZero();
        assertThat(lowCard.reason()).isEqualTo("적용 가능한 혜택 없음");
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 테스트 코드·값이 시드와 겹치지 않도록 IT 전용 코드('IT_REC_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        memberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "recommend-it@test.local", "x", "추천테스트회원", "추천IT별명");

        long parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_REC_FOOD", "IT추천대분류");
        cafeCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_REC_CAFE", "IT추천카페", parentCategoryId);

        starbucksMerchantId = insert(
                "INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
                "IT_REC_STARBUCKS", "IT추천스타벅스", cafeCategoryId);

        long cardCompanyId = insert(
            "INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
            "TEST_CARD", "테스트카드사"
        );

        // 카드 두 장 — 같은 카테고리에 혜택률만 다르게 둬 순위 비교가 성립하게 한다
        highCardId = insert(
                "INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "추천IT_10퍼센트카드", cardCompanyId, "CREDIT");
        lowCardId = insert(
                "INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "추천IT_5퍼센트카드", cardCompanyId, "CREDIT");

        highUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, highCardId, "1111-****-****-1111");
        lowUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, lowCardId, "2222-****-****-2222");

        // 실적구간: 0원 구간(통합한도 없음) + 30만 구간(통합한도 5만).
        // 0원 구간은 판정이 항상 행 하나를 반환하기 위한 필수 행이다.
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, NULL)", highCardId);
        highTierIdOfHighCard = insert(
                "INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 50000)",
                highCardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, NULL)", lowCardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 50000)", lowCardId);

        highBenefitId = insert(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, apply_timing, "
                        + "target_type, target_category_id, require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, '카페 10% 할인', 'DISCOUNT', 'RATE', 10.00, 'BILLED', 'CATEGORY', ?, 'Y', 'Y', 'Y')",
                highCardId, cafeCategoryId);
        jdbc.update(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, apply_timing, "
                        + "target_type, target_category_id, require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, '카페 5% 할인', 'DISCOUNT', 'RATE', 5.00, 'BILLED', 'CATEGORY', ?, 'Y', 'Y', 'Y')",
                lowCardId, cafeCategoryId);

        // 10% 카드: 기준월(2026-08) 행이 있고 전월실적 35만이 이월돼 있다
        jdbc.update(
                "INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) "
                        + "VALUES (?, '2026-08', 350000, 0, 0)",
                highUserCardId);

        // 5% 카드: 기준월 행이 없다(월이 바뀐 뒤 첫 결제 전).
        // 직전월(2026-07) 행의 당월누적 40만이 전월실적으로 쓰여야 한다 — 폴백 검증용
        jdbc.update(
                "INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) "
                        + "VALUES (?, '2026-07', 0, 400000, 0)",
                lowUserCardId);
    }

    private RecommendationRequest request(long merchantId, long expectedAmount) {
        RecommendationRequest request = new RecommendationRequest();
        request.setMerchantId(merchantId);
        request.setExpectedAmount(expectedAmount);
        return request;
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
