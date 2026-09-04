"""약관 색인 테스트 — 구현이 둘이어도 계약은 하나.

지키려는 것은 셋이다.
  ① 어느 구현이든 가까운 순으로 돌려주고 score 를 얹는다
  ② Qdrant 쪽은 본문을 MySQL 에서 읽고, MySQL 에 없는 id 는 조용히 뺀다
  ③ 설정으로 구현이 갈리고, 모르는 값은 거절한다

Qdrant 서버와 MySQL 은 가짜로 바꾼다. 확인하려는 것은 두 저장소를 잇는 규칙이지
저장소 자체가 아니다.
"""

import numpy as np
import pytest

from chatbot import term_index
from chatbot.term_index import NumpyTermIndex, QdrantTermIndex
from vectorstore import INDEX_NUMPY, INDEX_QDRANT, VectorStoreSettings


def _row(company, heading):
    return {"card_company_id": company, "company_name": f"카드사{company}",
            "source_card_name": "약관", "heading": heading, "content": heading + " 본문"}


class TestNumpy:

    def _index(self):
        # 서로 다른 방향 셋. 길이는 1로 맞춰 둔다 (실제 적재도 그렇게 한다).
        matrix = np.array([[1, 0, 0], [0, 1, 0], [0.6, 0.8, 0]], dtype=np.float32)
        rows = [_row(1, "가로"), _row(2, "세로"), _row(3, "대각")]
        return NumpyTermIndex(matrix=matrix, rows=rows, model_tag="stub-hash-3")

    def test_가까운_순으로_돌려준다(self):
        ranked = self._index().rank(None, [1, 0, 0], limit=3)

        assert [r["heading"] for r in ranked] == ["가로", "대각", "세로"]
        assert ranked[0]["score"] == pytest.approx(1.0)

    def test_limit_만큼만_돌려준다(self):
        assert len(self._index().rank(None, [1, 0, 0], limit=2)) == 2

    def test_길이_0_질문은_빈_결과(self):
        assert self._index().rank(None, [0, 0, 0], limit=3) == []

    def test_비어_있으면_빈_결과(self):
        empty = NumpyTermIndex(matrix=np.empty((0, 0), dtype=np.float32), rows=[],
                               model_tag="stub-hash-3")
        assert empty.rank(None, [1, 0, 0], limit=3) == []


class FakeConn:
    """id 로 본문을 돌려주는 MySQL 흉내."""

    def __init__(self, rows_by_id):
        self._rows = rows_by_id

    def cursor(self):
        return _FakeCursor(self._rows)


class _FakeCursor:
    def __init__(self, rows_by_id):
        self._rows = rows_by_id
        self._out = []

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def execute(self, _sql, params):
        ids = params[0]
        self._out = [dict(self._rows[i], card_term_chunk_id=i) for i in ids if i in self._rows]

    def fetchall(self):
        return self._out


class TestQdrant:

    @pytest.fixture
    def hits(self, monkeypatch):
        """Qdrant 가 돌려줄 (id, score). 테스트마다 바꿔 끼운다."""
        holder = {"hits": []}
        monkeypatch.setattr("vectorstore.qdrant.search",
                            lambda client, name, vector, limit: holder["hits"][:limit])
        return holder

    def _index(self, available=True):
        return QdrantTermIndex(client=object(), collection="card_term__stub-hash-3",
                               model_tag="stub-hash-3", available=available)

    def test_Qdrant_순서를_지키고_본문을_MySQL_에서_읽는다(self, hits):
        hits["hits"] = [(20, 0.9), (10, 0.7)]
        conn = FakeConn({10: _row(1, "십"), 20: _row(2, "이십")})

        ranked = self._index().rank(conn, [1, 0, 0], limit=5)

        assert [r["heading"] for r in ranked] == ["이십", "십"]
        assert [r["score"] for r in ranked] == [0.9, 0.7]
        assert ranked[0]["content"] == "이십 본문"

    def test_MySQL_에_없는_id_는_뺀다(self, hits):
        """조각을 다시 잘라 id 가 바뀌었는데 Qdrant 를 아직 동기화하지 않은 경우다."""
        hits["hits"] = [(99, 0.95), (10, 0.7)]
        conn = FakeConn({10: _row(1, "십")})

        ranked = self._index().rank(conn, [1, 0, 0], limit=5)

        assert [r["heading"] for r in ranked] == ["십"]

    def test_컬렉션이_없으면_빈_결과(self, hits):
        hits["hits"] = [(10, 0.9)]

        assert self._index(available=False).rank(FakeConn({}), [1, 0, 0], limit=5) == []

    def test_Qdrant_가_비면_MySQL_을_읽지_않는다(self, hits):
        hits["hits"] = []

        class Exploding:
            def cursor(self):
                raise AssertionError("MySQL 을 읽으면 안 된다")

        assert self._index().rank(Exploding(), [1, 0, 0], limit=5) == []


class TestFactory:

    @pytest.fixture(autouse=True)
    def fresh(self):
        term_index.reset()
        yield
        term_index.reset()

    def _settings(self, monkeypatch, index):
        monkeypatch.setattr(
            "chatbot.term_index.get_vectorstore_settings",
            lambda: VectorStoreSettings(term_index=index, qdrant_url="http://x:6333"),
        )

    def test_모르는_색인은_거절한다(self, monkeypatch):
        self._settings(monkeypatch, "pinecone")

        with pytest.raises(ValueError):
            term_index.get_index(FakeConn({}))

    def test_qdrant_설정이면_Qdrant_구현을_만든다(self, monkeypatch):
        self._settings(monkeypatch, INDEX_QDRANT)
        monkeypatch.setattr("vectorstore.qdrant.connect", lambda url: object())
        monkeypatch.setattr("vectorstore.qdrant.has_collection", lambda client, name: True)

        index = term_index.get_index(FakeConn({}))

        assert isinstance(index, QdrantTermIndex)
        assert index.available

    def test_컬렉션이_없어도_기동은_된다(self, monkeypatch):
        """동기화 전이라도 서버가 죽지 않고 '못 찾았다' 로 답해야 한다."""
        self._settings(monkeypatch, INDEX_QDRANT)
        monkeypatch.setattr("vectorstore.qdrant.connect", lambda url: object())
        monkeypatch.setattr("vectorstore.qdrant.has_collection", lambda client, name: False)

        index = term_index.get_index(FakeConn({}))

        assert isinstance(index, QdrantTermIndex)
        assert not index.available

    def test_한_번_만들면_다시_만들지_않는다(self, monkeypatch):
        self._settings(monkeypatch, INDEX_QDRANT)
        calls = []
        monkeypatch.setattr("vectorstore.qdrant.connect", lambda url: calls.append(url) or object())
        monkeypatch.setattr("vectorstore.qdrant.has_collection", lambda client, name: True)

        first = term_index.get_index(FakeConn({}))
        second = term_index.get_index(FakeConn({}))

        assert first is second
        assert len(calls) == 1
