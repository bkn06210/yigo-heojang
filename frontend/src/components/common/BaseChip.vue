<template>
  <button
    class="base-chip"
    :class="{ active }"
    type="button"
    @click="$emit('click')"
  >
    <slot>
      {{ text }}
    </slot>
  </button>
</template>


<script setup>
defineProps({
  /**
   * 기본 표시 텍스트
   */
  text: {
    type: String,
    default: ''
  },

  /**
   * 선택 상태
   */
  active: {
    type: Boolean,
    default: false
  }
})


defineEmits([
  'click'
])
</script>


<style scoped>
.base-chip {
  display: inline-flex;

  align-items: center;
  justify-content: center;

  height: 32px;

  padding: 0 var(--space-sm);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-full);

  background: var(--color-surface);

  color: var(--color-text-secondary);

  font-size: var(--font-sm);
  font-weight: var(--font-medium);

  cursor: pointer;

  transition: var(--transition-fast);
}


.base-chip:hover {
  border-color: var(--color-primary);
}


.base-chip.active {
  border-color: var(--color-filter-active-bg);

  background: var(--color-filter-active-bg);

  color: var(--color-filter-active-text);
}
</style>