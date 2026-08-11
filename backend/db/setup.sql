-- ============================================================
-- setup.sql — DB 전체를 한 번에 세팅한다.
--
-- 아래 파일들을 순서대로 실행하는 것과 같다. 순서를 지켜야 하는 이유는
-- 뒤 파일이 앞 파일이 만든 행을 참조하기 때문이다.
--
--   schema.sql → data.sql → 91 → 92 → 94
--
-- 실행 (backend/db 폴더에서):
--   mysql -u root -p <DB이름> < setup.sql
--
-- DB 이름을 명령줄에서 받으므로 사람마다 다른 이름을 써도 된다.
-- 빈 DB 는 미리 만들어 두어야 한다. 이 파일은 DB 를 만들지 않는다:
--   CREATE DATABASE wallet DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--
-- SOURCE 는 상대 경로를 현재 작업 폴더 기준으로 찾는다.
-- 반드시 backend/db 안에서 실행해야 한다.
--
-- ⚠ schema.sql 이 기존 테이블을 전부 DROP 한다. 남겨야 할 데이터가 있으면 실행하지 마라.
--   이미 세팅된 DB 에 최신 변경만 반영하려면 바뀐 9x_ 파일 하나만 실행하면 된다.
-- ============================================================

SOURCE schema.sql;
SOURCE data.sql;
SOURCE 91_seed_card_benefit.sql;
SOURCE 92_seed_user_data.sql;
SOURCE 94_seed_membership_usage_place.sql;

SELECT CONCAT(
    '세팅 완료 — 카드 ', (SELECT COUNT(*) FROM card),
    '개 / 소비내역 ', (SELECT COUNT(*) FROM expense),
    '건 / 멤버십 사용처 ', (SELECT COUNT(*) FROM point_usage_place),
    '행'
) AS result;

