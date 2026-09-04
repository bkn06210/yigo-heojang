"""약관 원문에서 질문과 관련된 조항을 찾는다.

금액·한도 같은 계산값은 엔진 API 에서 받지만 약관은 다르다. 계산 규칙이 아니라
텍스트라 두 곳에서 규칙이 갈릴 위험이 없고, 엔진이 내려줄 수 있는 모양도 아니다.
그래서 이것만 DB 를 직접 읽는다.

찾는 방법은 임베딩이다. 문장을 숫자 배열로 바꿔 두고 뜻이 가까운 것을 고른다.
낱말 검색(FULLTEXT)에서 옮겨온 이유는 점수가 "뜻이 맞나"가 아니라 "글자가 얼마나
겹치나"였기 때문이다. 실측에서 뜻이 정반대인 두 질문이 같은 조문을 거의 같은 점수로
집었다 — "카드를 잃어버렸어요" 22.46점, "카드 없애고 싶은데" 22.36점, 둘 다 분실 조항.
질문에 '카드'만 들어가면 '카드'가 많이 나오는 조각이 올라오는 구조라, 점수 하한으로
걸러지지도 않았다.

낱말 검색을 후보 추리기로 남기는 안도 재봤으나 버렸다. 질문 셋 중 둘에서 정답이
상위 30건 안에 아예 없어, 뒤에 무엇을 붙여도 찾을 수 없었다.
"""

from dataclasses import dataclass
from typing import List, Optional, Sequence

from embedding import create_embedding_client

from . import term_index

# 답변에 실을 조항 수. 늘리면 프롬프트만 길어지고 답이 흐려진다.
_MAX_PASSAGES = 3

# 이 값에 못 미치면 못 찾은 것으로 본다. 코사인 유사도라 -1 ~ 1 범위다.
#
# 하한이 없으면 어떤 질문에도 뭔가는 걸려 나온다. 그 상태로 답을 만들면 무관한 조항을
# 근거로 그럴듯한 문장이 나가는데, 사용자는 틀렸다는 것을 알 방법이 없다.
#
# 실측한 사이:
#   약관 질문(분실·연회비·해지·할부·한도)  0.422 ~ 0.560
#   무관한 질문(날씨·저녁 메뉴·코딩)       0.204 ~ 0.263
# 가운데를 잡아 양쪽으로 0.07 이상 여유를 둔다. 질문 아홉 개로 잰 값이라
# 확정이 아니라 출발점이다 — 오탐·누락이 보이면 이 값을 옮긴다.
_MIN_SCORE = 0.35

# 색인에서 몇 건을 받아 볼지. 카드사마다 하나만 남기고 세 건을 고르므로 넉넉해야 한다.
# 같은 조문이 카드사 수만큼 복사돼 있어 상위가 한 조문으로 채워지는 일이 흔하다.
_CANDIDATES = 30

# 한 조항을 몇 글자까지 실을지. 조문 하나가 1,200자까지 가므로 셋을 다 실으면
# 프롬프트가 길어진다. 앞부분에 핵심이 오는 구조라 뒤를 자른다.
_MAX_PASSAGE_CHARS = 700

# 카드사가 달라도 개인회원 표준약관은 내용이 사실상 같다. 상위 세 건이 3사의 같은
# 조문으로 채워지면 LLM 에게 같은 글을 세 번 주는 셈이라, 카드사마다 하나만 남긴다.


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

    # 문서를 색인할 때와 같은 모델로 질문을 바꿔야 거리 비교가 성립한다.
    query_vector = _client().embed_query(query)
    rows = term_index.get_index(conn).rank(conn, query_vector, _CANDIDATES)

    return _pick(rows, company_ids)


_embedding_client = None


def _client():
    """임베딩 어댑터. 한 번 만들어 두고 계속 쓴다."""
    global _embedding_client
    if _embedding_client is None:
        _embedding_client = create_embedding_client()
    return _embedding_client


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
