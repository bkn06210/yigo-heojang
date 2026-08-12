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

## DB 세팅

`backend/db/`의 SQL을 **번호 순서대로** 실행한다. 순서를 지켜야 하는 이유는 뒤 파일이 앞 파일이 만든 행을 참조하기 때문이다.

| 순서 | 파일 | 내용 |
| --- | --- | --- |
| 1 | `schema.sql` | 테이블 정의 (기존 DB를 지우고 다시 만든다) |
| 2 | `data.sql` | 회원·약관·카테고리·포인트사 등 기준 데이터 |
| 3 | `91_seed_card_benefit.sql` | 카드·혜택·연회비 |
| 4 | `92_seed_user_data.sql` | 보유카드·소비내역·계산 상태 |
| 5 | `94_seed_membership_usage_place.sql` | 멤버십 주요 사용처·공식 사이트 URL |

```bash
cd backend/db
for f in schema.sql data.sql 91_seed_card_benefit.sql 92_seed_user_data.sql 94_seed_membership_usage_place.sql; do
  mysql -u root -p <DB이름> < "$f"
done
```

### pull 받은 뒤 화면이 비거나 500이 날 때

**DB 스크립트를 다시 돌려야 한다.** 코드는 `git pull`로 따라오지만 DB는 각자 로컬에 있어 자동으로 바뀌지 않는다.
컬럼이 늘어난 변경을 받고 스크립트를 안 돌리면 `Unknown column ...` 으로 API가 500을 내고, 화면은 빈 채로 보인다.

전체를 다시 만들기 부담스러우면 바뀐 파일만 돌려도 된다. `9x_` 파일은 다시 실행해도 중복이 쌓이지 않게 만들어 두었다.

```bash
mysql -u root -p <DB이름> < backend/db/94_seed_membership_usage_place.sql
```

### 카드 이미지

카드 이미지는 WAR 바깥 폴더에서 서빙한다. `backend/src/main/resources/db.properties`에 경로를 적고,
그 아래 `cards/` 폴더에 이미지를 둔다.

```
app.image.storage-path=C:/Users/<사용자>/Desktop/wallet-images
```

### DB를 바꾸는 변경을 올릴 때

- 컬럼 추가는 `schema.sql`에도 반영한다. 새로 세팅하는 사람이 스크립트를 따로 안 돌려도 되게 한다.
- 기존 DB를 쓰는 사람을 위해 `9x_` 파일에도 `ALTER`를 넣되, 컬럼이 이미 있으면 건너뛰게 만든다.
  MySQL 8에는 `ADD COLUMN IF NOT EXISTS`가 없어 `information_schema`로 직접 확인해야 한다
  (예: `94_seed_membership_usage_place.sql`).
- 시드에서 다른 테이블을 참조할 때는 **id가 아니라 이름으로** 찾는다. id는 DB마다 다르다 —
  실제로 개발 DB와 새로 만든 DB의 `point_provider` id가 2씩 어긋나 있었고, id로 적었을 때
  URL 18건이 에러 없이 엉뚱한 제공사에 들어갔다.
- PR 본문에 "이 스크립트를 돌려야 한다"를 적는다.
