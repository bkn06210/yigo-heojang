<script setup>
import BaseCard from '@/components/common/BaseCard.vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps({
  count: {
    type: Number,
    default: 0
  },
  totalAmount: {
    type: Number,
    default: 0
  },
  hasCard: {
    type: Boolean,
    default: true
  },
  isLoggedIn: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['open', 'go-card-list'])

const openList = () => {
  if (props.hasCard) {
    emit('open')
  } else {
    emit('go-card-list')
  }
}
</script>

<template>
  <BaseCard clickable class="spending-summary-card" @click="openList">
    <div class="spending-icon">
      <Icon name="payment" size="sm" />
    </div>

    <div class="spending-info">
      <p class="spending-label">이번 달 카드 상세 내역</p>
      <p v-if="!isLoggedIn" class="spending-value empty-state">
        <strong>이용 내역 없음</strong>
      </p>
      <p v-else-if="hasCard" class="spending-value">
        <strong>{{ totalAmount.toLocaleString() }}원</strong>
        <span> · {{ count }}건</span>
      </p>
      <p v-else class="spending-value empty-state">
        <strong>등록된 카드가 없어요</strong>
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

.empty-state {
  color: var(--color-text-secondary) !important;
}

.empty-state strong {
  color: var(--color-text-secondary) !important;
}
</style>
