-- ============================================================
-- 05_state.sql — 상태 2개 (user_card_monthly_state, user_benefit_usage)
-- ------------------------------------------------------------
-- 선행: 03_benefit.sql (benefit), 04_external_stub.sql (user_card)
--
-- 앞의 8개 테이블이 "카드가 이렇게 생겼다"는 정적 데이터라면,
-- 이 둘은 "이 사용자가 이번 달 얼마 썼고 얼마 소진했다"는 움직이는 값이다.
-- 결제할 때마다 갱신되고, 그 갱신이 다음 추천을 바꾼다. (= 동적 전환)
--
-- 저장 원칙 — 원본만 저장하고 파생값은 저장하지 않는다.
--   저장 O : 전월실적, 당월누적, 한도사용액, 적용횟수   (합산 비용이 크거나 시점 의존)
--   저장 X : 달성률, 잔여한도, 이용률, 이용상태, 적용구간 (조회 시 계산)
--   파생값을 저장하면 원본과 어긋나는 순간이 반드시 온다. 계산은 싸다.
-- ============================================================

-- ------------------------------------------------------------
-- user_card_monthly_state : 보유카드 1장의 월별 실적·소진 상태
--
-- PK가 (user_card_id, base_year_month) 복합키인 이유:
--   엔진 조회가 전부 "이 카드의 이번 달 상태" 형태라 이 조합이 곧 인덱스가 된다.
--   서로게이트 id를 두면 실제로 안 쓰는 컬럼과 인덱스가 하나씩 늘 뿐이다.
--   이 테이블을 FK로 참조하는 테이블이 없어서 복합키의 단점도 발생하지 않는다.
--
-- prev_performance_amount를 저장하는 이유 (파생값 아닌가?):
--   지난달 소비내역 전체를 실적제외 규칙까지 적용해 합산해야 나오는 값이다.
--   추천 API가 호출될 때마다 이걸 다시 계산하면 비용이 크다.
--   또 월이 넘어가면 확정되어 변하지 않는 값이라, 집계 스냅샷으로 저장하는 게 정당하다.
--   (반면 달성률은 이 값 나누기 한 번이라 저장할 이유가 없다)
--
--   ★ 단, 이 값은 전월 행의 current_performance_amount와 같은 값이다. 즉 복사본이다.
--     원본은 "전월 행의 current_performance_amount"이고 이 컬럼은 그것을 당겨온 캐시다.
--     전월 행이 없을 때(가입 첫 달, 마이데이터 과거분 적재)만 소비내역 합산으로 채운다.
--     둘이 어긋나도 DB는 막지 못하므로, 전월 거래를 수정·취소하는 기능이 생기면
--     반드시 다음 달 행의 이 값도 같이 고쳐야 한다. (현재는 당월 취소만 지원하므로 미발생)
--
-- 저장하지 않는 것 — 적용 실적구간(tier):
--   prev_performance_amount만 있으면 performance_tier 조회로 항상 도출된다.
--   저장해두면 구간 데이터를 고쳤을 때 옛 판정이 남아 어긋난다.
-- ------------------------------------------------------------
CREATE TABLE user_card_monthly_state (
    user_card_id               BIGINT   NOT NULL COMMENT '보유카드 ID',
    base_year_month            CHAR(7)  NOT NULL COMMENT '기준 연월 (YYYY-MM)',
    prev_performance_amount    BIGINT   NOT NULL DEFAULT 0 COMMENT '전월실적(원). 실적제외 규칙 적용 후 합산액',
    current_performance_amount BIGINT   NOT NULL DEFAULT 0 COMMENT '당월 누적 실적인정액(원). 다음 달 전월실적이 된다',
    shared_limit_used          BIGINT   NOT NULL DEFAULT 0 COMMENT '통합할인한도 사용액(원)',
    updated_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (user_card_id, base_year_month),
    CONSTRAINT fk_user_card_monthly_state_user_card FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '보유카드 월별 실적·소진 상태';

-- ------------------------------------------------------------
-- user_benefit_usage : 혜택 1건의 월별 소진·적용횟수
--
-- 카드 통합한도(위 테이블)와 별개로, 혜택마다 개별 한도·횟수가 따로 돈다.
--   "카페 10% (월 5천원 한도, 월 3회)" → 이 테이블 1행이 그 5천원과 3회를 센다.
--
-- last_applied_date와 daily_used_count가 같이 있는 이유:
--   daily_count_limit(일 N회) 판정에 "오늘 몇 번 썼나"가 필요한데, 날짜별 행을
--   따로 쌓으면 테이블이 커진다. 대신 마지막 적용일과 그날의 횟수만 들고 있다가,
--   결제일이 last_applied_date와 다르면 daily_used_count를 0으로 리셋하고 센다.
--   일 단위 이력이 필요하면 소비내역(consumption)에 이미 남으므로 손실이 없다.
-- ------------------------------------------------------------
CREATE TABLE user_benefit_usage (
    user_card_id      BIGINT        NOT NULL COMMENT '보유카드 ID',
    benefit_id        BIGINT        NOT NULL COMMENT '혜택 ID',
    base_year_month   CHAR(7)       NOT NULL COMMENT '기준 연월 (YYYY-MM)',
    used_amount       BIGINT        NOT NULL DEFAULT 0 COMMENT '이 혜택으로 받은 누적 혜택액(원)',
    used_count        INT           NOT NULL DEFAULT 0 COMMENT '당월 누적 적용 횟수',
    last_applied_date DATE          NULL COMMENT '마지막 적용 일자. 일 단위 횟수 리셋 판정용',
    daily_used_count  INT           NOT NULL DEFAULT 0 COMMENT 'last_applied_date 당일의 적용 횟수',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (user_card_id, benefit_id, base_year_month),
    KEY idx_user_benefit_usage_benefit (benefit_id),
    CONSTRAINT fk_user_benefit_usage_user_card FOREIGN KEY (user_card_id) REFERENCES user_card (user_card_id),
    CONSTRAINT fk_user_benefit_usage_benefit   FOREIGN KEY (benefit_id)   REFERENCES benefit (benefit_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택별 월 소진·적용횟수';
