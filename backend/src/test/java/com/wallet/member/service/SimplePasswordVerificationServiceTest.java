package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.service.EmailSender;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordEmailVerificationResponse;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyResponse;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

class SimplePasswordVerificationServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long VERIFICATION_ID = 100L;
    private static final String EMAIL = "user@example.com";
    private static final String VERIFICATION_CODE = "123456";
    private static final String CHANGE_TOKEN = "generated-change-token";

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 12, 0);

    private SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private MemberMapper memberMapper;
    private EmailSender emailSender;
    private VerificationCodeGenerator verificationCodeGenerator;
    private VerificationTokenGenerator verificationTokenGenerator;
    private TokenHashUtil tokenHashUtil;
    private Clock clock;

    private SimplePasswordVerificationService service;

    @BeforeEach
    void setUp() {
        simplePasswordVerificationMapper = mock(SimplePasswordVerificationMapper.class);
        memberMapper = mock(MemberMapper.class);
        emailSender = mock(EmailSender.class);
        verificationCodeGenerator = mock(VerificationCodeGenerator.class);
        verificationTokenGenerator = mock(VerificationTokenGenerator.class);
        tokenHashUtil = new TokenHashUtil();
        clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul")
        );

        service = new SimplePasswordVerificationService(
            simplePasswordVerificationMapper,
            memberMapper,
            emailSender,
            verificationCodeGenerator,
            verificationTokenGenerator,
            tokenHashUtil,
            clock
        );
    }

    // ── 인증 코드 발송 ────────────────────────────────────────────────────

    @Test
    @DisplayName("인증 코드 발송 성공 - 코드는 해시로 저장하고 마스킹된 이메일을 반환한다")
    void sendVerificationCode_success() {
        // given
        givenActiveMember();
        when(verificationCodeGenerator.generateSixDigitCode()).thenReturn(VERIFICATION_CODE);
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(null);

        // when
        SimplePasswordEmailVerificationResponse response = service.sendVerificationCode(MEMBER_ID);

        // then
        assertThat(response.email()).isEqualTo("us***@example.com");
        assertThat(response.expiresIn()).isEqualTo(300);

        // 새 코드를 내면 이전 인증은 전부 만료시킨다.
        verify(simplePasswordVerificationMapper).expireActiveByMemberId(MEMBER_ID);

        ArgumentCaptor<SimplePasswordVerification> captor =
            ArgumentCaptor.forClass(SimplePasswordVerification.class);
        verify(simplePasswordVerificationMapper).insert(captor.capture());

        SimplePasswordVerification saved = captor.getValue();
        // DB에는 원문이 아니라 해시가 들어가야 한다.
        assertThat(saved.getVerificationCodeHash())
            .isEqualTo(tokenHashUtil.sha256(VERIFICATION_CODE))
            .isNotEqualTo(VERIFICATION_CODE);
        assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(saved.getVerificationCodeExpiresAt()).isEqualTo(NOW.plusMinutes(5));

        // 메일에는 원문 코드가 나가야 사용자가 입력할 수 있다.
        verify(emailSender).sendSimplePasswordVerificationCode(EMAIL, VERIFICATION_CODE, 5);
    }

    @Test
    @DisplayName("인증 코드 발송 실패 - 1분 내 재요청이면 쿨다운 예외")
    void sendVerificationCode_fail_whenReissuedTooSoon() {
        // given
        givenActiveMember();

        // 30초 전에 발송된 인증 건이 남아 있다.
        SimplePasswordVerification recent = pendingVerification();
        setField(recent, "updatedAt", NOW.minusSeconds(30));
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(recent);

        // when & then
        assertThatThrownBy(() -> service.sendVerificationCode(MEMBER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_REISSUE_COOLDOWN);

        verify(simplePasswordVerificationMapper, never()).insert(any());
        verify(emailSender, never()).sendSimplePasswordVerificationCode(any(), any(), anyLong());
    }

    @Test
    @DisplayName("인증 코드 발송 - 1분이 지났으면 재발송할 수 있다")
    void sendVerificationCode_success_whenCooldownPassed() {
        // given
        givenActiveMember();
        when(verificationCodeGenerator.generateSixDigitCode()).thenReturn(VERIFICATION_CODE);

        SimplePasswordVerification old = pendingVerification();
        setField(old, "updatedAt", NOW.minusSeconds(61));
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(old);

        // when
        service.sendVerificationCode(MEMBER_ID);

        // then
        verify(simplePasswordVerificationMapper).insert(any());
    }

    @Test
    @DisplayName("인증 코드 발송 실패 - 메일 발송에 실패하면 EMAIL_SEND_FAILED로 바꿔 던진다")
    void sendVerificationCode_fail_whenEmailSendFails() {
        // given
        givenActiveMember();
        when(verificationCodeGenerator.generateSixDigitCode()).thenReturn(VERIFICATION_CODE);
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(null);

        doThrow(new IllegalStateException("SMTP 실패"))
            .when(emailSender)
            .sendSimplePasswordVerificationCode(anyString(), anyString(), anyLong());

        // when & then
        // 트랜잭션이 롤백되므로 위에서 INSERT한 인증 행도 남지 않는다.
        assertThatThrownBy(() -> service.sendVerificationCode(MEMBER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_FAILED);
    }

    @Test
    @DisplayName("인증 코드 발송 실패 - 탈퇴한 회원이면 MEMBER_WITHDRAWN")
    void sendVerificationCode_fail_whenMemberWithdrawn() {
        // given
        Member member = member();
        setField(member, "memberStatus", "WITHDRAWN");
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);

        // when & then
        assertThatThrownBy(() -> service.sendVerificationCode(MEMBER_ID))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_WITHDRAWN);
    }

    // ── 인증 코드 검증 ────────────────────────────────────────────────────

    @Test
    @DisplayName("인증 코드 검증 성공 - 변경 토큰을 발급하고 해시만 저장한다")
    void verifyCode_success() {
        // given
        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(pendingVerification());
        when(verificationTokenGenerator.generate()).thenReturn(CHANGE_TOKEN);
        when(simplePasswordVerificationMapper.verify(eq(VERIFICATION_ID), anyString(), any()))
            .thenReturn(1);

        // when
        SimplePasswordEmailVerificationVerifyResponse response =
            service.verifyCode(MEMBER_ID, verifyRequest(VERIFICATION_CODE));

        // then
        // 원문 토큰은 응답으로만 나간다.
        assertThat(response.simplePasswordChangeToken()).isEqualTo(CHANGE_TOKEN);
        assertThat(response.expiresIn()).isEqualTo(600);

        verify(simplePasswordVerificationMapper).verify(
            VERIFICATION_ID,
            tokenHashUtil.sha256(CHANGE_TOKEN),
            NOW.plusMinutes(10)
        );
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 코드가 다르면 실패 횟수를 늘리고 CODE_INVALID")
    void verifyCode_fail_whenCodeMismatch() {
        // given
        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(pendingVerification());

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest("999999")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_INVALID);

        verify(simplePasswordVerificationMapper).increaseFailedAttemptCount(VERIFICATION_ID);
        verify(simplePasswordVerificationMapper, never()).verify(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 5회째 실패면 ATTEMPT_LIMIT_EXCEEDED")
    void verifyCode_fail_whenAttemptLimitReached() {
        // given
        SimplePasswordVerification verification = pendingVerification();
        setField(verification, "failedAttemptCount", 4);

        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(verification);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest("999999")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);

        verify(simplePasswordVerificationMapper).increaseFailedAttemptCount(VERIFICATION_ID);
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 5회 실패했으면 코드 비교 없이 차단한다")
    void verifyCode_fail_whenAlreadyOverAttemptLimit() {
        // given
        SimplePasswordVerification verification = pendingVerification();
        setField(verification, "failedAttemptCount", 5);

        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(verification);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest(VERIFICATION_CODE)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);

        verify(simplePasswordVerificationMapper, never()).increaseFailedAttemptCount(anyLong());
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 만료된 코드면 EXPIRED로 바꾸고 예외를 던진다")
    void verifyCode_fail_whenCodeExpired() {
        // given
        SimplePasswordVerification verification = pendingVerification();
        setField(verification, "verificationCodeExpiresAt", NOW.minusSeconds(1));

        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(verification);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest(VERIFICATION_CODE)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_EXPIRED);

        verify(simplePasswordVerificationMapper).expireVerificationCode(VERIFICATION_ID);
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 이미 인증에 사용된 건이면 ALREADY_USED")
    void verifyCode_fail_whenAlreadyVerified() {
        // given
        SimplePasswordVerification verification = pendingVerification();
        setField(verification, "verificationStatus", VerificationStatus.VERIFIED);

        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(verification);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest(VERIFICATION_CODE)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_ALREADY_USED);
    }

    @Test
    @DisplayName("인증 코드 검증 실패 - 인증 요청 이력이 없으면 NOT_FOUND")
    void verifyCode_fail_whenNoVerification() {
        // given
        givenActiveMember();
        when(simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(MEMBER_ID))
            .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> service.verifyCode(MEMBER_ID, verifyRequest(VERIFICATION_CODE)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_VERIFICATION_NOT_FOUND);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private void givenActiveMember() {
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member());
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
    }

    private Member member() {
        Member member = Member.createSignupMember(
            EMAIL, "$2a$10$login-hash", "김활성", "별명A");
        setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private SimplePasswordVerification pendingVerification() {
        SimplePasswordVerification verification = SimplePasswordVerification.createPending(
            MEMBER_ID,
            tokenHashUtil.sha256(VERIFICATION_CODE),
            NOW.plusMinutes(5)
        );
        setField(verification, "simplePasswordVerificationId", VERIFICATION_ID);
        return verification;
    }

    private SimplePasswordEmailVerificationVerifyRequest verifyRequest(String code) {
        return new SimplePasswordEmailVerificationVerifyRequest(code);
    }

    // 도메인 객체가 세터를 열어두지 않아, 테스트 픽스처는 리플렉션으로 채운다.
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
