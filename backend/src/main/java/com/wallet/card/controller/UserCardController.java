package com.wallet.card.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.card.service.UserCardService;
import com.wallet.common.ApiResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user-cards")
public class UserCardController {

    private final UserCardService userCardService;

    /**
     * 로그인 회원의 보유 카드를 등록한다.
     *
     * memberId는 클라이언트가 body로 보내는 값이 아니라,
     * JwtAuthenticationFilter가 Access Token 검증 후 request attribute에 넣어준 값을 사용한다.
     * 이렇게 해서 다른 회원 ID를 조작해서 카드를 등록하는 문제를 막는다.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<UserCardRegisterResponse> registerUserCard(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @Valid @RequestBody UserCardRegisterRequest request
    ) {
        UserCardRegisterResponse response =
            userCardService.registerUserCard(memberId, request);

        return ApiResponse.success(response);
    }
}