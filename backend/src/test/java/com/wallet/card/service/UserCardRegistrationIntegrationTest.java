package com.wallet.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

/**
 * 카드번호 기반 보유 카드 등록의 MySQL 통합 테스트.
 *
 * 실 MySQL(db/schema.sql + db/93_seed_mock_card.sql 또는 db/migration_mock_card.sql 적용 필요)에 연결한다.
 * 단위 테스트는 매퍼를 모킹하므로 "무엇을 호출하는지"까지만 검증할 수 있다.
 * 여기서 검증하는 건 그 호출들이 실제 DB에서 통하는지다:
 *   ① 시드에 있는 번호로 등록하면 그 번호가 가리키는 카드 상품이 연결되는지
 *   ② 하이픈이 섞인 입력도 정규화되어 매칭되는지
 *   ③ 전체 번호가 아니라 마스킹된 번호만 user_card에 저장되는지
 *   ④ 시드에 없는 번호와 비활성 번호가 모두 CARD_NOT_SUPPORTED로 막히는지
 *   ⑤ 삭제한 카드를 다시 등록하면 새 행이 아니라 기존 행이 되살아나는지
 *
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class UserCardRegistrationIntegrationTest {

    private static final String EMAIL = "card-register-it@test.local";

    @Autowired
    private UserCardService userCardService;

    private JdbcTemplate jdbc;

    private long memberId;
    private long seededCardId;
    private String seededCardNumber;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);

        memberId = insert(
            "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
            EMAIL, "x", "카드등록테스트", "카드IT별명");

        // 시드에 들어 있는 활성 Mock 카드 하나를 골라 쓴다.
        // 번호를 테스트에 하드코딩하면 시드가 바뀔 때 같이 깨지므로 DB에서 읽어온다.
        seededCardNumber = jdbc.queryForObject(
            "SELECT mc.card_number FROM mock_card mc "
                + "INNER JOIN card c ON c.card_id = mc.card_id "
                + "WHERE mc.is_active = 'Y' AND c.is_active = 'Y' "
                + "ORDER BY mc.mock_card_id LIMIT 1",
            String.class);
        seededCardId = jdbc.queryForObject(
            "SELECT card_id FROM mock_card WHERE card_number = ?",
            Long.class, seededCardNumber);
    }

    @Test
    @DisplayName("시드에 있는 카드번호로 등록하면 그 번호가 가리키는 카드 상품이 연결된다")
    void 카드번호로_카드상품이_자동_연결된다() {
        UserCardRegisterResponse response = userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber));

        // 클라이언트는 cardId를 보내지 않았는데 서버가 매칭해 응답에 담아준다.
        assertThat(response.cardId()).isEqualTo(seededCardId);
        assertThat(response.userCardId()).isNotNull();
        assertThat(response.cardName()).isNotBlank();
        assertThat(response.issuerName()).isNotBlank();
        assertThat(response.cardType()).isNotBlank();

        // DB에도 그 카드 상품으로 저장돼야 한다.
        assertThat(jdbc.queryForObject(
            "SELECT card_id FROM user_card WHERE user_card_id = ?",
            Long.class, response.userCardId()))
            .isEqualTo(seededCardId);
    }

    @Test
    @DisplayName("하이픈이 섞인 카드번호도 정규화되어 같은 카드로 매칭된다")
    void 하이픈이_섞여도_매칭된다() {
        String hyphenated = seededCardNumber.replaceAll("(\\d{4})(?=\\d)", "$1-");

        UserCardRegisterResponse response = userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(hyphenated));

        assertThat(hyphenated).contains("-");
        assertThat(response.cardId()).isEqualTo(seededCardId);
    }

    @Test
    @DisplayName("전체 카드번호는 저장하지 않고 마지막 4자리만 남긴 마스킹 값만 저장한다")
    void 전체_카드번호는_저장하지_않는다() {
        UserCardRegisterResponse response = userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber));

        String stored = jdbc.queryForObject(
            "SELECT masked_card_number FROM user_card WHERE user_card_id = ?",
            String.class, response.userCardId());

        assertThat(stored).doesNotContain(seededCardNumber);
        assertThat(stored).endsWith(seededCardNumber.substring(seededCardNumber.length() - 4));
        assertThat(stored).startsWith("****");
    }

    @Test
    @DisplayName("Mock 목록에 없는 카드번호는 CARD_NOT_SUPPORTED로 막힌다")
    void 목록에_없는_번호는_등록할_수_없다() {
        // 룬 검증은 통과하지만 mock_card에 없는 번호다.
        String unknownButValid = "4242424242424242";

        assertThatThrownBy(() -> userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(unknownButValid)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_SUPPORTED);

        assertThat(countUserCards()).isZero();
    }

    @Test
    @DisplayName("비활성화된 Mock 카드번호도 CARD_NOT_SUPPORTED로 막힌다")
    void 비활성_번호는_등록할_수_없다() {
        jdbc.update("UPDATE mock_card SET is_active = 'N' WHERE card_number = ?",
            seededCardNumber);

        assertThatThrownBy(() -> userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NOT_SUPPORTED);

        assertThat(countUserCards()).isZero();
    }

    @Test
    @DisplayName("룬 검증을 통과하지 못하는 카드번호는 CARD_NUMBER_INVALID로 막힌다")
    void 형식이_틀린_번호는_등록할_수_없다() {
        assertThatThrownBy(() -> userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest("1234-5678-0000-0000")))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CARD_NUMBER_INVALID);
    }

    @Test
    @DisplayName("이미 등록한 카드를 다시 등록하면 USER_CARD_ALREADY_EXISTS로 막힌다")
    void 같은_카드를_두_번_등록할_수_없다() {
        userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber));

        assertThatThrownBy(() -> userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber)))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_CARD_ALREADY_EXISTS);

        assertThat(countUserCards()).isEqualTo(1);
    }

    @Test
    @DisplayName("삭제한 카드를 다시 등록하면 새 행을 만들지 않고 기존 행을 되살린다")
    void 삭제한_카드를_다시_등록하면_재활성화된다() {
        UserCardRegisterResponse first = userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber));

        userCardService.deleteUserCard(memberId, first.userCardId());

        UserCardRegisterResponse second = userCardService.registerUserCard(
            memberId, new UserCardRegisterRequest(seededCardNumber));

        // 같은 user_card_id가 다시 살아나야 한다.
        assertThat(second.userCardId()).isEqualTo(first.userCardId());
        assertThat(countUserCards()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT card_status FROM user_card WHERE user_card_id = ?",
            String.class, first.userCardId()))
            .isEqualTo("ACTIVE");
    }

    private int countUserCards() {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_card WHERE member_id = ?", Integer.class, memberId);
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
