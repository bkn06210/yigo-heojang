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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.dto.LoginMemberResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

class AuthServiceTest {

    private MemberMapper memberMapper;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        passwordEncoder = new BCryptPasswordEncoder();
        refreshTokenService = mock(RefreshTokenService.class);

        authService = new AuthService(
            memberMapper,
            jwtTokenProvider,
            passwordEncoder,
            refreshTokenService
        );
    }

    @Test
    @DisplayName("로그인 성공 - 정상 회원이면 Access Token과 회원 정보를 반환한다")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "1234");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("1234"),
            "이재혁",
            "ACTIVE"
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        when(jwtTokenProvider.createAccessToken(member))
            .thenReturn("access.token.value");

        when(jwtTokenProvider.createRefreshToken(member.getMemberId()))
            .thenReturn("refresh.token.value");

        when(jwtTokenProvider.getAccessTokenValidityInSeconds())
            .thenReturn(1800L);

        when(jwtTokenProvider.getRefreshTokenValidityInSeconds())
            .thenReturn(1209600L);

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access.token.value");
        assertThat(result.refreshToken()).isEqualTo("refresh.token.value");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(1800L);

        LoginMemberResponse loginMember = result.member();
        assertThat(loginMember.memberId()).isEqualTo(1L);
        assertThat(loginMember.email()).isEqualTo("user@example.com");
        assertThat(loginMember.name()).isEqualTo("이재혁");

        verify(memberMapper).findByEmail("user@example.com");
        verify(jwtTokenProvider).createAccessToken(member);
        verify(jwtTokenProvider).createRefreshToken(member.getMemberId());

        verify(refreshTokenService).replace(
            eq(member.getMemberId()),
            eq("refresh.token.value"),
            any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 LOGIN_CREDENTIAL_MISMATCH 예외가 발생한다")
    void login_fail_whenEmailNotFound() {
        // given
        LoginRequest request = new LoginRequest("unknown@example.com", "1234");

        when(memberMapper.findByEmail("unknown@example.com"))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);

        verify(memberMapper).findByEmail("unknown@example.com");

        verify(refreshTokenService, never()).replace(
            any(),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 일치하지 않으면 LOGIN_CREDENTIAL_MISMATCH 예외가 발생한다")
    void login_fail_whenPasswordMismatch() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("1234"),
            "이재혁",
            "ACTIVE"
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);

        verify(memberMapper).findByEmail("user@example.com");

        verify(refreshTokenService, never()).replace(
            any(),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("로그인 실패 - 탈퇴한 회원이면 MEMBER_WITHDRAWN 예외가 발생한다")
    void login_fail_whenMemberWithdrawn() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "1234");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("1234"),
            "이재혁",
            "WITHDRAWN"
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_WITHDRAWN);

        verify(memberMapper).findByEmail("user@example.com");

        verify(refreshTokenService, never()).replace(
            any(),
            any(),
            any()
        );
    }

    @Test
    @DisplayName("로그인 실패 - ACTIVE가 아닌 회원이면 MEMBER_SUSPENDED 예외가 발생한다")
    void login_fail_whenMemberNotActive() {
        // given
        LoginRequest request = new LoginRequest("user@example.com", "1234");

        Member member = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("1234"),
            "이재혁",
            "SUSPENDED"
        );

        when(memberMapper.findByEmail("user@example.com"))
            .thenReturn(member);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_SUSPENDED);

        verify(memberMapper).findByEmail("user@example.com");

        verify(refreshTokenService, never()).replace(
            any(),
            any(),
            any()
        );
    }

    private Member createMember(
        Long memberId,
        String email,
        String password,
        String name,
        String memberStatus
    ) {
        Member member = new Member();

        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "password", password);
        ReflectionTestUtils.setField(member, "name", name);
        ReflectionTestUtils.setField(member, "memberStatus", memberStatus);
        return member;
    }
}