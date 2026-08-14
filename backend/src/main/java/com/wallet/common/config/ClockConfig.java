package com.wallet.common.config;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 애플리케이션 전역에서 쓸 Clock 빈을 등록한다.
 * <p>
 * 코드 안에서 LocalDateTime.now()를 직접 호출하면, "5분 잠금이 풀렸는가" 같은 걸
 * 테스트하려고 실제로 5분을 기다려야 한다. Clock을 주입받는 형태로 바꾸면
 * 테스트에서 Clock.fixed(...)로 원하는 시각을 넣어 어떤 상황이든 재현할 수 있다.
 * <p>
 * 이 프로젝트는 KST 기준으로 날짜·시각을 판단하므로 Clock 빈 자체를 Asia/Seoul로
 * 고정해서 등록한다. 그러면 이 Clock을 쓰는 곳은 매번 시간대를 다시 맞출 필요 없이
 * LocalDateTime.now(clock)만 호출하면 항상 KST 기준 현재 시각이 나온다.
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
     * 로컬 전용. -Dtest.clock.date=2026-08-24 VM 옵션을 주면
     * 시스템 시각이 흐르는 상태로 애플리케이션의 기준 날짜를 이동한다.
     * <p>
     * 날짜 조건을 원하는 날짜로 테스트하면서도 간편비밀번호 잠금,
     * 인증 토큰 만료처럼 경과시간을 사용하는 기능이 정상 동작하도록 한다.
     */
    @Bean
    @Profile("local")
    public Clock localClock() {
        Clock systemClock = Clock.system(KST);
        String configuredDate = System.getProperty("test.clock.date");

        if (configuredDate == null || configuredDate.isBlank()) {
            return systemClock;
        }

        LocalDate targetDate = LocalDate.parse(configuredDate);
        LocalDate currentDate = LocalDate.now(systemClock);

        /*
         * 시스템 시계를 설정한 날짜까지 이동시키되, Clock.fixed와 달리
         * 실제 시간이 흐르도록 한다. 따라서 5분 잠금 같은 경과시간도 만료된다.
         */
        long offsetDays = ChronoUnit.DAYS.between(currentDate, targetDate);

        return Clock.offset(systemClock, Duration.ofDays(offsetDays));
    }
}
