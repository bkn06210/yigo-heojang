package com.wallet.membership.dto;

import java.util.List;

public class MyMembershipListResponse {

    private List<MyMembershipResponse> memberships;

    public MyMembershipListResponse(List<MyMembershipResponse> memberships) {
        this.memberships = memberships;
    }

    public List<MyMembershipResponse> getMemberships() {
        return memberships;
    }
}
