package com.wallet.notification.batch.scheduler;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.batch.assembler.NotificationComposer;
import com.wallet.notification.batch.calculator.BenefitLimitCandidateCalculator;
import com.wallet.notification.batch.calculator.PerformanceShortageCandidateCalculator;
import com.wallet.notification.batch.model.BenefitLimitCandidate;
import com.wallet.notification.batch.model.BenefitLimitStatus;
import com.wallet.notification.batch.model.BenefitLimitUnit;
import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.batch.model.PerformanceShortageTrigger;
import com.wallet.notification.redis.DistributedLockRepository;

class NotificationBatchSchedulerTest {
    private DistributedLockRepository lockRepository;
    private PerformanceShortageCandidateCalculator shortageCalculator;
    private BenefitLimitCandidateCalculator limitCalculator;
    private NotificationComposer notificationComposer;
    private NotificationBatchScheduler scheduler;

    private static final int LOCK_TTL_SECONDS = 600;

    @BeforeEach
    void setUp() {
        lockRepository = mock(DistributedLockRepository.class);
        shortageCalculator = mock(PerformanceShortageCandidateCalculator.class);
        limitCalculator = mock(BenefitLimitCandidateCalculator.class);
        notificationComposer = mock(NotificationComposer.class);
        scheduler = new NotificationBatchScheduler(
            lockRepository, shortageCalculator, limitCalculator, notificationComposer, LOCK_TTL_SECONDS
        );
    }

    private DistributedLockRepository.LockHandle handle(String jobName) {
        return new DistributedLockRepository.LockHandle("noti:lock:" + jobName + ":2026-08-24", "instance-1");
    }

    private PerformanceShortageCandidate shortageCandidate() {
        return new PerformanceShortageCandidate(
            1L, 10L, 10L, "카드A", 300_000L, 100_000L, 200_000L, 33.3,
            PerformanceShortageTrigger.D7, "2026-08", "PERF_SHORTAGE:10:MONTH:2026-08:D7"
        );
    }

    private BenefitLimitCandidate limitCandidate() {
        return new BenefitLimitCandidate(
            1L, 20L, 20L, "카드B", BenefitLimitUnit.INDIVIDUAL, 100L, "혜택A", null,
            10_000L, 9_000L, 90.0, BenefitLimitStatus.NEAR, "2026-08", "BENEFIT_LIMIT:B:20:100:2026-08:NEAR"
        );
    }

    @Test
    @DisplayName("실적 부족 배치 - 락 획득 성공 시 계산→조립·저장 순서로 실행하고 락을 해제한다")
    void runPerformanceShortageJob_success_runsAndUnlocks() {
        // given
        DistributedLockRepository.LockHandle lockHandle = handle("performance-shortage");
        when(lockRepository.tryLock("performance-shortage", LOCK_TTL_SECONDS)).thenReturn(Optional.of(lockHandle));

        List<PerformanceShortageCandidate> candidates = List.of(shortageCandidate());
        when(shortageCalculator.calculate()).thenReturn(candidates);
        when(notificationComposer.composeAndSave(candidates, List.of())).thenReturn(1);

        // when
        scheduler.runPerformanceShortageJob();

        // then
        verify(shortageCalculator).calculate();
        verify(notificationComposer).composeAndSave(candidates, List.of());
        verify(lockRepository).unlock(lockHandle);
    }

    @Test
    @DisplayName("실적 부족 배치 - 락을 못 얻으면 계산·저장을 하지 않는다")
    void runPerformanceShortageJob_lockNotAcquired_skipsExecution() {
        // given
        when(lockRepository.tryLock("performance-shortage", LOCK_TTL_SECONDS)).thenReturn(Optional.empty());

        // when
        scheduler.runPerformanceShortageJob();

        // then
        verify(shortageCalculator, never()).calculate();
        verify(notificationComposer, never()).composeAndSave(anyList(), anyList());
        verify(lockRepository, never()).unlock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("실적 부족 배치 - 실행 중 예외가 나도 락은 반드시 해제된다")
    void runPerformanceShortageJob_exceptionDuringJob_stillUnlocks() {
        // given
        DistributedLockRepository.LockHandle lockHandle = handle("performance-shortage");
        when(lockRepository.tryLock("performance-shortage", LOCK_TTL_SECONDS)).thenReturn(Optional.of(lockHandle));
        when(shortageCalculator.calculate()).thenThrow(new RuntimeException("DB 조회 실패"));

        // when & then: 예외가 스케줄러 밖으로 전파되지 않아야 한다.
        scheduler.runPerformanceShortageJob();

        verify(lockRepository).unlock(lockHandle);
    }

    @Test
    @DisplayName("혜택 한도 배치 - 실적 부족과 다른 잡 이름으로 락을 잡고, 혜택 한도 계산기만 호출한다")
    void runBenefitLimitJob_success_usesOwnJobNameAndCalculator() {
        // given
        DistributedLockRepository.LockHandle lockHandle = handle("benefit-limit");
        when(lockRepository.tryLock("benefit-limit", LOCK_TTL_SECONDS)).thenReturn(Optional.of(lockHandle));

        List<BenefitLimitCandidate> candidates = List.of(limitCandidate());
        when(limitCalculator.calculate()).thenReturn(candidates);
        when(notificationComposer.composeAndSave(List.of(), candidates)).thenReturn(1);

        // when
        scheduler.runBenefitLimitJob();

        // then
        verify(limitCalculator).calculate();
        verify(shortageCalculator, never()).calculate();
        verify(notificationComposer).composeAndSave(List.of(), candidates);
        verify(lockRepository).unlock(lockHandle);
    }
}