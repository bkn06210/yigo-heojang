package com.wallet.personalization.dto;

import java.util.List;

public record PersonalizationSubcategoryResponse(
    Long categoryId,
    String categoryCode,
    String categoryName,
    boolean selected,
    List<String> brands,
    List<PersonalizationMerchantResponse> merchants
) {
}
