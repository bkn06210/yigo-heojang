package com.wallet.member.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.wallet.auth.domain.TermScope;
import com.wallet.auth.dto.TermResponse;
import com.wallet.auth.dto.TermsResponse;
import com.wallet.auth.service.TermsService;
import com.wallet.common.exception.GlobalExceptionHandler;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.domain.WithdrawalReasonType;
import com.wallet.member.dto.MemberUpdateRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationResponse;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyResponse;
import com.wallet.member.dto.SimplePasswordVerifyResponse;
import com.wallet.member.service.MemberService;
import com.wallet.member.service.SimplePasswordService;
import com.wallet.member.service.SimplePasswordVerificationService;

class MemberControllerTest {
    private static final String AUTHENTICATED_MEMBER_ID = "authenticatedMemberId";

    private final MemberService memberService = mock(MemberService.class);
    private final TermsService termsService = mock(TermsService.class);
    private final SimplePasswordVerificationService simplePasswordVerificationService =
        mock(SimplePasswordVerificationService.class);
    private final SimplePasswordService simplePasswordService = mock(SimplePasswordService.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new MemberController(
            memberService,
            termsService,
            simplePasswordVerificationService,
            simplePasswordService
        ))
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
            false,
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
            false,
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

    // ── 간편비밀번호 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("간편비밀번호 인증 코드 발송 성공 - 202와 마스킹된 이메일을 반환한다")
    void sendSimplePasswordVerificationCode_success() throws Exception {
        // given
        Long memberId = 1L;

        when(simplePasswordVerificationService.sendVerificationCode(memberId))
            .thenReturn(new SimplePasswordEmailVerificationResponse("us***@example.com", 300));

        // when & then
        mockMvc.perform(
                post("/api/members/me/simple-password/email-verifications")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
            )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("간편비밀번호 변경 인증 코드가 발송되었습니다."))
            // 전체 이메일이 그대로 나가면 안 된다.
            .andExpect(jsonPath("$.data.email").value("us***@example.com"))
            .andExpect(jsonPath("$.data.expiresIn").value(300));

