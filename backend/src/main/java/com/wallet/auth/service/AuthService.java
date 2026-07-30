package com.wallet.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.MemberTermAgreement;
import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.dto.LoginMemberResponse;
import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.LoginResult;
import com.wallet.auth.dto.SignupRequest;
import com.wallet.auth.dto.SignupResponse;
import com.wallet.auth.dto.TermAgreementRequest;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.jwt.JwtTokenProvider;
import com.wallet.auth.mapper.TermAgreementMapper;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

@RequiredArgsConstructor
@Service
public class AuthService {
    private static final String[] NICKNAME_ADJECTIVES = {
        "든든한", "똑똑한", "알뜰한", "빠른", "차분한", "성실한"
    };

    private static final String[] NICKNAME_NOUNS = {
        "밥그릇", "카드", "충전기", "얼룩말", "고래", "토끼"
    };

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberMapper memberMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final TermAgreementMapper termAgreementMapper;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateEmailNotDuplicated(request.email());
        validateTermsAgreements(request.termsAgreements());

        String encodedPassword = passwordEncoder.encode(request.password());
        String nickname = generateRandomNickname();

        Member member = Member.createSignupMember(
            request.email(),
            encodedPassword,
            request.name(),
            nickname
        );

        memberMapper.insertMember(member);

        List<MemberTermAgreement> agreements = request.termsAgreements()
            .stream()
            .map(termAgreement -> new MemberTermAgreement(
                member.getMemberId(),
                termAgreement.termsVersionId(),
                termAgreement.agreed()
            ))
            .toList();

        termAgreementMapper.insertMemberTermAgreements(agreements);

        Member savedMember = memberMapper.findById(member.getMemberId());

        return SignupResponse.from(savedMember);
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
            member.getName(),
            member.getNickname()
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

    private void validateEmailNotDuplicated(String email) {
        if (memberMapper.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateTermsAgreements(List<TermAgreementRequest> termsAgreements) {
        Set<Long> requestedTermVersionIds = termsAgreements.stream()
            .map(TermAgreementRequest::termsVersionId)
            .collect(Collectors.toSet());
        if (requestedTermVersionIds.size() != termsAgreements.size()) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }

        int activeTermVersionCount = termAgreementMapper.countActiveTermVersionsByIds(
            List.copyOf(requestedTermVersionIds)
        );
        if (activeTermVersionCount != requestedTermVersionIds.size()) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }

        Set<Long> agreedTermVersionIds = termsAgreements.stream()
            .filter(TermAgreementRequest::agreed)
            .map(TermAgreementRequest::termsVersionId)
            .collect(Collectors.toSet());

        List<Long> requiredTermVersionIds = termAgreementMapper.findActiveRequiredTermVersionIds();

        if (!agreedTermVersionIds.containsAll(requiredTermVersionIds)) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
    }

    private String generateRandomNickname() {
        String adjective = NICKNAME_ADJECTIVES[RANDOM.nextInt(NICKNAME_ADJECTIVES.length)];
        String noun = NICKNAME_NOUNS[RANDOM.nextInt(NICKNAME_NOUNS.length)];
        int number = RANDOM.nextInt(10_000);
        
        return adjective + noun + String.format("%04d", number);
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