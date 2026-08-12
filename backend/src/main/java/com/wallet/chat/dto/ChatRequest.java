package com.wallet.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.Map;

/**
 * 챗봇 질문.
 *
 * 회원 id는 여기 담지 않는다. 인증 필터가 토큰에서 꺼내 둔 값을 쓴다 —
 * 본문으로 받으면 남의 id를 적어 보내는 것을 막을 수 없다.
 */
public class ChatRequest {

    @NotBlank(message = "question은 필수입니다.")
    @Size(max = 500, message = "question은 500자를 넘을 수 없습니다.")
    private String question;

    /**
     * 직전 답변이 내려준 대화 맥락. 되물음에 답하는 질문일 때만 채워 보낸다.
     *
     * 형태를 여기서 정의하지 않고 그대로 전달한다. 이 서버는 내용을 해석하지 않고
     * 챗봇 서버만 읽으므로, 필드를 양쪽에 정의하면 하나가 늘 때마다 두 곳을 고쳐야 한다.
     */
    private Map<String, Object> pendingContext;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, Object> getPendingContext() {
        return pendingContext;
    }

    public void setPendingContext(Map<String, Object> pendingContext) {
        this.pendingContext = pendingContext;
    }
}
