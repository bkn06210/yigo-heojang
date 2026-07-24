package com.wallet.auth.controller;

import java.time.Duration;

import javax.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResponse;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.service.AuthService;
import com.wallet.common.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofSeconds(result.refreshTokenExpiresIn()))
            .sameSite("Lax")
            .build();

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
}