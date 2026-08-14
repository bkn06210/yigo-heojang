package com.wallet.member.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.MemberTermAgreement;
import com.wallet.auth.domain.TermScope;
import com.wallet.auth.mapper.TermAgreementMapper;
import com.wallet.auth.service.RefreshTokenService;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.MemberWithdrawal;
import com.wallet.member.domain.MemberWithdrawalArchive;
import com.wallet.member.dto.MemberMeResponse;
import com.wallet.member.dto.MemberUpdateRequest;
import com.wallet.member.dto.MemberWithdrawRequest;
import com.wallet.member.mapper.CreditInfoWithdrawalMapper;
import com.wallet.member.mapper.MemberDataCleanupMapper;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.MemberWithdrawalArchiveMapper;
import com.wallet.member.mapper.MemberWithdrawalMapper;

@RequiredArgsConstructor
@Service
public class MemberService {
    private static final String ANONYMIZED_EMAIL_DOMAIN = "@deleted.local";
    private static final String ANONYMIZED_EMAIL_PREFIX = "withdrawn_";
    private static final String ANONYMIZED_DISPLAY_NAME = "탈퇴회원";
    private static final String ANONYMIZED_PASSWORD_HASH = "WITHDRAWN_MEMBER_CANNOT_LOGIN";

    // 정확한 문구는 법무 검토가 필요하다.
    private static final String RETENTION_REASON =
        "재가입 어뷰징 방지 및 CS 대응을 위한 보관. 탈퇴 시 회원에게 고지하고 동의를 받음";

    private final MemberMapper memberMapper;
    private final MemberWithdrawalMapper memberWithdrawalMapper;
    private final MemberWithdrawalArchiveMapper memberWithdrawalArchiveMapper;
    private final CreditInfoWithdrawalMapper creditInfoWithdrawalMapper;
    private final MemberDataCleanupMapper memberDataCleanupMapper;
    private final TermAgreementMapper termAgreementMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final Sha256Hasher sha256Hasher;

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


    // 현재 로그인한 회원의 닉네임을 수정합니다.
    @Transactional
    public MemberMeResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        validateAuthenticatedMemberId(memberId);

        int updatedCount = memberMapper.updateMemberInfo(
            memberId,
            request.nickname()
        );
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Member updatedMember = memberMapper.findById(memberId);

