-- ============================================================
-- migration_representative_multi.sql — 대표(고정) 카드를 3개까지 허용한다
--
-- 증상: 카드 하나를 고정한 뒤 두 번째 카드를 고정하면 500이 떨어졌다.
--   PATCH /api/user-cards/{id}/representative
--   → Duplicate entry '{member_id}' for key 'user_card.uk_user_card_representative'
--
-- 원인: 로컬 DB에 회원당 대표카드를 1개로 강제하는 UNIQUE 인덱스
--   uk_user_card_representative 가 남아 있다. schema.sql 에는 이 인덱스가 없고
--   레포 어디에도 이걸 만드는 구문이 없다 — 예전 스키마나 수동 작업의 잔재다.
--
-- schema.sql 은 반대로 못을 박아 두었다(474줄 주석):
--   "대표 카드는 회원당 최대 3개까지 허용한다.
--    DB에서는 대표 카드 여부만 저장하고, '최대 3개' 규칙은 서비스 트랜잭션에서 검증한다.
--    이유: 일반적인 UNIQUE 제약만으로 '회원당 최대 3개' 같은 개수 제한을 표현하기 어렵기 때문이다."
--
-- 즉 개수 제한의 주인은 UserCardService.MAX_REPRESENTATIVE_CARD_COUNT(=3) 이고,
-- 이 인덱스는 그 규칙과 정면으로 충돌한다. 한도를 넘기면 409로 안내해야 하는데
-- 인덱스가 먼저 걸려 500이 나가므로, 인덱스를 걷어낸다.
--
-- 실행 (Workbench: 대상 스키마를 먼저 지정하거나 아래 USE 주석을 푼다)
-- USE wallet_team_pr33_demo;
--
-- 여러 번 실행해도 안전하다. 인덱스가 없으면 건너뛴다.
-- ============================================================

SET NAMES utf8mb4;

-- MySQL 은 DROP INDEX 에 IF EXISTS 를 지원하지 않아 information_schema 로 갈라준다.
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_card'
      AND index_name = 'uk_user_card_representative'
);

SET @drop_sql = IF(
    @index_exists > 0,
    'ALTER TABLE user_card DROP INDEX uk_user_card_representative',
    'DO 0'
);

PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

-- 확인 — 0 이어야 한다.
SELECT COUNT(*) AS remaining_unique_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'user_card'
  AND index_name = 'uk_user_card_representative';
