package com.wallet.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 커밋 이후에만 실행돼야 하는 부수 효과(Redis 캐시 갱신 등)를 위한 유틸리티.
 *
 * @Transactional 메서드 안에서 Redis를 바로 갱신하면, 그 이후 같은 트랜잭션에서 다른 이유로
 * 롤백이 나도 Redis는 이미 바뀐 채로 남는다 — 캐시와 DB가 서로 다른 값을 가리키게 된다.
 * registerSynchronization으로 감싸두면, 커밋이 실제로 성공했을 때만 action이 실행된다.
 */
public final class AfterCommitExecutor {

    private AfterCommitExecutor() {
    }

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            // 진행 중인 트랜잭션이 없으면(단위 테스트에서 서비스만 단독으로 호출하는 경우 등)
            // "커밋 이후"라는 개념 자체가 없으므로 즉시 실행한다.
            action.run();
        }
    }
}