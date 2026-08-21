"""약관 원문에서 질문과 관련된 조항을 찾는다.

금액·한도 같은 계산값은 엔진 API 에서 받지만 약관은 다르다. 계산 규칙이 아니라
텍스트라 두 곳에서 규칙이 갈릴 위험이 없고, 엔진이 내려줄 수 있는 모양도 아니다.
그래서 이것만 DB 를 직접 읽는다.

찾는 방법은 전문검색(FULLTEXT)이다. 한글은 띄어쓰기로 단어가 갈리지 않아 ngram 파서로
글자 두 개씩 색인해 두었다. 뜻으로 찾는 임베딩을 함께 쓰면 더 정확하지만, 지금 키에
임베딩 모델 권한이 없어 낱말 검색만 쓴다. 대신 분류 단계에서 사용자의 말을 약관 낱말로
바꿔 넘겨받는다 — "잃어버렸어요"를 그대로 넣으면 한 건도 걸리지 않는다.
"""

from dataclasses import dataclass
from typing import List, Optional, Sequence

# 답변에 실을 조항 수. 늘리면 프롬프트만 길어지고 답이 흐려진다.
_MAX_PASSAGES = 3

# 이 점수에 못 미치면 못 찾은 것으로 본다.
#
# 하한이 없으면 무관한 조항이 0.04 점으로 걸려 나온다. 그 상태로 답을 만들면
# "카드 없애고 싶은데"에 분실 조항을 근거로 그럴듯한 문장이 나가는데, 사용자는
# 틀렸다는 것을 알 방법이 없다. 실측에서 맞는 조항은 5점을 넘었고 무관한 것은
# 0.1 미만이라 사이가 넓다.
_MIN_SCORE = 1.0

# 한 조항을 몇 글자까지 실을지. 조문 하나가 1,200자까지 가므로 셋을 다 실으면
# 프롬프트가 길어진다. 앞부분에 핵심이 오는 구조라 뒤를 자른다.
_MAX_PASSAGE_CHARS = 700

# 카드사가 달라도 개인회원 표준약관은 내용이 사실상 같다. 상위 세 건이 3사의 같은
# 조문으로 채워지면 LLM 에게 같은 글을 세 번 주는 셈이라, 카드사마다 하나만 남긴다.
_SEARCH_SQL = """
SELECT d.card_company_id,
       COALESCE(cc.company_name, d.issuer) AS company_name,
       d.source_card_name,
       c.heading,
       c.content,
       MATCH(c.content) AGAINST (%s IN NATURAL LANGUAGE MODE) AS score
FROM card_term_chunk c
JOIN card_term_document d ON d.card_term_document_id = c.card_term_document_id
LEFT JOIN card_company cc ON cc.card_company_id = d.card_company_id
WHERE MATCH(c.content) AGAINST (%s IN NATURAL LANGUAGE MODE)
ORDER BY score DESC
LIMIT 30
"""


@dataclass(frozen=True)
class Passage:
    company_name: str
    document_name: str
    heading: Optional[str]
    content: str

    def label(self) -> str:
        """근거 표시용 이름. 어느 카드사 무슨 약관의 몇 조인지 밝힌다."""
        parts = [self.company_name, self.document_name]
        if self.heading:
            parts.append(self.heading)
        return " / ".join(parts)


def search(
    conn, query: str, company_ids: Optional[Sequence[int]] = None
) -> List[Passage]:
    """약관 조각을 찾는다. 쓸 만한 것이 없으면 빈 목록."""
    if not query or not query.strip():
        return []

    with conn.cursor() as cursor:
        cursor.execute(_SEARCH_SQL, (query, query))
        rows = cursor.fetchall()

    return _pick(rows, company_ids)


def _pick(rows, company_ids: Optional[Sequence[int]]) -> List[Passage]:
    """점수·카드사 기준으로 실을 것만 고른다."""
    allowed = set(company_ids) if company_ids else None
    seen_companies = set()
    picked: List[Passage] = []

    for row in rows:
        if float(row["score"]) < _MIN_SCORE:
            # 점수 내림차순이라 하나가 걸리면 뒤는 볼 것도 없다.
            break

        company_id = row["card_company_id"]
        if allowed is not None and company_id not in allowed:
            continue
        if company_id in seen_companies:
            continue

        seen_companies.add(company_id)
        picked.append(
            Passage(
                company_name=row["company_name"],
                document_name=row["source_card_name"],
                heading=row["heading"],
                content=_shorten(row["content"]),
            )
        )
        if len(picked) >= _MAX_PASSAGES:
            break

    return picked


def _shorten(content: str) -> str:
    if len(content) <= _MAX_PASSAGE_CHARS:
        return content
    # 자른 사실을 남긴다. 조용히 줄이면 그 조문이 거기서 끝나는 것처럼 읽힌다.
    return content[:_MAX_PASSAGE_CHARS].rstrip() + " …(이하 생략)"


def company_id_of_card(conn, card_id: int) -> Optional[int]:
    """카드가 속한 카드사.

    질문이 카드를 짚었을 때 그 카드사 약관으로 좁히는 데 쓴다. 좁히지 않으면
    "삼성카드 잃어버리면?"에 세 카드사 약관이 함께 실려, 정작 삼성 조항이
    맞는지 사용자가 가릴 수 없다.

    회원의 보유 카드로 좁히지는 않는다. 이 서버는 토큰을 열지 않아 회원이 누구인지
    모르고, 알아내려면 검증이 두 벌이 된다.
    """
    with conn.cursor() as cursor:
        cursor.execute(
            "SELECT card_company_id FROM card WHERE card_id = %s", (card_id,)
        )
        row = cursor.fetchone()
        return row["card_company_id"] if row else None
