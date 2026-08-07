package com.wallet.card.dto;

import javax.validation.constraints.NotNull;

/**
 * 보유 카드의 대표 카드 여부를 변경할 때 사용하는 요청 DTO다.
 * <p>
 * 프론트는 현재 대표 카드 여부를 직접 토글한 뒤,
 * 서버에는 "변경하고 싶은 최종 상태"를 전달한다.
 * <p>
 * e.g.
 * - 대표 카드로 설정하고 싶으면 representative = true
 * - 대표 카드에서 제외하고 싶으면 representative = false
 */
public record UserCardRepresentativeUpdateRequest(
    @NotNull(message = "대표 카드 설정 여부는 필수입니다.")
    Boolean representative
) {
}