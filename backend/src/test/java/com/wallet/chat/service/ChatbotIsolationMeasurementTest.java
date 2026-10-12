package com.wallet.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.wallet.chat.config.ChatbotResilienceFixture;
import com.wallet.chat.dto.ChatRequest;
import com.wallet.common.exception.BusinessException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 챗봇 서버가 멈추거나 느려졌을 때 장애가 어디까지 번지는지 잰다. 격리 전과 후를 같은 조건에서 견준다.
 *
 * <p><b>무엇을 흉내 내나</b>
 * <ul>
 *   <li>톰캣 — 요청 하나에 스레드 하나를 배정하는 고정 크기 풀이다. 같은 모양의 스레드 풀을
 *       톰캣 기본값(200개)으로 만들고, 채팅 요청과 다른 API 요청을 같은 풀에 넣는다.</li>
 *   <li>챗봇 서버 — JDK 내장 HTTP 서버. 응답을 아예 안 주거나(멈춤) 늦게 준다(느림).</li>
 * </ul>
 *
 * <p><b>무엇을 줄였나</b> — 시간만 1/25로 줄였다. 읽기 타임아웃 25초 → 1초, 차단기 대기 30초 → 1.2초,
 * 느린 호출 기준 15초 → 0.6초. 스레드 수·상한·건수·비율은 운영 설정 그대로다. 연결 타임아웃은 줄이지
 * 않았다(로컬이라 바로 연결되어 결과에 영향이 없다).
 *
 * <p><b>격리 전</b>은 같은 코드에 상한을 사실상 무한으로, 차단기를 꺼 둔 상태로 넣어 재현한다.
 * 코드 경로가 같아야 차이가 격리 장치에서만 나온다. 읽기 타임아웃도 양쪽이 같다.
 *
 * <p>시간 단언은 넉넉하게 잡았다. 보려는 것은 "타임아웃만큼 기다리느냐, 바로 끝나느냐"라
 * 둘 사이가 자릿수로 갈린다. 측정값은 표준 출력에 표로 남긴다.
 */
class ChatbotIsolationMeasurementTest {

    private static final int TIME_SCALE = 25;

    /** 톰캣 기본 maxThreads. 줄이지 않았다. */
    private static final int TOMCAT_THREADS = 200;

    /** 한꺼번에 들어오는 채팅 수. 스레드 풀보다 많아야 풀이 바닥나는 상황이 재현된다. */
    private static final int CHAT_BURST = 300;

    /** 채팅이 몰린 직후 들어오는 다른 API(결제·추천 등) 요청 수. */
    private static final int OTHER_API_REQUESTS = 10;

    private static final long READ_TIMEOUT_MILLIS =
            ChatbotResilienceFixture.scaledMillis(ChatbotResilienceFixture.READ_TIMEOUT, TIME_SCALE);

    private static final int MAX_CONCURRENT_CALLS =
            ChatbotResilienceFixture.intValue(ChatbotResilienceFixture.MAX_CONCURRENT_CALLS);

    private static final int MINIMUM_NUMBER_OF_CALLS =
            ChatbotResilienceFixture.intValue(ChatbotResilienceFixture.MINIMUM_NUMBER_OF_CALLS);

    private FakeChatbotServer chatbot;

    @BeforeEach
    void startChatbot() throws IOException {
        chatbot = new FakeChatbotServer();
    }

    @AfterEach
    void stopChatbot() {
        chatbot.close();
    }

    // ── 1. 몰림: 챗봇이 멈춘 상태에서 채팅이 한꺼번에 들어온다 ──────────────────

