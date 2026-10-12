"""
두 서버(개선 전·후)에 같은 요청을 보내 응답 본문이 완전히 같은지 대조한다.

성능 개선은 계산 결과를 바꾸면 안 된다. 조회 방식만 바꿨더라도 행 순서·조인 조건이 달라지면
금액이나 순위가 조용히 바뀔 수 있어, 입력 조합을 넓게 훑어 응답의 data 를 그대로 비교한다.

    python compare_responses.py --before http://127.0.0.1:8080 --after http://127.0.0.1:8081

다루는 입력: 측정 회원(카드 2·10·20장) × [가맹점 전부 × 금액 3종 × 결제수단 유무,
업종 전부, 장소 미정] 추천 + 전체·카드별 현황(기준월과 지난 두 달).
"""
from __future__ import annotations

import argparse
import asyncio
import json

import httpx

MEMBERS = ["lt-c02@loadtest.local", "lt-c10@loadtest.local", "lt-c20@loadtest.local"]
PASSWORD = "LoadTest!2026"
AMOUNTS = [3_000, 15_000, 120_000]
PAYMENT_TYPES = [None, "KB_PAY"]


def login(base_url: str, email: str) -> str:
    response = httpx.post(f"{base_url}/api/auth/login", json={"email": email, "password": PASSWORD}, timeout=10)
    response.raise_for_status()
    return response.json()["data"]["accessToken"]


def recommend_bodies(merchant_count: int, category_ids: list[int]) -> list[dict]:
    bodies = []
    for payment_type in PAYMENT_TYPES:
        for merchant_id in range(1, merchant_count + 1):
            for amount in AMOUNTS:
                bodies.append({"merchantId": merchant_id, "expectedAmount": amount, "paymentType": payment_type})
        for category_id in category_ids:
            bodies.append({"categoryId": category_id, "expectedAmount": 15_000, "paymentType": payment_type})
        bodies.append({"expectedAmount": 15_000, "paymentType": payment_type})
    return bodies


async def fetch(client: httpx.AsyncClient, method: str, path: str, body: dict | None) -> tuple[int, object]:
    response = await client.request(method, path, json=body)
    payload = response.json()
    return response.status_code, payload.get("data") if payload.get("success") else payload


async def compare_member(args: argparse.Namespace, email: str, category_ids: list[int],
                         months: list[str]) -> tuple[int, list[str]]:
    before_client = httpx.AsyncClient(base_url=args.before, timeout=30,
                                      headers={"Authorization": f"Bearer {login(args.before, email)}"})
    after_client = httpx.AsyncClient(base_url=args.after, timeout=30,
                                     headers={"Authorization": f"Bearer {login(args.after, email)}"})
    requests: list[tuple[str, str, dict | None]] = [
        ("POST", "/api/recommendations", body) for body in recommend_bodies(args.merchant_count, category_ids)]

    # 현황: 전체와 카드별, 기준월과 지난 두 달
    _, overview = await fetch(before_client, "GET", "/api/cards/monthly-status", None)
    user_card_ids = [card["userCardId"] for card in (overview or {}).get("cards", [])]
    for month in [None, *months]:
        query = "" if month is None else f"?yearMonth={month}"
        requests.append(("GET", f"/api/cards/monthly-status{query}", None))
        for user_card_id in user_card_ids:
            requests.append(("GET", f"/api/cards/{user_card_id}/monthly-status{query}", None))

    semaphore = asyncio.Semaphore(args.concurrency)
    mismatches: list[str] = []

    async def check(method: str, path: str, body: dict | None) -> None:
        async with semaphore:
            before = await fetch(before_client, method, path, body)
            after = await fetch(after_client, method, path, body)
        if before != after:
            mismatches.append(f"{email} {method} {path} {json.dumps(body, ensure_ascii=False)}\n"
                              f"  before={json.dumps(before, ensure_ascii=False)[:300]}\n"
                              f"  after ={json.dumps(after, ensure_ascii=False)[:300]}")

    try:
        await asyncio.gather(*(check(*request) for request in requests))
    finally:
        await before_client.aclose()
        await after_client.aclose()
    return len(requests), mismatches


async def main_async(args: argparse.Namespace) -> int:
    category_ids = [int(value) for value in args.category_ids.split(",")]
    months = args.months.split(",") if args.months else []
    total = 0
    all_mismatches: list[str] = []
    for email in MEMBERS:
        count, mismatches = await compare_member(args, email, category_ids, months)
        print(f"{email}: {count}건 비교, 불일치 {len(mismatches)}건")
        total += count
        all_mismatches.extend(mismatches)
    for mismatch in all_mismatches[:10]:
        print(mismatch)
    print(f"합계: {total}건 비교, 불일치 {len(all_mismatches)}건")
    return 1 if all_mismatches else 0


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--before", default="http://127.0.0.1:8080")
    parser.add_argument("--after", default="http://127.0.0.1:8081")
    parser.add_argument("--merchant-count", type=int, default=225)
    # 표준 업종 46개(대분류 7 + 중분류 39). 대분류로 물어도 하위 혜택이 매칭되는지까지 함께 본다
    parser.add_argument("--category-ids", default=(
        "1,2,3,4,5,6,7,101,102,103,104,105,201,202,203,204,205,206,207,301,302,303,304,305,306,"
        "307,308,401,402,403,404,405,406,407,501,502,503,504,505,506,601,602,603,701,702,703"))
    parser.add_argument("--months", default="", help="현황을 추가로 볼 연월. 예: 2026-08,2026-07")
    parser.add_argument("--concurrency", type=int, default=8)
    raise SystemExit(asyncio.run(main_async(parser.parse_args())))


if __name__ == "__main__":
    main()
