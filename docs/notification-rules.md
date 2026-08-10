# 알림 규칙 — 최종 단순 1차안

> Redis는 일 배치 분산 락과 안 읽은 알림 수 캐시에만 사용한다. 다이제스트 구성 항목 테이블, outbox와 후보 상태 기계는 도입하지 않는다.

## 실적 부족 / 혜택 월 한도 임박·소진

## 0. 범위

### 포함

- 앱 내 알림
- MONTH 실적 부족 알림
- 월간 금액 한도 임박·소진 알림
  - 혜택 개별 월 한도
  - `limit_group_code` 그룹 월 한도
  - 카드 통합 월 한도
- 후보가 많을 때 다이제스트 전환
- Redis 일 배치 분산 락
- Redis 안 읽은 알림 수 캐시

### 제외

- 푸시와 이메일 등 외부 채널
- QUARTER 실적 부족 알림
- 일·분기·연간 금액 한도 알림
- 일·월·분기·연간 횟수 한도 알림
- 사용자별 임계값 설정
- Redis 후보 큐, Redis dedup, Redis Stream
- notification outbox와 후보 상태 기계

모든 날짜와 배치 시각은 `Asia/Seoul`(KST)을 기준으로 계산한다.

---

# 1. 공통 원칙

## 1.1 행동 가능한 알림

사용자가 알림을 확인한 뒤 취할 수 있는 행동이 있을 때 발송한다.

| 알림 유형         | 사용자가 할 수 있는 행동                                    |
| ----------------- | ----------------------------------------------------------- |
| 실적 부족         | 해당 카드로 더 사용하여 기본 실적을 채운다                  |
| 혜택 월 한도 임박 | 남은 한도를 계획적으로 사용하거나 다른 카드로 소비를 옮긴다 |
| 혜택 월 한도 소진 | 해당 혜택 사용을 멈추고 다른 카드로 소비를 옮긴다           |

## 1.2 MySQL이 최종 원본이다

알림 후보는 배치 실행 시 MySQL의 현재 상태에서 다시 계산한다. Redis에는 후보를 저장하지 않는다.

최종 중복 방지는 다음 제약으로 보장한다.

```sql
UNIQUE(member_id, deduplication_key)
```

동일 배치가 다시 실행되어 unique 충돌이 발생하면 이미 처리된 알림으로 간주한다.

## 1.3 비율

- `achievementRate`와 `usageRate`는 `0.0~100.0` 퍼센트 단위다.
- 조건 판정에는 반올림된 표시값을 사용하지 않는다.
- 원본 정수 금액으로 조건을 판정하고 화면 표시 시에만 소수점 한 자리로 반올림한다.

```text
80% 이상 판정:
usedAmount * 100 >= limitAmount * 80
```

## 1.4 설정

| 설정                           | 적용 알림              |
| ------------------------------ | ---------------------- |
| `performance_shortage_enabled` | 실적 부족              |
| `benefit_limit_enabled`        | 혜택 월 한도 임박·소진 |

설정 행이 없는 회원은 스키마 기본값과 같이 두 알림 모두 활성화된 것으로 처리한다.

---

# 2. 실적 부족 알림

## 2.1 평가 대상

`performance_tier`에 실제로 `period_type = 'MONTH'` 행이 있는 카드만 평가한다. `{MONTH, QUARTER}`를 코드에서 고정 순회하지 않는다.

다음 대상은 제외한다.

- 비활성 회원
- 비활성 보유카드
- MONTH 구간표가 없는 카드
- MONTH 구간표에 양수 실적 구간이 없는 카드
- `performance_shortage_enabled = false`인 회원

## 2.2 기본 목표

MONTH 구간 중 첫 번째 양수 구간을 기본 실적 목표로 사용한다.

```text
targetPerformance = MIN(min_performance_amount)
WHERE card_id = 대상 카드
  AND period_type = 'MONTH'
  AND min_performance_amount > 0
```

