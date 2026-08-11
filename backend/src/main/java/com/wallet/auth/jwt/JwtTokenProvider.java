package com.wallet.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import com.wallet.member.domain.Member;

@Component
public class JwtTokenProvider {
    /**
     * Access Token 서명용 비밀키.
     *
     * 현재 설정된 값은 로컬 테스트용 임시 문자열로,
     * 추후 배포 시 환경 변수 또는 properties 파일로 분리가 필요하다.
     */
    private final String accessTokenSecret =
        "this-is-a-sample-secret-key-for-access-token-issue-please-change";

    /**
     * Refresh Token 서명용 비밀키.
     *
     * Access Token과 Refresh Token은 수명과 사용 목적이 다르므로
     * 서로 다른 서명키를 사용한다.
     */
    private final String refreshTokenSecret =
        "this-is-a-sample-secret-key-for-refresh-token-issue-please-change";

    private final long accessTokenValidityInSeconds = 60L * 10L;  // 10분
    private final long refreshTokenValidityInSeconds = 60L * 60L * 12L; // 12시간

    private SecretKey getAccessSigningKey() {
        return Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
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
            .signWith(getAccessSigningKey())
            .compact();
    }

    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityInSeconds * 1000);

        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getRefreshSigningKey())
            .compact();
    }

    // token이 유효한지 true/false만 필요한 곳에서 사용
    public boolean validateAccessToken(String token) {
        try {
            validateAccessTokenOrThrow(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Access Token을 검증하고, 실패 원인을 호출한 쪽에서 구분할 수 있도록 예외를 그대로 던진다.
     *
     * 기존 validateAccessToken()은 모든 JWT 예외를 내부에서 잡고 false만 반환한다.
     *
     * 그래서 인증 필터에서는 이 메서드를 사용해 ExpiredJwtException을 따로 잡고,
     * 만료 토큰은 ACCESS_TOKEN_EXPIRED로 응답할 수 있게 한다.
     */
    public void validateAccessTokenOrThrow(String token) {
        Jwts.parserBuilder()
            .setSigningKey(getAccessSigningKey())
            .build()
            .parseClaimsJws(token);
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getRefreshSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberIdFromAccessToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getAccessSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

        return Long.valueOf(claims.getSubject());
    }

    public Long getMemberIdFromRefreshToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getRefreshSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 기존 코드 호환용 메서드.
     *
     * 인증 필터 구현 전까지 기존 getMemberId(token) 호출부가 있다면 깨지지 않도록 둔다.
     * 단, Access Token 기준으로만 파싱한다.
     */
    public Long getMemberId(String token) {
        return getMemberIdFromAccessToken(token);
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInSeconds;
    }
}