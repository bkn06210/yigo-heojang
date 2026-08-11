import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// Pinia 상태 관리
import { createPinia } from 'pinia'

import '@/assets/styles/tokens.css';

// 테마 초기화 (App 생성 전)
const savedTheme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', savedTheme);

const app = createApp(App)


// 전역 상태 관리 등록
app.use(createPinia())


// 라우터 등록
app.use(router)


app.mount('#app')