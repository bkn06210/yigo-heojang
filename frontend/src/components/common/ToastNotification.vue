<script setup>
import { computed, ref, onMounted } from 'vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'success', // success, error, info, warning
  },
  message: {
    type: String,
    required: true,
  },
  duration: {
    type: Number,
    default: 4000, // 4초
  },
})

const emit = defineEmits(['close'])

const isVisible = ref(true)

const handleClose = () => {
  isVisible.value = false
  emit('close')
}

const iconMap = {
  success: 'check',
  error: 'close',
  info: 'info',
  warning: 'alert',
}

const colorMap = {
  success: '#B8A03A',
  error: '#A84E68',
  info: '#B8A03A',
  warning: '#E6D94D',
}

const bgColorMap = {
  success: '#E3EDE7',
  error: '#F3E7EB',
  info: '#E3EDE7',
  warning: '#F8F3D4',
}

// Auto close on mount
onMounted(() => {
  if (props.duration) {
    setTimeout(handleClose, props.duration)
  }
})
</script>

<template>
  <transition name="toast-slide">
    <div v-if="isVisible" class="toast" :class="`toast-${type}`">
      <div class="toast-icon" :style="{ color: colorMap[type] }">
        <Icon :name="iconMap[type]" size="sm" />
      </div>
      <div class="toast-content">
        <p class="toast-message">{{ message }}</p>
      </div>
      <button class="toast-close" @click="handleClose"><Icon name="close" size="xs" /></button>
    </div>
  </transition>
</template>

<style scoped>
.toast {
  position: fixed;
  bottom: 94px;
  left: 50%;
  transform: translateX(-50%);
  max-width: 400px;
  width: 90%;
  padding: 12px 16px;
  background: #FAFBFC;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: var(--z-toast);
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.toast-success {
  background: #F9FBF9;
}

.toast-error {
  background: #FBF9FA;
}

.toast-info {
  background: #F9FBF9;
}

.toast-warning {
  background: #FFFDF9;
}

.toast-icon {
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-message {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #24242A;
  line-height: 1.4;
}

.toast-close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: #63666B;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease;
}

.toast-close:hover {
  color: #24242A;
}

/* Animations */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.3s ease;
}

.toast-slide-enter-from {
  transform: translateX(-50%) translateY(100px);
  opacity: 0;
}

.toast-slide-leave-to {
  transform: translateX(-50%) translateY(100px);
  opacity: 0;
}

/* Dark mode */
[data-theme="dark"] .toast {
  background: #2a2a2e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border-color: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .toast-success {
  background: #272e2a;
}

[data-theme="dark"] .toast-error {
  background: #2e272a;
}

[data-theme="dark"] .toast-info {
  background: #272e2a;
}

[data-theme="dark"] .toast-warning {
  background: #2e2a27;
}

[data-theme="dark"] .toast-message {
  color: #f0f0f0;
}

[data-theme="dark"] .toast-close {
  color: #83868B;
}

[data-theme="dark"] .toast-close:hover {
  color: #f0f0f0;
}
</style>
