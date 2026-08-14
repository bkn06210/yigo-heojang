package com.wallet.chat.dto;

import java.util.List;
import java.util.Map;

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

    /** 되물어야 할 때의 질문. 없으면 null. */
    private String followUpQuestion;

    /**
     * 되물었을 때만 채워진다. 프론트가 다음 질문에 그대로 실어 보내면 대화가 이어진다.
     *
     * 서버가 보관하지 않는 이유는 재시작·다중화 때문이다. 보관하면 서버가 내려갈 때
     * 진행 중인 대화가 끊기고, 여러 대로 늘리면 요청마다 다른 서버로 가 맥락을 잃는다.
     */
    private Map<String, Object> pendingContext;

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

    public Map<String, Object> getPendingContext() {
        return pendingContext;
    }

    public void setPendingContext(Map<String, Object> pendingContext) {
        this.pendingContext = pendingContext;
    }
}
