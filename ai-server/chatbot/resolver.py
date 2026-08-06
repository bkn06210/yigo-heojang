"""사람이 말한 표현을 가맹점·카드 식별자로 바꾼다.

엔진 API 는 숫자 id 를 받고 사람은 별명으로 말한다. 그 사이를 잇는 자리다.

이 일을 LLM 에게 맡기지 않는 이유는 LLM 이 우리 merchant 테이블을 본 적이 없기 때문이다.
세상의 정식 명칭과 우리 DB 의 표기가 다르다 — '서브웨이'가 아니라 '써브웨이',
'하나로마트'가 아니라 '농협 하나로마트'다. LLM 이 정규화해 줘도 그 문자열이 DB 에
실재하는지 확인하는 단계는 없앨 수 없고, 별칭 테이블이 그 확인의 사전이다.

찾는 순서가 있다.

    ① 정식 명칭 정확 일치
    ② 별칭 정확 일치
    ③ 표현 안에 이름·별칭이 통째로 들어 있는 경우 ("스타벅스에서")
    ④ 실패 → 되묻는다

①이 ②보다 먼저인 것은 판단이다. 정식 명칭은 약관에서 나온 값이라 흔들리지 않지만
별칭은 사람이 손으로 넣는 값이라 실수가 들어올 수 있다. 틀릴 수 있는 쪽을 나중에 본다.

③은 LLM 이 조사를 떼지 못했을 때를 위한 보험이다. 평소에는 ①·②에서 끝난다.
"""

from dataclasses import dataclass
from typing import List, Optional, Tuple

MATCH_NAME = "NAME"
MATCH_ALIAS = "ALIAS"
MATCH_PARTIAL = "PARTIAL"


@dataclass(frozen=True)
class Match:
    """찾은 대상 하나."""

    target_id: int
    name: str
    matched_by: str
    # 가맹점이면 카테고리, 카드면 카드사. 답변에 함께 실어 잘못 잡힌 것을 눈에 띄게 한다.
    # 이마트(대형마트)와 이마트24(편의점)는 혜택이 전혀 다르므로 조용히 틀리면 안 된다.
    detail: Optional[str] = None


@dataclass(frozen=True)
class Resolution:
    """해석 결과.

    못 찾았을 때 후보를 함께 담는다. "이마트인가요, 이마트24인가요?"처럼
    되물으려면 무엇들 사이에서 갈렸는지 알아야 한다.
    """

    match: Optional[Match] = None
    candidates: Tuple[str, ...] = ()

    @property
    def found(self) -> bool:
        return self.match is not None


_MERCHANT_COLUMNS = """
    m.merchant_id AS target_id, m.merchant_name AS name, c.category_name AS detail
"""
_MERCHANT_FROM = """
    FROM merchant m
    JOIN category c ON c.category_id = m.category_id
"""

_MERCHANT_BY_NAME = f"SELECT {_MERCHANT_COLUMNS} {_MERCHANT_FROM} WHERE m.merchant_name = %s"

_MERCHANT_BY_ALIAS = f"""
    SELECT {_MERCHANT_COLUMNS}
    {_MERCHANT_FROM}
    JOIN merchant_alias a ON a.merchant_id = m.merchant_id
    WHERE a.alias = %s
"""

# 표현 안에 이름 또는 별칭이 통째로 들어 있는 경우 ("스타벅스에서", "스벅에서").
# 반대 방향(이름 안에 표현이 들어 있는 경우)은 넣지 않았다. "롯데"가 롯데마트·롯데백화점·
# 롯데시네마 여섯 곳에 걸려 오매칭만 늘고, 사람이 이름 앞부분만 말하는 경우는 드물다.
_MERCHANT_PARTIAL = f"""
    SELECT {_MERCHANT_COLUMNS}
    {_MERCHANT_FROM}
    WHERE %s LIKE CONCAT('%%', m.merchant_name, '%%')
    UNION
    SELECT {_MERCHANT_COLUMNS}
    {_MERCHANT_FROM}
    JOIN merchant_alias a ON a.merchant_id = m.merchant_id
    WHERE %s LIKE CONCAT('%%', a.alias, '%%')
"""

