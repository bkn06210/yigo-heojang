package com.wallet.notification.batch.model;

/**
 * 실적 부족 알림 후보 (규칙 문서 2장).
 * <p>
 * 아직 notification 테이블에 저장할 형태(Notification)가 아니다. "이 보유카드가
 * 오늘 실적 부족 알림 대상이다"라는 계산 결과만 담고, 알림 문구 작성이나
 * 개별/다이제스트 결정은 이후 조립 단계에서 이 값을 가지고 처리한다.
 *
 * @param achievementRate  화면 표시·정렬용 달성률(%, 0~100+). 트리거 판정 자체는
 *                         계산기 내부에서 정수 교차 곱셈으로 하므로, 이 값의 반올림 오차가
 *                         판정 결과에 영향을 주지 않는다.
 * @param deduplicationKey PERF_SHORTAGE:{userCardId}:MONTH:{yearMonth}:D{7|3} (규칙 문서 2.6절)
 */
public record PerformanceShortageCandidate(
    Long memberId,
    Long userCardId,
    Long cardId,
    String cardName,
    long targetPerformance,
    long currentPerformance,
    long remainingPerformance,
    double achievementRate,
    PerformanceShortageTrigger trigger,
    String yearMonth,
    String deduplicationKey
) {
}