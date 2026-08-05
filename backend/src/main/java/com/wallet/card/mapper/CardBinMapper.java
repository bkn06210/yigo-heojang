package com.wallet.card.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.card.domain.CardBin;

@Mapper
public interface CardBinMapper {
    // BIN prefix로 활성 카드사 매핑 정보를 조회한다.
    CardBin findActiveByPrefix(@Param("binPrefix") String binPrefix);
}