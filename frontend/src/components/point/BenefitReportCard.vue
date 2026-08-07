<script setup>
import BaseCard from '@/components/common/BaseCard.vue'

defineProps({
  report: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open'])

const openDetail = () => {
  emit('open')
}
</script>

<template>
  <BaseCard clickable class="benefit-report-card" @click="openDetail">
    <p class="title">이번 달 받은 혜택</p>
    <strong class="amount">{{ report.totalBenefit.toLocaleString() }}원</strong>

    <div class="max-benefit">
      <span>최대 혜택 영역</span>
      <b>{{ report.maxCategory }}</b>
    </div>

    <p class="detail-button">자세히 보기 →</p>
  </BaseCard>
</template>

<style scoped>
.benefit-report-card {
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.18);
}

[data-theme="dark"] .benefit-report-card {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.07) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.26);
}

.title {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
}

.amount {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
}

.max-benefit {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-top: var(--space-md);
  font-size: var(--font-sm);
}

.max-benefit span {
  color: var(--color-text-secondary);
}

.max-benefit b {
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
}

.detail-button {
  text-align: right;
  margin: var(--space-md) 0 0;
  color: var(--color-primary-dark);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
}
</style>
