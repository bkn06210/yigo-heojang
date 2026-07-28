package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.member.domain.Member;

@Mapper
public interface MemberMapper {
    Member findByEmail(String email);

    Member findById(@Param("memberId") Long memberId);
}