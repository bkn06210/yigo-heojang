"""임베딩 어댑터 테스트.

지키려는 것은 셋이다.
  ① 같은 글에는 항상 같은 값 — 흔들리면 검색 결과가 재현되지 않는다
  ② 좌표계가 다른 값은 섞이지 않는다 — 차원이 다르면 견주지 않고, 이름으로 구분된다
  ③ 저장했다 꺼내도 값이 그대로다 — 조용히 뭉개지면 검색이 서서히 어긋난다
"""

import pytest

from embedding import (
    PROVIDER_STUB,
    EmbeddingSettings,
    StubEmbeddingClient,
    cosine_similarity,
    create_embedding_client,
    normalize,
    pack_vector,
    unpack_vector,
)


class TestStub:

    def test_같은_글에는_같은_값이_나온다(self):
        client = StubEmbeddingClient(dimension=256)

        first = client.embed_query("카드를 잃어버렸어요")
        second = client.embed_query("카드를 잃어버렸어요")

        assert first == second

    def test_질문과_문서를_같은_방식으로_만든다(self):
        """뜻을 읽는 모델이 아니라 역할을 구분할 근거가 없다.
        다르게 만들면 같은 글을 넣어도 자기 자신을 못 찾는다."""
        client = StubEmbeddingClient(dimension=256)

        assert client.embed_query("연회비 반환") == client.embed_documents(["연회비 반환"])[0]

    def test_설정한_차원으로_만든다(self):
        assert len(StubEmbeddingClient(dimension=512).embed_query("분실 신고")) == 512

    def test_입력_순서와_결과_순서가_같다(self):
        client = StubEmbeddingClient(dimension=128)
        texts = ["연회비", "분실 신고", "할부 수수료"]

        vectors = client.embed_documents(texts)

        assert vectors == [client.embed_query(text) for text in texts]

    def test_글자가_겹치는_글끼리_더_가깝다(self):
        client = StubEmbeddingClient(dimension=512)
        query = client.embed_query("연회비 반환 기준")

        related = cosine_similarity(query, client.embed_query("연회비 반환 기준은 다음과 같습니다"))
        unrelated = cosine_similarity(query, client.embed_query("해외 이용 시 적용되는 환율"))

        assert related > unrelated

    def test_빈_글도_터지지_않는다(self):
        """조각 본문이 비는 일은 없어야 하지만, 색인 도중 한 건에 터지면 나머지가 함께 멈춘다."""
        vector = StubEmbeddingClient(dimension=64).embed_query("")

        assert len(vector) == 64
        assert all(value == 0.0 for value in vector)


class TestModelTag:
    """저장되는 이름에 차원이 들어가야 한다.

    모델 이름만 저장하면 차원을 바꿔도 '이미 색인된 조각'으로 보여 다시 만들지 않고,
    길이가 다른 벡터끼리 유사도를 구하다 검색이 조용히 어긋난다.
    """

    def test_차원이_이름에_들어간다(self):
        assert StubEmbeddingClient(dimension=1024).model_tag == "stub-hash-1024"

    def test_차원이_다르면_다른_이름이_된다(self):
        assert (StubEmbeddingClient(dimension=512).model_tag
                != StubEmbeddingClient(dimension=1024).model_tag)

    def test_저장_칸에_들어가는_길이다(self):
        """card_term_chunk.embedding_model 은 VARCHAR(50)."""
        assert len(StubEmbeddingClient(dimension=2048).model_tag) <= 50


class TestVector:

    def test_저장했다_꺼내도_값이_그대로다(self):
        vector = normalize([0.5, -0.25, 0.125, 2.0])

        restored = unpack_vector(pack_vector(vector))

        assert restored == pytest.approx(vector, rel=1e-6)

    def test_길이가_다르면_견주지_않는다(self):
        """앞을 잘라 맞추면 좌표계가 다른 값끼리 그럴듯한 점수가 나와 알아채기 어렵다."""
        assert cosine_similarity([1.0, 0.0], [1.0, 0.0, 0.0]) == 0.0

    def test_같은_벡터의_유사도는_1이다(self):
        vector = normalize([0.3, 0.4, 0.5])

        assert cosine_similarity(vector, vector) == pytest.approx(1.0)

    def test_길이가_0인_벡터는_그대로_둔다(self):
        assert normalize([0.0, 0.0]) == [0.0, 0.0]
        assert cosine_similarity([0.0, 0.0], [1.0, 1.0]) == 0.0


class TestFactory:
    """어느 구현을 쓸지는 설정 한 곳에서만 정한다."""

    def test_stub_설정이면_가짜를_준다(self):
        client = create_embedding_client(_settings(provider=PROVIDER_STUB))

        assert client.provider == "stub"
        assert client.dimension == 256

    def test_모르는_공급자는_거절한다(self):
        with pytest.raises(ValueError):
            create_embedding_client(_settings(provider="gemini"))

    def test_키_없이_실제_공급자를_켜면_바로_알려준다(self):
        """조각 수천 개를 돌리다 중간에 알게 되면 되돌리기 번거롭다."""
        with pytest.raises(ValueError):
            create_embedding_client(_settings(provider="voyage", api_key=""))


def _settings(provider: str, api_key: str = "") -> EmbeddingSettings:
    return EmbeddingSettings(
        provider=provider,
        model="voyage-4-lite",
        dimension=256,
        api_key=api_key,
        api_url="https://api.voyageai.com/v1/embeddings",
    )
