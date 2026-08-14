package com.wallet.member.domain;

import java.time.LocalDateTime;

import lombok.Getter;

// member_withdrawal_archive 테이블 한 행에 대응하는 도메인 객체
@Getter
public class MemberWithdrawalArchive {
    private Long memberWithdrawalArchiveId;
    private Long memberId;
    private String email;
    private String emailHash;
    private String name;
    private LocalDateTime withdrawnAt;
    private String retentionReason;
    private LocalDateTime createdAt;
    
    public static MemberWithdrawalArchive of(
        Long memberId,
        String email,
        String emailHash,
        String name,
        LocalDateTime withdrawnAt,
        String retentionReason
    ) {
        MemberWithdrawalArchive archive = new MemberWithdrawalArchive();
        archive.memberId = memberId;
        archive.email = email;
        archive.emailHash = emailHash;
        archive.name = name;
        archive.withdrawnAt = withdrawnAt;
        archive.retentionReason = retentionReason;
        return archive;
    }
}