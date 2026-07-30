package com.wallet.payment.dto;

public class PaymentQrPayResponse {

    private Long paymentId;
    private Long expenseId;
    private String status;
    private String message;

    public PaymentQrPayResponse() {
    }

    public PaymentQrPayResponse(Long paymentId, Long expenseId, String status) {
        this.paymentId = paymentId;
        this.expenseId = expenseId;
        this.status = status;
    }

    public PaymentQrPayResponse(Long paymentId, Long expenseId, String status, String message) {
        this.paymentId = paymentId;
        this.expenseId = expenseId;
        this.status = status;
        this.message = message;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}