package com.wallet.engine.service;

import com.wallet.engine.dao.BenefitReportMapper;
import com.wallet.engine.dao.dto.BenefitReportRow;
import com.wallet.engine.dto.BenefitReport;
import com.wallet.engine.dto.BenefitReportCategory;
import com.wallet.engine.dto.BenefitReportDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 달 혜택 리포트 — 얼마나 받았고 어느 부문에서 받았나.
 *
 * <p>계산이 아니라 집계다. 혜택액은 결제 시점에 엔진이 이미 확정해 소비내역에 적어 둔 값이라
 * 여기서 다시 계산하지 않는다. 다시 계산하면 그때의 한도 상태를 재현할 수 없어 실제와 달라진다.
 *
 * <p>총액·부문별 합계·거래 목록을 한 번의 조회로 만든다. 각각 따로 구하면 조회 사이에 결제가
 * 일어났을 때 합계와 상세가 어긋난다.
 */
@Service
public class BenefitReportService {

    private final BenefitReportMapper benefitReportMapper;

    public BenefitReportService(BenefitReportMapper benefitReportMapper) {
        this.benefitReportMapper = benefitReportMapper;
    }

    @Transactional(readOnly = true)
    public BenefitReport getReport(long memberId, YearMonth baseMonth) {
        String yearMonth = baseMonth.toString();
        List<BenefitReportRow> rows = benefitReportMapper.findBenefitedExpenses(memberId, yearMonth);

        // 조회가 결제일시 내림차순이라 넣는 순서가 곧 상세 목록의 순서다.
        Map<Long, List<BenefitReportRow>> byCategory = new LinkedHashMap<>();
        for (BenefitReportRow row : rows) {
            byCategory.computeIfAbsent(row.getGroupCategoryId(), key -> new ArrayList<>()).add(row);
        }

        List<BenefitReportCategory> categories = new ArrayList<>();
        long total = 0;
        for (List<BenefitReportRow> group : byCategory.values()) {
            long amount = group.stream().mapToLong(BenefitReportRow::getDiscountAmount).sum();
            total += amount;
            categories.add(new BenefitReportCategory(
                    group.get(0).getGroupCategoryId(),
                    group.get(0).getGroupCategoryName(),
                    group.get(0).getParentCategoryName(),
                    amount,
                    group.stream().map(BenefitReportService::toDetail).toList()));
        }

        // 동점이면 categoryId 오름차순 — 같은 데이터로 같은 화면이 나와야 한다.
        categories.sort(Comparator
                .comparingLong(BenefitReportCategory::getBenefitAmount).reversed()
                .thenComparingLong(BenefitReportCategory::getCategoryId));

        BenefitReportCategory top = categories.isEmpty() ? null : categories.get(0);
        return new BenefitReport(yearMonth, total,
                top == null ? null : top.getCategoryId(),
                top == null ? null : top.getCategoryName(),
                top == null ? null : top.getBenefitAmount(),
                categories);
    }

    private static BenefitReportDetail toDetail(BenefitReportRow row) {
        return new BenefitReportDetail(
                row.getExpenseId(), row.getMerchantName(), row.getCardName(),
                row.getAmount(), row.getDiscountAmount(), row.getBenefitName(),
                row.getPaymentDate());
    }
}
