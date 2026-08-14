package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

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
 * 회원 탈퇴의 MySQL 통합 테스트.
 *
 * 실 MySQL(db/schema.sql + db/migration_withdrawal.sql 적용 필요)에 연결한다.
 * 단위 테스트(MemberServiceWithdrawTest)는 매퍼를 모킹하므로 "어떤 순서로 호출하는지"까지만 검증할 수 있다.
 * 여기서 검증하는 건 그 호출들이 실제 DB에서 통하는지다:
 *   ① 11개 신용정보 테이블 + 알림·개인화 데이터가 FK 위반 없이 실제로 지워지는지
 *   ② member 행은 남되 email·name·password_hash가 마스킹되는지
 *   ③ 원본 개인정보가 member_withdrawal_archive로 옮겨지는지
 *   ④ 탈퇴한 이메일로 곧바로 재가입할 수 있는지 (uk_member_email 충돌이 없는지)
 *   ⑤ 비밀번호 불일치·약관 미동의면 아무것도 지워지지 않고 롤백되는지
 *
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class MemberWithdrawalIntegrationTest {

    private static final String RAW_PASSWORD = "Password1!";
    private static final String ORIGINAL_EMAIL = "withdraw-it@test.local";
    private static final String ORIGINAL_NAME = "탈퇴테스트회원";

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private JdbcTemplate jdbc;

    private long memberId;
    private long userCardId;
    private long expenseId;
    private long pointHistoryId;
    private long withdrawalTermVersionId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        insertFixture();
    }

    // ── 성공 경로 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("탈퇴하면 신용정보·알림·개인화 데이터가 FK 위반 없이 전부 물리 삭제된다")
    void 탈퇴하면_신용정보가_전부_삭제된다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        // 신용/거래 정보
        assertThat(countByMember("payment_qr")).isZero();
        assertThat(countByMember("payment")).isZero();
        assertThat(countByMember("point_history")).isZero();
        assertThat(countByMember("expense")).isZero();
        assertThat(countByMember("point_wallet")).isZero();
        assertThat(countByMember("membership_register")).isZero();
        assertThat(countByMember("recommend_input")).isZero();

        // user_card와, user_card_id로만 연결된 자식 테이블들
        assertThat(countByMember("user_card")).isZero();
        assertThat(countByUserCard("user_card_benefit_selection")).isZero();
        assertThat(countByUserCard("user_card_monthly_state")).isZero();
        assertThat(countByUserCard("user_benefit_usage")).isZero();

        // 알림·개인화·인증 부가정보
        assertThat(countByMember("notification")).isZero();
        assertThat(countByMember("notification_setting")).isZero();
        assertThat(countByMember("member_preferred_category")).isZero();
        assertThat(countByMember("member_preferred_merchant")).isZero();
        assertThat(countByMember("member_personalization_brand")).isZero();
        assertThat(countByMember("member_personalization_category")).isZero();
        assertThat(countByMember("password_reset_verification")).isZero();
        assertThat(countByMember("simple_password_verification")).isZero();
    }

    @Test
    @DisplayName("탈퇴하면 간편비밀번호 해시도 지워져 재가입해도 복원되지 않는다")
    void 탈퇴하면_간편비밀번호가_지워진다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        assertThat(memberColumn("simple_password_hash")).isNull();
        assertThat(jdbc.queryForObject(
            "SELECT simple_password_failed_attempt_count FROM member WHERE member_id = ?",
            Integer.class, memberId))
            .isZero();
        assertThat(memberColumn("simple_password_locked_until")).isNull();
    }

    @Test
    @DisplayName("탈퇴해도 member 행은 남고, 개인정보만 마스킹 값으로 바뀐다")
    void 탈퇴하면_member행은_남고_개인정보만_마스킹된다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        // member_term_agreement가 member를 FK로 참조하므로 행 자체는 지울 수 없다.
        assertThat(countByMember("member", "member_id")).isEqualTo(1);

        assertThat(memberColumn("email")).isEqualTo("withdrawn_" + memberId + "@deleted.local");
        assertThat(memberColumn("name")).isEqualTo("탈퇴회원");
        assertThat(memberColumn("nickname")).isEqualTo("탈퇴회원");
        assertThat(memberColumn("member_status")).isEqualTo("WITHDRAWN");

        // 어떤 원문으로도 매칭될 수 없는 값이라 로그인 자체가 불가능해진다.
        assertThat(memberColumn("password_hash")).isEqualTo("WITHDRAWN_MEMBER_CANNOT_LOGIN");
        assertThat(passwordEncoder.matches(RAW_PASSWORD, memberColumn("password_hash"))).isFalse();

        assertThat(jdbc.queryForObject(
            "SELECT withdrawn_at FROM member WHERE member_id = ?", LocalDateTime.class, memberId))
            .isNotNull();
    }

    @Test
    @DisplayName("탈퇴 시 원본 개인정보가 보관 테이블로 옮겨지고 탈퇴 사유가 기록된다")
    void 탈퇴하면_원본이_보관되고_사유가_기록된다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        assertThat(jdbc.queryForObject(
            "SELECT email FROM member_withdrawal_archive WHERE member_id = ?", String.class, memberId))
            .isEqualTo(ORIGINAL_EMAIL);
        assertThat(jdbc.queryForObject(
            "SELECT name FROM member_withdrawal_archive WHERE member_id = ?", String.class, memberId))
            .isEqualTo(ORIGINAL_NAME);
        // SHA-256 hex는 64자다.
        assertThat(jdbc.queryForObject(
            "SELECT email_hash FROM member_withdrawal_archive WHERE member_id = ?", String.class, memberId))
            .hasSize(64);

        assertThat(jdbc.queryForObject(
            "SELECT reason_type FROM member_withdrawal WHERE member_id = ?", String.class, memberId))
            .isEqualTo("LOW_USAGE");
        assertThat(jdbc.queryForObject(
            "SELECT reason_detail FROM member_withdrawal WHERE member_id = ?", String.class, memberId))
            .isEqualTo("자주 사용하지 않아서요.");

        // 탈퇴 고지 약관에 동의한 이력이 남는다.
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM member_term_agreement WHERE member_id = ? AND term_version_id = ? AND is_agreed = 1",
            Integer.class, memberId, withdrawalTermVersionId))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴 시 이 회원의 Refresh Token이 모두 폐기된다")
    void 탈퇴하면_리프레시토큰이_폐기된다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM refresh_token WHERE member_id = ? AND revoked_at IS NULL",
            Integer.class, memberId))
            .isZero();
        assertThat(jdbc.queryForObject(
            "SELECT revoke_reason FROM refresh_token WHERE member_id = ? LIMIT 1", String.class, memberId))
            .isEqualTo("MEMBER_WITHDRAWN");
    }

    @Test
    @DisplayName("탈퇴한 이메일로 곧바로 재가입할 수 있다 - 이메일이 마스킹돼 UNIQUE 제약에 걸리지 않는다")
    void 탈퇴후_같은_이메일로_재가입할_수_있다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        // 재가입 시 AuthService가 하는 중복 검사와 같은 조건이다.
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, ORIGINAL_EMAIL))
            .isZero();

        // 실제로 같은 이메일로 INSERT가 통하는지까지 확인한다 (uk_member_email).
        jdbc.update(
            "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
            ORIGINAL_EMAIL, "x", ORIGINAL_NAME, "재가입별명");

        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, ORIGINAL_EMAIL))
            .isEqualTo(1);
    }

    // ── 실패 경로 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("비밀번호가 틀리면 WITHDRAWAL_PASSWORD_MISMATCH가 나고 데이터가 그대로 남는다")
    void 비밀번호가_틀리면_아무것도_지워지지_않는다() {
        assertThatThrownBy(() -> memberService.withdraw(memberId, request("WrongPassword1!")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WITHDRAWAL_PASSWORD_MISMATCH);

        assertThat(countByMember("user_card")).isEqualTo(1);
        assertThat(countByMember("expense")).isEqualTo(1);
        assertThat(countByMember("notification")).isEqualTo(1);
        assertThat(memberColumn("email")).isEqualTo(ORIGINAL_EMAIL);
        assertThat(memberColumn("member_status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("유효한 탈퇴 고지 약관 버전이 아니면 INPUT_INVALID가 나고 데이터가 그대로 남는다")
    void 약관버전이_유효하지_않으면_아무것도_지워지지_않는다() {
        MemberWithdrawRequest invalid = new MemberWithdrawRequest(
            RAW_PASSWORD, WithdrawalReasonType.LOW_USAGE, null, 999_999L);

        assertThatThrownBy(() -> memberService.withdraw(memberId, invalid))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INPUT_INVALID);

        assertThat(countByMember("user_card")).isEqualTo(1);
        assertThat(memberColumn("member_status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("가입 약관 버전으로는 탈퇴할 수 없다 - 스코프가 WITHDRAWAL인 약관만 통과한다")
    void 가입약관으로는_탈퇴할_수_없다() {
        Long signupTermVersionId = jdbc.queryForObject(
            "SELECT tv.term_version_id FROM term_version tv "
                + "INNER JOIN term t ON tv.term_id = t.term_id "
                + "WHERE t.term_scope = 'SIGNUP' AND t.is_required = 1 LIMIT 1",
            Long.class);

        MemberWithdrawRequest signupScoped = new MemberWithdrawRequest(
            RAW_PASSWORD, WithdrawalReasonType.LOW_USAGE, null, signupTermVersionId);

        assertThatThrownBy(() -> memberService.withdraw(memberId, signupScoped))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INPUT_INVALID);

        assertThat(memberColumn("member_status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("이미 탈퇴한 회원이 다시 탈퇴하면 MEMBER_NOT_FOUND")
    void 이미_탈퇴한_회원은_다시_탈퇴할_수_없다() {
        memberService.withdraw(memberId, request(RAW_PASSWORD));

        assertThatThrownBy(() -> memberService.withdraw(memberId, request(RAW_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private MemberWithdrawRequest request(String password) {
        return new MemberWithdrawRequest(
            password,
            WithdrawalReasonType.LOW_USAGE,
            "자주 사용하지 않아서요.",
            withdrawalTermVersionId
        );
    }

    /** 탈퇴가 지워야 할 모든 테이블에 이 회원의 행을 하나씩 심어둔다. */
    private void insertFixture() {
        // 지금 유효한 탈퇴 고지 약관 버전. data.sql/migration이 넣어둔 행을 그대로 쓴다.
        withdrawalTermVersionId = jdbc.queryForObject(
            "SELECT tv.term_version_id FROM term_version tv "
                + "INNER JOIN term t ON tv.term_id = t.term_id "
                + "WHERE t.term_scope = 'WITHDRAWAL' AND t.is_required = 1 "
                + "  AND t.term_status = 'ACTIVE' "
                + "  AND tv.effective_started_at <= NOW() "
                + "  AND (tv.effective_ended_at IS NULL OR tv.effective_ended_at > NOW()) "
                + "LIMIT 1",
            Long.class);

        memberId = insert("INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
            ORIGINAL_EMAIL, passwordEncoder.encode(RAW_PASSWORD), ORIGINAL_NAME, "탈퇴IT별명");

        long parentCategoryId = insert(
            "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, NULL)",
            "IT_WD_FOOD", "IT탈퇴대분류");
        long categoryId = insert(
            "INSERT INTO category (category_code, category_name, parent_category_id) VALUES (?, ?, ?)",
            "IT_WD_CAFE", "IT탈퇴카페", parentCategoryId);
        long merchantId = insert(
            "INSERT INTO merchant (merchant_code, merchant_name, category_id) VALUES (?, ?, ?)",
            "IT_WD_CAFE_SHOP", "IT탈퇴카페샵", categoryId);

        long cardCompanyId = insert(
            "INSERT INTO card_company (company_code, company_name) VALUES (?, ?)",
            "IT_WD_CARD", "IT탈퇴카드사");
        long cardId = insert("INSERT INTO card (card_name, card_company_id, card_type) VALUES (?, ?, ?)",
            "IT탈퇴_테스트카드", cardCompanyId, "CREDIT");
        long benefitId = insert(
            "INSERT INTO benefit (card_id, benefit_name, benefit_kind, calc_method, benefit_value, "
                + "apply_timing, target_type, target_category_id, is_active) "
                + "VALUES (?, 'IT탈퇴 카페 할인', 'DISCOUNT', 'RATE', 10.00, 'BILLED', 'CATEGORY', ?, 'Y')",
            cardId, categoryId);

        userCardId = insert(
            "INSERT INTO user_card (member_id, card_id, masked_card_number) VALUES (?, ?, ?)",
            memberId, cardId, "4444-****-****-4444");

        // user_card_id로만 회원과 연결되는 테이블들 — 서브쿼리 삭제가 실제로 도는지 확인용
        jdbc.update("INSERT INTO user_card_benefit_selection "
                + "(user_card_id, base_year_month, option_group_code, selected_option_key) VALUES (?, ?, ?, ?)",
            userCardId, "2026-08", "IT_WD_GROUP", "IT_WD_OPTION");
        jdbc.update("INSERT INTO user_card_monthly_state (user_card_id, base_year_month) VALUES (?, ?)",
            userCardId, "2026-08");
        jdbc.update("INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month) VALUES (?, ?, ?)",
            userCardId, benefitId, "2026-08");

        expenseId = insert(
            "INSERT INTO expense (member_id, user_card_id, category_id, merchant_id, merchant_name, "
                + "amount, payment_date, input_type, payment_type, is_interest_free, payment_status) "
                + "VALUES (?, ?, ?, ?, ?, 10000, '2026-08-10 13:00:00', 'PAYMENT', 'CARD', 'N', 'APPROVED')",
            memberId, userCardId, categoryId, merchantId, "IT탈퇴카페샵");

        long paymentId = insert(
            "INSERT INTO payment (member_id, user_card_id, expense_id, merchant_id, merchant_name, "
                + "payment_amount, payment_status) VALUES (?, ?, ?, ?, ?, 10000, 'APPROVED')",
            memberId, userCardId, expenseId, merchantId, "IT탈퇴카페샵");

        // status·created_at은 schema.sql에는 DEFAULT가 있지만 실제 개발 DB의 payment_qr에는 없다.
        // 어느 쪽에서 돌려도 통하도록 픽스처에서 전부 명시한다.
        jdbc.update("INSERT INTO payment_qr "
                + "(qr_token, member_id, user_card_id, payment_amount, status, expires_at, payment_id, created_at) "
                + "VALUES (?, ?, ?, 10000, 'USED', '2026-08-10 14:00:00', ?, '2026-08-10 13:59:00')",
            "IT_WD_QR_TOKEN", memberId, userCardId, paymentId);

        long pointProviderId = insert(
            "INSERT INTO point_provider (point_provider_name, point_provider_type) VALUES (?, ?)",
            "IT탈퇴포인트사", "CARD");
        long pointWalletId = insert(
            "INSERT INTO point_wallet (member_id, point_provider_id, total_point) VALUES (?, ?, 5000)",
            memberId, pointProviderId);
        pointHistoryId = insert(
            "INSERT INTO point_history (member_id, point_wallet_id, expense_id, point_type, point_amount) "
                + "VALUES (?, ?, ?, 'EARN', 100)",
            memberId, pointWalletId, expenseId);

        jdbc.update("INSERT INTO membership_register (member_id, point_provider_id, register_status) "
                + "VALUES (?, ?, 'REGISTERED')", memberId, pointProviderId);
        jdbc.update("INSERT INTO recommend_input (member_id, category_id, merchant_id, expected_amount) "
                + "VALUES (?, ?, ?, 10000)", memberId, categoryId, merchantId);

        // notification은 point_history와 user_card를 동시에 참조한다 —
        // 삭제 순서가 뒤집히면 여기서 FK 위반이 터진다.
        jdbc.update("INSERT INTO notification (member_id, user_card_id, benefit_id, point_history_id, "
                + "notification_type, title, content, deduplication_key) "
                + "VALUES (?, ?, ?, ?, 'IT_WD_TYPE', 'IT탈퇴알림', 'IT탈퇴알림내용', ?)",
            memberId, userCardId, benefitId, pointHistoryId, "IT_WD_DEDUP_" + memberId);
        jdbc.update("INSERT INTO notification_setting (member_id) VALUES (?)", memberId);

        jdbc.update("INSERT INTO member_preferred_category (member_id, category_id) VALUES (?, ?)",
            memberId, categoryId);
        jdbc.update("INSERT INTO member_preferred_merchant (member_id, category_id, merchant_id, priority) "
                + "VALUES (?, ?, ?, 1)", memberId, categoryId, merchantId);

        // brand가 category를 참조한다(ON DELETE CASCADE). 명시적 삭제 순서가 맞는지 확인용
        jdbc.update("INSERT INTO member_personalization_category (member_id, category_key) VALUES (?, ?)",
            memberId, "IT_WD_KEY");
        jdbc.update("INSERT INTO member_personalization_brand (member_id, category_key, priority, brand_name) "
                + "VALUES (?, ?, 1, ?)", memberId, "IT_WD_KEY", "IT탈퇴브랜드");

        jdbc.update("INSERT INTO password_reset_verification "
                + "(member_id, verification_code_hash, verification_code_expires_at) "
                + "VALUES (?, 'it-wd-hash', '2026-12-31 23:59:59')", memberId);

        // 간편비밀번호를 설정해둔 회원이라, 탈퇴 시 해시와 인증 이력이 모두 사라져야 한다.
        jdbc.update("UPDATE member SET simple_password_hash = ? WHERE member_id = ?",
            passwordEncoder.encode("012345"), memberId);
        jdbc.update("INSERT INTO simple_password_verification "
                + "(member_id, verification_code_hash, verification_code_expires_at) "
                + "VALUES (?, 'it-wd-simple-hash', '2026-12-31 23:59:59')", memberId);

        jdbc.update("INSERT INTO refresh_token (member_id, token_hash, expires_at) "
                + "VALUES (?, ?, '2026-12-31 23:59:59')", memberId, "it-wd-refresh-token-hash");
    }

    private int countByMember(String table) {
        return countByMember(table, "member_id");
    }

    private int countByMember(String table, String column) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, memberId);
    }

    private int countByUserCard(String table) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE user_card_id = ?", Integer.class, userCardId);
    }

    private String memberColumn(String column) {
        return jdbc.queryForObject(
            "SELECT " + column + " FROM member WHERE member_id = ?", String.class, memberId);
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
