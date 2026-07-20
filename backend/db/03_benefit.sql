-- ============================================================
-- 03_benefit.sql — 혜택 3개 (benefit, benefit_tier_limit, benefit_exclusion)
-- ------------------------------------------------------------
-- 선행: 01_master.sql (card, category, merchant), 02_performance.sql (performance_tier)
--
-- 이 파일이 엔진의 심장이다. 카드 약관의 "무엇을, 얼마나, 어떤 조건에서, 어디까지"를
-- 전부 컬럼으로 편다. 새 카드가 추가되면 여기에 행만 늘어나고 코드는 바뀌지 않는다.
--
-- ★ 엔진 규칙 — 한 결제에 적용되는 혜택은 카드당 1개다.
--   가맹점 혜택과 카테고리 혜택을 둘 다 조회하므로 한 결제에 여러 혜택이 매칭될 수 있다.
--   (예: 스타벅스 10% 할인 + 카페 5% 적립)
--   이때 합산하지 않고 계산 결과가 가장 큰 혜택 1개만 적용한다. 동점이면 benefit_id 오름차순.
--   근거: ①실제 약관은 "타 할인과 중복 불가"가 기본이다 ②소비내역의 applied_benefit_id가
--   단수라 이미 이 전제 위에 있다 ③계단식 정액할인(3만↑3천/5만↑5천)도 자동으로 맞게 동작한다.
--   중복 적용이 명시된 카드는 범위 밖(필요해지면 benefit.allow_stacking + applied_benefit_id 복수화).
-- ============================================================

