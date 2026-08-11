<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';

// 공통 컴포넌트
import AppInput from '@/components/common/AppInput.vue';
import AppButton from '@/components/common/AppButton.vue';
import PasswordInput from '@/components/common/PasswordInput.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';

// API 전달 데이터: name, email, password, signupVerificationToken
import {
  signup,
  sendSignupEmailVerification,
  verifySignupEmailVerification,
} from '@/api/authApi';

// 토스트 알림
import { useToast } from '@/composables/useToast';

const router = useRouter();
const { showToast } = useToast();

// 입력값
const name = ref('');
const email = ref('');
const password = ref('');
const passwordConfirm = ref('');
const showPassword = ref(false);
const showPasswordConfirm = ref(false);

// 이메일 인증 상태
const verificationCode = ref('');
const signupVerificationToken = ref('');
const isEmailVerified = ref(false);

// 비밀번호 조건 상태
const passwordValid = ref(true);

// 비밀번호 확인 오류
const passwordConfirmError = ref('');

// 이메일 중복 검사 상태
const emailDuplicateError = ref('');

// 인증 모달 표시 여부
const showAuthModal = ref(false);

// 이메일 인증 (중복 검사 포함)
const checkEmail = async () => {
  if (!email.value) {
    showToast('warning', '이메일을 입력해주세요.');
    return;
  }

  try {
    await sendSignupEmailVerification(email.value);
    emailDuplicateError.value = '';
    showAuthModal.value = true;
    showToast('success', '인증 코드가 발송되었습니다.');
  } catch (error) {
    emailDuplicateError.value = error.response?.data?.message || '이메일 인증 요청에 실패했습니다.';
    showToast('error', emailDuplicateError.value);
  }
};

// 인증 모달 닫기
const closeAuthModal = () => {
  showAuthModal.value = false;
};

// 인증 성공
const handleAuthSuccess = () => {
  showAuthModal.value = false;
  isEmailVerified.value = true;
  showToast('success', '이메일 인증이 완료되었습니다.');
};

// 이메일 인증번호 검증
const confirmVerificationCode = async () => {
  if (!verificationCode.value) {
    showToast('warning', '인증번호를 입력해주세요.');
    return;
  }

  try {
    const response = await verifySignupEmailVerification(
      email.value,
      verificationCode.value,
    );

    signupVerificationToken.value = response.data.data.signupVerificationToken;

    isEmailVerified.value = true;

    showToast('success', '이메일 인증이 완료되었습니다.');
  } catch (error) {
    showToast('error', error.response?.data?.message || '인증번호 확인에 실패했습니다.');
  }
};

// 비밀번호 조건 검사
const validatePassword = () => {
  const passwordRule =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/;

  if (!password.value) {
    passwordValid.value = true;
    return;
  }

  passwordValid.value = passwordRule.test(password.value);
};

// 비밀번호 확인 검사
const validatePasswordConfirm = () => {
  if (!passwordConfirm.value) {
    passwordConfirmError.value = '';
    return;
  }

  if (password.value !== passwordConfirm.value) {
    passwordConfirmError.value = '비밀번호가 일치하지 않습니다.';
  } else {
    passwordConfirmError.value = '';
  }
};

// 입력 감지
watch(password, () => {
  validatePassword();
  validatePasswordConfirm();
});

watch(passwordConfirm, () => {
  validatePasswordConfirm();
});

// 뒤로가기
const goBack = () => {
  router.push('/auth/terms');
};

// 로그인 이동
const goLogin = () => {
  router.push('/auth/login');
};

// 나중에 하기
const skip = () => {
  router.push('/home');
};

// 회원가입
const nextStep = async () => {
  if (!name.value) {
    showToast('warning', '이름을 입력해주세요.');
    return;
  }

  if (!email.value) {
    showToast('warning', '이메일을 입력해주세요.');
    return;
  }

  if (!isEmailVerified.value) {
    showToast('warning', '이메일 인증을 완료해주세요.');
    return;
  }

  if (!password.value) {
    showToast('warning', '비밀번호를 입력해주세요.');
    return;
  }

  if (!passwordValid.value || passwordConfirmError.value) {
    return;
  }

  try {
    const userData = {
      name: name.value,
      email: email.value,
      password: password.value,
      signupVerificationToken: signupVerificationToken.value,
    };

    await signup(userData);

    showToast('success', '회원가입이 완료되었습니다.');

    router.push('/auth/login');
  } catch (error) {
    if (error.response) {
      showToast('error', error.response.data.message);
    } else {
      showToast('error', '서버와 연결할 수 없습니다.');
    }
  }
};
</script>

