package com.wallet.member.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordUpdateRequest;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

@RequiredArgsConstructor
@Service
public class SimplePasswordService {
    private final MemberMapper memberMapper;
    private final SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private final TokenHashUtil tokenHashUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void updateSimplePassword(Long memberId, SimplePasswordUpdateRequest request) {
        validateAuthenticatedMemberId(memberId);
        validateSimplePasswordConfirmation(request);

        /*
         * 회원 행과 인증 행을 트랜잭션이 끝날 때까지 잠근다. 같은 토큰으로 요청이 동시에 와도
         * 첫 요청만 저장하고, 다음 요청은 USED 상태를 확인해 거부할 수 있다.
         */
        if (memberMapper.lockActiveMemberById(memberId) == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Member member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        String changeTokenHash = tokenHashUtil.sha256(request.simplePasswordChangeToken());
        SimplePasswordVerification verification =
            simplePasswordVerificationMapper.findByChangeTokenHashForUpdate(changeTokenHash);

        validateChangeToken(verification, memberId);

        String simplePasswordHash = passwordEncoder.encode(request.simplePassword());
        int updatedCount = memberMapper.updateSimplePassword(memberId, simplePasswordHash);
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_UPDATE_FAILED);
        }

        int usedCount = simplePasswordVerificationMapper.markAsUsed(
            verification.getSimplePasswordVerificationId()
        );
        if (usedCount == 0) {
            /*
             * 이 예외가 발생하면 @Transactional이 간편비밀번호 UPDATE도 함께 롤백한다.
             * 따라서 비밀번호만 바뀌고 토큰은 재사용 가능한 불완전한 상태가 남지 않는다.
             */
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_INVALID);
        }
    }

    private void validateAuthenticatedMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private void validateSimplePasswordConfirmation(SimplePasswordUpdateRequest request) {
        if (!request.simplePassword().equals(request.simplePasswordConfirm())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    private void validateChangeToken(
        SimplePasswordVerification verification,
        Long memberId
    ) {
        /*
         * 토큰이 없거나 다른 회원에게 발급된 경우를 같은 오류로 응답해
         * 공격자가 토큰의 존재 여부나 소유 회원을 추측하지 못하게 한다.
         */
        if (verification == null || !memberId.equals(verification.getMemberId())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_INVALID);
        }

        if (VerificationStatus.USED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_ALREADY_USED);
        }

        if (!VerificationStatus.VERIFIED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_INVALID);
        }

        if (verification.getChangeTokenExpiresAt() == null
            || !verification.getChangeTokenExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_EXPIRED);
        }
    }
}
