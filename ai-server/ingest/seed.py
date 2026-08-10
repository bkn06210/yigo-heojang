"""구조화 결과를 카드·혜택 시드 SQL로 만든다.

원문(card_term_document)은 DB에 직접 넣지만 이쪽은 SQL 파일로 뽑는다.
혜택 데이터는 팀이 함께 쓰는 값이라 파일로 있어야 각자 적용할 수 있고,
크기도 원문과 달리 저장소에 넣을 만하다.

ID를 코드가 정해서 박는다. AUTO_INCREMENT에 맡기면 benefit이 어느 카드의
몇 번 구간을 가리키는지 SQL 안에서 이을 수 없다.
"""

import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional

from .load import connect

_STRUCTURED_DIR = Path(__file__).resolve().parent / "out" / "structured"
_MERCHANTS_PATH = Path(__file__).resolve().parent / "out" / "merchants.json"
_OUTPUT_PATH = (
    Path(__file__).resolve().parent.parent.parent / "backend" / "db" / "91_seed_card_benefit.sql"
)

# 엔진이 판정할 수 없는 제외 유형은 시드에 넣지 않는다.
# MERCHANT_LOCATION("백화점 입점 매장 제외")은 가맹점이 브랜드 단위라 지점을 구분하지 못한다.
# 넣으면 엔진이 모르는 값이라 혜택 조회가 통째로 실패한다 — 값 하나가 API를 죽인다.
# 표준값 정의는 스키마에 남아 있고, 마이데이터로 지점명이 들어오면 그때 판정 경로가 생긴다.
_UNJUDGEABLE_EXCLUSION_TYPES = {"MERCHANT_LOCATION"}


