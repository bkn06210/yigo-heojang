package com.wallet.notification.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.notification.redis.NotificationUnreadCountCacheRepository;
import com.wallet.notification.repository.NotificationRepository;

/** 안 읽은 알림 수를 "캐시 우선, 없으면 MySQL" 전략으로 조회한다 (규칙 문서 6.2절). */
@Service
@RequiredArgsConstructor
public class NotificationUnreadCountService {
    private final NotificationUnreadCountCacheRepository cacheRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public int getUnreadCount(long memberId) {
        return cacheRepository.find(memberId)
            .orElseGet(() -> {
                int count = notificationRepository.countUnread(memberId);
                cacheRepository.save(memberId, count);
                return count;
            });
    }
}