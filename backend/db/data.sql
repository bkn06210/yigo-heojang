-- ============================================================
-- data.sql — 카드 혜택 최적화 전자지갑 개발/테스트용 시드 데이터
-- 대상 스키마: schema.sql (MySQL 8.0)
--
-- 실행 순서:
--   1) schema.sql
--   2) data.sql
--
-- 주의:
--   · 개발/테스트 환경 전용 데이터다.
--   · ID를 명시적으로 고정해 FK 관계와 테스트 결과를 예측 가능하게 했다.
--   · 비밀번호와 토큰 값은 실제 원문이 아닌 테스트용 해시 문자열이다.
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- ============================================================
-- 1. 회원 · 인증
-- ============================================================

INSERT INTO member (
    member_id, email, password_hash, name, member_status,
    created_at, updated_at, withdrawn_at
) VALUES
    (1, 'active@example.com',
     '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiLKYWN3JZ9u7WZm0tJ4f0W0J0A0A0A',
     '김활성', 'ACTIVE',
     '2026-06-01 09:00:00', '2026-07-20 10:00:00', NULL),

    (2, 'suspended@example.com',
     '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiLKYWN3JZ9u7WZm0tJ4f0W0J0A0A0A',
     '이정지', 'SUSPENDED',
     '2026-06-05 09:00:00', '2026-07-18 15:00:00', NULL),

    (3, 'withdrawn@example.com',
     '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiLKYWN3JZ9u7WZm0tJ4f0W0J0A0A0A',
     '박탈퇴', 'WITHDRAWN',
     '2026-05-01 09:00:00', '2026-07-10 14:00:00', '2026-07-10 14:00:00');

INSERT INTO member_withdrawal (
    member_withdrawal_id, member_id, reason_type, reason_detail,
    withdrawn_at, created_at
) VALUES
    (1, 3, 'LOW_USAGE', '서비스를 자주 사용하지 않아서 탈퇴합니다.',
     '2026-07-10 14:00:00', '2026-07-10 14:00:00');

-- PENDING: 인증번호 발급 직후
INSERT INTO password_reset_verification (
    password_reset_verification_id, member_id,
    verification_code_hash, verification_status, failed_attempt_count,
    verification_code_expires_at,
    reset_token_hash, reset_token_expires_at,
    verified_at, used_at, created_at, updated_at
) VALUES
    (1, 1,
     'sha256:test-verification-code-123456',
     'PENDING', 0,
     '2026-07-21 23:59:59',
     NULL, NULL,
     NULL, NULL,
     '2026-07-21 21:00:00', '2026-07-21 21:00:00'),

    (2, 2,
     'sha256:test-verification-code-expired',
     'EXPIRED', 3,
     '2026-07-18 09:05:00',
     NULL, NULL,
     NULL, NULL,
     '2026-07-18 09:00:00', '2026-07-18 09:10:00');

INSERT INTO refresh_token (
    refresh_token_id, member_id, token_hash,
    expires_at, revoked_at, revoke_reason, created_at
) VALUES
    (1, 1, 'sha256:active-member-device-1',
     '2026-08-04 09:00:00', NULL, NULL, '2026-07-21 09:00:00'),

    (2, 1, 'sha256:active-member-logged-out-device',
     '2026-08-01 12:00:00', '2026-07-20 12:30:00', 'LOGOUT',
     '2026-07-18 12:00:00'),

    (3, 3, 'sha256:withdrawn-member-token',
     '2026-07-24 10:00:00', '2026-07-10 14:00:00', 'MEMBER_WITHDRAWN',
     '2026-07-10 10:00:00');

-- ============================================================
-- 2. 서비스 약관
-- ============================================================

INSERT INTO term (
    term_id, term_code, term_name, is_required, term_status
) VALUES
    (1, 'SERVICE_TERMS', '서비스 이용약관', 1, 'ACTIVE'),
    (2, 'PRIVACY_POLICY', '개인정보 수집 및 이용 동의', 1, 'ACTIVE'),
    (3, 'MARKETING_CONSENT', '마케팅 정보 수신 동의', 0, 'ACTIVE'),
    (4, 'LOCATION_TERMS', '위치정보 이용약관', 0, 'INACTIVE');

