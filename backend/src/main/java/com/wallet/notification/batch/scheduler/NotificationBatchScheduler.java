package com.wallet.notification.batch.scheduler;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wallet.notification.batch.assembler.NotificationComposer;
import com.wallet.notification.batch.calculator.BenefitLimitCandidateCalculator;
import com.wallet.notification.batch.calculator.PerformanceShortageCandidateCalculator;
import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.redis.DistributedLockRepository;

/**
 * 실적 부족·혜택 한도 알림 배치를 KST 기준 매일 실행한다 (규칙 문서 5장).
 * <p>
 * 각 Job은 "계산(계산기) → 조립·저장·캐시 반영(NotificationComposer)"의 순서를 그대로 따르고,
 * 그 앞뒤를 Redis 분산 락으로 감싼다. 실제 계산·저장 로직은 이 클래스가 갖고 있지 않는다 —
 * 여기는 순서를 정하고, 락을 관리하고, 결과를 로깅하는 오케스트레이션만 담당한다.
 */
@Component
public class NotificationBatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(NotificationBatchScheduler.class);

    private static final String PERFORMANCE_SHORTAGE_JOB = "performance-shortage";
    private static final String BENEFIT_LIMIT_JOB = "benefit-limit";

    private final DistributedLockRepository lockRepository;
    private final PerformanceShortageCandidateCalculator shortageCalculator;
    private final BenefitLimitCandidateCalculator limitCalculator;
    private final NotificationComposer notificationComposer;
    private final int lockTtlSeconds;

    public NotificationBatchScheduler(
        DistributedLockRepository lockRepository,
        PerformanceShortageCandidateCalculator shortageCalculator,
        BenefitLimitCandidateCalculator limitCalculator,
        NotificationComposer notificationComposer,
        @Value("${redis.lock.ttl-seconds}") int lockTtlSeconds
    ) {
        this.lockRepository = lockRepository;
        this.shortageCalculator = shortageCalculator;
        this.limitCalculator = limitCalculator;
        this.notificationComposer = notificationComposer;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    /**
     * 매일 09:00(KST) 실행. cron 표현식의 zone을 명시적으로 Asia/Seoul로 고정한다 —
     * 서버가 어느 시간대에서 뜨든(e.g. 컨테이너 기본 UTC) 이 배치만은 항상 KST 09:00에 돈다.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void runPerformanceShortageJob() {
        runJob(PERFORMANCE_SHORTAGE_JOB, () -> {
            List<PerformanceShortageCandidate> candidates = shortageCalculator.calculate();
            int savedCount = notificationComposer.composeAndSave(candidates, List.of());
            return new JobResult(candidates.size(), savedCount);
        });
    }

    /** 매일 09:05(KST) 실행. 실적 부족 배치와 동시에 시작해 자원을 다투지 않도록 5분 띄웠다. */
    @Scheduled(cron = "0 5 9 * * *", zone = "Asia/Seoul")
    public void runBenefitLimitJob() {
        runJob(BENEFIT_LIMIT_JOB, () -> {
            List<BenefitLimitCandidate> candidates = limitCalculator.calculate();
            int savedCount = notificationComposer.composeAndSave(List.of(), candidates);
            return new JobResult(candidates.size(), savedCount);
        });
    }

    private void runJob(String jobName, Supplier<JobResult> job) {
        Optional<DistributedLockRepository.LockHandle> lockHandle = lockRepository.tryLock(jobName, lockTtlSeconds);
        if (lockHandle.isEmpty()) {
            // 락을 못 얻는 두 가지 경우(다른 인스턴스가 실행 중 / Redis 장애로 락 획득 자체가 실패)를
            // 여기서는 구분하지 않는다 — 어느 쪽이든 "이번 실행은 건너뛴다"는 대응이 같기 때문이다.
            log.info("[{}] 락을 획득하지 못해 이번 실행을 건너뜁니다.", jobName);
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            JobResult result = job.get();
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("[{}] 배치 완료 - 후보 {}건, 저장 {}건, 소요 {}ms",
                jobName, result.candidateCount(), result.savedCount(), elapsedMs);
        } catch (Exception e) {
            // 여기서 다시 던지지 않는다. @Scheduled 메서드가 예외를 던지면 그 실행은 실패로
            // 끝나지만, 다음 스케줄은 정상적으로 계속 돈다 — 로깅만으로 충분하고, 예외를 밖으로
            // 던지지 않아야 아래 finally의 unlock이 항상 보장된다는 걸 코드로도 명확히 드러낼 수 있다.
            log.error("[{}] 배치 실행 중 오류가 발생했습니다.", jobName, e);
        } finally {
            lockRepository.unlock(lockHandle.get());
        }
    }

    private record JobResult(int candidateCount, int savedCount) {
    }
}