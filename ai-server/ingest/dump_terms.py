"""약관 원문과 조문 조각을 SQL 파일로 뽑는다 — 저장소 밖으로 전달하기 위한 도구.

혜택 시드(91)와 달리 이 결과물은 <b>저장소에 올리지 않는다</b>. 카드사 약관 원문이라
공개 저장소에 넣으면 재배포가 된다. 그래서 파일은 gitignore 대상 폴더에 만들고,
파일 자체는 팀 안에서 다른 경로로 주고받는다. 저장소에 올라가는 것은 이 스크립트뿐이다.

<b>왜 필요한가.</b> 원문은 시드 SQL이 없어 ingest.run(카드사 사이트 접속)으로만 채워진다.
그래서 DB를 다시 만들면 약관 Q&A가 조용히 죽는다 — 오류가 아니라 "찾지 못했다"로 답해서
시연 중에야 알게 된다. 실제로 카드별 상품설명서가 그렇게 사라진 적이 있다.
받는 사람이 수집 파이프라인을 돌리지 않고 SQL 한 번으로 복구할 수 있어야 한다.

여러 번 적용해도 결과가 같다. 두 테이블을 비우고 다시 넣으므로, 이전에 들어 있던
지난 회차 문서가 남아 뒤섞이지 않는다.

조각의 임베딩까지 담는다. 약관 검색이 임베딩으로만 돌기 때문에 원문만 넘기면
받는 쪽에서 약관 Q&A 가 여전히 죽어 있다.

    python -m ingest.dump_terms            파일 생성
    mysql -u root -p wallet < <파일>       받는 쪽에서 적용 (번호 순서상 맨 뒤)
"""

import sys
from pathlib import Path
from typing import List

from .load import connect

_OUTPUT_PATH = (
    Path(__file__).resolve().parent.parent.parent
    / "backend" / "db" / "local" / "95_card_term.sql"
)

# 뽑는 순서가 곧 적용 순서다. 조각이 문서를 참조하므로 문서가 먼저 들어가야 한다.
_TABLES = ("card_term_document", "card_term_chunk")

# 임베딩까지 함께 뽑는다. 파생물이라 받는 쪽에서 다시 만들 수도 있지만, 그러려면 사람마다
# 임베딩 공급자 키가 있어야 하고 중간에 끊기면 절반만 색인된 상태가 된다. 그 상태는 챗봇이
# 정상으로 뜨고 어떤 질문만 못 찾는 모양이라 원인이 데이터라는 것을 알아채기 어렵다.
_SKIP_COLUMNS = {}

_HEADER = """-- ============================================================
-- 95_card_term.sql — 카드 약관 원문과 조문 조각
--
-- 저장소에 올리지 않는다. 카드사 약관 원문이라 공개 저장소에 넣으면 재배포가 된다.
-- 이 파일은 ingest/dump_terms.py 가 만들고, 팀 안에서 따로 주고받는다.
--
-- 실행 순서: schema.sql -> data.sql -> 91 -> 92 -> 93 -> 94 -> 이 파일
--            문서가 카드사 마스터를 참조하므로 최소한 91 다음이어야 한다.
--            수집 파이프라인(ingest.run)을 돌릴 필요가 없다.
--
-- 조각의 임베딩까지 들어 있다. 약관 검색이 임베딩으로만 돌아서, 이 값이 없으면
-- 챗봇이 정상으로 뜬 채 약관 질문에만 "찾지 못했다"고 답한다.
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- 참조하는 쪽(조각)부터 지운다. 반대로 지우면 FK 제약에 걸린다.
DELETE FROM card_term_chunk;
DELETE FROM card_term_document;

"""


def sql_value(value) -> str:
    """SQL 리터럴. 약관 본문에 작은따옴표와 역슬래시가 들어 있어 그대로 넣으면 구문이 깨진다."""
    if value is None:
        return "NULL"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (bytes, bytearray)):
        return "0x" + value.hex()
    text = str(value)
    escaped = text.replace("\\", "\\\\").replace("'", "\\'")
    return "'" + escaped + "'"


