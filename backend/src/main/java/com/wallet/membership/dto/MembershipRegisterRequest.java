package com.wallet.membership.dto;

import javax.validation.constraints.NotNull;

public class MembershipRegisterRequest {

    @NotNull(message = "pointProviderId는 필수입니다.")
    private Long pointProviderId;

    public Long getPointProviderId() {
        return pointProviderId;
    }

    public void setPointProviderId(Long pointProviderId) {
        this.pointProviderId = pointProviderId;
    }
}
