package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wallet.auth.domain.MemberTermAgreement;
import com.wallet.auth.domain.TermScope;
import com.wallet.auth.mapper.TermAgreementMapper;
import com.wallet.auth.service.RefreshTokenService;
import com.wallet.auth.support.TokenHashUtil;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.Member;
import com.wallet.member.domain.MemberWithdrawal;
import com.wallet.member.domain.MemberWithdrawalArchive;
import com.wallet.member.domain.WithdrawalReasonType;
import com.wallet.member.dto.MemberWithdrawRequest;
import com.wallet.member.mapper.CreditInfoWithdrawalMapper;
import com.wallet.member.mapper.MemberDataCleanupMapper;
import com.wallet.member.mapper.MemberMapper;
import com.wallet.member.mapper.MemberWithdrawalArchiveMapper;
import com.wallet.member.mapper.MemberWithdrawalMapper;

class MemberServiceWithdrawTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long WITHDRAWAL_TERM_VERSION_ID = 5L;
    private static final String RAW_PASSWORD = "Password1!";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";
    private static final String ORIGINAL_EMAIL = "active@example.com";
    private static final String ORIGINAL_NAME = "김활성";

    private MemberMapper memberMapper;
    private MemberWithdrawalMapper memberWithdrawalMapper;
    private MemberWithdrawalArchiveMapper memberWithdrawalArchiveMapper;
    private CreditInfoWithdrawalMapper creditInfoWithdrawalMapper;
    private MemberDataCleanupMapper memberDataCleanupMapper;
    private TermAgreementMapper termAgreementMapper;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private TokenHashUtil tokenHashUtil;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        memberWithdrawalMapper = mock(MemberWithdrawalMapper.class);
        memberWithdrawalArchiveMapper = mock(MemberWithdrawalArchiveMapper.class);
        creditInfoWithdrawalMapper = mock(CreditInfoWithdrawalMapper.class);
        memberDataCleanupMapper = mock(MemberDataCleanupMapper.class);
        termAgreementMapper = mock(TermAgreementMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenService = mock(RefreshTokenService.class);
        tokenHashUtil = new TokenHashUtil();

        memberService = new MemberService(
            memberMapper,
            memberWithdrawalMapper,
            memberWithdrawalArchiveMapper,
            creditInfoWithdrawalMapper,
            memberDataCleanupMapper,
            termAgreementMapper,
            passwordEncoder,
            refreshTokenService,
            tokenHashUtil
        );
    }

    // Member는 회원가입 전용 정적 팩토리만 열려 있어 email/name/password만 채워지므로,
    // 탈퇴 테스트에 필요한 원본 회원 정보를 이 헬퍼로 만든다.
    private Member activeMember() {
        Member member = Member.createSignupMember(
            ORIGINAL_EMAIL,
            ENCODED_PASSWORD,
            ORIGINAL_NAME,
            "별명A"
        );
        setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private MemberWithdrawRequest request() {
        return new MemberWithdrawRequest(
            RAW_PASSWORD,
            WithdrawalReasonType.LOW_USAGE,
            "자주 사용하지 않아서요.",
            WITHDRAWAL_TERM_VERSION_ID
        );
    }

    private void givenWithdrawableMember() {
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(activeMember());
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(WITHDRAWAL_TERM_VERSION_ID));
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 회원 정보를 익명화하고 원본은 보관 테이블로 옮긴다")
    void withdraw_success_anonymizesMemberAndArchivesOriginal() {
        // given
        givenWithdrawableMember();

        // when
        memberService.withdraw(MEMBER_ID, request());

        // then
        verify(memberMapper).anonymizeMember(
            eq(MEMBER_ID),
            eq("withdrawn_1@deleted.local"),
            eq("탈퇴회원"),
            eq("탈퇴회원"),
            eq("WITHDRAWN_MEMBER_CANNOT_LOGIN"),
            any()
        );

        ArgumentCaptor<MemberWithdrawalArchive> archiveCaptor =
            ArgumentCaptor.forClass(MemberWithdrawalArchive.class);
        verify(memberWithdrawalArchiveMapper).insert(archiveCaptor.capture());

        MemberWithdrawalArchive archive = archiveCaptor.getValue();
        assertThat(archive.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(archive.getEmail()).isEqualTo(ORIGINAL_EMAIL);
        assertThat(archive.getName()).isEqualTo(ORIGINAL_NAME);
        assertThat(archive.getEmailHash()).isEqualTo(tokenHashUtil.sha256(ORIGINAL_EMAIL));
        assertThat(archive.getWithdrawnAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 탈퇴 사유와 고지 약관 동의 이력을 남긴다")
    void withdraw_success_recordsReasonAndTermAgreement() {
        // given
        givenWithdrawableMember();

        // when
        memberService.withdraw(MEMBER_ID, request());

        // then
        ArgumentCaptor<MemberWithdrawal> withdrawalCaptor =
            ArgumentCaptor.forClass(MemberWithdrawal.class);
        verify(memberWithdrawalMapper).insert(withdrawalCaptor.capture());

        MemberWithdrawal withdrawal = withdrawalCaptor.getValue();
        assertThat(withdrawal.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(withdrawal.getReasonType()).isEqualTo(WithdrawalReasonType.LOW_USAGE);
        assertThat(withdrawal.getReasonDetail()).isEqualTo("자주 사용하지 않아서요.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MemberTermAgreement>> agreementCaptor =
            ArgumentCaptor.forClass(List.class);
        verify(termAgreementMapper).insertMemberTermAgreements(agreementCaptor.capture());

        List<MemberTermAgreement> agreements = agreementCaptor.getValue();
        assertThat(agreements).hasSize(1);
        assertThat(agreements.get(0).getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(agreements.get(0).getTermsVersionId()).isEqualTo(WITHDRAWAL_TERM_VERSION_ID);
        assertThat(agreements.get(0).getAgreed()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - FK 제약 때문에 알림을 신용정보보다 먼저 삭제한다")
    void withdraw_success_deletesNotificationBeforeCreditInfo() {
        // given
        givenWithdrawableMember();

        // when
        memberService.withdraw(MEMBER_ID, request());

        // then
        // notification이 point_history·user_card를 FK로 참조하므로 이 순서가 뒤집히면
        // 실제 DB에서는 외래키 위반으로 삭제가 실패한다.
        InOrder inOrder = inOrder(memberDataCleanupMapper, creditInfoWithdrawalMapper);
        inOrder.verify(memberDataCleanupMapper).deleteNotificationByMemberId(MEMBER_ID);
        inOrder.verify(creditInfoWithdrawalMapper).deletePointHistoryByMemberId(MEMBER_ID);
        inOrder.verify(creditInfoWithdrawalMapper).deleteUserCardByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 모든 Refresh Token을 폐기한다")
    void withdraw_success_revokesAllRefreshTokens() {
        // given
        givenWithdrawableMember();

        // when
        memberService.withdraw(MEMBER_ID, request());

        // then
        verify(refreshTokenService).revokeAllByWithdrawal(MEMBER_ID);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 비밀번호가 틀리면 아무 데이터도 지우지 않는다")
    void withdraw_fail_whenPasswordMismatch() {
        // given
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(activeMember());
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WITHDRAWAL_PASSWORD_MISMATCH);

        verify(memberMapper, never()).anonymizeMember(
            anyLong(), any(), any(), any(), any(), any());
        verify(creditInfoWithdrawalMapper, never()).deleteUserCardByMemberId(anyLong());
        verify(refreshTokenService, never()).revokeAllByWithdrawal(anyLong());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 유효한 탈퇴 고지 약관 버전이 아니면 INPUT_INVALID")
    void withdraw_fail_whenTermVersionInvalid() {
        // given
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(MEMBER_ID);
        when(memberMapper.findById(MEMBER_ID)).thenReturn(activeMember());
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // 프론트가 보낸 5번이 아니라 지금은 99번이 유효한 상황 (조회 후 약관이 개정된 경우)
        when(termAgreementMapper.findActiveRequiredTermVersionIds(TermScope.WITHDRAWAL))
            .thenReturn(List.of(99L));

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INPUT_INVALID);

        verify(memberMapper, never()).anonymizeMember(
            anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 회원이면 MEMBER_NOT_FOUND")
    void withdraw_fail_whenAlreadyWithdrawn() {
        // given
        // lockActiveMemberById는 ACTIVE + withdrawn_at IS NULL 조건이라 이미 탈퇴한 회원은 null이 나온다.
        when(memberMapper.lockActiveMemberById(MEMBER_ID)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(memberMapper, never()).findById(anyLong());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 인증 정보가 없으면 ACCESS_TOKEN_INVALID")
    void withdraw_fail_whenMemberIdIsNull() {
        // when & then
        assertThatThrownBy(() -> memberService.withdraw(null, request()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_TOKEN_INVALID);

        verify(memberMapper, never()).lockActiveMemberById(anyLong());
    }
}
