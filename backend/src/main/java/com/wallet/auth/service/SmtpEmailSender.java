package com.wallet.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailSender implements EmailSender {
    // 메일 본문이 "두리입니다"로 나가고 있어 제목의 서비스명도 두리로 맞춘다.
    private static final String SIGNUP_VERIFICATION_SUBJECT =
        "[두리] 회원가입 이메일 인증 코드";

    private static final String PASSWORD_RESET_VERIFICATION_SUBJECT =
        "[두리] 비밀번호 재설정 인증 코드";

    private static final String SIMPLE_PASSWORD_VERIFICATION_SUBJECT =
        "[두리] 간편비밀번호 변경 인증 코드";

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

    @Override
    public void sendPasswordResetVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(PASSWORD_RESET_VERIFICATION_SUBJECT);
        message.setText(createPasswordResetVerificationText(verificationCode, expiresInMinutes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new IllegalStateException("비밀번호 재설정 인증 메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public void sendSimplePasswordVerificationCode(
        String toEmail,
        String verificationCode,
        long expiresInMinutes
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(SIMPLE_PASSWORD_VERIFICATION_SUBJECT);
        message.setText(createSimplePasswordVerificationText(verificationCode, expiresInMinutes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new IllegalStateException("간편비밀번호 변경 인증 메일 발송에 실패했습니다.", e);
        }
    }

    private String createSignupVerificationText(
        String verificationCode,
        long expiresInMinutes
    ) {
        return String.join(System.lineSeparator(),
            "안녕하세요. 두리입니다.",
            "",
            "회원가입을 계속하려면 아래 인증 코드를 입력해 주세요.",
            "",
            "인증 코드: " + verificationCode,
            "유효 시간: " + expiresInMinutes + "분",
            "",
            "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );
    }

    private String createPasswordResetVerificationText(
        String verificationCode,
        long expiresInMinutes
    ) {
        return String.join(System.lineSeparator(),
            "안녕하세요. 두리입니다.",
            "",
            "비밀번호 재설정을 계속하려면 아래 인증 코드를 입력해 주세요.",
            "",
            "인증 코드: " + verificationCode,
            "유효 시간: " + expiresInMinutes + "분",
            "",
            "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );
    }

    private String createSimplePasswordVerificationText(
        String verificationCode,
        long expiresInMinutes
    ) {
        return String.join(System.lineSeparator(),
            "안녕하세요. 두리입니다.",
            "",
            "간편비밀번호 설정 또는 변경을 계속하려면 아래 인증 코드를 입력해 주세요.",
            "",
            "인증 코드: " + verificationCode,
            "유효 시간: " + expiresInMinutes + "분",
            "",
            "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );
    }
}