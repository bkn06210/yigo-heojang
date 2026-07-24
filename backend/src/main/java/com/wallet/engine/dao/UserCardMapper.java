package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.UserCardRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 보유카드 조회 매퍼 (엔진이 쓰는 최소 조회).
 *
 * 지금은 userCardId→cardId를 잇는 용도지만, 5번(추천)의 보유카드 목록 조회로도 재사용된다.
 */
@Mapper
public interface UserCardMapper {

    /** 회원의 활성 보유카드 목록(user_card_id, card_id)을 조회한다. */
    List<UserCardRow> findActiveCards(@Param("memberId") long memberId);
}
