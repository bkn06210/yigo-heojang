<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/common/PageHeader.vue'
import AppButton from '@/components/common/AppButton.vue'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()

// 비밀번호 입력
const password = ref('')
const passwordConfirm = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

// 조건 상태
const passwordValid = ref(true)

// 확인 오류
const passwordConfirmError = ref('')

// 비밀번호 검사
const validatePassword = () => {
  const passwordRule = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/

  if (!password.value) {
    passwordValid.value = true
    return
  }

  passwordValid.value = passwordRule.test(password.value)
}

// 확인 검사
const validatePasswordConfirm = () => {
  if (!passwordConfirm.value) {
    passwordConfirmError.value = ''
    return
  }

  if (password.value !== passwordConfirm.value) {
    passwordConfirmError.value = '비밀번호가 일치하지 않습니다.'
  } else {
    passwordConfirmError.value = ''
  }
}

// 입력 감지
watch(password, () => {
  validatePassword()
  validatePasswordConfirm()
})

watch(passwordConfirm, () => {
  validatePasswordConfirm()
})

// 뒤로가기
const goBack = () => {
  router.go(-1)
}

// 변경 버튼
const changePassword = () => {
  if (!password.value) {
    showToast('warning', '새 비밀번호를 입력해주세요.')
    return
  }

  if (!passwordValid.value) {
    showToast('warning', '비밀번호 조건을 확인해주세요.')
    return
  }

  if (!passwordConfirm.value) {
    showToast('warning', '비밀번호 확인을 입력해주세요.')
    return
  }

  if (passwordConfirmError.value) {
    showToast('warning', '비밀번호가 일치하지 않습니다.')
    return
  }

  // TODO: 비밀번호 변경 API 연결
  showToast('success', '비밀번호가 안전하게 변경되었습니다.')
  router.go(-1)
}

</script>


<template>
  <div class="password-change-view">
    <!-- 상단 헤더 -->
    <PageHeader title="비밀번호 변경" @back="goBack" />

    <div class="content-container">
      <h1 class="page-title">비밀번호 변경</h1>

      <!-- 입력 폼 -->
      <form @submit.prevent="changePassword" class="password-form">

        <!-- 새 비밀번호 입력 -->
        <div class="form-group">
          <label class="form-label">
            새 비밀번호
            <span class="label-hint">(8~20자의 영문, 숫자, 특수문자 조합)</span>
          </label>
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              :class="['text-input', 'password-input', { 'input-error': password && !passwordValid }]"
              placeholder="새 비밀번호를 입력해주세요"
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
              placeholder="비밀번호를 다시 입력해주세요"
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

        <!-- 변경 버튼 -->
        <button type="submit" class="change-btn">
          변경하기
        </button>

      </form>
    </div>
  </div>
</template>



<style scoped>

.password-change-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
}

.content-container {
  flex: 1;
  padding: var(--space-lg);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}

/* 타이틀 */
.page-title {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xl);
}

/* 폼 */
.password-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.form-label {
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: left;
}

.label-hint {
  font-size: var(--font-xs);
  font-weight: var(--font-normal);
  color: var(--color-text-tertiary);
  text-transform: none;
  letter-spacing: normal;
  margin-left: 4px;
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
  border-color: var(--color-error, #e74c3c) !important;
}

.input-error:focus {
  border-color: var(--color-error, #e74c3c) !important;
}

.form-error {
  font-size: var(--font-xs);
  color: var(--color-error, #e74c3c);
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

.change-btn {
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
  margin-top: var(--space-md);
}

.change-btn:hover {
  transform: translateY(-1px);
}

</style>
