package com.wallet.chat.controller;

import com.wallet.chat.dto.ChatRequest;
import com.wallet.chat.dto.ChatResponse;
import com.wallet.chat.service.ChatService;
import com.wallet.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 챗봇 질의 API.
 *
 * 프론트가 챗봇 서버를 직접 부르지 않고 이 경로로만 들어온다. 그래야 인증 필터가
 * 한 번만 검증하면 되고, 챗봇 서버를 외부에 열지 않아도 된다.
 *
 * <p>답변 생성에 LLM 호출이 들어가 응답이 수 초 걸린다. 다른 API보다 느린 것이 정상이다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 질문 하나를 받아 답변을 반환한다.
     *
     * 대상을 특정하지 못하면 답변 대신 되묻는 문장이 followUpQuestion에 담긴다 —
     * 추측해서 틀린 값을 내려주지 않는다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> ask(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            // 챗봇이 이 서버의 엔진 API를 다시 호출할 때 쓰도록 원본 토큰을 그대로 넘긴다.
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChatRequest request) {

        ChatResponse response = chatService.ask(memberId, request, authorization);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
