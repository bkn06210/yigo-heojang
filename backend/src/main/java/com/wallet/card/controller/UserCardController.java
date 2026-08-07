package com.wallet.card.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.card.dto.UserCardDetailResponse;
import com.wallet.card.dto.UserCardListResponse;
import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.card.dto.UserCardRepresentativeUpdateRequest;
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
     * 로그인 회원이 대표 카드로 설정한 활성 보유 카드 목록을 조회한다.
     * <p>
     * 대표 카드 목록은 기존 보유 카드 목록과 같은 응답 구조를 사용한다.
     * 차이는 전체 보유 카드가 아니라 is_representative = 1인 카드만 반환한다는 점이다.
     * <p>
     * 대표 카드가 하나도 없어도 예외가 아니라 정상 상태이므로,
     * 빈 목록과 totalCount = 0을 반환한다.
     */
    @GetMapping("/representative")
    public ResponseEntity<ApiResponse<UserCardListResponse>> getRepresentativeUserCards(
        @RequestAttribute(RequestAttributeNames.AUTHENTICATED_MEMBER_ID) Long memberId
    ) {
        UserCardListResponse response =
            userCardService.getRepresentativeUserCards(memberId);

        return ResponseEntity.ok(
            ApiResponse.success("대표 카드 목록 조회에 성공했습니다.", response)
        );
    }

    /**
     * 로그인한 회원이 보유한 특정 카드의 상세 기본 정보를 조회한다.
     * <p>
     * memberId는 클라이언트가 직접 보내는 값이 아니라,
     * JWT 인증 필터가 검증 후 request attribute에 저장한 값을 사용한다.
     * 따라서 사용자는 자신의 보유 카드만 조회할 수 있다.
     */
    @GetMapping("/{userCardId}")
    public ResponseEntity<ApiResponse<UserCardDetailResponse>> getUserCardDetail(
        @RequestAttribute(RequestAttributeNames.AUTHENTICATED_MEMBER_ID) Long memberId,
        @PathVariable Long userCardId
    ) {
        UserCardDetailResponse response =
            userCardService.getUserCardDetail(memberId, userCardId);

        return ResponseEntity.ok(
            ApiResponse.success("보유 카드 상세 조회에 성공했습니다.", response));
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
    /**
     * 로그인 회원이 소유한 보유 카드의 대표 카드 여부를 변경한다.
     * <p>
     * 이 API는 서버가 현재 상태를 반대로 뒤집는 토글 API가 아니다.
     * 프론트가 원하는 최종 상태를 representative 값으로 보내면,
     * 서버는 해당 상태가 되도록 처리한다.
     * <p>
     * e.g.
     * - representative = true  : 대표 카드로 설정
     * - representative = false : 대표 카드에서 제외
     * <p>
     * 이미 요청한 상태와 같은 경우에는 Service에서 변경 없이 성공 처리한다.
     */
    @PatchMapping("/{userCardId}/representative")
    public ResponseEntity<Void> updateRepresentative(
        @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
        @PathVariable Long userCardId,
        @Valid @RequestBody UserCardRepresentativeUpdateRequest request
    ) {
        userCardService.updateRepresentative(
            memberId,
            userCardId,
            request.representative()
        );

        return ResponseEntity.noContent().build();
    }
}
