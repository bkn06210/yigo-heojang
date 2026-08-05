# 카드 혜택 챗봇 서버

카드·혜택 질문에 답하는 별도 프로세스다. Spring(WAR)과 같은 서버에 뜨지 않고
REST 로 통신한다.

## 호출 경로

```
프론트  →  Spring  →  챗봇 서버(FastAPI)  →  Spring 엔진 API
                                          →  MySQL (약관 원문)
```

**프론트가 이 서버를 직접 부르지 않는다.** Spring 의 JWT 필터가 검증을 끝낸 요청만
넘어오고, 이 서버는 토큰을 다시 열지 않는다. 검증 로직이 자바·파이썬 두 벌이 되면
서명 알고리즘이나 만료 처리가 한쪽만 바뀌어도 조용히 어긋난다.

그래서 기본 바인딩이 `127.0.0.1` 이다. 외부에 열리면 아무도 토큰을 확인하지 않은 채
`memberId` 만 바꿔 남의 소비내역을 물어볼 수 있다.

## 금액은 이 서버가 계산하지 않는다

할인액·실적 달성률·남은 한도는 **Spring 엔진 API 를 호출해 받아온다.**
파이썬이 SQL 로 다시 구하면 묶음 한도·구간별 개별 한도·횟수 묶음 규칙이 두 곳에 생기고,
한쪽만 고치면 화면 숫자와 챗봇 숫자가 달라진다. 어느 쪽이 맞는지 알 수 없게 된다.

약관 원문은 계산 규칙이 아니라 텍스트라 중복될 게 없으므로 DB 에서 직접 읽는다.

## LLM 은 두 자리에만 쓴다

```
질문
 ↓ classify   사람 말 → 의도 + 언급된 표현        ← LLM
 ↓ 서버       표현 → 가맹점·카드 식별 (별칭 조회)
 ↓ 서버       엔진 API / 약관 검색으로 값 확보
 ↓ compose    확보한 값 → 문장                   ← LLM
답변
```

판단과 계산은 그 사이 어디에도 들어가지 않는다. LLM 에게 산수를 시키지 않는다 —
숫자는 이미 완성된 상태로 넘긴다.

## 실행

```bash
cd ai-server
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r chatbot/requirements.txt   # Windows
# source .venv/bin/activate && pip install -r chatbot/requirements.txt  # macOS/Linux

cp .env.example .env        # 값 채우기
.venv/Scripts/python.exe -m uvicorn chatbot.main:app --reload
```

- API 문서: <http://127.0.0.1:8000/docs>
- 헬스체크: `GET /health` → `{"status":"UP","llmProvider":"stub"}`

## 개발 중에는 LLM 을 부르지 않는다

`.env` 의 `LLM_PROVIDER` 하나로 갈린다.

| 값 | 동작 | 비용 |
|---|---|---|
| `stub` (기본) | 규칙 기반 가짜 응답 | 0 |
| `openai` | 실제 호출 | 토큰 사용량만큼 |

stub 은 **등록해둔 표현만** 알아듣는다(`llm/stub.py`). 실제 사용자는 아무 말이나 하므로
**시연 전에는 반드시 `openai` 로 한 번 돌려본다.** `GET /health` 의 `llmProvider` 가
그때 stub 인 채로 들어가는 사고를 막는 장치다.

API 키는 `.env` 에만 둔다. 코드·커밋·발표자료·캡처에 넣지 않는다.

## 프롬프트는 코드가 아니라 파일이다

`prompts/classify.md`, `prompts/compose.md` 에 있다. 프롬프트가 사실상 이 서버의 규칙이라
변경 이력이 diff 로 보여야 하고, 고칠 때 파이썬을 몰라도 되어야 한다.

## 테스트

```bash
.venv/Scripts/python.exe -m pytest chatbot/tests -q
```

stub 으로 돌아 네트워크도 비용도 들지 않는다. 어댑터를 분리한 실질적인 값이 이것이다.

## 폴더

```
chatbot/
  main.py          FastAPI 앱·라우팅
  config.py        .env 로딩 (설정을 읽는 유일한 곳)
  schema.py        Spring 과 주고받는 형식
  llm/
    base.py        어댑터 계약 (LlmClient, Intent, Answer)
    stub.py        개발용 가짜
    openai_client.py
  prompts/         프롬프트 원문
  tests/
```
