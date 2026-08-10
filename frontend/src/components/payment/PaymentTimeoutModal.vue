<script setup>
const emit = defineEmits(['regenerate', 'cancel']);
</script>

<template>
  <div class="overlay">
    <div class="modal">
      <h2>QR 코드 만료</h2>

      <p class="description">
        QR 코드 유효시간이 만료되었습니다.<br/>
        다시 생성하시거나 취소하실 수 있습니다.
      </p>

      <div class="button-group">
        <button class="btn-secondary" @click="$emit('cancel')">
          취소
        </button>
        <button class="btn-primary" @click="$emit('regenerate')">
          재생성
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  inset: 0;
  width: 100%;
  max-width: 480px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

.modal {
  width: 90%;
  max-width: 480px;
  padding: var(--space-xl);
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  text-align: center;
  box-sizing: border-box;
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
}

.description {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-lg) 0;
  line-height: 1.5;
}

.button-group {
  display: flex;
  gap: var(--space-md);
}

.btn-secondary,
.btn-primary {
  flex: 1;
  padding: var(--space-md);
  height: 48px;
  border: none;
  border-radius: var(--radius-md);
  font-weight: var(--font-semibold);
  font-size: var(--font-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: var(--color-border);
  color: var(--color-text-primary);
}

.btn-secondary:hover {
  opacity: 0.8;
}

.btn-secondary:active {
  transform: scale(0.98);
}

.btn-primary {
  background: linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );
  color: var(--color-btn-primary-text);
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-primary:active {
  transform: scale(0.98);
}
</style>
