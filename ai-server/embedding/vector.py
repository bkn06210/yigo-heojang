"""벡터를 저장하고 견주는 함수들.

MySQL 8.0 에는 VECTOR 타입이 없어(9.0부터) 이진으로 담고 유사도는 앱이 계산한다.
조각이 1,550개 규모라 전수 계산이 밀리초로 끝나므로 벡터DB 없이도 검색이 성립한다.
"""

import struct
from typing import List, Sequence


def pack_vector(vector: Sequence[float]) -> bytes:
    """숫자 배열을 float32 이진으로 담는다.

    JSON 문자열로 두면 '0.0123456' 처럼 숫자 하나가 열 바이트 남짓을 먹어
    같은 값이 두 배 넘는 자리를 차지한다. 카드가 늘어 조각이 수만 개가 되면
    그 차이가 백 메가 단위로 벌어진다.
    """
    return struct.pack(f"<{len(vector)}f", *vector)


def unpack_vector(blob: bytes) -> List[float]:
    """저장한 이진을 숫자 배열로 되돌린다."""
    return list(struct.unpack(f"<{len(blob) // 4}f", blob))


def cosine_similarity(left: Sequence[float], right: Sequence[float]) -> float:
    """두 벡터가 얼마나 같은 방향인지. 1에 가까울수록 뜻이 가깝다.

    길이가 다르면 0을 준다. 서로 다른 모델이나 차원으로 만든 값이 섞인 경우인데,
    앞을 잘라 맞추면 좌표계가 다른 값끼리 그럴듯한 점수가 나와 알아채기 어렵다.
    """
    if len(left) != len(right) or not left:
        return 0.0

    dot = 0.0
    left_norm = 0.0
    right_norm = 0.0
    for a, b in zip(left, right):
        dot += a * b
        left_norm += a * a
        right_norm += b * b

    if left_norm <= 0.0 or right_norm <= 0.0:
        return 0.0
    return dot / ((left_norm ** 0.5) * (right_norm ** 0.5))


def normalize(vector: Sequence[float]) -> List[float]:
    """길이를 1로 맞춘다. 길이가 0이면 그대로 둔다(나눌 수 없다)."""
    norm = sum(value * value for value in vector) ** 0.5
    if norm <= 0.0:
        return list(vector)
    return [value / norm for value in vector]
