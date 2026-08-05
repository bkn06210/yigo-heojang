package com.wallet.card.support;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

public final class CardMaskingSupport {

    private static final int LAST_FOUR_DIGITS_LENGTH = 4;

    private CardMaskingSupport() {
    }

    /**
     * DB 저장용 마스킹 카드번호를 만든다.
     * <p>
     * 전체 카드번호와 CVC는 저장하지 않고,
     * 화면 표시와 사용자 구분에 필요한 마지막 4자리만 저장한다.
     */
    public static String mask(String normalizedCardNumber) {
        if (normalizedCardNumber == null
            || normalizedCardNumber.length() < LAST_FOUR_DIGITS_LENGTH) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }

        String lastFourDigits = normalizedCardNumber.substring(
            normalizedCardNumber.length() - LAST_FOUR_DIGITS_LENGTH
        );

        return "****-****-****-" + lastFourDigits;
    }
}