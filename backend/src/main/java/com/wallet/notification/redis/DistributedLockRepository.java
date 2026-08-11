package com.wallet.notification.redis;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

/**
 * 규칙 문서 6.1절: KST 일 배치가 여러 인스턴스에서 동시에 돌지 않도록 막는 분산 락.
 * <p>
 * SET NX EX는 "키가 없을 때만 설정하고 TTL도 같이 건다"를 한 번의 원자적 명령으로 처리한다.
 * "있는지 확인" + "없으면 설정"을 각각 별도 명령으로 나누면 그 사이에 다른 인스턴스가 끼어들어
 * 둘 다 락을 잡는 경쟁 상태가 생길 수 있는데, NX EX는 그 틈을 원천적으로 없앤다.
 */
@Repository
@RequiredArgsConstructor
public class DistributedLockRepository {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockRepository.class);

    // 저장된 값이 자기 instanceId와 같을 때만 지운다. 이걸 GET → 비교 → DEL로 나눠서 하면,
    // 확인하는 사이에 TTL이 만료되고 다른 인스턴스가 같은 키로 새 락을 잡은 뒤 내가 뒤늦게
    // DEL을 호출해 남의 락을 지워버리는 사고가 날 수 있다. Lua 스크립트는 Redis 안에서
    // 한 번에 원자적으로 실행되므로 이 틈이 없다.
    private static final String UNLOCK_SCRIPT =
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final JedisPool jedisPool;
    private final Clock clock;
    
    /**
     * 오늘 날짜 기준으로 jobName에 대한 락을 시도한다.
     *
     * @param lockTtlSeconds 정상 최대 배치 시간보다 길게 잡아야 한다 — 배치가 끝나기 전에
     *                       TTL이 만료되면 다른 인스턴스가 같은 락을 잡아 중복 실행될 수 있다.
     * @return 락을 획득했으면 소유권을 나타내는 LockHandle, 이미 다른 인스턴스가 실행 중이면 empty
     */
    public Optional<LockHandle> tryLock(String jobName, int lockTtlSeconds) {
        String key = RedisKeys.lockKey(jobName, LocalDate.now(clock));
        String instanceId = UUID.randomUUID().toString();

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, instanceId, SetParams.setParams().nx().ex(lockTtlSeconds));
            if (result == null) {
                // NX 조건 때문에 값을 설정하지 못했다 = 이미 다른 인스턴스가 오늘 이 잡을 실행 중이다.
                return Optional.empty();
            }
            return Optional.of(new LockHandle(key, instanceId));
        } catch (JedisException e) {
            // Redis 장애 시 "락을 못 얻은 것"으로 처리해 배치를 건너뛴다.
            // "락 없이 중복 실행"보다 "이번 배치를 건너뛰고 다음 스케줄에서 재시도"가 더 안전하다.
            log.warn("Redis 락 획득 실패 - Redis 장애로 간주하고 이번 실행을 건너뜁니다. jobName={}", jobName, e);
            return Optional.empty();
        }
    }

    /** 저장된 값이 내 instanceId와 같을 때만 락을 해제한다. */
    public void unlock(LockHandle handle) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.eval(UNLOCK_SCRIPT,
                Collections.singletonList(handle.key()),
                Collections.singletonList(handle.instanceId()));
        } catch (JedisException e) {
            // 여기서 실패해도 TTL이 있어 결국 자동 만료되므로 배치 실행 자체를 실패로 보지 않는다.
            log.warn("Redis 락 해제 실패 - TTL 만료로 자동 해제될 때까지 기다립니다. key={}", handle.key(), e);
        }
    }

    /** 이 인스턴스가 지금 들고 있는 락을 식별하는 값. key와 instanceId를 함께 들고 있어야 해제할 수 있다. */
    public record LockHandle(String key, String instanceId) {
    }
}