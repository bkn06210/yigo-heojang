package com.wallet.auth.jwt;

import com.wallet.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();

    @Test
    @DisplayName("Access Token 생성 - 정상 회원 정보로 JWT를 생성한다")
    void createAccessToken_success() {
        // given
        Member member = createMember();

        // when
        String token = jwtTokenProvider.createAccessToken(member);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("회원 ID 추출 - 생성된 토큰에서 memberId를 추출한다")
    void getMemberId_success() {
        // given
        Member member = createMember();
        String token = jwtTokenProvider.createAccessToken(member);

        // when
        Long memberId = jwtTokenProvider.getMemberId(token);

        // then
        assertThat(memberId).isEqualTo(1L);
    }

    @Test
    @DisplayName("토큰 유효시간 조회 - Access Token 만료 시간이 반환된다")
    void getAccessTokenValidityInSeconds_success() {
        // when
        long expiresIn = jwtTokenProvider.getAccessTokenValidityInSeconds();

        // then
        assertThat(expiresIn).isGreaterThan(0);
    }

    @Test
    @DisplayName("잘못된 토큰 검증 - 잘못된 JWT이면 예외가 발생한다")
    void getMemberId_fail_whenInvalidToken() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when & then
        assertThrows(
            RuntimeException.class,
            () -> jwtTokenProvider.getMemberId(invalidToken)
        );
    }

    private Member createMember() {
        Member member = new Member();

        ReflectionTestUtils.setField(member, "memberId", 1L);
        ReflectionTestUtils.setField(member, "email", "user@example.com");
        ReflectionTestUtils.setField(member, "name", "이재혁");
        ReflectionTestUtils.setField(member, "memberStatus", "ACTIVE");
        return member;
    }
}