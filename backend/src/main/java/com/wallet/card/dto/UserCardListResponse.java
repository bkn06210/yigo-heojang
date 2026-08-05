package com.wallet.card.dto;

import java.util.List;

/**
 * 보유 카드 목록 조회 성공 응답 DTO.
 * <p>
 * 카드 목록과 목록에 포함된 카드 개수를 함께 반환한다.
 * 회원에게 등록된 카드가 없으면 예외를 발생시키지 않고,
 * 빈 List와 0을 반환한다.
 */
public record UserCardListResponse(
    List<UserCardListItemResponse> userCards,
    int totalCount
) {
    /**
     * 조회된 카드 목록으로 응답 객체를 생성한다.
     * <p>
     * totalCount를 호출하는 쪽에서 따로 계산하게 두면
     * 실제 목록 크기와 totalCount가 서로 달라질 수 있다.
     * 따라서 이 생성 메서드에서 목록 크기를 기준으로 자동 계산한다.
     */
    public static UserCardListResponse from(
        List<UserCardListItemResponse> userCards
    ) {
        return new UserCardListResponse(
            List.copyOf(userCards),
            userCards.size()
        );
    }
}