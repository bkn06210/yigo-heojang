package com.wallet.member.dto;

// 이메일 인증 성공 시 발급되는 일회용 변경 토큰. 프론트는 이 값을 간편비밀번호
// 저장 요청에 그대로 실어 보낸다.
public record SimplePasswordEmailVerificationVerifyResponse(
    String simplePasswordChangeToken,
    long expiresIn
) {
}
