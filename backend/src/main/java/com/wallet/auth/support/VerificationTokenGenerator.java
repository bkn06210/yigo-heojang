package com.wallet.auth.support;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class VerificationTokenGenerator {
    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_SAFE_ENCODER =
        Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return URL_SAFE_ENCODER.encodeToString(randomBytes);
    }
}