package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordUpdateRequest;
import com.wallet.member.dto.SimplePasswordVerifyRequest;
import com.wallet.member.dto.SimplePasswordVerifyResponse;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

class SimplePasswordServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long VERIFICATION_ID = 100L;
    private static final String CHANGE_TOKEN = "change-token-value";
    private static final String SIMPLE_PASSWORD = "012345";
    private static final String STORED_HASH = "$2a$10$stored-simple-password-hash";

    // 시각 의존 로직(잠금 만료)을 재현하려고 시계를 고정한다.
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 12, 0);

    private MemberMapper memberMapper;
    private SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private TokenHashUtil tokenHashUtil;
    private PasswordEncoder passwordEncoder;
    private Clock clock;

    private SimplePasswordService simplePasswordService;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        simplePasswordVerificationMapper = mock(SimplePasswordVerificationMapper.class);
        tokenHashUtil = new TokenHashUtil();
        passwordEncoder = mock(PasswordEncoder.class);
        clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul")
        );

        simplePasswordService = new SimplePasswordService(
            memberMapper,
            simplePasswordVerificationMapper,
            tokenHashUtil,
            passwordEncoder,
            clock
        );
    }

    // ── 설정·변경 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("간편비밀번호 저장 성공 - BCrypt 해시로 저장하고 변경 토큰을 사용 완료 처리한다")
    void updateSimplePassword_success() {
        // given
        givenLockedMember();
        givenVerifiedChangeToken();

        when(passwordEncoder.encode(SIMPLE_PASSWORD)).thenReturn(STORED_HASH);
        when(memberMapper.updateSimplePassword(MEMBER_ID, STORED_HASH)).thenReturn(1);
        when(simplePasswordVerificationMapper.markAsUsed(VERIFICATION_ID)).thenReturn(1);

        // when
        simplePasswordService.updateSimplePassword(MEMBER_ID, updateRequest(SIMPLE_PASSWORD));

        // then
        // 원문이 아니라 해시가 저장돼야 한다.
        verify(memberMapper).updateSimplePassword(MEMBER_ID, STORED_HASH);
        // 토큰을 USED로 바꿔 재사용을 막는다.
        verify(simplePasswordVerificationMapper).markAsUsed(VERIFICATION_ID);
    }

    @Test
    @DisplayName("간편비밀번호 저장 실패 - 확인값이 다르면 토큰을 조회하지도 않는다")
    void updateSimplePassword_fail_whenConfirmationMismatch() {
        // given
        SimplePasswordUpdateRequest request =
            new SimplePasswordUpdateRequest(CHANGE_TOKEN, SIMPLE_PASSWORD, "999999");

        // when & then
        assertThatThrownBy(() -> simplePasswordService.updateSimplePassword(MEMBER_ID, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMPLE_PASSWORD_CONFIRMATION_MISMATCH);

        verify(memberMapper, never()).updateSimplePassword(anyLong(), any());
    }

    @Test
    @DisplayName("간편비밀번호 저장 실패 - 다른 회원에게 발급된 토큰이면 INVALID")
    void updateSimplePassword_fail_whenTokenBelongsToOtherMember() {
        // given
        givenLockedMember();

        SimplePasswordVerification otherMemberVerification = verification(
            999L, VerificationStatus.VERIFIED, NOW.plusMinutes(5));
        when(simplePasswordVerificationMapper.findByChangeTokenHashForUpdate(changeTokenHash()))
            .thenReturn(otherMemberVerification);

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.updateSimplePassword(MEMBER_ID, updateRequest(SIMPLE_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_INVALID);

        verify(memberMapper, never()).updateSimplePassword(anyLong(), any());
    }

    @Test
    @DisplayName("간편비밀번호 저장 실패 - 이미 사용한 토큰이면 ALREADY_USED")
    void updateSimplePassword_fail_whenTokenAlreadyUsed() {
        // given
        givenLockedMember();

        when(simplePasswordVerificationMapper.findByChangeTokenHashForUpdate(changeTokenHash()))
            .thenReturn(verification(MEMBER_ID, VerificationStatus.USED, NOW.plusMinutes(5)));

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.updateSimplePassword(MEMBER_ID, updateRequest(SIMPLE_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_ALREADY_USED);

        verify(memberMapper, never()).updateSimplePassword(anyLong(), any());
    }

    @Test
    @DisplayName("간편비밀번호 저장 실패 - 만료된 토큰이면 EXPIRED")
    void updateSimplePassword_fail_whenTokenExpired() {
        // given
        givenLockedMember();

        when(simplePasswordVerificationMapper.findByChangeTokenHashForUpdate(changeTokenHash()))
            .thenReturn(verification(MEMBER_ID, VerificationStatus.VERIFIED, NOW.minusSeconds(1)));

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.updateSimplePassword(MEMBER_ID, updateRequest(SIMPLE_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_EXPIRED);

        verify(memberMapper, never()).updateSimplePassword(anyLong(), any());
    }

    // ── 검증 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("간편비밀번호 검증 성공 - 일치하면 matched=true를 반환한다")
    void verifySimplePassword_success_whenMatched() {
        // given
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID)).thenReturn(memberWithSimplePassword(0, null));
        when(passwordEncoder.matches(SIMPLE_PASSWORD, STORED_HASH)).thenReturn(true);

        // when
        SimplePasswordVerifyResponse response =
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest());

        // then
        assertThat(response.matched()).isTrue();
        // 실패 이력이 없으면 굳이 초기화 쿼리를 날리지 않는다.
        verify(memberMapper, never()).resetSimplePasswordVerificationFailure(anyLong());
    }

    @Test
    @DisplayName("간편비밀번호 검증 성공 - 실패 이력이 있었다면 성공 시 초기화한다")
    void verifySimplePassword_success_resetsPreviousFailures() {
        // given
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID)).thenReturn(memberWithSimplePassword(3, null));
        when(passwordEncoder.matches(SIMPLE_PASSWORD, STORED_HASH)).thenReturn(true);
        when(memberMapper.resetSimplePasswordVerificationFailure(MEMBER_ID)).thenReturn(1);

        // when
        SimplePasswordVerifyResponse response =
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest());

        // then
        assertThat(response.matched()).isTrue();
        verify(memberMapper).resetSimplePasswordVerificationFailure(MEMBER_ID);
    }

    @Test
    @DisplayName("간편비밀번호 검증 실패 - 5회 미만이면 실패 횟수만 늘리고 matched=false를 반환한다")
    void verifySimplePassword_fail_increasesAttemptCount() {
        // given
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID)).thenReturn(memberWithSimplePassword(2, null));
        when(passwordEncoder.matches(SIMPLE_PASSWORD, STORED_HASH)).thenReturn(false);
        when(memberMapper.increaseSimplePasswordFailedAttemptCount(MEMBER_ID)).thenReturn(1);

        // when
        SimplePasswordVerifyResponse response =
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest());

        // then
        assertThat(response.matched()).isFalse();
        verify(memberMapper).increaseSimplePasswordFailedAttemptCount(MEMBER_ID);
        verify(memberMapper, never()).lockSimplePasswordVerification(anyLong(), any());
    }

    @Test
    @DisplayName("간편비밀번호 검증 실패 - 5회째 실패면 5분간 잠그고 예외를 던진다")
    void verifySimplePassword_fail_locksAfterFifthAttempt() {
        // given
        // 이미 4회 실패한 상태에서 한 번 더 틀리면 5회가 되어 잠긴다.
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID)).thenReturn(memberWithSimplePassword(4, null));
        when(passwordEncoder.matches(SIMPLE_PASSWORD, STORED_HASH)).thenReturn(false);
        when(memberMapper.lockSimplePasswordVerification(eq(MEMBER_ID), any())).thenReturn(1);

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);

        verify(memberMapper).lockSimplePasswordVerification(MEMBER_ID, NOW.plusMinutes(5));
    }

    @Test
    @DisplayName("간편비밀번호 검증 실패 - 잠금 시간이 남아 있으면 비교조차 하지 않는다")
    void verifySimplePassword_fail_whenStillLocked() {
        // given
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID))
            .thenReturn(memberWithSimplePassword(5, NOW.plusMinutes(3)));

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);

        // 잠긴 동안에는 해시 비교 자체를 하지 않아야 무차별 대입을 실제로 막을 수 있다.
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 검증 - 잠금이 만료됐으면 실패 횟수를 초기화하고 다시 시도할 수 있다")
    void verifySimplePassword_resetsWhenLockExpired() {
        // given
        // 잠금 만료시각이 지났고 카운트는 5로 남아 있는 상태.
        givenLockedMember();
        when(memberMapper.findById(MEMBER_ID))
            .thenReturn(memberWithSimplePassword(5, NOW.minusMinutes(1)));
        when(memberMapper.resetSimplePasswordVerificationFailure(MEMBER_ID)).thenReturn(1);
        when(passwordEncoder.matches(SIMPLE_PASSWORD, STORED_HASH)).thenReturn(false);
        when(memberMapper.increaseSimplePasswordFailedAttemptCount(MEMBER_ID)).thenReturn(1);

        // when
        SimplePasswordVerifyResponse response =
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest());

        // then
        assertThat(response.matched()).isFalse();
        verify(memberMapper).resetSimplePasswordVerificationFailure(MEMBER_ID);
        // 카운트가 0부터 다시 세지므로 곧바로 재잠금되지 않는다.
        verify(memberMapper).increaseSimplePasswordFailedAttemptCount(MEMBER_ID);
        verify(memberMapper, never()).lockSimplePasswordVerification(anyLong(), any());
    }

    @Test
    @DisplayName("간편비밀번호 검증 실패 - 아직 설정하지 않았으면 SIMPLE_PASSWORD_NOT_SET")
    void verifySimplePassword_fail_whenNotSet() {
        // given
        givenLockedMember();
        Member member = member();
        setField(member, "simplePasswordHash", null);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);

        // when & then
        assertThatThrownBy(() ->
            simplePasswordService.verifySimplePassword(MEMBER_ID, verifyRequest()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMPLE_PASSWORD_NOT_SET);
    }

    @Test
    @DisplayName("간편비밀번호 검증 실패 - 인증 정보가 없으면 ACCESS_TOKEN_INVALID")
    void verifySimplePassword_fail_whenMemberIdNull() {
        assertThatThrownBy(() ->
            simplePasswordService.verifySimplePassword(null, verifyRequest()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_INVALID);

        verify(memberMapper, never()).lockActiveMemberById(anyLong());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private void givenLockedMember() {
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
    }

    private void givenVerifiedChangeToken() {
        when(simplePasswordVerificationMapper.findByChangeTokenHashForUpdate(changeTokenHash()))
            .thenReturn(verification(MEMBER_ID, VerificationStatus.VERIFIED, NOW.plusMinutes(5)));
    }

    private String changeTokenHash() {
        return tokenHashUtil.sha256(CHANGE_TOKEN);
    }

    private SimplePasswordUpdateRequest updateRequest(String simplePassword) {
        return new SimplePasswordUpdateRequest(CHANGE_TOKEN, simplePassword, simplePassword);
    }

    private SimplePasswordVerifyRequest verifyRequest() {
        return new SimplePasswordVerifyRequest(SIMPLE_PASSWORD);
    }

    private SimplePasswordVerification verification(
        Long memberId,
        String status,
        LocalDateTime changeTokenExpiresAt
    ) {
        SimplePasswordVerification verification =
            SimplePasswordVerification.createPending(memberId, "code-hash", NOW.plusMinutes(5));
        setField(verification, "simplePasswordVerificationId", VERIFICATION_ID);
        setField(verification, "verificationStatus", status);
        setField(verification, "changeTokenExpiresAt", changeTokenExpiresAt);
        return verification;
    }

    private Member member() {
        Member member = Member.createSignupMember(
            "user@example.com", "$2a$10$login-hash", "김활성", "별명A");
        setField(member, "memberId", MEMBER_ID);
        setField(member, "simplePasswordHash", STORED_HASH);
        return member;
    }

    private Member memberWithSimplePassword(int failedAttemptCount, LocalDateTime lockedUntil) {
        Member member = member();
        setField(member, "simplePasswordFailedAttemptCount", failedAttemptCount);
        setField(member, "simplePasswordLockedUntil", lockedUntil);
        return member;
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
