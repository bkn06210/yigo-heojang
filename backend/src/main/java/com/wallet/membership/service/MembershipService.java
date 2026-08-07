package com.wallet.membership.service;

import com.wallet.membership.dto.MembershipCancelResponse;
import com.wallet.membership.dto.MembershipDetailResponse;
import com.wallet.membership.dto.MembershipProviderListResponse;
import com.wallet.membership.dto.MembershipRegisterRequest;
import com.wallet.membership.dto.MembershipRegisterResponse;
import com.wallet.membership.dto.MyMembershipListResponse;
public interface MembershipService {
    MyMembershipListResponse getMyMemberships(Long memberId);
    MembershipProviderListResponse getMembershipProviders(Long memberId);

    MembershipRegisterResponse registerMembership(
            Long memberId,
            MembershipRegisterRequest request
    );

    MembershipCancelResponse cancelMembership(
            Long memberId,
            Long membershipRegisterId
    );

    MembershipDetailResponse getMembershipDetail(
            Long memberId,
            Long membershipRegisterId
    );
}
