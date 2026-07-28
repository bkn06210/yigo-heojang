package com.wallet.member.dto;

import java.time.LocalDateTime;

import com.wallet.member.domain.Member;

public record MemberMeResponse(
    Long memberId,
    String email,
    String name,
    String memberStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(
            member.getMemberId(),
            member.getEmail(),
            member.getName(),
            member.getMemberStatus(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }
}