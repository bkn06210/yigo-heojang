-- ============================================================
-- data.sql — 카드 혜택 최적화 전자지갑 개발/테스트용 시드 데이터
-- 대상 스키마: schema.sql (MySQL 8.0)
--
-- 실행 순서:
--   1) schema.sql
--   2) data.sql                   이 파일 — 회원·약관·카테고리·포인트
--   3) 91_seed_card_benefit.sql   카드사·BIN·카드·가맹점·실적구간·혜택
--   4) 92_seed_user_data.sql      보유카드·소비내역·결제·월별 계산 상태
--
-- 파일이 셋인 이유:
--   · 91은 카드 약관에서 기계로 뽑아낸 결과라 손으로 고치지 않는다.
--   · 92는 엔진에 거래를 흘려보내 계산 결과까지 채운 것이다. 사람이 맞출 수 없는
--     묶음 한도·구간별 한도 상속·일 소진 리셋이 얽혀 있어, 손으로 넣으면 규칙과 어긋난다.
--   · 셋이 같은 테이블에 행을 넣으면 PK가 충돌하므로 경계를 넘지 않는다.
--
-- 주의:
--   · 개발/테스트 환경 전용 데이터다.
--   · ID를 명시적으로 고정해 FK 관계와 테스트 결과를 예측 가능하게 했다.
--   · 비밀번호와 토큰 값은 실제 원문이 아닌 테스트용 해시 문자열이다.
--   · 섹션 번호는 원래 순서를 유지한다(3·5~11·13·14는 91·92로 옮겨 비어 있다).
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- ============================================================
-- 1. 회원 · 인증
-- ============================================================

INSERT INTO member (
    member_id, email, password_hash, name, nickname, member_status,
    created_at, updated_at, withdrawn_at
) VALUES
    (1, 'active@example.com',
     '$2a$10$Exc5x/juU54pWSz1Wo8E7Opgva7S.1W5uq6in1D/BH.krG3gKs93C',
     '김활성', '별명A', 'ACTIVE',
     '2026-06-01 09:00:00', '2026-07-20 10:00:00', NULL),

    (2, 'suspended@example.com',
     '$2a$10$Exc5x/juU54pWSz1Wo8E7Opgva7S.1W5uq6in1D/BH.krG3gKs93C',
     '이정지', '별명B', 'SUSPENDED',
     '2026-06-05 09:00:00', '2026-07-18 15:00:00', NULL),

    -- member_id=3은 "탈퇴 처리가 이미 끝난 뒤"의 상태를 나타낸다.
    -- email/name/nickname/password_hash가 원본 그대로 남아있으면 안 되므로,
    -- 실제 탈퇴 서비스 코드가 채워 넣을 값의 형태를 그대로 흉내 낸다.
    -- email: 'withdrawn_{memberId}@deleted.local' 형태로, uk_member_email 제약을 어기지 않으면서
    --        재가입(원래 이메일로 다시 가입) 시 중복 체크에 걸리지 않게 한다.
    -- password_hash: 어떤 원문으로도 매칭될 수 없는 무의미한 문자열로, 로그인 자체가 불가능하게 한다.
    (3, 'withdrawn_3@deleted.local',
     'WITHDRAWN_MEMBER_CANNOT_LOGIN',
     '탈퇴회원', '탈퇴회원', 'WITHDRAWN',
     '2026-05-01 09:00:00', '2026-07-10 14:00:00', '2026-07-10 14:00:00');

INSERT INTO member_withdrawal (
    member_withdrawal_id, member_id, reason_type, reason_detail,
    withdrawn_at, created_at
) VALUES
    (1, 3, 'LOW_USAGE', '서비스를 자주 사용하지 않아서 탈퇴합니다.',
     '2026-07-10 14:00:00', '2026-07-10 14:00:00');

