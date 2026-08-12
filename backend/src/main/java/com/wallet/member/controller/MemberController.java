package com.wallet.member.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.auth.domain.TermScope;
import com.wallet.auth.dto.TermsResponse;
import com.wallet.auth.service.TermsService;
import com.wallet.common.ApiResponse;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.dto.MemberUpdateRequest;
import com.wallet.member.service.MemberService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final TermsService termsService;

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

    // 탈퇴 화면에 진입했을 때 프론트가, 고지 문구와 term_version_id를 받아가는 API.
    // 이 term_version_id를 사용자가 "동의" 체크 후 DELETE /api/members/me 요청의
    // termVersionId로 그대로 담아 보내면, 그 값이 지금 실제로 유효한
    // WITHDRAWAL 스코프 약관 버전이 맞는지 서버가 한 번 더 검증한다(프론트가
    // 보낸 값을 그대로 믿지 않는다).
    //
    // 회원 정보를 조회하는 게 아니라 "지금 유효한 약관이 무엇인지"만 필요하므로
    // HttpServletRequest에서 memberId를 꺼내 쓰지 않는다. 다만 이 API 자체는
    // JwtAuthenticationFilter의 인증 대상 경로(/api/members/**)에 포함되어 있어,
    // 로그인하지 않은 상태로는 호출할 수 없다.
    @GetMapping("/me/withdrawal-terms")
    public ResponseEntity<ApiResponse<TermsResponse>> getWithdrawalTerms() {
        TermsResponse response = termsService.getTerms(TermScope.WITHDRAWAL);

        return ResponseEntity.ok(
            ApiResponse.success("탈퇴 안내 약관 조회에 성공했습니다.", response)
        );
    }
}