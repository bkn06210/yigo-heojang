<script setup>
import { computed } from 'vue'

const props = defineProps({
  id: {
    type: Number,
    required: true,
  },
  name: {
    type: String,
    required: true,
  },
  point: {
    type: Number,
    required: true,
  },
})

const formattedPoint = computed(() => props.point ? props.point.toLocaleString() : '0')

const emit = defineEmits(['click', 'select-point'])

const handleClick = () => {
  emit('click', props.id)
  emit('select-point', {
    id: props.id,
    name: props.name,
    point: props.point,
  })
}
</script>

<template>
  <div class="point-item" @click="handleClick">
    <span class="point-name">{{ name }}</span>

    <div class="point-right">
      <span class="point-value">{{ formattedPoint }}P</span>
      <span class="arrow">›</span>
    </div>
  </div>
</template>

<style scoped>
.point-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-xs);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition-fast);
}

.point-item:hover {
  background: rgba(255, 255, 255, 0.15);
}

[data-theme="dark"] .point-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.point-item:not(:last-child) {
  border-bottom: 1px solid var(--color-border);
}

.point-name {
  font-size: var(--font-sm);
  color: var(--color-text-primary);
}

.point-right {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.point-value {
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.arrow {
  color: var(--color-text-secondary);
  font-size: var(--font-lg);
}
</style>
