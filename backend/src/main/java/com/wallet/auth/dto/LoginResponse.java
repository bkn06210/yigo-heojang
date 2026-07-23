package com.wallet.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    LoginMemberResponse member
){}