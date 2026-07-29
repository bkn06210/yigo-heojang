package com.wallet.engine.service;

import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.dto.PerformanceTransactionRow;
import com.wallet.engine.model.PerformanceStatus;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전월실적 경로(조회 매퍼 → 계산기 → 구간 판정)의 MySQL 통합 테스트.
 *
 * 실 MySQL(docker-compose의 yigo-mysql, schema.sql 적용 필요)에 연결한다. H2는 쓰지 않는다 —
 * user_card의 생성 컬럼·benefit의 CHECK 제약이 방언 차이로 뜨지 않기 때문이다.
 *
 * 검증하는 것은 계산 총액이 아니라 '매퍼 SQL과 경계 변환의 정확성'이다:
 *   ① 카테고리 상위 조인이 parentCategoryCode를 채운다(대분류 제외 상향 매칭의 전제)
 *   ② 날짜 경계 필터가 전월 거래만 가져온다(당월·전전월 제외)
 *   ③ payment_status='CANCELED' 행이 결과에서 빠진다
 *   ④ is_interest_free='Y' → assembler가 interestFree=true로 접는다
 *   ⑤ end-to-end: 픽스처 → 기대 PerformanceStatus(tierId·min·통합한도)
 *
 * 클래스 이름이 *Test라 surefire가 실행한다(*IT는 failsafe 몫이라 안 돈다).
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백하므로 시드 데이터에 의존·오염되지 않는다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class PerformanceEvaluationServiceIntegrationTest {

    // 기준월. 전월실적은 이 달의 직전 달(2026-07) 거래로 계산한다. 시계에 의존하지 않도록 고정한다.
    private static final YearMonth BASE_MONTH = YearMonth.of(2026, 8);

    @Autowired
    private PerformanceMapper performanceMapper;

    @Autowired
    private PerformanceEvaluationService performanceEvaluationService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long cardId;
    private long userCardId;
    private long parentCategoryId;
    private long childCategoryId;
    private long tierHighId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    @Test
    @DisplayName("조회 매퍼: 전월 거래만·취소 제외, 상위 카테고리 조인이 채워진다")
    void 조회_매퍼_기간과_취소_필터_및_상위_카테고리_조인() {
        LocalDateTime from = BASE_MONTH.minusMonths(1).atDay(1).atStartOfDay(); // 2026-07-01 00:00
        LocalDateTime to = BASE_MONTH.atDay(1).atStartOfDay();                  // 2026-08-01 00:00

        List<PerformanceTransactionRow> rows =
                performanceMapper.findTransactionsInPeriod(userCardId, from, to);

        // 전월 4건(E1~E4)만. 취소(E5)·당월(E6)은 빠진다.
        assertThat(rows).hasSize(4);
        assertThat(rows).noneMatch(row -> "CANCELED".equals(row.getPaymentStatus()));

        // 하위 카테고리 결제는 상위 코드가 채워지고(상향 매칭 전제), 상위 카테고리 결제는 NULL이다(LEFT JOIN).
        PerformanceTransactionRow child = rows.stream()
                .filter(row -> "IT_CHILD".equals(row.getCategoryCode()))
                .findFirst().orElseThrow();
        assertThat(child.getParentCategoryCode()).isEqualTo("IT_PARENT");

        PerformanceTransactionRow parent = rows.stream()
                .filter(row -> "IT_PARENT".equals(row.getCategoryCode()))
                .findFirst().orElseThrow();
        assertThat(parent.getParentCategoryCode()).isNull();

        // is_interest_free 원값은 'Y'/'N' 문자열 그대로 조회된다(boolean 변환은 assembler가 한다).
        assertThat(rows).anyMatch(row -> "Y".equals(row.getIsInterestFree()));
    }

    @Test
    @DisplayName("서비스 end-to-end: 제외 규칙 적용 후 실적으로 상위 구간을 판정한다")
    void 서비스_전월실적_계산_후_구간_판정() {
        // E1(20만, 인정) + E2(15만, 인정) = 35만. E3(간편결제)·E4(무이자)는 제외.
        // 35만 ≥ 30만 → 상위 구간(tierHigh), 통합한도 1만, 실적 충족.
        PerformanceStatus status =
                performanceEvaluationService.evaluate(userCardId, cardId, BASE_MONTH);

        assertThat(status.tierId()).isEqualTo(tierHighId);
        assertThat(status.minPerformanceAmount()).isEqualTo(300_000L);
        assertThat(status.sharedMonthlyLimit()).isEqualTo(10_000L);
        assertThat(status.performanceMet()).isTrue();
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 테스트 코드·값이 시드와 겹치지 않도록 IT 전용 코드('IT_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        memberId = insert(
                "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
                "perf-it@test.local", "x", "실적테스트회원", "별명A");

        cardId = insert(
                "INSERT INTO card (card_name, issuer, card_type) VALUES (?, ?, ?)",
                "실적테스트카드", "TEST", "CREDIT");

        parentCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
                "IT_PARENT", "IT대분류");
        childCategoryId = insert(
                "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
                "IT_CHILD", "IT중분류", parentCategoryId);

        userCardId = insert(
                "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
                memberId, cardId, "1234-****-****-5678");

        // 실적구간: 0원 구간(통합한도 없음, NULL) + 30만 구간(통합한도 1만).
        // 0원 구간은 판정이 항상 행 하나를 반환하기 위한 필수 행이라 ID는 참조하지 않아도 넣는다.
        jdbc.update(
                "INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 0, NULL)",
                cardId);
        tierHighId = insert(
                "INSERT INTO performance_tier (card_id, min_performance_amount, shared_monthly_limit) VALUES (?, 300000, 10000)",
                cardId);

        // 실적 제외 규칙: 간편결제·무이자할부 건 제외
        jdbc.update("INSERT INTO performance_exclusion (card_id, exclusion_type, exclusion_value) VALUES (?, 'PAYMENT_TYPE', 'SIMPLE_PAY')", cardId);
        jdbc.update("INSERT INTO performance_exclusion (card_id, exclusion_type, exclusion_value) VALUES (?, 'TRANSACTION_ATTR', 'INTEREST_FREE')", cardId);

        // 전월(2026-07) 거래
        insertExpense(childCategoryId, 200_000, "2026-07-15 10:00:00", "APPROVED", "CARD", "N");       // E1 인정
        insertExpense(parentCategoryId, 150_000, "2026-07-16 10:00:00", "APPROVED", "CARD", "N");      // E2 인정(상위 카테고리)
        insertExpense(childCategoryId, 100_000, "2026-07-17 10:00:00", "APPROVED", "SIMPLE_PAY", "N"); // E3 제외(간편결제)
        insertExpense(childCategoryId, 100_000, "2026-07-18 10:00:00", "APPROVED", "CARD", "Y");       // E4 제외(무이자)
        insertExpense(childCategoryId, 500_000, "2026-07-19 10:00:00", "CANCELED", "CARD", "N");       // E5 취소 → SQL에서 제외
        // 당월(2026-08) 거래 → 날짜 경계 밖
        insertExpense(childCategoryId, 999_999, "2026-08-05 10:00:00", "APPROVED", "CARD", "N");       // E6 범위 밖
    }

    private void insertExpense(long categoryId, long amount, String paymentDate,
                               String paymentStatus, String paymentType, String interestFree) {
        jdbc.update(
                "INSERT INTO expense (member_id, user_card_id, category_id, amount, payment_date, "
                        + "input_type, payment_status, payment_type, is_interest_free) "
                        + "VALUES (?, ?, ?, ?, ?, 'MANUAL', ?, ?, ?)",
                memberId, userCardId, categoryId, amount, paymentDate,
                paymentStatus, paymentType, interestFree);
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
