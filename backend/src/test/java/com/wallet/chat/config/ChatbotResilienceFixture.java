package com.wallet.chat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * 테스트용 동시 호출 상한·차단기.
 *
 * 운영 설정 파일(chatbot.properties)을 그대로 읽어 운영과 같은 방법(ChatbotResilienceConfig)으로 만든다.
 * 값을 테스트에 따로 적으면 설정을 바꿨을 때 테스트는 옛 값을 검증하게 된다.
 *
 * 시간 값만 timeScale로 나눈다. 25초짜리 타임아웃을 실제로 기다리면 테스트 하나가 분 단위가 되기
 * 때문이다. 건수·비율은 그대로 두어 "몇 건에서 열리는가" 같은 판단은 운영과 같다.
 */
public final class ChatbotResilienceFixture {

    public static final String READ_TIMEOUT = "chatbot.read-timeout-millis";
    public static final String CONNECT_TIMEOUT = "chatbot.connect-timeout-millis";
    public static final String MAX_CONCURRENT_CALLS = "chatbot.bulkhead.max-concurrent-calls";
    public static final String MINIMUM_NUMBER_OF_CALLS = "chatbot.circuit-breaker.minimum-number-of-calls";
    public static final String SLOW_CALL_DURATION_THRESHOLD =
            "chatbot.circuit-breaker.slow-call-duration-threshold-millis";
    public static final String WAIT_DURATION_IN_OPEN_STATE =
            "chatbot.circuit-breaker.wait-duration-in-open-state-millis";
    public static final String PERMITTED_CALLS_IN_HALF_OPEN_STATE =
            "chatbot.circuit-breaker.permitted-number-of-calls-in-half-open-state";

    private static final Properties PROPERTIES = load();

    private ChatbotResilienceFixture() {
    }

    public static int intValue(String key) {
        return Integer.parseInt(PROPERTIES.getProperty(key).trim());
    }

    /** 시간 설정값(밀리초)을 timeScale로 줄인 값. */
    public static long scaledMillis(String key, int timeScale) {
        return Long.parseLong(PROPERTIES.getProperty(key).trim()) / timeScale;
    }

    public static Bulkhead bulkhead() {
        return new ChatbotResilienceConfig().chatbotBulkhead(intValue(MAX_CONCURRENT_CALLS));
    }

    public static CircuitBreaker circuitBreaker(int timeScale) {
        return circuitBreaker(timeScale, scaledMillis(SLOW_CALL_DURATION_THRESHOLD, timeScale));
    }

    /** 느린 호출 기준만 바꾼 차단기. 느린 호출을 세지 않을 때와 견주는 데 쓴다. */
    public static CircuitBreaker circuitBreaker(int timeScale, long slowCallDurationThresholdMillis) {
        return new ChatbotResilienceConfig().chatbotCircuitBreaker(
                intValue("chatbot.circuit-breaker.sliding-window-size"),
                intValue(MINIMUM_NUMBER_OF_CALLS),
                Float.parseFloat(PROPERTIES.getProperty("chatbot.circuit-breaker.failure-rate-threshold").trim()),
                slowCallDurationThresholdMillis,
                Float.parseFloat(PROPERTIES.getProperty("chatbot.circuit-breaker.slow-call-rate-threshold").trim()),
                scaledMillis(WAIT_DURATION_IN_OPEN_STATE, timeScale),
                intValue(PERMITTED_CALLS_IN_HALF_OPEN_STATE));
    }

    private static Properties load() {
        try {
            return PropertiesLoaderUtils.loadProperties(new ClassPathResource("chatbot.properties"));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