        return MemberMeResponse.from(updatedMember);
    }

    @Transactional
    public void withdraw(Long memberId, MemberWithdrawRequest request) {
        // 1. 회원 행을 잠근다. 대표 카드 변경 로직(UserCardService.lockActiveMember)과
        // 동일한 조건(ACTIVE + withdrawn_at IS NULL)이라, 이미 탈퇴한 회원이
        // 다시 탈퇴를 시도하면 여기서 null을 반환해 MEMBER_NOT_FOUND로 막힌다.
        Long lockedMemberId = memberMapper.lockActiveMemberById(memberId);
        if (lockedMemberId == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 2. 익명화되기 전에 원본 정보를 읽어둔다. 이 시점 이후로는 member
        // 테이블의 email·name을 다시 조회해도 마스킹된 값만 나오게 되므로,
        // archive에 넣을 원본은 지금 확보해야 한다.
        Member member = memberMapper.findById(memberId);

        validatePassword(request.password(), member.getPassword());
        validateWithdrawalTermAgreement(request.termVersionId());

        // 이후 여러 테이블에 기록되는 시각을 하나로 통일한다.
        // member.withdrawn_at, member_withdrawal.withdrawn_at,
        // member_withdrawal_archive.withdrawn_at, purge_scheduled_at 계산까지
        // 전부 이 값을 기준으로 삼는다.
        LocalDateTime withdrawnAt = LocalDateTime.now();

        // 3. 탈퇴 고지 약관에 동의했다는 이력을 남긴다. 회원가입과 같은
        // insertMemberTermAgreements를 재사용한다 — 원소 하나짜리 리스트를
        // 넘기면 동일하게 동작한다.
        termAgreementMapper.insertMemberTermAgreements(
            List.of(new MemberTermAgreement(memberId, request.termVersionId(), true))
        );

        // 4. 알림 관련 데이터를 가장 먼저 지운다. notification이 아래
        // 신용정보 그룹의 point_history·user_card를 FK로 참조하고 있어서,
        // 반드시 이 그룹 전체가 신용정보 삭제보다 먼저 끝나야 한다.
        memberDataCleanupMapper.deleteNotificationByMemberId(memberId);
        memberDataCleanupMapper.deleteNotificationSettingByMemberId(memberId);
        memberDataCleanupMapper.deleteMemberPreferredCategoryByMemberId(memberId);
        memberDataCleanupMapper.deleteMemberPreferredMerchantByMemberId(memberId);
        memberDataCleanupMapper.deleteMemberPersonalizationBrandByMemberId(memberId);
        memberDataCleanupMapper.deleteMemberPersonalizationCategoryByMemberId(memberId);
        memberDataCleanupMapper.deletePasswordResetVerificationByMemberId(memberId);

        // 5. 신용정보를 삭제한다. 이 순서(FK 자식 → 부모)는
        // CreditInfoWithdrawalMapper 인터페이스 주석에 상세히 설명되어 있다.
        creditInfoWithdrawalMapper.deletePaymentQrByMemberId(memberId);
        creditInfoWithdrawalMapper.deletePaymentByMemberId(memberId);
        creditInfoWithdrawalMapper.deletePointHistoryByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteExpenseByMemberId(memberId);
        creditInfoWithdrawalMapper.deletePointWalletByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteUserCardBenefitSelectionByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteUserCardMonthlyStateByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteUserBenefitUsageByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteUserCardByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteMembershipRegisterByMemberId(memberId);
        creditInfoWithdrawalMapper.deleteRecommendInputByMemberId(memberId);

        // 6. member 자신을 익명화한다.
        // 논리적으로 "다른 도메인 정리 → 회원 자신 처리"
        // 순서가 자연스러워 이 위치에 둔다.
        memberMapper.anonymizeMember(
            memberId,
            buildAnonymizedEmail(memberId),
            ANONYMIZED_DISPLAY_NAME,
            ANONYMIZED_DISPLAY_NAME,
            ANONYMIZED_PASSWORD_HASH,
            withdrawnAt
        );

        // 7. 탈퇴 사유를 기록한다.
        memberWithdrawalMapper.insert(
            MemberWithdrawal.create(
                memberId,
                request.reasonType(),
                request.reasonDetail(),
                withdrawnAt
            )
        );

        // 8. 원본 개인정보를 보관 테이블로 옮긴다. 2번에서 읽어둔
        // member(익명화되기 전 원본)의 email·name을 사용한다.
        String emailHash = sha256Hasher.sha256(member.getEmail());
        memberWithdrawalArchiveMapper.insert(
            MemberWithdrawalArchive.of(
                memberId,
                member.getEmail(),
                emailHash,
                member.getName(),
                withdrawnAt,
                RETENTION_REASON
            )
        );

        // 9. 모든 활성 세션을 종료한다. 데이터 정리가 전부 끝난 뒤 마지막에
        // 실행해, 트랜잭션 중간에 실패하더라도(전체 롤백되므로) 세션이
        // 먼저 끊기고 데이터는 안 지워지는 어중간한 상태가 남지 않는다.
        refreshTokenService.revokeAllByWithdrawal(memberId);
    }

    // 인증 필터에서 memberId를 정상적으로 전달했는지 확인
    private void validateAuthenticatedMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);
        }
    }

    // 프론트가 제출한 termVersionId가 "지금 실제로 유효한 탈퇴 고지 약관"이
    // 맞는지 검증한다. `탈퇴 고지 약관 조회`에서 내려준 값을 그대로
    // 믿지 않는 이유는, 조회 시점과 탈퇴 실행 시점 사이에 약관이 개정될
    // 수 있기 때문이다.
    private void validateWithdrawalTermAgreement(Long termVersionId) {
        List<Long> requiredWithdrawalTermVersionIds =
            termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL);

        if (!requiredWithdrawalTermVersionIds.contains(termVersionId)) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
    }

    private String buildAnonymizedEmail(Long memberId) {
        return ANONYMIZED_EMAIL_PREFIX + memberId + ANONYMIZED_EMAIL_DOMAIN;
    }
}