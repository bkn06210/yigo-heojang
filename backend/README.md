# backend

Spring Legacy(비 Boot) + MyBatis + MySQL, Maven WAR 배포.

> 초안. 팀 합의에 따라 PR로 수정한다.

## 필요 버전

- JDK 17 / Tomcat 9 (javax 서블릿 스펙 — Tomcat 10 아님) / MySQL 8.0

## 실행

1. **DB 설정** — `src/main/resources/db.properties.example`를 `db.properties`로 복사 후 값 입력.
   (로컬 실행: `host=localhost` / docker-compose: `host=mysql`)
2. **빌드** — `./mvnw clean package` (Windows: `mvnw.cmd clean package`) → `target/ROOT.war`
3. **실행** — 로컬 Tomcat 9에 `ROOT.war` 배포, 또는 `docker compose up -d` (tomcat + mysql)
4. **헬스체크** — `curl http://localhost:8080/api/health`
   → `{"success":true,"data":"ok","message":null}`