구간이 `0원, 30만원, 60만원, 100만원`이면 목표는 30만원이다. 30만원을 달성한 뒤에는 상위 구간을 위한 실적 부족 알림을 보내지 않는다.

## 2.3 현재 실적

해당 월 `user_card_monthly_state.current_performance_amount`를 사용한다. 기준월 상태 행이 없으면 현재 실적은 0원으로 간주한다.

```text
remainingPerformance = MAX(targetPerformance - currentPerformance, 0)
achievementRate      = currentPerformance * 100 / targetPerformance
```

## 2.4 트리거

KST 기준 매일 1회 판단한다.

| 규칙 | 조건                                                 |
| ---- | ---------------------------------------------------- |
| D-7  | 월 말일 7일 전 AND `achievementRate < 80.0`          |
| D-3  | 월 말일 3일 전 AND `80.0 <= achievementRate < 100.0` |

D-7에는 별도의 60% 하한을 두지 않는다. D-3은 달성에 가까운 사용자에게 막판 안내를 제공한다.

## 2.5 정렬

```text
1순위: achievementRate 내림차순
2순위: targetPerformance 내림차순
3순위: userCardId 오름차순
```

## 2.6 중복 키

```text
PERF_SHORTAGE:{userCardId}:MONTH:{yearMonth}:D{7|3}
```

---

# 3. 혜택 월 한도 임박·소진

## 3.1 평가 단위

| 한도      | 식별 단위                     | 한도값                                   | 사용량                  |
| --------- | ----------------------------- | ---------------------------------------- | ----------------------- |
| 개별 혜택 | `userCardId + benefitId`      | 유효 개별 월 한도                        | 혜택 `used_amount`      |
| 그룹 한도 | `userCardId + limitGroupCode` | 그룹 유효 월 한도                        | 그룹 `used_amount` 합계 |
| 카드 통합 | `userCardId`                  | 현재 MONTH 구간의 `shared_monthly_limit` | `shared_limit_used`     |

## 3.2 유효 개별 월 한도

```text
effectiveMonthlyLimit =
  tier_monthly_limit이 있으면 tier_monthly_limit
  아니면 benefit.monthly_limit
```

현재 적용 구간은 전월 실적으로 판정한다. 당월 진행 실적을 사용하지 않는다.

전월 실적은 알림 배치가 별도 계산하지 않고 기존 `CardStateAssembler.resolvePrevPerformanceAmount`와 동일한 규칙으로 해석한다.

```text
기준월 상태 행이 있으면
  → 해당 행의 prev_performance_amount

기준월 상태 행이 없고 직전월 상태 행이 있으면
  → 직전월 행의 current_performance_amount

둘 다 없으면
  → 0
```

기준월 상태 행을 INNER JOIN하여 행이 없는 카드를 배치 대상에서 제외하지 않는다. 조회 시점마다 소비내역을 다시 합산하지도 않는다.

유효 한도가 `NULL`이면 월 한도가 없다는 뜻이고, `0`이면 이번 달에 사용할 수 있는 혜택이 없다는 뜻이므로 둘 다 제외한다.

## 3.3 선행 필터

- 회원과 보유카드가 활성 상태다.
- `benefit.is_active = 'Y'`다.
- `GIFT`, `INSTALLMENT_FREE`, `RETROACTIVE`가 아니다.
- `require_performance = 'N'`이면 `performance_period`와 관계없이 실적 필터를 통과한다.
- `require_performance = 'Y' AND performance_period = 'MONTH'`이면 MONTH 실적 조건을 충족했다.
- `require_performance = 'Y' AND performance_period = 'QUARTER'`이면 1차 대상에서 제외한다.
- 선택형 혜택이면 해당 월에 사용자가 선택한 option이다.
- `benefit_limit_enabled = true`다.
- 유효 월 한도가 0보다 크다.

## 3.4 그룹 한도

`limit_group_code`가 있는 혜택은 개별 혜택별로 알리지 않고 그룹 단위로 한 번 평가한다.

