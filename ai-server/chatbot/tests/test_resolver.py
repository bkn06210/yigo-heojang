"""표현 → 식별자 해석.

시드가 적재된 MySQL 이 필요하다. 별칭 조회·부분 일치 규칙이 전부 SQL 안에 있어
DB 없이 검증하면 정작 확인하려는 것을 못 본다. DB 가 없으면 건너뛴다.
"""

import pytest

from chatbot.db import connection
from chatbot.resolver import MATCH_ALIAS, MATCH_NAME, MATCH_PARTIAL, resolve_card, resolve_merchant


@pytest.fixture(scope="module")
def conn():
    try:
        with connection() as connected:
            yield connected
    except Exception as error:  # 접속 실패·시드 미적재
        pytest.skip(f"MySQL 연결 실패: {error}")


def test_정식_명칭은_그대로_찾는다(conn):
    result = resolve_merchant(conn, "스타벅스")

    assert result.match.name == "스타벅스"
    assert result.match.matched_by == MATCH_NAME
    assert result.match.detail == "카페"


def test_축약어는_별칭으로_찾는다(conn):
    # '스벅'과 '스타벅스'는 겹치는 글자가 '스' 하나뿐이라
    # 어떤 문자열 유사도로도 이어지지 않는다. 별칭 등록이 유일한 방법이다.
    result = resolve_merchant(conn, "스벅")

    assert result.match.name == "스타벅스"
    assert result.match.matched_by == MATCH_ALIAS


def test_조사가_붙어도_찾는다(conn):
    # 의도 분류가 조사를 떼어 주는 것이 정상 경로이고, 이건 그게 실패했을 때의 보험이다.
    assert resolve_merchant(conn, "스타벅스에서").match.name == "스타벅스"
    assert resolve_merchant(conn, "스벅에서").match.name == "스타벅스"


def test_정확_일치가_더_긴_이름을_이긴다(conn):
    # '이마트'는 '이마트24'의 앞부분이지만, 정확히 '이마트'라고 말하면 이마트여야 한다.
    result = resolve_merchant(conn, "이마트")

    assert result.match.name == "이마트"
    assert result.match.detail == "대형마트"


def test_입력된_글자가_긴_이름이면_그것을_고른다(conn):
    # 이마트24는 편의점, 이마트는 대형마트라 혜택이 전혀 다르다. 조용히 틀리면 안 된다.
    result = resolve_merchant(conn, "이마트24에서")

    assert result.match.name == "이마트24"
    assert result.match.detail == "편의점"


def test_짧은_이름이_딸려와도_입력된_이름을_고른다(conn):
    # 'KT'는 'SKT' 안에 우연히 들어 있어 후보에 든다. 다른 회사이므로 고르면 안 된다.
    assert resolve_merchant(conn, "SKT 요금 냈어").match.name == "SKT"
    assert resolve_merchant(conn, "KT 요금 냈어").match.name == "KT"
    assert resolve_merchant(conn, "마켓컬리에서").match.name == "마켓컬리"


def test_겹치지_않는_후보가_여럿이면_고르지_않는다(conn):
    result = resolve_merchant(conn, "스타벅스랑 이마트")

    assert not result.found
    assert set(result.candidates) == {"스타벅스", "이마트"}


def test_모호하거나_없는_표현은_못_찾은_것으로_둔다(conn):
    # '롯데'는 롯데마트·롯데백화점·롯데시네마 등에 걸린다. 지어내지 않고 되묻게 한다.
    assert not resolve_merchant(conn, "롯데").found
    assert not resolve_merchant(conn, "메가커피").found
    assert not resolve_merchant(conn, "").found


def test_카드도_같은_규칙으로_찾는다(conn):
    assert resolve_card(conn, "신한카드 핏(Fit)").match.matched_by == MATCH_NAME
    assert resolve_card(conn, "핏카드").match.name == "신한카드 핏(Fit)"
    assert resolve_card(conn, "신한 핏").match.detail == "신한카드"
    # 마이핏은 적립형·할인형 두 장이라 별칭을 등록하지 않았다.
    assert not resolve_card(conn, "마이핏").found


def test_별칭은_대소문자를_구분하지_않는다(conn):
    # 콜레이션이 utf8mb4_0900_ai_ci 라 정규화 코드를 따로 두지 않는다.
    assert resolve_card(conn, "id on").match.name == "삼성 iD ON 카드"


def test_부분_일치로_찾은_것도_표시가_남는다(conn):
    # 어느 경로로 찾았는지 남겨야 나중에 오매칭을 추적할 수 있다.
    assert resolve_merchant(conn, "스타벅스에서").match.matched_by == MATCH_PARTIAL
