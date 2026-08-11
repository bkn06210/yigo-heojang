package com.wallet.auth.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class TokenHashUtil {

    private static final String SHA_256 = "SHA-256";

    /**
     * 전달받은 원문 값을 SHA-256 해시 문자열로 변환한다.
     *
     * @param rawValue 인증 코드 또는 토큰 원문
     * @return SHA-256 hex 문자열
     */
    public String sha256(String rawValue) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA_256);
            byte[] digest = messageDigest.digest(rawValue.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}