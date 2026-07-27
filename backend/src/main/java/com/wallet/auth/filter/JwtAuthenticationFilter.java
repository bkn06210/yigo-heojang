package com.wallet.auth.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wallet.auth.jwt.JwtTokenProvider;

@RequiredArgsConstructor
@Component("jwtAuthenticationFilter")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = extractAccessToken(request);

        // TODO: 다음 커밋에서 인증 제외 경로를 먼저 판단한다.
        // TODO: 이후 커밋에서 Access Token 검증 및 memberId 저장을 추가한다.
        // TODO: 이후 커밋에서 인증 실패 JSON 응답 처리를 추가한다.

        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!hasBearerToken(authorizationHeader)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private boolean hasBearerToken(String authorizationHeader) {
        return authorizationHeader != null
            && authorizationHeader.startsWith(BEARER_PREFIX)
            && authorizationHeader.length() > BEARER_PREFIX.length();
    }
}