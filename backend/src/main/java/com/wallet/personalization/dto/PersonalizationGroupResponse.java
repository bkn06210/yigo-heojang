package com.wallet.personalization.dto;

import java.util.List;

public record PersonalizationGroupResponse(
    Long categoryId,
    String categoryCode,
    String categoryName,
    List<PersonalizationSubcategoryResponse> children
) {
}
