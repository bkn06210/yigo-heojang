package com.wallet.member.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.mapper.MemberMapper;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberMapper memberMapper;


    // 현재 로그인한 회원의 정보를 조회
    @Transactional(readOnly = true)
    public MemberMeResponse getMyInfo(Long memberId) {
        validateAuthenticatedMemberId(memberId);

        Member member = memberMapper.findById(memberId);

        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return MemberMeResponse.from(member);
    }


    // 인증 필터에서 memberId를 정상적으로 전달했는지 확인
    private void validateAuthenticatedMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }
}