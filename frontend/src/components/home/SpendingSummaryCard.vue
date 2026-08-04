<script setup>
import BaseCard from '@/components/common/BaseCard.vue'
import Icon from '@/components/common/Icon.vue'

defineProps({
  count: {
    type: Number,
    default: 0
  },
  totalAmount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['open'])

const openList = () => {
  emit('open')
}
</script>

<template>
  <BaseCard clickable class="spending-summary-card" @click="openList">
    <div class="spending-icon">
      <Icon name="payment" size="sm" />
    </div>

    <div class="spending-info">
      <p class="spending-label">이번 달 소비내역</p>
      <p class="spending-value">
        <strong>{{ totalAmount.toLocaleString() }}원</strong>
        <span> · {{ count }}건</span>
      </p>
    </div>

    <span class="spending-arrow">→</span>
  </BaseCard>
</template>

<style scoped>
.spending-summary-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
}

.spending-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-primary-dark);
}

.spending-info {
  flex: 1;
  min-width: 0;
}

.spending-label {
  margin: 0 0 var(--space-xxs);
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
}

.spending-value {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-primary);
}

.spending-value strong {
  font-weight: var(--font-bold);
}

.spending-value span {
  color: var(--color-text-tertiary);
}

.spending-arrow {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
  font-size: var(--font-md);
}
</style>
