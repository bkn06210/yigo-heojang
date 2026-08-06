package com.wallet.chat.service;

import com.wallet.chat.dto.ChatRequest;
import com.wallet.chat.dto.ChatResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 챗봇 서버(별도 프로세스) 호출.
 *
 * 같은 WAR 안이 아니라 REST로 부르는 이유는 챗봇이 파이썬이기 때문이다.
 * 결제·정산처럼 트랜잭션을 묶어야 하는 호출이 아니라 조회뿐이라 프로세스가 갈려도 된다.
 *
 * <p>요청의 Authorization 헤더를 그대로 넘긴다. 챗봇이 답을 만들려면 실적·추천 같은
 * 계산값이 필요한데, 그 값을 챗봇이 직접 구하지 않고 이 서버의 엔진 API를 다시 호출해
 * 받아가기 때문이다. 계산 규칙을 파이썬에 복제하지 않기 위한 구조이고, 그래서 챗봇 요청이
 * 이 서버로 한 번 되돌아온다.
 *
 * <p>챗봇 서버는 토큰을 검증하지 않는다. 인증 필터가 이미 검증했고, 검증 로직이 두 벌이
 * 되면 서명 방식이나 만료 처리가 한쪽만 바뀌었을 때 조용히 어긋난다.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String CHAT_PATH = "/chat";

    private final RestTemplate restTemplate;
    private final String chatbotBaseUrl;

    public ChatService(RestTemplate restTemplate,
                       @Value("${chatbot.base-url}") String chatbotBaseUrl) {
        this.restTemplate = restTemplate;
        this.chatbotBaseUrl = chatbotBaseUrl;
    }

    public ChatResponse ask(Long memberId, ChatRequest request, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("memberId", memberId);
        body.put("question", request.getQuestion());
        body.put("pendingContext", request.getPendingContext());

        try {
            ChatResponse response = restTemplate.exchange(
                    chatbotBaseUrl + CHAT_PATH,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    ChatResponse.class).getBody();

            if (response == null) {
                throw new BusinessException(ErrorCode.CHATBOT_UNAVAILABLE);
            }
            return response;

        } catch (RestClientException error) {
            // 챗봇 서버가 안 떠 있거나 응답이 늦은 경우다. 질문 내용은 개인정보라 남기지 않는다.
            log.warn("챗봇 서버 호출 실패: memberId={}", memberId, error);
            throw new BusinessException(ErrorCode.CHATBOT_UNAVAILABLE);
        }
    }
}
