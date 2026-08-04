<script setup>
import { useRouter } from 'vue-router'
import { maskCardNumber } from '@/utils/card'
import BaseCard from '@/components/common/BaseCard.vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps({
  card: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['toggle-pin'])
const router = useRouter()

const goDetail = () => {
  router.push(`/cards/${props.card.id}`)
}

const handlePinClick = (e) => {
  e.stopPropagation()
  emit('toggle-pin', props.card.id)
}
</script>

<template>
  <BaseCard clickable class="card-item" @click="goDetail">
    <div class="card-container">
      <!-- 카드 이미지 -->
      <template v-if="card.image">
        <img
          :src="card.image"
          :alt="card.name"
          class="card-image"
        />
      </template>
      <template v-else>
        <div class="card-image image-placeholder">CARD</div>
      </template>

      <!-- 카드 정보 -->
      <div class="card-info">
        <div class="card-name">{{ card.name }}</div>
        <div class="card-company">
          {{ card.company }}
        </div>
        <!-- 달성도 프로그래스 바 -->
        <div class="progress-wrapper" v-if="card.achievementRate !== undefined">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: card.achievementRate + '%' }"></div>
          </div>
          <div class="progress-text">{{ card.achievementRate }}%</div>
        </div>
      </div>

      <!-- 우측 화살표 -->
      <div class="card-arrow">
        <span>›</span>
      </div>
    </div>
  </BaseCard>
</template>

<style scoped>
.card-container {
  display: flex;
  flex-direction: row;
  gap: var(--space-md);
  width: 100%;
  align-items: center;
}

.card-image {
  width: 60px;
  height: 95px;
  border-radius: var(--radius-md);
  object-fit: contain;
  background: var(--color-bg);
  flex-shrink: 0;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-point-bg);
  color: var(--color-point-icon);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  flex: 1;
  min-width: 0;
}

.card-name {
  font-size: var(--font-sm);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  word-break: break-word;
  line-height: 1.3;
}

.card-company {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

.card-arrow {
  color: var(--color-text-tertiary);
  font-size: var(--font-lg);
  flex-shrink: 0;
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-top: var(--space-xs);
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: var(--radius-full);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-semibold);
  white-space: nowrap;
}
</style>
