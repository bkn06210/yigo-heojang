package com.wallet.membership.dto;

public class MembershipRegisterCommand {

    private Long membershipRegisterId;
    private Long memberId;
    private Long pointProviderId;

    public Long getMembershipRegisterId() {
        return membershipRegisterId;
    }

    public void setMembershipRegisterId(Long membershipRegisterId) {
        this.membershipRegisterId = membershipRegisterId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getPointProviderId() {
        return pointProviderId;
    }

    public void setPointProviderId(Long pointProviderId) {
        this.pointProviderId = pointProviderId;
    }
}
