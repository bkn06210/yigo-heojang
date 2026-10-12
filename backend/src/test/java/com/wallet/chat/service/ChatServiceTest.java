package com.wallet.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.wallet.chat.config.ChatbotResilienceFixture;
import com.wallet.chat.dto.ChatRequest;
import com.wallet.chat.dto.ChatResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * 챗봇 서버 호출의 계약.
 *
 * 챗봇 서버는 MockRestServiceServer로 흉내 낸다. 기대하지 않은 호출이 나가면 그 자리에서
 * 실패하므로 "부르지 않았다"를 따로 세지 않아도 확인된다.
 *
 * 상한·차단기는 운영 설정 파일로 만든다. 시간 값은 이 테스트에서 기다릴 일이 없어 줄이지 않았다.
 * 실제로 몇 초가 걸리고 몇 개의 스레드가 묶이는지는 ChatbotIsolationMeasurementTest가 잰다.
 */
class ChatServiceTest {

    private static final String CHATBOT_URL = "http://chatbot.test";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer chatbot = MockRestServiceServer.bindTo(restTemplate).build();

    private final Bulkhead bulkhead = ChatbotResilienceFixture.bulkhead();
    private final CircuitBreaker circuitBreaker = ChatbotResilienceFixture.circuitBreaker(1);

    private final ChatService chatService =
            new ChatService(restTemplate, CHATBOT_URL, bulkhead, circuitBreaker);

    @Test
    @DisplayName("질문·회원 id·원본 토큰을 넘기고 챗봇 답변을 그대로 돌려준다")
    void 정상_응답은_그대로_돌려준다() {
        chatbot.expect(requestTo(CHATBOT_URL + "/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.question").value("실적 채웠어?"))
                .andRespond(withSuccess("{\"answer\":\"실적 82.6% 달성했습니다.\",\"intent\":\"CARD_STATUS\"}",
                        MediaType.APPLICATION_JSON));

        ChatResponse response = chatService.ask(1L, question(), "Bearer test-token");

        assertThat(response.getAnswer()).isEqualTo("실적 82.6% 달성했습니다.");
        chatbot.verify();
    }

    @Test
    @DisplayName("동시 호출이 상한에 차 있으면 기다리지 않고 503 - 챗봇을 부르지 않는다")
    void 상한을_넘으면_바로_거절한다() {
        // 상한만큼의 채팅이 이미 챗봇의 답을 기다리는 상태
        int maxConcurrentCalls = bulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        for (int occupied = 0; occupied < maxConcurrentCalls; occupied++) {
            assertThat(bulkhead.tryAcquirePermission()).isTrue();
        }

        long startedAt = System.nanoTime();
        assertUnavailable();
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        // 자리가 날 때까지 기다리면 그동안 이 스레드도 묶인다. 넉넉히 잡아도 타임아웃과는 자릿수가 다르다.
        assertThat(elapsedMillis).isLessThan(500);
        chatbot.verify();
    }

    @Test
    @DisplayName("상한 초과로 거절한 것은 차단기가 챗봇 실패로 세지 않는다 - 몰림만으로 차단기가 열리면 안 된다")
    void 상한_거절은_차단기에_쌓이지_않는다() {
        int maxConcurrentCalls = bulkhead.getBulkheadConfig().getMaxConcurrentCalls();
        for (int occupied = 0; occupied < maxConcurrentCalls; occupied++) {
            bulkhead.tryAcquirePermission();
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            assertUnavailable();
        }

        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("차단기가 열려 있으면 챗봇을 부르지 않고 503")
    void 차단기가_열리면_부르지_않는다() {
        circuitBreaker.transitionToOpenState();

        assertUnavailable();

        chatbot.verify();
    }

    @Test
    @DisplayName("챗봇 서버 오류가 최소 판단 건수만큼 이어지면 차단기가 열리고, 그 뒤로는 부르지 않는다")
    void 서버_오류가_이어지면_차단기가_열린다() {
        int minimumNumberOfCalls = ChatbotResilienceFixture.intValue(ChatbotResilienceFixture.MINIMUM_NUMBER_OF_CALLS);
        chatbot.expect(times(minimumNumberOfCalls), requestTo(CHATBOT_URL + "/chat"))
                .andRespond(withServerError());

        for (int attempt = 0; attempt < minimumNumberOfCalls; attempt++) {
            assertUnavailable();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 열린 뒤의 요청은 챗봇까지 가지 않는다. 갔다면 기대하지 않은 호출이라 여기서 실패한다.
        for (int attempt = 0; attempt < 20; attempt++) {
            assertUnavailable();
        }
        chatbot.verify();
    }

    @Test
    @DisplayName("챗봇 4xx는 차단기가 실패로 세지 않는다 - 잘못된 요청이 멀쩡한 챗봇을 막으면 안 된다")
    void 클라이언트_오류는_차단기에_세지_않는다() {
        chatbot.expect(times(20), requestTo(CHATBOT_URL + "/chat"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        for (int attempt = 0; attempt < 20; attempt++) {
            assertUnavailable();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        chatbot.verify();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> chatService.ask(1L, question(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.CHATBOT_UNAVAILABLE);
    }

    private ChatRequest question() {
        ChatRequest request = new ChatRequest();
        request.setQuestion("실적 채웠어?");
        return request;
    }
}
