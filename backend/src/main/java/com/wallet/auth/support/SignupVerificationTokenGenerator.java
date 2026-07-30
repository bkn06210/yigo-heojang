package com.wallet.auth.support;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class SignupVerificationTokenGenerator {
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tokenBytes);
    }
}