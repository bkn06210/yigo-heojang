package com.wallet.common.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 애플리케이션 전역에서 쓸 Clock 빈을 등록한다.
 * <p>
 * 코드 안에서 LocalDate.now()를 직접 호출하면, "오늘이 월말 7일 전인가" 같은 걸
 * 테스트하려고 실제로 그 날짜가 될 때까지 기다려야 한다. Clock을 주입받는 형태로 바꾸면
 * 테스트에서 Clock.fixed(...)로 원하는 "가짜 날짜"를 넣어 어떤 날짜든 재현할 햣수 있다.
 * <p>
 * 이 프로젝트의 모든 배치가 KST 기준으로 날짜를 판단하므로, Clock 빈 자체를 Asia/Seoul로
 * 고정해서 등록한다. 그러면 이 Clock을 쓰는 곳은 매번 시간대를 다시 맞출 필요 없이
 * LocalDate.now(clock)만 호출하면 항상 KST 기준 오늘이 나온다.
 */
@Configuration
public class ClockConfig {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 운영을 포함한 기본 환경: 항상 실제 시스템 시각을 KST로 반환한다. */
    @Bean
    @Profile("!local")
    public Clock systemClock() {
        return Clock.system(KST);
    }

    /**
     * 로컬 전용. -Dtest.clock.date=2026-08-24 VM 옵션을 주면 "오늘"을 설정한 날짜로 고정한다.
     * 옵션을 안 주면 평소처럼 실제 시각을 쓴다. D-7/D-3처럼 특정 날짜에만 동작하는 배치를
     * 그 날짜까지 기다리지 않고 테스트하기 위한 용도다.
     */
    @Bean
    @Profile("local")
    public Clock localClock() {
        String fixedDate = System.getProperty("test.clock.date");
        if (fixedDate == null || fixedDate.isBlank()) {
            return Clock.system(KST);
        }
        LocalDate date = LocalDate.parse(fixedDate); // "2026-08-24" 형식
        return Clock.fixed(date.atStartOfDay(KST).toInstant(), KST);
    }
}