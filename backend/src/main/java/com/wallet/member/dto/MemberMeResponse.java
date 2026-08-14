package com.wallet.member.dto;

import java.time.LocalDateTime;

import com.wallet.member.domain.Member;

public record MemberMeResponse(
    Long memberId,
    String email,
    String name,
    String nickname,
    boolean simplePasswordSet,
    String memberStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(
            member.getMemberId(),
            member.getEmail(),
            member.getName(),
            member.getNickname(),
            // 해시값 자체는 노출하지 않고, 값의 존재 여부만 프론트에 전달한다.
            member.getSimplePasswordHash() != null,
            member.getMemberStatus(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }
}
