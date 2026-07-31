<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { logout as logoutApi } from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore';

import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import ProfileEditBottomSheet from '@/components/settings/ProfileEditBottomSheet.vue';


const router = useRouter();


// 사용자 정보
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);


// 프로필 수정 바텀시트 상태
const isProfileSheetOpen = ref(false);


// 프로필 수정창 열기
const openProfileEdit = () => {

  if (!user.value) return;

  isProfileSheetOpen.value = true;

};


// 프로필 수정창 닫기
const closeProfileEdit = () => {

  isProfileSheetOpen.value = false;

};

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    console.log('로그아웃 API 실패:', error)
  } finally {
    authStore.logout()
    router.push('/auth/login')
  }
}

// 프로필 저장
const updateProfile = (updatedUser) => {

  userStore.updateUser(updatedUser);

  closeProfileEdit();

};


// 보기 설정
const displaySetting = ref('system');


// 뒤로가기
const goBack = () => {

  router.go(-1);

};


// 메뉴 이동
const navigateTo = (path) => {

  router.push(path);

};

// 로그아웃


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


        <!-- 프로필 이미지 -->
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


        <!-- 닉네임 + 수정 버튼 -->
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



      <!-- 로그인하지 않은 경우 -->
      <section
        v-else
        class="profile-section"
      >

        <span>
          로그인해주세요.
        </span>

      </section>



      <!-- 설정 메뉴 -->
      <section class="menu-list">


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



        <div
          class="menu-item"
          @click="navigateTo('/settings/security')"
        >

          <span>
            보안 및 로그인
          </span>

          <span>
            ›
          </span>

        </div>



        <div
          class="menu-item"
          @click="navigateTo('/settings/account')"
        >

          <span>
            계정
          </span>

          <span>
            ›
          </span>

        </div>


      </section>



      <!-- 로그아웃 -->
      <section class="logout-section">

  <button
    class="logout-button"
    type="button"
    @click="handleLogout"
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
  display: flex;

  flex-direction: column;

  min-height: 100vh;

  background: #f9f9f9;
}


.settings-content {
  flex: 1;

  padding: 20px;

  padding-bottom: 80px;
}


.profile-section {
  display: flex;

  flex-direction: column;

  align-items: center;

  margin-bottom: 30px;
}


.profile-image-wrapper {
  width: 100px;

  height: 100px;
}


.profile-image {
  width: 100%;

  height: 100%;

  border-radius: 50%;

  object-fit: cover;
}


.default-image {
  background: #ddd;
}


.profile-info {
  margin-top: 12px;
}


.nickname-area {
  display: flex;

  align-items: center;

  justify-content: center;

  gap: 6px;
}


.nickname {
  font-size: 1.2rem;

  font-weight: bold;
}


.edit-profile-button {
  width: 24px;

  height: 24px;

  border: none;

  background: none;

  display: flex;

  justify-content: center;

  align-items: center;

  cursor: pointer;
}


.edit-profile-button .material-icons {
  font-size: 18px;

  color: #888;
}


.menu-list {
  background: white;

  border-radius: 8px;

  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}


.menu-item {
  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 15px 20px;

  border-bottom: 1px solid #eee;

  cursor: pointer;
}


.menu-item:last-child {
  border-bottom: none;
}


.dropdown-item {
  cursor: default;
}


.menu-value-dropdown {
  position: relative;

  display: flex;

  align-items: center;
}


.menu-value-dropdown select {
  appearance: none;

  background: transparent;

  border: none;

  font-size: 0.9rem;

  color: #666;

  padding-right: 20px;
}


.dropdown-arrow {
  position: absolute;

  right: 0;
}


.logout-section {
  margin-top: 40px;

  text-align: center;
}


.logout-button {
  color: #d9534f;

  font-weight: bold;

  border: none;

  background: none;

  cursor: pointer;
}

</style>