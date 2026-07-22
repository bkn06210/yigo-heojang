// axios 통신 라이브러리
import axios from 'axios'


// 백엔드 서버와 통신하는 기본 설정
const api = axios.create({

  // 개발 환경 백엔드 주소
  // 추후 실제 서버 주소로 변경
  baseURL: 'http://localhost:8080',

  // 요청 제한 시간
  timeout: 5000,

  // JSON 형식으로 데이터 전송
  headers: {
    'Content-Type': 'application/json'
  }

})


export default api