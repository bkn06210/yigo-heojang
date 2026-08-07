package com.wallet.personalization.dto;

import java.util.List;

public record PersonalizationUpdateRequest(
    List<Long> categoryIds,
    List<PersonalizationBrandRequest> brands
) {
}
