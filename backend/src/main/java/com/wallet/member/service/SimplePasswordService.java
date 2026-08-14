package com.wallet.member.service;

import java.time.Clock;
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
import com.wallet.member.dto.SimplePasswordVerifyRequest;
import com.wallet.member.dto.SimplePasswordVerifyResponse;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

/**
 * 간편비밀번호 저장과 검증을 담당한다.
 * <p>
 * 저장은 SimplePasswordVerificationService가 발급한 일회용 변경 토큰이 있어야만 가능하다.
 * 검증은 6자리라 전수 조사가 쉬우므로, 연속 5회 실패하면 5분간 잠근다.
 */
@RequiredArgsConstructor
@Service
public class SimplePasswordService {
    private static final int MAX_FAILED_ATTEMPT_COUNT = 5;
    private static final long LOCK_MINUTES = 5;

    private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";
    private static final String MEMBER_STATUS_WITHDRAWN = "WITHDRAWN";

    private final MemberMapper memberMapper;
    private final SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private final TokenHashUtil tokenHashUtil;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /** 최초 설정과 기존 간편비밀번호 변경 모두 이 메서드를 쓴다. */
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

    /*
     * 실패 횟수 증가와 잠금 상태를 저장해야 하므로 읽기 전용 트랜잭션을 쓰지 않는다.
     * 또한 잠금 예외를 던지면서도 그 직전의 실패 기록은 남아야 하므로 noRollbackFor를 건다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public SimplePasswordVerifyResponse verifySimplePassword(
        Long memberId,
        SimplePasswordVerifyRequest request
    ) {
        validateAuthenticatedMemberId(memberId);

        /* 회원 행을 잠가 동시에 들어온 검사 요청이 실패 횟수를 덮어쓰지 못하게 한다. */
        if (memberMapper.lockActiveMemberById(memberId) == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Member member = findVerifiableMember(memberId);

        LocalDateTime now = LocalDateTime.now(clock);
        validateNotLocked(member, now);

        /*
         * 잠금 시각은 지났는데 카운트가 5로 남아 있는 상태다. 그대로 두면 다음 실패 한 번에
         * 곧바로 다시 잠기므로, 잠금이 풀린 시점에 카운트도 0으로 되돌린다.
         */
        boolean lockJustExpired = isExpiredLockState(member, now);
        int failedAttemptCount = lockJustExpired ? 0 : currentFailedAttemptCount(member);
        if (lockJustExpired) {
            resetVerificationFailure(memberId);
        }

        /*
         * BCrypt는 같은 원문도 매번 다른 해시가 만들어지므로 문자열끼리 비교하면 안 된다.
         * matches()가 요청 원문을 저장된 해시의 salt와 비용 설정으로 다시 계산해 비교한다.
         */
        boolean matched = passwordEncoder.matches(
            request.simplePassword(),
            member.getSimplePasswordHash()
        );

        if (matched) {
            boolean hasFailureStateToClear =
                failedAttemptCount > 0 || member.getSimplePasswordLockedUntil() != null;
            if (!lockJustExpired && hasFailureStateToClear) {
                resetVerificationFailure(memberId);
            }
            return new SimplePasswordVerifyResponse(true);
        }

        handleMismatch(memberId, failedAttemptCount, now);

        return new SimplePasswordVerifyResponse(false);
    }

    private Member findVerifiableMember(Long memberId) {
        Member member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (MEMBER_STATUS_WITHDRAWN.equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        if (!MEMBER_STATUS_ACTIVE.equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        if (member.getSimplePasswordHash() == null) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_NOT_SET);
        }
        return member;
    }

    private void validateNotLocked(Member member, LocalDateTime now) {
        if (member.getSimplePasswordLockedUntil() != null
            && member.getSimplePasswordLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    private boolean isExpiredLockState(Member member, LocalDateTime now) {
        return member.getSimplePasswordLockedUntil() != null
            && !member.getSimplePasswordLockedUntil().isAfter(now);
    }

    private int currentFailedAttemptCount(Member member) {
        return member.getSimplePasswordFailedAttemptCount() == null
            ? 0
            : member.getSimplePasswordFailedAttemptCount();
    }

    private void handleMismatch(Long memberId, int failedAttemptCount, LocalDateTime now) {
        boolean reachesLimit = failedAttemptCount + 1 >= MAX_FAILED_ATTEMPT_COUNT;

        int updatedCount = reachesLimit
            ? memberMapper.lockSimplePasswordVerification(memberId, now.plusMinutes(LOCK_MINUTES))
            : memberMapper.increaseSimplePasswordFailedAttemptCount(memberId);

        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_UPDATE_FAILED);
        }

        if (reachesLimit) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    private void resetVerificationFailure(Long memberId) {
        if (memberMapper.resetSimplePasswordVerificationFailure(memberId) == 0) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_ATTEMPT_UPDATE_FAILED);
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

    private void validateChangeToken(SimplePasswordVerification verification, Long memberId) {
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
            || !verification.getChangeTokenExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_EXPIRED);
        }
    }
}
