package com.wallet.notification.dto;

import com.wallet.notification.domain.NotificationSetting;

public record NotificationSettingResponse(
    boolean performanceShortageEnabled,
    boolean benefitLimitEnabled
) {
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
            setting.isPerformanceShortageEnabled(),
            setting.isBenefitLimitEnabled()
        );
    }
}
