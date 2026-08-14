package com.wallet.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

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

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

/**
 * 대표(고정) 카드 지정의 MySQL 통합 테스트.
 *
 * 화면에서 두 번째 카드를 고정하면 500이 떨어지는 문제를 좁히기 위해 만들었다.
 * 개수 제한은 서비스 트랜잭션이 검증하므로(스키마에 제약이 없다) 실제 DB에 붙어
 * 세 장까지 지정되는지, 네 장째가 409로 막히는지를 여기서 확인한다.
 *
 * @Transactional으로 픽스처를 넣고 테스트마다 롤백한다.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class UserCardRepresentativeIntegrationTest {

    private static final String EMAIL = "card-representative-it@test.local";

    @Autowired
    private UserCardService userCardService;

    private JdbcTemplate jdbc;

    private long memberId;
    private final List<Long> userCardIds = new ArrayList<>();

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        userCardIds.clear();

        memberId = insert(
            "INSERT INTO member (email, password_hash, name, nickname) VALUES (?, ?, ?, ?)",
            EMAIL, "x", "대표카드테스트", "대표IT별명");

        // 카드 상품은 시드에서 골라 쓴다. 번호를 하드코딩하면 시드가 바뀔 때 같이 깨진다.
        List<Long> cardIds = jdbc.queryForList(
            "SELECT card_id FROM card WHERE is_active = 'Y' ORDER BY card_id LIMIT 4",
            Long.class);

        for (int index = 0; index < cardIds.size(); index++) {
            userCardIds.add(insert(
                "INSERT INTO user_card "
                    + "(member_id, card_id, masked_card_number, is_representative, card_status) "
                    + "VALUES (?, ?, ?, 0, 'ACTIVE')",
                memberId, cardIds.get(index), "****-****-****-90" + index));
        }
    }

    @Test
    @DisplayName("대표 카드를 세 장까지 지정할 수 있다")
    void 대표카드를_세장까지_지정할_수_있다() {
        userCardService.updateRepresentative(memberId, userCardIds.get(0), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(1), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(2), true);

        assertThat(countRepresentative()).isEqualTo(3);
    }

    @Test
    @DisplayName("네 장째는 409(REPRESENTATIVE_CARD_LIMIT_EXCEEDED)로 막힌다")
    void 네장째는_막힌다() {
        userCardService.updateRepresentative(memberId, userCardIds.get(0), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(1), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(2), true);

        assertThatThrownBy(() ->
            userCardService.updateRepresentative(memberId, userCardIds.get(3), true))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPRESENTATIVE_CARD_LIMIT_EXCEEDED);

        assertThat(countRepresentative()).isEqualTo(3);
    }

    @Test
    @DisplayName("해제했다가 다시 지정해도 개수가 어긋나지 않는다")
    void 해제후_재지정해도_개수가_맞는다() {
        userCardService.updateRepresentative(memberId, userCardIds.get(0), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(1), true);
        userCardService.updateRepresentative(memberId, userCardIds.get(0), false);
        userCardService.updateRepresentative(memberId, userCardIds.get(2), true);

        assertThat(countRepresentative()).isEqualTo(2);
    }

    private int countRepresentative() {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_card "
                + "WHERE member_id = ? AND card_status = 'ACTIVE' AND is_representative = 1",
            Integer.class, memberId);
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
