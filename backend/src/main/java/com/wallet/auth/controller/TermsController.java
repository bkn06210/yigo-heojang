package com.wallet.auth.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.auth.domain.TermScope;
import com.wallet.auth.dto.TermsResponse;
import com.wallet.auth.service.TermsService;
import com.wallet.common.ApiResponse;

@RequiredArgsConstructor
@RestController
public class TermsController {
    private final TermsService termsService;

    @GetMapping("/api/terms")
    public ResponseEntity<ApiResponse<TermsResponse>> getTerms() {
        // 해당 엔드포인트는 원래부터 회원가입 화면 전용이었으므로 SIGNUP으로 고정한다.
        // 탈퇴 화면용 약관 조회는 별도 엔드포인트(GET /api/members/me/withdrawal-terms)로 추가한다.
        TermsResponse response = termsService.getTerms(TermScope.SIGNUP);

        return ResponseEntity.ok(
            ApiResponse.success("약관 목록 조회에 성공했습니다.", response)
        );
    }
}