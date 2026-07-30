package com.wallet.auth.support;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class VerificationCodeGenerator {
    private static final int CODE_BOUND = 1_000_000;
    private static final String SIX_DIGIT_FORMAT = "%06d";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSixDigitCode() {
        return String.format(SIX_DIGIT_FORMAT, secureRandom.nextInt(CODE_BOUND));
    }
}