package com.wallet.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.chat.dto.ChatRequest;
import com.wallet.chat.dto.ChatResponse;
import com.wallet.chat.service.ChatService;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * 챗봇 질의 API.
 *
 * 챗봇 서버는 별도 프로세스라 여기서는 서비스를 대역으로 둔다.
 * 확인하려는 것은 답변 내용이 아니라 컨트롤러 계층의 계약이다 —
 * 회원 id를 어디서 꺼내는지, 토큰을 넘기는지, 실패를 어떤 상태 코드로 바꾸는지.
 */
class ChatControllerTest {

    private static final String AUTHENTICATED_MEMBER_ID = "authenticatedMemberId";

    private final ChatService chatService = mock(ChatService.class);

    // Spring MVC가 실제로 쓰는 설정으로 만든다. new ObjectMapper()는 모르는 필드를 만나면
    // 예외를 던지지만 MVC 기본값은 무시하고 넘어가, 그대로 쓰면 실제와 다른 동작을 검증하게 된다.
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ChatController(chatService))
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
    @DisplayName("질문을 넘기면 챗봇 답변을 응답 봉투에 담아 반환한다")
    void 질문하면_답변을_반환한다() throws Exception {
        when(chatService.ask(eq(1L), any(ChatRequest.class), any())).thenReturn(answer());

        mockMvc.perform(post("/api/chat")
                        .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"실적 채웠어?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("CARD_STATUS"))
                .andExpect(jsonPath("$.data.answer").value("실적 82.6% 달성했습니다."))
                .andExpect(jsonPath("$.data.sources[0]").value("보유 카드 월별 현황"));
    }

    @Test
    @DisplayName("원본 토큰을 챗봇 서버로 그대로 넘긴다 - 챗봇이 엔진 API를 다시 호출해야 한다")
    void 토큰을_그대로_넘긴다() throws Exception {
        when(chatService.ask(any(), any(), any())).thenReturn(answer());

        mockMvc.perform(post("/api/chat")
                        .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"실적 채웠어?\"}"))
                .andExpect(status().isOk());

        verify(chatService).ask(eq(1L),
                argThat(sent -> "실적 채웠어?".equals(sent.getQuestion())),
                eq("Bearer test-token"));
    }

    @Test
    @DisplayName("회원 id는 요청 본문이 아니라 인증 정보에서 꺼낸다")
    void 본문의_회원id는_무시된다() throws Exception {
        when(chatService.ask(any(), any(), any())).thenReturn(answer());

        mockMvc.perform(post("/api/chat")
                        .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"실적 채웠어?\",\"memberId\":999}"))
                .andExpect(status().isOk());

        // 본문에 999를 적어 보내도 인증된 1번 회원으로 조회한다.
        // ChatRequest에 memberId 필드 자체가 없어 값이 들어올 자리가 없다.
        verify(chatService).ask(eq(1L), any(), any());
    }

    @Test
    @DisplayName("빈 질문은 400으로 거절한다")
    void 빈_질문은_거절한다() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("챗봇 서버가 응답하지 않으면 503으로 알린다")
    void 챗봇_서버_장애는_503이다() throws Exception {
        when(chatService.ask(any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.CHATBOT_UNAVAILABLE));

        mockMvc.perform(post("/api/chat")
                        .requestAttr(AUTHENTICATED_MEMBER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"실적 채웠어?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CHATBOT_UNAVAILABLE"));
    }

    private ChatResponse answer() {
        ChatResponse response = new ChatResponse();
        response.setAnswer("실적 82.6% 달성했습니다.");
        response.setIntent("CARD_STATUS");
        response.setSources(List.of("보유 카드 월별 현황"));
        return response;
    }
}
