package com.wallet.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.wallet.notification.batch.model.MemberDedupKeyLookup;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
class NotificationMapperIntegrationTest {

    @Autowired
    private NotificationMapper notificationMapper;

    private JdbcTemplate jdbc;
    private long firstMemberId;
    private long secondMemberId;

    @BeforeEach
    void setUp(@Autowired DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        firstMemberId = insertMember("notification-mapper-it-1@example.com");
        secondMemberId = insertMember("notification-mapper-it-2@example.com");

        insertNotification(firstMemberId, "MATCH_A");
        insertNotification(firstMemberId, "MATCH_B");
        insertNotification(secondMemberId, "MATCH_A");
    }

    @Test
    @DisplayName("회원 ID와 dedup key 쌍을 MySQL에 바인딩해 존재하는 후보만 조회한다")
    void findExistingMemberDedupKeys_returnsOnlyMatchingPairs() {
        List<String> result = notificationMapper.findExistingMemberDedupKeys(List.of(
            new MemberDedupKeyLookup(firstMemberId, "MATCH_A"),
            new MemberDedupKeyLookup(firstMemberId, "MISSING"),
            new MemberDedupKeyLookup(secondMemberId, "MATCH_A")
        ));

        assertThat(result).containsExactlyInAnyOrder(
            firstMemberId + ":MATCH_A",
            secondMemberId + ":MATCH_A"
        );
    }

    private long insertMember(String email) {
        jdbc.update(
            "INSERT INTO member (email, password_hash, name, nickname, member_status) "
                + "VALUES (?, 'x', '알림 매퍼 통합 테스트', '알림IT', 'ACTIVE')",
            email
        );
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertNotification(long memberId, String deduplicationKey) {
        jdbc.update(
            "INSERT INTO notification "
                + "(member_id, notification_type, title, content, notification_status, deduplication_key) "
                + "VALUES (?, 'BENEFIT_LIMIT', '테스트', '테스트', 'SENT', ?)",
            memberId,
            deduplicationKey
        );
    }
}
