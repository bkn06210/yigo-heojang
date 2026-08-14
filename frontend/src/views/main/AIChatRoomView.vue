<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import ChatMessage from '@/components/chat/ChatMessage.vue';
import QuickQuestion from '@/components/chat/QuickQuestion.vue';
import Icon from '@/components/common/Icon.vue';

// 챗봇 질의 API
import { askChat } from '@/api/chatApi';

const router = useRouter();

// TODO
// GET /ai/chat/history — 지난 대화 불러오기
// 추천 질문 API 연결

const messages = ref([
  {
    id: 1,
    sender: 'ai',
    message: '안녕하세요!\n무엇을 도와드릴까요?',
    time: '오후 3:20',
  },
]);

// 렌더링할 메시지 (중복 제거)
const displayMessages = computed(() => {
  const seen = new Set();
  return messages.value.filter(msg => {
    const key = `${msg.sender}:${msg.message}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
});

// 추천 질문
const quickQuestions = ref([
  '추천 카드 알려줘',
  '이번 달 소비 분석',
  '혜택 리포트',
  '카드 사용 내역',
]);

// 입력창
const inputMessage = ref('');

// 답변 대기 상태
// LLM 호출이 두 번 들어가 응답이 수 초 걸린다. 그동안 중복 전송을 막고 상태를 보여준다.
const isLoading = ref(false);

// 되물음 맥락
// 서버가 되물으면서 pendingContext를 함께 내려준다. 다음 질문에 그대로 실어 보내야
// 앞 대화가 이어진다. 내용을 해석하거나 고치지 않는다 — 주제가 바뀌면 서버가 알아서 버린다.
const pendingContext = ref(null);

const pushMessage = (sender, message) => {
  // 마지막 메시지와 동일하면 추가하지 않음 (중복 방지)
  const lastMsg = messages.value[messages.value.length - 1];
  if (lastMsg && lastMsg.sender === sender && lastMsg.message === message) {
    return;
  }

  messages.value.push({
    id: Date.now() + Math.random(),
    sender,
    message,
    time: '방금',
  });
};

// 텍스트 포맷팅
const formatAnswer = (text) => {
  let formatted = text;

  // 1. 날짜 형식 변환: "2026-08" → "8월"
  formatted = formatted.replace(/\d{4}-(\d{2})/g, (match, month) => {
    const monthNum = parseInt(month);
    return `${monthNum}월`;
  });

  // 2. 제목 패턴: [텍스트] → **텍스트** (볼드 처리)
  formatted = formatted.replace(/\[([^\]]+)\]/g, '**$1**');

  // 3. 키워드 강조: "받은 혜택", "부문별" 등을 **텍스트** 처리
  const keywords = ['받은 혜택', '부문별', '추천', '혜택'];
  keywords.forEach(keyword => {
    const regex = new RegExp(`(^|\\n)(${keyword})`, 'g');
    formatted = formatted.replace(regex, '$1**$2**');
  });

  // 4. 소제목 아이콘 제거
  formatted = formatted.replace(/^💳 /m, '');
  formatted = formatted.replace(/^📋 /m, '');

  // 5. 개행 정리 (연속된 빈 줄 제거)
  formatted = formatted.replace(/\n{3,}/g, '\n\n');

  return formatted;
};

// 답변을 빈 줄 기준으로 분할 (중복 제거, 단순화)
const splitAnswer = (answer) => {
  // 연속된 빈 줄을 하나로 정리하고 분할
  const parts = answer.split(/\n\n+/).filter(part => part.trim());
  return parts.length > 0 ? parts : [answer];
};

// 에러를 사용자 문장으로 바꾼다.
// 본문이 success:false로 온 경우(api 모듈이 던짐)와 HTTP 에러로 온 경우 code 자리가 다르다.
// 응답 본문을 먼저 본다 — AxiosError 는 자체 code('ERR_BAD_RESPONSE' 등)를 갖고 있어서
// error.code 를 먼저 보면 서버가 준 코드가 가려진다.
const errorMessage = (error) => {
  const code = error.response?.data?.code || error.code;

  if (code === 'CHATBOT_UNAVAILABLE') {
    return '지금 AI 비서가 자리를 비웠어요. 잠시 후 다시 물어봐 주세요.';
  }

  if (code === 'INPUT_INVALID') {
    return error.response?.data?.message || '질문을 다시 입력해 주세요.';
  }

  // 응답 자체가 없으면 네트워크 문제이거나 답변이 너무 오래 걸린 경우다.
  if (!error.response) {
    return '답변을 받아오지 못했어요. 잠시 후 다시 시도해 주세요.';
  }

  return error.response?.data?.message || '답변을 준비하지 못했어요.';
};

// 전송
// POST /api/chat
const sendMessage = async () => {
  if (!inputMessage.value.trim()) {
    return;
  }

  // 답변을 기다리는 동안은 다음 질문을 받지 않는다.
  // 되물음 맥락이 있는 상태에서 두 질문이 겹치면 어느 쪽 답인지 알 수 없다.
  if (isLoading.value) {
    return;
  }

  pushMessage('user', inputMessage.value);

  const question = inputMessage.value;

  inputMessage.value = '';

  isLoading.value = true;

  try {
    const result = await askChat(question, pendingContext.value);

    // 답변 전체를 한 메시지로 추가 (분할 제거, 중복 방지)
    const formatted = formatAnswer(result.answer);
    pushMessage('ai', formatted);

    // 되물었으면 맥락을 들고 있다가 다음 질문에 실어 보낸다.
    // 되묻지 않았으면 대화가 끝난 것이므로 비운다.
    pendingContext.value = result.pendingContext ?? null;
  } catch (error) {
    console.error('챗봇 에러:', error);

    pushMessage('ai', errorMessage(error));

    // 실패한 턴의 맥락을 남겨두면 다음 질문이 엉뚱한 맥락으로 해석된다.
    pendingContext.value = null;
  } finally {
    isLoading.value = false;
  }
};

// 추천 질문 클릭
// 입력창에 채우기만 하고 전송은 사용자가 누른다 (원래 동작 유지).
const selectQuestion = (question) => {
  if (isLoading.value) {
    return;
  }

  inputMessage.value = question;
};
</script>

<template>
  <div class="chat-view">
    <PageHeader title="AI 금융 비서" @back="router.back()" />

    <!-- 채팅 영역 -->
    <section class="chat-area">
      <ChatMessage
        v-for="message in displayMessages"
        :key="message.id"
        :message="message"
      />

      <!-- 답변 대기 표시 (LLM 호출이 들어가 수 초 걸린다) -->
      <p v-if="isLoading" class="typing">답변을 준비하고 있어요…</p>
    </section>

    <!-- 추천 질문 -->
    <section class="quick-question-area">
      <QuickQuestion
        v-for="question in quickQuestions"
        :key="question"
        :question="question"
        @click="selectQuestion(question)"
      />
    </section>

    <!-- 입력 -->
    <section class="input-area">
      <input
        v-model="inputMessage"
        type="text"
        :placeholder="isLoading ? '답변을 기다리는 중이에요' : '메시지를 입력하세요.'"
        :disabled="isLoading"
        @keyup.enter="sendMessage"
      />

      <button type="button" :disabled="isLoading" @click="sendMessage"><Icon name="send" size="sm" /></button>
    </section>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;

  flex-direction: column;

  height: 100vh;

  background: var(--color-bg);
}

.chat-area {
  flex: 1;

  overflow-y: auto;

  display: flex;

  flex-direction: column;

  gap: var(--space-lg);

  padding: var(--space-lg) var(--space-md);

  max-width: 480px;

  margin: 0 auto;

  width: 100%;

  box-sizing: border-box;
}

.typing {
  align-self: flex-start;

  margin: 0;

  font-size: var(--font-sm);

  color: var(--color-text-secondary);
}

.quick-question-area {
  display: flex;

  gap: var(--space-xs);

  overflow-x: auto;

  padding: var(--space-xs) var(--space-md);
}

.quick-question-area::-webkit-scrollbar {
  display: none;
}

.input-area {
  display: flex;

  gap: var(--space-sm);

  padding: var(--space-md) var(--space-lg);

  border-top: 1px solid var(--color-border);

  background: var(--color-surface);

  max-width: 480px;

  margin: 0 auto;

  width: 100%;

  box-sizing: border-box;
}

.input-area input {
  flex: 1;

  border: 1px solid var(--color-input-border);

  border-radius: var(--radius-full);

  padding: var(--space-sm) var(--space-md);

  outline: none;
  color: var(--color-text-primary);

  background: var(--color-surface);
}

.input-area input::placeholder {
  color: var(--color-text-tertiary);
}

.input-area button {
  width: 44px;

  border: none;

  border-radius: var(--radius-full);

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  cursor: pointer;
}
</style>
