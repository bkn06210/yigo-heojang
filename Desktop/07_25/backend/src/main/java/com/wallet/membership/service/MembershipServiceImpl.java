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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pointProviderId가 필요합니다.");
        }

        Long pointProviderId = request.getPointProviderId();

        MembershipProviderCheckResponse provider =
                membershipMapper.selectProviderForRegister(pointProviderId);

        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 포인트사입니다.");
        }

        if (!"MEMBERSHIP".equals(provider.getProviderType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "멤버십 포인트사가 아닙니다.");
        }

        if (!"Y".equals(provider.getUseYn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용 가능한 포인트사가 아닙니다.");
        }

        MembershipRegisterResponse alreadyRegistered =
                membershipMapper.selectRegisteredMembership(memberId, pointProviderId);

        if (alreadyRegistered != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 멤버십입니다.");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "멤버십 등록 정보를 찾을 수 없습니다.");
        }

        if ("CANCELED".equals(membership.getRegisterStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해제된 멤버십입니다.");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "멤버십 상세 정보를 찾을 수 없습니다.");
        }

        List<MembershipUsagePlaceResponse> usagePlaces =
                membershipMapper.selectMembershipUsagePlaces(detail.getPointProviderId());

        detail.setUsagePlaces(usagePlaces);

        return detail;
    }
}