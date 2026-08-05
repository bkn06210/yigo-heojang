package com.wallet.card.support;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

public final class CardNumberSupport {
    private static final int MIN_CARD_NUMBER_LENGTH = 13;
    private static final int MAX_CARD_NUMBER_LENGTH = 19;
    private static final int LAST_FOUR_DIGITS_LENGTH = 4;

    private CardNumberSupport() {
    }

    /**
     * 카드번호에서 공백과 하이픈을 제거한다.
     */
    public static String normalize(String cardNumber) {
        if (cardNumber == null) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }

        return cardNumber.replaceAll("[\\s-]", "");
    }

    public static String normalizeAndValidate(String cardNumber) {
        String normalizedCardNumber = normalize(cardNumber);

        validateDigitsOnly(normalizedCardNumber);
        validateLength(normalizedCardNumber);
        validateLuhn(normalizedCardNumber);

        return normalizedCardNumber;
    }

    public static String extractLastFourDigits(String normalizedCardNumber) {
        if (normalizedCardNumber == null
            || normalizedCardNumber.length() < LAST_FOUR_DIGITS_LENGTH) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }

        return normalizedCardNumber.substring(
            normalizedCardNumber.length() - LAST_FOUR_DIGITS_LENGTH
        );
    }

    private static void validateDigitsOnly(String cardNumber) {
        if (!cardNumber.matches("\\d+")) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }
    }

    private static void validateLength(String cardNumber) {
        int length = cardNumber.length();

        if (length < MIN_CARD_NUMBER_LENGTH || length > MAX_CARD_NUMBER_LENGTH) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }
    }

    /**
     * 룬 알고리즘 검증.
     * <p>
     * 이 검증은 카드번호가 형식상 가능한 번호인지 확인하는 용도다.
     * 실제 발급 카드인지, 사용자가 소유한 카드인지는 확인하지 못한다.
     */
    private static void validateLuhn(String cardNumber) {
        int sum = 0;
        boolean shouldDouble = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';

            if (shouldDouble) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            shouldDouble = !shouldDouble;
        }

        if (sum % 10 != 0) {
            throw new BusinessException(ErrorCode.CARD_NUMBER_INVALID);
        }
    }
}