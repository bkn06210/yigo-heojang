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
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.domain.SignupEmailVerification;
import com.wallet.auth.dto.SignupEmailVerificationConfirmRequest;
import com.wallet.auth.dto.SignupEmailVerificationConfirmResponse;
import com.wallet.auth.dto.SignupEmailVerificationRequest;
import com.wallet.auth.dto.SignupEmailVerificationResponse;
import com.wallet.auth.mapper.SignupEmailVerificationMapper;
import com.wallet.auth.support.SignupVerificationTokenGenerator;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.mapper.MemberMapper;

class SignupEmailVerificationServiceTest {
    private SignupEmailVerificationMapper signupEmailVerificationMapper;
    private MemberMapper memberMapper;
    private EmailSender emailSender;
    private VerificationCodeGenerator verificationCodeGenerator;
    private SignupVerificationTokenGenerator signupVerificationTokenGenerator;
    private TokenHashUtil tokenHashUtil;
    private SignupEmailVerificationService signupEmailVerificationService;

    @BeforeEach
    void setUp() {
        signupEmailVerificationMapper = mock(SignupEmailVerificationMapper.class);
        memberMapper = mock(MemberMapper.class);
        emailSender = mock(EmailSender.class);
        verificationCodeGenerator = mock(VerificationCodeGenerator.class);
        signupVerificationTokenGenerator = mock(SignupVerificationTokenGenerator.class);
        tokenHashUtil = mock(TokenHashUtil.class);

        signupEmailVerificationService = new SignupEmailVerificationService(
            signupEmailVerificationMapper,
            memberMapper,
            emailSender,
            verificationCodeGenerator,
            signupVerificationTokenGenerator,
            tokenHashUtil
        );
    }

    @Test
    @DisplayName("인증 코드 요청 성공 - 기존 인증 정보가 없으면 새 인증 정보를 저장하고 메일을 발송한다")
    void sendVerificationCode_success_whenVerificationNotExists() {
        // given
        SignupEmailVerificationRequest request =
            new SignupEmailVerificationRequest("USER@example.com");

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(null);

        when(verificationCodeGenerator.generateSixDigitCode())
            .thenReturn("123456");

        when(tokenHashUtil.sha256("123456"))
            .thenReturn("code-hash");

        // when
        SignupEmailVerificationResponse response =
            signupEmailVerificationService.sendVerificationCode(request);

        // then
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.expiresInSeconds()).isEqualTo(300L);

        verify(memberMapper).existsByEmail("user@example.com");
        verify(signupEmailVerificationMapper).findByEmailForUpdate("user@example.com");

        verify(signupEmailVerificationMapper).insert(any(SignupEmailVerification.class));

        verify(signupEmailVerificationMapper, never()).updateForReissue(
            any(),
            any(),
            any()
        );

