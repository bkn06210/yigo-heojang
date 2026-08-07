<<<<<<< HEAD
﻿<script setup>
=======
<script setup>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppButton from '@/components/common/AppButton.vue';
import ConfirmModal from '@/components/common/ConfirmModal.vue';
<<<<<<< HEAD
import { useToast } from '@/composables/useToast';

const router = useRouter();
const { showToast } = useToast();
=======

const router = useRouter();
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const agreements = ref({
  deleteData: false,
  noticeConfirm: false
});

const reasons = ref({
  unused: false,
  inconvenient: false,
  etc: false
});

<<<<<<< HEAD
const etcReason = ref('');

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
const isModalOpen = ref(false);

// 필수 체크박스가 모두 체크되어야 버튼 활성화
const isMandatoryChecked = computed(() => {
  return agreements.value.deleteData && agreements.value.noticeConfirm;
});

const goBack = () => {
  router.go(-1);
};

const openConfirmModal = () => {
  if (!isMandatoryChecked.value) return;
  isModalOpen.value = true;
};

const handleWithdrawal = () => {
  isModalOpen.value = false;
<<<<<<< HEAD
  const withdrawalData = {
    ...reasons.value,
    etcReasonDetail: reasons.value.etc ? etcReason.value : null
  };
  console.log('회원 탈퇴 완료, 선택된 사유:', withdrawalData);
  showToast('success', '회원 탈퇴가 정상적으로 처리되었습니다.');
  router.push('/auth/login');
=======
  console.log('회원 탈퇴 완료, 선택된 사유:', reasons.value);
  alert('회원 탈퇴가 정상적으로 처리되었습니다.');
  router.push('/login');
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
};
</script>


<template>
  <div class="withdrawal-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="회원 탈퇴" @back="goBack" />

    <div class="content-container">
      <!-- 안내 타이틀 및 문구 -->
      <div class="notice-section">
        <h2 class="notice-title">회원 탈퇴</h2>
        <p class="notice-subtitle">탈퇴 전 확인해주세요.</p>
      </div>

      <!-- 필수 체크박스 영역 -->
      <div class="card-box checkbox-group">
        <AppCheckbox v-model="agreements.deleteData" label="필수 보유 데이터 삭제" />
        <AppCheckbox v-model="agreements.noticeConfirm" label="필수 탈퇴 안내 확인" />
      </div>

      <!-- 탈퇴 사유 선택 (선택) -->
      <div class="card-box reason-section">
        <div class="section-label">탈퇴 사유 (선택)</div>
        <div class="reason-list">
          <AppCheckbox v-model="reasons.unused" label="사용하지 않음" />
          <AppCheckbox v-model="reasons.inconvenient" label="기능이 편리하지 않음" />
          <AppCheckbox v-model="reasons.etc" label="기타" />
<<<<<<< HEAD

          <!-- 기타 선택 시 입력 박스 -->
          <div v-if="reasons.etc" class="etc-input-section">
            <input
              v-model="etcReason"
              type="text"
              placeholder="탈퇴 사유를 입력해주세요"
              class="etc-input"
              maxlength="200"
            />
            <span class="input-counter">{{ etcReason.length }}/200</span>
          </div>
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
        </div>
      </div>

      <!-- 하단 탈퇴하기 버튼 -->
      <div class="footer-button-area">
        <AppButton :disabled="!isMandatoryChecked" @click="openConfirmModal">탈퇴하기</AppButton>
      </div>
    </div>

    <!-- 정말 탈퇴하겠냐는 확인 팝업 (ConfirmModal 재사용) -->
    <ConfirmModal 
      v-if="isModalOpen" 
      title="정말 탈퇴하시겠습니까?" 
      message="탈퇴 시 모든 정보가 삭제되며 복구할 수 없습니다."
      confirm-text="탈퇴"
      cancel-text="취소"
      @confirm="handleWithdrawal" 
      @close="isModalOpen = false" 
    />
  </div>
</template>

<style scoped>
.withdrawal-view {
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

.notice-section {
  margin-bottom: var(--space-xl);
  padding-left: var(--space-xxs);
}

/* 안내 타이틀 (Display Typography) */
.notice-title {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}

.notice-subtitle {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

/* 카드 영역 (Soft Glassmorphism, 중립 톤) */
.card-box {
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  margin-bottom: var(--space-sm);

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0.2) 100%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .card-box {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.05);
=======
  padding: 20px;
  padding-bottom: 100px;
}

.notice-section {
  margin-bottom: 20px;
  padding-left: 4px;
}

.notice-title {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.notice-subtitle {
  font-size: 0.9rem;
  color: #666;
}

.card-box {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
  padding: 20px;
  margin-bottom: 16px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.checkbox-group {
  display: flex;
  flex-direction: column;
<<<<<<< HEAD
  gap: var(--space-sm);
}

.section-label {
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
=======
  gap: 12px;
}

.section-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px; 
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.reason-list {
  display: flex;
  flex-direction: column;
<<<<<<< HEAD
  gap: var(--space-sm);
}

.etc-input-section {
  margin-top: var(--space-xs);
  padding-top: 0;
  border-top: none;
}

.etc-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  font-family: inherit;
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.etc-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.1);
}

[data-theme="dark"] .etc-input:focus {
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.15);
}

.etc-input::placeholder {
  color: var(--color-text-tertiary);
}

.input-counter {
  display: block;
  margin-top: var(--space-xs);
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  text-align: right;
=======
  gap: 12px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

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
  box-shadow: -2px 10px rgba(0,0,0,0.05);
  box-sizing: border-box;
}
</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
