package com.wallet.membership.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.wallet.common.ApiResponse;
import com.wallet.membership.dto.MembershipCancelResponse;
import com.wallet.membership.dto.MembershipDetailResponse;
import com.wallet.membership.dto.MembershipProviderListResponse;
import com.wallet.membership.dto.MembershipRegisterRequest;
import com.wallet.membership.dto.MembershipRegisterResponse;
import com.wallet.membership.dto.MyMembershipListResponse;
import com.wallet.membership.service.MembershipService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public ApiResponse<MyMembershipListResponse> getMyMemberships(HttpServletRequest request) {
        Long memberId = authenticatedMemberId(request);
        MyMembershipListResponse data = membershipService.getMyMemberships(memberId);
        return ApiResponse.success("내 멤버십 목록 조회에 성공했습니다.", data);
    }

    @GetMapping("/providers")
    public ApiResponse<MembershipProviderListResponse> getMembershipProviders(HttpServletRequest request) {
        Long memberId = authenticatedMemberId(request);
        MembershipProviderListResponse data = membershipService.getMembershipProviders(memberId);
        return ApiResponse.success("멤버십 등록 가능 목록 조회에 성공했습니다.", data);
    }

    @PostMapping
    public ApiResponse<MembershipRegisterResponse> registerMembership(
            HttpServletRequest request,
            @Valid @RequestBody MembershipRegisterRequest registerRequest
    ) {
        Long memberId = authenticatedMemberId(request);
        MembershipRegisterResponse data = membershipService.registerMembership(memberId, registerRequest);
        return ApiResponse.success("멤버십 등록에 성공했습니다.", data);
    }

    @DeleteMapping("/{membershipRegisterId}")
    public ApiResponse<MembershipCancelResponse> cancelMembership(
            HttpServletRequest request,
            @PathVariable Long membershipRegisterId
    ) {
        Long memberId = authenticatedMemberId(request);
        MembershipCancelResponse data = membershipService.cancelMembership(memberId, membershipRegisterId);
        return ApiResponse.success("멤버십 등록 해제에 성공했습니다.", data);
    }

    @GetMapping("/{membershipRegisterId}")
    public ApiResponse<MembershipDetailResponse> getMembershipDetail(
            HttpServletRequest request,
            @PathVariable Long membershipRegisterId
    ) {
        Long memberId = authenticatedMemberId(request);
        MembershipDetailResponse data = membershipService.getMembershipDetail(memberId, membershipRegisterId);
        return ApiResponse.success("멤버십 상세 조회에 성공했습니다.", data);
    }

    private Long authenticatedMemberId(HttpServletRequest request) {
        return (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);
    }
}
