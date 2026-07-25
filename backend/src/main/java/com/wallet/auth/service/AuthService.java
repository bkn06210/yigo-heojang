package com.wallet.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.dto.LoginMemberResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

@Service
public class AuthService {
    private final MemberMapper memberMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
        MemberMapper memberMapper,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder,
        RefreshTokenService refreshTokenService
    ) {
        this.memberMapper = memberMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = memberMapper.findByEmail(request.email());

        validateLoginMember(request, member);

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
            .plusSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds());

        refreshTokenService.replace(
            member.getMemberId(),
            refreshToken,
            refreshTokenExpiresAt
        );

        LoginMemberResponse memberResponse = new LoginMemberResponse(
            member.getMemberId(),
            member.getEmail(),
            member.getName()
        );

        return new LoginResult(
            accessToken,
            refreshToken,
            "Bearer",
            jwtTokenProvider.getAccessTokenValidityInSeconds(),
            jwtTokenProvider.getRefreshTokenValidityInSeconds(),
            memberResponse
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse reissueAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_FAILED);
        }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_FAILED);
        }

        Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);

        RefreshToken savedToken = refreshTokenService.findValidToken(refreshToken);

        if (savedToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_FAILED);
        }

        if (!memberId.equals(savedToken.getMemberId())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_FAILED);
        }

        Member member = memberMapper.findById(memberId);

        if (member == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_FAILED);
        }

        validateActiveMember(member);

        String accessToken = jwtTokenProvider.createAccessToken(member);

        return new TokenResponse(
            accessToken,
            "Bearer",
            jwtTokenProvider.getAccessTokenValidityInSeconds()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }

        refreshTokenService.revokeByToken(refreshToken);
    }

    private void validateLoginMember(LoginRequest request, Member member) {
        if (member == null) {
            throw new BusinessException(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);
        }

        validateActiveMember(member);
    }

    private void validateActiveMember(Member member) {
        if ("WITHDRAWN".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }

        if (!"ACTIVE".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
    }
}