def sql_value(value) -> str:
    """SQL 리터럴. 문자열의 작은따옴표와 역슬래시를 이스케이프한다."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "'Y'" if value else "'N'"
    if isinstance(value, (int, float)):
        return str(value)
    escaped = str(value).replace("\\", "\\\\").replace("'", "''")
    return f"'{escaped}'"


def load_category_ids() -> Dict[str, int]:
    """카테고리는 이미 팀 표준으로 DB에 있으므로 코드→ID만 읽어온다."""
    conn = connect()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT category_code, category_id FROM category")
            return {code: cid for code, cid in cursor.fetchall()}
    finally:
        conn.close()


def read_cards() -> List[dict]:
    """구조화 결과를 카드 단위로 읽는다.

    카드명은 파일명(수집 목록 기준)을 쓴다. 약관 본문의 표기와 다를 수 있는데,
    card_term_document.source_card_name이 수집 목록 이름이라 그쪽에 맞춰야
    나중에 원문과 카드를 이을 수 있다.
    """
    cards = []
    for path in sorted(_STRUCTURED_DIR.glob("*.json")):
        issuer, card_name = path.stem.split("_", 1)
        data = json.loads(path.read_text(encoding="utf-8"))
        cards.append({"issuer": issuer, "card_name": card_name, "data": data})
    return cards


def representative_fee(card: dict) -> int:
    """card.annual_fee는 목록 화면용 대표값이다.

    약관이 조합별 금액만 주고 대표값을 정하지 않은 경우가 많아, 그때는
    가장 싼 조합을 쓴다. 임의로 비싼 쪽을 고르면 카드 비교에서 불리해진다.
    """
    info = card["data"].get("card") or {}
    if info.get("annual_fee") is not None:
        return int(info["annual_fee"])
    fees = [f.get("total_fee") for f in (info.get("annual_fees") or []) if f.get("total_fee")]
    return min(fees) if fees else 0


def _slug(name: str) -> str:
    return re.sub(r"[^A-Z0-9_]", "", name.upper().replace(" ", "_"))[:30]


# 수집 어댑터가 쓰는 카드사 표기 → 카드사 마스터(ID, 코드, 정식 명칭, BIN).
# BIN은 카드번호 앞자리로 카드사를 판별하는 값이라 수집 대상이 아니고 여기에 고정으로 둔다.
# 카드사와 BIN이 같은 파일에 있어야 참조가 어긋나지 않는다.
CARD_COMPANIES = {
    "KB국민": (1, "KB_CARD", "KB국민카드", "222879"),
    "신한": (2, "SHINHAN_CARD", "신한카드", "356078"),
    "삼성": (3, "SAMSUNG_CARD", "삼성카드", "376293"),
}


def build() -> str:
    category_ids = load_category_ids()
    merchants = json.loads(_MERCHANTS_PATH.read_text(encoding="utf-8"))
    cards = read_cards()

    lines: List[str] = []
    warnings: List[str] = []

    lines.append("-- 카드 약관에서 추출한 카드·혜택 시드. ingest 파이프라인이 생성한다.")
    lines.append("-- 직접 고치지 말고 ingest/seed.py를 다시 돌린다.")
    lines.append("SET NAMES utf8mb4;")
    lines.append("")

    # ── 가맹점 ──────────────────────────────────────────────
    merchant_ids: Dict[str, int] = {}
    rows = []
    for index, m in enumerate(merchants, start=1):
        category_id = category_ids.get(m["category_code"])
        if category_id is None:
            warnings.append(f"카테고리 없음: {m['merchant_name']} → {m['category_code']}")
            continue
        merchant_ids[m["merchant_name"]] = index
        rows.append(
            f"    ({index}, {sql_value(m['merchant_code'])}, "
            f"{sql_value(m['merchant_name'])}, {category_id})"
        )
    lines.append("INSERT INTO merchant (merchant_id, merchant_code, merchant_name, category_id) VALUES")
    lines.append(",\n".join(rows) + ";")
    lines.append("")

    # ── 카드사 · BIN ────────────────────────────────────────
    companies = sorted(CARD_COMPANIES.values())
    lines.append(
        "INSERT INTO card_company (card_company_id, company_code, company_name, is_active) VALUES"
    )
    lines.append(",\n".join(
        f"    ({cid}, {sql_value(code)}, {sql_value(name)}, 'Y')"
        for cid, code, name, _ in companies
    ) + ";")
    lines.append("")
    lines.append(
        "INSERT INTO card_bin (card_company_id, bin_prefix, bin_length, is_active) VALUES"
    )
    lines.append(",\n".join(
        f"    ({cid}, {sql_value(prefix)}, {len(prefix)}, 'Y')"
        for cid, _, _, prefix in companies
    ) + ";")
    lines.append("")

    # ── 카드 ────────────────────────────────────────────────
    card_ids: Dict[str, int] = {}
    rows = []
    for index, card in enumerate(cards, start=1):
        company = CARD_COMPANIES.get(card["issuer"])
        if company is None:
            warnings.append(f"카드사 매칭 실패: {card['issuer']} ({card['card_name']})")
            continue
        card_ids[card["card_name"]] = index
        info = card["data"].get("card") or {}
        card_type = info.get("card_type") or "CREDIT"
        rows.append(
            f"    ({index}, {company[0]}, {sql_value(card['card_name'])}, "
            f"{sql_value(card_type)}, {representative_fee(card)}, NULL, NULL, 'Y')"
        )
    lines.append(
        "INSERT INTO card (card_id, card_company_id, card_name, card_type, annual_fee,"
        " image_url, description, is_active) VALUES"
    )
    lines.append(",\n".join(rows) + ";")
    lines.append("")

    # ── 연회비 (브랜드·발급형태별) ──────────────────────────
    rows = []
    for card in cards:
        card_id = card_ids[card["card_name"]]
        for fee in ((card["data"].get("card") or {}).get("annual_fees") or []):
            total = fee.get("total_fee")
            if total is None:
                base, partner = fee.get("base_fee"), fee.get("partner_fee")
                if base is None:
                    warnings.append(f"연회비 금액 없음: {card['card_name']} {fee.get('brand')}")
                    continue
                total = base + (partner or 0)
            # variant는 리워드 종류로 연회비가 갈리는 카드에만 값이 있다.
            # 브랜드·발급형태와 다른 축이라 비우면 같은 (카드, 브랜드)에 금액이 둘이 되어
            # UNIQUE 제약에 걸린다 — 적재가 통째로 실패한다.
            rows.append(
                f"    ({card_id}, {sql_value(fee.get('brand') or 'ANY')}, "
                f"{sql_value(fee.get('issue_type') or 'ANY')}, "
                f"{sql_value(fee.get('variant') or 'ANY')}, {total}, "
                f"{sql_value(fee.get('base_fee'))}, {sql_value(fee.get('partner_fee'))})"
            )
    if rows:
        lines.append(
            "INSERT INTO card_annual_fee "
            "(card_id, brand, issue_type, variant, total_fee, base_fee, partner_fee) VALUES"
        )
        lines.append(",\n".join(rows) + ";")
        lines.append("")

    # ── 실적구간 ────────────────────────────────────────────
    tier_ids: Dict[tuple, int] = {}
    rows = []
    tier_seq = 0
    for card in cards:
        card_id = card_ids[card["card_name"]]
        tiers = card["data"].get("performance_tiers") or []
        # 실적 판정은 항상 구간 하나를 반환해야 한다. 0원 구간이 없으면 판정이 비는 카드가 생긴다.
        for period in {(t.get("period_type") or "MONTH") for t in tiers} or {"MONTH"}:
            if not any(
                (t.get("period_type") or "MONTH") == period and t.get("min_performance_amount") == 0
                for t in tiers
            ):
                tiers = tiers + [{"period_type": period, "min_performance_amount": 0,
                                  "shared_monthly_limit": None}]
                warnings.append(f"0원 구간 보충: {card['card_name']} ({period})")
        for tier in sorted(tiers, key=lambda t: (t.get("period_type") or "MONTH",
                                                 t.get("min_performance_amount") or 0)):
            tier_seq += 1
            period = tier.get("period_type") or "MONTH"
            amount = tier.get("min_performance_amount") or 0
            tier_ids[(card_id, period, amount)] = tier_seq
            rows.append(
                f"    ({tier_seq}, {card_id}, {sql_value(period)}, {amount}, "
                f"{sql_value(tier.get('shared_monthly_limit'))})"
            )
    lines.append(
        "INSERT INTO performance_tier (tier_id, card_id, period_type,"
        " min_performance_amount, shared_monthly_limit) VALUES"
    )
    lines.append(",\n".join(rows) + ";")
    lines.append("")

    # ── 실적 제외 ───────────────────────────────────────────
    rows = []
    for card in cards:
        card_id = card_ids[card["card_name"]]
        seen = set()
        for exclusion in (card["data"].get("performance_exclusions") or []):
            key = (card_id, exclusion.get("exclusion_type"), exclusion.get("exclusion_value"))
            if None in key or key in seen:
                continue
            seen.add(key)
            rows.append(f"    ({card_id}, {sql_value(key[1])}, {sql_value(key[2])})")
    if rows:
        lines.append(
            "INSERT INTO performance_exclusion (card_id, exclusion_type, exclusion_value) VALUES"
        )
        lines.append(",\n".join(rows) + ";")
        lines.append("")

    # ── 카드 전체 혜택 제외 ─────────────────────────────────
    rows = []
    for card in cards:
        card_id = card_ids[card["card_name"]]
        seen = set()
        for exclusion in (card["data"].get("card_exclusions") or []):
            key = (card_id, exclusion.get("exclusion_type"), exclusion.get("exclusion_value"))
            if None in key or key in seen:
                continue
            if key[1] in _UNJUDGEABLE_EXCLUSION_TYPES:
                warnings.append(f"판정 불가 제외 제거: {card['card_name']} / {key[1]} {key[2]}")
                continue
            seen.add(key)
            rows.append(f"    ({card_id}, {sql_value(key[1])}, {sql_value(key[2])})")
    if rows:
        lines.append(
            "INSERT INTO card_benefit_exclusion (card_id, exclusion_type, exclusion_value) VALUES"
        )
        lines.append(",\n".join(rows) + ";")
        lines.append("")

    # ── 혜택 ────────────────────────────────────────────────
    benefit_rows, tier_limit_rows, exclusion_rows = [], [], []
    benefit_seq = 0
    for card in cards:
        card_id = card_ids[card["card_name"]]
        for benefit in (card["data"].get("benefits") or []):
            target_type = benefit.get("target_type")
            category_id = merchant_id = None

            if target_type == "CATEGORY":
                code = benefit.get("target_category_code")
                category_id = category_ids.get(code)
                if category_id is None:
                    warnings.append(f"카테고리 매칭 실패: {card['card_name']} / {code}")
                    continue
            elif target_type == "MERCHANT":
                name = (benefit.get("target_merchant_name") or "").strip()
                merchant_id = merchant_ids.get(name)
                if merchant_id is None:
                    warnings.append(f"가맹점 매칭 실패: {card['card_name']} / {name}")
                    continue
            elif target_type != "ALL":
                warnings.append(f"대상 유형 불명: {card['card_name']} / {target_type}")
                continue

            kind = benefit.get("benefit_kind")
            calc = benefit.get("calc_method")
            value = benefit.get("benefit_value")
            tier_limits = benefit.get("tier_limits") or []

            # 구간마다 값이 다른 혜택은 최상위 benefit_value가 비어 있고 tier_limits에만 값이 있다.
            # benefit_value는 NOT NULL이므로 가장 낮은 구간 값을 기본값으로 삼는다.
            # 각 구간은 tier_limits가 다시 덮어쓰므로 계산 결과는 달라지지 않는다.
            if value is None:
                tiered = [
                    (t.get("min_performance_amount") or 0, t.get("benefit_value"))
                    for t in tier_limits if t.get("benefit_value") is not None
                ]
                if tiered:
                    value = min(tiered)[1]

            # 계산에서 빠지는 종류는 금액이 없어도 행을 만든다 — 카드 상세 화면이 정보로 표시한다.
            if kind in ("GIFT", "INSTALLMENT_FREE", "RETROACTIVE"):
                calc = calc or "FIXED"
                value = 0 if value is None else value

            if calc is None or value is None:
                # 계산할 수 없는 행은 넣지 않는다. 넣으면 엔진이 0원으로 취급해
                # "혜택이 있는데 늘 0원인 카드"가 조용히 생긴다.
                warnings.append(f"계산값 없음(제외): {card['card_name']} / {benefit.get('benefit_name')}")
                continue

            benefit_seq += 1

            benefit_rows.append(
                "    ({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},"
                " {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})".format(
                    benefit_seq, card_id,
                    sql_value(benefit.get("benefit_name")),
                    sql_value(kind), sql_value(calc), value,
                    sql_value(benefit.get("step_count")),
                    sql_value(benefit.get("apply_timing")),
                    sql_value(target_type),
                    sql_value(category_id), sql_value(merchant_id),
                    sql_value(benefit.get("require_performance") or "Y"),
                    sql_value(benefit.get("performance_period") or "MONTH"),
                    sql_value(benefit.get("require_payment_type")),
                    sql_value(benefit.get("min_txn_amount")),
                    sql_value(benefit.get("max_eligible_amount")),
                    sql_value(benefit.get("max_benefit_per_txn")),
                    sql_value(benefit.get("monthly_limit")),
                    sql_value(benefit.get("limit_group_code")),
                    sql_value(benefit.get("monthly_count_limit")),
                    sql_value(benefit.get("daily_count_limit")),
                    sql_value(benefit.get("yearly_count_limit")),
                    sql_value(benefit.get("quarterly_count_limit")),
                    sql_value(benefit.get("quarterly_limit")),
                    sql_value(benefit.get("yearly_limit")),
                    sql_value(benefit.get("count_group_code")),
                    sql_value(benefit.get("daily_limit")),
                    sql_value(benefit.get("use_shared_limit") or "N"),
                    sql_value(benefit.get("exclude_from_performance") or "N"),
                    # 선택형 혜택(매월 택1)은 이 두 값이 있어야 그달에 고른 선택지만 적용된다.
                    # 비우면 엔진이 "선택형이 아니다"로 보고 묶음의 모든 선택지를 동시에 켜서,
                    # 고르지도 않은 혜택이 추천 계산과 카드 상세 화면에 함께 나온다.
                    sql_value(benefit.get("option_group_code")),
                    sql_value(benefit.get("option_key")),
                )
            )

            period = benefit.get("performance_period") or "MONTH"
            for tier_limit in tier_limits:
                amount = tier_limit.get("min_performance_amount")
                tier_id = tier_ids.get((card_id, period, amount))
                if tier_id is None:
                    warnings.append(
                        f"구간 매칭 실패: {card['card_name']} / {period} {amount}"
                    )
                    continue
                tier_limit_rows.append(
                    f"    ({benefit_seq}, {tier_id}, "
                    f"{sql_value(tier_limit.get('monthly_limit'))}, "
                    f"{sql_value(tier_limit.get('quarterly_limit'))}, "
                    f"{sql_value(tier_limit.get('yearly_limit'))}, "
                    f"{sql_value(tier_limit.get('benefit_value'))})"
                )

            seen = set()
            for exclusion in (benefit.get("exclusions") or []):
                key = (exclusion.get("exclusion_type"), exclusion.get("exclusion_value"))
                if None in key or key in seen:
                    continue
                if key[0] in _UNJUDGEABLE_EXCLUSION_TYPES:
                    warnings.append(
                        f"판정 불가 제외 제거: {card['card_name']} / {benefit.get('benefit_name')}"
                        f" / {key[0]} {key[1]}")
                    continue
                seen.add(key)
                exclusion_rows.append(
                    f"    ({benefit_seq}, {sql_value(key[0])}, {sql_value(key[1])})"
                )

    lines.append(
        "INSERT INTO benefit (benefit_id, card_id, benefit_name, benefit_kind, calc_method,"
        " benefit_value, step_count, apply_timing, target_type, target_category_id,"
        " target_merchant_id, require_performance, performance_period, require_payment_type,"
        " min_txn_amount, max_eligible_amount, max_benefit_per_txn, monthly_limit,"
        " limit_group_code, monthly_count_limit, daily_count_limit, yearly_count_limit,"
        " quarterly_count_limit, quarterly_limit, yearly_limit, count_group_code,"
        " daily_limit, use_shared_limit, exclude_from_performance,"
        " option_group_code, option_key) VALUES"
    )
    lines.append(",\n".join(benefit_rows) + ";")
    lines.append("")

    if tier_limit_rows:
        lines.append(
            "INSERT INTO benefit_tier_limit (benefit_id, tier_id, tier_monthly_limit,"
            " tier_quarterly_limit, tier_yearly_limit, tier_benefit_value) VALUES"
        )
        lines.append(",\n".join(tier_limit_rows) + ";")
        lines.append("")

    if exclusion_rows:
        lines.append(
            "INSERT INTO benefit_exclusion (benefit_id, exclusion_type, exclusion_value) VALUES"
        )
        lines.append(",\n".join(exclusion_rows) + ";")
        lines.append("")

    return "\n".join(lines), warnings, (category_ids, merchant_ids, card_ids, tier_ids, cards)


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    sql, warnings, _ = build()
    _OUTPUT_PATH.write_text(sql, encoding="utf-8")
    print(f"저장: {_OUTPUT_PATH}  ({len(sql):,}자)")
    if warnings:
        print("확인 필요:")
        for w in warnings[:20]:
            print(f"  - {w}")


if __name__ == "__main__":
    main()
