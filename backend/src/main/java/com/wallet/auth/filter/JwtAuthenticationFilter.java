package com.wallet.auth.filter;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;

@RequiredArgsConstructor
@Component("jwtAuthenticationFilter")
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // CORS preflight 요청은 브라우저의 사전 확인용이므로, Access Token이 없어도 인증 필터를 통과시켜야 한다.
    private static final String OPTIONS_METHOD = "OPTIONS";

    // Access Token 없이 접근 가능한 API 경로 목록.
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/logout",
        "/api/auth/token",
        "/api/auth/signup",
        "/api/auth/signup/email-verifications",
        "/api/auth/signup/email-verifications/verify",
        "/api/auth/password/reset-link",
        "/api/auth/password/verify-code",
        "/api/auth/password/resets",
        "/api/terms"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origin:http://localhost:5173}")
    private String frontendOrigin;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = extractAccessToken(request);

        if (accessToken == null) {
            writeErrorResponse(request, response, ErrorCode.ACCESS_TOKEN_INVALID);
            return;
        }

        try {
            jwtTokenProvider.validateAccessTokenOrThrow(accessToken);

            Long memberId = jwtTokenProvider.getMemberIdFromAccessToken(accessToken);
            request.setAttribute(AUTHENTICATED_MEMBER_ID, memberId);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            writeErrorResponse(request, response, ErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            writeErrorResponse(request, response, ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    // 현재 요청이 인증 없이 접근 가능한 요청인지 확인한다.
    private boolean isPublicRequest(HttpServletRequest request) {
        return isPreflightRequest(request) || isPublicPath(request);
    }

    // CORS preflight 요청인지 확인한다.
    private boolean isPreflightRequest(HttpServletRequest request) {
        return OPTIONS_METHOD.equalsIgnoreCase(request.getMethod());
    }

    // 요청 URI가 공개 API 경로에 해당하는지 확인한다.
    private boolean isPublicPath(HttpServletRequest request) {
        String requestPath = getRequestPath(request);
        return PUBLIC_PATHS.contains(requestPath);
    }


    // context path를 제외한 실제 요청 경로를 구한다.
    private String getRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }

        return requestUri.substring(contextPath.length());
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

    private void writeErrorResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        ErrorCode errorCode
    ) throws IOException {
        String origin = request.getHeader("Origin");
        if (frontendOrigin != null && frontendOrigin.equals(origin)) {
            response.setHeader("Access-Control-Allow-Origin", frontendOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(errorCode);
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(responseBody);
    }
}
