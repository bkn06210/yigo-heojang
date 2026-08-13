package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시연용 전체 카드번호와 카드 상품의 매핑 정보.
 *
 * 실제 발급 카드가 아닌 개발·시연용 가상 카드만 이 도메인으로 다룬다.
 */
@Getter
@RequiredArgsConstructor
public class MockCard {
    private final Long mockCardId;
    private final Long cardId;
}
