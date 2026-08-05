package com.wallet.membership.service;

import com.wallet.membership.dto.MembershipCancelResponse;
import com.wallet.membership.dto.MembershipDetailResponse;
import com.wallet.membership.dto.MembershipProviderCheckResponse;
import com.wallet.membership.dto.MembershipProviderListResponse;
import com.wallet.membership.dto.MembershipProviderResponse;
import com.wallet.membership.dto.MembershipRegisterCommand;
import com.wallet.membership.dto.MembershipRegisterRequest;
import com.wallet.membership.dto.MembershipRegisterResponse;
import com.wallet.membership.dto.MembershipUsagePlaceResponse;
import com.wallet.membership.mapper.MembershipMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.wallet.membership.dto.MyMembershipListResponse;
import com.wallet.membership.dto.MyMembershipResponse;
import java.util.List;

@Service
public class MembershipServiceImpl implements MembershipService {

    private final MembershipMapper membershipMapper;
    @Override
    public MyMembershipListResponse getMyMemberships(Long memberId) {
        List<MyMembershipResponse> memberships =
                membershipMapper.selectMyMemberships(memberId);

        return new MyMembershipListResponse(memberships);
    }
    public MembershipServiceImpl(MembershipMapper membershipMapper) {
        this.membershipMapper = membershipMapper;
    }

    @Override
    public MembershipProviderListResponse getMembershipProviders(Long memberId) {
        List<MembershipProviderResponse> providers =
                membershipMapper.selectMembershipProviders(memberId);

        return new MembershipProviderListResponse(providers);
    }

    @Override
    @Transactional
    public MembershipRegisterResponse registerMembership(
            Long memberId,
            MembershipRegisterRequest request
    ) {
        if (request == null || request.getPointProviderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pointProviderId媛 ?꾩슂?⑸땲??");
        }

        Long pointProviderId = request.getPointProviderId();

        MembershipProviderCheckResponse provider =
                membershipMapper.selectProviderForRegister(pointProviderId);

        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "議댁옱?섏? ?딅뒗 ?ъ씤?몄궗?낅땲??");
        }

        if (!"MEMBERSHIP".equals(provider.getProviderType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "硫ㅻ쾭???ъ씤?몄궗媛 ?꾨떃?덈떎.");
        }

        if (!"Y".equals(provider.getUseYn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "?ъ슜 媛?ν븳 ?ъ씤?몄궗媛 ?꾨떃?덈떎.");
        }

        MembershipRegisterResponse alreadyRegistered =
                membershipMapper.selectRegisteredMembership(memberId, pointProviderId);

        if (alreadyRegistered != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? ?깅줉??硫ㅻ쾭??엯?덈떎.");
        }

        MembershipRegisterCommand command = new MembershipRegisterCommand();
        command.setMemberId(memberId);
        command.setPointProviderId(pointProviderId);

        MembershipRegisterResponse canceled =
                membershipMapper.selectCanceledMembership(memberId, pointProviderId);

        Long membershipRegisterId;

        if (canceled != null) {
            command.setMembershipRegisterId(canceled.getMembershipRegisterId());
            membershipMapper.updateCanceledMembershipToRegistered(command);
            membershipRegisterId = canceled.getMembershipRegisterId();
        } else {
            membershipMapper.insertMembershipRegister(command);
            membershipRegisterId = command.getMembershipRegisterId();
        }

        membershipMapper.insertPointWalletIfNotExists(command);

        return membershipMapper.selectMembershipRegisterResponse(memberId, membershipRegisterId);
    }

    @Override
    @Transactional
    public MembershipCancelResponse cancelMembership(
            Long memberId,
            Long membershipRegisterId
    ) {
        MembershipRegisterResponse membership =
                membershipMapper.selectMembershipStatusById(memberId, membershipRegisterId);

        if (membership == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "硫ㅻ쾭???깅줉 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        if ("CANCELED".equals(membership.getRegisterStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "?대? ?댁젣??硫ㅻ쾭??엯?덈떎.");
        }

        membershipMapper.cancelMembership(memberId, membershipRegisterId);

        return membershipMapper.selectMembershipCancelResponse(memberId, membershipRegisterId);
    }

    @Override
    public MembershipDetailResponse getMembershipDetail(
            Long memberId,
            Long membershipRegisterId
    ) {
        MembershipDetailResponse detail =
                membershipMapper.selectMembershipDetail(memberId, membershipRegisterId);

        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "硫ㅻ쾭???곸꽭 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        List<MembershipUsagePlaceResponse> usagePlaces =
                membershipMapper.selectMembershipUsagePlaces(detail.getPointProviderId());

        detail.setUsagePlaces(usagePlaces);

        return detail;
    }
}
