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

    python -m ingest.dump_terms            파일 생성
    mysql -u root -p wallet < <파일>       받는 쪽에서 적용
"""

import sys
from pathlib import Path
from typing import List

from .load import connect

_OUTPUT_PATH = (
    Path(__file__).resolve().parent.parent.parent
    / "backend" / "db" / "local" / "93_card_term.sql"
)

# 뽑는 순서가 곧 적용 순서다. 조각이 문서를 참조하므로 문서가 먼저 들어가야 한다.
_TABLES = ("card_term_document", "card_term_chunk")

# 임베딩은 뽑지 않는다. 조각에서 언제든 다시 만들 수 있는 파생물인데 BLOB이라 파일만 크게 만든다.
# 검색은 임베딩이 없어도 전문검색으로 동작한다.
_SKIP_COLUMNS = {"card_term_chunk": {"embedding", "embedding_model"}}

_HEADER = """-- ============================================================
-- 93_card_term.sql — 카드 약관 원문과 조문 조각
--
-- 저장소에 올리지 않는다. 카드사 약관 원문이라 공개 저장소에 넣으면 재배포가 된다.
-- 이 파일은 ingest/dump_terms.py 가 만들고, 팀 안에서 따로 주고받는다.
--
-- 적용: schema.sql 이 만들어진 DB에 이 파일 하나만 넣으면 챗봇 약관 Q&A 가 동작한다.
--       수집 파이프라인(ingest.run)을 돌릴 필요가 없다.
--
-- 임베딩은 들어 있지 않다 — 조각에서 다시 만들 수 있고, 없어도 전문검색으로 답한다.
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

    _OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    _OUTPUT_PATH.write_text(sql, encoding="utf-8")

    size_mb = len(sql.encode("utf-8")) / 1024 / 1024
    print(f"저장: {_OUTPUT_PATH}  ({size_mb:.2f}MB)")
    for table, count in counts.items():
        print(f"  {table}: {count}행")
    print("\n이 파일은 저장소에 올리지 않는다. 팀 안에서 따로 전달해라.")
    print("받는 쪽 적용:  mysql -u root -p wallet < 93_card_term.sql")


if __name__ == "__main__":
    main()
