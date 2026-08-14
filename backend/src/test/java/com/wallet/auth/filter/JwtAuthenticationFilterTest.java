package com.wallet.auth.filter;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.common.ErrorCode;
import com.wallet.member.mapper.MemberMapper;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter =
        new JwtAuthenticationFilter(jwtTokenProvider, new ObjectMapper(), memberMapper);

    @Test
    @DisplayName("공개 경로 요청 - Access Token 없이 필터를 통과한다")
    void doFilter_success_whenPublicPath() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();

        verify(jwtTokenProvider, never()).validateAccessTokenOrThrow(anyString());
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("OPTIONS 요청 - 인증 없이 200으로 응답하고 필터 체인으로 넘기지 않는다")
    void doFilter_success_whenOptionsRequest() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/members/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        // CORS preflight는 필터가 직접 200으로 끝내고 체인을 태우지 않는다.
        // 컨트롤러까지 갈 필요가 없는, 브라우저의 사전 확인용 요청이기 때문이다.
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider, never()).validateAccessTokenOrThrow(anyString());
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("Access Token 누락 - 보호 경로 요청이면 ACCESS_TOKEN_INVALID를 반환한다")
    void doFilter_fail_whenAccessTokenMissing() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_TOKEN_INVALID.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains("\"success\":false")
            .contains("\"code\":\"ACCESS_TOKEN_INVALID\"")
            .contains("\"message\":\"" + ErrorCode.ACCESS_TOKEN_INVALID.getMessage() + "\"");

        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider, never()).validateAccessTokenOrThrow(anyString());
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("Bearer 형식 오류 - ACCESS_TOKEN_INVALID를 반환한다")
    void doFilter_fail_whenAuthorizationHeaderIsNotBearerType() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_TOKEN_INVALID.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains("\"success\":false")
            .contains("\"code\":\"ACCESS_TOKEN_INVALID\"");

        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider, never()).validateAccessTokenOrThrow(anyString());
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("잘못된 Access Token - ACCESS_TOKEN_INVALID를 반환한다")
    void doFilter_fail_whenAccessTokenInvalid() throws Exception {
        // given
        String accessToken = "invalid.access.token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doThrow(new MalformedJwtException("잘못된 JWT입니다."))
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_TOKEN_INVALID.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains("\"success\":false")
            .contains("\"code\":\"ACCESS_TOKEN_INVALID\"");

        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider).validateAccessTokenOrThrow(accessToken);
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("만료된 Access Token - ACCESS_TOKEN_EXPIRED를 반환한다")
    void doFilter_fail_whenAccessTokenExpired() throws Exception {
        // given
        String accessToken = "expired.access.token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doThrow(new ExpiredJwtException(null, null, "만료된 JWT입니다."))
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_TOKEN_EXPIRED.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains("\"success\":false")
            .contains("\"code\":\"ACCESS_TOKEN_EXPIRED\"")
            .contains("\"message\":\"" + ErrorCode.ACCESS_TOKEN_EXPIRED.getMessage() + "\"");

        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider).validateAccessTokenOrThrow(accessToken);
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }

    @Test
    @DisplayName("정상 Access Token - memberId를 request attribute에 저장하고 필터를 통과한다")
    void doFilter_success_whenAccessTokenValid() throws Exception {
        // given
        String accessToken = "valid.access.token";
        Long memberId = 1L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doNothing()
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        when(jwtTokenProvider.getMemberIdFromAccessToken(accessToken))
            .thenReturn(memberId);

        when(memberMapper.findStatusById(memberId))
            .thenReturn("ACTIVE");

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(request.getAttribute(AUTHENTICATED_MEMBER_ID))
            .isEqualTo(memberId);

        verify(jwtTokenProvider).validateAccessTokenOrThrow(accessToken);
        verify(jwtTokenProvider).getMemberIdFromAccessToken(accessToken);
    }

    @Test
    @DisplayName("탈퇴한 회원의 유효한 Access Token - MEMBER_WITHDRAWN을 반환하고 필터 체인을 통과시키지 않는다")
    void doFilter_fail_whenMemberWithdrawn() throws Exception {
        // given
        String accessToken = "valid.access.token";
        Long memberId = 3L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doNothing()
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        when(jwtTokenProvider.getMemberIdFromAccessToken(accessToken))
            .thenReturn(memberId);

        when(memberMapper.findStatusById(memberId))
            .thenReturn("WITHDRAWN");

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus())
            .isEqualTo(ErrorCode.MEMBER_WITHDRAWN.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains(ErrorCode.MEMBER_WITHDRAWN.getCode());
        assertThat(filterChain.getRequest()).isNull();
        assertThat(request.getAttribute(AUTHENTICATED_MEMBER_ID)).isNull();
    }

    @Test
    @DisplayName("정지된 회원의 유효한 Access Token - MEMBER_SUSPENDED를 반환한다")
    void doFilter_fail_whenMemberSuspended() throws Exception {
        // given
        String accessToken = "valid.access.token";
        Long memberId = 2L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doNothing()
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        when(jwtTokenProvider.getMemberIdFromAccessToken(accessToken))
            .thenReturn(memberId);

        when(memberMapper.findStatusById(memberId))
            .thenReturn("SUSPENDED");

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus())
            .isEqualTo(ErrorCode.MEMBER_SUSPENDED.getStatus().value());
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 회원의 Access Token - MEMBER_NOT_FOUND를 반환한다")
    void doFilter_fail_whenMemberNotFound() throws Exception {
        // given
        String accessToken = "valid.access.token";
        Long memberId = 999L;

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doNothing()
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        when(jwtTokenProvider.getMemberIdFromAccessToken(accessToken))
            .thenReturn(memberId);

        when(memberMapper.findStatusById(memberId))
            .thenReturn(null);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus())
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND.getStatus().value());
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("memberId 추출 실패 - ACCESS_TOKEN_INVALID를 반환한다")
    void doFilter_fail_whenMemberIdExtractionFails() throws Exception {
        // given
        String accessToken = "token.with.invalid.subject";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        doNothing()
            .when(jwtTokenProvider)
            .validateAccessTokenOrThrow(accessToken);

        when(jwtTokenProvider.getMemberIdFromAccessToken(accessToken))
            .thenThrow(new IllegalArgumentException("memberId를 추출할 수 없습니다."));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(ErrorCode.ACCESS_TOKEN_INVALID.getStatus().value());
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
            .contains("\"success\":false")
            .contains("\"code\":\"ACCESS_TOKEN_INVALID\"");

        assertThat(filterChain.getRequest()).isNull();

        verify(jwtTokenProvider).validateAccessTokenOrThrow(accessToken);
        verify(jwtTokenProvider).getMemberIdFromAccessToken(accessToken);
    }

    @Test
    @DisplayName("로그아웃 요청 - Access Token 없이 필터를 통과한다")
    void doFilter_success_whenLogoutPath() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();

        verify(jwtTokenProvider, never()).validateAccessTokenOrThrow(anyString());
        verify(jwtTokenProvider, never()).getMemberIdFromAccessToken(anyString());
    }
}