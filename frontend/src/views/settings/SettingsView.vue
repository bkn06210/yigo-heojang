<script setup>
import { ref, watch, onMounted } from 'vue';
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
const displaySetting = ref(localStorage.getItem('theme') || 'light');
const isThemeDropdownOpen = ref(false);

const themeOptions = [
  { value: 'system', label: '시스템 설정' },
  { value: 'light', label: '라이트 모드' },
  { value: 'dark', label: '다크 모드' }
];

const getThemeLabel = () => {
  return themeOptions.find(opt => opt.value === displaySetting.value)?.label || '시스템 설정';
};

// 테마 변경
const selectTheme = (value) => {
  displaySetting.value = value;
  isThemeDropdownOpen.value = false;
};

// 테마 변경 감지
watch(displaySetting, (newValue) => {
  if (newValue === 'system') {
    // 시스템 설정은 light로 설정 (필요시 나중에 시스템 색상 감지 로직 추가)
    document.documentElement.setAttribute('data-theme', 'light');
    localStorage.setItem('theme', 'light');
  } else {
    document.documentElement.setAttribute('data-theme', newValue);
    localStorage.setItem('theme', newValue);
  }
});

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'light';
  displaySetting.value = savedTheme;
  document.documentElement.setAttribute('data-theme', savedTheme);
});


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
      :show-back="false"
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

          <div class="custom-dropdown">
            <button
              class="dropdown-trigger"
              @click="isThemeDropdownOpen = !isThemeDropdownOpen"
            >
              {{ getThemeLabel() }}
              <span class="dropdown-arrow" :class="{ open: isThemeDropdownOpen }">
                ▾
              </span>
            </button>

            <div
              v-if="isThemeDropdownOpen"
              class="dropdown-menu"
            >
              <button
                v-for="option in themeOptions"
                :key="option.value"
                class="dropdown-option"
                :class="{ active: displaySetting === option.value }"
                @click="selectTheme(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
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

  background: var(--color-bg);

  display:flex;

  flex-direction:column;

  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

  margin: 0 auto;

  max-width: 480px;

  box-sizing: border-box;

  overflow: hidden visible;

}



/* 본문 */

.settings-content {

  flex:1;

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

}



/* 프로필 (Bento: 강조 셀 + Soft Glassmorphism) */

.profile-section {

  border-radius: var(--radius-xl);

  padding: var(--space-2xl) var(--space-lg);

  display:flex;

  flex-direction:column;

  align-items:center;

  margin-bottom: var(--space-lg);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

}

[data-theme="dark"] .profile-section {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);
}



.guest-profile {

  gap: var(--space-md);

}



.guest-profile p {

  margin:0;

  color: var(--color-text-secondary);

  font-size: var(--font-sm);

}



.login-button {

  border:none;

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  padding: var(--space-xs) var(--space-xl);

  border-radius: var(--radius-lg);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor:pointer;

  transition: var(--transition-fast);

}

.login-button:hover {

  opacity: 0.9;

}





.profile-image-wrapper {

  width:88px;

  height:88px;

}



.profile-image {

  width:100%;

  height:100%;

  border-radius: var(--radius-full);

  object-fit:cover;

}



.default-image {

  background: var(--color-border);

}



.profile-info {

  margin-top: var(--space-sm);

}



.nickname-area {

  display:flex;

  align-items:center;

  gap: var(--space-xs);

}



.nickname {

  font-size: var(--font-2xl);

  font-weight: var(--font-bold);

  letter-spacing: -0.3px;

  color: var(--color-text-primary);

}



.edit-profile-button {

  width:28px;

  height:28px;

  border:none;

  background: rgba(255, 255, 255, 0.4);

  border-radius: var(--radius-full);

  display:flex;

  align-items:center;

  justify-content:center;

  cursor:pointer;

  transition: var(--transition-fast);

}

.edit-profile-button:hover {

  background: rgba(255, 255, 255, 0.6);

}

[data-theme="dark"] .edit-profile-button {

  background: rgba(255, 255, 255, 0.08);

}

[data-theme="dark"] .edit-profile-button:hover {

  background: rgba(255, 255, 255, 0.14);

}



.edit-profile-button .material-icons {

  font-size: var(--font-md);

  color: var(--color-text-secondary);

}





/* 설정 메뉴 */

.menu-list {

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  overflow: visible;

  box-shadow: var(--shadow-card);

  position: relative;

}



.menu-item {

  min-height:56px;

  padding: 0 var(--space-md);

  display:flex;

  justify-content:space-between;

  align-items:center;

  border-bottom: 1px solid var(--color-bg);

  font-size: var(--font-sm);

  color: var(--color-text-primary);

  cursor:pointer;

  transition: var(--transition-fast);

}

.menu-item:hover {

  background: var(--color-bg);

}



.menu-item:last-child {

  border-bottom:none;

}



.menu-item span:last-child {

  color: var(--color-text-tertiary);

  font-size: var(--font-2xl);

}





/* 보기 설정 */

.dropdown-item {
  cursor: default;
}

.custom-dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-sm);

  border: 1.5px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text-primary);

  height: 36px;
  min-width: 120px;
  padding: var(--space-xs) var(--space-md);

  border-radius: var(--radius-md);

  font-size: var(--font-sm);
  font-weight: var(--font-medium);
  cursor: pointer;

  transition: all var(--transition-fast);
}

.dropdown-trigger:hover {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.06) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);
}

.dropdown-trigger:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.1);
}

[data-theme="dark"] .dropdown-trigger:hover {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.08) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);
}

[data-theme="dark"] .dropdown-trigger:focus {
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.12);
}

.dropdown-arrow {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  transition: transform var(--transition-fast);
  margin-left: auto;
}

.dropdown-arrow.open {
  transform: rotate(-180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;

  min-width: 140px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;

  z-index: 1000;
}

[data-theme="dark"] .dropdown-menu {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.dropdown-option {
  width: 100%;
  text-align: left;

  border: none;
  background: transparent;
  color: var(--color-text-primary);

  padding: var(--space-sm) var(--space-md);
  font-size: var(--font-sm);
  cursor: pointer;

  transition: all var(--transition-fast);
}

.dropdown-option:hover {
  background: var(--color-bg);
}

.dropdown-option.active {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  color: var(--color-primary-dark);
  font-weight: var(--font-semibold);
}

[data-theme="dark"] .dropdown-option.active {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
}





/* 로그아웃 */

.logout-section {

  margin-top: var(--space-2xl);

  display:flex;

  justify-content:center;

}



.logout-button {

  border: none;

  background: transparent;


  width:100%;

  height:52px;


  border-radius: var(--radius-md);


  color: var(--color-coral);


  font-size: var(--font-sm);

  font-weight: var(--font-semibold);


  cursor:pointer;

  transition: var(--transition-fast);

}


</style>
