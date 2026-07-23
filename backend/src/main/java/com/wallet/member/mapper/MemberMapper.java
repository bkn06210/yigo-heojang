package com.wallet.member.mapper;

import com.wallet.member.domain.Member;

public interface MemberMapper {
    Member findByEmail(String email);
}