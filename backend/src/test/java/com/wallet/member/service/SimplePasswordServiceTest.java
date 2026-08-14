package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordUpdateRequest;
import com.wallet.member.dto.SimplePasswordVerifyRequest;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

class SimplePasswordServiceTest {
    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final SimplePasswordVerificationMapper verificationMapper =
        mock(SimplePasswordVerificationMapper.class);
    private final Sha256Hasher sha256Hasher = new Sha256Hasher();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Clock clock = Clock.fixed(
        Instant.parse("2026-08-13T05:00:00Z"),
        ZoneId.of("Asia/Seoul")
    );

    private SimplePasswordService service;

    @BeforeEach
    void setUp() {
        service = new SimplePasswordService(
            memberMapper,
            verificationMapper,
            sha256Hasher,
            passwordEncoder,
            clock
        );
    }

    @Test
    @DisplayName("간편비밀번호 설정 성공 - BCrypt 해시를 저장하고 변경 토큰을 사용 완료 처리한다")
    void updateSimplePassword_success() {
        String tokenHash = sha256Hasher.sha256("change-token");
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member(1L));
        when(verificationMapper.findByChangeTokenHashForUpdate(tokenHash))
            .thenReturn(verification(1L, VerificationStatus.VERIFIED, 10));
        when(memberMapper.updateSimplePassword(eq(1L), any())).thenReturn(1);
        when(verificationMapper.markAsUsed(10L)).thenReturn(1);

        service.updateSimplePassword(1L, request("012345", "012345"));

