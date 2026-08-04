<script setup>
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';

const router = useRouter();

const notifications = ref({
  shortage: true,
  limitExhausted: true,
  unappliedBenefit: false
});

const goBack = () => {
  router.go(-1);
};

 // 설정 값이 변경될 때마다 자동 저장 혹은 API 연동을 처리할 수 있음.
watch(notifications, (newVal) => {
  // TODO: 백엔드 API 호출하여 알림 설정 저장
  console.log('알림 설정 변경됨:', newVal);
}, { deep: true });
</script>

<template>
  <div class="notification-setting-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="알림 설정" @back="goBack" />

    <div class="content-container">
      <!-- 알림 설정 토글 리스트 -->
      <div class="setting-list">
        <!-- 1. 실적 부족 알림 -->
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-title">실적 부족 탈림</span>
            <span class="setting-desc">이번 달 목표 실적 달성이 어려울 때 알려드려요.</span>
          </div>
          <!-- 토글 스위치 -->
          <label class="toggle-switch">
            <input type="checkbox" v-model="notifications.shortage" />
            <span class="slider round"></span>
          </label>
        </div>

        <!-- 2. 혜택 한도 소진 알림 -->
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-title">혜택 한도 소진 알림</span>
            <span class="setting-desc">카드 할인 및 포인트 적립 한도가 거의 소진되면 알려드려요.</span>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="notifications.limitExhausted" />
            <span class="slider round"></span>
          </label>
        </div>

        <!-- 3. 혜택 미적용 결제 알림 -->
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-title">혜택 미적용 결제 알림</span>
            <span class="setting-desc">조건을 충족하지 않아 혜택을 받지 못한 결제가 발생했을 때 알려드려요.</span>
          </div>
          <label class="toggle-switch">
            <input type="checkbox" v-model="notifications.unappliedBenefit" />
            <span class="slider round"></span>
          </label>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-setting-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f9f9f9;
}

.content-container {
  flex: 1;
  padding: 20px;
}

.setting-list {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
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

/* 커스텀 토글 스위치 스타일 (CSS 기본) */
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
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #ccc;
  transition: .3s;
  border-radius: 28px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 22px;
  width: 22px; 
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .3s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #4CAF50; /* 활성화 색상 */
}

input:checked + .slider:before {
  transform: translateX(22px);
}
</style>
