package com.wallet.member.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.member.domain.Member;

class MemberMeResponseTest {

    @Test
    @DisplayName("간편비밀번호 해시가 있으면 설정 여부만 true로 변환한다")
    void from_simplePasswordSet() {
        Member member = memberWithSimplePasswordHash("bcrypt-hash");

        MemberMeResponse response = MemberMeResponse.from(member);

        assertThat(response.simplePasswordSet()).isTrue();
    }

    @Test
    @DisplayName("간편비밀번호 해시가 없으면 설정 여부를 false로 변환한다")
    void from_simplePasswordNotSet() {
        Member member = memberWithSimplePasswordHash(null);

        MemberMeResponse response = MemberMeResponse.from(member);

        assertThat(response.simplePasswordSet()).isFalse();
    }

    private Member memberWithSimplePasswordHash(String simplePasswordHash) {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "simplePasswordHash", simplePasswordHash);
        return member;
    }
}
