<script setup>
defineEmits(['click'])

defineProps({
  text: {
    type: String,
    default: ''
  },

  type: {
    type: String,
    default: 'primary'
  },

  size: {
    type: String,
    default: 'large'
  },

  disabled: {
    type: Boolean,
    default: false
  }
})
</script>


<template>
  <button
    class="app-button"
    :class="[type, size]"
    :disabled="disabled"
    type="button"
    @click="$emit('click')"
  >
    <slot>{{ text }}</slot>
  </button>
</template>


<style scoped>
.app-button {
  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: var(--radius-md);
  border: none;

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);
}


/* =====================
   Size
===================== */

.large {
  width: 100%;
  height: 52px;

  font-size: var(--font-md);
}


.small {
  width: auto;
  height: 36px;

  padding: 0 var(--space-md);

  font-size: var(--font-sm);
}



/* =====================
   Type
===================== */

.primary {
  color: var(--color-btn-primary-text);

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );
}


.secondary {
  color: var(--color-text-secondary);

  background: var(--color-border);
}


.outline {
  color: var(--color-primary-dark);

  background: var(--color-surface);

  border: 1px solid var(--color-border);
}



/* =====================
   Interaction
===================== */

.app-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(var(--color-primary-dark-rgb), 0.15);
}

.app-button:active:not(:disabled) {
  transform: scale(0.96) translateY(0);
  box-shadow: 0 2px 8px rgba(var(--color-primary-dark-rgb), 0.1);
}

.app-button:disabled {
  color: var(--color-btn-disabled-text);
  background: var(--color-btn-disabled-bg);
  cursor: not-allowed;
  opacity: 0.6;
}
</style>