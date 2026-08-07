<<<<<<< HEAD
﻿<script setup>
=======
<script setup>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import PasswordInput from '@/components/common/PasswordInput.vue';
import AppButton from '@/components/common/AppButton.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';
<<<<<<< HEAD
import { useToast } from '@/composables/useToast';

const router = useRouter();
const { showToast } = useToast();
=======

const router = useRouter();
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const form = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const isModalOpen = ref(false);

const isFormValid = computed(() => {        // 폼 입력값이 모두 유효한지 검사
  return (
    form.value.currentPassword.trim() !== '' &&
    form.value.newPassword.trim() !== '' &&
    form.value.confirmPassword.trim() !== '' &&
    form.value.newPassword === form.value.confirmPassword
  );
});

const goBack = () => {
  router.go(-1);
};

const openVerifyModal = () => {
  if (!isFormValid.value) return;
  isModalOpen.value = true;
};

const handleVerifySuccess = () => {
  isModalOpen.value = false;       // 인증 팝업 노출 여부 상태
<<<<<<< HEAD
  showToast('success', '비밀번호가 안전하게 변경되었습니다.');
=======
  alert('비밀번호가 안전하게 변경되었습니다.');
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  router.go(-1);
};
</script>

<template>
  <div class="password-change-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="비밀번호 변경" @back="goBack" />

    <div class="content-container">
<<<<<<< HEAD
      <h1 class="page-title">비밀번호 변경</h1>

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
      <div class="form-section">
        <!-- 1. 현재 비밀번호 입력 -->
        <div class="input-group">
          <label class="input-label">현재 비밀번호</label>
          <PasswordInput v-model="form.currentPassword" placeholder="현재 비밀번호를 입력해주세요" />
        </div>

        <!-- 2. 새 비밀번호 입력 -->
        <div class="input-group">
          <label class="input-label">새 비밀번호</label>
          <PasswordInput v-model="form.newPassword" placeholder="영문, 숫자, 특수문자 포함 8자 이상" />
        </div>

        <!-- 3. 새 비밀번호 확인 입력 -->
        <div class="input-group">
          <label class="input-label">새 비밀번호 확인</label>
          <PasswordInput v-model="form.confirmPassword" placeholder="새 비밀번호를 한번 더 입력해주세요" />
        </div>
      </div>

      <!-- 하단 변경하기 버튼 -->
      <div class="footer-button-area">
        <AppButton :disabled="!isFormValid" @click="openVerifyModal">변경하기</AppButton>
      </div>
    </div>

    <!-- 비밀번호 변경 인증용 팝업 (AuthVerifyModal 재사용) -->
    <AuthVerifyModal 
      v-if="isModalOpen" 
      @close="isModalOpen = false" 
      @verify-success="handleVerifySuccess" 
    />
  </div>
</template>

<style scoped>
.password-change-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
<<<<<<< HEAD
  background-color: var(--color-bg);
=======
  background-color: #f9f9f9;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.content-container {
  flex: 1;
<<<<<<< HEAD
  padding: var(--space-lg);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}

/* 타이틀 (Display Typography) */
.page-title {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xl);
}

/* 입력 폼 (Soft Glassmorphism) */
.form-section {
  border-radius: var(--radius-lg);
  padding: var(--space-lg);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.08) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.15);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .form-section {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.22);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.input-group {
  margin-bottom: var(--space-md);
=======
  padding: 20px;
  padding-bottom: 100px;
}

.form-section {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  padding: 20px;
}

.input-group {
  margin-bottom: 20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}
.input-group:last-child {
  margin-bottom: 0;
}

.input-label {
  display: block;
<<<<<<< HEAD
  font-size: var(--font-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}
=======
  font-size: 0.9rem;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
} 
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

.footer-button-area {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
<<<<<<< HEAD
  padding: var(--space-sm) var(--space-md);
  box-sizing: border-box;

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5) 0%, var(--color-surface) 40%);
  border-top: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

[data-theme="dark"] .footer-button-area {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, var(--color-surface) 40%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.3);
}
</style>
=======
  padding: 15px 20px;
  background-color: white;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  box-sizing: border-box;
}
</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
