package com.wallet.membership.mapper;

import com.wallet.membership.dto.MembershipCancelResponse;
import com.wallet.membership.dto.MembershipDetailResponse;
import com.wallet.membership.dto.MembershipProviderCheckResponse;
import com.wallet.membership.dto.MembershipProviderResponse;
import com.wallet.membership.dto.MembershipRegisterCommand;
import com.wallet.membership.dto.MembershipRegisterResponse;
import com.wallet.membership.dto.MembershipUsagePlaceResponse;
import org.apache.ibatis.annotations.Param;
import com.wallet.membership.dto.MyMembershipResponse;
import java.util.List;

public interface MembershipMapper {

    List<MembershipProviderResponse> selectMembershipProviders(
            @Param("memberId") Long memberId
    );
    List<MyMembershipResponse> selectMyMemberships(Long memberId);
    MembershipProviderCheckResponse selectProviderForRegister(
            @Param("pointProviderId") Long pointProviderId
    );

    MembershipRegisterResponse selectRegisteredMembership(
            @Param("memberId") Long memberId,
            @Param("pointProviderId") Long pointProviderId
    );

    MembershipRegisterResponse selectCanceledMembership(
            @Param("memberId") Long memberId,
            @Param("pointProviderId") Long pointProviderId
    );

    int insertMembershipRegister(MembershipRegisterCommand command);

    int updateCanceledMembershipToRegistered(MembershipRegisterCommand command);

    int insertPointWalletIfNotExists(MembershipRegisterCommand command);

    MembershipRegisterResponse selectMembershipRegisterResponse(
            @Param("memberId") Long memberId,
            @Param("membershipRegisterId") Long membershipRegisterId
    );

    MembershipRegisterResponse selectMembershipStatusById(
            @Param("memberId") Long memberId,
            @Param("membershipRegisterId") Long membershipRegisterId
    );

    int cancelMembership(
            @Param("memberId") Long memberId,
            @Param("membershipRegisterId") Long membershipRegisterId
    );

    MembershipCancelResponse selectMembershipCancelResponse(
            @Param("memberId") Long memberId,
            @Param("membershipRegisterId") Long membershipRegisterId
    );

    MembershipDetailResponse selectMembershipDetail(
            @Param("memberId") Long memberId,
            @Param("membershipRegisterId") Long membershipRegisterId
    );

    List<MembershipUsagePlaceResponse> selectMembershipUsagePlaces(
            @Param("pointProviderId") Long pointProviderId
    );
}