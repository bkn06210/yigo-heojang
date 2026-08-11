<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppInput from '@/components/common/AppInput.vue';
import AppButton from '@/components/common/AppButton.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';

// 로그인 API 함수
import { login as loginApi } from '@/api/authApi';

// 비밀번호 입력 공통 컴포넌트
import PasswordInput from '@/components/common/PasswordInput.vue';

// 로그인 상태 관리 Store
import { useAuthStore } from '@/stores/authStore';

// 토스트 알림
import { useToast } from '@/composables/useToast';


const router = useRouter();
const { showToast } = useToast();


// 로그인 상태 저장소
const authStore = useAuthStore();


const email = ref('');
const password = ref('');
const passwordValid = ref(true);
const showPassword = ref(false);
const emailError = ref('');
const passwordError = ref('');


// 로그인 요청 처리 함수
// 입력값 검증 후 백엔드 로그인 API 호출
const login = async () => {

  // 이메일 입력 확인
  if (!email.value) {
    showToast('warning', '이메일을 입력해주세요.');
    return;
  }


  // 비밀번호 입력 확인
  if (!password.value) {
    showToast('warning', '비밀번호를 입력해주세요.');
    return;
  }


  try {

    // 백엔드로 전달할 로그인 데이터
    // 비밀번호 확인 값은 없기 때문에 email, password만 전달
    const loginData = {
      email: email.value,
      password: password.value,
    };


    console.log(
      '요청 데이터:',
      loginData
    );


    // POST /api/auth/login 호출
    const response = await loginApi(loginData);


    console.log(
      '로그인 성공 응답:',
      response.data
    );


    // 로그인 성공 데이터
    const accessToken =
      response.data.data.accessToken;


    const member =
      response.data.data.member;



    // Pinia 저장
    // token + 로그인 사용자 정보 저장
    authStore.setLogin(
      accessToken,
      member
    );


    console.log(
      '저장된 사용자:',
      authStore.user
    );


    // 홈 이동
    console.log(
      '홈 이동'
    );

    router.push('/home');



  } catch (error) {

    console.log(
      '로그인 에러:',
      error
    );


    // 서버에서 내려준 오류 처리
    if (error.response) {

      showToast('error', error.response.data.message);


    } else {

      showToast('error', '서버와 연결할 수 없습니다.');

    }

  }

};



const goSignup = () => {

  router.push('/auth/signup');

};

const goHome = () => {

  router.push('/home');

};



// 비밀번호 찾기 팝업 표시 여부
const showPasswordFind = ref(false);



// 비밀번호 찾기 버튼 클릭
const findPassword = () => {

  showPasswordFind.value = true;

};



// 본인인증 완료
const verifyPasswordFind = (data) => {

  showPasswordFind.value = false;

  if (data?.passwordResetToken) {
    sessionStorage.setItem('passwordResetToken', data.passwordResetToken);
  }

  router.push('/auth/password-change');

};

</script>



<template>

  <div class="login-page">

    <div class="login-container">

      <!-- 헤더 -->
      <div class="login-header">
        <button class="back-button" @click="goHome">
          <span>‹</span>
        </button>
        <h1 class="login-title">Login</h1>
      </div>

      <!-- 환영 메시지 -->
      <div class="welcome-section">
        <p class="welcome-title">환영합니다!</p>
        <p class="welcome-description">
          금융 AI의 첫 스토리텍스트 금융 정보를<br />시작해보세요.
        </p>
      </div>

      <!-- 입력 폼 -->
      <form @submit.prevent="login" class="login-form">

        <!-- 이메일 입력 -->
        <div class="input-field">
          <label class="field-label">아이디</label>
          <input
            v-model="email"
            type="email"
            :class="['text-input', { error: emailError }]"
            placeholder="name@example.com"
          />
          <p v-if="emailError" class="field-error">{{ emailError }}</p>
        </div>

        <!-- 비밀번호 입력 -->
        <div class="input-field">
          <label class="field-label">비밀번호</label>
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :class="['text-input', 'password-input', { error: passwordError }]"
              placeholder="비밀번호를 입력해주세요"
            />
            <button
              type="button"
              class="eye-button"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 5C7 5 2.73 8.11 1 12.46c1.73 4.35 6 7.54 11 7.54s9.27-3.19 11-7.54C21.27 8.11 17 5 12 5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.81-2.89 3.69-4.75-1.73-4.35-6-7.54-11-7.54-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 001 12c1.73 4.35 6 7.54 11 7.54 2.04 0 4.05-.37 5.91-1.04l.64.64L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zM11.84 9.02l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" />
              </svg>
            </button>
          </div>
          <p v-if="passwordError" class="field-error">{{ passwordError }}</p>
        </div>

        <!-- 비밀번호 찾기 -->
        <div class="password-find">
          <button
            type="button"
            class="find-link"
            @click="findPassword"
          >
            비밀번호 찾기
          </button>
        </div>

        <!-- 로그인 버튼 -->
        <button
          type="submit"
          class="login-btn"
          @click.prevent="login"
        >
          로그인
        </button>

      </form>

      <!-- 회원가입 섹션 -->
      <div class="signup-section">
        <p class="signup-text">
          계정이 없으신가요?
          <button
            type="button"
            class="signup-link"
            @click="goSignup"
          >
            회원가입
          </button>
        </p>
      </div>

      <!-- 또는 -->
      <div class="or-section">
        <span class="or-line"></span>
        <span class="or-text">또는</span>
        <span class="or-line"></span>
      </div>

      <!-- 나중에 하기 -->
      <div class="later-section">
        <button
          type="button"
          class="later-link"
          @click="goHome"
        >
          나중에 하기
        </button>
      </div>

    </div>

    <AuthVerifyModal
      v-if="showPasswordFind"
      @close="showPasswordFind=false"
      @success="verifyPasswordFind"
    />

  </div>

