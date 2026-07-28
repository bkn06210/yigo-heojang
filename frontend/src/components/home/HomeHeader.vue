<script setup>
// =========================
// Props
// =========================
defineProps({
  // 읽지 않은 알림 여부
  hasUnreadNotification: {
    type: Boolean,
    default: false,
  },
})

// =========================
// Event
// =========================
const emit = defineEmits([
  'chat',
  'notification',
  'profile',
])
</script>

<template>
  <header class="home-header">

    <!-- 앱 이름 -->
    <h1 class="logo">
        YIGO
    </h1>

    <!-- 우측 버튼 -->
    <div class="actions">

      <!-- 챗봇 -->
      <button
        type="button"
        @click="$emit('chat')"
      >
        🤖
      </button>

      <!-- 알림 -->
      <button
        type="button"
        class="notification-btn"
        @click="$emit('notification')"
      >
        🔔

        <!-- 읽지 않은 알림 -->
        <span
          v-if="hasUnreadNotification"
          class="notification-dot"
        />
      </button>

      <!-- 프로필 -->
      <button
        type="button"
        @click="$emit('profile')"
      >
        👤
      </button>

    </div>

  </header>
</template>

<style scoped>
.home-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
    margin-bottom:16px;
}

.actions{
    display:flex;
    gap:12px;
}

button{
    position:relative;
}

.notification-dot{
    position:absolute;
    top:0;
    right:0;

    width:8px;
    height:8px;

    border-radius:50%;
    background:red;
}
</style>