        verify(simplePasswordVerificationService).sendVerificationCode(memberId);
    }

    @Test
    @DisplayName("간편비밀번호 이메일 인증 성공 - 변경 토큰을 반환한다")
    void verifySimplePasswordVerificationCode_success() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "verificationCode": "123456"
            }
            """;

        when(simplePasswordVerificationService.verifyCode(
            eq(memberId),
            argThat(request -> "123456".equals(request.verificationCode()))
        )).thenReturn(
            new SimplePasswordEmailVerificationVerifyResponse("change-token-value", 600)
        );

        // when & then
        mockMvc.perform(
                post("/api/members/me/simple-password/email-verifications/verify")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.simplePasswordChangeToken").value("change-token-value"))
            .andExpect(jsonPath("$.data.expiresIn").value(600));
    }

    @Test
    @DisplayName("간편비밀번호 이메일 인증 실패 - 인증 코드가 6자리 숫자가 아니면 INPUT_INVALID")
    void verifySimplePasswordVerificationCode_fail_whenCodeNotSixDigits() throws Exception {
        // given
        String requestBody = """
            {
              "verificationCode": "12a4"
            }
            """;

        // when & then
        mockMvc.perform(
                post("/api/members/me/simple-password/email-verifications/verify")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"))
            .andExpect(jsonPath("$.message").value("인증 코드는 6자리 숫자여야 합니다."));

        verify(simplePasswordVerificationService, never()).verifyCode(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 설정 성공 - 변경 토큰과 6자리 숫자를 전달하면 저장된다")
    void updateSimplePassword_success() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "simplePasswordChangeToken": "change-token-value",
              "simplePassword": "012345",
              "simplePasswordConfirm": "012345"
            }
            """;

        // when & then
        mockMvc.perform(
                put("/api/members/me/simple-password")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("간편비밀번호 설정 또는 변경이 완료되었습니다."));

        // 앞자리 0이 살아있어야 한다 — 숫자로 받았다면 12345가 됐을 값이다.
        verify(simplePasswordService).updateSimplePassword(
            eq(memberId),
            argThat(request ->
                "change-token-value".equals(request.simplePasswordChangeToken())
                    && "012345".equals(request.simplePassword())
                    && "012345".equals(request.simplePasswordConfirm())
            )
        );
    }

    @Test
    @DisplayName("간편비밀번호 설정 실패 - 6자리 숫자가 아니면 INPUT_INVALID")
    void updateSimplePassword_fail_whenNotSixDigits() throws Exception {
        // given
        String requestBody = """
            {
              "simplePasswordChangeToken": "change-token-value",
              "simplePassword": "12345",
              "simplePasswordConfirm": "12345"
            }
            """;

        // when & then
        mockMvc.perform(
                put("/api/members/me/simple-password")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"));

        verify(simplePasswordService, never()).updateSimplePassword(any(), any());
    }

    @Test
    @DisplayName("간편비밀번호 검증 성공 - 일치 여부만 matched로 반환한다")
    void verifySimplePassword_success() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "simplePassword": "012345"
            }
            """;

        when(simplePasswordService.verifySimplePassword(
            eq(memberId),
            argThat(request -> "012345".equals(request.simplePassword()))
        )).thenReturn(new SimplePasswordVerifyResponse(true));

        // when & then
        mockMvc.perform(
                post("/api/members/me/simple-password/verifications")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.matched").value(true))
            // 해시나 남은 시도 횟수 같은 힌트가 새어 나가면 안 된다.
            .andExpect(jsonPath("$.data.simplePasswordHash").doesNotExist());
    }

    // ── 회원 탈퇴 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("탈퇴 안내 약관 조회 성공 - WITHDRAWAL 스코프 약관을 반환한다")
    void getWithdrawalTerms_success() throws Exception {
        // given
        TermResponse term = new TermResponse(
            5L,
            "WITHDRAWAL_NOTICE",
            "회원 탈퇴 안내 및 동의",
            true,
            "ACTIVE",
            5L,
            "2026-07-01",
            "1. 탈퇴 시 보유카드, 소비내역, 결제내역, 포인트 정보는 즉시 삭제됩니다.",
            LocalDateTime.of(2026, 7, 1, 0, 0)
        );

        when(termsService.getTerms(TermScope.WITHDRAWAL))
            .thenReturn(new TermsResponse(List.of(term)));

        // when & then
        mockMvc.perform(
                get("/api/members/me/withdrawal-terms")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("탈퇴 안내 약관 조회에 성공했습니다."))
            // 프론트는 이 세 필드로 화면을 그리고, termsVersionId를 탈퇴 요청에 되돌려 보낸다.
            .andExpect(jsonPath("$.data.terms[0].termsVersionId").value(5))
            .andExpect(jsonPath("$.data.terms[0].termsName").value("회원 탈퇴 안내 및 동의"))
            .andExpect(jsonPath("$.data.terms[0].required").value(true))
            .andExpect(jsonPath("$.data.terms[0].content").exists());

        // 가입 약관이 섞여 나오면 안 되므로 스코프를 고정해 호출하는지 확인한다.
        verify(termsService).getTerms(TermScope.WITHDRAWAL);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 인증된 회원이면 탈퇴를 처리하고 성공 응답을 반환한다")
    void withdraw_success() throws Exception {
        // given
        Long memberId = 1L;

        String requestBody = """
            {
              "password": "Password1!",
              "reasonType": "LOW_USAGE",
              "reasonDetail": "자주 사용하지 않아서요.",
              "termVersionId": 5
            }
            """;

        // when & then
        mockMvc.perform(
                delete("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, memberId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

        verify(memberService).withdraw(
            eq(memberId),
            argThat(request ->
                "Password1!".equals(request.password())
                    && request.reasonType() == WithdrawalReasonType.LOW_USAGE
                    && "자주 사용하지 않아서요.".equals(request.reasonDetail())
                    && Long.valueOf(5L).equals(request.termVersionId())
            )
        );
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호가 없으면 INPUT_INVALID 응답을 반환한다")
    void withdraw_fail_whenPasswordMissing() throws Exception {
        // given
        String requestBody = """
            {
              "reasonType": "LOW_USAGE",
              "termVersionId": 5
            }
            """;

        // when & then
        mockMvc.perform(
                delete("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"))
            .andExpect(jsonPath("$.message").value("비밀번호는 필수입니다."));

        verify(memberService, never()).withdraw(any(), any());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 탈퇴 약관 동의(termVersionId)가 없으면 INPUT_INVALID 응답을 반환한다")
    void withdraw_fail_whenTermVersionIdMissing() throws Exception {
        // given
        String requestBody = """
            {
              "password": "Password1!",
              "reasonType": "LOW_USAGE"
            }
            """;

        // when & then
        mockMvc.perform(
                delete("/api/members/me")
                    .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("INPUT_INVALID"))
            .andExpect(jsonPath("$.message").value("탈퇴 안내 약관 동의는 필수입니다."));

        verify(memberService, never()).withdraw(any(), any());
    }
}