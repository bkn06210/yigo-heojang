FRONTEND README
# 두리 - 두리번거리지 말고, 두리 

금융 AI 추천 서비스 - 결제 카드 추천 및 관리 플랫폼


## 프로젝트 개요

- **스택**: Vue 3 (Composition API) + Vite + Pinia + Vue Router
- **스타일**: 커스텀 CSS (UI 프레임워크 없음, 디자인 토큰 기반)
- **상태관리**: Pinia (setup function style)
- **API 통신**: Axios (인터셉터로 Bearer 토큰 자동 첨부)


## 빠른 시작

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # 프로덕션 빌드
```


## 프로젝트 구조
```
src/
├── api/           # API 함수들 (authApi, cardApi, walletApi, ...)
├── assets/
│   └── styles/tokens.css  # 전역 디자인 토큰
├── components/    # UI 컴포넌트 (common, layout, 도메인별)
├── stores/        # Pinia 상태관리
├── views/         # 페이지 컴포넌트 (라우트별)
└── router/        # 라우팅 설정
```


## 개발 가이드

스타일링
tokens.css의 CSS 변수 사용
다크모드: [data-theme="dark"] 선택자

API 추가
src/api/에 도메인별 파일 생성
api.interceptors에서 토큰 자동 첨부

컴포넌트 작성
Vue 3 <script setup> 문법 사용
필요시 Pinia store에서 상태 관리


## 백엔드 API
Base URL: http://localhost:8080
모든 요청에 Authorization: Bearer <token> 필요
응답 형식: { success, code, data, message }


## 팀 워크플로우
feature 브랜치에서 개발
PR 작성 후 리뷰
develop에 merge
