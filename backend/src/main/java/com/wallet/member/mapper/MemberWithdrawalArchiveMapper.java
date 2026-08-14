package com.wallet.member.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.wallet.member.domain.MemberWithdrawalArchive;

@Mapper
public interface MemberWithdrawalArchiveMapper {
    int insert(MemberWithdrawalArchive archive);
}
