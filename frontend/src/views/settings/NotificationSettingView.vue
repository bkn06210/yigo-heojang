<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import {
  getNotificationSettings,
  updateNotificationSettings,
} from '@/api/notificationApi';

const router = useRouter();

const notifications = ref({
  shortage: true,
  limitExhausted: true,
  unappliedBenefit: false
});

// 화면의 토글과 서버 필드 대응.
//   shortage        ↔ performanceShortageEnabled (실적 부족)
//   limitExhausted  ↔ benefitLimitEnabled        (혜택 한도 임박·소진)
//
// unappliedBenefit(혜택 미적용 결제)은 서버에 대응 필드가 없다.
// notification_setting 테이블에도 컬럼이 없어서, 이 토글만 예전처럼 브라우저에 남긴다.
const LOCAL_ONLY_KEY = 'notificationSettings';

// 서버에 반영된 마지막 상태. 저장이 실패하면 여기로 되돌린다 —
// 실패했는데 토글이 켜진 채로 남으면 사용자는 꺼진 줄 알고 화면을 떠난다.
const lastSyncedState = ref({
  shortage: true,
  limitExhausted: true,
});

// 서버 응답을 화면에 밀어넣는 동안에는 watch가 다시 저장을 부르지 않게 막는다.
const isApplyingRemote = ref(false);

const goBack = () => {
  router.go(-1);
};

const applyRemoteState = (next) => {
  isApplyingRemote.value = true;

  notifications.value = {
    ...notifications.value,
    ...next,
  };

  lastSyncedState.value = {
    shortage: notifications.value.shortage,
    limitExhausted: notifications.value.limitExhausted,
  };

  // 값 대입으로 예약된 watch 콜백이 실행된 뒤에 잠금을 푼다.
  setTimeout(() => {
    isApplyingRemote.value = false;
  }, 0);
};

// 초기 로드 — 서버가 원본이고, 서버에 없는 항목만 localStorage에서 복원한다.
onMounted(async () => {
  const saved = localStorage.getItem(LOCAL_ONLY_KEY);
  let localUnappliedBenefit = notifications.value.unappliedBenefit;

  if (saved) {
    try {
      localUnappliedBenefit = Boolean(JSON.parse(saved)?.unappliedBenefit);
    } catch (e) {
      console.error('알림 설정 복원 실패:', e);
    }
  }

  try {
    const settings = await getNotificationSettings();

    applyRemoteState({
      shortage: Boolean(settings?.performanceShortageEnabled),
      limitExhausted: Boolean(settings?.benefitLimitEnabled),
      unappliedBenefit: localUnappliedBenefit,
    });
  } catch (error) {
    // 조회에 실패하면 기본값(모두 수신)을 그대로 두되, 그 상태를 "저장된 값"으로 착각하지 않는다.
    console.error('알림 설정 조회 실패:', error);

    applyRemoteState({ unappliedBenefit: localUnappliedBenefit });
  }
});

// 설정이 바뀌면 서버에 저장한다.
// 서버는 토글이 아니라 최종 상태 두 값을 항상 함께 받는다.
watch(notifications, async (newVal) => {
  // 서버에 없는 항목은 예전처럼 브라우저에만 남긴다.
  localStorage.setItem(
    LOCAL_ONLY_KEY,
    JSON.stringify({ unappliedBenefit: newVal.unappliedBenefit }),
  );

  if (isApplyingRemote.value) {
    return;
  }

  // 서버가 관리하는 두 값이 그대로면 저장할 것이 없다
  // (혜택 미적용 토글만 움직인 경우가 여기에 해당한다).
  if (
    newVal.shortage === lastSyncedState.value.shortage
    && newVal.limitExhausted === lastSyncedState.value.limitExhausted
  ) {
    return;
  }

  const requested = {
    shortage: newVal.shortage,
    limitExhausted: newVal.limitExhausted,
  };

  try {
    const settings = await updateNotificationSettings({
      performanceShortageEnabled: requested.shortage,
      benefitLimitEnabled: requested.limitExhausted,
    });

    // 서버가 확정한 값을 다시 반영한다. 보낸 값과 같더라도 여기서 맞춰두면
    // 나중에 서버가 값을 보정하더라도 화면이 어긋나지 않는다.
    applyRemoteState({
      shortage: Boolean(settings?.performanceShortageEnabled),
      limitExhausted: Boolean(settings?.benefitLimitEnabled),
    });
  } catch (error) {
    console.error('알림 설정 저장 실패:', error);

    // 저장에 실패했으므로 마지막으로 서버에 반영된 상태로 되돌린다.
    applyRemoteState({ ...lastSyncedState.value });
  }
}, { deep: true });
</script>

<template>
  <div class="notification-setting-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="알림 설정" @back="goBack" />

    <div class="content-container">
      <!-- 알림 설정 토글 리스트 (Bento: ON 항목 강조) -->
      <div class="setting-list">
        <!-- 1. 실적 부족 알림 -->
        <div class="setting-item" :class="{ 'is-active': notifications.shortage }">
          <div class="setting-info">
            <span class="setting-title">실적 부족 알림</span>
            <span class="setting-desc">이번 달 목표 실적 달성이 어려울 때 알려드려요.</span>
          </div>
          <!-- 토글 스위치 -->
          <label class="toggle-switch">
            <input type="checkbox" v-model="notifications.shortage" />
            <span class="slider round"></span>
          </label>
        </div>

        <!-- 2. 혜택 한도 소진 알림 -->
        <div class="setting-item" :class="{ 'is-active': notifications.limitExhausted }">
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
        <div class="setting-item" :class="{ 'is-active': notifications.unappliedBenefit }">
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

.setting-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 알림 카드 (Bento: 독립 카드) */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  transition: var(--transition-fast);
}

/* ON 상태 (Soft Glassmorphism 강조) */
.setting-item.is-active {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03), inset 0 1px 0 rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

[data-theme="dark"] .setting-item.is-active {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.06) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs);
  padding-right: var(--space-sm);
}

.setting-title {
  font-size: var(--font-md);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.setting-desc {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
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
  background-color: var(--color-text-tertiary);
  transition: var(--transition-normal);
  border-radius: 28px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 22px;
  width: 22px; 
  left: 3px;
  bottom: 3px;
  background-color: var(--color-surface);
  transition: var(--transition-normal);
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--color-primary);
}

input:checked + .slider:before {
  transform: translateX(22px);
}
</style>
