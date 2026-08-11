package com.wallet.membership.dto;

public class MembershipCancelResponse {

    private Long membershipRegisterId;
    private String registerStatus;
    private String canceledAt;

    public Long getMembershipRegisterId() {
        return membershipRegisterId;
    }

    public void setMembershipRegisterId(Long membershipRegisterId) {
        this.membershipRegisterId = membershipRegisterId;
    }

    public String getRegisterStatus() {
        return registerStatus;
    }

    public void setRegisterStatus(String registerStatus) {
        this.registerStatus = registerStatus;
    }

    public String getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(String canceledAt) {
        this.canceledAt = canceledAt;
    }
}