INSERT INTO term_version (
    term_version_id, term_id, version, content,
    effective_started_at, effective_ended_at
) VALUES
    (1, 1, '2026-06-01',
     '제1조 목적: 본 약관은 카드 혜택 최적화 전자지갑 서비스 이용 조건을 규정합니다.',
     '2026-06-01 00:00:00', NULL),

    (2, 2, '2026-06-01',
     '수집 항목: 이메일, 이름, 보유 카드 및 서비스 이용 기록. 수집 목적: 회원 관리와 혜택 분석.',
     '2026-06-01 00:00:00', NULL),

    (3, 3, '2026-06-01',
     '혜택과 이벤트 정보 수신에 동의합니다. 선택 동의이며 동의하지 않아도 서비스 이용이 가능합니다.',
     '2026-06-01 00:00:00', NULL),

    (4, 4, '2026-05-01',
     '위치 기반 서비스 제공을 위한 약관입니다.',
     '2026-05-01 00:00:00', '2026-06-30 23:59:59');

INSERT INTO member_term_agreement (
    member_term_agreement_id, member_id, term_version_id,
    is_agreed, agreed_at
) VALUES
    (1, 1, 1, 1, '2026-06-01 09:00:00'),
    (2, 1, 2, 1, '2026-06-01 09:00:00'),
    (3, 1, 3, 1, '2026-06-01 09:00:00'),
    (4, 2, 1, 1, '2026-06-05 09:00:00'),
    (5, 2, 2, 1, '2026-06-05 09:00:00'),
    (6, 2, 3, 0, NULL);

-- ============================================================
-- 3. 카드 마스터
-- ============================================================