def columns_of(cursor, table: str) -> List[str]:
    """실제 DB에서 칸 이름을 읽는다. 스키마가 바뀌어도 이 스크립트를 고칠 일이 없다."""
    cursor.execute(
        "SELECT column_name FROM information_schema.columns"
        " WHERE table_schema = DATABASE() AND table_name = %s"
        " ORDER BY ordinal_position",
        (table,),
    )
    skip = _SKIP_COLUMNS.get(table, set())
    return [name for (name,) in cursor.fetchall() if name not in skip]


def dump_table(cursor, table: str) -> tuple:
    """INSERT 문 줄들과 행 수를 함께 돌려준다.

    행 수를 만들어진 SQL에서 세지 않는다 — 약관 본문에 줄바꿈이 들어 있어
    줄을 세면 실제보다 몇 배 큰 값이 나온다(8건이 3,483건으로 보였다).
    """
    columns = columns_of(cursor, table)
    cursor.execute(
        "SELECT {} FROM {} ORDER BY {}".format(
            ", ".join(columns), table, columns[0]
        )
    )
    rows = cursor.fetchall()
    if not rows:
        return [], 0

    lines = ["INSERT INTO {} ({}) VALUES".format(table, ", ".join(columns))]
    values = ["    (" + ", ".join(sql_value(v) for v in row) + ")" for row in rows]
    lines.append(",\n".join(values) + ";")
    lines.append("")
    return lines, len(rows)


def embedding_state() -> tuple:
    """임베딩이 몇 개나 들어 있고 어떤 모델로 만들었는지."""
    conn = connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT COUNT(*), COUNT(embedding),"
                " GROUP_CONCAT(DISTINCT embedding_model ORDER BY embedding_model)"
                " FROM card_term_chunk"
            )
            total, embedded, models = cursor.fetchone()
    finally:
        conn.close()
    return embedded or 0, total or 0, (models or "").split(",") if models else []


def build() -> tuple:
    conn = connect()
    try:
        with conn.cursor() as cursor:
            lines = [_HEADER]
            counts = {}
            for table in _TABLES:
                table_lines, row_count = dump_table(cursor, table)
                counts[table] = row_count
                lines.extend(table_lines)
            lines.append("COMMIT;\n")
            return "\n".join(lines), counts
    finally:
        conn.close()


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    sql, counts = build()

    if not counts.get("card_term_document"):
        # 빈 파일을 만들어 두면 받는 쪽이 적용하고도 약관 Q&A 가 안 되는 이유를 모른다.
        print("약관 원문이 DB에 없다. ingest.run 또는 ingest.load_local 로 채운 뒤 다시 실행해라.")
        sys.exit(1)

    embedded, total, models = embedding_state()
    if not embedded:
        # 원문만 든 파일은 받는 쪽에서 약관 질문에만 조용히 실패한다. 만들지 않는다.
        print(f"조각 {total}개에 임베딩이 하나도 없다."
              " python -m ingest.build_embeddings 로 색인한 뒤 다시 실행해라.")
        sys.exit(1)

    _OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    _OUTPUT_PATH.write_text(sql, encoding="utf-8")

    size_mb = len(sql.encode("utf-8")) / 1024 / 1024
    print(f"저장: {_OUTPUT_PATH}  ({size_mb:.2f}MB)")
    for table, count in counts.items():
        print(f"  {table}: {count}행")
    print(f"  임베딩: {embedded}/{total}개 · 모델 {', '.join(models)}")

    if embedded < total:
        # 절반만 든 파일을 모르고 넘기면 받는 쪽에서 어떤 질문만 답이 안 나온다.
        print()
        print(f"⚠ 조각 {total - embedded}개에 임베딩이 없다. 그 조각은 검색에 잡히지 않는다")
    if len(models) > 1:
        # 좌표계가 다른 값이 한 표에 섞이면 어느 쪽이 가까운지가 무의미해진다.
        print()
        print("⚠ 모델이 섞여 있다. build_embeddings 로 한 모델에 맞춘 뒤 다시 뽑아라")
    print("\n이 파일은 저장소에 올리지 않는다. 팀 안에서 따로 전달해라.")
    print("받는 쪽 적용:  시드 SQL 을 번호 순으로 넣은 다음"
          "  mysql -u root -p wallet < 95_card_term.sql")


if __name__ == "__main__":
    main()
