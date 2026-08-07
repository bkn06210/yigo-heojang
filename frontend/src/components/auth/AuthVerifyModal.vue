<script>
import { ref, defineComponent } from 'vue'
import ToastNotification from '@/components/common/ToastNotification.vue'
import Icon from '@/components/common/Icon.vue'

<<<<<<< HEAD
export default defineComponent({
  components: {
    ToastNotification,
    Icon
  },
  props: {
    initialEmail: {
      type: String,
      default: ''
    },
    initialStep: {
      type: Number,
      default: 1
    }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const email = ref(props.initialEmail)
    const authCodeDigits = ref(['', '', '', '', '', ''])
    const step = ref(props.initialStep)
    const error = ref('')
    const timeLeft = ref(300) // 5분 = 300초
    let timerInterval = null
=======
import { ref } from 'vue'
import { requestPasswordResetCode, verifyPasswordResetCode } from '@/api/authApi'
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    // 토스트 상태
    const toastType = ref('success')
    const toastMessage = ref('')
    const showToast = ref(false)

    // 타이머 시작
    const startTimer = () => {
      timeLeft.value = 300
      clearInterval(timerInterval)
      timerInterval = setInterval(() => {
        timeLeft.value--
        if (timeLeft.value <= 0) {
          clearInterval(timerInterval)
          error.value = '인증 시간이 초과되었습니다. 다시 시도해주세요.'
          step.value = 1
          authCodeDigits.value = ['', '', '', '', '', '']
        }
      }, 1000)
    }

    // 타이머 중지
    const stopTimer = () => {
      clearInterval(timerInterval)
    }

    // 시간 포맷팅 (MM:SS)
    const formatTime = (seconds) => {
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${mins}:${secs.toString().padStart(2, '0')}`
    }

    const requestAuthCode = () => {
      if (!email.value.trim()) {
        error.value = '아이디를 입력해주세요'
        return
      }
      error.value = ''
      step.value = 2
      startTimer()
    }

    const submitAuthCode = () => {
      const fullCode = authCodeDigits.value.join('')
      if (!fullCode.trim()) {
        error.value = '인증번호를 입력해주세요'
        return
      }
      if (fullCode.length !== 6) {
        error.value = '인증번호는 6자리입니다'
        return
      }
      if (fullCode === '123456') {
        stopTimer()
        emit('success')
      } else {
        error.value = '인증번호가 일치하지 않습니다. 다시 확인해주세요.'
      }
    }

    const closeModal = () => {
      stopTimer()
      email.value = ''
      authCodeDigits.value = ['', '', '', '', '', '']
      step.value = 1
      error.value = ''
      emit('close')
    }

    const handleDigitInput = (index, event) => {
      const value = event.target.value
      if (/^\d$/.test(value)) {
        authCodeDigits.value[index] = value
        if (index < 5) {
          const nextInput = document.querySelector(`.digit-input-${index + 1}`)
          if (nextInput) nextInput.focus()
        }
      } else {
        authCodeDigits.value[index] = ''
      }
    }

<<<<<<< HEAD
    const handleDigitKeydown = (index, event) => {
      if (event.key === 'Backspace' && authCodeDigits.value[index] === '' && index > 0) {
        const prevInput = document.querySelector(`.digit-input-${index - 1}`)
        if (prevInput) prevInput.focus()
      }
    }
=======
const loading = ref(false)

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

const sendCode = async () => {
  emailError.value = ''
  if (!email.value) {
    emailError.value = '이메일을 입력해주세요.'
    return
  }
  loading.value = true
  try {
    await requestPasswordResetCode(email.value.trim())
    codeSent.value = true
    alert('인증 코드를 전송했습니다. 이메일을 확인해주세요.')
  } catch (error) {
    emailError.value = getErrorMessage(error, '인증 코드 전송에 실패했습니다.')
  } finally {
    loading.value = false
  }
}


// 인증 완료
const verify = async () => {
  authCodeError.value = ''
  if (!codeSent.value) {
    authCodeError.value = '먼저 인증 코드를 요청해주세요.'
    return
  }
  if (!/^\d{6}$/.test(authCode.value)) {
    authCodeError.value = '인증번호 6자리를 입력해주세요.'
    return
  }
  loading.value = true
  try {
    const response = await verifyPasswordResetCode(email.value.trim(), authCode.value)
    emit('success', response.data.data.passwordResetToken)
  } catch (error) {
    authCodeError.value = getErrorMessage(error, '인증번호 확인에 실패했습니다.')
  } finally {
    loading.value = false
  }
}


// 닫기
const close = () => {

  emit('close')

}
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    return {
      email,
      authCodeDigits,
      step,
      error,
      timeLeft,
      formatTime,
      toastType,
      toastMessage,
      showToast,
      requestAuthCode,
      submitAuthCode,
      closeModal,
      handleDigitInput,
      handleDigitKeydown
    }
  }
})
</script>


<template>

<!-- 인증 토스트 알림 -->
<ToastNotification
  v-if="showToast"
  :type="toastType"
  :message="toastMessage"
  :duration="2000"
  @close="showToast = false"
/>

<div class="overlay">


<div class="modal">


<button class="close" @click="closeModal"><Icon name="close" size="xs" /></button>


<!-- step 1: 아이디/이름 입력 -->
<div v-if="step === 1">

  <h2>
  본인인증
  </h2>

  <label>
  아이디(E-mail)
  </label>

  <input
  v-model="email"
  type="email"
  placeholder="이메일을 입력해주세요"
  />

  <p v-if="error" class="error-message">
    {{ error }}
  </p>

  <button class="confirm" @click="requestAuthCode">인증번호 받기</button>

</div>


<!-- step 2: 인증번호 입력 -->
<div v-else-if="step === 2">

  <h2>
  인증번호 입력
  </h2>

  <p class="timer-text" :class="{ 'timer-warning': timeLeft <= 60 }">
    {{ formatTime(timeLeft) }}
  </p>

<<<<<<< HEAD
  <p class="code-hint">
  {{ email }}로 전송된 6자리 인증번호를 입력해주세요.
  </p>
=======
<p v-if="emailError" class="error">{{ emailError }}</p>

<button class="send" :disabled="loading" @click="sendCode">
{{ codeSent ? '인증번호 재전송' : '인증번호 전송' }}
</button>

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  <div class="auth-code-boxes">
    <input
      v-for="(digit, index) in authCodeDigits"
      :key="index"
      :class="`digit-input digit-input-${index}`"
      type="text"
      inputmode="numeric"
      maxlength="1"
      :value="digit"
      @input="handleDigitInput(index, $event)"
      @keydown="handleDigitKeydown(index, $event)"
    />
  </div>

  <p v-if="error" class="error-message">
    {{ error }}
  </p>

  <button class="confirm" @click="submitAuthCode">확인</button>

<<<<<<< HEAD
</div>
=======
<input
v-model="authCode"
placeholder="인증번호를 입력해주세요"
maxlength="6"
/>

<p v-if="authCodeError" class="error">{{ authCodeError }}</p>



<button
class="confirm"
:disabled="loading"
@click="verify"
>
{{ loading ? '처리 중...' : '확인' }}
</button>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


</div>


</div>


</template>


<style scoped>

.overlay {

  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;

}


.modal {

  width: 320px;
  background: var(--color-surface);
  border-radius: 16px;
  padding: 24px;
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

}


h2 {

  margin: 0 0 16px 0;
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);

}


.close {

  position: absolute;
  right: 12px;
  top: 8px;
  border: none;
  background: none;
  font-size: 28px;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px;
  transition: color var(--transition-fast);

}


.close:hover {

  color: var(--color-text-primary);

}


label {

  display: block;
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  margin: 0 0 8px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;

}


input {

  width: 100%;
  height: 44px;
  padding: 0 12px;
  box-sizing: border-box;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  margin-bottom: 8px;
  transition: all var(--transition-fast);

}


input:focus {

  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.1);

}


input::placeholder {

  color: var(--color-text-secondary);

}


.auth-code-boxes {

  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin-bottom: 16px;

}


.digit-input {

  width: 100%;
  aspect-ratio: 1;
  padding: 0;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  text-align: center;
  font-size: 24px;
  font-weight: var(--font-bold);
  font-family: monospace;
  transition: all var(--transition-fast);

}


.digit-input:focus {

  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.1);

}


.digit-input::placeholder {

  color: var(--color-text-secondary);

}


.timer-text {

  font-size: var(--font-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-bold);
  font-family: monospace;
  margin: 0 0 4px 0;
  transition: color var(--transition-fast);

}


.timer-text.timer-warning {

  color: var(--color-coral);
  animation: pulse-warning 1s infinite;

}


.code-hint {

  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  margin: 0 0 20px 0;
  line-height: 1.5;

}


.error-message {

  color: var(--color-error, #e74c3c);
  font-size: var(--font-xs);
  margin: 12px 0 16px 0;

}


.confirm {

  width: 100%;
  height: 48px;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
  color: var(--color-btn-primary-text);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-top: 0;

}

<<<<<<< HEAD

.confirm:hover:not(:disabled) {

  transform: translateY(-1px);

}


.confirm:active:not(:disabled) {

  transform: scale(0.98);

}


.confirm:disabled {

  opacity: 0.6;
  cursor: not-allowed;

}


@keyframes pulse-warning {

  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }

}

</style>
=======
.send { width:100%; height:42px; margin:-8px 0 16px; border:1px solid #ddd; border-radius:8px; background:white; }
.error { margin:-10px 0 12px; color:#ef4444; font-size:12px; }

</style>
<!-- 07_25 연동 변경: 본인인증 요청·검증을 실제 백엔드 API와 연결한 컴포넌트다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
