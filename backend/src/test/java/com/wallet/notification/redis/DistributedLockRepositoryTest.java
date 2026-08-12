package com.wallet.notification.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

class DistributedLockRepositoryTest {
    private JedisPool jedisPool;
    private Jedis jedis;
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        jedisPool = mock(JedisPool.class);
        jedis = mock(Jedis.class);
        when(jedisPool.getResource()).thenReturn(jedis);

        Clock clock = Clock.fixed(
            LocalDate.of(2026, 8, 24).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul")
        );
        lockRepository = new DistributedLockRepository(jedisPool, clock);
    }

    @Test
    @DisplayName("락이 비어있으면 SET NX EX로 획득에 성공한다")
    void tryLock_success() {
        // given
        when(jedis.set(eq("noti:lock:performance-shortage:2026-08-24"), anyString(), any(SetParams.class)))
            .thenReturn("OK");

        // when
        Optional<DistributedLockRepository.LockHandle> handle =
            lockRepository.tryLock("performance-shortage", 600);

        // then
        assertThat(handle).isPresent();
        assertThat(handle.get().key()).isEqualTo("noti:lock:performance-shortage:2026-08-24");
    }

    @Test
    @DisplayName("다른 인스턴스가 이미 락을 잡고 있으면 획득에 실패한다")
    void tryLock_alreadyLocked() {
        // given: NX 조건 때문에 값을 설정하지 못하면 Jedis는 null을 돌려준다.
        when(jedis.set(anyString(), anyString(), any(SetParams.class))).thenReturn(null);

        // when
        Optional<DistributedLockRepository.LockHandle> handle =
            lockRepository.tryLock("performance-shortage", 600);

        // then
        assertThat(handle).isEmpty();
    }

    @Test
    @DisplayName("Redis 장애 시 락 획득 실패로 처리하고 예외를 던지지 않는다")
    void tryLock_redisFailure() {
        // given
        when(jedis.set(anyString(), anyString(), any(SetParams.class)))
            .thenThrow(new JedisException("connection refused"));

        // when
        Optional<DistributedLockRepository.LockHandle> handle =
            lockRepository.tryLock("performance-shortage", 600);

        // then
        assertThat(handle).isEmpty();
    }

    @Test
    @DisplayName("해제 시 소유자 확인 Lua 스크립트를 key·instanceId와 함께 실행한다")
    void unlock_evaluatesScriptWithKeyAndInstanceId() {
        // given
        DistributedLockRepository.LockHandle handle =
            new DistributedLockRepository.LockHandle("noti:lock:performance-shortage:2026-08-24", "instance-1");

        // when
        lockRepository.unlock(handle);

        // then
        verify(jedis, times(1)).eval(
            anyString(),
            eq(java.util.List.of("noti:lock:performance-shortage:2026-08-24")),
            eq(java.util.List.of("instance-1"))
        );
    }

    @Test
    @DisplayName("해제 중 Redis 장애가 나도 예외를 밖으로 던지지 않는다")
    void unlock_redisFailure_doesNotThrow() {
        // given
        when(jedis.eval(anyString(), anyList(), anyList())).thenThrow(new JedisException("timeout"));
        DistributedLockRepository.LockHandle handle =
            new DistributedLockRepository.LockHandle("noti:lock:performance-shortage:2026-08-24", "instance-1");

        // when & then (예외가 발생하지 않아야 한다)
        lockRepository.unlock(handle);
    }
}