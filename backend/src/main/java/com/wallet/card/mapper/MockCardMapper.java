package com.wallet.card.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.card.domain.MockCard;

@Mapper
public interface MockCardMapper {

    /**
     * 공백과 하이픈이 제거된 전체 카드번호로 등록 가능한 Mock 카드를 조회한다.
     */
    MockCard findActiveByCardNumber(
        @Param("cardNumber") String cardNumber
    );
}
