package com.wallet.auth.controller;

import java.time.Duration;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import com.wallet.auth.dto.PasswordResetCodeRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyRequest;
import com.wallet.auth.dto.PasswordResetCodeVerifyResponse;
import com.wallet.auth.dto.PasswordResetRequest;
import com.wallet.auth.dto.SignupEmailVerificationConfirmRequest;
import com.wallet.auth.dto.SignupEmailVerificationConfirmResponse;
import com.wallet.auth.dto.SignupEmailVerificationRequest;
import com.wallet.auth.dto.SignupEmailVerificationResponse;
import com.wallet.auth.dto.SignupRequest;
import com.wallet.auth.dto.SignupResponse;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.service.AuthService;
import com.wallet.auth.service.PasswordResetService;
import com.wallet.auth.service.SignupEmailVerificationService;
import com.wallet.common.ApiResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final SignupEmailVerificationService signupEmailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
        @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/signup/email-verifications")
    public ResponseEntity<ApiResponse<SignupEmailVerificationResponse>> sendSignupEmailVerificationCode(
        @Valid @RequestBody SignupEmailVerificationRequest request
    ) {
        SignupEmailVerificationResponse response =
            signupEmailVerificationService.sendVerificationCode(request);

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success("인증 코드가 발송되었습니다.", response));
    }

    @PostMapping("/signup/email-verifications/verify")
    public ResponseEntity<ApiResponse<SignupEmailVerificationConfirmResponse>> verifySignupEmailVerificationCode(
        @Valid @RequestBody SignupEmailVerificationConfirmRequest request
    ) {
        SignupEmailVerificationConfirmResponse response =
            signupEmailVerificationService.verifyCode(request);

        return ResponseEntity
            .ok(ApiResponse.success("이메일 인증이 완료되었습니다.", response));
    }

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

    @PostMapping("/password/reset-link")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(
        @Valid @RequestBody PasswordResetCodeRequest request
    ) {
        passwordResetService.sendResetCode(request);

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success("입력한 이메일로 비밀번호 초기화 안내를 전송했습니다.", null));
    }

    @PostMapping("/password/verify-code")
    public ResponseEntity<ApiResponse<PasswordResetCodeVerifyResponse>> verifyPasswordResetCode(
        @Valid @RequestBody PasswordResetCodeVerifyRequest request
    ) {
        PasswordResetCodeVerifyResponse response = passwordResetService.verifyResetCode(request);

        return ResponseEntity.ok(
            ApiResponse.success("인증 번호 검증에 성공했습니다.", response)
        );
    }

    @PostMapping("/password/resets")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody PasswordResetRequest request
    ) {
        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
            ApiResponse.success("비밀번호 재설정이 완료되었습니다.", null)
        );
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