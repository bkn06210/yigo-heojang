-- ============================================================
-- 95_seed_notification.sql — member_id=1 알림 화면 시연 데이터
--
-- 실행 순서: schema.sql → data.sql → 91 → 92 → 93 → 94 → 이 파일
--
-- 알림 목록에서 아래 네 가지 형태를 모두 확인하기 위한 시연 fixture다.
--   1) 실적 부족 개별 알림
--   2) 실적 부족 다이제스트
--   3) 혜택 한도 개별 알림
--   4) 혜택 한도 다이제스트
--
-- 2026년 8월 D-7은 8월 24일이다. 해당 알림은 삭제하지 않는 한
-- 8월 25~26일 시연에서도 그대로 조회된다.
--
-- 실적 부족 수치는 91의 카드별 최소 실적구간과 92의 8월 current_performance_amount로
-- 계산했다. 혜택 한도 수치는 아래에 추가하는 전월실적/혜택사용 상태를
-- 실제 BenefitLimitCandidateCalculator 규칙(소진 >= 100%, 임박 >= 80%)으로 계산했다.
--
-- notification.user_card_id와 benefit_id가 92/91 시드 데이터를 참조하므로
-- 반드시 두 파일 이후에 실행한다. INSERT IGNORE와 deduplication_key 덕분에
-- 이 파일을 여러 번 실행해도 같은 알림이 중복 생성되지 않는다.
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- ── 혜택 한도 알림 계산용 상태 ──────────────────────────────
--
-- 92 시드의 당월 실적(current_performance_amount)은 건드리지 않는다.
-- 따라서 아래 전월실적 보정 이후에도 실적 부족 D-7 수치와 후보 구성은 그대로다.
-- user_card_id=7은 92에 8월 상태 행이 없으므로 당월실적 0원으로 새로 만든다.
INSERT INTO user_card_monthly_state (
    user_card_id,
    base_year_month,
    prev_performance_amount,
    current_performance_amount,
    shared_limit_used
) VALUES
    (3, '2026-08', 400000, 25200, 0),
    (4, '2026-08', 500000, 39000, 0),
    (7, '2026-08', 300000, 0, 0)
ON DUPLICATE KEY UPDATE
    prev_performance_amount = VALUES(prev_performance_amount);

-- 8월 22일에는 KB Pay 개별 한도만 소진되어 개별 알림이 만들어지고,
-- 8월 24일에 나머지 세 묶음이 80%를 넘어서 총 4건짜리 다이제스트가 만들어진다.
--
--   benefit 26                : 5,000 / 5,000 = 100.0% (EXHAUSTED)
--   SIMPLE_PAY                : 27,900 / 30,000 = 93.0% (NEAR)
--   LIFESTYLE_SHOPPING_5000   : 4,400 / 5,000 = 88.0% (NEAR)
--   LIFESTYLE_COFFEE_10000    : 8,200 / 10,000 = 82.0% (NEAR)
INSERT INTO user_benefit_usage (
    user_card_id,
    benefit_id,
    base_year_month,
    used_amount,
    used_count,
    last_applied_date,
    daily_used_count,
    daily_used_amount
) VALUES
    (3, 26,  '2026-08', 5000,  2, '2026-08-22', 1, 2500),
    (4, 53,  '2026-08', 10000, 4, '2026-08-24', 1, 2500),
    (4, 54,  '2026-08', 9000,  3, '2026-08-24', 1, 3000),
    (4, 55,  '2026-08', 8900,  3, '2026-08-24', 1, 2900),
    (7, 225, '2026-08', 2000,  2, '2026-08-24', 1, 1000),
    (7, 226, '2026-08', 1400,  2, '2026-08-24', 1, 700),
    (7, 227, '2026-08', 1000,  1, '2026-08-24', 1, 1000),
    (7, 234, '2026-08', 8200,  4, '2026-08-24', 1, 2050)
ON DUPLICATE KEY UPDATE
    used_amount = VALUES(used_amount),
    used_count = VALUES(used_count),
    last_applied_date = VALUES(last_applied_date),
    daily_used_count = VALUES(daily_used_count),
    daily_used_amount = VALUES(daily_used_amount);

