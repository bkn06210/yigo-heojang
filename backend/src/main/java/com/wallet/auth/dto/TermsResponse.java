package com.wallet.auth.dto;

import java.util.List;

public record TermsResponse(
    List<TermResponse> terms
) {
}