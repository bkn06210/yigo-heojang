package com.wallet.chat.config;

import java.time.Duration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 챗봇 서버 호출을 감싸는 두 장치를 만든다.
 * <p>
 * 챗봇은 LLM을 두 번 부르느라 한 건에 수 초가 걸리고, 챗봇 서버가 멈추면 호출마다 읽기
 * 타임아웃까지 톰캣 스레드를 붙잡는다. 채팅이 몰리면 스레드 풀이 바닥나 결제·추천처럼
 * 챗봇과 무관한 API까지 응답하지 못한다. 부가 기능의 장애가 핵심 기능으로 번지는 것을 막는다.
 * <ul>
 *   <li>동시 호출 상한(bulkhead) — 한순간에 채팅이 쥘 수 있는 스레드 수를 묶는다. <b>몰림</b>을 막는다.</li>
 *   <li>차단기(circuit breaker) — 챗봇이 계속 실패하거나 느리면 한동안 아예 부르지 않는다.
 *       <b>이어지는 장애</b> 동안 요청마다 타임아웃까지 기다리는 일을 막는다.</li>
 * </ul>
 * 둘은 서로를 대신하지 못한다. 상한만 있으면 장애 내내 상한만큼의 스레드가 매번 타임아웃까지 묶이고,
 * 차단기만 있으면 차단기가 열리기 전(실패가 쌓이는 동안)에 몰린 요청이 스레드를 다 가져간다.
 * <p>
 * XML이 아니라 자바로 만드는 이유는 두 설정이 빌더로만 만들어지기 때문이다. 값과 근거는 chatbot.properties.
 */
@Configuration
public class ChatbotResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatbotResilienceConfig.class);

    private static final String NAME = "chatbot";

    @Bean
    public Bulkhead chatbotBulkhead(
            @Value("${chatbot.bulkhead.max-concurrent-calls}") int maxConcurrentCalls) {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                // 자리가 날 때까지 기다리게 하면 기다리는 동안 톰캣 스레드를 쥐고 있게 되어
                // 상한을 둔 의미가 없어진다. 넘치면 바로 거절한다.
                .maxWaitDuration(Duration.ZERO)
                .build();
        return Bulkhead.of(NAME, config);
    }

    @Bean
    public CircuitBreaker chatbotCircuitBreaker(
            @Value("${chatbot.circuit-breaker.sliding-window-size}") int slidingWindowSize,
            @Value("${chatbot.circuit-breaker.minimum-number-of-calls}") int minimumNumberOfCalls,
            @Value("${chatbot.circuit-breaker.failure-rate-threshold}") float failureRateThreshold,
            @Value("${chatbot.circuit-breaker.slow-call-duration-threshold-millis}") long slowCallDurationThresholdMillis,
            @Value("${chatbot.circuit-breaker.slow-call-rate-threshold}") float slowCallRateThreshold,
            @Value("${chatbot.circuit-breaker.wait-duration-in-open-state-millis}") long waitDurationInOpenStateMillis,
            @Value("${chatbot.circuit-breaker.permitted-number-of-calls-in-half-open-state}") int permittedCallsInHalfOpenState) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 시간이 아니라 건수로 본다. 채팅은 사람이 입력하는 속도로 와서 시간 창으로 보면
                // 한가한 때는 창 안에 호출이 몇 건 없어 판단 자체가 안 선다.
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThresholdMillis))
                .slowCallRateThreshold(slowCallRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenStateMillis))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                // 4xx는 요청이 잘못된 것이지 챗봇 서버가 아픈 것이 아니다. 실패로 세면
                // 잘못된 요청 몇 건이 멀쩡한 챗봇을 모두에게서 막는다.
                .ignoreExceptions(HttpClientErrorException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);
        // 상태가 바뀔 때만 남긴다. 열린 동안 거절한 요청마다 남기면 로그가 같은 줄로 덮인다.
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("챗봇 차단기 상태 전환: {}", event.getStateTransition()));
        return circuitBreaker;
    }
}
