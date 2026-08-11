package com.wallet.notification.batch.assembler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.BenefitLimitStatus;
import com.wallet.notification.batch.model.BenefitLimitUnit;
import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.batch.model.PerformanceShortageTrigger;
import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationType;
import com.wallet.notification.redis.NotificationUnreadCountCacheRepository;
import com.wallet.notification.repository.NotificationRepository;

class NotificationComposerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private NotificationRepository notificationRepository;
    private NotificationComposer composer;
    private NotificationUnreadCountCacheRepository unreadCountCacheRepository;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 8, 24, 9, 0).atZone(KST).toInstant(), KST
        );
        unreadCountCacheRepository = mock(NotificationUnreadCountCacheRepository.class);
        composer = new NotificationComposer(notificationRepository, clock, unreadCountCacheRepository);
    }

    private PerformanceShortageCandidate shortageCandidate(long memberId, long userCardId, String dedupKey) {
        return new PerformanceShortageCandidate(
            memberId, userCardId, userCardId, "카드" + userCardId,
            300_000L, 100_000L, 200_000L, 33.3,
            PerformanceShortageTrigger.D7, "2026-08", dedupKey
        );
    }

    private BenefitLimitCandidate individualLimitCandidate(long memberId, long userCardId, long benefitId, String dedupKey) {
        return new BenefitLimitCandidate(
            memberId, userCardId, userCardId, "카드" + userCardId,
            BenefitLimitUnit.INDIVIDUAL, benefitId, "혜택" + benefitId, null,
            10_000L, 9_000L, 90.0, BenefitLimitStatus.NEAR, "2026-08", dedupKey
        );
    }

    private BenefitLimitCandidate groupLimitCandidate(long memberId, long userCardId, String groupCode, String dedupKey) {
        return new BenefitLimitCandidate(
            memberId, userCardId, userCardId, "카드" + userCardId,
            BenefitLimitUnit.GROUP, null, null, groupCode,
            20_000L, 18_000L, 90.0, BenefitLimitStatus.NEAR, "2026-08", dedupKey
        );
    }

    @Test
    @DisplayName("실적 부족 - 3건 이하면 후보별로 개별 알림을 만든다")
    void compose_shortage_individual_whenThreeOrFewer() {
        // given
        List<PerformanceShortageCandidate> candidates = List.of(
            shortageCandidate(1L, 10L, "KEY_A"),
            shortageCandidate(1L, 11L, "KEY_B")
        );

        // when
        List<Notification> result = composer.compose(candidates, List.of());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.getNotificationType() == NotificationType.PERFORMANCE_SHORTAGE);
        assertThat(result).extracting(Notification::getDeduplicationKey)
            .containsExactlyInAnyOrder("KEY_A", "KEY_B");
        assertThat(result).allMatch(n -> n.getUserCardId() != null);
    }

    @Test
    @DisplayName("실적 부족 - 3건을 초과하면 다이제스트 1건으로 합친다")
    void compose_shortage_digest_whenMoreThanThree() {
        // given: 4건 중 상위 3건이 미리보기에 들어가고 1건은 '외 1개'로 빠진다.
        List<PerformanceShortageCandidate> candidates = List.of(
            shortageCandidate(1L, 10L, "KEY_A"),
            shortageCandidate(1L, 11L, "KEY_B"),
            shortageCandidate(1L, 12L, "KEY_C"),
            shortageCandidate(1L, 13L, "KEY_D")
        );

        // when
        List<Notification> result = composer.compose(candidates, List.of());

        // then
        assertThat(result).hasSize(1);
        Notification digest = result.get(0);
        assertThat(digest.getNotificationType()).isEqualTo(NotificationType.PERFORMANCE_SHORTAGE_DIGEST);
        assertThat(digest.getDeduplicationKey()).isEqualTo("PERF_SHORTAGE_DIGEST:MONTH:2026-08:D7");
        // 다이제스트는 특정 카드에 묶이지 않으므로 userCardId가 없어야 한다.
        assertThat(digest.getUserCardId()).isNull();
        assertThat(digest.getTitle()).contains("카드10 외 3건");
        assertThat(digest.getContent()).contains("카드10").contains("카드13");
    }

    @Test
    @DisplayName("혜택 한도 - 개별 단위는 benefitId를 채우고, 그룹 단위는 채우지 않는다")
    void compose_limit_individual_unitDeterminesBenefitId() {
        // given
        List<BenefitLimitCandidate> candidates = List.of(
            individualLimitCandidate(1L, 20L, 100L, "KEY_INDIVIDUAL"),
            groupLimitCandidate(1L, 21L, "LIVING", "KEY_GROUP")
        );

        // when
        List<Notification> result = composer.compose(List.of(), candidates);

        // then
        assertThat(result).hasSize(2);

        Notification individual = result.stream()
            .filter(n -> n.getDeduplicationKey().equals("KEY_INDIVIDUAL"))
            .findFirst().orElseThrow();
        assertThat(individual.getBenefitId()).isEqualTo(100L);
        assertThat(individual.getUserCardId()).isEqualTo(20L);

        Notification group = result.stream()
            .filter(n -> n.getDeduplicationKey().equals("KEY_GROUP"))
            .findFirst().orElseThrow();
        assertThat(group.getBenefitId()).isNull();
        assertThat(group.getUserCardId()).isEqualTo(21L);
    }

    @Test
    @DisplayName("혜택 한도 - 4건을 초과하면 다이제스트 1건으로 합치고, 해시는 전체 후보의 dedup key로 계산한다")
    void compose_limit_digest_hashCoversAllCandidates() {
        // given
        List<BenefitLimitCandidate> candidates = List.of(
            individualLimitCandidate(1L, 20L, 100L, "BENEFIT_LIMIT:B:20:100:2026-08:NEAR"),
            individualLimitCandidate(1L, 21L, 101L, "BENEFIT_LIMIT:B:21:101:2026-08:NEAR"),
            individualLimitCandidate(1L, 22L, 102L, "BENEFIT_LIMIT:B:22:102:2026-08:EXHAUSTED"),
            individualLimitCandidate(1L, 23L, 103L, "BENEFIT_LIMIT:B:23:103:2026-08:NEAR")
        );

        // when
        List<Notification> result = composer.compose(List.of(), candidates);

        // then
        assertThat(result).hasSize(1);
        Notification digest = result.get(0);
        assertThat(digest.getNotificationType()).isEqualTo(NotificationType.BENEFIT_LIMIT_DIGEST);

        String expectedHash = expectedHash(candidates.stream()
            .map(BenefitLimitCandidate::deduplicationKey)
            .toList());
        assertThat(digest.getDeduplicationKey()).isEqualTo("BENEFIT_LIMIT_DIGEST:2026-08:" + expectedHash);
        assertThat(digest.getUserCardId()).isNull();
        assertThat(digest.getBenefitId()).isNull();
    }

    @Test
    @DisplayName("회원별로 독립적으로 판단한다 - 한 회원이 다이제스트여도 다른 회원은 개별 알림일 수 있다")
    void compose_groupsByMemberIndependently() {
        // given: 회원 1은 2건(개별), 회원 2는 4건(다이제스트)
        List<PerformanceShortageCandidate> candidates = List.of(
            shortageCandidate(1L, 10L, "M1_A"),
            shortageCandidate(1L, 11L, "M1_B"),
            shortageCandidate(2L, 20L, "M2_A"),
            shortageCandidate(2L, 21L, "M2_B"),
            shortageCandidate(2L, 22L, "M2_C"),
            shortageCandidate(2L, 23L, "M2_D")
        );

        // when
        List<Notification> result = composer.compose(candidates, List.of());

        // then
        List<Notification> member1Notifications = result.stream()
            .filter(n -> n.getMemberId().equals(1L)).toList();
        List<Notification> member2Notifications = result.stream()
            .filter(n -> n.getMemberId().equals(2L)).toList();

        assertThat(member1Notifications).hasSize(2)
            .allMatch(n -> n.getNotificationType() == NotificationType.PERFORMANCE_SHORTAGE);
        assertThat(member2Notifications).hasSize(1)
            .allMatch(n -> n.getNotificationType() == NotificationType.PERFORMANCE_SHORTAGE_DIGEST);
    }

    @Test
    @DisplayName("같은 회원이라도 실적 부족과 혜택 한도는 서로 다른 그룹으로 판단해 섞이지 않는다")
    void compose_shortageAndLimitNeverMixed() {
        // given: 실적 부족 2건 + 혜택 한도 2건 = 합치면 4건이지만, 유형별로는 각각 3건 이하다.
        List<PerformanceShortageCandidate> shortageCandidates = List.of(
            shortageCandidate(1L, 10L, "S_A"),
            shortageCandidate(1L, 11L, "S_B")
        );
        List<BenefitLimitCandidate> limitCandidates = List.of(
            individualLimitCandidate(1L, 20L, 100L, "L_A"),
            individualLimitCandidate(1L, 21L, 101L, "L_B")
        );

        // when
        List<Notification> result = composer.compose(shortageCandidates, limitCandidates);

        // then: 유형이 섞여 다이제스트가 되지 않고, 4건 모두 개별 알림으로 남아야 한다.
        assertThat(result).hasSize(4);
        assertThat(result).noneMatch(n ->
            n.getNotificationType() == NotificationType.PERFORMANCE_SHORTAGE_DIGEST
                || n.getNotificationType() == NotificationType.BENEFIT_LIMIT_DIGEST
        );
    }

    @Test
    @DisplayName("composeAndSave는 조립 결과를 저장 계층에 위임하고 그 반환값을 그대로 돌려준다")
    void composeAndSave_delegatesToRepositoryAndReturnsCount() {
        // given
        List<PerformanceShortageCandidate> candidates = List.of(shortageCandidate(1L, 10L, "KEY_A"));
        when(notificationRepository.saveAll(org.mockito.ArgumentMatchers.anyList())).thenReturn(1);

        // when
        int savedCount = composer.composeAndSave(candidates, List.of());

        // then
        assertThat(savedCount).isEqualTo(1);
        verify(notificationRepository).saveAll(org.mockito.ArgumentMatchers.argThat(
            list -> list.size() == 1
        ));
    }

    private String expectedHash(List<String> dedupKeys) {
        String joined = dedupKeys.stream().sorted().collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}