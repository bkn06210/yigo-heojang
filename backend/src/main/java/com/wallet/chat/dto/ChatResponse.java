package com.wallet.chat.dto;

import java.util.List;

/**
 * 챗봇 답변.
 *
 * 챗봇 서버가 주는 형식과 프론트에 내려가는 형식이 같아 한 클래스를 양쪽에 쓴다.
 * 중간에 형식을 바꾸면 필드가 하나 늘 때마다 두 곳을 고쳐야 한다.
 */
public class ChatResponse {

    private String answer;

    /** 질문 유형. 프론트가 답변 표시 방식을 나눌 때 쓴다. */
    private String intent;

    /** 답변의 숫자가 어디서 나왔는지. 근거로 화면에 표시할 수 있다. */
    private List<String> sources;

    /** 대상을 특정하지 못해 되물어야 할 때의 질문. 없으면 null. */
    private String followUpQuestion;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }
}
