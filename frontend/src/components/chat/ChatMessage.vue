<script setup>
import { computed } from 'vue';

const props = defineProps({
  message:{
    type:Object,
    required:true
  }
});

// AI 메시지 파싱: 정확한 금액 형식만 감지
const parseAIMessage = (text) => {
  const lines = text.split('\n');

  if (lines.length === 0) {
    return { amount: null, rest: text };
  }

  // 첫 줄이 "숫자원" 또는 "숫자,숫자원" 형식만 금액으로 취급
  const firstLine = lines[0].trim();
  const amountMatch = firstLine.match(/^[\d,]+원$/);

  if (amountMatch && lines.length > 1) {
    const amount = firstLine;
    const rest = lines.slice(1).join('\n').trim();
    return { amount, rest };
  }

  // 금액이 아니면 전체를 통계로 표시
  return { amount: null, rest: text };
};

// AI 응답 구조화
const aiData = computed(() => {
  if (props.message.sender !== 'ai') {
    return null;
  }
  return parseAIMessage(props.message.message);
});

// 마크다운 **텍스트** → <strong> 변환
const markdownToHtml = (text) => {
  return text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
};

// 사용자 메시지 포맷
const userText = computed(() => {
  if (props.message.sender === 'ai') {
    return null;
  }
  return props.message.message;
});

// AI 메시지 HTML 포맷 (마크다운 변환)
const aiRestHtml = computed(() => {
  return markdownToHtml(aiData.value?.rest || props.message.message);
});

</script>


<template>
  <div class="message" :class="message.sender">
    <!-- 사용자 메시지 -->
    <div v-if="message.sender === 'user'" class="bubble user-bubble">
      {{ userText }}
    </div>

    <!-- AI 메시지 -->
    <div v-else class="bubble ai-bubble">
      <div v-if="aiData?.amount" class="ai-amount">{{ aiData.amount }}</div>
      <div v-if="aiData?.amount" class="ai-divider"></div>
      <div class="ai-stats" v-html="aiRestHtml"></div>
    </div>

    <span class="time">{{ message.time }}</span>
  </div>
</template>


<style scoped>
.message {
  display: flex;
  flex-direction: column;
}

.message.user {
  align-items: flex-end;
}

.message.ai {
  align-items: flex-start;
}

.bubble {
  max-width: 75%;
  padding: 16px;
  border-radius: var(--radius-lg);
  word-break: break-word;
}

/* 사용자 버블 */
.user-bubble {
  background: var(--color-primary);
  color: #24242A;
  font: 500 14px / 1.5 'Pretendard', sans-serif;
}

[data-theme='dark'] .user-bubble {
  background: #86d9a8;
  color: #16130f;
}

/* AI 버블 */
.ai-bubble {
  background: var(--color-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 8px rgba(36, 36, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  max-width: 100%;
  width: fit-content;
}

[data-theme='dark'] .ai-bubble {
  background: #211c16;
  border: 1px solid rgba(244, 241, 234, 0.09);
  box-shadow: none;
}

/* AI 금액 */
.ai-amount {
  font: 700 16px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
  line-height: 1.2;
  letter-spacing: -0.01em;
}

[data-theme='dark'] .ai-amount {
  color: #f4f1ea;
}

/* AI 구분선 */
.ai-divider {
  height: 1px;
  background: var(--color-border);
}

[data-theme='dark'] .ai-divider {
  background: rgba(244, 241, 234, 0.1);
}

/* AI 통계 */
.ai-stats {
  font: 400 12px / 1.6 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
}

[data-theme='dark'] .ai-stats {
  color: rgba(244, 241, 234, 0.55);
}

/* AI 통계의 강조 텍스트 */
.ai-stats strong {
  font-weight: 600;
  color: var(--color-text-primary);
}

[data-theme='dark'] .ai-stats strong {
  color: #f4f1ea;
}

/* 시간 레이블 */
.time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

[data-theme='dark'] .time {
  color: rgba(244, 241, 234, 0.45);
}

</style>