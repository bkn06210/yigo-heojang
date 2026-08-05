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
import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
    @GetMapping("/api/memberships")
    public Map<String, Object> getMyMemberships(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MyMembershipListResponse data =
                membershipService.getMyMemberships(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "??硫ㅻ쾭??紐⑸줉 議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }
    @GetMapping("/api/memberships/providers")
    public Map<String, Object> getMembershipProviders(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MembershipProviderListResponse data =
                membershipService.getMembershipProviders(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "硫ㅻ쾭???깅줉 媛??紐⑸줉 議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }

    @PostMapping("/api/memberships")
    public Map<String, Object> registerMembership(
            HttpServletRequest servletRequest,
            @RequestBody MembershipRegisterRequest request
    ) {
        Long memberId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);

        MembershipRegisterResponse data =
                membershipService.registerMembership(memberId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "硫ㅻ쾭???깅줉???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }

    @DeleteMapping("/api/memberships/{membershipRegisterId}")
    public Map<String, Object> cancelMembership(
            HttpServletRequest request,
            @PathVariable("membershipRegisterId") Long membershipRegisterId
    ) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MembershipCancelResponse data =
                membershipService.cancelMembership(memberId, membershipRegisterId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "硫ㅻ쾭???깅줉 ?댁젣???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/memberships/{membershipRegisterId}")
    public Map<String, Object> getMembershipDetail(
            HttpServletRequest request,
            @PathVariable("membershipRegisterId") Long membershipRegisterId
    ) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MembershipDetailResponse data =
                membershipService.getMembershipDetail(memberId, membershipRegisterId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "硫ㅻ쾭???곸꽭 議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }

}
