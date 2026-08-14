package com.wallet.notification.batch.model;

/** findExistingMemberDedupKeys 조회에 넘길 (회원, dedup key) 쌍. */
public record MemberDedupKeyLookup(Long memberId, String deduplicationKey) {
}