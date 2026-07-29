package com.wallet.member.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.common.ApiResponse;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.service.MemberService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyInfo(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MemberMeResponse response = memberService.getMyInfo(memberId);

        return ResponseEntity.ok(
            ApiResponse.success("회원정보 조회에 성공했습니다.", response)
        );
    }
}