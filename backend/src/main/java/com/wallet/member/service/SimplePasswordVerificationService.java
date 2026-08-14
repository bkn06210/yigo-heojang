package com.wallet.member.service;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.VerificationStatus;
import com.wallet.auth.service.EmailSender;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.auth.support.VerificationTokenGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.SimplePasswordVerification;
import com.wallet.member.dto.SimplePasswordEmailVerificationResponse;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyRequest;
import com.wallet.member.dto.SimplePasswordEmailVerificationVerifyResponse;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.SimplePasswordVerificationMapper;

/**
 * 간편비밀번호를 설정·변경하기 전에 거치는 이메일 인증을 담당한다.
 * <p>
 * 로그인 상태만으로 간편비밀번호를 바꾸게 두지 않는 이유는, 기기를 잠깐 빼앗긴 상황에서도
 * 결제 수단을 잠글 수 있어야 하기 때문이다. 회원 본인의 이메일로 코드를 보내고,
 * 그 코드를 맞힌 사람에게만 일회용 변경 토큰을 준다.
 */
@RequiredArgsConstructor
@Service
public class SimplePasswordVerificationService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final long CHANGE_TOKEN_EXPIRE_MINUTES = 10;
    private static final long REISSUE_WAIT_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPT_COUNT = 5;

    private static final String MEMBER_STATUS_ACTIVE = "ACTIVE";
    private static final String MEMBER_STATUS_WITHDRAWN = "WITHDRAWN";

    private final SimplePasswordVerificationMapper simplePasswordVerificationMapper;
    private final MemberMapper memberMapper;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final TokenHashUtil tokenHashUtil;
    private final Clock clock;

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
        String verificationCodeHash = tokenHashUtil.sha256(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now(clock)
            .plusMinutes(VERIFICATION_CODE_EXPIRE_MINUTES);

        // 새 코드를 발급하면 이전 코드와 아직 쓰지 않은 변경 토큰은 전부 무효가 된다.
        simplePasswordVerificationMapper.expireActiveByMemberId(memberId);

        simplePasswordVerificationMapper.insert(
            SimplePasswordVerification.createPending(
                memberId,
                verificationCodeHash,
                expiresAt
            )
        );

        try {
            emailSender.sendSimplePasswordVerificationCode(
                member.getEmail(),
                verificationCode,
                VERIFICATION_CODE_EXPIRE_MINUTES
            );
        } catch (RuntimeException e) {
            // 메일 발송에 실패하면 위에서 만든 인증 행도 롤백돼야 한다.
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

        String requestCodeHash = tokenHashUtil.sha256(request.verificationCode());
        if (!verification.getVerificationCodeHash().equals(requestCodeHash)) {
            handleCodeMismatch(verification);
        }

        String changeToken = verificationTokenGenerator.generate();
        String changeTokenHash = tokenHashUtil.sha256(changeToken);
        LocalDateTime changeTokenExpiresAt = LocalDateTime.now(clock)
            .plusMinutes(CHANGE_TOKEN_EXPIRE_MINUTES);

        int updatedCount = simplePasswordVerificationMapper.verify(
            verification.getSimplePasswordVerificationId(),
            changeTokenHash,
            changeTokenExpiresAt
        );
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_FAILED);
        }

        // 원문 토큰은 이 응답으로 딱 한 번만 나가고, DB에는 해시만 남는다.
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
        if (MEMBER_STATUS_WITHDRAWN.equals(member.getMemberStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_WITHDRAWN);
        }
        if (!MEMBER_STATUS_ACTIVE.equals(member.getMemberStatus())) {
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
        if (reissueAllowedAt.isAfter(LocalDateTime.now(clock))) {
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
        if (!verification.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now(clock))) {
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

        // 조회해둔 객체는 증가 전 값이므로 +1해서 이번 실패까지 반영해 판단한다.
        if (verification.getFailedAttemptCount() + 1 >= MAX_FAILED_ATTEMPT_COUNT) {
            throw new BusinessException(
                ErrorCode.SIMPLE_PASSWORD_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED
            );
        }
        throw new BusinessException(ErrorCode.SIMPLE_PASSWORD_VERIFICATION_CODE_INVALID);
    }

    // 어느 주소로 보냈는지 확인은 되면서 전체 주소는 노출되지 않도록 가린다.
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
