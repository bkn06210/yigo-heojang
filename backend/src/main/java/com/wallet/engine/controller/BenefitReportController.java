package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.dto.BenefitReport;
import com.wallet.engine.service.BenefitReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 한 달 혜택 리포트.
 *
 * 요약(총액·최대 부문) → 부문별 목록 → 부문 안의 거래까지 한 응답에 담는다.
 * 화면이 단계적으로 파고들지만 조회를 나누면 그 사이 결제가 일어났을 때 합계와 상세가 어긋난다.
 */
@RestController
@RequestMapping("/api/benefits/report")
public class BenefitReportController {

    private final BenefitReportService benefitReportService;

    public BenefitReportController(BenefitReportService benefitReportService) {
        this.benefitReportService = benefitReportService;
    }

    /**
     * @param yearMonth 기준 연월(YYYY-MM). 생략하면 이번 달
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BenefitReport>> getReport(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {

        BenefitReport report = benefitReportService.getReport(memberId, resolveBaseMonth(yearMonth));

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 형식이 틀린 값은 400으로 막는다. 조용히 이번 달로 넘기면 사용자가 요청한 달과 다른 달의
     * 숫자를 그 달의 값인 것처럼 보게 된다.
     */
    private YearMonth resolveBaseMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.from(LocalDate.now());
        }
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
    }
}