<template>
  <div class="signup-page">
    <div class="signup-container">

      <!-- 헤더 -->
      <div class="signup-header">
        <button class="back-button" @click="goBack">
          <span>‹</span>
        </button>
        <h1 class="signup-title">회원가입</h1>
      </div>

      <!-- 입력 폼 -->
      <form @submit.prevent="nextStep" class="signup-form">

        <!-- 이름 입력 -->
        <div class="form-group">
          <label class="form-label">이름</label>
          <input
            v-model="name"
            type="text"
            class="text-input"
            placeholder="이름을 입력해주세요"
          />
          
        </div>

        <!-- 이메일 입력 -->
        <div class="form-group">
          <label class="form-label">이메일</label>
          <div class="email-group">
            <input
              v-model="email"
              type="email"
              class="text-input"
              placeholder="example@gmail.com"
            />
            <button
              type="button"
              class="verify-button"
              @click="checkEmail"
            >
              인증하기
            </button>
          </div>
          <p v-if="emailDuplicateError" class="form-error">
            {{ emailDuplicateError }}
          </p>
          <p v-if="isEmailVerified" class="form-success">
            ✓ 인증되었습니다
          </p>
        </div>

        <!-- 비밀번호 입력 -->
        <div class="form-group">
          <label class="form-label">
            비밀번호
            <span class="label-hint">(8~20자의 영문, 숫자, 특수문자 조합)</span>
          </label>
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :class="['text-input', 'password-input', { 'input-error': password && !passwordValid }]"
              placeholder="비밀번호를 입력해주세요"
            />
            <button
              type="button"
              class="eye-button"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 5C7 5 2.73 8.11 1 12.46c1.73 4.35 6 7.54 11 7.54s9.27-3.19 11-7.54C21.27 8.11 17 5 12 5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.81-2.89 3.69-4.75-1.73-4.35-6-7.54-11-7.54-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 001 12c1.73 4.35 6 7.54 11 7.54 2.04 0 4.05-.37 5.91-1.04l.64.64L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zM11.84 9.02l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" />
              </svg>
            </button>
          </div>
          <p v-if="passwordValid && password" class="form-success">
            ✓ 안전한 비밀번호입니다
          </p>
        </div>

        <!-- 비밀번호 확인 입력 -->
        <div class="form-group">
          <label class="form-label">비밀번호 확인</label>
          <div class="password-wrapper">
            <input
              v-model="passwordConfirm"
              :type="showPasswordConfirm ? 'text' : 'password'"
              :class="['text-input', 'password-input', { 'input-error': passwordConfirm && passwordConfirmError }]"
              placeholder="비밀번호를 입력해주세요"
            />
            <button
              type="button"
              class="eye-button"
              @click="showPasswordConfirm = !showPasswordConfirm"
            >
              <svg v-if="showPasswordConfirm" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 5C7 5 2.73 8.11 1 12.46c1.73 4.35 6 7.54 11 7.54s9.27-3.19 11-7.54C21.27 8.11 17 5 12 5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
              </svg>
              <svg v-else viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.81-2.89 3.69-4.75-1.73-4.35-6-7.54-11-7.54-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 001 12c1.73 4.35 6 7.54 11 7.54 2.04 0 4.05-.37 5.91-1.04l.64.64L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zM11.84 9.02l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" />
              </svg>
            </button>
          </div>
          <p v-if="passwordConfirmError" class="form-error">
            {{ passwordConfirmError }}
          </p>
          <p v-if="!passwordConfirmError && passwordConfirm && password === passwordConfirm" class="form-success">
            ✓ 비밀번호가 일치합니다
          </p>
        </div>

        <!-- 다음 버튼 -->
        <button type="submit" class="next-btn">
          다음
        </button>

      </form>

      <!-- 이메일 인증 모달 -->
      <AuthVerifyModal
        v-if="showAuthModal"
        :initial-email="email"
        :initial-step="2"
        @close="closeAuthModal"
        @success="handleAuthSuccess"
      />

      <!-- 로그인 섹션 -->
      <div class="login-section">
        <p class="login-text">
          이미 계정이 있으신가요?
          <button
            type="button"
            class="login-link"
            @click="goLogin"
          >
            로그인
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
          @click="skip"
        >
          나중에 하기
        </button>
      </div>

      <!-- 보안 안내 -->
      <p class="security-text">
        입력하신 개인정보는 안전하게 암호화되어 보호됩니다.
      </p>

    </div>
  </div>
</template>



<style scoped>

.signup-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 15% 15%, rgba(var(--color-primary-dark-rgb), 0.2) 0%, transparent 45%),
    radial-gradient(circle at 85% 85%, rgba(var(--color-primary-dark-rgb), 0.15) 0%, transparent 45%),
    var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-md);
  box-sizing: border-box;
}

.signup-container {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.signup-header {
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

.signup-title {
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.signup-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  margin-bottom: var(--space-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.form-label {
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  text-align: left;
}

.label-hint {
  font-size: var(--font-xs);
  font-weight: var(--font-normal);
  color: var(--color-text-secondary);
  text-transform: none;
  letter-spacing: normal;
  display: block;
  margin-top: var(--space-xs);
  margin-left: 0;
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

.input-error {
  border-color: var(--color-coral) !important;
}

.input-error:focus {
  border-color: var(--color-coral) !important;
}

.form-help {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  margin: 0;
  text-align: left;
  transition: color var(--transition-fast);
}

.form-help-error {
  color: var(--color-coral);
}

.form-error {
  font-size: var(--font-xs);
  color: var(--color-coral);
  margin: 8px 0 0 0;
  text-align: left;
}

.form-success {
  font-size: var(--font-xs);
  color: var(--color-primary-dark);
  margin: 8px 0 0 0;
  text-align: left;
  font-weight: var(--font-semibold);
}

.email-group {
  display: flex;
  gap: var(--space-sm);
  align-items: center;
}

.email-group .text-input {
  flex: 1;
}

.verify-button {
  padding: 0 var(--space-md);
  height: 44px;
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.5);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary-dark);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.verify-button:hover {
  background: rgba(var(--color-primary-dark-rgb), 0.08);
  border-color: rgba(var(--color-primary-dark-rgb), 0.7);
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
}

.eye-button svg {
  width: 20px;
  height: 20px;
  fill: var(--color-text-secondary);
}

.next-btn {
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

.next-btn:hover {
  transform: translateY(-1px);
}

.login-section {
  text-align: center;
  padding: 0;
  margin-bottom: var(--space-sm);
}

.login-text {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.login-link {
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

.login-link:hover {
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
  color: var(--color-primary-dark);
}

.security-text {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  margin: var(--space-2xl) 0 0 0;
  line-height: 1.4;
  text-align: center;
}

</style>
