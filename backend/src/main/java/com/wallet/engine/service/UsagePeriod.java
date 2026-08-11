package com.wallet.engine.service;

import java.time.YearMonth;

/**
 * 소진 합산에 쓸 연월 구간 계산.
 *
 * 분기·연 소진은 저장하지 않고 월 소진 행을 합산해 구하므로, "어느 달부터 어느 달까지"를
 * 서비스가 정해 매퍼에 넘긴다. SQL에 날짜 함수를 두지 않아 조회가 시계에 의존하지 않고,
 * 지난달 현황 조회처럼 기준월이 오늘이 아닌 경우에도 같은 경로가 그대로 쓰인다.
 *
 * 구간의 끝은 항상 기준월이다. 분기 전체를 합산하면 조회한 달 이후에 쌓인 소진까지 섞여,
 * 그 달 시점에는 있지도 않던 값으로 한도가 판정된다.
 */
public final class UsagePeriod {

    private UsagePeriod() {
    }

    /** 기준월이 속한 분기의 첫 달 (1·4·7·10월) */
    public static YearMonth quarterStart(YearMonth baseMonth) {
        int firstMonthOfQuarter = ((baseMonth.getMonthValue() - 1) / 3) * 3 + 1;
        return YearMonth.of(baseMonth.getYear(), firstMonthOfQuarter);
    }

    /** 기준월이 속한 해의 첫 달 */
    public static YearMonth yearStart(YearMonth baseMonth) {
        return YearMonth.of(baseMonth.getYear(), 1);
    }

    /**
     * 직전 분기의 첫 달 — 전분기 실적 합산 구간의 시작.
     *
     * 전월실적이 "지난달"이듯 전분기 실적은 "지난 분기"다. 이번 분기를 합산하면 아직 끝나지
     * 않은 기간으로 판정하게 되어, 분기 초에는 늘 미충족이 나온다.
     */
    public static YearMonth previousQuarterStart(YearMonth baseMonth) {
        return quarterStart(baseMonth).minusMonths(3);
    }

    /** 직전 분기의 끝 달 */
    public static YearMonth previousQuarterEnd(YearMonth baseMonth) {
        return quarterStart(baseMonth).minusMonths(1);
    }
}
