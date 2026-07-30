package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

/**
 * 전월실적 계산 결과 — 실적인정액과 해석하지 못해 무시한 제외 규칙 목록.
 *
 * amount를 최상위에 둬 정상 경로는 result.amount() 한 번으로 끝난다. ignored는 거의 늘 빈
 * 목록이고, 채워질 때만 상위(서비스·챗봇)가 소비한다.
 *
 * @param amount  실적인정액(원). 제외 규칙에 걸리지 않은 거래의 승인액 합계
 * @param ignored 해석하지 못해 무시한 제외 규칙 목록. 없으면 빈 목록
 */
public record PerformanceAmountResult(long amount, List<IgnoredExclusion> ignored) {

    public PerformanceAmountResult {
        ignored = ignored == null ? List.of() : Collections.unmodifiableList(List.copyOf(ignored));
    }
}
