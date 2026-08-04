<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppButton from '@/components/common/AppButton.vue';
import ConfirmModal from '@/components/common/ConfirmModal.vue';

const router = useRouter();

const agreements = ref({
  deleteData: false,
  noticeConfirm: false
});

const reasons = ref({
  unused: false,
  inconvenient: false,
  etc: false
});

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
  console.log('회원 탈퇴 완료, 선택된 사유:', reasons.value);
  alert('회원 탈퇴가 정상적으로 처리되었습니다.');
  router.push('/login');
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
  background-color: #f9f9f9;
}

.content-container {
  flex: 1;
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
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px; 
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-button-area {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 15px 20px;
  background-color: white;
  box-shadow: -2px 10px rgba(0,0,0,0.05);
  box-sizing: border-box;
}
</style>
