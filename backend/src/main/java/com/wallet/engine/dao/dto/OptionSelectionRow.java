package com.wallet.engine.dao.dto;

/**
 * user_card_benefit_selection 한 행의 조회 투영 — 선택형 혜택 묶음의 그달 선택.
 *
 * 혜택 id가 아니라 선택지(option_key)를 담는다. 선택지 하나가 혜택 여러 개로 이뤄지는 경우가 있어
 * ("배달팩을 고르면 4개 혜택이 함께 켜진다") 혜택 id로는 고른 것을 표현할 수 없다.
 *
 * 회원의 보유카드를 한 번에 조회하므로 그룹핑 키로 userCardId를 함께 가져온다.
 */
public class OptionSelectionRow {

    private long userCardId;
    private String optionGroupCode;
    private String selectedOptionKey;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public String getOptionGroupCode() {
        return optionGroupCode;
    }

    public void setOptionGroupCode(String optionGroupCode) {
        this.optionGroupCode = optionGroupCode;
    }

    public String getSelectedOptionKey() {
        return selectedOptionKey;
    }

    public void setSelectedOptionKey(String selectedOptionKey) {
        this.selectedOptionKey = selectedOptionKey;
    }
}
