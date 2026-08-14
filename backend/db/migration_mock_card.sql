-- ============================================================
-- Mock 카드(카드번호 기반 자동 등록) 마이그레이션
--
-- schema.sql은 DROP 후 전부 새로 만드는 스크립트라, 이미 데이터가 들어있는 DB에는
-- 그대로 돌릴 수 없다. 이 파일은 그런 DB에 mock_card 테이블과 시드만 얹기 위한 것이다.
-- 새로 세팅하는 DB라면 이 파일 대신 schema.sql + 93_seed_mock_card.sql을 쓰면 된다.
--
-- 여러 번 실행해도 안전하도록 작성했다.
-- 실행: mysql -u root -p {DB이름} < db/migration_mock_card.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. mock_card: 등록을 허용할 카드번호와 그 번호가 가리키는 카드 상품을 담는다.
--    외부 카드사 연동이 없어 카드번호만으로는 카드 상품을 알 수 없으므로,
--    서버가 신뢰하는 allowlist를 두고 여기서 카드 상품을 결정한다.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mock_card (
    mock_card_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Mock 카드 ID',
    card_id      BIGINT      NOT NULL COMMENT '연결할 카드 상품 ID',
    card_number  VARCHAR(19) NOT NULL COMMENT '정규화된 전체 카드번호',
    is_active    CHAR(1)     NOT NULL DEFAULT 'Y' COMMENT '등록 허용 여부: Y | N',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (mock_card_id),
    UNIQUE KEY uk_mock_card_number (card_number),
    KEY idx_mock_card_card_active (card_id, is_active),
    CONSTRAINT fk_mock_card_card FOREIGN KEY (card_id) REFERENCES card (card_id),
    CONSTRAINT chk_mock_card_number CHECK (card_number REGEXP '^[0-9]{13,19}$'),
    CONSTRAINT chk_mock_card_active CHECK (is_active IN ('Y', 'N'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '시연용 카드번호와 카드 상품 매핑';


-- ------------------------------------------------------------
-- 2. 시연용 번호 시드. 모두 룬(Luhn) 검증을 통과하는 가상 번호다.
--    card_number에 UNIQUE가 걸려 있어 ON DUPLICATE KEY로 재실행에 안전하다.
-- ------------------------------------------------------------
INSERT INTO mock_card (card_id, card_number, is_active) VALUES
    (1,  '2228790000000016', 'Y'),
    (2,  '2228790000000024', 'Y'),
    (3,  '2228790000000032', 'Y'),
    (4,  '2228790000000040', 'Y'),
    (5,  '2228790000000057', 'Y'),
    (6,  '3762930000000067', 'Y'),
    (7,  '3762930000000075', 'Y'),
    (8,  '3762930000000083', 'Y'),
    (9,  '3762930000000091', 'Y'),
    (10, '3762930000000109', 'Y'),
    (11, '3560780000000110', 'Y'),
    (12, '3560780000000128', 'Y'),
    (13, '3560780000000136', 'Y'),
    (14, '3560780000000144', 'Y'),
    (15, '3560780000000151', 'Y')
ON DUPLICATE KEY UPDATE
    card_id   = VALUES(card_id),
    is_active = VALUES(is_active);


-- ------------------------------------------------------------
-- 3. 확인용 조회 — 등록 화면에서 쓸 수 있는 번호 목록
-- ------------------------------------------------------------
SELECT mc.card_number, c.card_name, cc.company_name, mc.is_active
FROM mock_card mc
INNER JOIN card c ON c.card_id = mc.card_id
INNER JOIN card_company cc ON cc.card_company_id = c.card_company_id
ORDER BY mc.mock_card_id;
