<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import PasswordInput from '@/components/common/PasswordInput.vue';
import AppButton from '@/components/common/AppButton.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';

const router = useRouter();

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
  alert('비밀번호가 안전하게 변경되었습니다.');
  router.go(-1);
};
</script>

<template>
  <div class="password-change-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="비밀번호 변경" @back="goBack" />

    <div class="content-container">
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
  background-color: #f9f9f9;
}

.content-container {
  flex: 1;
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
}
.input-group:last-child {
  margin-bottom: 0;
}

.input-label {
  display: block;
  font-size: 0.9rem;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.footer-button-area {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 15px 20px;
  background-color: white;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  box-sizing: border-box;
}
</style>