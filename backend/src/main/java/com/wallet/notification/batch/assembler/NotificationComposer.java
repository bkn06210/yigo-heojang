package com.wallet.notification.batch.assembler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.BenefitLimitStatus;
import com.wallet.notification.batch.model.BenefitLimitUnit;
import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.domain.Notification;
import com.wallet.notification.domain.NotificationStatus;
import com.wallet.notification.domain.NotificationType;
import com.wallet.notification.repository.NotificationRepository;

/**
 * 실적 부족·혜택 한도 후보를 개별/다이제스트 알림으로 조립하고 저장한다 (규칙 문서 4장)
 * <p>
 * 이 클래스는 "어떤 후보가 있는가"는 계산기에서 이미 정해왔다고 보고,
 * "그걸 알림으로 몇 건 보여줄 것인가"(후보별 개별 알림 vs 다이제스트 1건)만 결정한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationComposer {
    private static final Locale LOCALE = Locale.KOREA;

    // 규칙 문서 4장: 후보가 3건 이하면 후보별 개별 알림, 3건을 초과하면 다이제스트 1건.
    private static final int DIGEST_THRESHOLD = 3;
    private static final int CONTENT_MAX_LENGTH = 1000; // notification.content 컬럼 길이(VARCHAR(1000))에 맞춘 안전장치

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    /**
     * 후보를 알림으로 조립해 저장하고, 실제로 새로 저장된 건수를 반환한다.
     * unique 충돌 판단이나 insert 성공 건수 계산은 여기서 다시 하지 않는다 —
     * 저장 계층(NotificationRepository.saveAll)이 이미 하는 일이라 그 결과를 그대로 쓴다.
     */
    public int composeAndSave(
        List<PerformanceShortageCandidate> shortageCandidates,
        List<BenefitLimitCandidate> limitCandidates
    ) {
        List<Notification> notifications = compose(shortageCandidates, limitCandidates);
        return notificationRepository.saveAll(notifications);
    }

    /**
     * 저장을 거치지 않는 순수 조립 로직. 저장소 없이도 조립 규칙만 독립적으로 테스트할 수 있게 분리했다.
     */
    public List<Notification> compose(
        List<PerformanceShortageCandidate> shortageCandidates,
        List<BenefitLimitCandidate> limitCandidates
    ) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 규칙 문서 4장: "실적 부족과 혜택 월 한도 후보는 서로 섞이지 않는다."
        // 같은 회원이라도 두 유형을 하나의 다이제스트로 합치지 않고, 유형별로 따로 그룹핑·판단한다.
        List<Notification> shortageNotifications = groupByMember(shortageCandidates, PerformanceShortageCandidate::memberId)
            .values().stream()
            .flatMap(candidates -> composeShortageGroup(candidates, now).stream())
            .toList();

        List<Notification> limitNotifications = groupByMember(limitCandidates, BenefitLimitCandidate::memberId)
            .values().stream()
            .flatMap(candidates -> composeLimitGroup(candidates, now).stream())
            .toList();

        return Stream.concat(shortageNotifications.stream(), limitNotifications.stream()).toList();
    }

    private <T> Map<Long, List<T>> groupByMember(List<T> candidates, Function<T, Long> memberIdExtractor) {
        // LinkedHashMap을 써서, 계산기가 이미 정렬해 둔 후보 순서를 회원별 그룹 안에서도 그대로 유지한다.
        // 이 순서가 다이제스트의 "상위 3건"을 결정하므로 순서 보존이 중요하다.
        return candidates.stream()
            .collect(Collectors.groupingBy(memberIdExtractor, LinkedHashMap::new, Collectors.toList()));
    }

    // ── 실적 부족 ─────────────────────────────────────────────

    private List<Notification> composeShortageGroup(List<PerformanceShortageCandidate> candidates, LocalDateTime now) {
        if (candidates.size() <= DIGEST_THRESHOLD) {
            return candidates.stream()
                .map(c -> toShortageIndividual(c, now))
                .toList();
        }
        return List.of(toShortageDigest(candidates, now));
    }

    private Notification toShortageIndividual(PerformanceShortageCandidate c, LocalDateTime now) {
        // 목록 화면 한 줄(title)에서 바로 급한 정도를 숫자로 보여준다.
        String title = String.format(LOCALE,
            "%s의 이번 달 실적 목표까지 %,d원 남았어요. (현재 달성률 %.1f%%)",
            c.cardName(), c.remainingPerformance(), c.achievementRate());
        // content(상세 화면)는 숫자를 반복하지 않고 "무엇을 하면 되는지"(규칙 문서 1.1절)를 안내한다.
        String content = "%s로 조금 더 사용하면 이번 달 목표를 채울 수 있어요.".formatted(c.cardName());

        return baseNotificationBuilder(c.memberId(), NotificationType.PERFORMANCE_SHORTAGE, title, content, now)
            .userCardId(c.userCardId())
            .deduplicationKey(c.deduplicationKey())
            .build();
    }

    private Notification toShortageDigest(List<PerformanceShortageCandidate> candidates, LocalDateTime now) {
        long memberId = candidates.get(0).memberId();
        String yearMonth = candidates.get(0).yearMonth();
        String triggerName = candidates.get(0).trigger().name(); // D7 | D3

        // candidates는 계산기가 이미 2.5절 기준(달성률 내림차순 등)으로 정렬해 둔 상태라,
        // 맨 앞 원소가 곧 "가장 중요한 대상"이다.
        PerformanceShortageCandidate topCandidate = candidates.get(0);
        int otherCount = candidates.size() - 1;

        String title = "%s 외 %d건, 실적이 부족해요".formatted(topCandidate.cardName(), otherCount);
        String content = joinWithLengthGuard(
            candidates.stream()
                .map(c -> String.format(LOCALE, "%s: 목표까지 %,d원 (달성률 %.1f%%)",
                    c.cardName(), c.remainingPerformance(), c.achievementRate()))
                .toList()
        );

        String dedupKey = "PERF_SHORTAGE_DIGEST:MONTH:%s:%s".formatted(yearMonth, triggerName);

        return baseNotificationBuilder(memberId, NotificationType.PERFORMANCE_SHORTAGE_DIGEST, title, content, now)
            .deduplicationKey(dedupKey)
            .build();
    }

    // ── 혜택 한도 ─────────────────────────────────────────────

    private List<Notification> composeLimitGroup(List<BenefitLimitCandidate> candidates, LocalDateTime now) {
        if (candidates.size() <= DIGEST_THRESHOLD) {
            return candidates.stream()
                .map(c -> toLimitIndividual(c, now))
                .toList();
        }
        return List.of(toLimitDigest(candidates, now));
    }

    private Notification toLimitIndividual(BenefitLimitCandidate c, LocalDateTime now) {
        String subject = limitSubjectLabel(c);
        boolean exhausted = c.status() == BenefitLimitStatus.EXHAUSTED;

        String title = exhausted
            ? String.format(LOCALE, "%s 한도(%,d원)를 모두 사용했어요", subject, c.limitAmount())
            : String.format(LOCALE, "%s 한도의 %.1f%%를 사용했어요", subject, c.usageRate());

        // 1.1절 표에 정의된 "사용자가 할 수 있는 행동"을 그대로 안내 문구로 옮겼다.
        String content = exhausted
            ? "해당 혜택 사용을 멈추고 다른 카드로 소비를 옮겨보세요."
            : "남은 한도를 계획적으로 사용하거나 다른 카드로 소비를 옮겨보세요.";

        Notification.NotificationBuilder builder =
            baseNotificationBuilder(c.memberId(), NotificationType.BENEFIT_LIMIT, title, content, now)
                .deduplicationKey(c.deduplicationKey())
                .userCardId(c.userCardId());

        if (c.unit() == BenefitLimitUnit.INDIVIDUAL) {
            builder.benefitId(c.benefitId());
        }

        return builder.build();
    }

    private Notification toLimitDigest(List<BenefitLimitCandidate> candidates, LocalDateTime now) {
        long memberId = candidates.get(0).memberId();
        String yearMonth = candidates.get(0).yearMonth();

        // candidates는 계산기가 이미 3.7절 기준(EXHAUSTED 우선 → 사용률 내림차순)으로 정렬해 둔 상태다.
        BenefitLimitCandidate topCandidate = candidates.get(0);
        int otherCount = candidates.size() - 1;

        String title = "%s 한도 외 %d건, 확인이 필요해요".formatted(limitSubjectLabel(topCandidate), otherCount);
        String content = joinWithLengthGuard(
            candidates.stream()
                .map(c -> String.format(LOCALE, "%s: %s (%.1f%%)",
                    limitSubjectLabel(c),
                    c.status() == BenefitLimitStatus.EXHAUSTED ? "소진" : "임박",
                    c.usageRate()))
                .toList()
        );

        String dedupKey = "BENEFIT_LIMIT_DIGEST:%s:%s".formatted(yearMonth, candidateSetHash(candidates));

        return baseNotificationBuilder(memberId, NotificationType.BENEFIT_LIMIT_DIGEST, title, content, now)
            .deduplicationKey(dedupKey)
            .build();
    }

    private String limitSubjectLabel(BenefitLimitCandidate c) {
        return switch (c.unit()) {
            case INDIVIDUAL -> "%s %s".formatted(c.cardName(), c.benefitName());
            case GROUP -> "%s 묶음(%s)".formatted(c.cardName(), c.limitGroupCode());
            case SHARED -> "%s 통합".formatted(c.cardName());
        };
    }

    /**
     * 규칙 문서 4장의 candidateSetHash 계산.
     * 1) dedup key를 문자열 오름차순 정렬 2) LF(\n)로 연결 3) UTF-8 바이트 4) SHA-256 5) 64자리 소문자 hex.
     * <p>
     * 화면에 보여주는 상위 3건이 아니라 다이제스트에 포함된 전체 후보를 대상으로 계산한다.
     * 그래야 안 보이는 나머지 후보가 바뀌어도 다른 다이제스트로 인식된다.
     */
    private String candidateSetHash(List<BenefitLimitCandidate> candidates) {
        String joined = candidates.stream()
            .map(BenefitLimitCandidate::deduplicationKey)
            .sorted()
            .collect(Collectors.joining("\n"));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 표준 JVM이 반드시 제공해야 하는 알고리즘이라 이 경로는 실행되지 않는다.
            // 그래도 checked exception이라 컴파일이 요구하므로, 원인을 보존해 런타임 예외로 감싼다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }

    /**
     * 후보 설명 줄들을 이어붙이되, content 컬럼 길이(1000자)를 넘기지 않도록 중간에 멈춘다.
     * "외 N개 더" 문구가 붙을 여유(20자)를 미리 빼두고 계산한다.
     */
    private String joinWithLengthGuard(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String line : lines) {
            String toAppend = (sb.isEmpty() ? "" : "\n") + line;
            if (sb.length() + toAppend.length() > CONTENT_MAX_LENGTH - 20) {
                break;
            }
            sb.append(toAppend);
            shown++;
        }
        int remaining = lines.size() - shown;
        if (remaining > 0) {
            sb.append("\n외 %d개 더".formatted(remaining));
        }
        return sb.toString();
    }

    private Notification.NotificationBuilder baseNotificationBuilder(
        long memberId, NotificationType type, String title, String content, LocalDateTime now
    ) {
        return Notification.builder()
            .memberId(memberId)
            .notificationType(type)
            .title(title)
            .content(content)
            .notificationStatus(NotificationStatus.SENT)
            .sentAt(now);
    }
}