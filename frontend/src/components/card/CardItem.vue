<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { maskCardNumber } from '@/utils/card'
import BaseCard from '@/components/common/BaseCard.vue'
import Icon from '@/components/common/Icon.vue'
import { useRouter } from 'vue-router';

import { maskCardNumber } from '@/utils/card';

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

const handleTouchStart = (e) => {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
  isMoving = false
}

const handleTouchMove = (e) => {
  const deltaX = touchStartX - e.touches[0].clientX
  if (Math.abs(deltaX) > 10) {
    isMoving = true
  }
}

const handleTouchEnd = (e) => {
  if (!isMoving) return

  const touchEndX = e.changedTouches[0].clientX
  const touchEndY = e.changedTouches[0].clientY

  const deltaX = touchStartX - touchEndX
  const deltaY = Math.abs(touchStartY - touchEndY)

  // 오른쪽에서 왼쪽으로 스와이프 감지 (최소 40px, 수직 이동은 무시)
  console.log('Swipe detected:', { deltaX, deltaY })
  if (deltaX > 40 && deltaY < 80) {
    console.log('Toggling pin for card:', props.card.id)
    e.stopPropagation()
    e.preventDefault()
    emit('toggle-pin', props.card.id)
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
    <BaseCard>
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
}

.card-image {
  width: 48px;
  height: 76px;
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
});

const emit = defineEmits([
  'toggle-pin',
]);

const router = useRouter();


// 카드 상세 이동
const goDetail = () => {
  router.push(`/cards/${props.card.id}`);
};


// 고정 버튼 클릭
const handlePinClick = () => {
  emit('toggle-pin', props.card.id);
};
</script>


<template>
  <div
    class="card-item"
    @click="goDetail"
  >

    <!-- 카드 정보 -->
    <div class="card-left">

      <img
        :src="card.image"
        :alt="card.name"
        class="card-image"
      />


      <div class="card-info">

        <!-- 카드명 -->
        <div class="card-name">
          {{ card.name }}
        </div>


        <!-- 카드사 -->
        <div class="company">
          {{ card.company }}
        </div>


        <!-- 카드번호 -->
        <div class="number">

          {{ card.owner }}

          {{ maskCardNumber(card.cardNumber) }}

        </div>

      </div>

    </div>



    <!-- 고정 버튼 -->
    <button
      class="pin-button"
      @click.stop="handlePinClick"
    >
      {{ card.pinned ? '📌' : '📍' }}
    </button>


  </div>
</template>



<style scoped>

.card-item {

  display:flex;

  justify-content:space-between;

  align-items:center;

  padding:16px 0;

  cursor:pointer;

}


.card-left {

  display:flex;

  align-items:center;

  gap:16px;

}



.card-image {

  width:72px;

  height:auto;

  border-radius:10px;

}



.card-info {

  display:flex;

  flex-direction:column;

  gap:4px;

}



.card-name {

  font-size:15px;

  font-weight:700;

}



.company {

  font-size:13px;

  color:#666;

}



.number {

  font-size:13px;

  color:#888;

}



.pin-button {

  background:none;

  border:none;

  font-size:20px;

  cursor:pointer;

}


</style>
