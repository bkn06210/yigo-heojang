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

    /**
     * 비밀번호 재설정 인증 코드를 발송한다.
     *
     * 회원가입 인증과 비밀번호 재설정 인증은 둘 다 이메일 인증 코드를 보내지만,
     * 사용자가 받는 메일 제목과 안내 문구가 다르므로 메서드를 분리한다.
     */
    void sendPasswordResetVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    );

    /** 간편비밀번호 설정·변경 권한을 확인하기 위한 이메일 인증 코드를 발송한다. */
    void sendSimplePasswordVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    );
}