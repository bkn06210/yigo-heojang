# 챗봇 서버 이미지.
#
# MySQL·톰캣과 달리 가져다 쓸 이미지가 없어서 직접 만든다 — 우리가 짠 코드이기 때문이다.
#
# 수집 파이프라인(ingest)은 담지 않는다. 상시 떠 있는 서버가 아니라 카드를 추가할 때만
# 돌리는 배치라, 넣어봐야 이미지만 커지고 컨테이너 안에서 실행할 일이 없다.

FROM python:3.12-slim

WORKDIR /app

# 의존성을 먼저 설치한다. 코드만 고쳤을 때 이 층을 다시 만들지 않아 빌드가 빨라진다.
COPY chatbot/requirements.txt chatbot/requirements.txt
RUN pip install --no-cache-dir -r chatbot/requirements.txt

COPY chatbot/ chatbot/
# 약관 검색이 쓰는 임베딩 어댑터. 수집 파이프라인도 같은 것을 쓰므로 chatbot 밖에 있다.
COPY embedding/ embedding/

# 컨테이너 안에서는 0.0.0.0 으로 연다. 컨테이너마다 IP가 달라 127.0.0.1 로 묶으면
# 옆 컨테이너(톰캣)가 닿지 못한다. 밖으로 노출되지 않는 것은 compose 에서 포트를
# 매핑하지 않는 것으로 보장한다 — 열어두는 범위와 노출 여부는 다른 문제다.
EXPOSE 8000
CMD ["uvicorn", "chatbot.main:app", "--host", "0.0.0.0", "--port", "8000"]
