package com.wallet.member.domain;

import java.time.LocalDateTime;

public class Member {
    private Long memberId;
    private String email;
    private String password;  // 해시된 비밀번호
    private String name;
    private String memberStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;

    public Long getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

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