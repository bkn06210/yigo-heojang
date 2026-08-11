<script setup>
import { onMounted } from 'vue';
import { useToast } from '@/composables/useToast';
import { useAuthStore } from '@/stores/authStore';
import { useTutorialStore } from '@/stores/tutorialStore';
import ToastNotification from '@/components/common/ToastNotification.vue';
import TutorialOverlay from '@/components/common/TutorialOverlay.vue';

const authStore = useAuthStore();
const tutorialStore = useTutorialStore();

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);

  if (authStore.isLogin() && !tutorialStore.isCompleted) {
    tutorialStore.startTutorial();
  }
});

// 전역 토스트 — 어느 화면에서 showToast()를 호출하든 여기 하나로 렌더링됨
const { toast, closeToast } = useToast();
</script>

<template>
  <div class="page-transition-wrapper">
    <router-view v-slot="{ Component, route }">
      <transition :name="route.meta.transition || 'fade'">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>

  <TutorialOverlay />

  <ToastNotification
    v-if="toast"
    :key="toast.key"
    :type="toast.type"
    :message="toast.message"
    :duration="toast.duration"
    @close="closeToast"
  />
</template>

<style>

.page-transition-wrapper {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 나가는 페이지만 absolute로 띄워서 위에 얹고, 들어오는 페이지는 정상 흐름을
   유지해 래퍼 높이가 항상 "새 페이지" 기준으로 유지되도록 함
   (둘 다 absolute로 빼면 전환 중 래퍼가 min-height:100vh로 줄었다 늘어나면서
   세로 스크롤바가 껐다 켜져 화면이 반짝이는 문제가 있었음) */
.slide-forward-leave-active,
.slide-back-leave-active,
.fade-leave-active {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

/* 상세 페이지 진입 (탭 → 상세) */
.slide-forward-enter-active,
.slide-forward-leave-active {
  transition: transform var(--transition-normal), opacity var(--transition-normal);
}

.slide-forward-enter-from {
  transform: translateX(100%);
}

.slide-forward-leave-to {
  transform: translateX(-30%);
  opacity: 0.6;
}

/* 뒤로가기 (상세 → 탭) */
.slide-back-enter-active,
.slide-back-leave-active {
  transition: transform var(--transition-normal), opacity var(--transition-normal);
}

.slide-back-enter-from {
  transform: translateX(-30%);
  opacity: 0.6;
}

.slide-back-leave-to {
  transform: translateX(100%);
}

/* 하단 탭 간 이동 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

</style>