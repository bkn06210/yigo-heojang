package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.domain.WithdrawalReasonType;
import com.wallet.member.dto.MemberWithdrawRequest;

/**
 * 회원 탈퇴 경로의 MySQL 통합 테스트.
 *
 * 실 MySQL(docker-compose의 yigo-mysql, schema.sql·data.sql 적용 필요)에 연결한다.
 * SettlementServiceIntegrationTest와 동일하게 H2는 쓰지 않는다.
 *
 * MemberServiceTest(단위 테스트)는 매퍼를 전부 mock으로 대체해
 * "올바른 순서로 호출되는지"만 검증했다. 이 테스트가 추가로 확인하는 것은
 * '그 호출을 실제 MySQL에 실행했을 때 정말로 되는가'다:
 *   ① notification을 point_history·user_card보다 먼저 지우지 않으면 FK 위반이
 *      나는 게 커밋 7-2에서 SQL만으로 확인한 것인데, 이 테스트는 그 순서가
 *      실제 실행 경로에서도 지켜지는지 실제 FK 제약으로 검증한다
 *      (notification 픽스처가 user_card·point_history를 실제로 참조하게 만든다)
 *   ② 신용정보 11개 + 부가정보 7개가 실제로 물리 삭제되는지
 *   ③ member가 실제로 익명화되고, 원본 email·name이 archive에 실제로 남는지
 *   ④ 실패 시(비밀번호 불일치 등) 트랜잭션이 실제로 롤백되어 아무 데이터도
 *      바뀌지 않는지
 *
 * 클래스 이름이 *Test라 surefire가 실행한다. @Transactional으로 픽스처를 넣고
 * 테스트마다 롤백한다. MemberService.withdraw 자체도 @Transactional이지만
 * REQUIRED 전파라 이 테스트의 트랜잭션에 합류할 뿐, 별도 커밋이 일어나지 않는다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class MemberWithdrawalIntegrationTest {

    private static final String RAW_PASSWORD = "withdraw-it-password";
    private static final String WRONG_PASSWORD = "wrong-password";

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private JdbcTemplate jdbc;

    private long memberId;
    private long userCardId;
    private long categoryId;
    private long merchantId;
    private long benefitId;
    private long pointProviderId;
    private Long withdrawalTermVersionId;
    private Long signupTermVersionId;

    // 수정 후
    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();  // 약관도 이 안에서 함께 만든다
    }

    // ── 성공 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("탈퇴하면 신용정보·부가정보는 물리 삭제되고, member는 익명화되며, 원본 개인정보는 archive에 보관된다")
    void 탈퇴하면_데이터가_정리되고_member는_익명화되고_원본은_보관된다() {
        MemberWithdrawRequest request = new MemberWithdrawRequest(
            RAW_PASSWORD, WithdrawalReasonType.LOW_USAGE, "자주 사용하지 않아서 탈퇴합니다.",
            withdrawalTermVersionId
        );

        memberService.withdraw(memberId, request);

        // 신용정보 11개
        assertThat(countByMemberId("payment_qr")).isZero();
        assertThat(countByMemberId("payment")).isZero();
        assertThat(countByMemberId("point_history")).isZero();
        assertThat(countByMemberId("expense")).isZero();
        assertThat(countByMemberId("point_wallet")).isZero();
        assertThat(countByUserCardId("user_card_benefit_selection")).isZero();
        assertThat(countByUserCardId("user_card_monthly_state")).isZero();
        assertThat(countByUserCardId("user_benefit_usage")).isZero();
        assertThat(countByMemberId("user_card")).isZero();
        assertThat(countByMemberId("membership_register")).isZero();
        assertThat(countByMemberId("recommend_input")).isZero();

        /*
         * notification이 실제로 user_card·point_history를 참조하는 픽스처였는데도
         * (아래 insertFixture 참고) 예외 없이 전부 삭제됐다는 것 자체가,
         * "notification을 point_history·user_card보다 먼저 지운다"는 순서가
         * 실제 MySQL에서 FK 위반 없이 지켜졌다는 증거다.
         */
        assertThat(countByMemberId("notification")).isZero();
        assertThat(countByMemberId("notification_setting")).isZero();
        assertThat(countByMemberId("member_preferred_category")).isZero();
        assertThat(countByMemberId("member_preferred_merchant")).isZero();
        assertThat(countByMemberId("member_personalization_category")).isZero();
        assertThat(countByMemberId("member_personalization_brand")).isZero();
        assertThat(countByMemberId("password_reset_verification")).isZero();

        // member 익명화
        Map<String, Object> member = jdbc.queryForMap(
            "SELECT email, name, nickname, password_hash, member_status, withdrawn_at "
                + "FROM member WHERE member_id = ?", memberId
        );
        assertThat(member.get("email")).isEqualTo("withdrawn_" + memberId + "@deleted.local");
        assertThat(member.get("name")).isEqualTo("탈퇴회원");
        assertThat(member.get("nickname")).isEqualTo("탈퇴회원");
        assertThat(member.get("password_hash")).isEqualTo("WITHDRAWN_MEMBER_CANNOT_LOGIN");
        assertThat(member.get("member_status")).isEqualTo("WITHDRAWN");
        assertThat(member.get("withdrawn_at")).isNotNull();

        // 탈퇴 사유 기록
        Map<String, Object> withdrawal = jdbc.queryForMap(
            "SELECT reason_type, reason_detail FROM member_withdrawal WHERE member_id = ?", memberId
        );
        assertThat(withdrawal.get("reason_type")).isEqualTo("LOW_USAGE");

        // 원본 개인정보 보관 — member는 익명화됐지만 archive에는 원본이 남아야 한다
        Map<String, Object> archive = jdbc.queryForMap(
            "SELECT email, name, email_hash FROM member_withdrawal_archive WHERE member_id = ?", memberId
        );
        assertThat(archive.get("email")).isEqualTo("withdraw-it@test.local");
        assertThat(archive.get("name")).isEqualTo("탈퇴IT회원");
        assertThat(archive.get("email_hash")).isNotNull();

        // refresh token 폐기 — 행 자체는 남고 revoked 처리만 된다
        Map<String, Object> refreshToken = jdbc.queryForMap(
            "SELECT revoked_at, revoke_reason FROM refresh_token WHERE member_id = ?", memberId
        );
        assertThat(refreshToken.get("revoked_at")).isNotNull();
        assertThat(refreshToken.get("revoke_reason")).isEqualTo("MEMBER_WITHDRAWN");

        // member_term_agreement는 3년 보관 대상이라 삭제되지 않아야 한다.
        // 회원가입 때 동의한 이력(픽스처) + 이번에 새로 추가된 탈퇴 고지 동의, 총 2건이 남는다.
        long agreementCount = count(
            "SELECT COUNT(*) FROM member_term_agreement WHERE member_id = ?", memberId
        );
        assertThat(agreementCount).isEqualTo(2L);

        Map<String, Object> withdrawalAgreement = jdbc.queryForMap(
            "SELECT is_agreed FROM member_term_agreement WHERE member_id = ? AND term_version_id = ?",
            memberId, withdrawalTermVersionId
        );
        assertThat((Boolean) withdrawalAgreement.get("is_agreed")).isTrue();
    }

    // ── 실패 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("이미 탈퇴한 회원이 다시 탈퇴를 시도하면 MEMBER_NOT_FOUND이고 아무 데이터도 바뀌지 않는다")
    void 이미_탈퇴한_회원은_MEMBER_NOT_FOUND이고_데이터가_그대로다() {
        jdbc.update(
            "UPDATE member SET member_status = 'WITHDRAWN', withdrawn_at = NOW() WHERE member_id = ?",
            memberId
        );

        MemberWithdrawRequest request = new MemberWithdrawRequest(
            RAW_PASSWORD, WithdrawalReasonType.LOW_USAGE, null, withdrawalTermVersionId
        );

        assertThatThrownBy(() -> memberService.withdraw(memberId, request))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

        // 잠금 단계에서 막혔으므로 신용정보가 그대로 남아 있어야 한다.
        assertThat(countByMemberId("expense")).isEqualTo(1L);
        assertThat(countByMemberId("user_card")).isEqualTo(1L);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 LOGIN_CREDENTIAL_MISMATCH이고 아무 데이터도 바뀌지 않는다")
    void 비밀번호가_틀리면_LOGIN_CREDENTIAL_MISMATCH이고_데이터가_그대로다() {
        MemberWithdrawRequest request = new MemberWithdrawRequest(
            WRONG_PASSWORD, WithdrawalReasonType.LOW_USAGE, null, withdrawalTermVersionId
        );

        assertThatThrownBy(() -> memberService.withdraw(memberId, request))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_CREDENTIAL_MISMATCH));

        assertThat(countByMemberId("expense")).isEqualTo(1L);
        assertThat(countByMemberId("notification")).isEqualTo(1L);

        String email = jdbc.queryForObject(
            "SELECT email FROM member WHERE member_id = ?", String.class, memberId
        );
        assertThat(email).isEqualTo("withdraw-it@test.local");
    }

    @Test
    @DisplayName("유효하지 않은 termVersionId면 INPUT_INVALID이고 아무 데이터도 바뀌지 않는다")
    void 유효하지_않은_약관버전이면_INPUT_INVALID이고_데이터가_그대로다() {
        MemberWithdrawRequest request = new MemberWithdrawRequest(
            RAW_PASSWORD, WithdrawalReasonType.LOW_USAGE, null, -1L
        );

        assertThatThrownBy(() -> memberService.withdraw(memberId, request))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INPUT_INVALID));

        assertThat(countByMemberId("expense")).isEqualTo(1L);
    }

    // ── 픽스처 ─────────────────────────────────────────────────────────────
    // 시드와 겹치지 않도록 IT 전용 코드('IT_WD_*', 고유 이메일)를 쓴다.

    private void insertFixture() {
        /*
         * CI는 schema.sql만 적용하고 data.sql은 넣지 않아, 시드를 조회하면
         * 로컬에서는 통과하고 CI에서만 EmptyResultDataAccessException으로 깨진다.
         *
         * 주의: term_code에 UNIQUE 제약(uk_term_code)이 있어서, data.sql이
         * 적용된 로컬에서도 충돌하지 않도록 IT 전용 코드를 쓴다.
         */
        long withdrawalTermId = insert(
            "INSERT INTO term (term_code, term_scope, term_name, is_required, term_status) "
                + "VALUES ('IT_WD_WITHDRAWAL_NOTICE', 'WITHDRAWAL', 'IT탈퇴안내', 1, 'ACTIVE')"
        );
        withdrawalTermVersionId = insert(
            "INSERT INTO term_version (term_id, version, content, effective_started_at, effective_ended_at) "
                + "VALUES (?, 'IT-v1', 'IT 탈퇴 고지 문안', '2020-01-01 00:00:00', NULL)",
            withdrawalTermId
        );

        // 회원가입 필수 약관도 하나 만든다. 탈퇴와 무관하게 보관되어야 하는
        // member_term_agreement 행을 만들기 위한 것이다.
        long signupTermId = insert(
            "INSERT INTO term (term_code, term_scope, term_name, is_required, term_status) "
                + "VALUES ('IT_WD_SERVICE_TERMS', 'SIGNUP', 'IT서비스이용약관', 1, 'ACTIVE')"
        );
        signupTermVersionId = insert(
            "INSERT INTO term_version (term_id, version, content, effective_started_at, effective_ended_at) "
                + "VALUES (?, 'IT-v1', 'IT 서비스 이용약관 문안', '2020-01-01 00:00:00', NULL)",
            signupTermId
        );

        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);
        memberId = insert(
            "INSERT INTO member (email, password_hash, name, nickname, member_status) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE')",
            "withdraw-it@test.local", encodedPassword, "탈퇴IT회원", "탈퇴IT별명"
        );

        long cardCompanyId = insert(
            "INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
            "IT_WD_COMPANY", "IT탈퇴카드사"
        );
        long cardId = insert(
            "INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
            "IT탈퇴카드", cardCompanyId, "CREDIT"
        );

        long parentCategoryId = insert(
            "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
            "IT_WD_FOOD", "IT탈퇴대분류"
        );
        categoryId = insert(
            "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
            "IT_WD_CAFE", "IT탈퇴카페", parentCategoryId
        );
        merchantId = insert(
            "INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
            "IT_WD_STARBUCKS", "IT탈퇴스타벅스", categoryId
        );

        // target_type=ALL이면 target_category_id·target_merchant_id를 둘 다 안 채워도
        // ck_benefit_target CHECK 제약을 만족한다. 이 테스트는 혜택 계산 결과가
        // 아니라 삭제 여부만 보므로 실제 혜택 규칙은 단순하게 둔다.
        benefitId = insert(
            "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, target_type) "
                + "VALUES (?, 'IT탈퇴혜택', 'DISCOUNT', 'RATE', 5.00, 'ALL')",
            cardId
        );

        userCardId = insert(
            "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
            memberId, cardId, "9999-****-****-9999"
        );

        jdbc.update(
            "INSERT INTO user_card_benefit_selection "
                + "(user_card_id, base_year_month, option_group_code, selected_option_key) "
                + "VALUES (?, '2026-08', 'IT_WD_GROUP', 'IT_WD_OPTION')",
            userCardId
        );
        jdbc.update(
            "INSERT INTO user_card_monthly_state (user_card_id, base_year_month) VALUES (?, '2026-08')",
            userCardId
        );
        jdbc.update(
            "INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month) VALUES (?, ?, '2026-08')",
            userCardId, benefitId
        );

        long expenseId = insert(
            "INSERT INTO expense (member_id, user_card_id, category_id, merchant_id, merchant_name, "
                + "amount, payment_date, input_type, applied_benefit_id, discount_amount, payment_type, "
                + "is_interest_free) VALUES (?, ?, ?, ?, ?, 10000, '2026-08-10 12:00:00', 'PAYMENT', ?, 500, 'CARD', 'N')",
            memberId, userCardId, categoryId, merchantId, "IT탈퇴스타벅스", benefitId
        );

        long paymentId = insert(
            "INSERT INTO payment (member_id, user_card_id, expense_id, merchant_id, merchant_name, "
                + "payment_amount, payment_status) VALUES (?, ?, ?, ?, ?, 10000, 'SUCCESS')",
            memberId, userCardId, expenseId, merchantId, "IT탈퇴스타벅스"
        );

        insert(
            "INSERT INTO payment_qr (qr_token, member_id, user_card_id, payment_amount, payment_id, expires_at) "
                + "VALUES (?, ?, ?, 10000, ?, '2027-01-01 00:00:00')",
            "IT-WD-QR-" + memberId, memberId, userCardId, paymentId
        );

        pointProviderId = insert(
            "INSERT INTO point_provider (point_provider_name, point_provider_type) VALUES (?, 'MEMBERSHIP')",
            "IT탈퇴포인트사-" + memberId
        );
        long pointWalletId = insert(
            "INSERT INTO point_wallet (member_id, point_provider_id) VALUES (?, ?)",
            memberId, pointProviderId
        );
        insert(
            "INSERT INTO point_history (member_id, point_wallet_id, expense_id, point_type, point_amount) "
                + "VALUES (?, ?, ?, 'SAVE', 100)",
            memberId, pointWalletId, expenseId
        );

        insert(
            "INSERT INTO membership_register (member_id, point_provider_id, register_status) VALUES (?, ?, 'REGISTERED')",
            memberId, pointProviderId
        );
        insert(
            "INSERT INTO recommend_input (member_id, category_id, merchant_id, expected_amount) VALUES (?, ?, ?, 10000)",
            memberId, categoryId, merchantId
        );

        /*
         * notification이 실제로 user_card_id·point_history_id를 채운 상태로 넣는다.
         * 이게 이 통합 테스트에서 가장 중요한 픽스처다 — 여기서 값을 안 채우면
         * (NULL로 두면) notification 삭제 순서가 틀려도 FK 위반이 안 나서
         * 커밋 7-2에서 발견한 문제를 이 테스트가 재현하지 못한다.
         */
        long lastPointHistoryId = jdbc.queryForObject(
            "SELECT point_history_id FROM point_history WHERE member_id = ? ORDER BY point_history_id DESC LIMIT 1",
            Long.class, memberId
        );
        insert(
            "INSERT INTO notification (member_id, user_card_id, point_history_id, notification_type, "
                + "title, content, deduplication_key) VALUES (?, ?, ?, 'IT_WD_TYPE', 'IT탈퇴알림', 'IT탈퇴알림내용', ?)",
            memberId, userCardId, lastPointHistoryId, "IT-WD-DEDUP-" + memberId
        );
        insert(
            "INSERT INTO notification_setting (member_id) VALUES (?)",
            memberId
        );

        insert(
            "INSERT INTO member_preferred_category (member_id, category_id) VALUES (?, ?)",
            memberId, categoryId
        );
        insert(
            "INSERT INTO member_preferred_merchant (member_id, category_id, merchant_id, priority) VALUES (?, ?, ?, 1)",
            memberId, categoryId, merchantId
        );

        jdbc.update(
            "INSERT INTO member_personalization_category (member_id, category_key) VALUES (?, 'IT_WD_CAT_KEY')",
            memberId
        );
        jdbc.update(
            "INSERT INTO member_personalization_brand (member_id, category_key, priority, brand_name) "
                + "VALUES (?, 'IT_WD_CAT_KEY', 1, 'IT탈퇴브랜드')",
            memberId
        );

        insert(
            "INSERT INTO password_reset_verification "
                + "(member_id, verification_code_hash, verification_code_expires_at) "
                + "VALUES (?, 'IT-WD-CODE-HASH', '2027-01-01 00:00:00')",
            memberId
        );
        insert(
            "INSERT INTO refresh_token (member_id, token_hash, expires_at) VALUES (?, ?, '2027-01-01 00:00:00')",
            memberId, "IT-WD-REFRESH-" + memberId
        );

        // 회원가입 때 동의한 것으로 가정하는 필수 약관 하나. 
        insert(
            "INSERT INTO member_term_agreement (member_id, term_version_id, is_agreed, agreed_at) "
                + "VALUES (?, ?, 1, NOW())",
            memberId, signupTermVersionId
        );
    }

    private long countByMemberId(String table) {
        return count("SELECT COUNT(*) FROM " + table + " WHERE member_id = ?", memberId);
    }

    private long countByUserCardId(String table) {
        return count("SELECT COUNT(*) FROM " + table + " WHERE user_card_id = ?", userCardId);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    /** INSERT 후 AUTO_INCREMENT PK를 돌려준다. @Transactional 안에서 같은 커넥션이라 LAST_INSERT_ID가 유효하다. */
    private long insert(String sql, Object... args) {
        jdbc.update(sql, args);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("생성 키를 가져오지 못했다: " + sql);
        }
        return id;
    }
}