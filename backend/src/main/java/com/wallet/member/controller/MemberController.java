package com.wallet.member.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.common.ApiResponse;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.dto.MemberUpdateRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationResponse;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyResponse;
import com.wallet.member.service.MemberService;
import com.wallet.member.service.SimplePasswordVerificationService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final SimplePasswordVerificationService simplePasswordVerificationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyInfo(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MemberMeResponse response = memberService.getMyInfo(memberId);

        return ResponseEntity.ok(
            ApiResponse.success("회원정보 조회에 성공했습니다.", response)
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyInfo(
        HttpServletRequest request,
        @Valid @RequestBody MemberUpdateRequest updateRequest
    ) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        MemberMeResponse response = memberService.updateMyInfo(memberId, updateRequest);

        return ResponseEntity.ok(
            ApiResponse.success("회원정보 수정에 성공했습니다.", response)
        );
    }

    @PostMapping("/me/simple-password/email-verifications")
    public ResponseEntity<ApiResponse<SimplePasswordEmailVerificationResponse>>
        sendSimplePasswordVerificationCode(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        SimplePasswordEmailVerificationResponse response =
            simplePasswordVerificationService.sendVerificationCode(memberId);

        return ResponseEntity.accepted().body(
            ApiResponse.success("간편비밀번호 변경 인증 코드가 발송되었습니다.", response)
        );
    }

    @PostMapping("/me/simple-password/email-verifications/verify")
    public ResponseEntity<ApiResponse<SimplePasswordEmailVerificationVerifyResponse>>
        verifySimplePasswordVerificationCode(
            HttpServletRequest request,
            @Valid @RequestBody SimplePasswordEmailVerificationVerifyRequest verifyRequest
        ) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        SimplePasswordEmailVerificationVerifyResponse response =
            simplePasswordVerificationService.verifyCode(memberId, verifyRequest);

        return ResponseEntity.ok(
            ApiResponse.success("간편비밀번호 변경 이메일 인증이 완료되었습니다.", response)
        );
    }

}
