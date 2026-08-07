<script setup>
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

// 부모(HomeView)로 전달하는 이벤트
defineEmits([
  'chat',
  'click-notification',
  'profile',
]);

// Props
defineProps({

  hasUnreadNotification: {
    type: Boolean,
    default: false,
  },

  user: {
    type: Object,
    default: null,
  },

});
</script>

<template>

<header class="home-header">

<<<<<<< HEAD
  <!-- 앱 이름 (아이콘 + 텍스트) -->
  <h1 class="logo">
    <span class="logo-badge">
      <Icon name="lightbulb" size="sm" />
    </span>
    두리
=======
  <!-- 앱 이름 -->
  <h1 class="logo">
    YIGO
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  </h1>



  <!-- 우측 영역 -->
  <div class="actions">

    <!-- 챗봇 -->
    <button
      type="button"
      class="icon-button"
      @click="$emit('chat')"
    >
<<<<<<< HEAD
      <Icon name="chat" size="md" />
=======
      🤖
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    </button>



    <!-- 알림 -->
    <button
      type="button"
      class="icon-button notification-btn"
      @click="$emit('click-notification')"
    >

<<<<<<< HEAD
      <Icon name="bell" size="md" />
=======
      🔔
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


      <!-- 읽지 않은 알림 -->
      <span
        v-if="hasUnreadNotification"
        class="notification-dot"
      />

    </button>

<<<<<<< HEAD
=======


    <!-- 사용자 이름 -->
    <span
      v-if="user"
      class="user-name"
    >
      {{ user.name }}
    </span>



>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    <!-- 프로필 -->
    <button
      type="button"
      class="profile-button"
      @click="$emit('profile')"
    >

      <img
        v-if="user && user.profileImageUrl"
        :src="user.profileImageUrl"
        class="profile-image"
        alt="프로필 이미지"
      />


      <span
        v-else
        class="default-profile"
      >
<<<<<<< HEAD
        <Icon name="profile" size="md" />
=======
        👤
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
      </span>

    </button>

  </div>

</header>

</template>



<style scoped>

.home-header {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:16px;

}



.logo {

<<<<<<< HEAD
  display:flex;

  align-items:center;

  gap:6px;

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  font-size:24px;

  font-weight:700;

}



<<<<<<< HEAD
/* 로고 아이콘 배지 - 브랜드 옐로우 원형 배경 */
.logo-badge {

  display:flex;

  align-items:center;

  justify-content:center;

  width:28px;

  height:28px;

  border-radius:50%;

  background: var(--color-primary);

  color: var(--color-point-icon);

  flex-shrink:0;

}



=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
.actions {

  display:flex;

  align-items:center;

  gap:12px;

}



.user-name {

  font-size:14px;

  font-weight:600;

}



<<<<<<< HEAD
/* 공통 아이콘 버튼 — 시각적 아이콘 크기는 그대로 두고 터치 영역만
   iOS/Android 권장 최소 터치 타깃(44px)에 가깝게 확대 */
.icon-button,
.profile-button {

  width:40px;

  height:40px;
=======
/* 공통 아이콘 버튼 */
.icon-button,
.profile-button {

  width:32px;

  height:32px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  border:none;

  background:none;

  padding:0;

  display:flex;

  align-items:center;

  justify-content:center;

  cursor:pointer;

<<<<<<< HEAD
  color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



/* 알림 버튼 기준 */
.notification-btn {

  position:relative;

}



/* 빨간 점 */
.notification-dot {

  position:absolute;

  top:3px;

  right:3px;

  width:8px;

  height:8px;

  border-radius:50%;

<<<<<<< HEAD
  background: var(--color-coral);
=======
  background:#ff3b30;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



/* 기본 프로필 */
.default-profile {

  font-size:24px;

}



/* 프로필 이미지 */
.profile-image {

  width:32px;

  height:32px;

  border-radius:50%;

  object-fit:cover;

}

</style>
