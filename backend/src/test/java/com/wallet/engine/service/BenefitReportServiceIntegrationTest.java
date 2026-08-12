package com.wallet.engine.service;

import com.wallet.engine.dto.BenefitReport;
import com.wallet.engine.dto.BenefitReportCategory;
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

/**
 * 혜택 리포트 집계의 MySQL 통합 테스트.
 *
 * 확인하는 것은 다음 넷이다.
 *   ① 혜택을 받은 거래만 담는다 (취소·혜택 0원 제외)
 *   ② 거래에 기록된 카테고리 단위로 묶이고 상위 분류가 함께 담긴다
 *   ③ 부문이 혜택 금액 내림차순으로 정렬되고 1위가 최대 부문이 된다
 *   ④ 총액이 부문 합계와 일치한다
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class BenefitReportServiceIntegrationTest {

    private static final YearMonth BASE_MONTH = YearMonth.of(2026, 8);

    @Autowired
    private BenefitReportService benefitReportService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long userCardId;
    private long cafeCategoryId;      // 중분류 카페 — 상위는 외식
    private long cvsCategoryId;       // 중분류 편의점 — 상위는 쇼핑

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @Test
    @DisplayName("거래에 기록된 카테고리 단위로 합산하고 상위 분류를 함께 담는다")
    void 중분류_단위로_묶는다() {
        BenefitReport report = benefitReportService.getReport(memberId, BASE_MONTH);

        // 카페 두 건이 '카페'로 묶인다. '외식'으로 올려 묶으면 어디서 아꼈는지가 사라진다.
        BenefitReportCategory cafe = categoryOf(report, cafeCategoryId);
        assertThat(cafe.getCategoryName()).isEqualTo("IT리포트카페");
        assertThat(cafe.getParentCategoryName()).isEqualTo("IT리포트외식");
        assertThat(cafe.getBenefitAmount()).isEqualTo(3_000L);
        assertThat(cafe.getDetails()).hasSize(2);
    }

    @Test
    @DisplayName("부문은 혜택 금액이 큰 순서이고 1위가 최대 혜택 부문이 된다")
    void 최대_혜택_부문을_알려준다() {
        BenefitReport report = benefitReportService.getReport(memberId, BASE_MONTH);

        assertThat(report.getCategories()).extracting(BenefitReportCategory::getCategoryName)
                .containsExactly("IT리포트카페", "IT리포트편의점");
        assertThat(report.getTopCategoryId()).isEqualTo(cafeCategoryId);
        assertThat(report.getTopCategoryName()).isEqualTo("IT리포트카페");
        assertThat(report.getTopCategoryBenefitAmount()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("총액은 부문 합계와 같다")
    void 총액은_부문_합계와_같다() {
        BenefitReport report = benefitReportService.getReport(memberId, BASE_MONTH);

        long sum = report.getCategories().stream()
                .mapToLong(BenefitReportCategory::getBenefitAmount).sum();

        assertThat(report.getTotalBenefitAmount()).isEqualTo(sum).isEqualTo(3_500L);
    }

    @Test
    @DisplayName("취소된 거래와 혜택 0원 거래는 담지 않는다")
    void 혜택을_받지_않은_거래는_제외한다() {
        BenefitReport report = benefitReportService.getReport(memberId, BASE_MONTH);

        // 픽스처에는 취소 1건(2,000원)과 혜택 0원 1건이 더 있다. 담기면 총액이 부풀고
        // 상세 목록에 받지도 않은 거래가 섞인다.
        assertThat(report.getCategories()).flatExtracting(BenefitReportCategory::getDetails)
                .hasSize(3);
        assertThat(report.getTotalBenefitAmount()).isEqualTo(3_500L);
    }

    @Test
    @DisplayName("혜택을 받은 거래가 없으면 총액 0에 최대 부문은 없다")
    void 받은_혜택이_없으면_최대_부문이_없다() {
        BenefitReport report = benefitReportService.getReport(memberId, YearMonth.of(2026, 1));

        assertThat(report.getTotalBenefitAmount()).isZero();
        assertThat(report.getCategories()).isEmpty();
        assertThat(report.getTopCategoryName()).isNull();
    }

    private BenefitReportCategory categoryOf(BenefitReport report, long categoryId) {
        return report.getCategories().stream()
                .filter(category -> category.getCategoryId() == categoryId)
                .findFirst().orElseThrow();
    }

    private void insertFixture() {
        cafeCategoryId = insertCategory("IT_REPORT_CAFE", "IT리포트카페",
                insertCategory("IT_REPORT_DINING", "IT리포트외식", null));
        cvsCategoryId = insertCategory("IT_REPORT_CVS", "IT리포트편의점",
                insertCategory("IT_REPORT_SHOPPING", "IT리포트쇼핑", null));

        jdbc.update("INSERT INTO member (email, password_hash, name, nickname, member_status)"
                + " VALUES ('benefit-report-it@example.com', 'x', '리포트IT', '리포트IT', 'ACTIVE')");
        memberId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update("INSERT INTO card_company (company_code, company_name)"
                + " VALUES ('IT_REPORT_COMPANY', 'IT리포트카드사')");
        long companyId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO card (card_company_id, card_name, card_type, annual_fee, is_active)"
                + " VALUES (?, '리포트IT_카드', 'CREDIT', 0, 'Y')", companyId);
        long cardId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        jdbc.update("INSERT INTO user_card (member_id, card_id, masked_card_number, card_status)"
                + " VALUES (?, ?, '1234-****-****-5678', 'ACTIVE')", memberId, cardId);
        userCardId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 카페 2건 3,000원
        insertExpense(cafeCategoryId, "리포트IT_카페A", 10_000L, 2_000L, "APPROVED");
        insertExpense(cafeCategoryId, "리포트IT_카페B", 5_000L, 1_000L, "APPROVED");
        // 편의점 1건 500원
        insertExpense(cvsCategoryId, "리포트IT_편의점", 3_000L, 500L, "APPROVED");
        // 담기면 안 되는 것들
        insertExpense(cafeCategoryId, "리포트IT_취소건", 20_000L, 2_000L, "CANCELED");
        insertExpense(cvsCategoryId, "리포트IT_혜택없음", 1_000L, 0L, "APPROVED");
    }

    private long insertCategory(String code, String name, Long parentCategoryId) {
        jdbc.update("INSERT INTO category (category_code, category_name, parent_category_id)"
                + " VALUES (?, ?, ?)", code, name, parentCategoryId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertExpense(long categoryId, String merchantName, long amount,
                               long discountAmount, String paymentStatus) {
        jdbc.update("INSERT INTO expense (member_id, user_card_id, category_id, merchant_name,"
                        + " amount, payment_date, input_type, payment_status, discount_amount)"
                        + " VALUES (?, ?, ?, ?, ?, '2026-08-10 12:00:00', 'MANUAL', ?, ?)",
                memberId, userCardId, categoryId, merchantName, amount, paymentStatus, discountAmount);
    }
}
