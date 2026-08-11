package com.wallet.engine.dto;

/**
 * 결제 직전 금융포인트 잔액 안내.
 *
 * <b>아직 산출하지 않는다.</b> 응답에는 항상 null로 나간다 — 포인트 기능이 후순위다.
 *
 * 멤버십(CJ ONE 등)이 아니라 <b>금융포인트</b> 기준이다. 멤버십은 잔액이 연동되지 않아
 * 잔액 대신 적립 가능 안내({@link MembershipEarn})만 내려간다.
 *
 * @param pointProviderName 금융포인트사명 (예: 마이신한포인트)
 * @param usablePoint       보유 잔액(이 결제에 사용 가능)
 * @param message           안내 문구
 */
public record PointGuide(
        String pointProviderName,
        long usablePoint,
        String message
) {
}
