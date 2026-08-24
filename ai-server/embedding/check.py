"""임베딩 설정이 실제로 동작하는지 확인한다.

    python -m embedding.check

색인은 조각 수천 개를 도는 배치라, 설정이 틀린 채로 시작하면 한참 뒤에 알게 된다.
호출 한 번으로 먼저 확인하고 넘어가기 위한 도구다.

주소가 갈리는 것을 여기서 흡수한다. 키를 어디서 만들었는지에 따라 주소가 다른데,
틀리면 인증 오류로 떨어져 "키가 잘못됐나"를 먼저 의심하게 된다. 설정한 주소가 막히면
알려진 다른 주소로 한 번 더 시도해 어느 쪽이 맞는지 알려준다.

키는 화면에 찍지 않는다. 확인하려다 캡처에 남으면 확인한 의미가 없다.
"""

import sys

from .config import PROVIDER_STUB, get_embedding_settings
from .voyage import EmbeddingApiError, VoyageEmbeddingClient

# 같은 API 를 서로 다른 주소로 연다. 키를 발급받은 곳에 따라 맞는 쪽이 갈린다.
_KNOWN_URLS = (
    "https://api.voyageai.com/v1/embeddings",
    "https://ai.mongodb.com/v1/embeddings",
)

_SAMPLE = "회원은 카드를 분실하거나 도난당한 경우 지체 없이 신고하여야 한다"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    settings = get_embedding_settings()

    print(f"공급자   {settings.provider}")
    print(f"모델     {settings.model}")
    print(f"차원     {settings.dimension}")

    if settings.provider == PROVIDER_STUB:
        print("\n개발용 설정이라 부를 곳이 없다. 글자만 보므로 뜻으로는 찾지 못한다.")
        print("실제로 쓰려면 .env 의 EMBEDDING_PROVIDER 를 바꾼다.")
        return

    print(f"주소     {settings.api_url}")
    print(f"키       {'있음' if settings.api_key else '없음'}")

    if not settings.api_key:
        raise SystemExit("\nVOYAGE_API_KEY 가 비어 있다. ai-server/.env 를 확인할 것")

    print()
    if _try(settings.api_url, settings):
        return

    # 설정한 주소가 막혔다. 다른 주소가 맞는지 확인해 무엇을 고쳐야 하는지 알려준다.
    for candidate in _KNOWN_URLS:
        if candidate == settings.api_url:
            continue
        print(f"\n다른 주소로 확인: {candidate}")
        if _try(candidate, settings):
            print(f"\n→ .env 의 EMBEDDING_API_URL 을 {candidate} 로 바꾼다")
            return

    raise SystemExit("\n어느 주소로도 응답을 받지 못했다. 키와 모델 이름을 확인할 것")


def _try(api_url: str, settings) -> bool:
    """한 건을 실제로 임베딩해 본다. 성공하면 True."""
    client = VoyageEmbeddingClient(
        settings.api_key, settings.model, settings.dimension, api_url
    )
    try:
        vector = client.embed_query(_SAMPLE)
    except EmbeddingApiError as error:
        print(f"  실패 {error.status_code} {_hint(error.status_code)}")
        print(f"       {error.detail}")
        return False
    except Exception as error:
        print(f"  실패 {type(error).__name__}: {error}")
        return False

    print(f"  성공 · 차원 {len(vector)} · 저장할 이름 {client.model_tag}")
    return True


def _hint(status_code: int) -> str:
    if status_code in (401, 403):
        return "— 키가 이 주소에서 통하지 않는다 (발급처와 주소가 다를 수 있다)"
    if status_code == 404:
        return "— 주소나 모델 이름이 맞지 않는다"
    if status_code == 429:
        return "— 분당 한도에 걸렸다"
    return ""


if __name__ == "__main__":
    main()
