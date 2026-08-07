package com.wallet.personalization.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.common.ApiResponse;
import com.wallet.personalization.dto.PersonalizationResponse;
import com.wallet.personalization.dto.PersonalizationUpdateRequest;
import com.wallet.personalization.service.PersonalizationService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/personalization")
public class PersonalizationController {
    private final PersonalizationService personalizationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PersonalizationResponse>> get(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);
        return ResponseEntity.ok(ApiResponse.success(
            "개인화 설정을 조회했습니다.", personalizationService.get(memberId)
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PersonalizationResponse>> update(
        HttpServletRequest request,
        @RequestBody PersonalizationUpdateRequest updateRequest
    ) {
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);
        return ResponseEntity.ok(ApiResponse.success(
            "개인화 설정을 저장했습니다.",
            personalizationService.update(memberId, updateRequest)
        ));
    }
}