        verify(memberMapper).updateSimplePassword(
            eq(1L),
            org.mockito.ArgumentMatchers.argThat(
                hash -> passwordEncoder.matches("012345", hash)
            )
        );
        verify(verificationMapper).markAsUsed(10L);
    }

    @Test
    @DisplayName("간편비밀번호 설정 실패 - 확인값이 다르면 DB를 변경하지 않는다")
    void updateSimplePassword_fail_whenConfirmationMismatch() {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.updateSimplePassword(1L, request("012345", "543210"))
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_CONFIRMATION_MISMATCH);
        verify(memberMapper, never()).updateSimplePassword(any(), any());
        verify(verificationMapper, never()).markAsUsed(any());
    }

    @Test
    @DisplayName("간편비밀번호 설정 실패 - 다른 회원의 토큰은 사용할 수 없다")
    void updateSimplePassword_fail_whenTokenBelongsToAnotherMember() {
        String tokenHash = sha256Hasher.sha256("change-token");
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member(1L));
        when(verificationMapper.findByChangeTokenHashForUpdate(tokenHash))
            .thenReturn(verification(2L, VerificationStatus.VERIFIED, 10));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.updateSimplePassword(1L, request("012345", "012345"))
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_INVALID);
        verify(memberMapper, never()).updateSimplePassword(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 설정 실패 - 만료된 변경 토큰은 사용할 수 없다")
    void updateSimplePassword_fail_whenTokenExpired() {
        String tokenHash = sha256Hasher.sha256("change-token");
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member(1L));
        SimplePasswordVerification verification =
            verification(1L, VerificationStatus.VERIFIED, 10);
        ReflectionTestUtils.setField(
            verification,
            "changeTokenExpiresAt",
            LocalDateTime.now().minusSeconds(1)
        );
        when(verificationMapper.findByChangeTokenHashForUpdate(tokenHash))
            .thenReturn(verification);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.updateSimplePassword(1L, request("012345", "012345"))
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_EXPIRED);
        verify(memberMapper, never()).updateSimplePassword(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 설정 실패 - 사용 완료된 변경 토큰은 재사용할 수 없다")
    void updateSimplePassword_fail_whenTokenAlreadyUsed() {
        String tokenHash = sha256Hasher.sha256("change-token");
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member(1L));
        when(verificationMapper.findByChangeTokenHashForUpdate(tokenHash))
            .thenReturn(verification(1L, VerificationStatus.USED, 10));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.updateSimplePassword(1L, request("012345", "012345"))
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_ALREADY_USED);
        verify(memberMapper, never()).updateSimplePassword(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 검사 성공 - 입력값이 저장된 BCrypt 해시와 일치한다")
    void verifySimplePassword_success_whenMatched() {
        Member member = member(1L);
        ReflectionTestUtils.setField(
            member,
            "simplePasswordHash",
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);

        var response = service.verifySimplePassword(
            1L,
            new SimplePasswordVerifyRequest("012345")
        );

        assertThat(response.matched()).isTrue();
    }

    @Test
    @DisplayName("간편비밀번호 검사 성공 - 입력값이 다르면 matched false를 반환한다")
    void verifySimplePassword_success_whenMismatched() {
        Member member = member(1L);
        ReflectionTestUtils.setField(
            member,
            "simplePasswordHash",
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);
        when(memberMapper.increaseSimplePasswordFailedAttemptCount(1L)).thenReturn(1);

        var response = service.verifySimplePassword(
            1L,
            new SimplePasswordVerifyRequest("999999")
        );

        assertThat(response.matched()).isFalse();
        verify(memberMapper).increaseSimplePasswordFailedAttemptCount(1L);
    }

    @Test
    @DisplayName("간편비밀번호 검사 실패 - 간편비밀번호가 설정되지 않았다")
    void verifySimplePassword_fail_whenNotSet() {
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member(1L));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.verifySimplePassword(
                1L,
                new SimplePasswordVerifyRequest("012345")
            )
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SIMPLE_PASSWORD_NOT_SET);
    }

    @Test
    @DisplayName("간편비밀번호 검사 실패 - 다섯 번째 불일치에서 5분간 잠근다")
    void verifySimplePassword_fail_whenFifthAttemptFails() {
        Member member = memberWithVerificationState(
            4,
            null,
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);
        when(memberMapper.lockSimplePasswordVerification(eq(1L), any())).thenReturn(1);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.verifySimplePassword(
                1L,
                new SimplePasswordVerifyRequest("999999")
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);
        verify(memberMapper).lockSimplePasswordVerification(
            1L,
            LocalDateTime.of(2026, 8, 13, 14, 5)
        );
    }

    @Test
    @DisplayName("간편비밀번호 검사 실패 - 잠금 시간 중에는 BCrypt 비교 전에 차단한다")
    void verifySimplePassword_fail_whenLocked() {
        Member member = memberWithVerificationState(
            5,
            LocalDateTime.of(2026, 8, 13, 14, 1),
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.verifySimplePassword(
                1L,
                new SimplePasswordVerifyRequest("012345")
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);
        verify(memberMapper, never()).resetSimplePasswordVerificationFailure(any());
    }

    @Test
    @DisplayName("간편비밀번호 검사 성공 - 이전 실패 횟수를 초기화한다")
    void verifySimplePassword_success_resetsPreviousFailures() {
        Member member = memberWithVerificationState(
            2,
            null,
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);
        when(memberMapper.resetSimplePasswordVerificationFailure(1L)).thenReturn(1);

        var response = service.verifySimplePassword(
            1L,
            new SimplePasswordVerifyRequest("012345")
        );

        assertThat(response.matched()).isTrue();
        verify(memberMapper).resetSimplePasswordVerificationFailure(1L);
    }

    @Test
    @DisplayName("간편비밀번호 검사 성공 - 잠금 만료 후 실패 상태를 초기화하고 다시 검사한다")
    void verifySimplePassword_success_afterLockExpired() {
        Member member = memberWithVerificationState(
            5,
            LocalDateTime.of(2026, 8, 13, 13, 59),
            passwordEncoder.encode("012345")
        );
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(memberMapper.findById(1L)).thenReturn(member);
        when(memberMapper.resetSimplePasswordVerificationFailure(1L)).thenReturn(1);

        var response = service.verifySimplePassword(
            1L,
            new SimplePasswordVerifyRequest("012345")
        );

        assertThat(response.matched()).isTrue();
        verify(memberMapper).resetSimplePasswordVerificationFailure(1L);
    }

    private SimplePasswordUpdateRequest request(String password, String confirmation) {
        return new SimplePasswordUpdateRequest("change-token", password, confirmation);
    }

    private Member member(Long memberId) {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "memberStatus", "ACTIVE");
        return member;
    }

    private Member memberWithVerificationState(
        int failedAttemptCount,
        LocalDateTime lockedUntil,
        String simplePasswordHash
    ) {
        Member member = member(1L);
        ReflectionTestUtils.setField(member, "simplePasswordHash", simplePasswordHash);
        ReflectionTestUtils.setField(
            member,
            "simplePasswordFailedAttemptCount",
            failedAttemptCount
        );
        ReflectionTestUtils.setField(member, "simplePasswordLockedUntil", lockedUntil);
        return member;
    }

    private SimplePasswordVerification verification(
        Long memberId,
        String status,
        long expiresInMinutes
    ) {
        SimplePasswordVerification verification = new SimplePasswordVerification();
        ReflectionTestUtils.setField(verification, "simplePasswordVerificationId", 10L);
        ReflectionTestUtils.setField(verification, "memberId", memberId);
        ReflectionTestUtils.setField(verification, "verificationStatus", status);
        ReflectionTestUtils.setField(
            verification,
            "changeTokenExpiresAt",
            LocalDateTime.now().plusMinutes(expiresInMinutes)
        );
        return verification;
    }
}
