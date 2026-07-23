package com.wallet.auth.service;

import org.springframework.stereotype.Service;

import com.wallet.auth.dto.LoginMemberResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResponse;
import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
    private final MemberMapper memberMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
        MemberMapper memberMapper,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder
    ) {
        this.memberMapper = memberMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberMapper.findByEmail(request.email());

        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);
        }

        if ("WITHDRAWN".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (!"ACTIVE".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member);

        LoginMemberResponse memberResponse = new LoginMemberResponse(
            member.getMemberId(),
            member.getEmail(),
            member.getName()
        );

        return new LoginResponse(
            accessToken,
            "Bearer",
            jwtTokenProvider.getAccessTokenValidityInSeconds(),
            memberResponse
        );
    }
}