-- ============================================================
-- 02_performance.sql — 실적 2개 (performance_tier, performance_exclusion)
-- ------------------------------------------------------------
-- 선행: 01_master.sql (card, category)
-- ============================================================

-- ------------------------------------------------------------
-- performance_tier : 실적구간 → 통합할인한도
--
-- 카드 약관의 "전월 30만원 이상 이용 시 월 1만원 한도"를 데이터로 표현한다.
-- if문의 조건(전월실적 임계값)이 컬럼, 결과(한도)가 값, 분기가 행이다.
--
-- 엔진의 실적 판정 규칙:
--   min_performance_amount <= 전월실적  인 행 중에서
--   min_performance_amount 가 가장 큰 행 하나를 고른다.
--   예) 구간 0 / 20만 / 50만, 전월 25만 → 20만 구간
--
-- 규칙 — 모든 카드는 min_performance_amount = 0 인 행을 반드시 1개 갖는다.
--   실적 조건이 없는 카드(대부분의 체크카드)도 0원 구간 1행으로 표현한다.
--   그래야 위 판정이 항상 행 하나를 반환하고, 엔진에 "구간 없음" NULL 분기가
--   생기지 않는다. 실적 미달 시 혜택이 아예 없는 카드라면 0원 구간의
--   shared_monthly_limit 을 0으로 넣으면 된다.
-- ------------------------------------------------------------
CREATE TABLE performance_tier (
    tier_id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '실적구간 ID',
    card_id                BIGINT        NOT NULL COMMENT '카드 ID',
    min_performance_amount BIGINT        NOT NULL COMMENT '이 구간의 최소 전월실적(원). 조건 없으면 0',
    shared_monthly_limit   BIGINT        NULL COMMENT '통합할인한도(월, 원). NULL = 통합한도 없음(혜택별 개별한도만 적용), 0 = 혜택 없음',
    PRIMARY KEY (tier_id),
    UNIQUE KEY uk_performance_tier (card_id, min_performance_amount),
    CONSTRAINT fk_performance_tier_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 통합할인한도';

-- ------------------------------------------------------------
-- performance_exclusion : 전월실적 산정에서 빼는 항목
--
-- 카드사가 전월실적 값을 주지 않으므로 엔진이 직접 계산한다.
--   전월실적 = 지난달 소비내역 합계 − 여기 걸린 제외 항목
--
-- 제외 축이 3종이라 (유형, 값) 2컬럼으로 받는다. 값은 유형에 따라 해석이 달라지는
-- 다형성 값이므로 VARCHAR 로 통일한다. (FK 는 걸 수 없다 — 아래 트레이드오프 참조)
--
--   exclusion_type      exclusion_value 예시        의미
--   ─────────────────   ─────────────────────────   ──────────────────────────
--   CATEGORY            'PUBLIC_TRANSPORT'          해당 카테고리 결제는 실적 제외
--   PAYMENT_TYPE        'SIMPLE_PAY'                해당 결제수단 결제는 실적 제외
--   TRANSACTION_ATTR    'INTEREST_FREE'             무이자할부 건은 실적 제외
--                       'DISCOUNTED'                이미 할인받은 건은 실적 제외
--                       'OVERSEAS'                  해외 이용분은 실적 제외
--   MIN_TXN_AMOUNT      '10000'                     건당 1만원 미만 결제는 실적 제외
--
-- MIN_TXN_AMOUNT는 값이 코드가 아니라 금액 임계값이라 성격이 조금 다르다. 그래도
-- exclusion_value가 이미 다형성 VARCHAR라 스키마 변경 없이 해석 규칙만 추가하면 된다.
-- ("1건 1만원 미만은 실적 제외"는 실제 카드 약관에 매우 흔하다)
-- MERCHANT 유형은 지금 없다 — 조사한 약관에 사례가 없어서다. 필요하면 값만 늘리면 된다.
--
-- 값에 FK 를 못 거는 게 이 설계의 비용이다. 없는 코드를 넣어도 DB 가 막지 못한다.
-- 그래서 id 대신 코드 문자열(category_code)을 쓴다 — 어차피 무결성 이점이 없으니
-- 시드 가독성을 택하는 게 낫다('301'보다 'PUBLIC_TRANSPORT'). 대분류 코드를 넣으면
-- 하위 전체가 제외되는 확장성도 그대로다.
-- (컬럼을 분리해 FK 를 걸면 무결성은 얻지만 NULL 컬럼 3개 + 배타 CHECK 가 붙는다.)
-- ------------------------------------------------------------
CREATE TABLE performance_exclusion (
    card_id         BIGINT      NOT NULL COMMENT '카드 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | PAYMENT_TYPE | TRANSACTION_ATTR | MIN_TXN_AMOUNT',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값. 유형에 따라 해석이 달라진다 (CATEGORY면 category_code, MIN_TXN_AMOUNT면 금액 문자열)',
    PRIMARY KEY (card_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_performance_exclusion_card FOREIGN KEY (card_id) REFERENCES card (card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '전월실적 제외 항목';