```text
groupUsedAmount = 같은 userCardId와 limitGroupCode에 속한
                  활성·적용 가능 혜택 used_amount 합계
```

같은 그룹 구성원의 유효 월 한도는 모두 같아야 한다. 다르면 임의의 값을 선택하지 않고 데이터 오류로 기록한 뒤 해당 그룹 알림을 건너뛴다.

그룹 구성원 중 `require_performance = 'Y' AND performance_period = 'QUARTER'`인 혜택이 있으면 일부 구성원만 빼서 사용량을 계산하지 않고 해당 그룹 전체를 1차 대상에서 제외한다.

## 3.5 카드 통합 한도

현재 MONTH 실적 구간의 `shared_monthly_limit`이 0보다 큰 카드만 평가한다.

```text
limitAmount = shared_monthly_limit
usedAmount  = shared_limit_used
```

## 3.6 상태

```text
EXHAUSTED:
  usedAmount >= limitAmount

NEAR:
  usedAmount < limitAmount
  AND usedAmount * 100 >= limitAmount * 80
```

- 개별 알림은 같은 월에 동일한 NEAR 또는 EXHAUSTED key로 최대 한 번 생성한다.
- NEAR 이후 소진되면 EXHAUSTED를 추가 발송할 수 있다.
- NEAR 개별 후보를 만들기 전에 같은 한도와 연월의 EXHAUSTED 개별 dedup key가 `notification`에 있으면 NEAR를 생성하지 않는다.
- EXHAUSTED가 다이제스트에만 포함된 뒤 취소로 사용량이 NEAR 구간으로 내려온 경우에는 구성 이력을 확인할 수 없으므로 NEAR가 생성될 수 있다. 이 역방향 알림은 별도 전달 이력 테이블을 두지 않는 1차 단순화의 허용사항이다.
- 개별 알림으로 처리된 상태는 취소 후 다시 임계값을 넘더라도 같은 월에 재발송하지 않는다.

## 3.7 정렬

```text
1순위: EXHAUSTED, NEAR
2순위: 사용률 내림차순
3순위: limitAmount 내림차순
4순위: deduplicationKey 오름차순
```

## 3.8 중복 키

```text
개별:
BENEFIT_LIMIT:B:{userCardId}:{benefitId}:{yearMonth}:{NEAR|EXHAUSTED}

그룹:
BENEFIT_LIMIT:G:{userCardId}:{limitGroupCode}:{yearMonth}:{NEAR|EXHAUSTED}

카드 통합:
BENEFIT_LIMIT:S:{userCardId}:{yearMonth}:{NEAR|EXHAUSTED}
```

---

# 4. 다이제스트

실적 부족과 혜택 월 한도 후보는 서로 섞지 않는다.

```text
후보 수 <= 3
  → 후보별 개별 알림

후보 수 > 3
  → 개별 알림 대신 다이제스트 1건
  → 상위 3개 + “외 N개” 표시
```

다이제스트는 다음처럼 저장한다.

```text
notification.user_card_id = NULL
notification.benefit_id = NULL
```

중복 키:

```text
PERF_SHORTAGE_DIGEST:MONTH:{yearMonth}:D{7|3}
BENEFIT_LIMIT_DIGEST:{yearMonth}:{candidateSetHash}
```

`candidateSetHash`는 다음처럼 계산한다.

```text
1. 다이제스트에 포함할 후보의 dedup key를 문자열 오름차순으로 정렬
2. 정렬한 키를 LF 줄바꿈 문자(\n)로 연결
3. 연결한 문자열을 UTF-8 바이트로 변환
4. SHA-256 계산
5. 64자리 소문자 hexadecimal 문자열 사용
```

동일 후보 집합은 매일 같은 해시를 만들기 때문에 MySQL unique 제약이 반복 다이제스트를 막는다. 후보가 추가되거나 상태가 바뀌어 집합이 달라지면 새 다이제스트가 생성될 수 있고, 이때 이전 후보가 새 다이제스트에 다시 포함될 수 있다.

