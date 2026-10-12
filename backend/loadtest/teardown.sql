-- setup.sql 이 넣은 부하 측정 데이터를 지운다. 표시는 이메일 도메인 @loadtest.local.
--
-- 로그인이 refresh_token 행을 남기므로 그것도 지운다. 외래키 순서대로 자식부터 지우며,
-- 측정 중 다른 경로(알림 배치 등)가 행을 만들었을 수 있어 member·user_card 를 참조하는
-- 테이블은 전부 훑는다. 마지막 SELECT 가 모두 0이어야 깨끗하게 지워진 것이다.

CREATE TEMPORARY TABLE loadtest_member AS
SELECT member_id FROM member WHERE email LIKE '%@loadtest.local';

CREATE TEMPORARY TABLE loadtest_user_card AS
SELECT user_card_id FROM user_card WHERE member_id IN (SELECT member_id FROM loadtest_member);

DELETE FROM user_benefit_usage
 WHERE user_card_id IN (SELECT user_card_id FROM loadtest_user_card);
DELETE FROM user_card_benefit_selection
 WHERE user_card_id IN (SELECT user_card_id FROM loadtest_user_card);
DELETE FROM user_card_monthly_state
 WHERE user_card_id IN (SELECT user_card_id FROM loadtest_user_card);
DELETE FROM notification
 WHERE member_id IN (SELECT member_id FROM loadtest_member);
DELETE FROM notification_setting
 WHERE member_id IN (SELECT member_id FROM loadtest_member);
DELETE FROM refresh_token
 WHERE member_id IN (SELECT member_id FROM loadtest_member);
DELETE FROM user_card
 WHERE user_card_id IN (SELECT user_card_id FROM loadtest_user_card);
DELETE FROM member
 WHERE member_id IN (SELECT member_id FROM loadtest_member);

SELECT (SELECT COUNT(*) FROM member WHERE email LIKE '%@loadtest.local') AS members,
       (SELECT COUNT(*) FROM user_card
         WHERE user_card_id IN (SELECT user_card_id FROM loadtest_user_card)) AS user_cards,
       (SELECT COUNT(*) FROM refresh_token
         WHERE member_id IN (SELECT member_id FROM loadtest_member)) AS refresh_tokens;

DROP TEMPORARY TABLE loadtest_user_card;
DROP TEMPORARY TABLE loadtest_member;
