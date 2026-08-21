package com.wallet.engine.dao.dto;

/** 카드가 제공하는 선택형 혜택 묶음과 그 선택지 하나 (회원이 고른 값이 아니다) */
public class OptionKeyRow {

    private String optionGroupCode;
    private String optionKey;

    public String getOptionGroupCode() {
        return optionGroupCode;
    }

    public void setOptionGroupCode(String optionGroupCode) {
        this.optionGroupCode = optionGroupCode;
    }

    public String getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(String optionKey) {
        this.optionKey = optionKey;
    }
}