-- member_withdrawal(탈퇴 사유)과 별개로, 위에서 익명화되며 사라진 원본 개인정보(이메일·성명)를
-- 3년 보관 목적으로 옮겨 담은 행이다. 실제 서비스에서는 회원 탈퇴 트랜잭션이
-- 익명화 UPDATE 직전에 원본을 읽어 이 테이블에 INSERT한다.
INSERT INTO member_withdrawal_archive (
    member_withdrawal_archive_id, member_id, email, email_hash, name,
    withdrawn_at, retention_reason
) VALUES
    (1, 3, 'withdrawn@example.com',
     '07aef5eecdfdc974d2007a8453bb7dff05c0279bae91bbb9b17c7c5bc1350719',
     '박탈퇴',
     '2026-07-10 14:00:00',
     '재가입 어뷰징 방지 및 CS 대응을 위한 보관. 탈퇴 시 회원에게 고지하고 동의를 받음');

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
      (1, 'SERVICE_TERMS', 'SIGNUP', '서비스 이용약관', 1, 'ACTIVE'),
      (2, 'PRIVACY_POLICY', 'SIGNUP', '개인정보 수집 및 이용 동의', 1, 'ACTIVE'),
      (3, 'MARKETING_CONSENT', 'SIGNUP', '마케팅 정보 수신 동의', 0, 'ACTIVE'),
      -- 회원 탈퇴 화면에서 고지하고 동의를 받는 약관이다.
      -- is_required = 1인 이유: 탈퇴는 신용정보 즉시 삭제·개인정보 3년 보관이라는
      -- 되돌릴 수 없는 처리가 뒤따르므로, 이 고지에 동의하지 않으면 탈퇴 자체를 진행할 수 없다.
      (4, 'WITHDRAWAL_NOTICE', 'WITHDRAWAL', '회원 탈퇴 안내 및 동의', 1, 'ACTIVE');

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
    (4, 4, '2026-07-01',
     '1. 탈퇴 시 보유카드, 소비내역, 결제내역, 포인트 정보는 즉시 삭제되며 복구할 수 없습니다.
2. 이메일, 성명 등 회원정보는 소비자 분쟁 처리를 위해 탈퇴일로부터 3년간 보관 후 파기됩니다.
3. 탈퇴 후 동일 이메일로 재가입할 수 있으나, 이전 데이터는 복원되지 않습니다.',
     '2026-07-01 00:00:00', NULL);

INSERT INTO member_term_agreement (
    member_term_agreement_id, member_id, term_version_id,
    is_agreed, agreed_at
) VALUES
    (1, 1, 1, 1, '2026-06-01 09:00:00'),
    (2, 1, 2, 1, '2026-06-01 09:00:00'),
    (3, 1, 3, 1, '2026-06-01 09:00:00'),
    (4, 2, 1, 1, '2026-06-05 09:00:00'),
    (5, 2, 2, 1, '2026-06-05 09:00:00'),
    (6, 2, 3, 0, NULL),
    (7, 3, 4, 1, '2026-07-10 14:00:00');

-- ============================================================
-- 4. 카테고리 표준: 대분류 7 + 중분류 31
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
    (6, 'MEDICAL', '의료', NULL, 6),
    (7, 'EDUCATION', '교육', NULL, 7);

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
    (206, 'BOOKSTORE', '서점', 2, 6),
    (207, 'SUPERMARKET', '슈퍼마켓', 2, 7);

-- 교통
INSERT INTO category VALUES
    (301, 'PUBLIC_TRANSPORT', '대중교통', 3, 1),
    (302, 'TAXI', '택시', 3, 2),
    (303, 'FUEL', '주유', 3, 3),
    (304, 'PARKING_MAINTENANCE', '주차정비', 3, 4),
    (305, 'RAILWAY', '철도', 3, 5),
    (306, 'EXPRESS_BUS', '고속시외버스', 3, 6);

-- 생활
INSERT INTO category VALUES
    (401, 'TELECOM', '이동통신', 4, 1),
    (402, 'UTILITY', '공과금', 4, 2),
    (403, 'APARTMENT_FEE', '아파트관리비', 4, 3),
    (404, 'INSURANCE', '보험료', 4, 4),
    (405, 'LIFE_SERVICE', '세탁생활서비스', 4, 5),
    (406, 'RENT', '임대료', 4, 6),
    (407, 'PERSONAL_CARE', '개인관리서비스', 4, 7);

-- 문화여가
INSERT INTO category VALUES
    (501, 'MOVIE', '영화', 5, 1),
    (502, 'SUBSCRIPTION_STREAMING', '구독스트리밍', 5, 2),
    (503, 'SPORTS_LEISURE', '스포츠레저', 5, 3),
    (504, 'GOLF', '골프', 5, 4);

-- 의료
INSERT INTO category VALUES
    (601, 'HOSPITAL', '병원', 6, 1),
    (602, 'PHARMACY', '약국', 6, 2),
    (603, 'ANIMAL_HOSPITAL', '동물병원', 6, 3);

