package com.wallet.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
