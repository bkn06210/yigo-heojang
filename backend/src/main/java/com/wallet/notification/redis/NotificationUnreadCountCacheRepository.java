package com.wallet.notification.redis;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * 규칙 문서 6.2절: 안 읽은 알림 수 캐시.
 * <p>
 * 모든 메서드가 Redis 장애를 예외로 던지지 않고 흡수한다. 캐시는 "있으면 빠르게, 없으면
 * MySQL로" 동작해야지, 캐시가 장애 났다고 API 전체가 죽으면 캐시를 두는 의미가 없다
 * (규칙 문서: "Redis 조회 장애 시 MySQL COUNT로 응답한다").
 */
@Repository
public class NotificationUnreadCountCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(NotificationUnreadCountCacheRepository.class);

    private final JedisPool jedisPool;
    private final int ttlSeconds;

    public NotificationUnreadCountCacheRepository(
        JedisPool jedisPool,
        @Value("${redis.unread-count.ttl-seconds}") int ttlSeconds
    ) {
        this.jedisPool = jedisPool;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * 캐시에 값이 있으면 그 값을, 없거나(miss) Redis 장애면 empty를 반환한다.
     */
    public Optional<Integer> find(long memberId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(RedisKeys.unreadCountKey(memberId));
            if (value == null) {
                return Optional.empty();
            }
            
            return Optional.of(Integer.parseInt(value));
        } catch (JedisException | NumberFormatException e) {
            log.warn("Redis 안 읽은 수 캐시 조회 실패 - MySQL로 대체합니다. memberId={}", memberId, e);
            return Optional.empty();
        }
    }

    /**
     * MySQL에서 막 센 값을 캐시에 채운다(캐시 miss 복구).
     */
    public void save(long memberId, int unreadCount) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(RedisKeys.unreadCountKey(memberId), ttlSeconds, String.valueOf(unreadCount));
        } catch (JedisException e) {
            log.warn("Redis 안 읽은 수 캐시 저장 실패 - 다음 조회에서 다시 MySQL로 채워집니다. memberId={}", memberId, e);
        }
    }

    /**
     * 알림이 새로 생성된 만큼 늘린다.
     */
    public void increment(long memberId, int delta) {
        applyDelta(memberId, delta);
    }

    /**
     * 알림을 읽거나 삭제한 만큼 줄인다.
     */
    public void decrement(long memberId, int delta) {
        applyDelta(memberId, -delta);
    }

    private void applyDelta(long memberId, long delta) {
        if (delta == 0) {
            return;
        }
        String key = RedisKeys.unreadCountKey(memberId);
        try (Jedis jedis = jedisPool.getResource()) {
            long newValue = jedis.incrBy(key, delta);
            // 규칙 문서 6.2절: "값이 음수가 되면 키를 삭제하고 다음 조회에서 MySQL로 복구한다."
            // 캐시가 실제보다 먼저 줄어드는 순서 꼬임이 생기면 음수가 될 수 있는데,
            // 0으로 어설프게 고치지 않고 아예 지워서 다음 조회 때 MySQL로 정확히 다시 채운다.
            if (newValue < 0) {
                jedis.del(key);
            }
        } catch (JedisException e) {
            // 증감이 실제로 반영됐는지 알 수 없는 상태다. "불확실하면 삭제" 원칙을 그대로 따른다.
            log.warn("Redis 안 읽은 수 캐시 증감 실패 - 캐시를 무효화합니다. memberId={}", memberId, e);
            invalidate(memberId);
        }
    }

    /**
     * 캐시 값을 신뢰할 수 없을 때(불확실한 갱신, 벌크 INSERT 등) 통째로 지운다.
     */
    public void invalidate(long memberId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(RedisKeys.unreadCountKey(memberId));
        } catch (JedisException e) {
            log.warn("Redis 안 읽은 수 캐시 무효화 실패 - 다음 조회에서 다시 MySQL로 채워집니다. memberId={}", memberId, e);
        }
    }
}