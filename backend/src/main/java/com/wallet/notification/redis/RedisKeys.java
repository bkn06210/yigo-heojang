package com.wallet.notification.redis;

import java.time.LocalDate;

/** 규칙 문서 6장의 Redis 키 형식을 한곳에 모아둔다. */
public final class RedisKeys {
    private RedisKeys() {
    }

    public static String lockKey(String jobName, LocalDate date) {
        return "noti:lock:%s:%s".formatted(jobName, date);
    }

    public static String unreadCountKey(long memberId) {
        return "noti:unread:%d".formatted(memberId);
    }
}