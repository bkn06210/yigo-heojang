package com.wallet.card.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보유 카드 목록 조회 쿼리의 결과를 담는 객체.
 *
 * 이 객체는 DB 조회 결과를 Service 계층으로 전달하는 역할을 한다.
 * API 응답 형식은 dto 패키지의 UserCardListItemResponse가 담당하므로,
 * DB 조회 객체와 API 응답 객체의 역할을 분리한다.
 */
@Getter
@RequiredArgsConstructor
public class UserCardListResult {
    private final Long userCardId;
    private final Long cardId;
    private final String cardName;
    private final String issuerName;
    private final String cardType;
    private final String maskedCardNumber;
    private final String imageUrl;
    private final Boolean representative;
    private final LocalDateTime registeredAt;
}