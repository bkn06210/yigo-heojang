package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.wallet.member.domain.MemberWithdrawal;

// member_withdrawal 테이블 자체를 다루는 매퍼.
// 이 매퍼는 "탈퇴 사유를 기록한다"는 member_withdrawal 도메인 고유의 책임만 가진다.
@Mapper
public interface MemberWithdrawalMapper {
    int insert(MemberWithdrawal memberWithdrawal);
}