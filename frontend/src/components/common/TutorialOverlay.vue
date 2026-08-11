<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useTutorialStore } from '@/stores/tutorialStore';

const tutorialStore = useTutorialStore();
const highlightRects = ref({});

const currentStep = computed(() => tutorialStore.getCurrentStep);

onMounted(() => {
  updateHighlights();
  window.addEventListener('resize', updateHighlights);
});

watch(() => tutorialStore.currentStep, () => {
  setTimeout(updateHighlights, 100);
});

const updateHighlights = () => {
  highlightRects.value = {};
  if (!currentStep.value) return;

  currentStep.value.highlights.forEach(highlight => {
    const element = document.getElementById(highlight.id);
    if (element) {
      const rect = element.getBoundingClientRect();
      highlightRects.value[highlight.id] = {
        top: rect.top,
        left: rect.left,
        width: rect.width,
        height: rect.height
      };
    }
  });
};

const handleNext = () => {
  tutorialStore.nextStep();
};

const handleSkip = () => {
  tutorialStore.skipTutorial();
};

const getHighlightStyle = (highlight) => {
  const rect = highlightRects.value[highlight.id];
  if (!rect) return {};

  return {
    position: 'fixed',
    top: rect.top - 8 + 'px',
    left: rect.left - 8 + 'px',
    width: rect.width + 16 + 'px',
    height: rect.height + 16 + 'px',
    border: '2px solid white',
    borderRadius: '12px',
    pointerEvents: 'none',
    zIndex: '9999'
  };
};

const getLabelPosition = (highlight) => {
  const rect = highlightRects.value[highlight.id];
  if (!rect) return {};

  const isTop = highlight.position === 'top';
  const top = isTop ? rect.top - 80 : rect.top + rect.height + 30;

  return {
    position: 'fixed',
    top: top + 'px',
    left: Math.max(12, rect.left - 40) + 'px',
    maxWidth: '280px',
    pointerEvents: 'none',
    zIndex: '10000'
  };
};
</script>

<template>
  <div v-if="tutorialStore.isActive" class="tutorial-overlay">
    <!-- 어두운 배경 오버레이 -->
    <div class="overlay-bg" @click="handleSkip"></div>

    <!-- 제목 -->
    <div v-if="currentStep" class="tutorial-header">
      <h2>{{ currentStep.title }}</h2>
    </div>

    <!-- Highlight 영역들과 라벨 -->
    <div v-if="currentStep">
      <template v-for="highlight in currentStep.highlights" :key="highlight.id">
        <!-- 강조 테두리 -->
        <div :style="getHighlightStyle(highlight)" class="highlight-border"></div>

        <!-- 라벨 -->
        <div :style="getLabelPosition(highlight)" class="label">
          <p>{{ highlight.text }}</p>
          <svg class="arrow" viewBox="0 0 20 20" width="20" height="20">
            <path d="M10 2 L10 14 M7 11 L10 14 L13 11" stroke="white" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </template>
    </div>

    <!-- 컨트롤 버튼 -->
    <div class="tutorial-controls">
      <button class="btn-skip" @click="handleSkip">건너뛰기</button>
      <button class="btn-next" @click="handleNext">다음</button>
    </div>
  </div>
</template>

<style scoped>
.tutorial-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  pointer-events: none;
}

.overlay-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  pointer-events: auto;
  z-index: 9998;
}

.highlight-border {
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.7);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.7), 0 0 8px 2px rgba(255, 255, 255, 0.3);
  }
  50% {
    box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.7), 0 0 16px 4px rgba(255, 255, 255, 0.5);
  }
}

.tutorial-header {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 16px 24px;
  border-radius: 12px;
  z-index: 10000;
  max-width: 320px;
  text-align: center;
  pointer-events: none;
}

.tutorial-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.label {
  background: rgba(0, 0, 0, 0.85);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-line;
  animation: fadeIn 0.3s ease-out;
}

.label p {
  margin: 0 0 8px 0;
}

.arrow {
  display: block;
  margin-top: 4px;
  opacity: 0.8;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.tutorial-controls {
  position: fixed;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 12px;
  z-index: 10000;
  pointer-events: auto;
}

.btn-skip {
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-skip:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
}

.btn-next {
  padding: 10px 20px;
  background: white;
  color: #1a1a1a;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-next:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

/* 다크 모드 */
@media (prefers-color-scheme: dark) {
  .tutorial-controls {
    gap: 12px;
  }
}
</style>
