package com.wallet.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.domain.PasswordResetVerification;
import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.dto.PasswordResetCodeRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyResponse;
import com.wallet.auth.dto.PasswordResetRequest;
import com.wallet.auth.mapper.PasswordResetVerificationMapper;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

class PasswordResetServiceTest {

    private PasswordResetVerificationMapper passwordResetVerificationMapper;
    private MemberMapper memberMapper;
    private EmailSender emailSender;
    private VerificationCodeGenerator verificationCodeGenerator;
    private VerificationTokenGenerator verificationTokenGenerator;
    private Sha256Hasher sha256Hasher;
    private PasswordResetService passwordResetService;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        passwordResetVerificationMapper = mock(PasswordResetVerificationMapper.class);
        memberMapper = mock(MemberMapper.class);
        emailSender = mock(EmailSender.class);
        verificationCodeGenerator = mock(VerificationCodeGenerator.class);
        verificationTokenGenerator = mock(VerificationTokenGenerator.class);
        sha256Hasher = new Sha256Hasher();
        passwordEncoder = new BCryptPasswordEncoder();
        refreshTokenService = mock(RefreshTokenService.class);

        passwordResetService = new PasswordResetService(
            passwordResetVerificationMapper,
            memberMapper,
            emailSender,
            verificationCodeGenerator,
            verificationTokenGenerator,
            sha256Hasher,
            passwordEncoder,
            refreshTokenService

        );
    }

    @Test
    @DisplayName("인증 코드 요청 성공 - ACTIVE 회원이면 기존 인증 정보를 만료시키고 새 인증 코드를 저장한 뒤 이메일을 발송한다")
    void sendResetCode_success() {
        // given
        PasswordResetCodeRequest request = new PasswordResetCodeRequest("USER@example.com");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(null);

        when(verificationCodeGenerator.generateSixDigitCode())
            .thenReturn("482913");

        // when
        passwordResetService.sendResetCode(request);

        // then
        verify(memberMapper).findByEmail("user@example.com");
        verify(passwordResetVerificationMapper).findLatestByMemberIdForUpdate(1L);
        verify(passwordResetVerificationMapper).expireActiveByMemberId(1L);

        ArgumentCaptor<PasswordResetVerification> verificationCaptor =
            ArgumentCaptor.forClass(PasswordResetVerification.class);

        verify(passwordResetVerificationMapper).insert(verificationCaptor.capture());

        PasswordResetVerification savedVerification = verificationCaptor.getValue();

        assertThat(savedVerification.getMemberId()).isEqualTo(1L);
        assertThat(savedVerification.getVerificationStatus())
            .isEqualTo(VerificationStatus.PENDING);
        assertThat(savedVerification.getFailedAttemptCount()).isZero();
        assertThat(savedVerification.getVerificationCodeHash())
            .isEqualTo(sha256Hasher.sha256("482913"));
        assertThat(savedVerification.getVerificationCodeExpiresAt())
            .isAfter(LocalDateTime.now());

        verify(emailSender).sendPasswordResetVerificationCode(
            "user@example.com",
            "482913",
            5L
        );
    }

    @Test
    @DisplayName("인증 코드 요청 - 존재하지 않는 이메일이어도 예외 없이 성공 처리하고 인증 코드를 저장하지 않는다")
    void sendResetCode_success_whenEmailNotFound() {
        // given
        PasswordResetCodeRequest request = new PasswordResetCodeRequest("unknown@example.com");

        when(memberMapper.findByEmail("unknown@example.com"))
            .thenReturn(null);

        // when
        passwordResetService.sendResetCode(request);

        // then
        verify(memberMapper).findByEmail("unknown@example.com");
        verify(passwordResetVerificationMapper, never()).insert(any());
        verify(emailSender, never()).sendPasswordResetVerificationCode(
            any(),
            any(),
            any(Long.class)
        );
    }

    @Test
    @DisplayName("인증 코드 요청 - ACTIVE 회원이 아니면 예외 없이 성공 처리하고 인증 코드를 저장하지 않는다")
    void sendResetCode_success_whenMemberIsNotActive() {
        // given
        PasswordResetCodeRequest request = new PasswordResetCodeRequest("user@example.com");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "SUSPENDED",
            null
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        // when
        passwordResetService.sendResetCode(request);

        // then
        verify(memberMapper).findByEmail("user@example.com");
        verify(passwordResetVerificationMapper, never()).insert(any());
        verify(emailSender, never()).sendPasswordResetVerificationCode(
            any(),
            any(),
            any(Long.class)
        );
    }

    @Test
    @DisplayName("인증 코드 요청 실패 - 60초 안에 다시 요청하면 PASSWORD_RESET_REISSUE_COOLDOWN 예외가 발생한다")
    void sendResetCode_fail_whenReissueTooSoon() {
        // given
        PasswordResetCodeRequest request = new PasswordResetCodeRequest("user@example.com");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification existingVerification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("111111"),
            VerificationStatus.PENDING,
            0,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now()
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(existingVerification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.sendResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_REISSUE_COOLDOWN);

        verify(passwordResetVerificationMapper, never()).expireActiveByMemberId(any());
        verify(passwordResetVerificationMapper, never()).insert(any());
        verify(emailSender, never()).sendPasswordResetVerificationCode(
            any(),
            any(),
            any(Long.class)
        );
    }

    @Test
    @DisplayName("인증 코드 검증 성공 - 코드가 일치하면 passwordResetToken을 발급하고 토큰 해시를 저장한다")
    void verifyResetCode_success() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("USER@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            0,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(2)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        when(verificationTokenGenerator.generate())
            .thenReturn("password-reset-token");

        when(passwordResetVerificationMapper.verify(
            eq(10L),
            eq(sha256Hasher.sha256("password-reset-token")),
            any(LocalDateTime.class)
        )).thenReturn(1);

        // when
        PasswordResetCodeVerifyResponse response =
            passwordResetService.verifyResetCode(request);

        // then
        assertThat(response.passwordResetToken())
            .isEqualTo("password-reset-token");
        assertThat(response.expiresIn()).isEqualTo(600L);

        verify(memberMapper).findByEmail("user@example.com");
        verify(passwordResetVerificationMapper).findLatestByMemberIdForUpdate(1L);
        verify(verificationTokenGenerator).generate();

        verify(passwordResetVerificationMapper).verify(
            eq(10L),
            eq(sha256Hasher.sha256("password-reset-token")),
            any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 회원이 없으면 PASSWORD_RESET_CODE_INVALID 예외가 발생한다")
    void verifyResetCode_fail_whenMemberNotFound() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("unknown@example.com", "482913");

        when(memberMapper.findByEmail("unknown@example.com"))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_INVALID);

        verify(passwordResetVerificationMapper, never()).findLatestByMemberIdForUpdate(any());
        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 정보가 없으면 PASSWORD_RESET_CODE_INVALID 예외가 발생한다")
    void verifyResetCode_fail_whenVerificationNotFound() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_INVALID);

        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 코드가 일치하지 않으면 실패 횟수를 증가시키고 PASSWORD_RESET_CODE_INVALID 예외가 발생한다")
    void verifyResetCode_fail_whenCodeMismatch() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "000000");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            0,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(2)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_INVALID);

        verify(passwordResetVerificationMapper).increaseFailedAttemptCount(10L);
        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이번 실패로 최대 실패 횟수에 도달하면 PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED 예외가 발생한다")
    void verifyResetCode_fail_whenCodeMismatchAndAttemptLimitReached() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "000000");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            4,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(2)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED);

        verify(passwordResetVerificationMapper).increaseFailedAttemptCount(10L);
        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 최대 실패 횟수를 초과한 인증 정보면 PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED 예외가 발생한다")
    void verifyResetCode_fail_whenAttemptLimitAlreadyExceeded() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            5,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(2)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED);

        verify(passwordResetVerificationMapper, never()).increaseFailedAttemptCount(any());
        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 코드가 만료되면 상태를 EXPIRED로 바꾸고 PASSWORD_RESET_CODE_EXPIRED 예외가 발생한다")
    void verifyResetCode_fail_whenCodeExpired() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            0,
            LocalDateTime.now().minusSeconds(1),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(10)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);

        verify(passwordResetVerificationMapper).expireVerificationCode(10L);
        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 VERIFIED 상태면 PASSWORD_RESET_CODE_ALREADY_USED 예외가 발생한다")
    void verifyResetCode_fail_whenAlreadyVerified() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().plusMinutes(5),
            sha256Hasher.sha256("already-issued-token"),
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(1),
            null,
            LocalDateTime.now().minusMinutes(2)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_ALREADY_USED);

        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 USED 상태면 PASSWORD_RESET_CODE_ALREADY_USED 예외가 발생한다")
    void verifyResetCode_fail_whenAlreadyUsed() {
        // given
        PasswordResetCodeVerifyRequest request =
            new PasswordResetCodeVerifyRequest("user@example.com", "482913");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "ACTIVE",
            null
        );

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.USED,
            0,
            LocalDateTime.now().plusMinutes(5),
            sha256Hasher.sha256("already-used-token"),
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(2),
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().minusMinutes(3)
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(passwordResetVerificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.verifyResetCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_ALREADY_USED);

        verify(verificationTokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("새 비밀번호 설정 성공 - 유효한 reset token이면 비밀번호를 변경하고 기존 Refresh Token을 모두 폐기한다")
    void resetPassword_success() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(1),
            null,
            LocalDateTime.now().minusMinutes(1)
        );

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("oldPassword123"),
            "ACTIVE",
            null
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        when(memberMapper.findById(1L))
            .thenReturn(member);

        when(memberMapper.updatePassword(eq(1L), any(String.class)))
            .thenReturn(1);

        when(passwordResetVerificationMapper.markAsUsed(10L))
            .thenReturn(1);

        // when
        passwordResetService.resetPassword(request);

        // then
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(memberMapper).updatePassword(eq(1L), passwordCaptor.capture());

        String encodedPassword = passwordCaptor.getValue();

        assertThat(encodedPassword).isNotEqualTo("newPassword123");
        assertThat(passwordEncoder.matches("newPassword123", encodedPassword)).isTrue();

        verify(passwordResetVerificationMapper).markAsUsed(10L);
        verify(refreshTokenService).revokeAllByPasswordReset(1L);
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - reset token이 없으면 PASSWORD_RESET_TOKEN_INVALID 예외가 발생한다")
    void resetPassword_fail_whenTokenNotFound() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "unknown-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("unknown-token");

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - VERIFIED 상태가 아니면 PASSWORD_RESET_TOKEN_INVALID 예외가 발생한다")
    void resetPassword_fail_whenStatusIsNotVerified() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.PENDING,
            0,
            LocalDateTime.now().plusMinutes(5),
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusMinutes(1)
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - 이미 USED 상태면 PASSWORD_RESET_TOKEN_ALREADY_USED 예외가 발생한다")
    void resetPassword_fail_whenTokenAlreadyUsed() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.USED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(2),
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().minusMinutes(1)
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - reset token이 만료되면 PASSWORD_RESET_TOKEN_EXPIRED 예외가 발생한다")
    void resetPassword_fail_whenTokenExpired() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().minusSeconds(1),
            LocalDateTime.now().minusMinutes(10),
            null,
            LocalDateTime.now().minusMinutes(10)
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - 회원이 없으면 PASSWORD_RESET_TOKEN_INVALID 예외가 발생한다")
    void resetPassword_fail_whenMemberNotFound() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(1),
            null,
            LocalDateTime.now().minusMinutes(1)
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        when(memberMapper.findById(1L))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - 비밀번호 업데이트 결과가 0이면 PASSWORD_RESET_TOKEN_INVALID 예외가 발생한다")
    void resetPassword_fail_whenPasswordUpdateFailed() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "newPassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(1),
            null,
            LocalDateTime.now().minusMinutes(1)
        );

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("oldPassword123"),
            "ACTIVE",
            null
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        when(memberMapper.findById(1L))
            .thenReturn(member);

        when(memberMapper.updatePassword(eq(1L), any(String.class)))
            .thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);

        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
        verify(refreshTokenService, never()).revokeAllByPasswordReset(any());
    }

    private Member createMember(
        Long memberId,
        String email,
        String password,
        String memberStatus,
        LocalDateTime withdrawnAt
    ) {
        Member member = new Member();

        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "password", password);
        ReflectionTestUtils.setField(member, "memberStatus", memberStatus);
        ReflectionTestUtils.setField(member, "withdrawnAt", withdrawnAt);

        return member;
    }

    @Test
    @DisplayName("새 비밀번호 설정 실패 - 기존 비밀번호와 같으면 PASSWORD_SAME_AS_CURRENT 예외가 발생한다")
    void resetPassword_fail_whenNewPasswordSameAsCurrentPassword() {
        // given
        PasswordResetRequest request = new PasswordResetRequest(
            "password-reset-token",
            "samePassword123"
        );

        String resetTokenHash = sha256Hasher.sha256("password-reset-token");

        PasswordResetVerification verification = createPasswordResetVerification(
            10L,
            1L,
            sha256Hasher.sha256("482913"),
            VerificationStatus.VERIFIED,
            0,
            LocalDateTime.now().minusMinutes(1),
            resetTokenHash,
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().minusMinutes(1),
            null,
            LocalDateTime.now().minusMinutes(1)
        );

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("samePassword123"),
            "ACTIVE",
            null
        );

        when(passwordResetVerificationMapper.findByResetTokenHashForUpdate(resetTokenHash))
            .thenReturn(verification);

        when(memberMapper.findById(1L))
            .thenReturn(member);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> passwordResetService.resetPassword(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PASSWORD_SAME_AS_CURRENT);

        verify(memberMapper, never()).updatePassword(any(), any());
        verify(passwordResetVerificationMapper, never()).markAsUsed(any());
    }

    private PasswordResetVerification createPasswordResetVerification(
        Long passwordResetVerificationId,
        Long memberId,
        String verificationCodeHash,
        String verificationStatus,
        Integer failedAttemptCount,
        LocalDateTime verificationCodeExpiresAt,
        String resetTokenHash,
        LocalDateTime resetTokenExpiresAt,
        LocalDateTime verifiedAt,
        LocalDateTime usedAt,
        LocalDateTime updatedAt
    ) {
        PasswordResetVerification verification = new PasswordResetVerification();

        ReflectionTestUtils.setField(
            verification,
            "passwordResetVerificationId",
            passwordResetVerificationId
        );
        ReflectionTestUtils.setField(verification, "memberId", memberId);
        ReflectionTestUtils.setField(verification, "verificationCodeHash", verificationCodeHash);
        ReflectionTestUtils.setField(verification, "verificationStatus", verificationStatus);
        ReflectionTestUtils.setField(verification, "failedAttemptCount", failedAttemptCount);
        ReflectionTestUtils.setField(
            verification,
            "verificationCodeExpiresAt",
            verificationCodeExpiresAt
        );
        ReflectionTestUtils.setField(verification, "resetTokenHash", resetTokenHash);
        ReflectionTestUtils.setField(verification, "resetTokenExpiresAt", resetTokenExpiresAt);
        ReflectionTestUtils.setField(verification, "verifiedAt", verifiedAt);
        ReflectionTestUtils.setField(verification, "usedAt", usedAt);
        ReflectionTestUtils.setField(verification, "updatedAt", updatedAt);

        return verification;
    }
}