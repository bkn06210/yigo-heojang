package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.CardStatusOverview;
import com.wallet.engine.service.CardStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 카드 현황 조회 API — 전체 보유 카드 현황(홈·목록)과 개별 카드 상세.
 *
 * 조회는 전부 서비스가 한다. 이 클래스는 요청 파라미터를 해석하고 응답 봉투를 씌우는 얇은 층이다.
 *
 * 회원 id는 <b>인증 필터가 검증해 넣어 둔 값</b>에서 꺼낸다 — 파라미터로 받으면 남의 현황을
 * 조회하는 것을 막을 수 없다. 기준월도 여기서 정해 서비스에 넘겨 서비스를 시계에서 떼어 둔다.
 */
@RestController
@RequestMapping("/api/cards")
public class CardStatusController {

    private final CardStatusService cardStatusService;

    public CardStatusController(CardStatusService cardStatusService) {
        this.cardStatusService = cardStatusService;
    }

    /**
     * 보유 카드 전부의 이번 달 현황과 홈 브리핑을 반환한다.
     * 카드가 여러 장이어도 한 번의 호출로 처리한다(카드마다 상세를 반복 호출하지 않는다).
     */
    @GetMapping("/monthly-status")
    public ResponseEntity<ApiResponse<CardStatusOverview>> getOverview(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {

        CardStatusOverview overview = cardStatusService.getOverview(memberId, resolveBaseMonth(yearMonth));

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    /**
     * 보유 카드 한 장의 실적·혜택 이용 현황을 반환한다.
     * 없는 카드와 남의 카드는 구분 없이 404다(존재 비노출).
     */
    @GetMapping("/{userCardId}/monthly-status")
    public ResponseEntity<ApiResponse<CardMonthlyStatus>> getCardStatus(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @PathVariable("userCardId") Long userCardId,
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {

        CardMonthlyStatus status =
                cardStatusService.getCardStatus(memberId, userCardId, resolveBaseMonth(yearMonth));

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 기준월 결정 — 생략하면 이번 달이다.
     *
     * 형식이 틀린 값은 400으로 막는다. 조용히 이번 달로 넘기면 사용자가 요청한 달과 다른 달의
     * 숫자를 그 달의 값인 것처럼 보게 되므로, 에러 없이 틀린 화면이 나오는 쪽이 더 나쁘다.
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
