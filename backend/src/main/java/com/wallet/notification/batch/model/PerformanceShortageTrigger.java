package com.wallet.notification.batch.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 실적 부족 알림의 두 트리거 시점 (규칙 문서 2.4절).
 * daysBeforeMonthEnd는 "월 말일 며칠 전인가"를 나타내고, 계산기가
 * 오늘과 말일의 차이를 이 값과 비교해서 오늘이 어느 트리거에 해당하는지 판정한다.
 */
@Getter
@RequiredArgsConstructor
public enum PerformanceShortageTrigger {
    D7(7),
    D3(3);

    private final int daysBeforeMonthEnd;
}