따라서 1차에서 보장하는 범위는 다음과 같다.

```text
개별 알림
  → 동일 후보 key를 월 1회로 제한

다이제스트
  → 동일한 후보 집합을 월 1회로 제한
  → 후보별 월 1회까지는 보장하지 않음
```

---

# 5. 배치

| 배치         | 주기       |
| ------------ | ---------- |
| 실적 부족    | KST 일 1회 |
| 혜택 월 한도 | KST 일 1회 |

```text
배치 시작
→ Redis 분산 락 획득
→ MySQL에서 현재 후보 일괄 조회·계산
→ 회원별 그룹핑과 정렬
→ 개별/다이제스트 결정
→ MySQL notification INSERT
→ COMMIT
→ Redis 안 읽은 수 증가 또는 캐시 무효화
→ Redis 분산 락 해제
```

회원·카드별 N+1 쿼리를 만들지 않고 관련 데이터를 일괄 조회한다.

---

# 6. Redis 역할

1차에서는 Redis를 다음 두 가지 용도로만 사용한다.

## 6.1 일 배치 분산 락

```text
key = noti:lock:{jobName}:{yyyy-MM-dd}
value = instanceId

SET key value NX EX lockTtlSeconds
```

- 획득 성공 시 배치를 실행한다.
- 실패하면 다른 인스턴스가 실행 중인 것으로 보고 종료한다.
- TTL은 정상 최대 배치 시간보다 길게 설정한다.
- 해제는 저장된 값이 자신의 `instanceId`와 같을 때만 Lua script로 수행한다.
- Redis 락이 만료되어 중복 실행되더라도 MySQL unique가 최종 중복을 막는다.

## 6.2 안 읽은 알림 수 캐시

```text
noti:unread:{memberId}
```

조회:

```text
Redis hit  → 캐시값 반환
Redis miss → MySQL COUNT → TTL과 함께 Redis 저장 → 반환
```

MySQL 기준:

```sql
SELECT COUNT(*)
FROM notification
WHERE member_id = ?
  AND read_at IS NULL
  AND deleted_at IS NULL;
```

변경:

```text
알림 INSERT COMMIT
→ 실제 생성 건수만큼 INCRBY

읽음·삭제 UPDATE COMMIT
→ 실제 안 읽음 감소 건수만큼 DECRBY
```

- Redis 갱신 실패 때문에 MySQL 트랜잭션을 롤백하지 않는다.
- 갱신 여부가 불확실하면 캐시 키를 삭제한다.
- 값이 음수가 되면 키를 삭제하고 다음 조회에서 MySQL로 복구한다.
- Redis 조회 장애 시 MySQL COUNT로 응답한다.

---

# 7. 1차 운영값

| 항목        | 값                             |
| ----------- | ------------------------------ |
| 시간대      | KST                            |
| 채널        | 앱 내 알림                     |
| 실적 기간   | MONTH                          |
| 실적 목표   | 첫 번째 양수 구간              |
| 실적 트리거 | D-7 80% 미만, D-3 80~100% 미만 |
| 혜택 한도   | 월간 금액 한도                 |
| 한도 단위   | 개별, 그룹, 카드 통합          |
| 임박        | 80% 이상 100% 미만             |
| 소진        | 100% 이상                      |
| 배치        | 유형별 KST 일 1회              |
| 다이제스트  | 후보 3건 초과                  |
| 중복 방지   | MySQL unique                   |
| Redis 1     | 일 배치 분산 락                |
| Redis 2     | 안 읽은 알림 수 캐시           |

---

# 8. 후속 범위

- QUARTER 실적 부족 알림
- 일·분기·연간 금액 한도
- 횟수 한도
- 사용자별 임계값
- 푸시와 이메일
- 실제 부하 측정 후 Redis 후보 큐 또는 outbox 검토
