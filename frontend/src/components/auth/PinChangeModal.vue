<script setup>
import { ref } from 'vue'
import { useToast } from '@/composables/useToast'

const emit = defineEmits(['close', 'success'])
const { showToast } = useToast()

const pin = ref('')
const pinConfirm = ref('')

const handleClose = () => {
  emit('close')
}

const isFormValid = () => {
  return (
    pin.value.length === 6 &&
    pinConfirm.value.length === 6 &&
    pin.value === pinConfirm.value &&
    /^\d{6}$/.test(pin.value)
  )
}

const handleKeyPress = (e) => {
  if (!/\d/.test(e.key)) {
    e.preventDefault()
  }
}

const changePinSuccess = () => {
  if (!isFormValid()) {
    showToast('warning', '6자리 숫자를 일치하게 입력해주세요.')
    return
  }

  showToast('success', '간편비밀번호가 변경되었습니다.')
  emit('success')
  handleClose()
}
</script>

<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>간편비밀번호 변경</h2>
        <button class="close-btn" @click="handleClose">✕</button>
      </div>

      <div class="modal-body">
        <p class="modal-description">새로운 간편비밀번호를 설정해주세요</p>

        <!-- 첫 번째 PIN 입력 -->
        <div class="form-group">
          <label class="form-label">간편비밀번호</label>
          <input
            v-model="pin"
            type="password"
            inputmode="numeric"
            maxlength="6"
            class="pin-input"
            placeholder="6자리 숫자 입력"
            @keypress="handleKeyPress"
          />
          <p v-if="pin.length > 0" class="pin-count">{{ pin.length }}/6</p>
        </div>

        <!-- PIN 확인 입력 -->
        <div class="form-group">
          <label class="form-label">간편비밀번호 확인</label>
          <input
            v-model="pinConfirm"
            type="password"
            inputmode="numeric"
            maxlength="6"
            class="pin-input"
            placeholder="6자리 숫자 입력"
            @keypress="handleKeyPress"
          />
          <p v-if="pinConfirm.length > 0" class="pin-count">{{ pinConfirm.length }}/6</p>
          <p v-if="pinConfirm && pin !== pinConfirm" class="form-error">
            비밀번호가 일치하지 않습니다
          </p>
          <p v-else-if="pin && pinConfirm && pin === pinConfirm" class="form-success">
            ✓ 일치합니다
          </p>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="handleClose">취소</button>
        <button
          class="btn-confirm"
          :disabled="!isFormValid()"
          @click="changePinSuccess"
        >
          변경하기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.modal-content {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface);
  border-radius: 24px 24px 0 0;
  box-sizing: border-box;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg) var(--space-lg) var(--space-md);
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  margin: 0;
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: var(--color-text-secondary);
  font-size: var(--font-lg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color var(--transition-fast);
}

.close-btn:hover {
  color: var(--color-text-primary);
}

.modal-body {
  padding: var(--space-lg);
}

.modal-description {
  margin: 0 0 var(--space-lg) 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.form-group {
  margin-bottom: var(--space-lg);
}

.form-label {
  display: block;
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--space-xs);
}

.pin-input {
  width: 100%;
  height: 48px;
  padding: 0 var(--space-md);
  box-sizing: border-box;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  letter-spacing: 8px;
  text-align: center;
  transition: border-color var(--transition-fast);
}

.pin-input::placeholder {
  color: var(--color-text-secondary);
  letter-spacing: 0;
}

.pin-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.pin-count {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  margin: 4px 0 0 0;
}

.form-error {
  font-size: var(--font-xs);
  color: var(--color-error, #e74c3c);
  margin: 4px 0 0 0;
}

.form-success {
  font-size: var(--font-xs);
  color: var(--color-primary-dark);
  margin: 4px 0 0 0;
  font-weight: var(--font-semibold);
}

.modal-footer {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-lg);
  border-top: 1px solid var(--color-border);
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  height: 44px;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-cancel {
  background: var(--color-bg);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.btn-cancel:hover {
  background: var(--color-border);
}

.btn-confirm {
  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
  color: var(--color-btn-primary-text);
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-confirm:not(:disabled):hover {
  transform: translateY(-1px);
}
</style>