-- 교육
INSERT INTO category VALUES
    (701, 'ACADEMY', '학원', 7, 1),
    (702, 'TUITION', '학교납입금', 7, 2);

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

    (3, 'CJ ONE', 'MEMBERSHIP', '/images/cj-one.png', 'Y', 'Y', 3, 'CJ 계열 제휴 멤버십'),
    (4, '해피포인트', 'MEMBERSHIP', '/images/happypoint.png', 'Y', 'Y', 1, 'SPC 계열 제휴 멤버십'),
    (5, 'L.POINT', 'MEMBERSHIP', '/images/lpoint.png', 'Y', 'Y', 2, '롯데 계열 제휴 멤버십'),
    (6, '신세계포인트', 'MEMBERSHIP', '/images/ssg-point.png', 'Y', 'N', NULL, '신세계 계열 제휴 멤버십'),
    (7, 'H.Point', 'MEMBERSHIP', '/images/h-point.png', 'Y', 'N', NULL, '현대백화점 그룹 제휴 멤버십'),
    (8, 'OK캐쉬백', 'MEMBERSHIP', '/images/okcashbag.png', 'Y', 'N', NULL, '포인트 적립·사용 멤버십'),
    (9, 'GS ALL 멤버십', 'MEMBERSHIP', '/images/gs-all.png', 'Y', 'N', NULL, 'GS 계열 통합 멤버십'),
    (10, 'E.POINT', 'MEMBERSHIP', '/images/e-point.png', 'Y', 'N', NULL, '이랜드 계열 제휴 멤버십'),
    (11, 'NH멤버스', 'MEMBERSHIP', '/images/nh-members.png', 'Y', 'N', NULL, '농협 포인트 멤버십'),
    (12, '뷰티포인트', 'MEMBERSHIP', '/images/beauty-point.png', 'Y', 'N', NULL, '아모레퍼시픽 뷰티 멤버십'),
    (13, 'T 멤버십', 'MEMBERSHIP', '/images/t-membership.png', 'Y', 'N', NULL, 'SKT 통신사 멤버십'),
    (14, 'KT 멤버십', 'MEMBERSHIP', '/images/kt-membership.png', 'Y', 'N', NULL, 'KT 통신사 멤버십'),
    (15, 'U+ 멤버십', 'MEMBERSHIP', '/images/uplus-membership.png', 'Y', 'N', NULL, 'LG U+ 통신사 멤버십'),
    (16, '네이버플러스 멤버십', 'MEMBERSHIP', '/images/naver-plus.png', 'Y', 'N', NULL, '네이버 구독형 멤버십'),
    (17, 'PAYCO 포인트', 'MEMBERSHIP', '/images/payco-point.png', 'Y', 'N', NULL, 'PAYCO 포인트 멤버십'),
    (18, '삼성패션 멤버십', 'MEMBERSHIP', NULL, 'Y', 'N', NULL, '삼성물산 패션 멤버십'),
    (19, 'LF Members', 'MEMBERSHIP', '/images/lf-members.png', 'Y', 'N', NULL, 'LF 패션 멤버십'),
    (20, '한섬 THE 클럽', 'MEMBERSHIP', '/images/handsome-club.png', 'Y', 'N', NULL, '한섬 패션 멤버십'),
    (21, '블루멤버스', 'MEMBERSHIP', '/images/blue-members.png', 'Y', 'N', NULL, '현대자동차 멤버십'),
    (22, '기아멤버스', 'MEMBERSHIP', '/images/kia-members.png', 'Y', 'N', NULL, '기아자동차 멤버십');

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
    -- expense_id는 소비내역이 92_seed_user_data.sql에서 만들어지므로 여기서는 채우지 않는다.
    -- 거래에 딸린 적립은 그 파일이 함께 만든다.
    (1, 1, 1, NULL, 'SAVE', 10000, '초기 테스트 포인트 적립', '2026-06-01 09:10:00'),
    (2, 1, 1, NULL, 'SAVE',   500, '이벤트 포인트 적립', '2026-07-06 18:40:05'),
    (3, 1, 1, NULL, 'SAVE',  3000, '이벤트 포인트 적립', '2026-07-15 09:00:00'),
    (4, 1, 1, NULL, 'USE',   1000, '포인트 사용', '2026-07-18 12:00:00'),

    (5, 1, 2, NULL, 'SAVE',  5500, '초기 테스트 포인트 적립', '2026-06-03 12:10:00'),
    (6, 1, 2, NULL, 'SAVE',  2800, '이벤트 포인트 적립', '2026-07-12 20:00:05'),

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

COMMIT;

-- ============================================================
-- 시드 확인용 간단 조회
-- ============================================================

-- 이 파일이 넣는 것만 센다. 카드·혜택은 91, 보유카드·소비내역은 92가 채운다.
SELECT 'member' AS table_name, COUNT(*) AS row_count FROM member
UNION ALL SELECT 'term', COUNT(*) FROM term
UNION ALL SELECT 'member_withdrawal_archive', COUNT(*) FROM member_withdrawal_archive
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'point_provider', COUNT(*) FROM point_provider
UNION ALL SELECT 'point_wallet', COUNT(*) FROM point_wallet;
