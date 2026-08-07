<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const emit = defineEmits(['close', 'timeout', 'success']);

const timeRemaining = ref(60);
let timerInterval = null;
let successTimeout = null;

onMounted(() => {
  successTimeout = setTimeout(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    emit('success');
  }, 3000);

  timerInterval = setInterval(() => {
    timeRemaining.value--;
    if (timeRemaining.value <= 0) {
      clearInterval(timerInterval);
      if (successTimeout) {
        clearTimeout(successTimeout);
      }
      emit('timeout');
    }
  }, 1000);
});

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval);
  }
  if (successTimeout) {
    clearTimeout(successTimeout);
  }
});

const closeModal = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
  }
  if (successTimeout) {
    clearTimeout(successTimeout);
  }
  emit('close');
};

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};
</script>

<template>
  <div class="overlay" @click="closeModal">
    <div class="modal" @click.stop>

      <h2>결제 인증</h2>

      <div class="timer">
        {{ formatTime(timeRemaining) }}
      </div>

      <div class="qr-container">
        <div class="qr-code">
          QR 코드
        </div>
      </div>

      <p class="instruction">
        QR 코드를 스캔하여 결제를 완료하세요
      </p>

      <button class="cancel-btn" @click="closeModal">
        결제 취소
      </button>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.modal {
  width: 320px;
  padding: var(--space-xl);
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: relative;
}

[data-theme="dark"] .modal {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}


.modal h2 {
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-md) 0;
  text-align: center;
}

.timer {
  text-align: center;
  font-size: 28px;
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-lg);
  font-family: 'Courier New', monospace;
}

.qr-container {
  display: flex;
  justify-content: center;
  margin: var(--space-lg) 0;
}

.qr-code {
  width: 200px;
  height: 200px;
  background: white;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.instruction {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  text-align: center;
  margin: var(--space-md) 0;
}

.cancel-btn {
  width: 100%;
  padding: var(--space-md);
  height: 48px;
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
  font-size: var(--font-sm);
  cursor: pointer;
  transition: all 0.2s;
  margin-top: var(--space-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-btn:hover {
  background: rgba(var(--color-primary-rgb), 0.1);
}

.cancel-btn:active {
  transform: scale(0.98);
}
</style>
