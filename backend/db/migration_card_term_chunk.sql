-- PR #59 (카드 약관 원문 적재 + 챗봇 약관 질의응답) 반영용 마이그레이션.
--
-- schema.sql 은 DROP TABLE 로 시작해 전체를 다시 만든다. 이미 데이터가 들어 있는
-- 로컬 DB 에 그대로 돌리면 회원·카드까지 날아가므로, 늘어난 부분만 여기 따로 둔다.
--
--   mysql -u root -p wallet_team_pr33_demo < backend/db/migration_card_term_chunk.sql
--
-- 한 번만 돌린다. MySQL 은 ADD COLUMN/ADD KEY 에 IF NOT EXISTS 를 받지 않아
-- 이미 반영된 DB 에서 다시 돌리면 "Duplicate column" 으로 멈춘다 (그 시점에 이미 끝난 상태다).

-- ── 1. card_term_document: 카드사 마스터 연결 ────────────────────────────
-- 수집기가 쓰는 표기(KB국민)와 카드사 마스터의 정식 명칭(KB국민카드)이 달라
-- 문자열로 이으면 표기가 갈리는 순간 조인이 조용히 빈다. ID 로 잇는다.
-- issuer 는 그대로 남긴다 — 매칭이 틀렸을 때 무엇을 보고 이었는지 근거가 된다.
ALTER TABLE card_term_document
    ADD COLUMN card_company_id BIGINT NULL
        COMMENT '카드사 ID. 마스터와 매칭되기 전이면 NULL'
        AFTER source_card_name;

ALTER TABLE card_term_document
    ADD KEY idx_card_term_document_company (card_company_id);

ALTER TABLE card_term_document
    ADD CONSTRAINT fk_card_term_document_company FOREIGN KEY (card_company_id)
        REFERENCES card_company (card_company_id);

-- ── 2. card_term_chunk: 약관 원문 검색 조각 ──────────────────────────────
-- 원문 한 장이 수만 자라 통째로는 프롬프트에 들어가지 않는다. 조문 단위로 잘라 두고
-- 질문에 가까운 몇 개만 골라 넣는다.
--
-- 개정본이 새 행으로 쌓이는 원문과 달리 조각은 현행본만 남긴다. 옛 조각이 함께 있으면
-- 지난 시행본과 현행본이 나란히 검색돼 어느 쪽이 지금 맞는 답인지 가릴 수 없다.
CREATE TABLE IF NOT EXISTS card_term_chunk (
    card_term_chunk_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '약관 조각 ID',
    card_term_document_id BIGINT       NOT NULL COMMENT '약관 문서 ID',
    chunk_index           INT          NOT NULL COMMENT '문서 안에서의 순서. 0부터',
    -- 조각만 따로 읽는 검색 단계에서 무엇에 관한 규정인지 드러나게 한다.
    -- 조문 구조가 없는 문서(상품설명서)는 글자 수로 자르므로 값이 없다.
    heading               VARCHAR(200) NULL COMMENT '속한 장·조 제목. 조문 구조가 없으면 NULL',
    content               TEXT         NOT NULL COMMENT '조각 본문',
    -- 임베딩은 float32 배열을 그대로 담는다. JSON 문자열로 두면 숫자 하나가
    -- 열 바이트 남짓을 먹어 같은 값이 두 배 넘는 자리를 차지한다.
    -- 파생물이라 값이 없어도 검색은 동작한다(키워드 검색으로만 돌아간다).
    embedding             BLOB         NULL COMMENT '임베딩 벡터(float32 이진). 미생성이면 NULL',
    embedding_model       VARCHAR(50)  NULL COMMENT '임베딩을 만든 모델. 섞이면 유사도가 무의미해진다',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',

    PRIMARY KEY (card_term_chunk_id),
    -- 같은 문서를 다시 자르면 조각이 두 벌로 쌓인다. 순서 번호로 막는다.
    UNIQUE KEY uk_card_term_chunk (card_term_document_id, chunk_index),
    -- 한글은 띄어쓰기로 단어가 갈리지 않아 기본 파서로는 색인이 되지 않는다.
    -- ngram 파서는 글자 두 개씩 잘라 색인하므로 '분실신고' 같은 말도 걸린다.
    FULLTEXT KEY ft_card_term_chunk_content (content) WITH PARSER ngram,
    -- 문서가 지워지면 조각도 함께 지운다. 남으면 원문 없는 조각이 검색에 걸린다.
    CONSTRAINT fk_card_term_chunk_document FOREIGN KEY (card_term_document_id)
        REFERENCES card_term_document (card_term_document_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT '약관 원문 검색 조각';
