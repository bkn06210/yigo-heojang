package com.wallet.auth.dto;

public record LoginMemberResponse(
    Long memberId,
    String email,
    String name,
    String nickname
){}
