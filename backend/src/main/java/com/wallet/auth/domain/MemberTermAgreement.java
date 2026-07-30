package com.wallet.auth.domain;

public class MemberTermAgreement {
    private final Long memberId;
    private final Long termsVersionId;
    private final Boolean agreed;

    public MemberTermAgreement(Long memberId, Long termsVersionId, Boolean agreed) {
        this.memberId = memberId;
        this.termsVersionId = termsVersionId;
        this.agreed = agreed;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getTermsVersionId() {
        return termsVersionId;
    }

    public Boolean getAgreed() {
        return agreed;
    }
}