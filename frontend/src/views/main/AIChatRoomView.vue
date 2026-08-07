<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import ChatMessage from '@/components/chat/ChatMessage.vue';
import QuickQuestion from '@/components/chat/QuickQuestion.vue';
import Icon from '@/components/common/Icon.vue';

const router = useRouter();

// TODO
// POST /ai/chat
// GET /ai/chat/history
// 추천 질문 API 연결

// 임시 메세지
const messages = ref([
  {
    id: 1,
    sender: 'ai',
    message: '안녕하세요!\n무엇을 도와드릴까요?',
    time: '오후 3:20',
  },
]);

// 추천 질문
const quickQuestions = ref([
  '추천 카드 알려줘',
  '이번 달 소비 분석',
  '혜택 리포트',
  '카드 사용 내역',
]);

// 입력창
const inputMessage = ref('');

// 임시 전송
const sendMessage = () => {
  if (!inputMessage.value.trim()) {
    return;
  }

  messages.value.push({
    id: Date.now(),

    sender: 'user',

    message: inputMessage.value,

    time: '방금',
  });

  inputMessage.value = '';
};

// 추천 질문 클릭
const selectQuestion = (question) => {
  inputMessage.value = question;
};
</script>

<template>
  <div class="chat-view">
    <PageHeader title="AI 금융 비서" @back="router.back()" />
    <PageHeader title="AI 금융 비서" />

    <!-- 채팅 영역 -->
    <section class="chat-area">
      <ChatMessage
        v-for="message in messages"
        :key="message.id"
        :message="message"
      />
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
        placeholder="메시지를 입력하세요."
        @keyup.enter="sendMessage"
      />

      <button type="button" @click="sendMessage"><Icon name="send" size="sm" /></button>
      <button type="button" @click="sendMessage">➤</button>
    </section>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;

  flex-direction: column;

  height: 100vh;

  background: var(--color-bg);
  background: #f5f6fa;
}

.chat-area {
  flex: 1;

  overflow-y: auto;

  display: flex;

  flex-direction: column;

  gap: var(--space-md);

  padding: var(--space-md);
  gap: 16px;

  padding: 20px;
}

.quick-question-area {
  display: flex;

  gap: var(--space-xs);

  overflow-x: auto;

  padding: var(--space-xs) var(--space-md);
  gap: 10px;

  overflow-x: auto;

  padding: 12px 16px;
}

.quick-question-area::-webkit-scrollbar {
  display: none;
}

.input-area {
  display: flex;

  gap: var(--space-sm);

  padding: var(--space-md);

  border-top: 1px solid var(--color-border);

  background: var(--color-surface);
  gap: 12px;

  padding: 16px;

  border-top: 1px solid #e5e7eb;

  background: white;
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
  border: 1px solid #ddd;

  border-radius: 24px;

  padding: 12px 16px;

  outline: none;
}

.input-area button {
  width: 44px;

  border: none;

  border-radius: var(--radius-full);

  background: var(--color-primary);

  color: var(--color-btn-primary-text);
  border-radius: 50%;

  background: #4f46e5;

  color: white;

  cursor: pointer;
}
</style>
