package com.wallet.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import com.wallet.member.domain.Member;

@Component
public class JwtTokenProvider {
    /**
     * 위조 방지용 서명을 남길 때, 사용되는 비밀키.
     *
     * 현재 설정된 값은 로컬 테스트용 임시 문자열로,
     * 추후 배포 시 환경 변수로 분리가 필요함.
     */
    private final String secret = "this-is-a-sample-secret-key-for-jwt-token-issue-please-change";
    private final long accessTokenValidityInSeconds = 1800L;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityInSeconds * 1000);

        return Jwts.builder()
            .setSubject(String.valueOf(member.getMemberId()))
            .claim("email", member.getEmail())
            .claim("name", member.getName())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey())
            .compact();
    }

    public Long getMemberId(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

        return Long.valueOf(claims.getSubject());
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }
}