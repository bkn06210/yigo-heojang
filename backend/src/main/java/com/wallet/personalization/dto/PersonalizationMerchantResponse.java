package com.wallet.personalization.dto;

public record PersonalizationMerchantResponse(
    Long merchantId,
    String merchantName,
    boolean selected,
    Integer priority
) {
}