        verify(emailSender).sendSignupVerificationCode(
            "user@example.com",
            "123456",
            5L
        );
    }

    @Test
    @DisplayName("인증 코드 요청 성공 - 기존 인증 정보가 있으면 기존 행을 재사용해 갱신하고 메일을 발송한다")
    void sendVerificationCode_success_whenVerificationExists() {
        // given
        SignupEmailVerificationRequest request =
            new SignupEmailVerificationRequest("user@example.com");

        SignupEmailVerification existingVerification = createVerification(
            1L,
            "user@example.com",
            "old-code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(existingVerification);

        when(verificationCodeGenerator.generateSixDigitCode())
            .thenReturn("654321");

        when(tokenHashUtil.sha256("654321"))
            .thenReturn("new-code-hash");

        // when
        SignupEmailVerificationResponse response =
            signupEmailVerificationService.sendVerificationCode(request);

        // then
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.expiresInSeconds()).isEqualTo(300L);

        verify(signupEmailVerificationMapper, never()).insert(any());

        verify(signupEmailVerificationMapper).updateForReissue(
            eq("user@example.com"),
            eq("new-code-hash"),
            any(LocalDateTime.class)
        );

        verify(emailSender).sendSignupVerificationCode(
            "user@example.com",
            "654321",
            5L
        );
    }

    @Test
    @DisplayName("인증 코드 요청 실패 - 이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외가 발생한다")
    void sendVerificationCode_fail_whenEmailAlreadyExists() {
        // given
        SignupEmailVerificationRequest request =
            new SignupEmailVerificationRequest("user@example.com");

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(true);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.sendVerificationCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(memberMapper).existsByEmail("user@example.com");
        verify(signupEmailVerificationMapper, never()).findByEmailForUpdate(any());
        verify(emailSender, never()).sendSignupVerificationCode(any(), any(), any(Long.class));
    }

    @Test
    @DisplayName("인증 코드 요청 실패 - 재요청 제한 시간 전이면 REQUEST_TOO_FREQUENT 예외가 발생한다")
    void sendVerificationCode_fail_whenRequestedTooFrequently() {
        // given
        SignupEmailVerificationRequest request =
            new SignupEmailVerificationRequest("user@example.com");

        SignupEmailVerification existingVerification = createVerification(
            1L,
            "user@example.com",
            "old-code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(10)
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(existingVerification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.sendVerificationCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT);

        verify(verificationCodeGenerator, never()).generateSixDigitCode();
        verify(signupEmailVerificationMapper, never()).insert(any());
        verify(signupEmailVerificationMapper, never()).updateForReissue(any(), any(), any());
        verify(emailSender, never()).sendSignupVerificationCode(any(), any(), any(Long.class));
    }

    @Test
    @DisplayName("인증 코드 요청 실패 - 메일 발송 실패 시 EMAIL_SEND_FAILED 예외가 발생한다")
    void sendVerificationCode_fail_whenEmailSendFailed() {
        // given
        SignupEmailVerificationRequest request =
            new SignupEmailVerificationRequest("user@example.com");

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(null);

        when(verificationCodeGenerator.generateSixDigitCode())
            .thenReturn("123456");

        when(tokenHashUtil.sha256("123456"))
            .thenReturn("code-hash");

        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP fail"))
            .when(emailSender)
            .sendSignupVerificationCode("user@example.com", "123456", 5L);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.sendVerificationCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.EMAIL_SEND_FAILED);

        verify(signupEmailVerificationMapper).insert(any(SignupEmailVerification.class));
    }

    @Test
    @DisplayName("인증 코드 검증 성공 - 올바른 코드이면 signupVerificationToken을 발급한다")
    void verifyCode_success() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("USER@example.com", "123456");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        when(tokenHashUtil.sha256("123456"))
            .thenReturn("code-hash");

        when(signupVerificationTokenGenerator.generate())
            .thenReturn("signup-token");

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.verify(
            eq(1L),
            eq("signup-token-hash"),
            any(LocalDateTime.class)
        )).thenReturn(1);

        // when
        SignupEmailVerificationConfirmResponse response =
            signupEmailVerificationService.verifyCode(request);

        // then
        assertThat(response.signupVerificationToken()).isEqualTo("signup-token");
        assertThat(response.expiresInSeconds()).isEqualTo(600L);

        verify(signupEmailVerificationMapper).findByEmailForUpdate("user@example.com");

        verify(signupEmailVerificationMapper).verify(
            eq(1L),
            eq("signup-token-hash"),
            any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 정보가 없으면 NOT_FOUND 예외가 발생한다")
    void verifyCode_fail_whenVerificationNotFound() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "123456");

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_NOT_FOUND);

        verify(tokenHashUtil, never()).sha256(any());
        verify(signupEmailVerificationMapper, never()).verify(any(), any(), any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 검증 완료된 상태이면 CODE_INVALID 예외가 발생한다")
    void verifyCode_fail_whenStatusIsNotPending() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "123456");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "code-hash",
            SignupEmailVerification.STATUS_VERIFIED,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_INVALID);

        verify(tokenHashUtil, never()).sha256(any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 실패 횟수가 제한에 도달했으면 ATTEMPT_LIMIT_EXCEEDED 예외가 발생한다")
    void verifyCode_fail_whenAttemptLimitExceeded() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "123456");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "code-hash",
            SignupEmailVerification.STATUS_PENDING,
            5,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);

        verify(tokenHashUtil, never()).sha256(any());
        verify(signupEmailVerificationMapper, never()).increaseFailedAttemptCount(any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 코드가 만료되었으면 EXPIRED 처리 후 CODE_EXPIRED 예외가 발생한다")
    void verifyCode_fail_whenCodeExpired() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "123456");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().minusSeconds(1),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_EXPIRED);

        verify(signupEmailVerificationMapper).expireVerificationCode(1L);
        verify(tokenHashUtil, never()).sha256(any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 코드가 일치하지 않으면 실패 횟수를 증가시킨다")
    void verifyCode_fail_whenCodeMismatch() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "000000");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "correct-code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        when(tokenHashUtil.sha256("000000"))
            .thenReturn("wrong-code-hash");

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_CODE_INVALID);

        verify(signupEmailVerificationMapper).increaseFailedAttemptCount(1L);
        verify(signupVerificationTokenGenerator, never()).generate();
        verify(signupEmailVerificationMapper, never()).verify(any(), any(), any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 검증 완료 업데이트에 실패하면 TOKEN_FAILED 예외가 발생한다")
    void verifyCode_fail_whenVerifyUpdateFailed() {
        // given
        SignupEmailVerificationConfirmRequest request =
            new SignupEmailVerificationConfirmRequest("user@example.com", "123456");

        SignupEmailVerification verification = createVerification(
            1L,
            "user@example.com",
            "code-hash",
            SignupEmailVerification.STATUS_PENDING,
            0,
            LocalDateTime.now().plusMinutes(3),
            LocalDateTime.now().minusSeconds(61)
        );

        when(signupEmailVerificationMapper.findByEmailForUpdate("user@example.com"))
            .thenReturn(verification);

        when(tokenHashUtil.sha256("123456"))
            .thenReturn("code-hash");

        when(signupVerificationTokenGenerator.generate())
            .thenReturn("signup-token");

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.verify(
            eq(1L),
            eq("signup-token-hash"),
            any(LocalDateTime.class)
        )).thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.verifyCode(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_TOKEN_FAILED);
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 성공 - VERIFIED 상태이고 만료 전이며 이메일이 일치하면 인증 ID를 반환한다")
    void validateSignupVerificationToken_success() {
        // given
        SignupEmailVerification verification = createSignupTokenVerification(
            1L,
            "user@example.com",
            SignupEmailVerification.STATUS_VERIFIED,
            LocalDateTime.now().plusMinutes(5)
        );

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("signup-token-hash"))
            .thenReturn(verification);

        // when
        Long verificationId = signupEmailVerificationService.validateSignupVerificationToken(
            "USER@example.com",
            "signup-token"
        );

        // then
        assertThat(verificationId).isEqualTo(1L);

        verify(tokenHashUtil).sha256("signup-token");
        verify(signupEmailVerificationMapper)
            .findBySignupTokenHashForUpdate("signup-token-hash");
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 실패 - 토큰 해시에 해당하는 인증 정보가 없으면 TOKEN_INVALID 예외가 발생한다")
    void validateSignupVerificationToken_fail_whenTokenNotFound() {
        // given
        when(tokenHashUtil.sha256("invalid-token"))
            .thenReturn("invalid-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("invalid-token-hash"))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.validateSignupVerificationToken(
                "user@example.com",
                "invalid-token"
            )
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 실패 - 이미 USED 상태이면 ALREADY_USED 예외가 발생한다")
    void validateSignupVerificationToken_fail_whenAlreadyUsed() {
        // given
        SignupEmailVerification verification = createSignupTokenVerification(
            1L,
            "user@example.com",
            SignupEmailVerification.STATUS_USED,
            LocalDateTime.now().plusMinutes(5)
        );

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("signup-token-hash"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.validateSignupVerificationToken(
                "user@example.com",
                "signup-token"
            )
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_EMAIL_VERIFICATION_ALREADY_USED);
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 실패 - VERIFIED 상태가 아니면 TOKEN_INVALID 예외가 발생한다")
    void validateSignupVerificationToken_fail_whenStatusIsNotVerified() {
        // given
        SignupEmailVerification verification = createSignupTokenVerification(
            1L,
            "user@example.com",
            SignupEmailVerification.STATUS_PENDING,
            LocalDateTime.now().plusMinutes(5)
        );

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("signup-token-hash"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.validateSignupVerificationToken(
                "user@example.com",
                "signup-token"
            )
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 실패 - 토큰이 만료되었으면 TOKEN_EXPIRED 예외가 발생한다")
    void validateSignupVerificationToken_fail_whenTokenExpired() {
        // given
        SignupEmailVerification verification = createSignupTokenVerification(
            1L,
            "user@example.com",
            SignupEmailVerification.STATUS_VERIFIED,
            LocalDateTime.now().minusSeconds(1)
        );

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("signup-token-hash"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.validateSignupVerificationToken(
                "user@example.com",
                "signup-token"
            )
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("회원가입 인증 토큰 검증 실패 - 인증 이메일과 요청 이메일이 다르면 EMAIL_MISMATCH 예외가 발생한다")
    void validateSignupVerificationToken_fail_whenEmailMismatch() {
        // given
        SignupEmailVerification verification = createSignupTokenVerification(
            1L,
            "verified@example.com",
            SignupEmailVerification.STATUS_VERIFIED,
            LocalDateTime.now().plusMinutes(5)
        );

        when(tokenHashUtil.sha256("signup-token"))
            .thenReturn("signup-token-hash");

        when(signupEmailVerificationMapper.findBySignupTokenHashForUpdate("signup-token-hash"))
            .thenReturn(verification);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.validateSignupVerificationToken(
                "request@example.com",
                "signup-token"
            )
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_EMAIL_MISMATCH);
    }

    @Test
    @DisplayName("회원가입 이메일 인증 사용 완료 처리 성공")
    void markAsUsed_success() {
        // given
        when(signupEmailVerificationMapper.markAsUsed(1L))
            .thenReturn(1);

        // when
        signupEmailVerificationService.markAsUsed(1L);

        // then
        verify(signupEmailVerificationMapper).markAsUsed(1L);
    }

    @Test
    @DisplayName("회원가입 이메일 인증 사용 완료 처리 실패 - 갱신된 행이 없으면 TOKEN_INVALID 예외가 발생한다")
    void markAsUsed_fail_whenUpdatedCountIsZero() {
        // given
        when(signupEmailVerificationMapper.markAsUsed(1L))
            .thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> signupEmailVerificationService.markAsUsed(1L)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIGNUP_VERIFICATION_TOKEN_INVALID);
    }

    private SignupEmailVerification createVerification(
        Long id,
        String email,
        String verificationCodeHash,
        String status,
        int failedAttemptCount,
        LocalDateTime verificationCodeExpiresAt,
        LocalDateTime updatedAt
    ) {
        SignupEmailVerification verification = new SignupEmailVerification();

        ReflectionTestUtils.setField(verification, "signupEmailVerificationId", id);
        ReflectionTestUtils.setField(verification, "email", email);
        ReflectionTestUtils.setField(verification, "verificationCodeHash", verificationCodeHash);
        ReflectionTestUtils.setField(verification, "verificationStatus", status);
        ReflectionTestUtils.setField(verification, "failedAttemptCount", failedAttemptCount);
        ReflectionTestUtils.setField(verification, "verificationCodeExpiresAt", verificationCodeExpiresAt);
        ReflectionTestUtils.setField(verification, "updatedAt", updatedAt);

        return verification;
    }

    private SignupEmailVerification createSignupTokenVerification(
        Long id,
        String email,
        String status,
        LocalDateTime signupTokenExpiresAt
    ) {
        SignupEmailVerification verification = new SignupEmailVerification();

        ReflectionTestUtils.setField(verification, "signupEmailVerificationId", id);
        ReflectionTestUtils.setField(verification, "email", email);
        ReflectionTestUtils.setField(verification, "verificationStatus", status);
        ReflectionTestUtils.setField(verification, "signupTokenHash", "signup-token-hash");
        ReflectionTestUtils.setField(verification, "signupTokenExpiresAt", signupTokenExpiresAt);

        return verification;
    }
}