    @Test
    @DisplayName("챗봇이 멈춘 채 채팅이 몰려도 다른 API는 스레드를 얻어 바로 응답한다")
    void 채팅이_몰려도_다른_API가_응답한다() throws Exception {
        chatbot.hang();

        BurstResult before = runBurst(this::withoutIsolation);
        BurstResult after = runBurst(this::withIsolation);

        printTable(String.format(
                        "[몰림] 톰캣 스레드 %d · 채팅 %d건 동시 · 챗봇 멈춤 · 읽기 타임아웃 %dms(실제 25초)",
                        TOMCAT_THREADS, CHAT_BURST, READ_TIMEOUT_MILLIS),
                List.of(
                        row("채팅이 쥔 최대 스레드",
                                before.chatThreadsPeak + "/" + TOMCAT_THREADS,
                                after.chatThreadsPeak + "/" + TOMCAT_THREADS),
                        row("챗봇까지 간 채팅", before.reachedChatbot(), after.reachedChatbot()),
                        row("부르지 않고 바로 거절한 채팅", before.rejectedImmediately(), after.rejectedImmediately()),
                        row("바로 거절까지 걸린 시간 p50/최대",
                                before.rejectedLatency(), after.rejectedLatency()),
                        row("챗봇을 기다린 채팅 응답 p50/최대", before.waitedLatency(), after.waitedLatency()),
                        row("다른 API 응답 p50/최대", before.otherApiLatency(), after.otherApiLatency())));

        // 격리 전: 채팅이 스레드를 전부 쥐고, 다른 API는 채팅이 타임아웃으로 풀려날 때까지 기다린다.
        assertThat(before.chatThreadsPeak).isEqualTo(TOMCAT_THREADS);
        assertThat(before.reachedChatbot()).isEqualTo(CHAT_BURST);
        assertThat(percentile(before.otherApiMillis, 50)).isGreaterThanOrEqualTo(READ_TIMEOUT_MILLIS * 8 / 10);

        // 격리 후: 채팅은 상한만큼만 스레드를 쥐고 나머지는 부르지도 않고 돌려보낸다.
        assertThat(after.chatThreadsPeak).isLessThanOrEqualTo(MAX_CONCURRENT_CALLS);
        assertThat(after.reachedChatbot()).isLessThanOrEqualTo(MAX_CONCURRENT_CALLS);
        assertThat(after.rejectedImmediately()).isGreaterThanOrEqualTo(CHAT_BURST - MAX_CONCURRENT_CALLS);
        assertThat(max(after.rejectedMillis)).isLessThan(READ_TIMEOUT_MILLIS / 3);
        assertThat(max(after.otherApiMillis)).isLessThan(READ_TIMEOUT_MILLIS / 3);
    }

    // ── 2. 이어지는 장애: 챗봇이 멈춘 채로 있다 ─────────────────────────────

