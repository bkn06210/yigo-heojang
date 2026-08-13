package com.wallet.member.domain;

import java.time.LocalDateTime;

public class Member {
    private Long memberId;
    private String email;
    private String password;  // 해시된 비밀번호
    // 간편비밀번호 원문이 아니라 BCrypt 해시를 담는다. 미설정 회원은 null이다.
    private String simplePasswordHash;
    private String name;
    private String nickname;
    private String memberStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;

    // 회원가입 전용 정적 팩토리 메서드
    public static Member createSignupMember(
        String email,
        String encodedPassword,
        String name,
        String nickname
    ) {
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.name = name;
        member.nickname = nickname;
        member.memberStatus = "ACTIVE";
        return member;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getSimplePasswordHash() {
        return simplePasswordHash;
    }

    public String getName() {
        return name;
    }

    public String getNickname() { return nickname; }

    public String getMemberStatus() {
        return memberStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }
}
