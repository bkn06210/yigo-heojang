package com.wallet.engine.reconciliation;

import com.wallet.engine.model.ReconciliationDiscrepancy;
import com.wallet.engine.model.ReconciliationReport;
import com.wallet.engine.model.ReconciliationUnverifiable;
import com.wallet.engine.service.StateReconciliationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * 상태 대사 실행 도구 — 연결된 DB의 전 회원·전 기간을 대조해 결과를 파일로 남긴다.
 *
 * <b>테스트가 아니라 도구다.</b> 단언이 없다. 공용 개발 DB에는 손으로 넣은 상태 행이 섞일 수 있어
 * "불일치 0건"을 단언하면 코드와 무관하게 깨진다. 대사 로직 자체의 검증은
 * StateReconcilerTest와 StateReconciliationServiceIntegrationTest가 한다.
 * <pre>
 *   ./mvnw -o test -Dtest=StateReconciliationRunTest -Dsurefire.failIfNoSpecifiedTests=false \
 *          -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.engine.extension.DisabledCondition
 * </pre>
 * 마지막 옵션이 없으면 클래스를 골라 지정해도 &#64;Disabled 때문에 건너뛴다.
 * 회원 한 명만 보려면 {@code -Dreconciliation.memberId=1} 을 더한다.
 *
 * <b>DB를 바꾸지 않는다.</b> 조회만 하며, 어긋난 값을 고치지도 않는다.
 * 결과는 target/reconciliation-report.txt 에 쓴다.
 */
@Disabled("상태 대사 실행 도구 — 점검할 때만 수동 실행")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
class StateReconciliationRunTest {

    private static final Path OUTPUT = Path.of("target", "reconciliation-report.txt");

    /** 회원 한 명만 볼 때 쓰는 시스템 프로퍼티. 없으면 전 회원을 대조한다 */
    private static final String MEMBER_ID_PROPERTY = "reconciliation.memberId";

    @Autowired
    private StateReconciliationService reconciliationService;

    private JdbcTemplate jdbc;

    @Autowired
    void setDataSource(javax.sql.DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void 전체_상태_대사() throws IOException {
        YearMonth fromMonth = earliestMonth();
        YearMonth toMonth = latestMonth();
        if (fromMonth == null || toMonth == null) {
            Files.writeString(OUTPUT, "대조할 상태도 소비내역도 없다\n", StandardCharsets.UTF_8);
            return;
        }

        Long memberId = Long.getLong(MEMBER_ID_PROPERTY);

        // 첫 실행은 커넥션 확보·매퍼 초기화가 섞이므로 한 번 돌려 데운 뒤 잰다
        reconciliationService.reconcile(fromMonth, toMonth, memberId);
        long startedAt = System.nanoTime();
        ReconciliationReport report = reconciliationService.reconcile(fromMonth, toMonth, memberId);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        String text = render(fromMonth, toMonth, memberId, report, elapsedMillis);
        Files.writeString(OUTPUT, text, StandardCharsets.UTF_8);
        System.out.println(text);
    }

    /** 상태와 소비내역 중 더 이른 쪽의 달. 한쪽에만 있는 달도 대조 대상이어야 한다 */
    private YearMonth earliestMonth() {
        return parse(jdbc.queryForObject("""
                SELECT LEAST(
                    COALESCE((SELECT MIN(base_year_month) FROM user_card_monthly_state), '9999-12'),
                    COALESCE((SELECT MIN(base_year_month) FROM user_benefit_usage), '9999-12'),
                    COALESCE((SELECT DATE_FORMAT(MIN(payment_date), '%Y-%m') FROM expense), '9999-12'))
                """, String.class), "9999-12");
    }

    private YearMonth latestMonth() {
        return parse(jdbc.queryForObject("""
                SELECT GREATEST(
                    COALESCE((SELECT MAX(base_year_month) FROM user_card_monthly_state), '0000-01'),
                    COALESCE((SELECT MAX(base_year_month) FROM user_benefit_usage), '0000-01'),
                    COALESCE((SELECT DATE_FORMAT(MAX(payment_date), '%Y-%m') FROM expense), '0000-01'))
                """, String.class), "0000-01");
    }

    private YearMonth parse(String yearMonth, String emptyMarker) {
        return yearMonth == null || emptyMarker.equals(yearMonth) ? null : YearMonth.parse(yearMonth);
    }

    private String render(YearMonth fromMonth, YearMonth toMonth, Long memberId,
                          ReconciliationReport report, long elapsedMillis) {
        Map<Long, String> cardLabels = labels("""
                SELECT uc.user_card_id, CONCAT('회원 ', uc.member_id, ' / ', c.card_name)
                FROM user_card uc JOIN card c ON c.card_id = uc.card_id
                """);
        Map<Long, String> benefitLabels = labels(
                "SELECT benefit_id, CONCAT(benefit_name, ' [', calc_method, ']') FROM benefit");

        StringBuilder sb = new StringBuilder();
        sb.append("상태 대사 결과 (%s ~ %s, %s)%n".formatted(
                fromMonth, toMonth, memberId == null ? "전 회원" : "회원 " + memberId));
        sb.append("  읽은 것    카드 월 상태 %,d행 · 혜택 소진 %,d행 · 소비내역 %,d건%n".formatted(
                report.stateRowCount(), report.usageRowCount(), report.expenseCount()));
        sb.append("  대조       %,d건 = 일치 %,d + 불일치 %,d  (별도: 대조 불가 %,d건)%n".formatted(
                report.comparisonCount(), report.matchedCount(), report.mismatchedCount(),
                report.unverifiableCount()));
        sb.append("  소요       %,d ms%n%n".formatted(elapsedMillis));

        sb.append("불일치 %,d건%n".formatted(report.mismatchedCount()));
        for (ReconciliationDiscrepancy discrepancy : report.discrepancies()) {
            sb.append("  %-20s %-21s %s  user_card=%d(%s)%s  저장=%s 원본=%s 차이=%+,d%n".formatted(
                    discrepancy.target(), discrepancy.kind(), discrepancy.baseYearMonth(),
                    discrepancy.userCardId(), cardLabels.get(discrepancy.userCardId()),
                    discrepancy.benefitId() == null ? "" : "  benefit=%d(%s)".formatted(
                            discrepancy.benefitId(), benefitLabels.get(discrepancy.benefitId())),
                    display(discrepancy.storedValue()), display(discrepancy.sourceValue()),
                    discrepancy.difference()));
        }

        sb.append("%n대조 불가 %,d건 (원본에 흔적이 없어 맞는지 가릴 수 없는 값)%n"
                .formatted(report.unverifiableCount()));
        for (ReconciliationUnverifiable unverifiable : report.unverifiables()) {
            sb.append("  %-20s %s  user_card=%d(%s)  benefit=%d(%s)  저장=%,d 원본에 보이는 만큼=%,d%n".formatted(
                    unverifiable.target(), unverifiable.baseYearMonth(),
                    unverifiable.userCardId(), cardLabels.get(unverifiable.userCardId()),
                    unverifiable.benefitId(), benefitLabels.get(unverifiable.benefitId()),
                    unverifiable.storedValue(), unverifiable.tracedValue()));
        }
        return sb.toString();
    }

    private String display(Long value) {
        return value == null ? "(없음)" : "%,d".formatted(value);
    }

    private Map<Long, String> labels(String sql) {
        Map<Long, String> labels = new HashMap<>();
        jdbc.query(sql, rs -> {
            labels.put(rs.getLong(1), rs.getString(2));
        });
        return labels;
    }
}
