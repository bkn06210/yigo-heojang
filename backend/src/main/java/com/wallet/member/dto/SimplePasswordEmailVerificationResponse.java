package com.wallet.member.dto;

// 인증 코드 발송 결과. 어느 주소로 보냈는지 확인할 수 있도록 마스킹된 이메일과
// 남은 유효시간(초)만 내려준다.
public record SimplePasswordEmailVerificationResponse(
    String email,
    long expiresIn
) {
}
