<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';

import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import ProfileEditBottomSheet from '@/components/settings/ProfileEditBottomSheet.vue';


const router = useRouter();


// 로그인 상태
// TODO : 백엔드/Pinia 인증 상태 연결
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);


// 프로필 수정 바텀시트
const isProfileSheetOpen = ref(false);


// 보기 설정
const displaySetting = ref('system');


// 로그인 이동
const goLogin = () => {

  router.push('/auth/login');

};


// 뒤로가기
const goBack = () => {

  router.go(-1);

};


// 메뉴 이동
// 로그인 상태에서만 이동 가능
const navigateTo = (path) => {

  if (!user.value) {

    goLogin();

    return;

  }


  router.push(path);

};



// 프로필 수정 열기
const openProfileEdit = () => {

  if (!user.value) {

    goLogin();

    return;

  }


  isProfileSheetOpen.value = true;

};



// 프로필 수정 닫기
const closeProfileEdit = () => {

  isProfileSheetOpen.value = false;

};



// 프로필 저장
const updateProfile = (updatedUser) => {

  // TODO : 실제 API 연결 시 수정 API 호출

  authStore.updateUser(updatedUser);


  closeProfileEdit();

};



// 로그아웃
const logout = () => {

  authStore.logout();


  router.push('/');

};

</script>

<template>

  <div class="settings-view">


    <!-- 헤더 -->
    <PageHeader
      title="설정"
      @back="goBack"
    />



    <main class="settings-content">


      <!-- 프로필 영역 -->

      <section
        v-if="user"
        class="profile-section"
      >


        <div class="profile-image-wrapper">


          <img
            v-if="user.profileImageUrl"
            :src="user.profileImageUrl"
            alt="프로필 이미지"
            class="profile-image"
          />


          <div
            v-else
            class="profile-image default-image"
          />


        </div>




        <div class="profile-info">


          <div class="nickname-area">


            <span class="nickname">
              {{ user.nickname }}
            </span>



            <button
              class="edit-profile-button"
              @click="openProfileEdit"
            >

              <span class="material-icons">
                edit
              </span>

            </button>


          </div>


        </div>


      </section>



      <!-- 비로그인 상태 -->

      <section
        v-else
        class="profile-section guest-profile"
      >

        <p>
          로그인 후 이용해주세요.
        </p>


        <button
          class="login-button"
          @click="goLogin"
        >

          로그인

        </button>


      </section>





      <!-- 설정 메뉴 -->

      <section class="menu-list">



        <!-- 개인 맞춤 설정 -->

        <div
          class="menu-item"
          @click="navigateTo('/settings/personalization')"
        >

          <span>
            개인 맞춤 설정
          </span>


          <span>
            ›
          </span>


        </div>





        <!-- 알림 -->

        <div
          class="menu-item"
          @click="navigateTo('/settings/notifications')"
        >

          <span>
            알림
          </span>


          <span>
            ›
          </span>


        </div>





        <!-- 보기 설정 -->

        <div class="menu-item dropdown-item">


          <span>
            보기
          </span>



          <div class="menu-value-dropdown">


            <select v-model="displaySetting">


              <option value="system">
                시스템 설정
              </option>


              <option value="light">
                라이트 모드
              </option>


              <option value="dark">
                다크 모드
              </option>


            </select>



            <span class="dropdown-arrow">
              ▾
            </span>


          </div>


        </div>





        <!-- 계정 및 보안 -->

        <div
          class="menu-item"
          @click="navigateTo('/settings/account')"
        >

          <span>
            계정 및 보안
          </span>


          <span>
            ›
          </span>


        </div>



      </section>





      <!-- 로그아웃 -->

      <section
        v-if="user"
        class="logout-section"
      >


        <button
          class="logout-button"
          type="button"
          @click="logout"
        >

          로그아웃

        </button>


      </section>



    </main>





    <!-- 프로필 수정 바텀시트 -->

    <ProfileEditBottomSheet

      v-if="isProfileSheetOpen"

      :user="user"

      @close="closeProfileEdit"

      @save="updateProfile"

    />





    <!-- 하단 네비게이션 -->

    <BottomNavigation />


  </div>


</template>

<style scoped>

.settings-view {

  min-height: 100vh;

  background: #f7f7f8;

  display:flex;

  flex-direction:column;

}



/* 본문 */

.settings-content {

  flex:1;

  padding:20px;

  padding-bottom:100px;

}



/* 프로필 */

.profile-section {

  background:white;

  border-radius:20px;

  padding:24px 20px;

  display:flex;

  flex-direction:column;

  align-items:center;

  margin-bottom:20px;

}



.guest-profile {

  gap:16px;

}



.guest-profile p {

  margin:0;

  color:#666;

  font-size:15px;

}



.login-button {

  border:none;

  background:#4F46E5;

  color:white;

  padding:10px 24px;

  border-radius:20px;

  font-size:14px;

  cursor:pointer;

}





.profile-image-wrapper {

  width:88px;

  height:88px;

}



.profile-image {

  width:100%;

  height:100%;

  border-radius:50%;

  object-fit:cover;

}



.default-image {

  background:#e5e7eb;

}



.profile-info {

  margin-top:14px;

}



.nickname-area {

  display:flex;

  align-items:center;

  gap:8px;

}



.nickname {

  font-size:18px;

  font-weight:700;

}



.edit-profile-button {

  width:28px;

  height:28px;

  border:none;

  background:#f3f4f6;

  border-radius:50%;

  display:flex;

  align-items:center;

  justify-content:center;

  cursor:pointer;

}



.edit-profile-button .material-icons {

  font-size:16px;

  color:#666;

}





/* 설정 메뉴 */

.menu-list {

  background:white;

  border-radius:20px;

  overflow:hidden;

  box-shadow:0 2px 10px rgba(0,0,0,0.04);

}



.menu-item {

  min-height:56px;

  padding:0 20px;

  display:flex;

  justify-content:space-between;

  align-items:center;

  border-bottom:1px solid #f1f1f1;

  font-size:15px;

  cursor:pointer;

}



.menu-item:last-child {

  border-bottom:none;

}



.menu-item span:last-child {

  color:#999;

  font-size:22px;

}





/* 보기 설정 */

.dropdown-item {

  cursor:default;

}



.menu-value-dropdown {

  position:relative;

  display:flex;

  align-items:center;

}



.menu-value-dropdown select {

  appearance:none;

  -webkit-appearance:none;

  border:none;

  outline:none;


  background:#f5f5f7;

  color:#555;


  height:34px;

  min-width:110px;


  padding:0 34px 0 14px;


  border-radius:18px;


  font-size:13px;

  font-weight:500;

  cursor:pointer;

}



.dropdown-arrow {

  position:absolute;

  right:12px;

  color:#888;

  font-size:12px;

  pointer-events:none;

}





/* 로그아웃 */

.logout-section {

  margin-top:32px;

  display:flex;

  justify-content:center;

}



.logout-button {

  border:none;

  background:white;


  width:100%;

  height:52px;


  border-radius:16px;


  color:#ef4444;


  font-size:15px;

  font-weight:600;


  cursor:pointer;


  box-shadow:0 2px 8px rgba(0,0,0,0.04);

}


</style>