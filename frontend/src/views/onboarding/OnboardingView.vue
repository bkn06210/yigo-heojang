<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import Page1Illustration from '@/components/onboarding/Page1Illustration.vue'
import Page2Illustration from '@/components/onboarding/Page2Illustration.vue'
import Page3Illustration from '@/components/onboarding/Page3Illustration.vue'

const router = useRouter()

const currentPage = ref(0)
const dragging = ref(false)
const drag = ref(0)
let startX = 0

const pages = [
  {
    label: '01 · 카드 관리',
    title: '보유한 카드를\n언제든 조회하고 관리하세요',
    description: '모든 카드의 정보를 한곳에서 확인하고 별칭으로 구분해 관리합니다.',
    component: Page1Illustration,
    accentColor: 'mint'
  },
  {
    label: '02 · AI 금융 비서',
    title: '물어보면\n바로 도와드립니다',
    description: '이번 달 소비 분석부터 카드 추천까지, 당신의 금융 질문에 즉시 답변합니다.',
    component: Page2Illustration,
    accentColor: 'mint'
  },
  {
    label: '03 · 포인트 관리',
    title: '여러 포인트를\n한눈에 조회하세요',
    description: '카드별로 적립된 포인트와 혜택을 한곳에서 확인하고 효율적으로 관리합니다.',
    component: Page3Illustration,
    accentColor: 'amber'
  }
]

const trackX = computed(() => {
  const max = -2 * 372
  const raw = -currentPage.value * 372 + drag.value
  return Math.round(Math.max(max - 40, Math.min(40, raw)))
})

const trackTransition = computed(() => {
  return dragging.value ? 'none' : 'transform .46s cubic-bezier(.22,.61,.36,1)'
})

const dots = computed(() => {
  return [0, 1, 2].map(i => ({
    width: i === currentPage.value ? '20px' : '6px',
    background: i === currentPage.value
      ? 'var(--color-primary)'
      : 'var(--color-border)'
  }))
})

const ctaLabel = computed(() => {
  return currentPage.value === 2 ? '시작하기' : '다음'
})

const onDown = (e) => {
  dragging.value = true
  startX = e.clientX
  drag.value = 0
}

const onMove = (e) => {
  if (!dragging.value) return
  drag.value = e.clientX - startX
}

const onUp = () => {
  if (!dragging.value) return
  dragging.value = false

  const threshold = 60
  if (drag.value < -threshold && currentPage.value < 2) {
    currentPage.value++
  } else if (drag.value > threshold && currentPage.value > 0) {
    currentPage.value--
  }
  drag.value = 0
}

const next = () => {
  if (currentPage.value === 2) {
    goSignup()
    return
  }
  currentPage.value++
}

const skip = () => {
  currentPage.value = 2
}

const goSignup = () => {
  localStorage.setItem('onboardingDone', 'true')
  router.push('/auth/terms')
}
</script>

<template>
  <div class="onboarding">
    <!-- 스와이프 영역 -->
    <div
      class="swipe-area"
      @pointerdown="onDown"
      @pointermove="onMove"
      @pointerup="onUp"
      @pointercancel="onUp"
    >
      <div
        class="track"
        :style="{
          transform: `translate3d(${trackX}px, 0, 0)`,
          transition: trackTransition
        }"
      >
        <!-- 페이지 1 -->
        <div class="page">
          <div class="page-content">
            <div class="illustration-area">
              <component :is="pages[0].component"></component>
            </div>
            <div class="copy-block">
              <div class="label">{{ pages[0].label }}</div>
              <h1 class="title">{{ pages[0].title }}</h1>
              <p class="description">{{ pages[0].description }}</p>
            </div>
          </div>
        </div>

        <!-- 페이지 2 -->
        <div class="page">
          <div class="page-content">
            <div class="illustration-area">
              <component :is="pages[1].component"></component>
            </div>
            <div class="copy-block">
              <div class="label">{{ pages[1].label }}</div>
              <h1 class="title">{{ pages[1].title }}</h1>
              <p class="description">{{ pages[1].description }}</p>
            </div>
          </div>
        </div>

        <!-- 페이지 3 -->
        <div class="page">
          <div class="page-content">
            <div class="illustration-area">
              <component :is="pages[2].component"></component>
            </div>
            <div class="copy-block">
              <div class="label">{{ pages[2].label }}</div>
              <h1 class="title">{{ pages[2].title }}</h1>
              <p class="description">{{ pages[2].description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 하단 오버레이 바 -->
      <footer class="bottom-bar">
        <div class="dots">
          <div
            v-for="(dot, i) in dots"
            :key="i"
            class="dot"
            :style="{
              width: dot.width,
              background: dot.background,
              transition: 'width .3s, background .3s'
            }"
          ></div>
        </div>
        <div class="controls">
          <button class="skip-button" @click="skip">건너뛰기</button>
          <button class="cta-button" @click="next">{{ ctaLabel }}</button>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.onboarding {
  width: 100%;
  height: 100vh;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

[data-theme='dark'] .onboarding {
  background: #16130f;
}


.swipe-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  touch-action: pan-y;
  cursor: grab;
}

.swipe-area:active {
  cursor: grabbing;
}

.track {
  width: 1116px;
  height: 100%;
  display: flex;
  will-change: transform;
}

.page {
  width: 372px;
  height: 100%;
  padding: 16px 24px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.illustration-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  margin-bottom: var(--space-md);
}

.copy-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 104px;
}

.label {
  font: 600 12px 'Pretendard', sans-serif;
  color: var(--color-primary);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

[data-theme='dark'] .label {
  color: #86d9a8;
}

.title {
  font: 700 28px / 1.35 'Pretendard', sans-serif;
  color: var(--color-text-primary);
  letter-spacing: -0.01em;
  margin: 0;
  white-space: pre-line;
}

[data-theme='dark'] .title {
  color: #f4f1ea;
}

.description {
  font: 400 14px / 1.65 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
  margin: 0;
  white-space: pre-line;
}

[data-theme='dark'] .description {
  color: rgba(244, 241, 234, 0.55);
}

.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-lg) var(--space-xl);
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(to top, var(--color-bg) 60%, rgba(247, 248, 250, 0));
  z-index: 10;
}

[data-theme='dark'] .bottom-bar {
  background: linear-gradient(to top, #16130f 60%, rgba(22, 19, 15, 0));
}

.dots {
  display: flex;
  gap: 7px;
  align-items: center;
}

.dot {
  height: 7px;
  border-radius: 99px;
  flex-shrink: 0;
}

.controls {
  display: flex;
  align-items: center;
  gap: 14px;
}

.skip-button {
  border: none;
  background: none;
  font: 500 14px 'Pretendard', sans-serif;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 0;
  transition: opacity 0.2s;
}

[data-theme='dark'] .skip-button {
  color: rgba(244, 241, 234, 0.45);
}

.skip-button:active {
  opacity: 0.7;
}

.cta-button {
  padding: var(--space-md) var(--space-xl);
  border-radius: 99px;
  border: none;
  background: var(--color-btn-primary-start);
  font: 600 14px 'Pretendard', sans-serif;
  color: var(--color-btn-primary-text);
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}

[data-theme='dark'] .cta-button {
  background: #f4f1ea;
  color: #16130f;
}

.cta-button:active {
  opacity: 0.8;
}
</style>
