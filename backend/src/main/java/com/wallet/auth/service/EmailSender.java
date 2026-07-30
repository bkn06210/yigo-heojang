package com.wallet.auth.service;

public interface EmailSender {
    /**
     * 회원가입 이메일 인증 코드를 발송한다.
     *
     * @param toEmail          인증 코드를 받을 이메일
     * @param verificationCode 사용자에게 전달할 인증 코드 원문
     * @param expiresInMinutes 인증 코드 만료 시간(분)
     */
    void sendSignupVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    );
}