    @Test
    @DisplayName("챗봇이 멈춘 채로 이어지면 차단기가 열려 타임아웃을 기다리지 않고, 회복하면 다시 닫힌다")
    void 멈춤이_이어지면_차단기가_열리고_회복하면_닫힌다() throws Exception {
        chatbot.hang();

        PhaseResult before = runTwoPhases(withoutIsolation(new CallMeter()));
        CircuitBreaker circuitBreaker = ChatbotResilienceFixture.circuitBreaker(TIME_SCALE);
        ChatService isolated = withIsolation(new CallMeter(), ChatbotResilienceFixture.bulkhead(), circuitBreaker);
        PhaseResult after = runTwoPhases(isolated);
        CircuitBreaker.State stateAfterFailures = circuitBreaker.getState();

        // 챗봇이 살아난 뒤: 대기 시간이 지나면 몇 건만 흘려 확인하고 닫힌다.
        long waitInOpenMillis = ChatbotResilienceFixture.scaledMillis(
                ChatbotResilienceFixture.WAIT_DURATION_IN_OPEN_STATE, TIME_SCALE);
        chatbot.respondAfter(0);
        Thread.sleep(waitInOpenMillis + 100);
        int trialCalls = ChatbotResilienceFixture.intValue(
                ChatbotResilienceFixture.PERMITTED_CALLS_IN_HALF_OPEN_STATE);
        List<ChatOutcome> trials = new ArrayList<>();
        for (int trial = 0; trial < trialCalls; trial++) {
            trials.add(chat(isolated, System.nanoTime()));
        }

        printTable(String.format(
                        "[이어지는 멈춤] 1단계 %d건 → 2단계 %d건 · 읽기 타임아웃 %dms(실제 25초)",
                        MINIMUM_NUMBER_OF_CALLS, MAX_CONCURRENT_CALLS, READ_TIMEOUT_MILLIS),
                List.of(
                        row("1단계 응답 p50/최대", latency(before.firstPhase), latency(after.firstPhase)),
                        row("2단계 응답 p50/최대", latency(before.secondPhase), latency(after.secondPhase)),
                        row("2단계에 챗봇까지 간 호출", before.secondPhaseReached, after.secondPhaseReached),
                        row("1단계 뒤 차단기 상태", "(없음)", stateAfterFailures),
                        row("회복 확인 " + trialCalls + "건 뒤 상태", "-", circuitBreaker.getState())));

        assertThat(latencies(before.secondPhase).get(0)).isGreaterThanOrEqualTo(READ_TIMEOUT_MILLIS * 8 / 10);
        assertThat(before.secondPhaseReached).isEqualTo(MAX_CONCURRENT_CALLS);

        assertThat(stateAfterFailures).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(max(latencies(after.secondPhase))).isLessThan(READ_TIMEOUT_MILLIS / 3);
        assertThat(after.secondPhaseReached).isZero();

        assertThat(trials).allMatch(ChatOutcome::answered);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // ── 3. 느린 성공: 챗봇이 마감시간에 걸려 대체 답변만 늦게 돌려준다 ──────────────

    @Test
    @DisplayName("챗봇이 200을 주더라도 매번 느리면 차단기가 연다 - 실패만 세면 열리지 않는다")
    void 느린_성공도_차단기가_센다() throws Exception {
        // 챗봇 서버의 마감시간(20초)에 걸려 대체 답변을 200으로 돌려주는 상황. 1/25로 줄여 800ms.
        long chatbotDeadlineMillis = 20_000 / TIME_SCALE;
        chatbot.respondAfter(chatbotDeadlineMillis);

        PhaseResult before = runTwoPhases(withoutIsolation(new CallMeter()));

        // 느린 호출 기준을 읽기 타임아웃보다 길게 두면 느린 성공은 절대 걸리지 않는다 = 실패만 세는 차단기
        CircuitBreaker failureOnlyBreaker =
                ChatbotResilienceFixture.circuitBreaker(TIME_SCALE, READ_TIMEOUT_MILLIS * 10);
        PhaseResult failureOnly = runTwoPhases(
                withIsolation(new CallMeter(), ChatbotResilienceFixture.bulkhead(), failureOnlyBreaker));

        CircuitBreaker slowAwareBreaker = ChatbotResilienceFixture.circuitBreaker(TIME_SCALE);
        PhaseResult slowAware = runTwoPhases(
                withIsolation(new CallMeter(), ChatbotResilienceFixture.bulkhead(), slowAwareBreaker));

        System.out.println();
        System.out.printf("[느린 성공] 챗봇이 %dms(실제 20초)마다 200 대체 답변 · 느린 호출 기준 %dms(실제 15초)%n",
                chatbotDeadlineMillis,
                ChatbotResilienceFixture.scaledMillis(ChatbotResilienceFixture.SLOW_CALL_DURATION_THRESHOLD, TIME_SCALE));
        System.out.printf("%-26s %-16s %-16s %-16s%n", "", "격리 전", "실패만 세는 차단기", "느린 호출도 셈(운영)");
        System.out.printf("%-26s %-16s %-16s %-16s%n", "1단계 뒤 차단기 상태",
                "(없음)", failureOnlyBreaker.getState(), slowAwareBreaker.getState());
        System.out.printf("%-26s %-16s %-16s %-16s%n", "2단계 응답 p50/최대",
                latency(before.secondPhase), latency(failureOnly.secondPhase), latency(slowAware.secondPhase));
        System.out.printf("%-26s %-16s %-16s %-16s%n", "2단계에 챗봇까지 간 호출",
                before.secondPhaseReached, failureOnly.secondPhaseReached, slowAware.secondPhaseReached);

        assertThat(failureOnlyBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(failureOnly.secondPhaseReached).isEqualTo(MAX_CONCURRENT_CALLS);

        assertThat(slowAwareBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(slowAware.secondPhaseReached).isZero();
        assertThat(max(latencies(slowAware.secondPhase))).isLessThan(chatbotDeadlineMillis / 3);
    }

    // ── 실행 ──────────────────────────────────────────────────────────

    private BurstResult runBurst(Function<CallMeter, ChatService> chatServiceFactory) throws Exception {
        CallMeter meter = new CallMeter();
        ChatService chatService = chatServiceFactory.apply(meter);

        ThreadPoolExecutor tomcat = new ThreadPoolExecutor(TOMCAT_THREADS, TOMCAT_THREADS,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        tomcat.prestartAllCoreThreads();
        try {
            List<Future<ChatOutcome>> chats = new ArrayList<>();
            for (int request = 0; request < CHAT_BURST; request++) {
                long submittedAt = System.nanoTime();
                chats.add(tomcat.submit(() -> chat(chatService, submittedAt)));
            }
            // 다른 API는 스레드를 얻는 즉시 끝나는 가벼운 요청으로 본다. 잰 값은 곧 "스레드를 얻기까지 기다린 시간"이다.
            List<Future<Long>> otherApis = new ArrayList<>();
            for (int request = 0; request < OTHER_API_REQUESTS; request++) {
                long submittedAt = System.nanoTime();
                otherApis.add(tomcat.submit(() -> millisSince(submittedAt)));
            }

            List<ChatOutcome> chatOutcomes = collect(chats);
            List<Long> otherApiMillis = collect(otherApis);
            return new BurstResult(chatOutcomes, otherApiMillis, meter.maxInFlight.get());
        } finally {
            tomcat.shutdownNow();
        }
    }

    /** 1단계로 차단기가 판단할 만큼 실패를 쌓고, 2단계에서 그 뒤의 요청이 어떻게 되는지 본다. */
    private PhaseResult runTwoPhases(ChatService chatService) throws Exception {
        List<ChatOutcome> firstPhase = runConcurrently(chatService, MINIMUM_NUMBER_OF_CALLS);
        int reachedBeforeSecondPhase = chatbot.received.get();
        // 상한 이하로 보내 상한 거절이 섞이지 않게 한다. 2단계의 거절은 차단기 몫만 남는다.
        List<ChatOutcome> secondPhase = runConcurrently(chatService, MAX_CONCURRENT_CALLS);
        int secondPhaseReached = chatbot.received.get() - reachedBeforeSecondPhase;

        return new PhaseResult(firstPhase, secondPhase, secondPhaseReached);
    }

    private List<ChatOutcome> runConcurrently(ChatService chatService, int count) throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(count);
        try {
            List<Future<ChatOutcome>> futures = new ArrayList<>();
            for (int request = 0; request < count; request++) {
                long submittedAt = System.nanoTime();
                futures.add(threads.submit(() -> chat(chatService, submittedAt)));
            }
            return collect(futures);
        } finally {
            threads.shutdownNow();
        }
    }

    private ChatOutcome chat(ChatService chatService, long submittedAt) {
        CallMeter.calledOnThisThread.set(false);
        boolean answered;
        try {
            chatService.ask(1L, question(), "Bearer test-token");
            answered = true;
        } catch (BusinessException unavailable) {
            answered = false;
        }
        return new ChatOutcome(millisSince(submittedAt), CallMeter.calledOnThisThread.get(), answered);
    }

    // ── 격리 전·후 ─────────────────────────────────────────────────────

    private ChatService withoutIsolation(CallMeter meter) {
        // 상한이 사실상 없고 차단기가 꺼진 상태 = 격리 장치를 넣기 전의 동작
        Bulkhead unlimited = Bulkhead.of("unlimited",
                BulkheadConfig.custom().maxConcurrentCalls(Integer.MAX_VALUE).build());
        CircuitBreaker disabled = CircuitBreaker.ofDefaults("disabled");
        disabled.transitionToDisabledState();
        return new ChatService(restTemplate(meter), chatbot.baseUrl(), unlimited, disabled);
    }

    private ChatService withIsolation(CallMeter meter) {
        return withIsolation(meter, ChatbotResilienceFixture.bulkhead(),
                ChatbotResilienceFixture.circuitBreaker(TIME_SCALE));
    }

    private ChatService withIsolation(CallMeter meter, Bulkhead bulkhead, CircuitBreaker circuitBreaker) {
        return new ChatService(restTemplate(meter), chatbot.baseUrl(), bulkhead, circuitBreaker);
    }

    private RestTemplate restTemplate(CallMeter meter) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(ChatbotResilienceFixture.intValue(ChatbotResilienceFixture.CONNECT_TIMEOUT));
        requestFactory.setReadTimeout((int) READ_TIMEOUT_MILLIS);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setInterceptors(List.of(meter));
        return restTemplate;
    }

    private static ChatRequest question() {
        ChatRequest request = new ChatRequest();
        request.setQuestion("실적 채웠어?");
        return request;
    }

    // ── 측정 도구 ──────────────────────────────────────────────────────

    /**
     * 챗봇 호출을 기다리고 있는 스레드 수를 센다.
     *
     * RestTemplate 가로채기 자리에 끼워, 실제로 챗봇에 요청을 보내고 답을 기다리는 구간만 센다.
     * 상한·차단기에 막힌 요청은 이 구간에 들어오지 않는다.
     */
    private static final class CallMeter implements ClientHttpRequestInterceptor {

        /** 이 스레드의 채팅이 챗봇까지 갔는가. 거절된 채팅과 챗봇을 기다린 채팅을 가른다. */
        static final ThreadLocal<Boolean> calledOnThisThread = ThreadLocal.withInitial(() -> false);

        private final AtomicInteger inFlight = new AtomicInteger();
        private final AtomicInteger maxInFlight = new AtomicInteger();

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            calledOnThisThread.set(true);
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            try {
                ClientHttpResponse response = execution.execute(request, body);
                // POST는 본문을 보내자마자 돌아오고, 답을 기다리는 일은 상태 코드를 읽을 때 일어난다.
                // 여기서 읽어야 기다리는 구간이 이 안에 들어온다. 어차피 RestTemplate이 곧바로 읽는 값이다.
                response.getRawStatusCode();
                return response;
            } finally {
                inFlight.decrementAndGet();
            }
        }
    }

    /** 응답을 안 주거나(멈춤) 정해진 시간 뒤에 주는 가짜 챗봇 서버. */
    private static final class FakeChatbotServer implements AutoCloseable {

        private static final long HANG = -1;
        private static final byte[] ANSWER = ("{\"answer\":\"지금은 답변을 만들지 못했습니다.\","
                + "\"intent\":\"UNKNOWN\",\"sources\":[]}").getBytes(StandardCharsets.UTF_8);

        private final ExecutorService handlers = Executors.newCachedThreadPool();
        private final HttpServer server;
        private final AtomicInteger received = new AtomicInteger();
        private volatile long responseDelayMillis;

        FakeChatbotServer() throws IOException {
            // 대기열을 넉넉히 둔다. 서버 쪽 연결 대기열이 막혀서 느려지면 측정하려는 것과 다른 것을 재게 된다.
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), CHAT_BURST * 2);
            server.createContext("/chat", this::handle);
            server.setExecutor(handlers);
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void hang() {
            responseDelayMillis = HANG;
        }

        void respondAfter(long delayMillis) {
            responseDelayMillis = delayMillis;
        }

        private void handle(HttpExchange exchange) throws IOException {
            received.incrementAndGet();
            try {
                exchange.getRequestBody().readAllBytes();
                long delay = responseDelayMillis;
                if (delay == HANG) {
                    // 연결은 받아 놓고 답을 주지 않는다. 호출한 쪽은 읽기 타임아웃까지 기다린다.
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                    return;
                }
                Thread.sleep(delay);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, ANSWER.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(ANSWER);
                }
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            } catch (IOException callerGone) {
                // 호출한 쪽이 이미 타임아웃으로 끊었다
            } finally {
                exchange.close();
            }
        }

