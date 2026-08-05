CREATE TABLE IF NOT EXISTS member_personalization_category (
    member_id    BIGINT      NOT NULL COMMENT '회원 ID',
    category_key VARCHAR(30) NOT NULL COMMENT '프론트 개인화 카테고리 키',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_id, category_key),
    CONSTRAINT fk_personalization_category_member
        FOREIGN KEY (member_id) REFERENCES member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 개인화 카테고리';

CREATE TABLE IF NOT EXISTS member_personalization_brand (
    member_id    BIGINT       NOT NULL COMMENT '회원 ID',
    category_key VARCHAR(30)  NOT NULL COMMENT '프론트 개인화 카테고리 키',
    priority     TINYINT      NOT NULL COMMENT '브랜드 표시 순서 1~3',
    brand_name   VARCHAR(100) NOT NULL COMMENT '관심 브랜드명',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (member_id, category_key, priority),
    UNIQUE KEY uk_personalization_brand_name (member_id, category_key, brand_name),
    CONSTRAINT fk_personalization_brand_category
        FOREIGN KEY (member_id, category_key)
        REFERENCES member_personalization_category (member_id, category_key)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '회원 개인화 관심 브랜드';

-- 기존 선호도 데이터를 최초 개인화 설정으로 이관한다.
INSERT IGNORE INTO member_personalization_category (member_id, category_key)
SELECT pc.member_id,
       CASE c.category_code
           WHEN 'CAFE' THEN 'cafe'
           WHEN 'CONVENIENCE_STORE' THEN 'convenience'
           WHEN 'RESTAURANT' THEN 'food'
           WHEN 'DELIVERY' THEN 'restaurant'
           WHEN 'LARGE_MART' THEN 'mart'
           WHEN 'DEPARTMENT_STORE' THEN 'department'
           WHEN 'BEAUTY' THEN 'beauty'
           WHEN 'CULTURE_LEISURE' THEN 'culture'
           WHEN 'PUBLIC_TRANSPORT' THEN 'transport'
           WHEN 'FUEL' THEN 'gas'
       END
FROM member_preferred_category pc
JOIN category c ON c.category_id = pc.category_id
WHERE c.category_code IN (
    'CAFE', 'CONVENIENCE_STORE', 'RESTAURANT', 'DELIVERY', 'LARGE_MART',
    'DEPARTMENT_STORE', 'BEAUTY', 'CULTURE_LEISURE', 'PUBLIC_TRANSPORT', 'FUEL'
);

INSERT IGNORE INTO member_personalization_brand
    (member_id, category_key, priority, brand_name)
SELECT pm.member_id,
       CASE c.category_code
           WHEN 'CAFE' THEN 'cafe'
           WHEN 'CONVENIENCE_STORE' THEN 'convenience'
           WHEN 'RESTAURANT' THEN 'food'
           WHEN 'DELIVERY' THEN 'restaurant'
           WHEN 'LARGE_MART' THEN 'mart'
           WHEN 'DEPARTMENT_STORE' THEN 'department'
           WHEN 'BEAUTY' THEN 'beauty'
           WHEN 'CULTURE_LEISURE' THEN 'culture'
           WHEN 'PUBLIC_TRANSPORT' THEN 'transport'
           WHEN 'FUEL' THEN 'gas'
       END,
       pm.priority,
       m.merchant_name
FROM member_preferred_merchant pm
JOIN category c ON c.category_id = pm.category_id
JOIN merchant m ON m.merchant_id = pm.merchant_id
WHERE c.category_code IN (
    'CAFE', 'CONVENIENCE_STORE', 'RESTAURANT', 'DELIVERY', 'LARGE_MART',
    'DEPARTMENT_STORE', 'BEAUTY', 'CULTURE_LEISURE', 'PUBLIC_TRANSPORT', 'FUEL'
);
