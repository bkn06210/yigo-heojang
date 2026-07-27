package com.wallet.auth.controller;

import java.time.Duration;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResponse;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.service.AuthService;
import com.wallet.common.ApiResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(
            result.refreshToken(),
            result.refreshTokenExpiresIn()
        );

        LoginResponse response = new LoginResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresIn(),
            result.member()
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> reissueAccessToken(
        @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        TokenResponse response = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok(
            ApiResponse.success("토큰 재발급에 성공했습니다.", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = deleteRefreshTokenCookie();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
            .body(ApiResponse.success("로그아웃에 성공했습니다.", null));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .sameSite("Lax")
            .build();
    }

    private ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
    }


}