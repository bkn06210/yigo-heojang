<script setup>
import { computed, ref } from 'vue'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Pagination } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/pagination'
import BaseCard from '@/components/common/BaseCard.vue'
import AppButton from '@/components/common/AppButton.vue'

const props = defineProps({
  isLogin: {
    type: Boolean,
    default: false,
  },
  cards: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['click-card', 'click-more'])

// 캐러셀에는 최대 3개까지만 노출
const displayCards = computed(() => props.cards.slice(0, 3))

// 슬롯은 세로인데 카드 도안은 대부분 가로다. 그대로 넣으면 가운데 띠처럼 들어가고 위아래가 빈다.
// 가로 도안만 세워서 슬롯을 채우고, 이미 세로인 도안은 건드리지 않는다.
// 캐러셀에 카드가 여러 장이라 카드별로 판정 결과를 들고 있어야 한다.
const landscapeCardIds = ref(new Set())

const handleCardImageLoad = (event, cardId) => {
  const image = event.currentTarget

  if (image.naturalWidth > image.naturalHeight) {
    landscapeCardIds.value.add(cardId)
  }
}

// 도넛 둘레(반지름 45 기준, 2 * PI * 45)
const DONUT_CIRCUMFERENCE = 282.74

const donutDashArray = (rate) => {
  const safeRate = rate ?? 0
  const filled = (safeRate / 100) * DONUT_CIRCUMFERENCE
  return `${filled.toFixed(2)} ${DONUT_CIRCUMFERENCE}`
}
</script>

<template>
  <BaseCard class="card-summary my-card">
    <div v-if="!props.isLogin" class="empty-content">
      <p>
        로그인하면
        <br />
        내 카드 혜택과 실적을 확인할 수 있어요.
      </p>
    </div>

    <div v-else-if="!displayCards.length" class="empty-content">
      <p>등록된 카드가 없습니다.</p>
      <AppButton
        text="카드 등록"
        type="primary"
        size="small"
        @click="emit('click-more')"
      />
    </div>

    <template v-else>
      <!-- 카드 캐러셀 (최대 3개) -->
      <Swiper
        :modules="[Pagination]"
        :slides-per-view="1"
        :space-between="16"
        :pagination="
          displayCards.length > 1
            ? { clickable: true, el: '.my-card-pagination' }
            : false
        "
        class="my-card-swiper"
      >
        <SwiperSlide v-for="card in displayCards" :key="card.id">
          <div class="card-content" @click="emit('click-card', card)">
            <!-- 카드 이미지 (고정 너비, flex 축소 안 됨) -->
            <div class="card-image">
              <img
                v-if="card.image"
                :src="card.image"
                alt="카드 이미지"
                :class="{ landscape: landscapeCardIds.has(card.id) }"
                @load="handleCardImageLoad($event, card.id)"
              />
              <div v-else class="image-placeholder">CARD</div>
            </div>

            <!-- 도넛 차트 (남은 공간에서 좌우 중앙 정렬) -->
            <div class="donut-section">
              <div class="donut-chart">
                <svg viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="45" fill="none" stroke="#E8E8E8" stroke-width="8"/>
                  <circle cx="50" cy="50" r="45" fill="none" stroke="var(--color-card-donut)" stroke-width="8"
                    :stroke-dasharray="donutDashArray(card.achievementRate)" stroke-dashoffset="0" stroke-linecap="round"
                    transform="rotate(-90 50 50)"/>
                </svg>
                <div class="donut-text">{{ card.achievementRate ?? 0 }}%</div>
              </div>
            </div>

            <!-- 실적 정보 (고정 너비, 오른쪽 정렬) -->
            <div class="achievement-info">
              <p class="label">이번 달 실적 금액</p>
              <p class="amount">30/50만원</p>
              <p class="remain-label">남은 실적</p>
              <p class="remain-amount">30만원</p>
              <p class="benefit-label">남은 혜택</p>
              <p class="benefit-text">교통 10% 할인</p>
            </div>
          </div>
        </SwiperSlide>
      </Swiper>

      <!-- 페이지네이션 인디케이터 (카드가 2개 이상일 때만) -->
      <div v-if="displayCards.length > 1" class="my-card-pagination"></div>
    </template>
  </BaseCard>
</template>

<style scoped>
.my-card {
  /* 높이를 고정하지 않고 내용에 맞춰 자동으로 늘어나게 함 (잘림 방지) */
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: var(--space-sm);
}

.empty-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: var(--space-sm);
  text-align: center;
  color: var(--color-text-secondary);
}