        @Override
        public void close() {
            server.stop(0);
            handlers.shutdownNow();
        }
    }

    // ── 결과 ─────────────────────────────────────────────────────────

    private record ChatOutcome(long elapsedMillis, boolean calledChatbot, boolean answered) {
    }

    private record PhaseResult(List<ChatOutcome> firstPhase, List<ChatOutcome> secondPhase,
                               int secondPhaseReached) {
    }

    private static final class BurstResult {

        final List<ChatOutcome> chats;
        final List<Long> otherApiMillis;
        final List<Long> rejectedMillis;
        final List<Long> waitedMillis;
        final int chatThreadsPeak;

        BurstResult(List<ChatOutcome> chats, List<Long> otherApiMillis, int chatThreadsPeak) {
            this.chats = chats;
            this.otherApiMillis = otherApiMillis;
            this.chatThreadsPeak = chatThreadsPeak;
            this.rejectedMillis = millisOf(chats, outcome -> !outcome.calledChatbot());
            this.waitedMillis = millisOf(chats, ChatOutcome::calledChatbot);
        }

        long reachedChatbot() {
            return waitedMillis.size();
        }

        long rejectedImmediately() {
            return rejectedMillis.size();
        }

        String rejectedLatency() {
            return format(rejectedMillis);
        }

        String waitedLatency() {
            return format(waitedMillis);
        }

        String otherApiLatency() {
            return format(otherApiMillis);
        }
    }

    private static List<Long> millisOf(List<ChatOutcome> outcomes, Predicate<ChatOutcome> filter) {
        return outcomes.stream().filter(filter).map(ChatOutcome::elapsedMillis).sorted()
                .collect(Collectors.toList());
    }

    private static List<Long> latencies(List<ChatOutcome> outcomes) {
        return millisOf(outcomes, outcome -> true);
    }

    private static String latency(List<ChatOutcome> outcomes) {
        return format(latencies(outcomes));
    }

    private static String format(List<Long> millis) {
        if (millis.isEmpty()) {
            return "-";
        }
        return percentile(millis, 50) + "/" + max(millis) + "ms";
    }

    private static long percentile(List<Long> millis, int percent) {
        List<Long> sorted = millis.stream().sorted().collect(Collectors.toList());
        int index = (int) Math.ceil(percent / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private static long max(List<Long> millis) {
        return millis.stream().max(Comparator.naturalOrder()).orElse(0L);
    }

    private static long millisSince(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    private static <T> List<T> collect(List<Future<T>> futures) throws Exception {
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(1, TimeUnit.MINUTES));
        }
        return results;
    }

    private static String[] row(String label, Object before, Object after) {
        return new String[]{label, String.valueOf(before), String.valueOf(after)};
    }

    private static void printTable(String title, List<String[]> rows) {
        System.out.println();
        System.out.println(title);
        System.out.printf("%-30s %-18s %-18s%n", "", "격리 전", "격리 후");
        for (String[] row : rows) {
            System.out.printf("%-30s %-18s %-18s%n", row[0], row[1], row[2]);
        }
    }
}
