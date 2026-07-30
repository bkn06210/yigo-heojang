package com.wallet.auth.dto;

import java.time.LocalDateTime;

import com.wallet.member.domain.Member;

public record SignupResponse(
    Long memberId,
    String email,
    String name,
    String memberStatus,
    LocalDateTime createdAt
) {
    public static SignupResponse from(Member member) {
        return new SignupResponse(
            member.getMemberId(),
            member.getEmail(),
            member.getName(),
            member.getMemberStatus(),
            member.getCreatedAt()
        );
    }
}