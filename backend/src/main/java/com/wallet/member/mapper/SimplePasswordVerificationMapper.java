package com.wallet.member.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.member.domain.SimplePasswordVerification;

@Mapper
public interface SimplePasswordVerificationMapper {
    // 같은 회원의 인증 요청이 동시에 처리되지 않도록 조회한 인증 행에 쓰기 잠금을 건다.
    SimplePasswordVerification findLatestByMemberIdForUpdate(@Param("memberId") Long memberId);

    SimplePasswordVerification findByChangeTokenHashForUpdate(
        @Param("changeTokenHash") String changeTokenHash
    );

    int expireActiveByMemberId(@Param("memberId") Long memberId);

    int insert(SimplePasswordVerification verification);

    int increaseFailedAttemptCount(
        @Param("simplePasswordVerificationId") Long simplePasswordVerificationId
    );

    int expireVerificationCode(
        @Param("simplePasswordVerificationId") Long simplePasswordVerificationId
    );

    int verify(
        @Param("simplePasswordVerificationId") Long simplePasswordVerificationId,
        @Param("changeTokenHash") String changeTokenHash,
        @Param("changeTokenExpiresAt") LocalDateTime changeTokenExpiresAt
    );

    int markAsUsed(
        @Param("simplePasswordVerificationId") Long simplePasswordVerificationId
    );
}