</template>



<style scoped>

.login-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 15% 15%, rgba(var(--color-primary-dark-rgb), 0.2) 0%, transparent 45%),
    radial-gradient(circle at 85% 85%, rgba(var(--color-primary-dark-rgb), 0.15) 0%, transparent 45%),
    var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  box-sizing: border-box;
}

.login-container {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.login-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-2xl);
}

.back-button {
  background: none;
  border: none;
  padding: 0;
  font-size: var(--font-lg);
  color: var(--color-text-primary);
  cursor: pointer;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.login-title {
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.welcome-section {
  margin-bottom: var(--space-2xl);
  text-align: left;
}

.welcome-title {
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-sm) 0;
  text-align: left;
}

.welcome-description {
  font-size: var(--font-sm);
  line-height: 1.5;
  color: var(--color-text-secondary);
  margin: 0;
  text-align: left;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-bottom: var(--space-2xl);
}

.input-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  margin-bottom: var(--space-sm);
}

.field-label {
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
  text-align: left;
}

.text-input {
  width: 100%;
  height: 44px;
  padding: 0 var(--space-md);
  box-sizing: border-box;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  transition: all var(--transition-fast);
}

.text-input::placeholder {
  color: var(--color-text-secondary);
}

.text-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-surface);
}

.text-input.error {
  border-color: var(--color-coral);
  background: rgba(255, 107, 107, 0.05);
}

.text-input.error:focus {
  border-color: var(--color-coral);
  background: rgba(255, 107, 107, 0.05);
}

.field-error {
  font-size: var(--font-xs);
  color: var(--color-coral);
  margin: var(--space-xs) 0 0 0;
  text-align: left;
}

.password-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input {
  padding-right: 44px;
}

.eye-button {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity var(--transition-fast);
}

.eye-button:hover {
  opacity: 0.7;
}

.eye-button svg {
  width: 20px;
  height: 20px;
  fill: var(--color-text-secondary);
  transition: fill var(--transition-fast);
}

.eye-button:hover svg {
  fill: var(--color-text-primary);
}

.field-help {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  margin: 0;
  margin-top: 4px;
  text-align: left;
}

.password-find {
  text-align: right;
  margin-bottom: var(--space-lg);
}

.find-link {
  background: none;
  border: none;
  padding: 0;
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.find-link:hover {
  color: var(--color-primary-dark);
}

.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
  color: var(--color-btn-primary-text);
  font-size: var(--font-md);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.login-btn:hover {
  transform: translateY(-1px);
}

.login-btn:active {
  transform: scale(0.98);
}

.login-form {
  margin-bottom: var(--space-md);
}

.signup-section {
  text-align: center;
  padding: 0;
  margin-bottom: var(--space-sm);
}

.signup-text {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.signup-link {
  background: none;
  border: none;
  padding: 0;
  margin-left: 4px;
  font-size: var(--font-sm);
  color: var(--color-primary-dark);
  cursor: pointer;
  font-weight: var(--font-bold);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.signup-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

.or-section {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin: var(--space-sm) 0;
}

.or-line {
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.or-text {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  margin: 0;
  white-space: nowrap;
}

.later-section {
  text-align: center;
  margin-top: var(--space-md);
}

.later-link {
  background: transparent;
  border: none;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  color: var(--color-primary-dark);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-weight: var(--font-semibold);
  display: inline-block;
}

.later-link:hover {
  background: rgba(var(--color-primary-dark-rgb), 0.08);
  border-color: rgba(var(--color-primary-dark-rgb), 0.7);
  color: var(--color-primary-dark);
}

</style>
