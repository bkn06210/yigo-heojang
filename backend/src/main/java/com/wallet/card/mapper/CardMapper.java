package com.wallet.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.card.domain.Card;

@Mapper
public interface CardMapper {
    // 카드사 ID 기준으로 활성 카드 상품 목록을 조회한다.
    List<Card> findActiveCardsByCompanyId(
        @Param("cardCompanyId") Long cardCompanyId
    );

    // 실제 활성 카드 상품인지 확인하기 위한 조회 메서드
    Card findActiveById(@Param("cardId") Long cardId);
}