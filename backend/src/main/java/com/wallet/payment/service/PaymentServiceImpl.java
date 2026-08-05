package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentCommand;
import com.wallet.payment.dto.PaymentPointWalletResponse;
import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;
import com.wallet.payment.mapper.PaymentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentProcessResponse processPayment(
            Long userId,
            PaymentRequest request
    ) {
        validateRequest(request);

        int cardCount = paymentMapper.countUserCardByUserId(userId, request.getUserCardId());
        if (cardCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "蹂몄씤 移대뱶媛 ?꾨떃?덈떎.");
        }

        int categoryCount = paymentMapper.countCategoryById(request.getCategoryId());
        if (categoryCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "移댄뀒怨좊━瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        PaymentCommand command = new PaymentCommand();
        command.setUserId(userId);
        command.setUserCardId(request.getUserCardId());
        command.setCategoryId(request.getCategoryId());
        command.setMerchantId(request.getMerchantId());
        command.setMerchantName(request.getMerchantName());
        command.setPaymentAmount(request.getPaymentAmount());
        command.setPaymentStatus("SUCCESS");
        command.setPaymentChannel("MOCK");
        command.setIsRecommendBased(Boolean.TRUE.equals(request.getIsRecommendBased()) ? "Y" : "N");
        command.setPaymentType(defaultValue(request.getPaymentType(), "CARD"));
        command.setInterestFreeYn(defaultValue(request.getInterestFreeYn(), "N"));

        /*
         * 1. 寃곗젣 ?깃났 ???뚮퉬?댁뿭 ?앹꽦
         * 2. 寃곗젣 ?대젰 ???
         * 3. ?ъ씤???꾩떆 ?곷┰
         *
         * ?쒗깮 ?붿쭊 ?곕룞 ?꾩씠誘濡?appliedBenefitId, discountAmount???꾩쭅 0/null 泥섎━.
         */
        paymentMapper.insertExpense(command);
        paymentMapper.insertPayment(command);

        Long savedPoint = calculateSavedPoint(request.getPaymentAmount());
        Long totalPoint = 0L;

        PaymentPointWalletResponse wallet = paymentMapper.selectDefaultPointWallet(userId);

        if (wallet != null && savedPoint > 0) {
            paymentMapper.updatePointWallet(wallet.getPointWalletId(), savedPoint);

            String content = command.getMerchantName() + " 寃곗젣濡??ъ씤???곷┰";
            paymentMapper.insertPointHistory(
                    userId,
                    wallet.getPointWalletId(),
                    command.getExpenseId(),
                    savedPoint,
                    content
            );

            totalPoint = wallet.getTotalPoint() + savedPoint;
        }

        PaymentProcessResponse response = new PaymentProcessResponse();
        response.setPaymentId(command.getPaymentId());
        response.setExpenseId(command.getExpenseId());
        response.setPaymentStatus("SUCCESS");
        response.setPaymentChannel("MOCK");
        response.setAppliedBenefitId(null);
        response.setAppliedBenefitName(null);
        response.setDiscountAmount(0L);
        response.setSavedPoint(savedPoint);
        response.setTotalPoint(totalPoint);
        response.setCompletedAt(null);

        PaymentResultResponse result =
                paymentMapper.selectPaymentResult(userId, command.getPaymentId());

        if (result != null) {
            response.setCompletedAt(result.getCompletedAt());
            response.setSavedPoint(result.getSavedPoint());
            response.setTotalPoint(result.getTotalPoint());
        }

        return response;
    }

    @Override
    public PaymentResultResponse getPaymentResult(
            Long userId,
            Long paymentId
    ) {
        PaymentResultResponse result =
                paymentMapper.selectPaymentResult(userId, paymentId);

        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "寃곗젣 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        return result;
    }

    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "?붿껌 蹂몃Ц???꾩슂?⑸땲??");
        }

        if (request.getUserCardId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCardId???꾩닔?낅땲??");
        }

        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId???꾩닔?낅땲??");
        }

        if (request.getPaymentAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentAmount???꾩닔?낅땲??");
        }

        if (request.getPaymentAmount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "寃곗젣湲덉븸? 1???댁긽?댁뼱???⑸땲??");
        }

        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantName? ?꾩닔?낅땲??");
        }
    }

    private Long calculateSavedPoint(Long paymentAmount) {
        /*
         * ?꾩떆 ?뺤콉:
         * ?쒗깮/?ъ씤???붿쭊 ?곌껐 ?꾧퉴吏 寃곗젣湲덉븸??1%瑜??곷┰ 泥섎━.
         * ?? 10,000??寃곗젣 ??100?ъ씤??
         */
        return paymentAmount / 100;
    }

    private String defaultValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
