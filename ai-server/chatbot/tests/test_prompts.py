"""프롬프트에 적어 둔 값이 DB 와 어긋나지 않는지.

업종 목록은 분류 프롬프트에 직접 적혀 있다. 카테고리 표준이 바뀌었는데 프롬프트를
안 고치면, 모델이 DB 에 없는 이름을 내놓고 해석이 조용히 실패한다.
문서와 데이터가 갈리는 자리라 사람이 기억하지 말고 테스트가 잡게 한다.
"""

import re
from pathlib import Path

import pytest

from chatbot.db import connection

_PROMPT_PATH = Path(__file__).resolve().parent.parent / "prompts" / "classify.md"


def _prompt_categories() -> set:
    text = _PROMPT_PATH.read_text(encoding="utf-8")
    block = re.search(r"### 업종 목록.*?```\n(.*?)```", text, re.S)
    assert block, "분류 프롬프트에서 업종 목록 블록을 찾지 못했다"
    return {name.strip() for line in block.group(1).splitlines() for name in line.split("·") if name.strip()}


def test_프롬프트의_업종_목록이_카테고리_표준과_같다():
    try:
        with connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT category_name FROM category")
                in_db = {row["category_name"] for row in cursor.fetchall()}
    except Exception as error:
        pytest.skip(f"MySQL 연결 실패: {error}")

    in_prompt = _prompt_categories()

    assert in_prompt - in_db == set(), f"프롬프트에만 있는 업종: {in_prompt - in_db}"
    assert in_db - in_prompt == set(), f"프롬프트에서 빠진 업종: {in_db - in_prompt}"
