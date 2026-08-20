package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.CardCatalogRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 카드 마스터 조회 — 추천 결과에 이름과 연회비를 붙이기 위한 것.
 */
@Mapper
public interface CardCatalogMapper {

    /**
     * 카드 이름·카드사·최저 연회비를 한 번에 읽는다.
     *
     * 연회비는 브랜드·발급형태별로 행이 여러 개다(국내전용·VISA·모바일…). 그중 <b>최저값</b>을
     * 쓰고 답에도 "최저 연회비 기준"으로 밝힌다 — 회원이 어느 브랜드로 발급할지 모르는 상태라
     * 최저값이 "적어도 이만큼"이라는 하한으로 읽힌다. 연회비 행이 없는 카드는 0으로 본다.
     */
    List<CardCatalogRow> findByCardIds(@Param("cardIds") List<Long> cardIds);
}
