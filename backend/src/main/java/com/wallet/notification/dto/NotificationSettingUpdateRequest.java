package com.wallet.notification.dto;

import javax.validation.constraints.NotNull;

/**
 * 알림 설정 변경 요청이다.
 * <p>
 * 이 API는 개별 필드에 대한 부분 갱신이 아니라,
 * 클라이언트가 원하는 알림 설정의 최종 상태를 전달받아 저장한다.
 * 따라서 두 설정값을 모두 필수로 받는다.
 * <p>
 * 필드 타입으로 Boolean을 사용하는 이유는 요청에서 필드가 누락된 경우 null로 받아
 * `@NotNull` 검증을 통해 400 Bad Request로 처리하기 위해서다.
 * 실제 서비스 계층에는 검증이 완료된 true 또는 false만 전달된다.
 */
public record NotificationSettingUpdateRequest(
    @NotNull(message = "실적 부족 알림 설정값은 필수입니다.")
    Boolean performanceShortageEnabled,

    @NotNull(message = "혜택 한도 알림 설정값은 필수입니다.")
    Boolean benefitLimitEnabled
) {
}