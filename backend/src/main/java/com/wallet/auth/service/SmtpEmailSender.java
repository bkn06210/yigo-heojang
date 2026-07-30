package com.wallet.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailSender implements EmailSender {

    private static final String SIGNUP_VERIFICATION_SUBJECT =
        "[이고허장] 회원가입 이메일 인증 코드";

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailSender(
        JavaMailSender mailSender,
        @Value("${mail.from}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendSignupVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(SIGNUP_VERIFICATION_SUBJECT);
        message.setText(createSignupVerificationText(verificationCode, expiresInMinutes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            /*
             * 해당 commit에서는 Spring의 메일 예외를 그대로 RuntimeException으로 감싼다.
             * 실제 ErrorCode 변환은 다음 커밋의 Service 계층에서 EMAIL_SEND_FAILED로 처리한다.
             */
            throw new IllegalStateException("회원가입 인증 메일 발송에 실패했습니다.", e);
        }
    }

    private String createSignupVerificationText(
        String verificationCode,
        long expiresInMinutes
    ) {
        return String.join(System.lineSeparator(),
            "안녕하세요. 이고허장입니다.",
            "",
            "회원가입을 계속하려면 아래 인증 코드를 입력해 주세요.",
            "",
            "인증 코드: " + verificationCode,
            "유효 시간: " + expiresInMinutes + "분",
            "",
            "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );
    }
}