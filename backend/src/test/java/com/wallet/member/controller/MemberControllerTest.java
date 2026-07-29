package com.wallet.member.controller;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.wallet.common.exception.GlobalExceptionHandler;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.service.MemberService;

class MemberControllerTest {

    private final MemberService memberService = mock(MemberService.class);

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new MemberController(memberService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    @Test
    @DisplayName("회원 정보 조회 성공 - 인증된 회원이면 내 회원 정보를 반환한다")
    void getMyInfo_success() throws Exception {
        // given
        Long memberId = 1L;

        MemberMeResponse response = new MemberMeResponse(
            memberId,
            "user@example.com",
            "이재혁",
            "별명A",
            "ACTIVE",
            LocalDateTime.of(2026, 7, 18, 11, 0),
            LocalDateTime.of(2026, 7, 20, 12, 30)
        );

        when(memberService.getMyInfo(memberId))
            .thenReturn(response);

        // when & then
        mockMvc.perform(
                get("/api/members/me")
                    .requestAttr("authenticatedMemberId", memberId)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("회원정보 조회에 성공했습니다."))
            .andExpect(jsonPath("$.data.memberId").value(1))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.name").value("이재혁"))
            .andExpect(jsonPath("$.data.nickname").value("별명A"))
            .andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            /*
             * password는 Member 도메인에는 존재하지만, 회원 정보 조회 응답에는 절대 포함되면 안 됩니다.
             * 응답 DTO를 분리한 목적이 민감정보 노출 방지이므로 테스트에서도 이를 확인합니다.
             */
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.withdrawnAt").doesNotExist());

        verify(memberService).getMyInfo(memberId);
    }
}