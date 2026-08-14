<!-- 본인 인증 담당 -->
<script setup>
import { ref, watch } from 'vue';
import PasswordInput from '@/components/common/PasswordInput.vue';

const props = defineProps({
  title: {
    type: String,
    default: '비밀번호 확인'
  },

  description: {
    type: String,
    default: '본인 확인을 위해 비밀번호를 다시 입력해주세요.'
  },

  confirmText: {
    type: String,
    default: '확인'
  },

  // 부모가 API를 호출하는 동안 버튼을 잠그고 문구를 바꾼다.
  loading: {
    type: Boolean,
    default: false
  },

  // 서버가 내려준 실패 사유(비밀번호 불일치 등)를 부모가 그대로 전달한다.
  errorMessage: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['confirm', 'close']);

const password = ref('');

// 사용자가 다시 입력하기 시작하면 이전 에러 문구는 지워준다.
// (부모가 내려준 errorMessage는 부모가 지우므로, 여기서는 로컬 표시만 끈다)
const dirty = ref(false);

watch(password, () => {
  dirty.value = true;
});

watch(
  () => props.errorMessage,
  () => {
    dirty.value = false;
  }
);

const handleConfirm = () => {
  if (props.loading || !password.value) {
    return;
  }

  emit('confirm', password.value);
};

const handleClose = () => {
  if (props.loading) {
    return;
  }

  emit('close');
};
</script>


<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <h3>{{ props.title }}</h3>

      <p class="description">{{ props.description }}</p>

      <form @submit.prevent="handleConfirm">
        <PasswordInput v-model="password" />

        <!-- 에러는 사용자가 새로 입력을 시작하면 감춘다 -->
        <p
          v-if="props.errorMessage && !dirty"
          class="error-message"
          role="alert"
        >
          {{ props.errorMessage }}
        </p>

        <div class="modal-buttons">
          <button
            type="button"
            class="cancel-btn"
            :disabled="props.loading"
            @click="handleClose"
          >
            취소
          </button>

          <button
            type="submit"
            class="confirm-btn"
            :disabled="props.loading || !password"
          >
            {{ props.loading ? '처리 중...' : props.confirmText }}
          </button>
        </div>
      </form>
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

  display: flex;
  align-items: center;
  justify-content: center;

  background: rgba(0, 0, 0, 0.5);
  z-index: var(--z-modal);
  padding: var(--space-lg);
  box-sizing: border-box;
}

.modal-content {
  width: 100%;
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  box-sizing: border-box;
}

h3 {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-md);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

.description {
  margin: 0 0 var(--space-md);
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.error-message {
  margin: var(--space-xs) 0 0;
  font-size: var(--font-xs);
  color: var(--color-input-error);
}

.modal-buttons {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  font-family: inherit;
  cursor: pointer;
  transition: var(--transition-fast);
}

.cancel-btn {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.confirm-btn {
  background: var(--color-coral);
  color: #fff;
}

.cancel-btn:disabled,
.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