.my-card-swiper {
  width: 100%;
  overflow: hidden;
}

.my-card-pagination {
  display: flex;
  gap: var(--space-xs);
  justify-content: center;
  margin-top: var(--space-xs);
}

.my-card-pagination :deep(.swiper-pagination-bullet) {
  width: 6px;
  height: 6px;
  background: var(--color-border);
  opacity: 1;
  transition: var(--transition-fast);
}

.my-card-pagination :deep(.swiper-pagination-bullet-active) {
  width: 18px;
  background: var(--color-primary);
  border-radius: var(--radius-full);
}

.card-content {
  /* flex row 3분할: 카드(고정) - 도넛(남는 공간에서 중앙) - 텍스트(고정, 오른쪽) */
  /* BaseCard가 이미 사방에 var(--space-lg) 패딩을 주므로 여기서는 추가 좌우 패딩을 두지 않음 (겹치면 좌우 여백이 달라짐) */
  display: flex;
  align-items: center;
  cursor: pointer;
  width: 100%;
  min-width: 0;
}

.card-image {
  /* 카드 너비 조절 (고정 크기, 줄어들지 않음) */
  width: 100px;
  /* 카드 높이 조절 (가로세로 비율 1:1.5 유지) */
  height: 145px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  /* 세운 도안이 슬롯 밖으로 삐져나가지 않도록 자르고, 안에서 가운데 정렬한다 */
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 가로 도안은 폭/높이를 뒤집고 90도 세운다.
   145x100 을 회전하면 화면상 100x145 가 되어 슬롯에 딱 맞는다. */
.card-image img.landscape {
  width: 145px;
  height: 100px;
  max-width: none;
  flex-shrink: 0;
  transform: rotate(90deg);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-point-bg);
  color: var(--color-point-icon);
  border-radius: var(--radius-md);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
}

.donut-section {
  /* 남은 공간을 전부 차지하면서 그 안에서 도넛을 좌우 중앙 정렬 */
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
}

.donut-chart {
  position: relative;
  /* 도넛 크기 조절 (width = height) */
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.donut-chart svg {
  width: 100%;
  height: 100%;
}

.donut-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-donut-text);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  text-align: center;
  line-height: 1;
}

.achievement-info {
  /* 고정 너비로 오른쪽에 자연스럽게 배치 (flex 마지막 요소) */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
}

.label {
  /* 라벨 텍스트 크기 조절: var(--font-sm) = 14px */
  margin: 0 0 var(--space-xxs) 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-medium);
}

.amount {
  /* 금액 텍스트 크기 조절: var(--font-lg) = 18px */
  margin: 0 0 var(--space-sm) 0;
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.remain-label {
  /* 남은 실적 라벨 크기 조절 */
  margin: 0 0 var(--space-xxs) 0;
  font-size: var(--font-sm);
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

.remain-amount {
  /* 남은 실적 금액 크기 조절 */
  margin: 0 0 var(--space-sm) 0;
  font-size: var(--font-md);
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
}

.benefit-label {
  /* 남은 혜택 라벨 크기 조절 */
  margin: 0 0 var(--space-xxs) 0;
  font-size: var(--font-sm);
  color: var(--color-text-tertiary);
  font-weight: var(--font-medium);
}

.benefit-text {
  /* 남은 혜택 텍스트 크기 조절 */
  margin: 0;
  font-size: var(--font-md);
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
}
</style>
