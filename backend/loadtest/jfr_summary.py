"""
JFR 기록에서 요청 처리 스레드의 CPU가 어디에 쓰였는지 묶어 본다.

처리량이 CPU에 막혔을 때 "무엇을 줄여야 하는가"를 정하는 근거다. 응답시간만 보면 DB가 느린지
앱이 느린지까지만 알 수 있고, 앱 안에서 드라이버·매퍼·계산 중 어디가 무거운지는 보이지 않는다.

부하를 거는 동안 톰캣 컨테이너에서 기록하고, JSON 으로 풀어 이 스크립트에 넘긴다.

    docker exec <컨테이너> jcmd 1 JFR.start settings=profile duration=20s filename=/tmp/run.jfr
    docker exec <컨테이너> jfr print --json --stack-depth 64 --events jdk.ExecutionSample /tmp/run.jfr > run.json
    python jfr_summary.py run.json

--stack-depth 를 빼면 스택이 위 5칸만 나온다. JDK 내부에서 찍힌 표본은 그 5칸에 부른 쪽이 없어
전부 '기타'로 떨어진다.

실행 표본(ExecutionSample)은 자바 코드를 실행 중인 스레드만 찍는다. 소켓 응답을 기다리는 시간은
들어가지 않으므로 여기 숫자는 "기다린 시간"이 아니라 "CPU를 쓴 시간"의 분포다.

표본 하나는 한 곳에만 넣는다. 스택 맨 위(지금 실행 중인 곳)에서 아래로 내려가며 처음 만나는
라이브러리·모듈의 몫으로 친다. JDK 내부 메서드는 그것을 부른 쪽에 넣는다 — 문자열을 만드는
시간은 String 이 아니라 그 문자열을 만들게 한 드라이버의 몫이다.
"""
from __future__ import annotations

import argparse
import json
from collections import Counter

# 위에서부터 먼저 맞는 것이 이긴다. 행 객체의 setter 는 매퍼가 결과를 채우며 부르므로 매퍼 몫이다.
# TLS 는 JDK 내부지만 따로 센다 — 드라이버가 DB 응답을 읽을 때 복호화에 쓰는 CPU 라, 드라이버 몫에
# 넣으면 "행을 읽는 비용"과 "연결을 암호화한 비용"이 섞여 무엇을 줄일지 가를 수 없다
CATEGORIES = [
    ("sun.security.ssl.", "DB 연결 암호화 (TLS)"),
    ("com.mysql.cj.", "MySQL 드라이버 (응답 읽기·값 변환)"),
    ("com.wallet.engine.dao.dto.", "MyBatis (결과 매핑·실행)"),
    ("org.apache.ibatis.", "MyBatis (결과 매핑·실행)"),
    ("org.mybatis.", "MyBatis (결과 매핑·실행)"),
    ("com.wallet.engine.calculator.", "혜택 계산"),
    ("com.wallet.engine.model.", "혜택 계산"),
    ("com.wallet.engine.assembler.", "조립 (행 → 계산 입력)"),
    ("com.wallet.engine.", "엔진 서비스 (묶기·정렬·응답 조립)"),
    ("com.wallet.", "인증 등 엔진 밖 코드"),
    ("com.zaxxer.hikari.", "커넥션 풀"),
    ("com.fasterxml.jackson.", "JSON 변환"),
    ("io.jsonwebtoken.", "인증 등 엔진 밖 코드"),
    ("org.springframework.", "Spring (요청 분배·트랜잭션)"),
    ("org.apache.", "톰캣"),
]
OTHER = "기타"


def category_of(frames: list[dict]) -> str:
    for frame in frames:
        class_name = ((frame.get("method") or {}).get("type") or {}).get("name") or ""
        class_name = class_name.replace("/", ".")
        for prefix, label in CATEGORIES:
            if class_name.startswith(prefix):
                return label
    return OTHER


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("path", help="jfr print --json 출력 파일")
    parser.add_argument("--thread-prefix", default="http-nio-",
                        help="요청 처리 스레드 이름의 앞부분. 다른 스레드(GC·스케줄러)는 뺀다")
    parser.add_argument("--label", default="")
    parser.add_argument("--out", default=None, help="결과를 JSON 한 줄로 덧붙일 파일")
    args = parser.parse_args()

    with open(args.path, encoding="utf-8") as file:
        events = json.load(file)["recording"]["events"]

    counts: Counter[str] = Counter()
    for event in events:
        values = event.get("values") or {}
        thread_name = (values.get("sampledThread") or {}).get("javaName") or ""
        if not thread_name.startswith(args.thread_prefix):
            continue
        counts[category_of((values.get("stackTrace") or {}).get("frames") or [])] += 1

    total = sum(counts.values())
    shares = {label: round(count / total * 100, 1) for label, count in counts.most_common()} if total else {}
    print(f"{args.label or args.path}: 요청 스레드 표본 {total}개")
    for label, share in shares.items():
        print(f"  {share:5.1f}%  {label}")
    if args.out:
        with open(args.out, "a", encoding="utf-8") as file:
            file.write(json.dumps({"label": args.label, "samples": total, "sharePercent": shares},
                                  ensure_ascii=False) + "\n")


if __name__ == "__main__":
    main()