INSERT INTO card (
    card_id, card_name, issuer, card_type, annual_fee,
    image_url, description, is_active, created_at, updated_at
) VALUES
    (1, 'KB 국민 나라사랑카드', 'KB국민카드', 'CHECK', 0,
     'https://example.com/images/cards/kb-narasarang.png',
     '교통·편의점·외식 생활 혜택 중심 체크카드', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (2, '삼성 iD ON 카드', '삼성카드', 'CREDIT', 20000,
     'https://example.com/images/cards/samsung-id-on.png',
     '카페·교통·통신 자동 맞춤 할인 카드', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (3, '신한 Deep Dream 카드', '신한카드', 'CREDIT', 8000,
     'https://example.com/images/cards/shinhan-deep-dream.png',
     '전 가맹점 기본 적립과 생활 영역 추가 적립 카드', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (4, '판매 종료 테스트 카드', '테스트카드사', 'CREDIT', 10000,
     NULL, '판매 종료 카드 조회 테스트용', 'N',
     '2026-05-01 00:00:00', '2026-06-30 00:00:00');

-- ============================================================
-- 4. 카테고리 표준: 대분류 6 + 중분류 24
-- ============================================================

-- 대분류
INSERT INTO category (
    category_id, category_code, category_name,
    parent_category_id, display_order
) VALUES
    (1, 'DINING', '외식', NULL, 1),
    (2, 'SHOPPING', '쇼핑', NULL, 2),
    (3, 'TRANSPORT', '교통', NULL, 3),
    (4, 'LIVING', '생활', NULL, 4),
    (5, 'CULTURE_LEISURE', '문화여가', NULL, 5),
    (6, 'MEDICAL', '의료', NULL, 6);

-- 외식
INSERT INTO category VALUES
    (101, 'RESTAURANT', '음식점', 1, 1),
    (102, 'CAFE', '카페', 1, 2),
    (103, 'DELIVERY', '배달앱', 1, 3),
    (104, 'FAST_FOOD', '패스트푸드', 1, 4);

-- 쇼핑
INSERT INTO category VALUES
    (201, 'CONVENIENCE_STORE', '편의점', 2, 1),
    (202, 'LARGE_MART', '대형마트', 2, 2),
    (203, 'DEPARTMENT_STORE', '백화점', 2, 3),
    (204, 'ONLINE_SHOPPING', '온라인쇼핑', 2, 4),
    (205, 'BEAUTY', '뷰티', 2, 5),
    (206, 'BOOKSTORE', '서점', 2, 6);

-- 교통
INSERT INTO category VALUES
    (301, 'PUBLIC_TRANSPORT', '대중교통', 3, 1),
    (302, 'TAXI', '택시', 3, 2),
    (303, 'FUEL', '주유', 3, 3),
    (304, 'PARKING_MAINTENANCE', '주차정비', 3, 4);

-- 생활
INSERT INTO category VALUES
    (401, 'TELECOM', '이동통신', 4, 1),
    (402, 'UTILITY', '공과금', 4, 2),
    (403, 'APARTMENT_FEE', '아파트관리비', 4, 3),
    (404, 'INSURANCE', '보험료', 4, 4),
    (405, 'LIFE_SERVICE', '세탁생활서비스', 4, 5);

-- 문화여가
INSERT INTO category VALUES
    (501, 'MOVIE', '영화', 5, 1),
    (502, 'SUBSCRIPTION_STREAMING', '구독스트리밍', 5, 2),
    (503, 'SPORTS_LEISURE', '스포츠레저', 5, 3);

-- 의료
INSERT INTO category VALUES
    (601, 'HOSPITAL', '병원', 6, 1),
    (602, 'PHARMACY', '약국', 6, 2);

-- ============================================================
-- 5. 가맹점
-- ============================================================

INSERT INTO merchant (
    merchant_id, merchant_code, merchant_name, category_id
) VALUES
    (1, 'STARBUCKS', '스타벅스', 102),
    (2, 'TWOSOME', '투썸플레이스', 102),
    (3, 'HOLLYS', '할리스', 102),
    (4, 'GS25', 'GS25', 201),
    (5, 'CU', 'CU', 201),
    (6, 'EMART24', '이마트24', 201),
    (7, 'COUPANG', '쿠팡', 204),
    (8, 'NAVER_SHOPPING', '네이버쇼핑', 204),
    (9, 'CGV', 'CGV', 501),
    (10, 'OLIVE_YOUNG', '올리브영', 205),
    (11, 'MCDONALDS', '맥도날드', 104),
    (12, 'KYobo_BOOK', '교보문고', 206);

-- ============================================================
-- 6. 실적 구간 및 제외 조건
-- ============================================================

INSERT INTO performance_tier (
    tier_id, card_id, min_performance_amount, shared_monthly_limit
) VALUES
    (1, 1,      0,     0),
    (2, 1, 200000, 10000),
    (3, 1, 300000, 20000),

    (4, 2,      0,     0),
    (5, 2, 300000, 30000),
    (6, 2, 600000, 50000),

    (7, 3,      0, NULL),
    (8, 3, 300000, NULL),
    (9, 3, 600000, NULL),

    (10, 4,     0,     0);

INSERT INTO performance_exclusion (
    card_id, exclusion_type, exclusion_value
) VALUES
    (1, 'TRANSACTION_ATTR', 'INTEREST_FREE'),
    (1, 'TRANSACTION_ATTR', 'DISCOUNTED'),
    (2, 'CATEGORY', 'UTILITY'),
    (2, 'TRANSACTION_ATTR', 'INTEREST_FREE'),
    (3, 'MIN_TXN_AMOUNT', '1000');

-- ============================================================
-- 7. 혜택 규칙
-- ============================================================

INSERT INTO benefit (
    benefit_id, card_id, benefit_name,
    benefit_kind, calc_method, benefit_value, apply_timing,
    target_type, target_category_id, target_merchant_id,
    require_performance, require_payment_type, min_txn_amount,
    max_eligible_amount, max_benefit_per_txn, monthly_limit,
    limit_group_code, monthly_count_limit, daily_count_limit,
    use_shared_limit, description, is_active, created_at, updated_at
) VALUES
    -- KB 나라사랑카드
    (1, 1, '대중교통 20% 청구할인',
     'DISCOUNT', 'RATE', 20.00, 'BILLED',
     'CATEGORY', 301, NULL,
     'Y', NULL, 1000,
     NULL, 5000, 10000,
     NULL, NULL, NULL,
     'Y', '버스·지하철 이용금액 대상', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (2, 1, '편의점 5% 즉시할인',
     'DISCOUNT', 'RATE', 5.00, 'IMMEDIATE',
     'CATEGORY', 201, NULL,
     'Y', NULL, 5000,
     NULL, 1000, 5000,
     NULL, 5, 1,
     'Y', '일 1회, 월 5회', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (3, 1, '스타벅스 10% 즉시할인',
     'DISCOUNT', 'RATE', 10.00, 'IMMEDIATE',
     'MERCHANT', NULL, 1,
     'Y', NULL, 5000,
     NULL, 3000, 10000,
     NULL, 5, 1,
     'Y', '스타벅스 매장 결제 대상', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    -- 삼성 iD ON
    (4, 2, '카페 30% 청구할인',
     'DISCOUNT', 'RATE', 30.00, 'BILLED',
     'CATEGORY', 102, NULL,
     'Y', NULL, 10000,
     30000, 10000, 10000,
     'IDON_DAILY', NULL, 1,
     'Y', '카페 영역 월 통합 할인', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (5, 2, '대중교통 10% 청구할인',
     'DISCOUNT', 'RATE', 10.00, 'BILLED',
     'CATEGORY', 301, NULL,
     'Y', NULL, NULL,
     NULL, 5000, 10000,
     'IDON_DAILY', NULL, NULL,
     'Y', '버스·지하철 대상', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (6, 2, '이동통신 10% 청구할인',
     'DISCOUNT', 'RATE', 10.00, 'BILLED',
     'CATEGORY', 401, NULL,
     'Y', NULL, 30000,
     NULL, 10000, 10000,
     'IDON_DAILY', 1, NULL,
     'Y', '이동통신 자동납부 대상', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    -- 신한 Deep Dream
    (7, 3, '전 가맹점 0.7% 포인트 적립',
     'POINT', 'RATE', 0.70, NULL,
     'ALL', NULL, NULL,
     'N', NULL, NULL,
     NULL, NULL, NULL,
     NULL, NULL, NULL,
     'N', '국내외 전 가맹점 기본 적립', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (8, 3, '편의점 3.5% 포인트 적립',
     'POINT', 'RATE', 3.50, NULL,
     'CATEGORY', 201, NULL,
     'Y', NULL, 1000,
     NULL, NULL, 20000,
     NULL, NULL, NULL,
     'N', '편의점 추가 적립', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00'),

    (9, 3, '온라인쇼핑 3.5% 포인트 적립',
     'POINT', 'RATE', 3.50, NULL,
     'CATEGORY', 204, NULL,
     'Y', 'SIMPLE_PAY', 10000,
     NULL, NULL, 20000,
     NULL, NULL, NULL,
     'N', '간편결제 사용 시 적립', 'Y',
     '2026-06-01 00:00:00', '2026-06-01 00:00:00');

INSERT INTO benefit_tier_limit (
    benefit_id, tier_id, tier_monthly_limit, tier_benefit_value
) VALUES
    (4, 5, 10000, 30.00),
    (4, 6, 20000, 30.00),
    (8, 8, 10000, 3.50),
    (8, 9, 20000, 5.00),
    (9, 8, 10000, 3.50),
    (9, 9, 20000, 5.00);

INSERT INTO benefit_exclusion (
    benefit_id, exclusion_type, exclusion_value
) VALUES
    (4, 'MERCHANT', 'HOLLYS'),
    (7, 'TRANSACTION_ATTR', 'OVERSEAS'),
    (8, 'PAYMENT_TYPE', 'GIFT_CARD');

-- ============================================================
-- 8. 보유 카드
-- ============================================================

INSERT INTO user_card (
    user_card_id, member_id, card_id, masked_card_number,
    is_representative, registered_at, card_status
) VALUES
    (1, 1, 1, '***-****-****-5678', 1, '2026-06-01 10:00:00', 'ACTIVE'),
    (2, 1, 2, '****-****-****-8765', 0, '2026-06-02 11:00:00', 'ACTIVE'),
    (3, 1, 3, '****-****-****-1111', 0, '2026-06-03 12:00:00', 'ACTIVE'),
    (4, 2, 2, '****-****-****-2222', 1, '2026-06-05 10:00:00', 'ACTIVE'),
    (5, 3, 1, '****-****-****-3333', 0, '2026-05-02 10:00:00', 'DELETED');

-- ============================================================
-- 9. 소비 내역
-- ============================================================

INSERT INTO expense (
    expense_id, member_id, user_card_id, category_id,
    merchant_id, merchant_name, amount, payment_date, input_type,
    applied_benefit_id, discount_amount, payment_type,
    is_interest_free, created_at
) VALUES
    -- 6월: 7월 실적의 근거가 되는 데이터
    (1, 1, 1, 301, NULL, '서울교통공사', 55000,
     '2026-06-05 08:10:00', 'MANUAL', 1, 5000, 'CARD', 'N',
     '2026-06-05 08:15:00'),

    (2, 1, 1, 201, 4, 'GS25', 20000,
     '2026-06-07 18:30:00', 'MANUAL', 2, 1000, 'CARD', 'N',
     '2026-06-07 18:31:00'),

    (3, 1, 1, 102, 1, '스타벅스', 15000,
     '2026-06-10 13:00:00', 'PAYMENT', 3, 1500, 'CARD', 'N',
     '2026-06-10 13:00:02'),

    (4, 1, 1, 202, NULL, '이마트', 150000,
     '2026-06-15 16:00:00', 'MANUAL', NULL, 0, 'CARD', 'N',
     '2026-06-15 16:05:00'),

    (5, 1, 2, 102, 2, '투썸플레이스', 18000,
     '2026-06-11 14:20:00', 'PAYMENT', 4, 5400, 'CARD', 'N',
     '2026-06-11 14:20:03'),

    (6, 1, 2, 401, NULL, 'KT', 65000,
     '2026-06-20 09:00:00', 'MANUAL', 6, 6500, 'CARD', 'N',
     '2026-06-20 09:01:00'),

    (7, 1, 2, 202, NULL, '홈플러스', 260000,
     '2026-06-22 17:00:00', 'MANUAL', NULL, 0, 'CARD', 'N',
     '2026-06-22 17:05:00'),

    (8, 1, 3, 204, 7, '쿠팡', 120000,
     '2026-06-25 21:00:00', 'PAYMENT', 9, 4200, 'SIMPLE_PAY', 'N',
     '2026-06-25 21:00:03'),

    (9, 1, 3, 201, 5, 'CU', 30000,
     '2026-06-26 12:00:00', 'MANUAL', 8, 1050, 'CARD', 'N',
     '2026-06-26 12:02:00'),

    (10, 1, 3, 101, NULL, '동네식당', 170000,
     '2026-06-28 19:00:00', 'MANUAL', 7, 1190, 'CARD', 'N',
     '2026-06-28 19:05:00'),

    -- 7월: 현재 월 누적 및 한도 소진 테스트
    (11, 1, 1, 301, NULL, '서울교통공사', 45000,
     '2026-07-03 08:05:00', 'MANUAL', 1, 5000, 'CARD', 'N',
     '2026-07-03 08:10:00'),

    (12, 1, 1, 201, 4, 'GS25', 12000,
     '2026-07-06 18:40:00', 'PAYMENT', 2, 600, 'CARD', 'N',
     '2026-07-06 18:40:02'),

    (13, 1, 2, 102, 1, '스타벅스', 22000,
     '2026-07-08 12:30:00', 'PAYMENT', 4, 6600, 'CARD', 'N',
     '2026-07-08 12:30:03'),

    (14, 1, 2, 301, NULL, '서울교통공사', 50000,
     '2026-07-10 08:00:00', 'MANUAL', 5, 5000, 'CARD', 'N',
     '2026-07-10 08:05:00'),

    (15, 1, 3, 204, 8, '네이버쇼핑', 80000,
     '2026-07-12 20:00:00', 'PAYMENT', 9, 2800, 'SIMPLE_PAY', 'N',
     '2026-07-12 20:00:02'),

    (16, 2, 4, 102, 2, '투썸플레이스', 15000,
     '2026-07-05 14:00:00', 'MANUAL', 4, 4500, 'CARD', 'N',
     '2026-07-05 14:01:00');

-- ============================================================
-- 10. 결제 및 추천 입력
-- ============================================================

INSERT INTO payment (
    payment_id, member_id, user_card_id, payment_channel,
    is_recommend_based, expense_id, merchant_id, merchant_name,
    payment_amount, payment_status, requested_at, completed_at
) VALUES
    (1, 1, 1, 'MOCK', 'Y', 3, 1, '스타벅스',
     15000, 'SUCCESS', '2026-06-10 12:59:58', '2026-06-10 13:00:02'),

    (2, 1, 2, 'MOCK', 'Y', 13, 1, '스타벅스',
     22000, 'SUCCESS', '2026-07-08 12:29:58', '2026-07-08 12:30:03'),

    (3, 1, 3, 'MOCK', 'N', NULL, 7, '쿠팡',
     50000, 'FAIL', '2026-07-15 19:00:00', '2026-07-15 19:00:02');

INSERT INTO recommend_input (
    recommend_input_id, member_id, category_id,
    merchant_name, expected_amount, input_at
) VALUES
    (1, 1, 102, '스타벅스', 22000, '2026-07-08 12:29:30'),
    (2, 1, 204, '쿠팡', 50000, '2026-07-15 18:59:30'),
    (3, 1, 201, 'GS25', 10000, '2026-07-20 17:00:00');

-- ============================================================
-- 11. 월별 계산 상태
-- ============================================================

INSERT INTO user_card_monthly_state (
    user_card_id, base_year_month,
    prev_performance_amount, current_performance_amount,
    shared_limit_used, updated_at
) VALUES
    (1, '2026-06',      0, 240000,  7500, '2026-06-30 23:59:00'),
    (1, '2026-07', 240000,  57000,  5600, '2026-07-20 18:00:00'),

    (2, '2026-06',      0, 343000, 11900, '2026-06-30 23:59:00'),
    (2, '2026-07', 343000,  72000, 11600, '2026-07-20 18:00:00'),

    (3, '2026-06',      0, 320000,     0, '2026-06-30 23:59:00'),
    (3, '2026-07', 320000,  80000,     0, '2026-07-20 18:00:00'),

    (4, '2026-07',      0,  15000,  4500, '2026-07-20 18:00:00');

INSERT INTO user_benefit_usage (
    user_card_id, benefit_id, base_year_month,
    used_amount, used_count, last_applied_date,
    daily_used_count, updated_at
) VALUES
    (1, 1, '2026-07', 5000, 1, '2026-07-03', 1, '2026-07-03 08:10:00'),
    (1, 2, '2026-07',  600, 1, '2026-07-06', 1, '2026-07-06 18:40:02'),

    (2, 4, '2026-07', 6600, 1, '2026-07-08', 1, '2026-07-08 12:30:03'),
    (2, 5, '2026-07', 5000, 1, '2026-07-10', 1, '2026-07-10 08:05:00'),

    (3, 9, '2026-07', 2800, 1, '2026-07-12', 1, '2026-07-12 20:00:02'),

    (4, 4, '2026-07', 4500, 1, '2026-07-05', 1, '2026-07-05 14:01:00');

-- ============================================================
-- 12. 포인트
-- ============================================================

INSERT INTO point_provider (
    point_provider_id, point_provider_name, point_provider_type,
    logo_image_url, use_yn, default_recommend_yn,
    recommend_priority, recommend_message
) VALUES
    (1, 'KB 포인트리', 'FINANCIAL_POINT',
     'https://example.com/images/points/kb-pointree.png',
     'Y', 'Y', 1, 'KB 포인트리를 결제에 활용할 수 있습니다.'),

    (2, '마이신한포인트', 'FINANCIAL_POINT',
     'https://example.com/images/points/shinhan-point.png',
     'Y', 'Y', 2, '마이신한포인트 사용 가능 여부를 확인하세요.'),

    (3, 'CJ ONE', 'MEMBERSHIP',
     'https://example.com/images/points/cj-one.png',
     'Y', 'N', NULL, NULL),

    (4, '해피포인트', 'MEMBERSHIP',
     'https://example.com/images/points/happy-point.png',
     'Y', 'N', NULL, NULL);

INSERT INTO point_wallet (
    point_wallet_id, member_id, point_provider_id,
    total_point, updated_at
) VALUES
    (1, 1, 1, 12500, '2026-07-20 18:00:00'),
    (2, 1, 2,  8300, '2026-07-20 18:00:00'),
    (3, 1, 3,  2500, '2026-07-20 18:00:00'),
    (4, 2, 2,  1200, '2026-07-18 10:00:00');

INSERT INTO point_history (
    point_history_id, member_id, point_wallet_id,
    expense_id, point_type, point_amount, content, occurred_at
) VALUES
    (1, 1, 1, NULL, 'SAVE', 10000, '초기 테스트 포인트 적립', '2026-06-01 09:10:00'),
    (2, 1, 1, 12,   'SAVE',   500, 'GS25 이용 포인트 적립', '2026-07-06 18:40:05'),
    (3, 1, 1, NULL, 'SAVE',  3000, '이벤트 포인트 적립', '2026-07-15 09:00:00'),
    (4, 1, 1, NULL, 'USE',   1000, '포인트 사용', '2026-07-18 12:00:00'),

    (5, 1, 2, NULL, 'SAVE',  5500, '초기 테스트 포인트 적립', '2026-06-03 12:10:00'),
    (6, 1, 2, 15,   'SAVE',  2800, '네이버쇼핑 적립', '2026-07-12 20:00:05'),

    (7, 1, 3, NULL, 'SAVE',  2500, '멤버십 포인트 동기화', '2026-07-01 09:00:00'),
    (8, 2, 4, NULL, 'SAVE',  1200, '초기 테스트 포인트 적립', '2026-06-05 10:10:00');

INSERT INTO point_usage_place (
    point_usage_place_id, point_provider_id, place_name,
    category_id, use_yn, description
) VALUES
    (1, 1, 'KB Pay 제휴 가맹점', 204, 'Y', 'KB Pay 결제 시 포인트리 사용 가능'),
    (2, 2, '신한 SOL페이 제휴 가맹점', 204, 'Y', '신한 SOL페이에서 포인트 사용 가능'),
    (3, 3, '올리브영', 205, 'Y', 'CJ ONE 포인트 적립 및 사용 가능'),
    (4, 3, 'CGV', 501, 'Y', 'CJ ONE 포인트 사용 가능'),
    (5, 4, '파리바게뜨', 104, 'Y', '해피포인트 적립 및 사용 가능');

INSERT INTO membership_register (
    membership_register_id, member_id, point_provider_id,
    register_status, registered_at, canceled_at
) VALUES
    (1, 1, 3, 'REGISTERED', '2026-06-10 10:00:00', NULL),
    (2, 1, 4, 'REGISTERED', '2026-06-11 10:00:00', NULL),
    (3, 2, 3, 'CANCELED',   '2026-06-08 10:00:00', '2026-07-01 10:00:00');

-- ============================================================
-- 13. 개인화 설정
-- ============================================================

INSERT INTO member_preferred_category (
    member_preferred_category_id, member_id, category_id, created_at
) VALUES
    (1, 1, 102, '2026-06-01 09:05:00'),
    (2, 1, 201, '2026-06-01 09:05:00'),
    (3, 1, 301, '2026-06-01 09:05:00'),
    (4, 2, 102, '2026-06-05 09:05:00');

INSERT INTO member_preferred_merchant (
    member_preferred_merchant_id, member_id, category_id,
    merchant_id, priority, created_at
) VALUES
    (1, 1, 102, 1, 1, '2026-06-01 09:06:00'),
    (2, 1, 102, 2, 2, '2026-06-01 09:06:00'),
    (3, 1, 102, 3, 3, '2026-06-01 09:06:00'),

    (4, 1, 201, 4, 1, '2026-06-01 09:06:00'),
    (5, 1, 201, 5, 2, '2026-06-01 09:06:00'),
    (6, 1, 201, 6, 3, '2026-06-01 09:06:00'),

    (7, 2, 102, 2, 1, '2026-06-05 09:06:00');

-- ============================================================
-- 14. 알림 및 알림 설정
-- ============================================================

INSERT INTO notification_setting (
    notification_setting_id, member_id,
    performance_shortage_enabled,
    benefit_limit_enabled,
    updated_at
) VALUES
    (1, 1, 1, 1, '2026-07-20 18:00:00'),
    (2, 2, 0, 1, '2026-07-18 10:00:00'),
    (3, 3, 0, 0, '2026-07-10 14:00:00');

INSERT INTO notification (
    notification_id, member_id, user_card_id, benefit_id,
    point_history_id, notification_type, title, content,
    notification_status, scheduled_at, sent_at, created_at,
    read_at, deleted_at, deduplication_key
) VALUES
    (1, 1, 2, NULL, NULL,
     'PERF_SHORT_001',
     '실적 달성까지 27만 8천원 남았어요',
     '삼성 iD ON 카드의 다음 실적 구간까지 278,000원이 남았습니다.',
     'SENT',
     '2026-07-20 09:00:00', '2026-07-20 09:00:02',
     '2026-07-20 08:55:00',
     NULL, NULL,
     'PERF_SHORT_001:USER_CARD:2:2026-07'),

    (2, 1, 2, 4, NULL,
     'BNFT_LIMIT_001',
     '카페 혜택 한도가 얼마 남지 않았어요',
     '삼성 iD ON 카드의 카페 혜택을 이번 달 6,600원 사용했습니다.',
     'SENT',
     '2026-07-20 09:00:00', '2026-07-20 09:00:03',
     '2026-07-20 08:55:00',
     '2026-07-20 10:00:00', NULL,
     'BNFT_LIMIT_001:BENEFIT:4:2026-07'),

    (3, 1, NULL, NULL, 4,
     'POINT_USE_001',
     '사용 가능한 포인트가 있어요',
     'KB 포인트리 12,500점을 보유하고 있습니다.',
     'SENT',
     '2026-07-21 09:00:00', '2026-07-21 09:00:02',
     '2026-07-21 08:55:00',
     NULL, NULL,
     'POINT_USE_001:WALLET:1:2026-07-21'),

    (4, 1, 1, NULL, NULL,
     'MONTH_END_CARD_001',
     '월말 카드 사용 현황을 확인하세요',
     '대표 카드의 실적과 혜택 한도를 확인해 보세요.',
     'PENDING',
     '2026-07-31 18:00:00', NULL,
     '2026-07-21 12:00:00',
     NULL, NULL,
     'MONTH_END_CARD_001:USER_CARD:1:2026-07'),

    (5, 2, 4, NULL, NULL,
     'PERF_SHORT_001',
     '실적 달성 안내',
     '다음 달 혜택 조건을 위해 카드 실적을 확인하세요.',
     'FAILED',
     '2026-07-18 09:00:00', NULL,
     '2026-07-18 08:55:00',
     NULL, NULL,
     'PERF_SHORT_001:USER_CARD:4:2026-07');

COMMIT;

-- ============================================================
-- 시드 확인용 간단 조회
-- ============================================================

SELECT 'member' AS table_name, COUNT(*) AS row_count FROM member
UNION ALL SELECT 'term', COUNT(*) FROM term
UNION ALL SELECT 'card', COUNT(*) FROM card
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'merchant', COUNT(*) FROM merchant
UNION ALL SELECT 'benefit', COUNT(*) FROM benefit
UNION ALL SELECT 'user_card', COUNT(*) FROM user_card
UNION ALL SELECT 'expense', COUNT(*) FROM expense
UNION ALL SELECT 'notification', COUNT(*) FROM notification;
