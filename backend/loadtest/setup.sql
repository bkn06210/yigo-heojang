-- 부하 측정 전용 회원·보유카드·월 상태.
--
-- 보유 카드 수만 다른 회원 셋(2·10·20장)을 만든다. 추천 API의 비용이 보유 카드 수에 따라
-- 어떻게 늘어나는지 보려는 것이라, 카드 구성은 포함 관계로 둔다(2장 ⊂ 10장 ⊂ 20장).
-- 카드 수 외의 조건이 섞이지 않게 하기 위해서다.
--
-- 이메일 도메인 @loadtest.local 이 표시다. teardown.sql 이 같은 표시로 지운다.
-- 시드 데이터는 읽기만 하고 고치지 않는다.
--
-- 월은 CURDATE() 기준으로 계산한다. 추천은 요청 시점의 달을 기준월로 쓰므로,
-- 고정 연월을 적으면 달이 바뀌는 순간 상태 행이 조회되지 않는다.
--
-- 비밀번호: LoadTest!2026 (BCrypt)

SET @base_month = DATE_FORMAT(CURDATE(), '%Y-%m');
SET @prev_month = DATE_FORMAT(CURDATE() - INTERVAL 1 MONTH, '%Y-%m');
SET @password_hash = '$2a$10$V6GvmShSEhEfHdxiPSmsL.qddb5/VXDsDuV6BE/b4TBbb1psg02uu';

INSERT INTO member (email, password_hash, name, nickname, member_status)
VALUES ('lt-c02@loadtest.local', @password_hash, '부하측정', 'lt-c02', 'ACTIVE'),
       ('lt-c10@loadtest.local', @password_hash, '부하측정', 'lt-c10', 'ACTIVE'),
       ('lt-c20@loadtest.local', @password_hash, '부하측정', 'lt-c20', 'ACTIVE');

-- 카드 id 1~39 중 홀수 20장. 카드사·혜택 수가 고루 섞이도록 연속 구간 대신 건너뛰며 고른다
INSERT INTO user_card (member_id, card_id, masked_card_number)
SELECT m.member_id, c.card_id, '0000-****-****-0000'
  FROM member m
  JOIN card c
    ON (m.email = 'lt-c02@loadtest.local' AND c.card_id IN (1, 3))
    OR (m.email = 'lt-c10@loadtest.local' AND c.card_id IN (1, 3, 5, 7, 9, 11, 13, 15, 17, 19))
    OR (m.email = 'lt-c20@loadtest.local' AND c.card_id IN (1, 3, 5, 7, 9, 11, 13, 15, 17, 19,
                                                           21, 23, 25, 27, 29, 31, 33, 35, 37, 39));

-- 직전월 누적 실적. 카드마다 달라야 여러 실적 구간(과 구간별 개별한도)이 판정된다
INSERT INTO user_card_monthly_state (user_card_id, base_year_month, prev_performance_amount,
                                     current_performance_amount, shared_limit_used)
SELECT uc.user_card_id, @prev_month, 0,
       CASE MOD(uc.card_id DIV 2, 4) WHEN 0 THEN 0 WHEN 1 THEN 300000 WHEN 2 THEN 500000 ELSE 1000000 END,
       0
  FROM user_card uc
  JOIN member m ON m.member_id = uc.member_id
 WHERE m.email LIKE '%@loadtest.local';

-- 기준월 행은 카드 절반에만 둔다. 나머지는 직전월 누적으로 전월실적을 읽는 경로를 탄다
INSERT INTO user_card_monthly_state (user_card_id, base_year_month, prev_performance_amount,
                                     current_performance_amount, shared_limit_used)
SELECT uc.user_card_id, @base_month,
       CASE MOD(uc.card_id DIV 2, 4) WHEN 0 THEN 0 WHEN 1 THEN 300000 WHEN 2 THEN 500000 ELSE 1000000 END,
       120000, 2000
  FROM user_card uc
  JOIN member m ON m.member_id = uc.member_id
 WHERE m.email LIKE '%@loadtest.local'
   AND MOD(uc.card_id DIV 2, 2) = 0;

-- 카드마다 계산 대상 혜택 두 개에 이번 달 소진을 남긴다. 한도가 일부 닳은 상태를 만들기 위해서다
INSERT INTO user_benefit_usage (user_card_id, benefit_id, base_year_month, used_amount, used_count,
                                last_applied_date, daily_used_count, daily_used_amount)
SELECT ranked.user_card_id, ranked.benefit_id, @base_month, 1500, 1, CURDATE(), 1, 1500
  FROM (SELECT uc.user_card_id, b.benefit_id,
               ROW_NUMBER() OVER (PARTITION BY uc.user_card_id ORDER BY b.benefit_id) AS rn
          FROM user_card uc
          JOIN member m ON m.member_id = uc.member_id
          JOIN benefit b ON b.card_id = uc.card_id
         WHERE m.email LIKE '%@loadtest.local'
           AND b.is_active = 'Y'
           AND b.benefit_kind NOT IN ('GIFT', 'INSTALLMENT_FREE', 'RETROACTIVE')) ranked
 WHERE ranked.rn <= 2;

-- 선택형 혜택이 있는 카드는 묶음마다 첫 선택지를 고른 것으로 둔다
INSERT INTO user_card_benefit_selection (user_card_id, base_year_month, option_group_code,
                                         selected_option_key)
SELECT uc.user_card_id, @base_month, b.option_group_code, MIN(b.option_key)
  FROM user_card uc
  JOIN member m ON m.member_id = uc.member_id
  JOIN benefit b ON b.card_id = uc.card_id
 WHERE m.email LIKE '%@loadtest.local'
   AND b.is_active = 'Y'
   AND b.option_group_code IS NOT NULL
   AND b.option_key IS NOT NULL
 GROUP BY uc.user_card_id, b.option_group_code;

SELECT m.email, m.member_id, COUNT(uc.user_card_id) AS cards
  FROM member m
  JOIN user_card uc ON uc.member_id = m.member_id
 WHERE m.email LIKE '%@loadtest.local'
 GROUP BY m.email, m.member_id
 ORDER BY m.email;
