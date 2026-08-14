package com.wallet.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.domain.NotificationSetting;
import com.wallet.notification.dto.NotificationSettingResponse;
import com.wallet.notification.dto.NotificationSettingUpdateRequest;
import com.wallet.notification.repository.NotificationRepository;

class NotificationSettingServiceTest {
    @Test
    @DisplayName("설정 행이 없는 회원은 기본값(둘 다 true)을 조회한다")
    void getSetting_returnsDefaultWhenNoRow() {
        // given
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationSettingService service = new NotificationSettingService(repository);

        when(repository.findEffectiveSetting(1L)).thenReturn(NotificationSetting.defaultEnabled(1L));

        // when
        NotificationSettingResponse response = service.getSetting(1L);

        // then
        assertThat(response.performanceShortageEnabled()).isTrue();
        assertThat(response.benefitLimitEnabled()).isTrue();
    }

    @Test
    @DisplayName("설정 행이 있는 회원은 저장된 값을 그대로 조회한다")
    void getSetting_returnsCustomValues() {
        // given
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationSettingService service = new NotificationSettingService(repository);

        NotificationSetting customSetting = NotificationSetting.builder()
            .memberId(1L)
            .performanceShortageEnabled(false)
            .benefitLimitEnabled(true)
            .build();
        when(repository.findEffectiveSetting(1L)).thenReturn(customSetting);

        // when
        NotificationSettingResponse response = service.getSetting(1L);

        // then
        assertThat(response.performanceShortageEnabled()).isFalse();
        assertThat(response.benefitLimitEnabled()).isTrue();
    }

    @Test
    @DisplayName("설정 변경 시 요청받은 두 값 그대로 저장하고 응답에 반영한다")
    void updateSetting_success_savesRequestedValuesAsIs() {
        // given
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationSettingService service = new NotificationSettingService(repository);

        NotificationSettingUpdateRequest request = new NotificationSettingUpdateRequest(false, true);

        // when
        NotificationSettingResponse response = service.updateSetting(1L, request);

        // then
        assertThat(response.performanceShortageEnabled()).isFalse();
        assertThat(response.benefitLimitEnabled()).isTrue();

        verify(repository).saveSetting(argThat(setting ->
            setting.getMemberId().equals(1L)
                && !setting.isPerformanceShortageEnabled()
                && setting.isBenefitLimitEnabled()
        ));
    }

    private static NotificationSetting argThat(java.util.function.Predicate<NotificationSetting> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
