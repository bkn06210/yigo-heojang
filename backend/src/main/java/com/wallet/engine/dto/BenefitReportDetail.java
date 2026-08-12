package com.wallet.engine.dto;

import java.time.LocalDateTime;

/** 혜택을 받은 거래 한 건. 부문을 눌러 상세를 열었을 때 보이는 줄이다. */
public class BenefitReportDetail {

    private long expenseId;
    private String merchantName;
    private String cardName;

    /** 결제금액(원). 카드 승인액이다. */
    private long paymentAmount;

    /** 실제 받은 할인·적립액(원). */
    private long benefitAmount;

    /** 적용된 혜택명. 소비내역에 기록된 값 그대로다. */
    private String benefitName;

    private LocalDateTime paymentDate;

    public BenefitReportDetail(long expenseId, String merchantName, String cardName,
                               long paymentAmount, long benefitAmount, String benefitName,
                               LocalDateTime paymentDate) {
        this.expenseId = expenseId;
        this.merchantName = merchantName;
        this.cardName = cardName;
        this.paymentAmount = paymentAmount;
        this.benefitAmount = benefitAmount;
        this.benefitName = benefitName;
        this.paymentDate = paymentDate;
    }

    public long getExpenseId() {
        return expenseId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getCardName() {
        return cardName;
    }

    public long getPaymentAmount() {
        return paymentAmount;
    }

    public long getBenefitAmount() {
        return benefitAmount;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
}
