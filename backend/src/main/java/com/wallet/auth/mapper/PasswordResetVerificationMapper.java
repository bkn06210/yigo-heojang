package com.wallet.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.auth.domain.PasswordResetVerification;

@Mapper
public interface PasswordResetVerificationMapper {
    // 회원 기준으로 가장 최근 비밀번호 재설정 인증 정보를 조회하면서 행 잠금
    PasswordResetVerification findLatestByMemberIdForUpdate(
        @Param("memberId") Long memberId
    );

    // 비밀번호 재설정 토큰 해시로 인증 정보를 조회하면서 행 잠금
    PasswordResetVerification findByResetTokenHashForUpdate(
        @Param("resetTokenHash") String resetTokenHash
    );

    // 같은 회원이 새 인증 코드를 요청하면 기존 미완료 인증 정보를 만료 처리
    int expireActiveByMemberId(
        @Param("memberId") Long memberId
    );

    // 비밀번호 재설정 인증 정보 최초 저장
    int insert(PasswordResetVerification verification);

    // 인증 번호가 틀렸을 때, 실패 횟수 1 증가
    int increaseFailedAttemptCount(
        @Param("passwordResetVerificationId") Long passwordResetVerificationId
    );

    // 인증 코드가 만료된 경우 상태를 EXPIRED로 변경
    int expireVerificationCode(
        @Param("passwordResetVerificationId") Long passwordResetVerificationId
    );

     // 인증 코드 검증 성공 시 상태를 VERIFIED로 변경하고, 새 비밀번호 설정에 사용할 reset token 해시를 저장
    int verify(
        @Param("passwordResetVerificationId") Long passwordResetVerificationId,
        @Param("resetTokenHash") String resetTokenHash,
        @Param("resetTokenExpiresAt") LocalDateTime resetTokenExpiresAt
    );


    // 새 비밀번호 설정 성공 후 인증 정보를 사용 완료 처리
    int markAsUsed(
        @Param("passwordResetVerificationId") Long passwordResetVerificationId
    );
}