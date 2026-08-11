<script setup>
import { computed, ref } from 'vue'
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

let touchStartX = 0
let touchStartY = 0
let isMoving = false

const cardTranslateX = ref(0)
const showSwipeEffect = ref(false)
const isAnimating = ref(false)
const isLandscapeImage = ref(false)

const handleCardImageLoad = (event) => {
  const image = event.currentTarget
  isLandscapeImage.value = image.naturalWidth > image.naturalHeight
}

const handleTouchStart = (e) => {
  if (isAnimating.value) return
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
  isMoving = false
  cardTranslateX.value = 0
  showSwipeEffect.value = false
}

const handleTouchMove = (e) => {
  if (isAnimating.value) return
  const currentX = e.touches[0].clientX
  const deltaX = touchStartX - currentX

  if (Math.abs(deltaX) > 10) {
    isMoving = true
    cardTranslateX.value = -deltaX * 0.5
  }
}

const handleTouchEnd = (e) => {
  if (!isMoving) return

  const touchEndX = e.changedTouches[0].clientX
  const touchEndY = e.changedTouches[0].clientY

  const deltaX = touchStartX - touchEndX
  const deltaY = Math.abs(touchStartY - touchEndY)

  if (deltaX > 40 && deltaY < 80) {
    console.log('Toggling pin for card:', props.card.id)
    isAnimating.value = true
    showSwipeEffect.value = true
    e.stopPropagation()

    emit('toggle-pin', props.card.id)

    setTimeout(() => {
      cardTranslateX.value = 0
      showSwipeEffect.value = false
      isAnimating.value = false
    }, 400)
  } else {
    isAnimating.value = true
    cardTranslateX.value = 0
    showSwipeEffect.value = false

    setTimeout(() => {
      isAnimating.value = false
    }, 300)
  }
}

const goDetail = () => {
  router.push(`/cards/${props.card.id}`)
}

const maskedCardNumber = computed(() => {
  if (!props.card.cardNumber) return ''
  const last4 = props.card.cardNumber.slice(-4)
  return `${last4.slice(0, 3)}*`
})
</script>

<template>
  <div class="card-item" @click="goDetail" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd" style="cursor: pointer;">
    <BaseCard :class="{ 'swipe-effect': showSwipeEffect }">
      <div class="card-container" :style="{ transform: `translateX(${cardTranslateX}px)` }" :class="{ 'animating': isAnimating }">
      <!-- 카드 이미지 -->
      <template v-if="card.image">
        <div class="card-image-frame">
          <img
            :src="card.image"
            :alt="card.name"
            class="card-image"
            :class="{ landscape: isLandscapeImage }"
            @load="handleCardImageLoad"
          />
        </div>
      </template>
      <template v-else>
        <div class="card-image-frame image-placeholder">CARD</div>
      </template>

      <!-- 카드 정보 -->
      <div class="card-info">
        <div class="card-name">{{ card.name }}</div>
        <div class="card-details">
          <div class="card-company">{{ card.company }} 본인 {{ maskedCardNumber }}</div>
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
  </div>
</template>

<style scoped>
.card-item {
  touch-action: pan-y;
}

.card-container {
  display: flex;
  flex-direction: row;
  gap: var(--space-sm);
  width: 100%;
  align-items: center;
  transition: transform 0.2s ease;
}

.card-container.animating {
  transition: transform 0.3s ease;
}

:deep(.swipe-effect) {
  background-color: var(--color-primary) !important;
  opacity: 0.95;
  transition: all 0.3s ease;
}

.card-image-frame {
  width: 56px;
  height: 88px;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.card-image.landscape {
  width: 88px;
  height: 56px;
  max-width: none;
  flex-shrink: 0;
  transform: rotate(90deg);
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
  gap: 2px;
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

.card-details {
  display: flex;
  gap: var(--space-sm);
  align-items: center;
}

.card-company {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

.card-number {
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