INSERT IGNORE INTO notification (
    member_id,
    user_card_id,
    benefit_id,
    point_history_id,
    notification_type,
    title,
    content,
    notification_status,
    scheduled_at,
    sent_at,
    created_at,
    read_at,
    deleted_at,
    deduplication_key
) VALUES
    -- 6월 D-3에는 80% 이상 100% 미만 카드가 user_card_id=1 하나뿐이어서
    -- 개별 알림이 된다: (353,300 / 400,000) × 100 = 88.3%, 잔여 46,700원.
    (
        1,
        1,
        NULL,
        NULL,
        'PERFORMANCE_SHORTAGE',
        '신한카드 핏(Fit)의 이번 달 실적 목표까지 46,700원 남았어요. (현재 달성률 88.3%)',
        '신한카드 핏(Fit)으로 조금 더 사용하면 이번 달 목표를 채울 수 있어요.',
        'SENT',
        NULL,
        '2026-06-27 09:00:00',
        '2026-06-27 09:00:00',
        '2026-06-28 12:30:00',
        NULL,
        'PERF_SHORTAGE:1:MONTH:2026-06:D3'
    ),

    -- 개별 혜택 한도 소진 알림. user_card_id=3은 member_id=1의 YOU Wish 카드,
    -- benefit_id=26은 해당 카드의 KB Pay 10% 청구할인 혜택이다.
    (
        1,
        3,
        26,
        NULL,
        'BENEFIT_LIMIT',
        'YOU Wish 카드 위시 베이직 - KB Pay 10% 청구할인 한도(5,000원)를 모두 사용했어요',
        '해당 혜택 사용을 멈추고 다른 카드로 소비를 옮겨보세요.',
        'SENT',
        NULL,
        '2026-08-22 09:05:00',
        '2026-08-22 09:05:00',
        NULL,
        NULL,
        'BENEFIT_LIMIT:B:3:26:2026-08:EXHAUSTED'
    ),

    -- 8월 D-7(8월 24일) 실적 부족 후보 6건을 묶은 다이제스트.
    -- 다이제스트는 특정 카드/혜택 하나에 귀속되지 않으므로 FK 컬럼을 비워 둔다.
    (
        1,
        NULL,
        NULL,
        NULL,
        'PERFORMANCE_SHORTAGE_DIGEST',
        'ALL point 카드 외 5건, 실적이 부족해요',
        CONCAT_WS(CHAR(10),
            'ALL point 카드: 목표까지 263,400원 (달성률 12.2%)',
            '신한카드 Deep Once: 목표까지 365,200원 (달성률 8.7%)',
            '삼성 iD ON 카드: 목표까지 274,000원 (달성률 8.7%)',
            '마이핏카드(적립형): 목표까지 461,000원 (달성률 7.8%)',
            'YOU Wish 카드: 목표까지 374,800원 (달성률 6.3%)',
            '삼성카드 taptap O: 목표까지 300,000원 (달성률 0.0%)'
        ),
        'SENT',
        NULL,
        '2026-08-24 09:00:00',
        '2026-08-24 09:00:00',
        NULL,
        NULL,
        'PERF_SHORTAGE_DIGEST:MONTH:2026-08:D7'
    ),

    -- 혜택 한도 후보 4건을 묶은 다이제스트. 실제 NotificationComposer와 같이
    -- 후보 dedup key 정렬 결과의 SHA-256을 다이제스트 키에 사용했다.
    (
        1,
        NULL,
        NULL,
        NULL,
        'BENEFIT_LIMIT_DIGEST',
        'YOU Wish 카드 위시 베이직 - KB Pay 10% 청구할인 한도 외 3건, 확인이 필요해요',
        CONCAT_WS(CHAR(10),
            'YOU Wish 카드 위시 베이직 - KB Pay 10% 청구할인: 소진 (100.0%)',
            '마이핏카드(적립형) 묶음(SIMPLE_PAY): 임박 (93.0%)',
            '삼성카드 taptap O 묶음(LIFESTYLE_SHOPPING_5000): 임박 (88.0%)',
            '삼성카드 taptap O 묶음(LIFESTYLE_COFFEE_10000): 임박 (82.0%)'
        ),
        'SENT',
        NULL,
        '2026-08-24 09:05:00',
        '2026-08-24 09:05:00',
        NULL,
        NULL,
        'BENEFIT_LIMIT_DIGEST:2026-08:efe673019330a138c11f691aabf33eff6d9ef5d68ec16c89d9008257791a95f3'
    );

COMMIT;
