package com.wallet.engine.dto;

/**
 * 결제 가맹점에서 적립되는 등록 멤버십 안내.
 *
 * <b>아직 산출하지 않는다.</b> 응답에는 항상 빈 배열로 나간다.
 * 산출하려면 "이 가맹점에서 적립되는 멤버십"을 판정해야 하는데,
 * point_usage_place에 merchant_id가 없어 현재 스키마로는 판정할 수 없다.
 *
 * @param pointProviderName 멤버십명 (예: CJ ONE)
 * @param message           적립 안내 문구
 */
public record MembershipEarn(
        String pointProviderName,
        String message
) {
}
