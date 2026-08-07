package com.wallet.personalization.dto;

public record PersonalizationBrandRequest(
    Long categoryId,
    String brandName
) {
}
