package com.wallet.notification.dto;

import javax.validation.constraints.NotNull;

/**
 * 두 필드 모두 필수다. 부분 수정(하나만 보내고 나머지는 유지)을 지원하지 않는 이유는,
 * "값을 안 보냈다"와 "false로 끄고 싶다"를 구분할 수 없어서다 — 필드가 boolean이라
 * 안 보낸 것과 false를 보낸 것을 JSON만으로는 구분하기 애매하다. 그래서 대표 카드
 * API와 같은 방식으로, 클라이언트가 항상 "원하는 최종 상태 둘 다"를 명시하게 한다.
 */
public record NotificationSettingUpdateRequest(
    @NotNull(message = "실적 부족 알림 설정값은 필수입니다.")
    Boolean performanceShortageEnabled,

    @NotNull(message = "혜택 한도 알림 설정값은 필수입니다.")
    Boolean benefitLimitEnabled
) {
}
