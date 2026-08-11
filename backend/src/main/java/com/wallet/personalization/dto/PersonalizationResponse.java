package com.wallet.personalization.dto;

import java.util.List;

public record PersonalizationResponse(
    List<PersonalizationGroupResponse> groups
) {
}
