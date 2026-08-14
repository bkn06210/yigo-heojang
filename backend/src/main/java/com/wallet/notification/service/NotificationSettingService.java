package com.wallet.notification.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.notification.domain.NotificationSetting;
import com.wallet.notification.dto.NotificationSettingResponse;
import com.wallet.notification.dto.NotificationSettingUpdateRequest;
import com.wallet.notification.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {
    private final NotificationRepository notificationRepository;

    /** 설정 행이 없는 회원은 findEffectiveSetting이 이미 기본값(둘 다 true)으로 채워서 돌려준다. */
    @Transactional(readOnly = true)
    public NotificationSettingResponse getSetting(Long memberId) {
        NotificationSetting setting = notificationRepository.findEffectiveSetting(memberId);
        return NotificationSettingResponse.from(setting);
    }

    /**
     * `@Valid`가 이미 두 필드의 null 여부를 걸러준 뒤 이 메서드가 호출되므로,
     * 여기서는 "행이 있었는지"를 신경 쓸 필요 없이 그대로 upsert만 하면 된다.
     */
    @Transactional
    public NotificationSettingResponse updateSetting(Long memberId, NotificationSettingUpdateRequest request) {
        NotificationSetting setting = NotificationSetting.builder()
            .memberId(memberId)
            .performanceShortageEnabled(request.performanceShortageEnabled())
            .benefitLimitEnabled(request.benefitLimitEnabled())
            .build();

        notificationRepository.saveSetting(setting);
        return NotificationSettingResponse.from(setting);
    }
}
