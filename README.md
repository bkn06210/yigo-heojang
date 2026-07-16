# yigo-heojang

카드 혜택 최적화 전자지갑 — 보유 카드·실적·가맹점을 바탕으로 결제 직전 최적 카드를 추천한다. (KB IT's Your Life 7기 종합실무)

## 폴더 구조

| 폴더 | 내용 |
| --- | --- |
| `backend/` | Spring Legacy 백엔드 · 추천/계산 엔진 (Maven, WAR) |
| `ai-server/` | Python AI 챗봇 서버 (V2, 추후) |
| `frontend/` | Vue.js 프론트엔드 |
| `docs/` | API 명세 등 문서 |

각 폴더는 별개 프로세스로 실행되며 REST로 통신한다. 실행 방법은 각 폴더의 README를 참고한다.
