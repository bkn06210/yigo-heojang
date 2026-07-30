// axios 통신 라이브러리
import axios from 'axios'

// 백엔드 API 통신 기본 설정
const api = axios.create({
  // Vite proxy 사용
  // /api 요청을 vite.config.js가 http://localhost:8080/api 로 넘겨줌
  baseURL: '/api',

  timeout: 5000,

  headers: {
    'Content-Type': 'application/json',
  },
})

export default api