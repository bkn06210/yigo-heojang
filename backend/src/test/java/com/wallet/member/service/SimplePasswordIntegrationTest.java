package com.wallet.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.wallet.auth.support.TokenHashUtil;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.dto.SimplePasswordUpdateRequest;
import com.wallet.member.dto.SimplePasswordVerifyRequest;
import com.wallet.member.dto.SimplePasswordVerifyResponse;

/**
 * 간편비밀번호 저장·검증의 MySQL 통합 테스트.
 *
 * 실 MySQL(db/schema.sql + db/migration_simple_password.sql 적용 필요)에 연결한다.
 * 단위 테스트는 매퍼를 모킹하므로 "무엇을 호출하는지"까지만 검증할 수 있다.
 * 여기서 검증하는 건 그 호출들이 실제 DB에서 통하는지다:
 *   ① 변경 토큰으로 간편비밀번호가 실제로 저장되고 토큰이 USED로 바뀌는지
 *   ② 같은 토큰을 두 번 쓰면 두 번째가 막히는지
 *   ③ 검증 실패가 누적되고 5회에서 잠기는지 (member의 카운트·잠금 컬럼)
 *   ④ 검증 성공 시 실패 상태가 초기화되는지
 *
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class SimplePasswordIntegrationTest {

    private static final String EMAIL = "simple-pw-it@test.local";
    private static final String SIMPLE_PASSWORD = "012345";
    private static final String CHANGE_TOKEN = "integration-test-change-token";

    @Autowired
    private SimplePasswordService simplePasswordService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenHashUtil tokenHashUtil;

    private JdbcTemplate jdbc;

    private long memberId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        memberId = insert(
            "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
            EMAIL, passwordEncoder.encode("Password1!"), "간편비번테스트", "간편IT별명");
    }

    // ── 저장 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("변경 토큰으로 간편비밀번호를 저장하면 해시로 들어가고 토큰이 USED로 바뀐다")
    void 간편비밀번호가_해시로_저장되고_토큰이_사용처리된다() {
        long verificationId = insertVerifiedToken();

        simplePasswordService.updateSimplePassword(memberId, updateRequest());

        String storedHash = memberColumn("simple_password_hash");
        // 원문이 그대로 들어가면 안 된다.
        assertThat(storedHash).isNotNull().isNotEqualTo(SIMPLE_PASSWORD);
        assertThat(passwordEncoder.matches(SIMPLE_PASSWORD, storedHash)).isTrue();

        assertThat(verificationColumn(verificationId, "verification_status")).isEqualTo("USED");
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM simple_password_verification "
                + "WHERE simple_password_verification_id = ? AND used_at IS NOT NULL",
            Integer.class, verificationId))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("같은 변경 토큰을 두 번 쓰면 두 번째는 ALREADY_USED로 막힌다")
    void 변경토큰은_한_번만_쓸_수_있다() {
        insertVerifiedToken();

        simplePasswordService.updateSimplePassword(memberId, updateRequest());

        assertThatThrownBy(() ->
            simplePasswordService.updateSimplePassword(memberId, updateRequest()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_CHANGE_TOKEN_ALREADY_USED);
    }

    @Test
    @DisplayName("간편비밀번호를 다시 설정하면 이전 실패 횟수와 잠금이 풀린다")
    void 재설정하면_잠금이_풀린다() {
        insertVerifiedToken();
        jdbc.update("UPDATE member SET simple_password_failed_attempt_count = 5, "
                + "simple_password_locked_until = NOW() + INTERVAL 5 MINUTE WHERE member_id = ?",
            memberId);

        simplePasswordService.updateSimplePassword(memberId, updateRequest());

        assertThat(intColumn("simple_password_failed_attempt_count")).isZero();
        assertThat(memberColumn("simple_password_locked_until")).isNull();
    }

    // ── 검증 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("간편비밀번호가 일치하면 matched=true를 반환하고 실패 상태를 초기화한다")
    void 일치하면_성공하고_실패상태가_초기화된다() {
        givenSimplePasswordSet();
        jdbc.update("UPDATE member SET simple_password_failed_attempt_count = 3 WHERE member_id = ?",
            memberId);

        SimplePasswordVerifyResponse response = simplePasswordService.verifySimplePassword(
            memberId, new SimplePasswordVerifyRequest(SIMPLE_PASSWORD));

        assertThat(response.matched()).isTrue();
        assertThat(intColumn("simple_password_failed_attempt_count")).isZero();
    }

    @Test
    @DisplayName("간편비밀번호가 틀리면 matched=false와 함께 실패 횟수가 DB에 누적된다")
    void 틀리면_실패횟수가_누적된다() {
        givenSimplePasswordSet();

        SimplePasswordVerifyResponse response = simplePasswordService.verifySimplePassword(
            memberId, new SimplePasswordVerifyRequest("999999"));

        assertThat(response.matched()).isFalse();
        assertThat(intColumn("simple_password_failed_attempt_count")).isEqualTo(1);
        assertThat(memberColumn("simple_password_locked_until")).isNull();
    }

    @Test
    @DisplayName("연속 5회 틀리면 잠기고, 잠긴 동안에는 올바른 비밀번호도 거부된다")
    void 오회_실패하면_잠긴다() {
        givenSimplePasswordSet();

        // 1~4회: 실패만 누적된다.
        for (int attempt = 1; attempt <= 4; attempt++) {
            simplePasswordService.verifySimplePassword(
                memberId, new SimplePasswordVerifyRequest("999999"));
        }
        assertThat(intColumn("simple_password_failed_attempt_count")).isEqualTo(4);
        assertThat(memberColumn("simple_password_locked_until")).isNull();

        // 5회째: 잠금과 함께 예외가 난다.
        assertThatThrownBy(() -> simplePasswordService.verifySimplePassword(
            memberId, new SimplePasswordVerifyRequest("999999")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);

        // noRollbackFor 덕분에 예외가 나도 잠금 기록은 DB에 남아 있어야 한다.
        assertThat(intColumn("simple_password_failed_attempt_count")).isEqualTo(5);
        assertThat(memberColumn("simple_password_locked_until")).isNotNull();

        // 잠긴 동안에는 맞는 비밀번호를 넣어도 통과시키지 않는다.
        assertThatThrownBy(() -> simplePasswordService.verifySimplePassword(
            memberId, new SimplePasswordVerifyRequest(SIMPLE_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue(
                "errorCode", ErrorCode.SIMPLE_PASSWORD_ATTEMPT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("간편비밀번호를 설정하지 않았으면 검증 시 SIMPLE_PASSWORD_NOT_SET")
    void 설정하지_않았으면_검증할_수_없다() {
        assertThatThrownBy(() -> simplePasswordService.verifySimplePassword(
            memberId, new SimplePasswordVerifyRequest(SIMPLE_PASSWORD)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIMPLE_PASSWORD_NOT_SET);
    }

    // ── 픽스처 ────────────────────────────────────────────────────────────

    private SimplePasswordUpdateRequest updateRequest() {
        return new SimplePasswordUpdateRequest(CHANGE_TOKEN, SIMPLE_PASSWORD, SIMPLE_PASSWORD);
    }

    /** 이메일 인증까지 끝난(VERIFIED) 상태의 인증 행을 심는다. */
    private long insertVerifiedToken() {
        return insert(
            "INSERT INTO simple_password_verification ("
                + "member_id, verification_code_hash, verification_status, "
                + "verification_code_expires_at, change_token_hash, change_token_expires_at, verified_at) "
                + "VALUES (?, ?, 'VERIFIED', NOW() + INTERVAL 5 MINUTE, ?, NOW() + INTERVAL 10 MINUTE, NOW())",
            memberId, tokenHashUtil.sha256("123456"), tokenHashUtil.sha256(CHANGE_TOKEN));
    }

    private void givenSimplePasswordSet() {
        jdbc.update("UPDATE member SET simple_password_hash = ? WHERE member_id = ?",
            passwordEncoder.encode(SIMPLE_PASSWORD), memberId);
    }

    private String memberColumn(String column) {
        return jdbc.queryForObject(
            "SELECT " + column + " FROM member WHERE member_id = ?", String.class, memberId);
    }

    private int intColumn(String column) {
        return jdbc.queryForObject(
            "SELECT " + column + " FROM member WHERE member_id = ?", Integer.class, memberId);
    }

    private String verificationColumn(long verificationId, String column) {
        return jdbc.queryForObject(
            "SELECT " + column + " FROM simple_password_verification "
                + "WHERE simple_password_verification_id = ?",
            String.class, verificationId);
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
