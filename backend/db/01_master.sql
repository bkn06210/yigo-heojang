-- ============================================================
-- 01_master.sql — 마스터 3개 (card, category, merchant)
-- ------------------------------------------------------------
-- 실행 순서: 01 → 02 → 03 → 04(stub) → 05
-- 공통 규약
--   · ID는 전부 BIGINT AUTO_INCREMENT (타입 불일치로 인한 FK 오류 방지)
--   · enum 성격 컬럼은 VARCHAR + COMMENT로 허용값 명시 (MySQL ENUM 미사용 —
--     값 추가 시 ALTER TABLE이 필요하고 MyBatis/Java enum과 이중 관리가 되므로)
--   · 금액 = BIGINT (원 단위 정수)
--     API 계약이 "원 정수, 원 미만 절사"이므로 타입이 그 계약을 강제하게 둔다.
--     DECIMAL(12,2)로 두면 소수부가 영원히 .00인데, 그 자리가 있으면 언젠가 1000.50이 들어간다.
--     MyBatis에서 Long으로 그대로 매핑되어 자바 쪽도 단순해진다.
--   · 비율/혜택값 = DECIMAL(10,2) — RATE일 때 1.5% 같은 소수가 실제로 존재하므로 예외
--   · 코드성 참조는 id가 아니라 코드 문자열로 (category_code, merchant_code)
--     제외/조건 규칙의 값 컬럼은 FK를 못 걸어 id를 써도 무결성 이점이 없고, 가독성만 잃는다.
-- ============================================================

-- ------------------------------------------------------------
-- card : 카드 마스터. 카드 1장 = 1행. 카드별 테이블 분리 절대 금지.
-- ------------------------------------------------------------
CREATE TABLE card (
    card_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '카드 ID',
    card_name    VARCHAR(100) NOT NULL COMMENT '카드명 (예: 나라사랑카드)',
    issuer       VARCHAR(50)  NOT NULL COMMENT '카드사 (예: KB국민, 현대)',
    card_type    VARCHAR(20)  NOT NULL COMMENT '카드 종류: CREDIT(신용) | CHECK(체크)',
    annual_fee   INT          NOT NULL DEFAULT 0 COMMENT '연회비(원). 체크카드는 0',
    image_url    VARCHAR(255) NULL COMMENT '카드 이미지 URL',
    description  VARCHAR(500) NULL COMMENT '카드 한줄 소개',
    is_active    CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT '판매중 여부: Y | N',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '카드 마스터';

-- ------------------------------------------------------------
-- category : 카테고리 표준 (팀 합의 대상). 혜택·소비내역이 공통으로 참조한다.
--   parent_category_id로 대분류/중분류 계층을 표현할 수 있게 열어둔다.
--   (계층을 안 쓰기로 하면 전부 NULL이면 되고, 스키마 변경은 필요 없다)
-- ------------------------------------------------------------
CREATE TABLE category (
    category_id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    category_code      VARCHAR(30) NOT NULL COMMENT '카테고리 코드 (예: CAFE, TRANSPORT)',
    category_name      VARCHAR(50) NOT NULL COMMENT '카테고리명 (예: 카페)',
    parent_category_id BIGINT      NULL COMMENT '상위 카테고리 ID. 최상위면 NULL',
    display_order      INT         NOT NULL DEFAULT 0 COMMENT '화면 노출 순서',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_code (category_code),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) REFERENCES category (category_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '카테고리 표준';

-- ------------------------------------------------------------
-- merchant : 혜택이 걸린 브랜드 단위 가맹점 (스타벅스, 롯데리아 …).
--   지점 단위가 아니다. 상품 단위 조건은 범위 밖(가맹점까지만 내려감).
--   category_id : 이 가맹점의 소속 카테고리.
--     혜택 매칭에서 "가맹점 직접 혜택 + 카테고리 혜택 둘 다" 조회할 때,
--     가맹점 → 카테고리로 올라가는 경로가 이 컬럼이다.
--
--   로고 이미지 컬럼은 두지 않는다. 브랜드 로고는 상표라 실물 확보가 곤란하고,
--   화면은 카테고리 단위 아이콘(24개)으로 대체된다. 필요해지면 NULL 컬럼이라
--   ALTER 한 줄로 붙일 수 있어 지금 미리 넣을 이유가 없다.
--   (card.image_url은 남긴다 — 카드는 10장뿐이라 확보가 쉽고, 카드 목록·추천 결과 화면이
--    쓸 개연성이 높다. 다만 현재 api-engine.md 응답 필드에는 없으므로, 프론트가 필요하다고
--    하면 그때 응답에 cardImageUrl을 추가한다.)
-- ------------------------------------------------------------
CREATE TABLE merchant (
    merchant_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '가맹점(브랜드) ID',
    merchant_code VARCHAR(30)  NOT NULL COMMENT '가맹점 코드 (예: STARBUCKS). 제외 규칙에서 이 값으로 참조한다',
    merchant_name VARCHAR(100) NOT NULL COMMENT '가맹점명 (예: 스타벅스)',
    category_id   BIGINT       NOT NULL COMMENT '소속 카테고리 ID',
    PRIMARY KEY (merchant_id),
    UNIQUE KEY uk_merchant_code (merchant_code),
    KEY idx_merchant_category (category_id),
    CONSTRAINT fk_merchant_category FOREIGN KEY (category_id) REFERENCES category (category_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '가맹점(브랜드)';
