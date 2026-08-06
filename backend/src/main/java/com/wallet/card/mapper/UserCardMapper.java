package com.wallet.card.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import com.wallet.card.domain.UserCard;
import com.wallet.card.domain.UserCardListResult;
import com.wallet.card.domain.UserCardRegistrationResult;

@Mapper
@Component("cardUserCardMapper") // 빈 이름 충돌 회피용. 다른 UserCardMapper(com.wallet.engine.dao)와 이름이 겹쳐서 임시로 추가함
public interface UserCardMapper {
    /**
     * 회원과 카드 상품 기준으로 기존 보유 카드 행을 조회한다.
     * <p>
     * ACTIVE 카드인지 DELETED 카드인지 판단해야 하므로,
     * card_status 조건을 걸지 않고 기존 행 자체를 조회한다.
     */
    UserCard findByMemberIdAndCardId(
        @Param("memberId") Long memberId,
        @Param("cardId") Long cardId
    );

    /**
     * 신규 보유 카드를 등록한다.
     * <p>
     * 전체 카드번호는 저장하지 않고,
     * 서비스에서 만든 마스킹 카드번호만 저장한다.
     */
    int insertUserCard(
        @Param("memberId") Long memberId,
        @Param("cardId") Long cardId,
        @Param("maskedCardNumber") String maskedCardNumber
    );

    /**
     * 삭제 상태의 보유 카드를 다시 활성화한다.
     * <p>
     * memberId 조건을 함께 걸어 다른 회원의 user_card 행이
     * 실수로 변경되지 않도록 방어한다.
     */
    int reactivateUserCard(
        @Param("userCardId") Long userCardId,
        @Param("memberId") Long memberId,
        @Param("maskedCardNumber") String maskedCardNumber
    );

    /**
     * 등록 또는 재활성화 완료 후 화면에 내려줄 보유 카드 정보를 조회한다.
     * <p>
     * memberId와 cardId를 함께 조건으로 사용해서,
     * 반드시 로그인 회원의 보유 카드만 조회한다.
     */
    UserCardRegistrationResult findRegistrationResult(
        @Param("memberId") Long memberId,
        @Param("cardId") Long cardId
    );

    /**
     * 로그인 회원이 보유한 활성 카드 목록을 조회한다.
     * <p>
     * memberId를 조회 조건으로 사용해서 다른 회원의 보유 카드가
     * 결과에 포함되지 않도록 DB 조회 단계에서 접근 범위를 제한한다.
     * <p>
     * 조회 결과가 없으면 MyBatis가 null이 아닌 빈 List를 반환한다.
     */
    List<UserCardListResult> findActiveUserCardsByMemberId(
        @Param("memberId") Long memberId
    );

    /**
     * 로그인 회원이 소유한 활성 보유 카드를 삭제 상태로 변경한다.
     * <p>
     * userCardId와 memberId를 함께 조건으로 사용해서
     * 다른 회원이 소유한 카드가 변경되지 않도록 제한한다.
     * <p>
     * 반환값은 실제로 변경된 행의 개수이다.
     * 1이면 삭제 성공이고, 0이면 삭제 가능한 카드가 없다는 의미이다.
     */
    int softDeleteByIdAndMemberId(
        @Param("userCardId") Long userCardId,
        @Param("memberId") Long memberId
    );
}