<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';


const router = useRouter();

const navigateTo = (path) => {
  router.push(path);
};

// 보안 설정 상태값
const security = ref({
  appLock: false,
  autoLogin: true,
});

const goBack = () => {
  router.go(-1);
};

// TODO: 백엔드 API 호출 또는 로컬 스토리지에 보안 설정 저장
watch(
  security,
  (newVal) => {
    console.log('보안 설정 변경됨:', newVal);
  },
  { deep: true },
);
</script>

<template>
  <div class="security-setting-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="보안 및 로그인" @back="goBack" />

    <div class="content-container">
      <!-- 섹션 타이틀 -->
      <div class="section-title">보안 및 로그인</div>

      <!-- 보안 설정 리스트 -->
      <div class="setting-list">
        <!-- 1. 앱 잠금 -->
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-title">앱 잠금</span>
            <span class="setting-desc"
              >앱 실행 시 생체 인증 또는 비밀번호로 잠금을 해제합니다.</span
            >
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="security.appLock" />
            <span class="slider round"></span>
          </label>
        </div>

        <!-- 2. 자동 로그인 -->
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-title">자동 로그인</span>
            <span class="setting-desc"
              >앱 재실행 시 로그인 정보를 유지합니다.</span
            >
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="security.autoLogin" />
            <span class="slider round"></span>
          </label>
        </div>
    </div>
    <!-- 비밀번호 변경 인증 팝업 -->
    <AuthVerifyModal
      v-if="showPasswordVerify"
      @close="showPasswordVerify = false"
      @verify-success="goPasswordChange"
    />
    </div>
  </div>
</template>

<style scoped>
.security-setting-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
}

.content-container {
  flex: 1;
  padding: 20px;
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}

.section-title {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 10px;
  font-weight: 600;
  padding-left: 4px;
}

.setting-list {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}
.setting-item:last-child {
  border-bottom: none;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 15px;
}

.setting-title {
  font-size: 1rem;
  font-weight: 500;
  color: #333;
}

.setting-desc {
  font-size: 0.8rem;
  color: #888;
  line-height: 1.3;
}

/* 토글 스위치 스타일 (이전 알림 설정과 동일하게 통일) */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 28px;
  flex-shrink: 0;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 28px;
}

.slider:before { 
  position: absolute;
  content: '';
  height: 22px;
  width: 22px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #4caf50;
}

input:checked + .slider:before {
  transform: translateX(22px);
}
</style>
