package com.wallet.auth.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.auth.dto.TermsResponse;
import com.wallet.auth.service.TermsService;
import com.wallet.common.ApiResponse;

@RequiredArgsConstructor
@RestController
public class TermsController {
    private final TermsService termsService;

    @GetMapping("/api/terms")
    public ResponseEntity<ApiResponse<TermsResponse>> getTerms() {
        TermsResponse response = termsService.getTerms();

        return ResponseEntity.ok(
            ApiResponse.success("약관 목록 조회에 성공했습니다.", response)
        );
    }
}