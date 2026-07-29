package com.wallet.engine.dto;

/**
 * 실적 미달 경고 — "이 카드 실적이 얼마 안 남았으니 이번엔 이걸 쓰라"는 안내.
 *
 * <b>아직 산출하지 않는다.</b> 응답에는 항상 null로 나가며, 판정 로직은 이후 단계에서 붙인다.
 * 필드를 미리 정의해 두는 이유는 명세가 확정돼 있어 프론트가 이 형태를 기대하기 때문이다.
 *
 * @param userCardId           대상 보유카드 id
 * @param cardName             카드명
 * @param remainingPerformance 실적까지 남은 금액(원)
 * @param message              경고 문구
 */
public record FutureOptimization(
        long userCardId,
        String cardName,
        long remainingPerformance,
        String message
) {
}
