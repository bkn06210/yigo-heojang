package com.wallet.notification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

class NotificationUnreadCountCacheRepositoryTest {
    private JedisPool jedisPool;
    private Jedis jedis;
    private NotificationUnreadCountCacheRepository cacheRepository;

    @BeforeEach
    void setUp() {
        jedisPool = mock(JedisPool.class);
        jedis = mock(Jedis.class);
        when(jedisPool.getResource()).thenReturn(jedis);
        cacheRepository = new NotificationUnreadCountCacheRepository(jedisPool, 3600);
    }

    @Test
    @DisplayName("캐시 hit이면 저장된 값을 반환한다")
    void find_hit() {
        // given
        when(jedis.get("noti:unread:1")).thenReturn("5");

        // when
        Optional<Integer> result = cacheRepository.find(1L);

        // then
        assertThat(result).contains(5);
    }

    @Test
    @DisplayName("캐시 miss면 empty를 반환한다")
    void find_miss() {
        // given
        when(jedis.get("noti:unread:1")).thenReturn(null);

        // when & then
        assertThat(cacheRepository.find(1L)).isEmpty();
    }

    @Test
    @DisplayName("Redis 조회 장애 시 예외를 던지지 않고 empty를 반환한다")
    void find_redisFailure() {
        // given
        when(jedis.get("noti:unread:1")).thenThrow(new JedisException("timeout"));

        // when & then
        assertThat(cacheRepository.find(1L)).isEmpty();
    }

    @Test
    @DisplayName("save는 TTL과 함께 SETEX로 저장한다")
    void save_setsWithTtl() {
        // when
        cacheRepository.save(1L, 7);

        // then
        verify(jedis).setex("noti:unread:1", 3600, "7");
    }

    @Test
    @DisplayName("increment는 INCRBY로 늘린다")
    void increment_appliesPositiveDelta() {
        // given
        when(jedis.incrBy("noti:unread:1", 3)).thenReturn(8L);

        // when
        cacheRepository.increment(1L, 3);

        // then
        verify(jedis).incrBy("noti:unread:1", 3);
        verify(jedis, never()).del("noti:unread:1");
    }

    @Test
    @DisplayName("decrement 결과가 음수가 되면 키를 삭제한다")
    void decrement_negativeResult_deletesKey() {
        // given: 캐시가 이미 0인데 2를 더 줄이면 -2가 된다.
        when(jedis.incrBy("noti:unread:1", -2)).thenReturn(-2L);

        // when
        cacheRepository.decrement(1L, 2);

        // then
        verify(jedis).del("noti:unread:1");
    }

    @Test
    @DisplayName("증감 중 Redis 장애가 나면 캐시를 무효화한다")
    void applyDelta_redisFailure_invalidatesCache() {
        // given
        when(jedis.incrBy("noti:unread:1", -1)).thenThrow(new JedisException("timeout"));

        // when
        cacheRepository.decrement(1L, 1);

        // then: 실패 복구용으로 invalidate가 별도 호출된다(같은 key에 del 시도).
        verify(jedis, times(1)).del("noti:unread:1");
    }

    @Test
    @DisplayName("delta가 0이면 Redis를 호출하지 않는다")
    void applyDelta_zeroDelta_noOp() {
        // when
        cacheRepository.increment(1L, 0);

        // then
        verify(jedis, never()).incrBy(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}