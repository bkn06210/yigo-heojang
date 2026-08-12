package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.member.domain.Member;

@Mapper
public interface MemberMapper {
    Member findByEmail(String email);

    Member findById(@Param("memberId") Long memberId);

    boolean existsByEmail(@Param("email") String email);

    /**JwtAuthenticationFilter가 매 요청마다 "이 회원이 지금 탈퇴 상태인지"만
     * 확인하기 위한 조회 쿼리.
     * 존재하지 않는 회원이면 null을 반환한다 (member_status 값 자체가 없으므로).
     */
    String findStatusById(@Param("memberId") Long memberId);
    
    int insertMember(Member member);

    int updateMemberInfo(
        @Param("memberId") Long memberId,
        @Param("nickname") String nickname
    );

    int updatePassword(
        @Param("memberId") Long memberId,
        @Param("encodedPassword") String encodedPassword
    );

    /**
     * 대표 카드 변경 작업 동안 회원 행을 잠근다.
     * <p>
     * 같은 회원이 동시에 여러 대표 카드 설정 요청을 보내면,
     * 둘 다 "현재 대표 카드 2개"라고 판단하고 각각 설정해서
     * 최종적으로 4개가 되는 문제가 생길 수 있다.
     * <p>
     * 회원 행을 먼저 잠그면 같은 회원의 대표 카드 변경 요청이
     * 한 번에 하나씩 처리되므로, 최대 3개 제한을 안전하게 지킬 수 있다.
     */
    Long lockActiveMemberById(@Param("memberId") Long memberId);
}