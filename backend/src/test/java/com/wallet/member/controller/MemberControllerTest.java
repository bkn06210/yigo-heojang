package com.wallet.member.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.wallet.auth.service.TermsService;
import com.wallet.common.exception.GlobalExceptionHandler;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.dto.MemberUpdateRequest;
import com.wallet.member.service.MemberService;

class MemberControllerTest {
    private static final String AUTHENTICATED_MEMBER_ID = "authenticatedMemberId";

    private final MemberService memberService = mock(MemberService.class);
    private final TermsService termsService = mock(TermsService.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new MemberController(memberService, termsService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .setValidator(validator())
        .build();

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        return validator;
    }

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

    @Test
    @DisplayName("회원 정보 수정 성공 - 인증된 회원이면 닉네임을 수정하고 최신 회원 정보를 반환한다")
    void updateMyInfo_success() throws Exception {
        // given
        Long memberId = 1L;

        MemberUpdateRequest request = new MemberUpdateRequest("새닉네임");

        MemberMeResponse response = new MemberMeResponse(
            memberId,
            "user@example.com",
            "이재혁",
            "새닉네임",
            "ACTIVE",
            LocalDateTime.of(2026, 7, 18, 11, 0),
            LocalDateTime.of(2026, 7, 28, 10, 30)
        );

        when(memberService.updateMyInfo(
            eq(memberId),
            argThat(updateRequest -> "새닉네임".equals(updateRequest.nickname()))
        ))
            .thenReturn(response);

        // when & then
        mockMvc.perform(
                patch("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("회원정보 수정에 성공했습니다."))
            .andExpect(jsonPath("$.data.memberId").value(1))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.name").value("이재혁"))
            .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
            .andExpect(jsonPath("$.data.memberStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            /*
             * 회원 정보 수정 응답도 조회 응답과 동일하게 민감정보를 포함하면 안 됩니다.
             * password는 해시값이어도 클라이언트 응답에 노출되면 안 됩니다.
             */
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.withdrawnAt").doesNotExist());

        verify(memberService).updateMyInfo(
            eq(memberId),
            argThat(updateRequest -> "새닉네임".equals(updateRequest.nickname()))
        );
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 닉네임이 빈 값이면 INPUT_INVALID 응답을 반환한다")
    void updateMyInfo_fail_whenNicknameIsBlank() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "nickname": ""
            }
            """;

        // when & then
        mockMvc.perform(
                patch("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"))
            .andExpect(jsonPath("$.message").value("닉네임은 필수입니다."));

        /*
         * 요청값 검증에서 실패하면 Controller 메서드의 본문 로직으로 들어가지 않습니다.
         * 따라서 Service도 호출되면 안 됩니다.
         */
        verify(memberService, never()).updateMyInfo(any(), any());
    }

    @Test
    @DisplayName("회원 정보 수정 실패 - 닉네임이 공백이면 INPUT_INVALID 응답을 반환한다")
    void updateMyInfo_fail_whenNicknameIsOnlyWhitespace() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "nickname": "   "
            }
            """;

        // when & then
        mockMvc.perform(
                patch("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"))
            .andExpect(jsonPath("$.message").value("닉네임은 필수입니다."));

        verify(memberService, never()).updateMyInfo(any(), any());
    }
}