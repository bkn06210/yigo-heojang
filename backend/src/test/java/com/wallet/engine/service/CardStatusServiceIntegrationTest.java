package com.wallet.engine.service;

import com.wallet.common.exception.BusinessException;
import com.wallet.engine.dto.BenefitSummary;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.BriefingType;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.dto.CardStatusSummary;
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
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 현황 조회 경로(조회 매퍼 → 조립 → 응답)의 MySQL 통합 테스트.
 *
 * 계산 공식은 단위 테스트가 덮으므로, 여기서 고정하는 것은 '매퍼 SQL과 조립의 정확성'이다:
 *   ① 개별 상세가 두 축(전월실적 → 충족·통합한도 / 당월누적 → 목표·달성률)을 함께 내려준다
 *   ② 묶음 한도가 그룹 합산으로, 한도 없는 혜택은 null로 내려간다
 *   ③ 증정(GIFT)은 조회 단계에서 빠진다 — 한도 없는 혜택을 담기 시작했으므로 이 방어가 유일하다
 *   ④ 전체 현황이 묶음을 한 줄로 접고 브리핑 카드를 고른다
 *   ⑤ 상태 행이 없는 카드도 0으로 조회되고, 조회가 행을 만들지 않는다(읽기 전용)
 *   ⑥ 남의 카드·없는 카드는 404
 *
 * 클래스 이름이 *Test라 surefire가 실행한다. @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class CardStatusServiceIntegrationTest {

    private static final YearMonth BASE_MONTH = YearMonth.of(2026, 8);

    @Autowired
    private CardStatusService cardStatusService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long otherMemberId;

    private long mainCardId;
    private long mainUserCardId;   // 실적구간·혜택이 있는 카드
    private long plainUserCardId;  // 실적 조건도 상태 행도 없는 카드
    private long otherUserCardId;  // 남의 카드

    private long telecomBenefitId;
    private long utilityBenefitId;
    private long storeBenefitId;
    private long unlimitedBenefitId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @Test
    @DisplayName("개별 상세는 전월실적으로 충족·통합한도를, 당월누적으로 목표·달성률을 내려준다")
    void 개별_상세는_두_축을_함께_내려준다() {
        CardMonthlyStatus status = cardStatusService.getCardStatus(memberId, mainUserCardId, BASE_MONTH);

        assertThat(status.userCardId()).isEqualTo(mainUserCardId);
        assertThat(status.cardName()).isEqualTo("현황IT_생활카드");
        assertThat(status.yearMonth()).isEqualTo("2026-08");

        // 전월실적 35만 → 30만 구간 판정(충족, 통합한도 2만)
        assertThat(status.prevPerformanceAmount()).isEqualTo(350_000L);
        assertThat(status.performanceMet()).isTrue();
        assertThat(status.sharedLimit()).isEqualTo(20_000L);
        assertThat(status.sharedLimitUsed()).isEqualTo(8_000L);

        // 당월누적 24만 → 다음 목표 30만, 남은 6만, 달성률 80%
        assertThat(status.currentPerformanceAmount()).isEqualTo(240_000L);
        assertThat(status.targetPerformance()).isEqualTo(300_000L);
        assertThat(status.remainingPerformance()).isEqualTo(60_000L);
        assertThat(status.achievementRate()).isEqualByComparingTo("80.0");
    }

    @Test
    @DisplayName("혜택별 현황: 묶음은 그룹 합산, 한도 없는 혜택은 세 값이 null이다")
    void 혜택별_현황이_묶음과_무한도를_구분해_내려준다() {
        CardMonthlyStatus status = cardStatusService.getCardStatus(memberId, mainUserCardId, BASE_MONTH);

        assertThat(status.benefits()).extracting(BenefitUsageStatus::benefitId)
                .containsExactly(telecomBenefitId, utilityBenefitId, storeBenefitId, unlimitedBenefitId);

        // 통신 1,000 + 공과금 2,000 = 그룹 소진 3,000 / 공유 한도 5,000 → 잔여 2,000, 60%
        assertThat(status.benefits()).filteredOn(benefit -> "LIVING".equals(benefit.limitGroupCode()))
                .allSatisfy(benefit -> {
                    assertThat(benefit.usedAmount()).isEqualTo(3_000L);
                    assertThat(benefit.monthlyLimit()).isEqualTo(5_000L);
                    assertThat(benefit.remainingLimit()).isEqualTo(2_000L);
                    assertThat(benefit.usageRate()).isEqualByComparingTo("60.0");
                });

        BenefitUsageStatus store = benefitOf(status, storeBenefitId);
        assertThat(store.usedAmount()).isEqualTo(8_000L);
        assertThat(store.remainingLimit()).isEqualTo(2_000L);
        assertThat(store.usageRate()).isEqualByComparingTo("80.0");

        // 한도 없는 혜택 — 목록에는 담되 잔여·이용률은 정의되지 않아 null(0이 아니다)
        BenefitUsageStatus unlimited = benefitOf(status, unlimitedBenefitId);
        assertThat(unlimited.usedAmount()).isEqualTo(1_500L);
        assertThat(unlimited.monthlyLimit()).isNull();
        assertThat(unlimited.remainingLimit()).isNull();
        assertThat(unlimited.usageRate()).isNull();
    }

    @Test
    @DisplayName("증정 혜택은 조회 단계에서 빠져 이용 현황에 잡히지 않는다")
    void 증정_혜택은_현황에_잡히지_않는다() {
        jdbc.update(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, "
                        + "target_type, require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, '공항 라운지 무료 이용', 'GIFT', 'FIXED', 0, 'ALL', 'N', 'N', 'Y')",
                mainCardId);

        CardMonthlyStatus status = cardStatusService.getCardStatus(memberId, mainUserCardId, BASE_MONTH);

        // 증정은 한도가 없어, 무한도 혜택을 담기 시작한 지금은 조회 SQL 제외만이 방어선이다
        assertThat(status.benefits()).extracting(BenefitUsageStatus::benefitName)
                .doesNotContain("공항 라운지 무료 이용");
    }

    @Test
    @DisplayName("전체 현황은 묶음을 한 줄로 접고 실적이 임박한 카드를 브리핑한다")
    void 전체_현황은_요약과_브리핑을_내려준다() {
        CardStatusOverview overview = cardStatusService.getOverview(memberId, BASE_MONTH);

        // 순서는 user_card_id 오름차순으로 고정된다 — 홈 카드 순서가 호출마다 바뀌면 안 된다
        assertThat(overview.cards()).extracting(CardStatusSummary::userCardId)
                .containsExactly(mainUserCardId, plainUserCardId);

        CardStatusSummary mainCard = summaryOf(overview, mainUserCardId);
        // 통신·공과금이 한 줄로 접히고, 편의점·무한도가 각각 한 줄 → 4개 혜택이 3줄로
        assertThat(mainCard.benefitsSummary()).hasSize(3);
        BenefitSummary living = mainCard.benefitsSummary().get(0);
        assertThat(living.benefitId()).isEqualTo(telecomBenefitId);
        assertThat(living.benefitName()).isEqualTo("통신 10% 할인 외 1건");
        assertThat(living.remainingLimit()).isEqualTo(2_000L);
        assertThat(mainCard.benefitsSummary().get(2).remainingLimit()).isNull();

        // 실적 조건 없는 카드는 브리핑 후보가 아니므로 생활카드(80%)가 뽑힌다
        assertThat(overview.briefing()).isNotNull();
        assertThat(overview.briefing().userCardId()).isEqualTo(mainUserCardId);
        assertThat(overview.briefing().achievementRate()).isEqualByComparingTo("80.0");
        assertThat(overview.briefing().type()).isEqualTo(BriefingType.PERFORMANCE_NEAR);
        // 문구는 달마다 갈리므로 채워진 값만 고정한다. 카드 수는 보유 수(2)다.
        assertThat(overview.briefing().message()).contains("2장", "현황IT_생활카드", "80%");
    }

    @Test
    @DisplayName("실적 미충족 카드는 실적 조건부 혜택이 홈 요약에서 빠지되 상세에는 남는다")
    void 실적_미충족_카드의_조건부_혜택은_요약에서만_빠진다() {
        // 편의점 혜택에 실적 조건을 걸고, 전월실적을 0원 구간으로 떨어뜨려 미충족을 만든다
        jdbc.update("UPDATE benefit SET require_performance = 'Y' WHERE benefit_id = ?", storeBenefitId);
        jdbc.update("UPDATE user_card_monthly_state SET prev_performance_amount = 0 "
                + "WHERE user_card_id = ? AND base_year_month = '2026-08'", mainUserCardId);

        CardMonthlyStatus detail = cardStatusService.getCardStatus(memberId, mainUserCardId, BASE_MONTH);
        assertThat(detail.performanceMet()).isFalse();
        // 상세는 "이 카드에 어떤 혜택이 있나"라서 그대로 담고, 플래그로 구분만 한다
        assertThat(benefitOf(detail, storeBenefitId).requirePerformance()).isTrue();

        CardStatusSummary summary = summaryOf(
                cardStatusService.getOverview(memberId, BASE_MONTH), mainUserCardId);
        // 홈은 "지금 쓸 수 있는 것"이라 빠진다 — 이번 달 계산기가 적용하지 않는 혜택이다
        assertThat(summary.benefitsSummary()).extracting(BenefitSummary::benefitId)
                .doesNotContain(storeBenefitId);
    }

    @Test
    @DisplayName("상태 행이 없는 카드도 0으로 조회되고, 조회가 행을 만들지 않는다")
    void 상태_행이_없어도_조회되고_행을_만들지_않는다() {
        CardMonthlyStatus status = cardStatusService.getCardStatus(memberId, plainUserCardId, BASE_MONTH);

        assertThat(status.prevPerformanceAmount()).isZero();
        assertThat(status.currentPerformanceAmount()).isZero();
        assertThat(status.sharedLimitUsed()).isZero();
        // 실적 구간이 0원 하나뿐 → 목표 0, 달성률은 0%가 아니라 null("실적 조건 없음")
        assertThat(status.targetPerformance()).isZero();
        assertThat(status.achievementRate()).isNull();
        assertThat(status.performanceMet()).isFalse();

        // 조회는 읽기만 한다 — 행 생성은 정산(결제)의 몫이다
        Integer rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_card_monthly_state WHERE user_card_id = ?",
                Integer.class, plainUserCardId);
        assertThat(rowCount).isZero();
    }

    @Test
    @DisplayName("남의 카드와 없는 카드는 구분 없이 404다")
    void 소유가_아니거나_없는_카드는_404다() {
        assertThatThrownBy(() -> cardStatusService.getCardStatus(memberId, otherUserCardId, BASE_MONTH))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("찾을 수 없");

        assertThatThrownBy(() -> cardStatusService.getCardStatus(memberId, 999_999_999L, BASE_MONTH))
                .isInstanceOf(BusinessException.class);

        // 남의 카드는 그 주인이 조회하면 정상이다 — 막는 것은 소유권이지 카드가 아니다
        assertThat(cardStatusService.getCardStatus(otherMemberId, otherUserCardId, BASE_MONTH))
                .isNotNull();
    }

    @Test
    @DisplayName("보유 카드가 없으면 에러가 아니라 카드 등록 안내를 내려준다")
    void 보유_카드가_없으면_등록을_안내한다() {
        long emptyMemberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "status-it-empty@test.local", "x", "현황빈회원", "현황IT빈별명");

        CardStatusOverview overview = cardStatusService.getOverview(emptyMemberId, BASE_MONTH);

        assertThat(overview.cards()).isEmpty();
        // 카드가 없는 것은 정상 상태다. 빈 화면을 두지 않고 다음에 할 일을 안내한다.
        assertThat(overview.briefing().type()).isEqualTo(BriefingType.NO_CARD);
        assertThat(overview.briefing().userCardId()).isNull();
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 시드와 겹치지 않도록 IT 전용 코드('IT_STATUS_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        memberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "status-it@test.local", "x", "현황테스트회원", "현황IT별명");
        otherMemberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "status-it-other@test.local", "x", "현황남회원", "현황IT남별명");

        long parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_STATUS_LIVING", "IT현황대분류");
        long telecomCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_STATUS_TELECOM", "IT현황통신", parentCategoryId);

        long cardCompanyId = insert(
            "INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
            "TEST_CARD", "테스트카드사"
        );

        mainCardId = insert(
                "INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "현황IT_생활카드", cardCompanyId, "CREDIT");
        long plainCardId = insert(
                "INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
                "현황IT_실적없는카드", cardCompanyId, "CREDIT");

        mainUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, mainCardId, "3333-****-****-3333");
        plainUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, plainCardId, "4444-****-****-4444");
        otherUserCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                otherMemberId, mainCardId, "5555-****-****-5555");

        // 생활카드: 0원 구간 + 30만 구간(통합한도 2만) / 실적없는카드: 0원 구간만
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, NULL)", mainCardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 20000)", mainCardId);
        jdbc.update("INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, NULL)", plainCardId);

        // 통신·공과금은 LIVING 코드로 월 5,000원 한도를 공유한다(묶음 한도)
        telecomBenefitId = insertBenefit(mainCardId, "통신 10% 할인", telecomCategoryId, 5_000L, "LIVING");
        utilityBenefitId = insertBenefit(mainCardId, "공과금 10% 할인", telecomCategoryId, 5_000L, "LIVING");
        storeBenefitId = insertBenefit(mainCardId, "편의점 10% 할인", telecomCategoryId, 10_000L, null);
        unlimitedBenefitId = insertBenefit(mainCardId, "전 가맹점 0.7% 적립", telecomCategoryId, null, null);

        // 생활카드 기준월 상태: 전월실적 35만(이월됨), 당월누적 24만, 통합한도 8,000 소진
        jdbc.update(
                "INSERT INTO user_card_monthly_state (user_card_id, base_year_month, "
                        + "prev_performance_amount, current_performance_amount, shared_limit_used) "
                        + "VALUES (?, '2026-08', 350000, 240000, 8000)",
                mainUserCardId);

        insertUsage(telecomBenefitId, 1_000L);
        insertUsage(utilityBenefitId, 2_000L);
        insertUsage(storeBenefitId, 8_000L);
        insertUsage(unlimitedBenefitId, 1_500L);
    }

    private long insertBenefit(long cardId, String name, long categoryId, Long monthlyLimit, String groupCode) {
        return insert(
                "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, apply_timing, "
                        + "target_type, target_category_id, monthly_limit, limit_group_code, "
                        + "require_performance, use_shared_limit, is_active) "
                        + "VALUES (?, ?, 'DISCOUNT', 'RATE', 10.00, 'BILLED', 'CATEGORY', ?, ?, ?, 'N', 'Y', 'Y')",
                cardId, name, categoryId, monthlyLimit, groupCode);
    }

    private void insertUsage(long benefitId, long usedAmount) {
        jdbc.update(
                "INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, "
                        + "used_amount, used_count, last_applied_date, daily_used_count, daily_used_amount) "
                        + "VALUES (?, ?, '2026-08', ?, 1, '2026-08-10', 1, 0)",
                mainUserCardId, benefitId, usedAmount);
    }

    private BenefitUsageStatus benefitOf(CardMonthlyStatus status, long benefitId) {
        return status.benefits().stream()
                .filter(benefit -> benefit.benefitId() == benefitId)
                .findFirst().orElseThrow();
    }

    private CardStatusSummary summaryOf(CardStatusOverview overview, long userCardId) {
        return overview.cards().stream()
                .filter(card -> card.userCardId() == userCardId)
                .findFirst().orElseThrow();
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
