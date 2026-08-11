<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  title: String,

  message: String,

  confirmText: {
    type: String,
    default: '확인'
  },

  cancelText: {
    type: String,
    default: '취소'
  }
});


defineEmits([
  'confirm',
  'close'
]);
</script>


<template>
  <div
    class="modal-overlay"
    @click.self="$emit('close')"
  >

    <div class="modal-content">

      <h3>
        {{ props.title }}
      </h3>


      <p>
        {{ props.message }}
      </p>


      <div class="modal-buttons">

        <button
          class="cancel-btn"
          @click="$emit('close')"
        >
          {{ props.cancelText }}
        </button>


        <button
          class="confirm-btn"
          @click="$emit('confirm')"
        >
          {{ props.confirmText }}
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

  background: rgba(0,0,0,0.5);

  display: flex;

  justify-content: center;

  align-items: center;

  z-index: var(--z-modal);
}


.modal-content {
  background: var(--color-surface);

  padding: var(--space-xl);

  border-radius: var(--radius-lg);

  width: 90%;
  max-width: 480px;

  text-align: center;

  box-shadow: var(--shadow-card);

  box-sizing: border-box;
}


h3 {
  margin: 0 0 var(--space-sm);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-semibold);
}


p {
  margin: 0;

  color: var(--color-text-secondary);

  font-size: var(--font-sm);
}


.modal-buttons {
  margin-top: var(--space-lg);

  display: flex;

  gap: var(--space-sm);
}


button {
  flex: 1;

  height: 40px;

  border-radius: var(--radius-sm);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);
}


.cancel-btn {
  border: 1px solid var(--color-border);

  background: var(--color-surface);

  color: var(--color-text-secondary);
}


.confirm-btn {
  border: none;

  background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

  color: var(--color-btn-primary-text);
}


button:hover {
  opacity: 0.9;
}
</style>