<template>
  <button
    class="base-button"
    :class="`base-button--${variant}`"
    :disabled="variant === 'disabled'"
    @click="$emit('click')"
  >
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary', // 'primary' | 'secondary' | 'disabled'
  },
});
defineEmits(['click']);
</script>

<style scoped>
.base-button {
  padding: 12px 20px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 800;
  border: none;
  cursor: pointer;
}

.base-button--primary {
  background: linear-gradient(135deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
  color: var(--color-btn-primary-text);
}

.base-button--secondary {
  background: transparent;
  color: var(--color-btn-secondary);
  border: 1.5px solid var(--color-btn-secondary);
}

.base-button--disabled {
  background: var(--color-btn-disabled-bg);
  color: var(--color-btn-disabled-text);
  cursor: not-allowed;
}
</style>