package com.wallet.member.service;

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

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.service.EmailSender;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyRequest;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

class SimplePasswordVerificationServiceTest {
    private final SimplePasswordVerificationMapper verificationMapper =
        mock(SimplePasswordVerificationMapper.class);
    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
    private final VerificationTokenGenerator tokenGenerator = mock(VerificationTokenGenerator.class);
    private final TokenHashUtil tokenHashUtil = new TokenHashUtil();

    private SimplePasswordVerificationService service;

    @BeforeEach
    void setUp() {
        service = new SimplePasswordVerificationService(
            verificationMapper,
            memberMapper,
            emailSender,
            codeGenerator,
            tokenGenerator,
            tokenHashUtil
        );
    }

    @Test
    @DisplayName("인증 코드 발송 성공 - 로그인 회원 이메일로 해시된 인증 정보를 저장한다")
    void sendVerificationCode_success() {
        Member member = member(1L, "user@example.com");
        when(memberMapper.findById(1L)).thenReturn(member);
        when(memberMapper.lockActiveMemberById(1L)).thenReturn(1L);
        when(codeGenerator.generateSixDigitCode()).thenReturn("482913");
        when(verificationMapper.insert(any())).thenReturn(1);

        var response = service.sendVerificationCode(1L);

        assertThat(response.email()).isEqualTo("us***@example.com");
        assertThat(response.expiresIn()).isEqualTo(300);
        verify(verificationMapper).expireActiveByMemberId(1L);
        verify(emailSender).sendSimplePasswordVerificationCode(
            "user@example.com",
            "482913",
            5
        );
        verify(verificationMapper).insert(any(SimplePasswordVerification.class));
    }

    @Test
    @DisplayName("인증 코드 검증 성공 - 일회용 변경 토큰을 발급하고 해시만 저장한다")
    void verifyCode_success() {
        when(memberMapper.findById(1L)).thenReturn(member(1L, "user@example.com"));
        when(verificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification(
                VerificationStatus.PENDING,
                0,
                LocalDateTime.now().plusMinutes(1)
            ));
        when(tokenGenerator.generate()).thenReturn("change-token");
        when(verificationMapper.verify(eq(10L), any(), any())).thenReturn(1);

        var response = service.verifyCode(
            1L,
            new SimplePasswordEmailVerificationVerifyRequest("482913")
        );

        assertThat(response.simplePasswordChangeToken()).isEqualTo("change-token");
        assertThat(response.expiresIn()).isEqualTo(600);
        verify(verificationMapper).verify(
            eq(10L),
            eq(tokenHashUtil.sha256("change-token")),
            any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 불일치하면 실패 횟수를 증가시킨다")
    void verifyCode_fail_whenCodeMismatch() {
        when(memberMapper.findById(1L)).thenReturn(member(1L, "user@example.com"));
        when(verificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification(
                VerificationStatus.PENDING,
                0,
                LocalDateTime.now().plusMinutes(1)
            ));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.verifyCode(
                1L,
                new SimplePasswordEmailVerificationVerifyRequest("111111")
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_INVALID);
        verify(verificationMapper).increaseFailedAttemptCount(10L);
        verify(verificationMapper, never()).verify(any(), any(), any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 만료된 코드는 만료 상태로 변경한다")
    void verifyCode_fail_whenCodeExpired() {
        when(memberMapper.findById(1L)).thenReturn(member(1L, "user@example.com"));
        when(verificationMapper.findLatestByMemberIdForUpdate(1L))
            .thenReturn(verification(
                VerificationStatus.PENDING,
                0,
                LocalDateTime.now().minusSeconds(1)
            ));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.verifyCode(
                1L,
                new SimplePasswordEmailVerificationVerifyRequest("482913")
            )
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_EXPIRED);
        verify(verificationMapper).expireVerificationCode(10L);
    }

    private Member member(Long memberId, String email) {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "memberStatus", "ACTIVE");
        return member;
    }

    private SimplePasswordVerification verification(
        String status,
        int failedAttemptCount,
        LocalDateTime expiresAt
    ) {
        SimplePasswordVerification verification = new SimplePasswordVerification();
        ReflectionTestUtils.setField(verification, "simplePasswordVerificationId", 10L);
        ReflectionTestUtils.setField(verification, "memberId", 1L);
        ReflectionTestUtils.setField(
            verification,
            "verificationCodeHash",
            tokenHashUtil.sha256("482913")
        );
        ReflectionTestUtils.setField(verification, "verificationStatus", status);
        ReflectionTestUtils.setField(verification, "failedAttemptCount", failedAttemptCount);
        ReflectionTestUtils.setField(verification, "verificationCodeExpiresAt", expiresAt);
        ReflectionTestUtils.setField(verification, "updatedAt", LocalDateTime.now().minusMinutes(2));
        return verification;
    }
}