_CARD_COLUMNS = """
    c.card_id AS target_id, c.card_name AS name, co.company_name AS detail
"""
_CARD_FROM = """
    FROM card c
    JOIN card_company co ON co.card_company_id = c.card_company_id
"""

_CARD_BY_NAME = f"SELECT {_CARD_COLUMNS} {_CARD_FROM} WHERE c.card_name = %s"

_CARD_BY_ALIAS = f"""
    SELECT {_CARD_COLUMNS}
    {_CARD_FROM}
    JOIN card_alias a ON a.card_id = c.card_id
    WHERE a.alias = %s
"""

_CARD_PARTIAL = f"""
    SELECT {_CARD_COLUMNS}
    {_CARD_FROM}
    WHERE %s LIKE CONCAT('%%', c.card_name, '%%')
    UNION
    SELECT {_CARD_COLUMNS}
    {_CARD_FROM}
    JOIN card_alias a ON a.card_id = c.card_id
    WHERE %s LIKE CONCAT('%%', a.alias, '%%')
"""


def resolve_merchant(conn, text: Optional[str]) -> Resolution:
    return _resolve(conn, text, _MERCHANT_BY_NAME, _MERCHANT_BY_ALIAS, _MERCHANT_PARTIAL)


def resolve_card(conn, text: Optional[str]) -> Resolution:
    return _resolve(conn, text, _CARD_BY_NAME, _CARD_BY_ALIAS, _CARD_PARTIAL)


def _resolve(conn, text: Optional[str], by_name: str, by_alias: str, partial: str) -> Resolution:
    keyword = (text or "").strip()
    if not keyword:
        return Resolution()

    for sql, matched_by in ((by_name, MATCH_NAME), (by_alias, MATCH_ALIAS)):
        rows = _query(conn, sql, (keyword,))
        if len(rows) == 1:
            return Resolution(match=_to_match(rows[0], matched_by))
        if len(rows) > 1:
            # 이름이 같은 대상이 둘 있는 경우다. 고를 근거가 없으니 되묻는다.
            return Resolution(candidates=_names(rows))

    rows = _query(conn, partial, (keyword, keyword))
    if len(rows) == 1:
        return Resolution(match=_to_match(rows[0], MATCH_PARTIAL))

    winner = _most_specific(rows)
    if winner is not None:
        return Resolution(match=_to_match(winner, MATCH_PARTIAL))
    return Resolution(candidates=_names(rows))


def _most_specific(rows: List[dict]) -> Optional[dict]:
    """후보 하나가 나머지를 전부 문자열로 품고 있으면 그것을 고른다.

    'SKT 요금'을 물으면 'SKT'와 'KT'가 함께 걸린다. 'KT'는 'SKT' 안에 우연히 들어
    있어 딸려온 것이고, 사용자가 글자 그대로 쓴 이름은 'SKT'다. 표현에 통째로 들어
    있는 이름만 후보가 되므로, 나머지를 품는 후보가 있다면 그것이 실제로 입력된 이름이다.

    브랜드 사이의 상하 관계를 따지는 것이 아니다. KT와 SKT는 다른 회사이고
    이마트(대형마트)와 이마트24(편의점)도 다른 브랜드다. 어느 문자열이 입력됐는지만 본다.

    품는 후보가 없으면(서로 겹치지 않는 별개의 이름들이면) 고르지 않고 되묻는다.
    """
    if len(rows) < 2:
        return None

    for candidate in rows:
        others = [row["name"] for row in rows if row["target_id"] != candidate["target_id"]]
        if all(other in candidate["name"] for other in others):
            return candidate
    return None


def _query(conn, sql: str, params: tuple) -> List[dict]:
    with conn.cursor() as cursor:
        cursor.execute(sql, params)
        return list(cursor.fetchall())


def _to_match(row: dict, matched_by: str) -> Match:
    return Match(
        target_id=row["target_id"],
        name=row["name"],
        matched_by=matched_by,
        detail=row.get("detail"),
    )


def _names(rows: List[dict]) -> Tuple[str, ...]:
    return tuple(row["name"] for row in rows)