-- ------------------------------------------------------------
-- benefit : 혜택 규칙 1건 = 1행
--
-- 컬럼을 4개 묶음으로 읽으면 된다.
--   [무엇을] target_type / target_category_id / target_merchant_id
--   [얼마나] calc_method / benefit_value  (RATE=율, FIXED=정액)
--   [조건]   require_performance / require_payment_type / min_txn_amount
--   [상한]   max_eligible_amount / max_benefit_per_txn / monthly_limit
--            / monthly_count_limit / daily_count_limit / use_shared_limit
--
-- 대상(target)을 다형성 FK 하나로 두지 않고 컬럼 2개로 분리했다.
-- 그래야 진짜 FK를 걸어 무결성을 DB가 보장한다. 대신 "정확히 하나만 채운다"는
-- 규칙이 필요해서 아래 CHECK 제약으로 막는다.
--
-- 상한 컬럼이 전부 NULL 허용인 이유: 약관에 없는 제약과 0인 제약은 다르다.
--   monthly_limit = NULL → 월 한도 없음
--   monthly_limit = 0    → 이번 달 혜택 없음
-- 엔진에서 NULL을 0으로 뭉개면 혜택이 통째로 사라진다. (performance_tier와 동일한 함정)
-- ------------------------------------------------------------
CREATE TABLE benefit (
    benefit_id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '혜택 ID',
    card_id              BIGINT        NOT NULL COMMENT '카드 ID',
    benefit_name         VARCHAR(100)  NOT NULL COMMENT '혜택명 (예: 카페 10% 청구할인)',

    -- 혜택 성격
    -- benefit_kind 하나로 통합했다. 원래 benefit_kind(DISCOUNT/SPECIAL_PRICE/GIFT/RETROACTIVE)와
    -- benefit_method(DISCOUNT/POINT)로 나눠 뒀는데, DISCOUNT가 양쪽에 있어 적립 혜택의
    -- kind가 무엇인지 정의되지 않았다. kind=DISCOUNT + method=POINT는
    -- "종류는 할인인데 지급은 적립"이라는 모순이고, API가 benefitKind를 그대로
    -- 내려보내므로 화면에 적립이 "DISCOUNT"로 표시된다. api-engine.md도 이미 두 값을
    -- 한 열거형처럼 섞어 쓰고 있었다. → 5값 단일 열거형으로 정리.
    benefit_kind         VARCHAR(20)   NOT NULL COMMENT '혜택 종류: DISCOUNT(할인) | POINT(적립) | SPECIAL_PRICE(특가) | GIFT(증정) | RETROACTIVE(사후정산, 계산 제외)',
    calc_method          VARCHAR(10)   NOT NULL COMMENT '계산 방식: RATE(정률) | FIXED(정액)',
    benefit_value        DECIMAL(10,2) NOT NULL COMMENT 'RATE면 퍼센트(10.00 = 10%), FIXED면 금액(원, 소수부 미사용)',
    apply_timing         VARCHAR(20)   NULL COMMENT '할인 시점: IMMEDIATE(즉시) | BILLED(청구). benefit_kind=DISCOUNT일 때만 값을 갖는다',

    -- 무엇을 겨냥하는가
    target_type          VARCHAR(20)   NOT NULL COMMENT '대상 유형: CATEGORY | MERCHANT | ALL(전 가맹점)',
    target_category_id   BIGINT        NULL COMMENT 'target_type=CATEGORY일 때만. 대분류를 넣으면 하위 중분류까지 적용',
    target_merchant_id   BIGINT        NULL COMMENT 'target_type=MERCHANT일 때만',

    -- 적용 조건
    --
    -- require_performance='Y'의 "충족" 판정 기준을 여기서 못 박는다.
    --   모든 카드가 0원 구간 행을 갖게 만든 결과, 실적 판정은 항상 어떤 구간을 반환한다.
    --   따라서 "구간을 못 찾음 = 미충족"이 성립하지 않는다.
    --   → 판정된 구간의 min_performance_amount > 0 이면 충족, 0 구간이면 미충족.
    --   이 기준이 없으면 구현자가 추측으로 짜게 되고, 그때마다 결과가 달라진다.
    require_performance  CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '전월실적 조건 필요 여부: Y | N. 충족 기준 = 판정된 구간의 min_performance_amount > 0',
    -- 요일·시간대·채널 같은 다른 "거는 조건"이 필요해지면
    -- benefit_condition(benefit_id, condition_type, condition_value) 테이블로 확장하고
    -- 이 컬럼을 그리로 흡수한다. (benefit_exclusion과 대칭 구조)
    -- 지금 안 만드는 이유: 조사한 카드 대부분이 결제수단 조건만 갖고, 타입별 파서와
    -- AND/OR 표현 규칙이 엔진에 붙는 비용이 크다. 컬럼 → 테이블 전환은 데이터 손실이 없다.
    require_payment_type VARCHAR(30)   NULL COMMENT '특정 결제수단에서만 적용 (예: SIMPLE_PAY). NULL이면 수단 무관',
    min_txn_amount       BIGINT        NULL COMMENT '건당 최소 결제금액(원). 미만이면 혜택 없음',

    -- 상한
    max_eligible_amount  BIGINT        NULL COMMENT '혜택 대상 금액 상한(원). 이 금액까지만 율을 곱한다',
    max_benefit_per_txn  BIGINT        NULL COMMENT '건당 최대 혜택액(원)',
    monthly_limit        BIGINT        NULL COMMENT '이 혜택의 월 개별 한도(원). NULL=한도 없음',
    -- "통신·공과금·마트 각 10%, 합쳐서 월 5천원" 같은 묶음 한도용.
    -- 대상이 3개라 혜택 3행으로 쪼개야 하는데, 그러면 한도가 5천원씩 3번 = 15,000원으로 샌다.
    -- 에러 없이 금액만 틀리는 유형이라 같은 카드 안에서 코드로 묶어 합산 판정한다.
    -- NULL이면 이 혜택 단독으로 monthly_limit을 쓴다(대부분의 경우).
    limit_group_code     VARCHAR(30)   NULL COMMENT '묶음 한도 코드. 같은 카드 내 같은 코드끼리 monthly_limit을 공유한다',
    monthly_count_limit  INT           NULL COMMENT '월 최대 적용 횟수',
    daily_count_limit    INT           NULL COMMENT '일 최대 적용 횟수',
    use_shared_limit     CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '카드 통합할인한도를 함께 소진하는가: Y | N',

    -- 계산에 절대 쓰지 않는다. 스키마로 표현하지 못한 약관 단서를 원문 그대로 남기는 칸이다.
    -- (예: "앱 결제분 제외" — 상품·채널 단위라 범위 밖이지만, 안 적어두면 정보가 사라진다)
    -- 추천 근거 문구는 여기서 읽지 않고 benefit_name + 계산값으로 엔진이 조립한다.
    -- 저장된 문장을 그대로 쓰면 한도가 소진돼도 문구가 그대로라 틀린 안내가 된다.
    description          VARCHAR(500)  NULL COMMENT '스키마로 표현 못 한 약관 단서 원문. 계산 사용 금지 — 표시·챗봇 컨텍스트용',
    is_active            CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '유효 여부: Y | N. 종료된 혜택은 N',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (benefit_id),
    KEY idx_benefit_card (card_id),
    KEY idx_benefit_target_category (target_category_id),
    KEY idx_benefit_target_merchant (target_merchant_id),
    KEY idx_benefit_limit_group (card_id, limit_group_code),

    CONSTRAINT fk_benefit_card     FOREIGN KEY (card_id)            REFERENCES card (card_id),
    CONSTRAINT fk_benefit_category FOREIGN KEY (target_category_id) REFERENCES category (category_id),
    CONSTRAINT fk_benefit_merchant FOREIGN KEY (target_merchant_id) REFERENCES merchant (merchant_id),

    -- 대상 컬럼은 target_type에 맞는 것 정확히 하나만 채운다
    CONSTRAINT ck_benefit_target CHECK (
        (target_type = 'CATEGORY' AND target_category_id IS NOT NULL AND target_merchant_id IS NULL)
     OR (target_type = 'MERCHANT' AND target_merchant_id IS NOT NULL AND target_category_id IS NULL)
     OR (target_type = 'ALL'      AND target_category_id IS NULL     AND target_merchant_id IS NULL)
    ),

    -- 할인 시점은 할인 혜택에만 있다. 적립·특가·증정·사후정산은 NULL이어야 한다.
    -- (예전 benefit_method 컬럼과 값이 동치라 한쪽만 잘못 넣으면 모순이 생겼던 자리)
    CONSTRAINT ck_benefit_apply_timing CHECK (
        (benefit_kind = 'DISCOUNT' AND apply_timing IS NOT NULL)
     OR (benefit_kind <> 'DISCOUNT' AND apply_timing IS NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 규칙';

-- ------------------------------------------------------------
-- benefit_tier_limit : 실적구간별로 달라지는 한도·혜택값
--
-- 약관 예) "전월 30만원 이상 1%, 60만원 이상 2%, 한도도 각각 5천원/1만원"
--   → benefit 1행 + 이 테이블 2행. 혜택을 구간 수만큼 쪼개지 않는다.
--
-- NULL의 의미: 그 구간에서는 benefit의 기본값을 그대로 쓴다.
--   한도만 구간별로 다르면 tier_monthly_limit만 채우고 tier_benefit_value는 NULL,
--   율까지 다르면 둘 다 채운다.
-- ------------------------------------------------------------
CREATE TABLE benefit_tier_limit (
    benefit_id         BIGINT        NOT NULL COMMENT '혜택 ID',
    tier_id            BIGINT        NOT NULL COMMENT '실적구간 ID',
    tier_monthly_limit BIGINT        NULL COMMENT '이 구간에서의 월 개별 한도(원). NULL이면 benefit.monthly_limit 사용',
    tier_benefit_value DECIMAL(10,2) NULL COMMENT '이 구간에서의 혜택값(율 또는 정액). NULL이면 benefit.benefit_value 사용',
    PRIMARY KEY (benefit_id, tier_id),
    KEY idx_benefit_tier_limit_tier (tier_id),
    CONSTRAINT fk_benefit_tier_limit_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id),
    CONSTRAINT fk_benefit_tier_limit_tier    FOREIGN KEY (tier_id)    REFERENCES performance_tier (tier_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '실적구간별 개별한도·혜택값';

-- ------------------------------------------------------------
-- benefit_exclusion : 이 혜택에서 빼는 대상
--
-- 약관 예) "외식 5% (단, 배달앱 제외)"
--   → benefit은 대분류 외식을 겨냥하고, 여기에 CATEGORY 'DELIVERY'(배달앱) 한 행.
--   계층 구조 덕분에 "대분류로 넓게 걸고 예외만 뺀다"가 자연스럽게 된다.
--
-- performance_exclusion과 동일한 (유형, 값) 패턴이다. 두 테이블을 같은 방식으로
-- 읽고 쓸 수 있도록 PK도 복합 PK로 통일했다. (CLAUDE.md 담당표의 exclusion_id
-- 서로게이트 안은 폐기 — 문서 쪽을 이 DDL에 맞춰 수정한다.)
-- ------------------------------------------------------------
CREATE TABLE benefit_exclusion (
    benefit_id      BIGINT      NOT NULL COMMENT '혜택 ID',
    exclusion_type  VARCHAR(20) NOT NULL COMMENT '제외 유형: CATEGORY | MERCHANT | PAYMENT_TYPE | TRANSACTION_ATTR',
    exclusion_value VARCHAR(50) NOT NULL COMMENT '제외 값. 유형에 따라 해석이 달라진다 (CATEGORY면 category_code, MERCHANT면 merchant_code)',
    PRIMARY KEY (benefit_id, exclusion_type, exclusion_value),
    CONSTRAINT fk_benefit_exclusion_benefit FOREIGN KEY (benefit_id) REFERENCES benefit (benefit_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '혜택 적용 예외';
