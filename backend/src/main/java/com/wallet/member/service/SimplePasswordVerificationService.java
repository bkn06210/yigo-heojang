package com.wallet.member.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.service.EmailSender;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordEmailVerificationResponse;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyResponse;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

@RequiredArgsConstructor
@Service
public class SimplePasswordVerificationService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final long CHANGE_TOKEN_EXPIRE_MINUTES = 10;
    private static final long REISSUE_WAIT_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPT_COUNT = 5;

    private final SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private final MemberMapper memberMapper;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final Sha256Hasher sha256Hasher;

    @Transactional
    public SimplePasswordEmailVerificationResponse sendVerificationCode(Long memberId) {
        Member member = findActiveMember(memberId);

        /*
         * 인증 이력이 아직 없는 회원은 잠글 인증 행이 없다. 회원 행을 먼저 잠그면 같은 회원의
         * 최초 발송 요청 두 개가 동시에 들어와 인증 코드가 중복 발급되는 상황을 막을 수 있다.
         */
        if (memberMapper.lockActiveMemberById(memberId) == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        SimplePasswordVerification existingVerification =
            simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(memberId);

        validateReissueAllowed(existingVerification);

        String verificationCode = verificationCodeGenerator.generateSixDigitCode();
        String verificationCodeHash = sha256Hasher.sha256(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusMinutes(VERIFICATION_CODE_EXPIRE_MINUTES);

        simplePasswordVerificationMapper.expireActiveByMemberId(memberId);

        SimplePasswordVerification verification = SimplePasswordVerification.createPending(
            memberId,
            verificationCodeHash,
            expiresAt
        );
        simplePasswordVerificationMapper.insert(verification);

        try {
            emailSender.sendSimplePasswordVerificationCode(
                member.getEmail(),
                verificationCode,
                VERIFICATION_CODE_EXPIRE_MINUTES
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }

        return new SimplePasswordEmailVerificationResponse(
            maskEmail(member.getEmail()),
            VERIFICATION_CODE_EXPIRE_MINUTES * 60
        );
    }

    /*
     * BusinessException이 발생해도 실패 횟수 증가와 만료 처리는 DB에 남아야 한다.
     * 기본 트랜잭션 설정은 RuntimeException에서 롤백하므로 noRollbackFor로 예외 처리 결과를 보존한다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public SimplePasswordEmailVerificationVerifyResponse verifyCode(
        Long memberId,
        SimplePasswordEmailVerificationVerifyRequest request
    ) {
        findActiveMember(memberId);

        SimplePasswordVerification verification =
            simplePasswordVerificationMapper.findLatestByMemberIdForUpdate(memberId);

        validateVerificationExists(verification);
        validateVerificationStatus(verification);
        validateAttemptLimit(verification);
        validateCodeNotExpired(verification);

        String requestCodeHash = sha256Hasher.sha256(request.verificationCode());
        if (!verification.getVerificationCodeHash().equals(requestCodeHash)) {
            handleCodeMismatch(verification);
        }

        String changeToken = verificationTokenGenerator.generate();
        String changeTokenHash = sha256Hasher.sha256(changeToken);
        LocalDateTime changeTokenExpiresAt = LocalDateTime.now()
            .plusMinutes(CHANGE_TOKEN_EXPIRE_MINUTES);

        int updatedCount = simplePasswordVerificationMapper.verify(
            verification.getSimplePasswordVerificationId(),
            changeTokenHash,
            changeTokenExpiresAt
        );
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_FAILED);
        }

        return new SimplePasswordEmailVerificationVerifyResponse(
            changeToken,
            CHANGE_TOKEN_EXPIRE_MINUTES * 60
        );
    }

    private Member findActiveMember(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }

        Member member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if ("WITHDRAWN".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        if (!"ACTIVE".equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        return member;
    }

    private void validateReissueAllowed(SimplePasswordVerification verification) {
        if (verification == null || verification.getUpdatedAt() == null) {
            return;
        }

        LocalDateTime reissueAllowedAt = verification.getUpdatedAt()
            .plusSeconds(REISSUE_WAIT_SECONDS);
        if (reissueAllowedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_REISSUE_COOLDOWN);
        }
    }

    private void validateVerificationExists(SimplePasswordVerification verification) {
        if (verification == null) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_NOT_FOUND);
        }
    }

    private void validateVerificationStatus(SimplePasswordVerification verification) {
        if (VerificationStatus.EXPIRED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_EXPIRED);
        }
        if (VerificationStatus.VERIFIED.equals(verification.getVerificationStatus())
            || VerificationStatus.USED.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_ALREADY_USED);
        }
        if (!VerificationStatus.PENDING.equals(verification.getVerificationStatus())) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_INVALID);
        }
    }

    private void validateAttemptLimit(SimplePasswordVerification verification) {
        if (verification.getFailedAttemptCount() >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(
                ErrorCode.SIMPLE_PASSWORD_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED
            );
        }
    }

    private void validateCodeNotExpired(SimplePasswordVerification verification) {
        if (!verification.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now())) {
            simplePasswordVerificationMapper.expireVerificationCode(
                verification.getSimplePasswordVerificationId()
            );
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_EXPIRED);
        }
    }

    private void handleCodeMismatch(SimplePasswordVerification verification) {
        simplePasswordVerificationMapper.increaseFailedAttemptCount(
            verification.getSimplePasswordVerificationId()
        );

        if (verification.getFailedAttemptCount() + 1 >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(
                ErrorCode.SIMPLE_PASSWORD_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED
            );
        }
        throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_INVALID);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        int visibleLength = Math.min(2, localPart.length());

        return localPart.substring(0, visibleLength) + "***" + domainPart;
    }
}
