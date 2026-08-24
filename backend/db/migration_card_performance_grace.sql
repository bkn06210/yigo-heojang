-- PR #63 이 schema.sql 에 추가한 card_performance_grace 테이블.
--
-- 지금은 <b>아무 코드도 이 테이블을 읽지 않는다</b> — 매퍼·서비스 어디에도 참조가 없다.
-- 뒤에 올 실적 유예 계산 PR 이 쓸 자리를 스키마에 먼저 잡아둔 것이다.
-- 그래서 이 파일은 돌리지 않아도 지금 기능은 전부 동작한다.
--
-- 돌리는 것은 schema.sql 과 실제 DB 를 맞춰두고 싶을 때다. 빈 테이블 하나가 늘 뿐
-- 기존 데이터는 건드리지 않는다.
--
--   mysql -u root -p wallet_team_pr33_demo < backend/db/migration_card_performance_grace.sql
--
-- 채울 데이터(어느 카드가 어느 구간으로 유예되는가)는 91_seed_card_benefit.sql 이 갖고 있다.
-- 시드를 현행(카드 15장)으로 유지하는 동안에는 이 테이블이 비어 있는 것이 정상이다.

CREATE TABLE IF NOT EXISTS card_performance_grace (
    card_id       BIGINT      NOT NULL COMMENT '카드 ID',
    period_type   VARCHAR(10) NOT NULL COMMENT '실적 축: MONTH | QUARTER (performance_tier.period_type과 짝)',
    tier_id       BIGINT      NOT NULL COMMENT '유예 기간에 적용할 실적 구간',
    -- 조사한 카드는 전부 1이다("등록월 + 다음달 말일까지", "발급월+1개월까지").
    -- 그래도 컬럼으로 두는 것은 60일·3개월로 적은 카드가 있어 값이 하나라고 단정할 수 없기 때문이다.
    grace_periods TINYINT     NOT NULL COMMENT '사용등록 기간 이후 몇 기간까지 유예되는가 (MONTH면 개월, QUARTER면 분기)',
    PRIMARY KEY (card_id, period_type),
    CONSTRAINT fk_card_performance_grace_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    CONSTRAINT fk_card_performance_grace_tier FOREIGN KEY (tier_id) REFERENCES performance_tier (tier_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '발급 초기 실적 유예';
