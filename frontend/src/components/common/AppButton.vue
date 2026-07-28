<script setup>

const emit = defineEmits(['click'])

defineProps({
  text: {
    type: String,
    required: true
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
    @click="$emit('click')"
  >
    {{ text }}
  </button>
</template>

<style scoped>
.app-button {
  border-radius: 12px;
  border: none;
  font-weight: 600;
  cursor: pointer;
}

/* 버튼 크기 */
.large {
  width: 100%;
  height: 52px;
  font-size: 16px;
}

.small {
  width: auto;
  height: 36px;
  padding: 0 14px;
  font-size: 14px;
}

/* 버튼 종류 */
.primary {
  background-color: #1d4ed8;
  color: white;
}

.secondary {
  background-color: #f1f1f1;
  color: #777;
}

.outline {
  background-color: white;
  border: 1px solid #ddd;
  color: #333;
}

.app-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>