<!--
  간편비밀번호 설정·변경 전 이메일 인증 모달.

  AuthVerifyModal(비밀번호 재설정용)과 API가 다르다. 이쪽은 로그인한 회원 전용이라
  이메일을 입력받지 않고 서버가 회원의 주소로 바로 보낸다. 그래서 모달이 열리면
  곧바로 코드를 발송하고, 사용자는 6자리 입력만 하면 된다.
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { sendSimplePasswordCode, verifySimplePasswordCode } from '@/api/memberApi';

const emit = defineEmits(['close', 'verified']);

const CODE_LENGTH = 6;

const maskedEmail = ref('');
const code = ref('');
const errorMessage = ref('');
const sending = ref(true);
const verifying = ref(false);

// 남은 유효시간(초). 서버가 내려준 expiresIn을 그대로 쓴다.
const secondsLeft = ref(0);
let timerId = null;

const formattedTimeLeft = computed(() => {
  const minutes = Math.floor(secondsLeft.value / 60);
  const seconds = secondsLeft.value % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
});

const expired = computed(() => secondsLeft.value <= 0);

const canSubmit = computed(
  () => code.value.length === CODE_LENGTH && !verifying.value && !expired.value
);

const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback;

const stopTimer = () => {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
};

const startTimer = (expiresIn) => {
  stopTimer();
  secondsLeft.value = expiresIn;
  timerId = setInterval(() => {
    secondsLeft.value -= 1;
    if (secondsLeft.value <= 0) {
      stopTimer();
      errorMessage.value = '인증 시간이 만료되었습니다. 코드를 다시 받아주세요.';
    }
  }, 1000);
};

const sendCode = async () => {
  sending.value = true;
  errorMessage.value = '';
  code.value = '';

  try {
    const response = await sendSimplePasswordCode();
    maskedEmail.value = response?.email ?? '';
    startTimer(response?.expiresIn ?? 300);
  } catch (error) {
    // 1분 쿨다운(429)도 여기로 온다. 서버 메시지를 그대로 보여준다.
    errorMessage.value = resolveErrorMessage(error, '인증 코드 발송에 실패했습니다.');
    stopTimer();
    secondsLeft.value = 0;
  } finally {
    sending.value = false;
  }
};

const submitCode = async () => {
  if (!canSubmit.value) {
    return;
  }

  verifying.value = true;
  errorMessage.value = '';

  try {
    const response = await verifySimplePasswordCode(code.value);
    stopTimer();
    // 변경 토큰은 여기서만 받을 수 있고, 저장 요청에 그대로 실어 보낸다.
    emit('verified', response?.simplePasswordChangeToken);
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, '인증 코드가 일치하지 않습니다.');
    code.value = '';
  } finally {
    verifying.value = false;
  }
};

// 숫자만, 6자리까지만 남긴다.
const onCodeInput = (event) => {
  code.value = event.target.value.replace(/\D/g, '').slice(0, CODE_LENGTH);
};

const handleClose = () => {
  stopTimer();
  emit('close');
};

onMounted(sendCode);
onBeforeUnmount(stopTimer);
</script>


<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>이메일 인증</h2>
        <button class="close-btn" type="button" @click="handleClose">✕</button>
      </div>

      <div class="modal-body">
        <p v-if="sending" class="description">인증 코드를 보내는 중입니다...</p>

        <p v-else-if="maskedEmail" class="description">
          <strong>{{ maskedEmail }}</strong> 으로 인증 코드를 보냈습니다.<br />
          메일에 적힌 6자리 숫자를 입력해주세요.
        </p>

        <form v-if="!sending" @submit.prevent="submitCode">
          <div class="form-group">
            <div class="code-row">
              <input
                :value="code"
                type="text"
                inputmode="numeric"
                autocomplete="one-time-code"
                maxlength="6"
                class="code-input"
                placeholder="6자리 인증 코드"
                :disabled="expired"
                @input="onCodeInput"
              />

              <span v-if="!expired && secondsLeft > 0" class="timer">
                {{ formattedTimeLeft }}
              </span>
            </div>

            <p v-if="errorMessage" class="form-error" role="alert">
              {{ errorMessage }}
            </p>
          </div>
        </form>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" type="button" @click="sendCode" :disabled="sending">
          코드 재발송
        </button>

        <button
          class="btn-confirm"
          type="button"
          :disabled="!canSubmit"
          @click="submitCode"
        >
          {{ verifying ? '확인 중...' : '인증하기' }}
        </button>
      </div>
    </div>
  </div>
</template>


<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  width: 100%;
  max-width: 480px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: var(--z-modal);
}

.modal-content {
  width: 100%;
  background: var(--color-surface);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  padding: var(--space-lg);
  box-sizing: border-box;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}

.modal-header h2 {
  margin: 0;
  font-size: var(--font-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

.close-btn {
  border: none;
  background: none;
  font-size: var(--font-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.description {
  margin: 0 0 var(--space-lg);
  font-size: var(--font-sm);
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.description strong {
  color: var(--color-text-primary);
}

.form-group {
  margin-bottom: var(--space-md);
}

.code-row {
  position: relative;
  display: flex;
  align-items: center;
}

.code-input {
  width: 100%;
  height: 52px;
  padding: 0 var(--space-2xl) 0 var(--space-md);
  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-size: var(--font-md);
  letter-spacing: 0.3em;
  font-family: inherit;
  box-sizing: border-box;
}

.code-input:focus {
  outline: none;
  border-color: var(--color-input-focus);
}

.code-input:disabled {
  opacity: 0.6;
}

.timer {
  position: absolute;
  right: var(--space-md);
  font-size: var(--font-xs);
  color: var(--color-input-error);
}

.form-error {
  margin: var(--space-xs) 0 0;
  font-size: var(--font-xs);
  color: var(--color-input-error);
}

.modal-footer {
  display: flex;
  gap: var(--space-sm);
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  font-family: inherit;
  cursor: pointer;
  transition: var(--transition-fast);
}

.btn-cancel {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
}

.btn-confirm {
  border: none;
  background: linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );
  color: var(--color-btn-primary-text);
}

.btn-cancel:disabled,
.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
