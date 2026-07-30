package com.wallet.membership.dto;

import java.util.List;

public class MembershipProviderListResponse {

    private List<MembershipProviderResponse> providers;

    public MembershipProviderListResponse(List<MembershipProviderResponse> providers) {
        this.providers = providers;
    }

    public List<MembershipProviderResponse> getProviders() {
        return providers;
    }

    public void setProviders(List<MembershipProviderResponse> providers) {
        this.providers = providers;
    }
}