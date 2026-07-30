package com.wallet.membership.controller;

import com.wallet.membership.dto.MembershipCancelResponse;
import com.wallet.membership.dto.MembershipDetailResponse;
import com.wallet.membership.dto.MembershipProviderListResponse;
import com.wallet.membership.dto.MembershipRegisterRequest;
import com.wallet.membership.dto.MembershipRegisterResponse;
import com.wallet.membership.service.MembershipService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.wallet.membership.dto.MyMembershipListResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
    @GetMapping("/api/memberships")
    public Map<String, Object> getMyMemberships() {
        Long memberId = 1L;

        MyMembershipListResponse data =
                membershipService.getMyMemberships(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "내 멤버십 목록 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }
    @GetMapping("/api/memberships/providers")
    public Map<String, Object> getMembershipProviders() {

        Long memberId = 1L;

        MembershipProviderListResponse data =
                membershipService.getMembershipProviders(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "멤버십 등록 가능 목록 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @PostMapping("/api/memberships")
    public Map<String, Object> registerMembership(
            @RequestBody MembershipRegisterRequest request
    ) {
        Long memberId = 1L;

        MembershipRegisterResponse data =
                membershipService.registerMembership(memberId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "멤버십 등록에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @DeleteMapping("/api/memberships/{membershipRegisterId}")
    public Map<String, Object> cancelMembership(
            @PathVariable("membershipRegisterId") Long membershipRegisterId
    ) {
        Long memberId = 1L;

        MembershipCancelResponse data =
                membershipService.cancelMembership(memberId, membershipRegisterId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "멤버십 등록 해제에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/memberships/{membershipRegisterId}")
    public Map<String, Object> getMembershipDetail(
            @PathVariable("membershipRegisterId") Long membershipRegisterId
    ) {
        Long memberId = 1L;

        MembershipDetailResponse data =
                membershipService.getMembershipDetail(memberId, membershipRegisterId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "멤버십 상세 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }
    
}