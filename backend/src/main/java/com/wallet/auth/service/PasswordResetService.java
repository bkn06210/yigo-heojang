package com.wallet.auth.service;

import java.time.LocalDateTime;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.PasswordResetVerification;
import com.wallet.auth.dto.PasswordResetCodeRequest;
import com.wallet.auth.mapper.PasswordResetVerificationMapper;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.auth.support.VerificationCodeGenerator;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.mapper.MemberMapper;

@RequiredArgsConstructor
@Service
public class PasswordResetService {
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;
    private static final long REISSUE_WAIT_SECONDS = 60;

    private final PasswordResetVerificationMapper passwordResetVerificationMapper;
    private final MemberMapper memberMapper;
    private final EmailSender emailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final TokenHashUtil tokenHashUtil;

    @Transactional
    public void sendResetCode(PasswordResetCodeRequest request) {
        String email = normalizeEmail(request.email());

        Member member = memberMapper.findByEmail(email);

        if (member == null || !isActiveMember(member)) {
            return;
        }

        PasswordResetVerification existingVerification =
            passwordResetVerificationMapper.findLatestByMemberIdForUpdate(
                member.getMemberId()
            );

        validateReissueAllowed(existingVerification);

        String verificationCode = verificationCodeGenerator.generateSixDigitCode();
        String verificationCodeHash = tokenHashUtil.sha256(verificationCode);
        LocalDateTime expiresAt = LocalDateTime.now()
            .plusMinutes(VERIFICATION_CODE_EXPIRE_MINUTES);

        passwordResetVerificationMapper.expireActiveByMemberId(member.getMemberId());

        PasswordResetVerification verification = PasswordResetVerification.createPending(
            member.getMemberId(),
            verificationCodeHash,
            expiresAt
        );

        passwordResetVerificationMapper.insert(verification);

        try {
            emailSender.sendPasswordResetVerificationCode(
                email,
                verificationCode,
                VERIFICATION_CODE_EXPIRE_MINUTES
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void validateReissueAllowed(PasswordResetVerification verification) {
        if (verification == null || verification.getUpdatedAt() == null) {
            return;
        }

        LocalDateTime reissueAllowedAt = verification.getUpdatedAt()
            .plusSeconds(REISSUE_WAIT_SECONDS);

        if (reissueAllowedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_REISSUE_COOLDOWN);
        }
    }

    private boolean isActiveMember(Member member) {
        return "ACTIVE".equals(member.getMemberStatus())
            && member.getWithdrawnAt() == null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}