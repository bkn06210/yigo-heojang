package com.wallet.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.domain.MemberTermAgreement;
import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.domain.TermScope;
import com.wallet.auth.dto.LoginMemberResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.dto.SignupRequest;
import com.wallet.auth.dto.SignupResponse;
import com.wallet.auth.dto.TermAgreementRequest;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.auth.mapper.TermAgreementMapper;
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
    private TermAgreementMapper termAgreementMapper;
    private SignupEmailVerificationService signupEmailVerificationService;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        passwordEncoder = new BCryptPasswordEncoder();
        refreshTokenService = mock(RefreshTokenService.class);
        termAgreementMapper = mock(TermAgreementMapper.class);
        signupEmailVerificationService = mock(SignupEmailVerificationService.class);

        authService = new AuthService(
            memberMapper,
            jwtTokenProvider,
            passwordEncoder,
            refreshTokenService,
            termAgreementMapper,
            signupEmailVerificationService
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

    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 Refresh Token이면 새로운 Access Token을 반환한다")
    void reissueAccessToken_success() {
        // given
        String refreshToken = "refresh.token.value";
        Long memberId = 1L;

        RefreshToken savedToken = new RefreshToken();
        ReflectionTestUtils.setField(savedToken, "memberId", memberId);

        Member member = createMember(
            memberId,
            "user@example.com",
            passwordEncoder.encode("1234"),
            "이재혁",
            "ACTIVE"
        );

        when(jwtTokenProvider.validateRefreshToken(refreshToken))
            .thenReturn(true);

        when(jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken))
            .thenReturn(memberId);

        when(refreshTokenService.findValidToken(refreshToken))
            .thenReturn(savedToken);

        when(memberMapper.findById(memberId))
            .thenReturn(member);

        when(jwtTokenProvider.createAccessToken(member))
            .thenReturn("new.access.token");

        when(jwtTokenProvider.getAccessTokenValidityInSeconds())
            .thenReturn(1800L);

        // when
        TokenResponse response = authService.reissueAccessToken(refreshToken);

        // then
        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);

        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(jwtTokenProvider).getMemberIdFromRefreshToken(refreshToken);
        verify(refreshTokenService).findValidToken(refreshToken);
        verify(memberMapper).findById(memberId);
        verify(jwtTokenProvider).createAccessToken(member);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - DB에 유효한 Refresh Token이 없으면 REFRESH_TOKEN_FAILED 예외가 발생한다")
    void reissueAccessToken_fail_whenTokenNotFoundInDatabase() {
        // given
        String refreshToken = "refresh.token.value";

        when(jwtTokenProvider.validateRefreshToken(refreshToken))
            .thenReturn(true);

        when(jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken))
            .thenReturn(1L);

        when(refreshTokenService.findValidToken(refreshToken))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.reissueAccessToken(refreshToken)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.REFRESH_TOKEN_FAILED);

        verify(refreshTokenService).findValidToken(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 성공 - 유효한 Refresh Token이면 토큰을 폐기한다")
    void logout_success() {
        // given
        String refreshToken = "refresh.token.value";

        when(jwtTokenProvider.validateRefreshToken(refreshToken))
            .thenReturn(true);

        // when
        authService.logout(refreshToken);

        // then
        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(refreshTokenService).revokeByToken(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 - Refresh Token이 없으면 아무 작업도 하지 않는다")
    void logout_whenRefreshTokenIsNull() {
        // when
        authService.logout(null);

        // then
        verify(refreshTokenService, never()).revokeByToken(any());
    }

    @Test
    @DisplayName("로그아웃 - 유효하지 않은 Refresh Token이면 DB 폐기를 수행하지 않는다")
    void logout_whenInvalidRefreshToken() {
        // given
        String refreshToken = "invalid.refresh.token";

        when(jwtTokenProvider.validateRefreshToken(refreshToken))
            .thenReturn(false);

        // when
        authService.logout(refreshToken);

        // then
        verify(refreshTokenService, never()).revokeByToken(any());
    }

    @Test
    @DisplayName("회원가입 성공 - 회원을 저장하고 약관 동의 이력을 저장한 뒤 가입 정보를 반환한다")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(11L, true),
                new TermAgreementRequest(12L, false)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(signupEmailVerificationService.validateSignupVerificationToken(
            "user@example.com",
            "signup-token"
        )).thenReturn(1L);

        when(termAgreementMapper.countActiveTermVersionsByIds(List.of(10L, 11L, 12L), TermScope.SIGNUP))
            .thenReturn(3);

        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.SIGNUP))
            .thenReturn(List.of(10L, 11L));

        when(memberMapper.insertMember(any(Member.class)))
            .thenAnswer(invocation -> {
                Member member = invocation.getArgument(0);
                ReflectionTestUtils.setField(member, "memberId", 1L);
                return 1;
            });

        Member savedMember = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "이재혁",
            "ACTIVE"
        );
        ReflectionTestUtils.setField(savedMember, "nickname", "든든한얼룩말0001");
        ReflectionTestUtils.setField(savedMember, "createdAt", LocalDateTime.of(2026, 7, 29, 10, 0));

        when(memberMapper.findById(1L))
            .thenReturn(savedMember);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("이재혁");
        assertThat(response.memberStatus()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 7, 29, 10, 0));


        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).insertMember(memberCaptor.capture());

        Member insertedMember = memberCaptor.getValue();

        assertThat(insertedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(insertedMember.getName()).isEqualTo("이재혁");
        assertThat(insertedMember.getMemberStatus()).isEqualTo("ACTIVE");
        assertThat(insertedMember.getNickname()).isNotBlank();


        assertThat(insertedMember.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", insertedMember.getPassword())).isTrue();

        ArgumentCaptor<List<MemberTermAgreement>> agreementsCaptor = ArgumentCaptor.forClass(List.class);
        verify(termAgreementMapper).insertMemberTermAgreements(agreementsCaptor.capture());

        List<MemberTermAgreement> agreements = agreementsCaptor.getValue();

        assertThat(agreements).hasSize(3);
        assertThat(agreements)
            .extracting(MemberTermAgreement::getMemberId)
            .containsOnly(1L);

        assertThat(agreements)
            .extracting(MemberTermAgreement::getTermsVersionId)
            .containsExactly(10L, 11L, 12L);

        assertThat(agreements)
            .extracting(MemberTermAgreement::getAgreed)
            .containsExactly(true, true, false);

        verify(memberMapper).existsByEmail("user@example.com");
        verify(signupEmailVerificationService).markAsUsed(1L);
        verify(termAgreementMapper).countActiveTermVersionsByIds(List.of(10L, 11L, 12L), TermScope.SIGNUP);
        verify(termAgreementMapper).findActiveRequiredTermVersionIds(TermScope.SIGNUP);
        verify(memberMapper).findById(1L);

    }

    @Test
    @DisplayName("회원가입 실패 - 이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외가 발생한다")
    void signup_fail_whenEmailAlreadyExists() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(11L, true)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(true);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.signup(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(memberMapper).existsByEmail("user@example.com");

        verify(termAgreementMapper, never()).countActiveTermVersionsByIds(any(), any());
        verify(termAgreementMapper, never()).findActiveRequiredTermVersionIds(TermScope.SIGNUP);
        verify(memberMapper, never()).insertMember(any());
        verify(termAgreementMapper, never()).insertMemberTermAgreements(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 요청한 약관 버전 중 유효하지 않은 약관이 있으면 INPUT_INVALID 예외가 발생한다")
    void signup_fail_whenTermVersionInvalid() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(999L, true)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(termAgreementMapper.countActiveTermVersionsByIds(anyList(), any()))
            .thenReturn(1);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.signup(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.INPUT_INVALID);

        verify(memberMapper).existsByEmail("user@example.com");
        verify(termAgreementMapper).countActiveTermVersionsByIds(anyList(), any());
        verify(termAgreementMapper, never()).findActiveRequiredTermVersionIds(any());
        verify(memberMapper, never()).insertMember(any());
        verify(termAgreementMapper, never()).insertMemberTermAgreements(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 필수 약관에 동의하지 않으면 INPUT_INVALID 예외가 발생한다")
    void signup_fail_whenRequiredTermNotAgreed() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(11L, false),
                new TermAgreementRequest(12L, false)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(termAgreementMapper.countActiveTermVersionsByIds(List.of(10L, 11L, 12L), TermScope.SIGNUP))
            .thenReturn(3);

        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.SIGNUP))
            .thenReturn(List.of(10L, 11L));

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.signup(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.INPUT_INVALID);

        verify(memberMapper).existsByEmail("user@example.com");
        verify(termAgreementMapper).countActiveTermVersionsByIds(List.of(10L, 11L, 12L), TermScope.SIGNUP);
        verify(termAgreementMapper).findActiveRequiredTermVersionIds(TermScope.SIGNUP);
        verify(memberMapper, never()).insertMember(any());
        verify(termAgreementMapper, never()).insertMemberTermAgreements(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 같은 약관 버전이 중복으로 들어오면 INPUT_INVALID 예외가 발생한다")
    void signup_fail_whenTermVersionDuplicated() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(10L, true)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.signup(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.INPUT_INVALID);

        verify(memberMapper).existsByEmail("user@example.com");

        verify(termAgreementMapper, never()).countActiveTermVersionsByIds(any(), any());
        verify(termAgreementMapper, never()).findActiveRequiredTermVersionIds(any());
        verify(memberMapper, never()).insertMember(any());
        verify(termAgreementMapper, never()).insertMemberTermAgreements(any());
    }

    @Test
    @DisplayName("회원가입 성공 - 선택 약관은 동의하지 않아도 가입할 수 있다")
    void signup_success_whenOptionalTermNotAgreed() {
        // given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123",
            "이재혁",
            "signup-token",
            List.of(
                new TermAgreementRequest(10L, true),
                new TermAgreementRequest(11L, true),
                new TermAgreementRequest(12L, false)
            )
        );

        when(memberMapper.existsByEmail("user@example.com"))
            .thenReturn(false);

        when(termAgreementMapper.countActiveTermVersionsByIds(List.of(10L, 11L, 12L), TermScope.SIGNUP))
            .thenReturn(3);
        
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.SIGNUP))
            .thenReturn(List.of(10L, 11L));

        when(memberMapper.insertMember(any(Member.class)))
            .thenAnswer(invocation -> {
                Member member = invocation.getArgument(0);
                ReflectionTestUtils.setField(member, "memberId", 1L);
                return 1;
            });

        Member savedMember = createMember(
            1L,
            "user@example.com",
            passwordEncoder.encode("password123"),
            "이재혁",
            "ACTIVE"
        );
        ReflectionTestUtils.setField(savedMember, "nickname", "알뜰한카드1234");
        ReflectionTestUtils.setField(savedMember, "createdAt", LocalDateTime.of(2026, 7, 29, 10, 0));

        when(memberMapper.findById(1L))
            .thenReturn(savedMember);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.memberId()).isEqualTo(1L);

        ArgumentCaptor<List<MemberTermAgreement>> agreementsCaptor = ArgumentCaptor.forClass(List.class);
        verify(termAgreementMapper).insertMemberTermAgreements(agreementsCaptor.capture());

        assertThat(agreementsCaptor.getValue())
            .extracting(MemberTermAgreement::getAgreed)
            .containsExactly(true, true, false);
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