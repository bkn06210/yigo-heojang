package com.wallet.card.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.card.dto.UserCardListResponse;
import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.card.service.UserCardService;
import com.wallet.common.ApiResponse;
import com.wallet.common.constant.RequestAttributeNames;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user-cards")
public class UserCardController {

    private final UserCardService userCardService;

    /**
     * 로그인 회원의 보유 카드를 등록한다.
     * <p>
     * memberId는 클라이언트가 body로 보내는 값이 아니라,
     * JwtAuthenticationFilter가 Access Token 검증 후 request attribute에 넣어준 값을 사용한다.
     * 이렇게 해서 다른 회원 ID를 조작해서 카드를 등록하는 문제를 막는다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserCardRegisterResponse>> registerUserCard(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @Valid @RequestBody UserCardRegisterRequest request
    ) {
        UserCardRegisterResponse response =
            userCardService.registerUserCard(memberId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("보유 카드 등록에 성공했습니다.", response));
    }

    /**
     * 로그인 회원이 등록한 활성 보유 카드 목록을 조회한다.
     * <p>
     * 회원 ID를 요청 파라미터로 받으면 클라이언트가 다른 회원의 ID를
     * 전달할 수 있으므로, JWT 인증 필터가 검증 후 저장한 회원 ID를 사용한다.
     * <p>
     * 등록된 카드가 없더라도 오류가 아니라 정상적인 조회 결과이므로
     * 빈 목록과 HTTP 200 OK를 반환한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserCardListResponse>> getUserCards(
        HttpServletRequest request
    ) {
        Long memberId = (Long) request.getAttribute(
            RequestAttributeNames.AUTHENTICATED_MEMBER_ID
        );

        UserCardListResponse response =
            userCardService.getUserCards(memberId);

        return ResponseEntity.ok(
            ApiResponse.success("보유 카드 목록 조회에 성공했습니다.", response));
    }

    /**
     * 로그인 회원이 소유한 보유 카드를 삭제 상태로 변경한다.
     * <p>
     * userCardId는 삭제할 보유 카드 행을 식별하기 위해 URL 경로에서 받고,
     * memberId는 JWT 인증 필터가 검증한 값을 사용한다.
     * 따라서 클라이언트가 다른 회원의 ID를 직접 전달해서 삭제할 수 없다.
     * <p>
     * 삭제에 성공하면 응답 본문 없이 HTTP 204 No Content를 반환한다.
     */
    @DeleteMapping("/{userCardId}")
    public ResponseEntity<Void> deleteUserCard(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @PathVariable Long userCardId
    ) {
        userCardService.deleteUserCard(memberId, userCardId);
        return ResponseEntity.noContent().build();
    }
}
