package com.wallet.auth.dto;

public record LoginResult(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    long refreshTokenExpiresIn,
    LoginMemberResponse member
) {}