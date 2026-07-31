<script setup>


// 부모(HomeView)로 전달하는 이벤트
defineEmits([
  'chat',
  'click-notification',
  'profile',
]);

// Props
defineProps({


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


  user: {
    type: Object,
    default: null,
  },
})


// Event
defineEmits([
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



  <!-- 우측 영역 -->
  <div class="actions">



  <!-- 앱 이름 -->
  <h1 class="logo">
    YIGO
  </h1>



  <!-- 우측 버튼 -->
  <div class="actions">


    <!-- 챗봇 -->
    <button
      type="button"
      class="icon-button"
      @click="$emit('chat')"
    >
      🤖
    </button>



    <!-- 알림 -->
    <button
      type="button"
      class="icon-button notification-btn"

      @click="$emit('click-notification')"
    >

      🔔


      <!-- 읽지 않은 알림 -->

      @click="$emit('notification')"
    >

      🔔


      <!-- 읽지 않은 알림 표시 -->

      <span
        v-if="hasUnreadNotification"
        class="notification-dot"
      />

    </button>



    <!-- 사용자 이름 -->
    <span
      v-if="user"
      class="user-name"
    >
      {{ user.name }}
    </span>



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
        👤
      </span>


    </button>

  </div>




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

  font-size:24px;

  font-weight:700;

}



.actions {

  display:flex;

  align-items:center;

  gap:12px;

}



.user-name {

  font-size:14px;

  font-weight:600;

}



/* 공통 아이콘 버튼 */
.icon-button,
.profile-button {

  width:32px;

  height:32px;

  border:none;

  background:none;

  padding:0;

}



.logo {

  font-size:24px;

  font-weight:700;

}



.actions {


  display:flex;

  align-items:center;


  justify-content:center;

  cursor:pointer;

}



/* 알림 버튼 기준 */
.notification-btn {

  position:relative;

}



/* 빨간 점 */

  gap:12px;

}



.user-name {

  font-size:14px;

  font-weight:600;

}



.icon-button,
.profile-button {

  width:32px;
  height:32px;

  border:none;
  background:none;

  padding:0;

  display:flex;
  align-items:center;
  justify-content:center;

  cursor:pointer;

}




.notification-dot {

  position:absolute;


  top:3px;

  right:3px;

  width:8px;


  top:0;
  right:0;

  width:8px;

  height:8px;

  border-radius:50%;


  background:#ff3b30;

}



/* 기본 프로필 */

  background:red;

}


.default-profile {

  font-size:24px;

}




/* 프로필 이미지 */
.profile-image {

  width:32px;


.profile-image {

  width:32px;

  height:32px;

  border-radius:50%;

  object-fit:cover;

}





.notification-btn {

  position:relative;

}



</style>