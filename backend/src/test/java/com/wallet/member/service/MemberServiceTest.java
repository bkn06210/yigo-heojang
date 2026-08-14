package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.wallet.auth.domain.TermScope;
import com.wallet.auth.mapper.TermAgreementMapper;
import com.wallet.auth.service.RefreshTokenService;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.common.util.Sha256Hasher;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.MemberWithdrawalArchive;
import com.wallet.member.domain.WithdrawalReasonType;
import com.wallet.member.dto.MemberWithdrawRequest;
import com.wallet.member.mapper.CreditInfoWithdrawalMapper;
import com.wallet.member.mapper.MemberDataCleanupMapper;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.MemberWithdrawalArchiveMapper;
import com.wallet.member.mapper.MemberWithdrawalMapper;

class MemberServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final String RAW_PASSWORD = "raw-password";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String ORIGINAL_EMAIL = "user@example.com";
    private static final String ORIGINAL_NAME = "아무개";
    private static final Long WITHDRAWAL_TERM_VERSION_ID = 5L;

    private final MemberMapper memberMapper = mock(MemberMapper.class);
    private final MemberWithdrawalMapper memberWithdrawalMapper = mock(MemberWithdrawalMapper.class);
    private final MemberWithdrawalArchiveMapper memberWithdrawalArchiveMapper =
        mock(MemberWithdrawalArchiveMapper.class);
    private final CreditInfoWithdrawalMapper creditInfoWithdrawalMapper =
        mock(CreditInfoWithdrawalMapper.class);
    private final MemberDataCleanupMapper memberDataCleanupMapper = mock(MemberDataCleanupMapper.class);
    private final TermAgreementMapper termAgreementMapper = mock(TermAgreementMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final Sha256Hasher sha256Hasher = mock(Sha256Hasher.class);

    private final MemberService memberService = new MemberService(
        memberMapper,
        memberWithdrawalMapper,
        memberWithdrawalArchiveMapper,
        creditInfoWithdrawalMapper,
        memberDataCleanupMapper,
        termAgreementMapper,
        passwordEncoder,
        refreshTokenService,
        sha256Hasher
    );

    // Member는 setter가 없고 createSignupMember 정적 팩토리 또는 MyBatis
    // 리플렉션으로만 필드가 채워지는 도메인 객체다. 테스트에서 memberId까지
    // 채운 완성된 회원이 필요할 때는, 프로덕션 코드에 테스트 전용 생성자를
    // 추가하는 대신 ReflectionTestUtils로 필드를 직접 채운다.
    private Member buildActiveMember() {
        Member member = Member.createSignupMember(
            ORIGINAL_EMAIL,
            ENCODED_PASSWORD,
            ORIGINAL_NAME,
            "별명A"
        );
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private MemberWithdrawRequest buildRequest() {
        return new MemberWithdrawRequest(
            RAW_PASSWORD,
            WithdrawalReasonType.LOW_USAGE,
            "자주 사용하지 않아서 탈퇴합니다.",
            WITHDRAWAL_TERM_VERSION_ID
        );
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 데이터 정리, 익명화, 이력 기록, 세션 폐기가 정해진 순서로 모두 실행된다")
    void withdraw_success() {
        // given
        Member member = buildActiveMember();

        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(WITHDRAWAL_TERM_VERSION_ID));
        when(sha256Hasher.sha256(ORIGINAL_EMAIL)).thenReturn("hashed-email");

        // when
        memberService.withdraw(MEMBER_ID, buildRequest());

        /*
         * 알림(notification) 삭제가 신용정보(point_history, user_card) 삭제보다
         * 먼저 실행되지 않으면 FK 위반이 나는 게 커밋 7-2에서 확인한 제약이다.
         * 이 제약이 실제 코드에서도 지켜지는지 InOrder로 검증한다.
         */
        InOrder inOrder = Mockito.inOrder(
            memberDataCleanupMapper,
            creditInfoWithdrawalMapper,
            memberMapper,
            memberWithdrawalMapper,
            memberWithdrawalArchiveMapper,
            refreshTokenService
        );

        inOrder.verify(memberDataCleanupMapper).deleteNotificationByMemberId(MEMBER_ID);
        inOrder.verify(memberDataCleanupMapper).deleteNotificationSettingByMemberId(MEMBER_ID);
        inOrder.verify(creditInfoWithdrawalMapper).deletePointHistoryByMemberId(MEMBER_ID);
        inOrder.verify(creditInfoWithdrawalMapper).deleteUserCardByMemberId(MEMBER_ID);
        inOrder.verify(memberMapper).anonymizeMember(
            eq(MEMBER_ID), any(), any(), any(), any(), any()
        );
        inOrder.verify(memberWithdrawalMapper).insert(any());
        inOrder.verify(memberWithdrawalArchiveMapper).insert(any());
        inOrder.verify(refreshTokenService).revokeAllByWithdrawal(MEMBER_ID);

        // 신용정보 삭제 11개가 전부 호출됐는지도 확인한다.
        verify(creditInfoWithdrawalMapper).deletePaymentQrByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deletePaymentByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteExpenseByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deletePointWalletByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteUserCardBenefitSelectionByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteUserCardMonthlyStateByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteUserBenefitUsageByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteMembershipRegisterByMemberId(MEMBER_ID);
        verify(creditInfoWithdrawalMapper).deleteRecommendInputByMemberId(MEMBER_ID);

        // 개인화·인증 부가정보 5개도 전부 호출됐는지 확인한다.
        verify(memberDataCleanupMapper).deleteMemberPreferredCategoryByMemberId(MEMBER_ID);
        verify(memberDataCleanupMapper).deleteMemberPreferredMerchantByMemberId(MEMBER_ID);
        verify(memberDataCleanupMapper).deleteMemberPersonalizationBrandByMemberId(MEMBER_ID);
        verify(memberDataCleanupMapper).deleteMemberPersonalizationCategoryByMemberId(MEMBER_ID);
        verify(memberDataCleanupMapper).deletePasswordResetVerificationByMemberId(MEMBER_ID);

        // 탈퇴 고지 약관 동의 이력이 저장됐는지 확인한다.
        verify(termAgreementMapper).insertMemberTermAgreements(
            argThatSingleAgreementFor(WITHDRAWAL_TERM_VERSION_ID)
        );
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 익명화된 이메일이 회원 ID 기반 규칙으로 생성된다")
    void withdraw_success_anonymizedEmailFollowsMemberIdRule() {
        // given
        Member member = buildActiveMember();

        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(WITHDRAWAL_TERM_VERSION_ID));
        when(sha256Hasher.sha256(ORIGINAL_EMAIL)).thenReturn("hashed-email");

        ArgumentCaptor<String> anonymizedEmailCaptor = ArgumentCaptor.forClass(String.class);

        // when
        memberService.withdraw(MEMBER_ID, buildRequest());

        // then
        verify(memberMapper).anonymizeMember(
            eq(MEMBER_ID),
            anonymizedEmailCaptor.capture(),
            eq("탈퇴회원"),
            eq("탈퇴회원"),
            eq("WITHDRAWN_MEMBER_CANNOT_LOGIN"),
            any()
        );

        assertThat(anonymizedEmailCaptor.getValue()).isEqualTo("withdrawn_1@deleted.local");
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - archive에는 익명화 전 원본 이메일·성명이 저장된다.")
    void withdraw_success_archiveKeepsOriginalPiiWithoutPurgeSchedule() {
        // given
        Member member = buildActiveMember();

        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(WITHDRAWAL_TERM_VERSION_ID));
        when(sha256Hasher.sha256(ORIGINAL_EMAIL)).thenReturn("hashed-email");

        ArgumentCaptor<MemberWithdrawalArchive> archiveCaptor =
            ArgumentCaptor.forClass(MemberWithdrawalArchive.class);

        // when
        memberService.withdraw(MEMBER_ID, buildRequest());

        // then
        verify(memberWithdrawalArchiveMapper).insert(archiveCaptor.capture());

        MemberWithdrawalArchive savedArchive = archiveCaptor.getValue();
        /*
         * member.anonymizeMember가 호출된 뒤에도 archive에는 익명화되기 전
         * 원본 이메일·성명이 그대로 들어가야 한다. member 객체에서 값을 읽은
         * 시점(익명화 이전)이 archive 생성 시점과 같기 때문이다.
         */
        assertThat(savedArchive.getEmail()).isEqualTo(ORIGINAL_EMAIL);
        assertThat(savedArchive.getName()).isEqualTo(ORIGINAL_NAME);
        assertThat(savedArchive.getEmailHash()).isEqualTo("hashed-email");
        assertThat(savedArchive.getRetentionReason()).isNotBlank();
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 존재하지 않거나 이미 탈퇴한 회원이면 MEMBER_NOT_FOUND를 던지고 아무것도 지우지 않는다")
    void withdraw_fail_whenMemberNotFoundOrAlreadyWithdrawn() {
        // given
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, buildRequest()))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        /*
         * 잠금 단계에서 실패하면 이후 어떤 삭제·수정도 실행되면 안 된다.
         * 트랜잭션이 롤백되긴 하지만, 애초에 호출 자체가 없어야 한다는 걸
         * 명시적으로 검증한다.
         */
        verifyNoInteractions(
            memberDataCleanupMapper,
            creditInfoWithdrawalMapper,
            memberWithdrawalMapper,
            memberWithdrawalArchiveMapper,
            refreshTokenService
        );
        verify(memberMapper, never()).anonymizeMember(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호가 일치하지 않으면 LOGIN_CREDENTIAL_MISMATCH를 던지고 아무것도 지우지 않는다")
    void withdraw_fail_whenPasswordMismatch() {
        // given
        Member member = buildActiveMember();

        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, buildRequest()))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.LOGIN_CREDENTIAL_MISMATCH);

        verifyNoInteractions(
            memberDataCleanupMapper,
            creditInfoWithdrawalMapper,
            memberWithdrawalMapper,
            memberWithdrawalArchiveMapper,
            refreshTokenService
        );
        // 약관 검증까지 가지 않고 비밀번호 확인 단계에서 먼저 막혀야 한다.
        verify(termAgreementMapper, never()).findActiveRequiredTermVersionIds(any());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 제출한 termVersionId가 현재 유효한 탈퇴 고지 약관이 아니면 INPUT_INVALID를 던진다")
    void withdraw_fail_whenTermVersionIsNotCurrentWithdrawalNotice() {
        // given
        Member member = buildActiveMember();

        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        // 현재 유효한 탈퇴 고지 약관은 999L인데 요청은 5L을 보낸 상황을 가정한다.
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(999L));

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, buildRequest()))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INPUT_INVALID);

        verifyNoInteractions(
            memberDataCleanupMapper,
            creditInfoWithdrawalMapper,
            memberWithdrawalMapper,
            memberWithdrawalArchiveMapper,
            refreshTokenService
        );
    }

    // insertMemberTermAgreements(List<MemberTermAgreement>)에 넘겨진 리스트가
    // "이 회원의 이 약관 버전에 대한 동의 1건"인지 확인하는 매처를 만든다.
    private List<com.wallet.auth.domain.MemberTermAgreement> argThatSingleAgreementFor(Long termVersionId) {
        return Mockito.argThat(agreements ->
            agreements != null
                && agreements.size() == 1
                && agreements.get(0).getMemberId().equals(MEMBER_ID)
                && agreements.get(0).getTermsVersionId().equals(termVersionId)
                && agreements.get(0).getAgreed()
        );
    }
}