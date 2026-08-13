package com.wallet.notification.admin;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.notification.batch.scheduler.NotificationBatchScheduler;

/**
 * 로컬 개발·수동 테스트 전용 컨트롤러. 운영 환경에 절대 노출하면 안 된다.
 * <p>
 * 이 컨트롤러는 인증 검사 없이 배치를 즉시 실행시킨다. 원래 해당 배치는 KST 09:00/09:05에만
 * 자동으로 도는데, Postman으로 "실적 부족 알림이 실제로 만들어지고 읽고 지워지는지"를
 * 그때까지 기다리지 않고 바로 확인하기 위한 용도로만 존재한다.
 * <p>
 * `@Profile("local")`이 유일한 방어선이다 — spring.profiles.active에 "local"이 명시적으로
 * 켜져 있을 때만 이 빈이 등록된다. 운영 배포 설정에는 이 프로파일을 절대 넣지 말 것.
 * 로컬에서 켜는 법: Tomcat 실행 시 VM 옵션에 -Dspring.profiles.active=local 추가,
 * 또는 환경 변수 SPRING_PROFILES_ACTIVE=local 설정.
 * <p>
 * 인증을 걸지 않은 이유: 로컬 전용이라는 전제 위에서 테스트 편의를 우선했다. 만약 이 프로파일을
 * 공유 개발 서버 등 로컬 밖에서 켜는 상황이 생긴다면, 이 컨트롤러는 그 서버에 접근 가능한
 * 누구나 배치를 무한정 실행시킬 수 있는 상태가 된다는 걸 반드시 인지하고 있어야 한다.
 */
@Profile("local")
@RestController
@RequestMapping("/api/admin/notifications/batch")
public class NotificationBatchAdminController {

    private final NotificationBatchScheduler scheduler;

    public NotificationBatchAdminController(NotificationBatchScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 지정한 배치를 즉시 1회 실행한다. Redis 락은 실제 배치와 동일하게 적용되므로,
     * 같은 잡을 짧은 간격으로 두 번 호출하면 두 번째는 "락을 못 얻어 건너뜀"으로 응답이 온다
     * (재실행 멱등성·동시 실행 시나리오를 그대로 재현해볼 수 있다).
     *
     * @param jobName "performance-shortage" 또는 "benefit-limit"
     */
    @PostMapping("/{jobName}/run")
    public ResponseEntity<ApiResponse<Void>> runJob(@PathVariable String jobName) {
        switch (jobName) {
            case "performance-shortage" -> scheduler.runPerformanceShortageJob();
            case "benefit-limit" -> scheduler.runBenefitLimitJob();
            default -> throw new BusinessException(ErrorCode.QUERY_PARAMETER_INVALID);
        }
        return ResponseEntity.ok(ApiResponse.success("%s 배치를 실행했습니다.".formatted(jobName), null));
    }
}