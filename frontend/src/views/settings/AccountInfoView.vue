<<<<<<< HEAD
﻿<script setup>
import { ref, watch, onMounted } from 'vue';
=======
<script setup>
import { computed, onMounted, ref } from 'vue';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import PageHeader from '@/components/common/PageHeader.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';
<<<<<<< HEAD
import PinChangeModal from '@/components/auth/PinChangeModal.vue';
=======
import { getMyInfo } from '@/api/memberApi';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const authStore = useAuthStore();

const { user } = storeToRefs(authStore);

const router = useRouter();

const showPasswordVerify = ref(false);
<<<<<<< HEAD
const showPinVerify = ref(false);
const showPinChangeModal = ref(false);
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const goPasswordChange = () => {
  showPasswordVerify.value = false;
  router.push('/auth/password-change');
};

<<<<<<< HEAD
const openPinVerify = () => {
  showPinVerify.value = true;
};

const closePinVerify = () => {
  showPinVerify.value = false;
};

const handlePinVerifySuccess = () => {
  showPinVerify.value = false;
  showPinChangeModal.value = true;
};

const closePinChangeModal = () => {
  showPinChangeModal.value = false;
};

const handlePinChangeSuccess = () => {
  showPinChangeModal.value = false;
};

const joinedDate = '2026.07.16';
=======
const joinedDate = computed(() =>
  user.value?.createdAt?.slice(0, 10).replaceAll('-', '.') || '-'
);

const loadMyInfo = async () => {
  try {
    authStore.updateUser(await getMyInfo());
  } catch (error) {
    console.error('회원정보 조회 실패:', error);
  }
};

onMounted(loadMyInfo);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

// 보안 설정
const appLock = ref(false);

const autoLogin = ref(true);

<<<<<<< HEAD
// 초기 로드 시 localStorage에서 보안 설정 복원
onMounted(() => {
  const saved = localStorage.getItem('accountSecuritySettings');
  if (saved) {
    try {
      const settings = JSON.parse(saved);
      appLock.value = settings.appLock ?? false;
      autoLogin.value = settings.autoLogin ?? true;
    } catch (e) {
      console.error('보안 설정 복원 실패:', e);
    }
  }
});

// 설정 값이 변경될 때마다 localStorage에 저장
watch([appLock, autoLogin], ([newAppLock, newAutoLogin]) => {
  localStorage.setItem('accountSecuritySettings', JSON.stringify({
    appLock: newAppLock,
    autoLogin: newAutoLogin
  }));
  console.log('보안 설정 변경됨:', { appLock: newAppLock, autoLogin: newAutoLogin });
});


=======

// 간편비밀번호 변경 이동
const goPinChange = () => {
  router.push('/settings/pin-change');
};
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

// 이름 마스킹
const maskName = (name) => {
  if (!name) return '';

  if (name.length === 2) {
    return name[0] + '*';
  }

  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1];
};

//이메일 마스킹
const maskEmail = (email) => {
  if (!email) return '-';

  const [id, domain] = email.split('@');

  if (!domain) return email;

  if (id.length <= 2) {
    return `${id[0]}*@${domain}`;
  }

  return `${id.slice(0, 2)}***@${domain}`;
};

const goBack = () => {
  router.go(-1);
};

// 라우터 페이지 이동
const navigateTo = (path) => {
  router.push(path);
};
</script>

<template>
  <div class="account-info-view">

    <!-- 상단 헤더 -->
    <PageHeader
      title="계정 및 보안"
      @back="goBack"
    />


    <div class="content-container">

<<<<<<< HEAD
      <h1 class="page-title">계정 및 보안</h1>

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

      <!-- 가입 정보 -->
      <div class="section-group">

        <div class="section-title">
          가입 정보
        </div>


        <div class="info-card">


          <!-- 이름 -->
          <div class="info-row">

            <span class="info-label">
              이름
            </span>


            <span class="info-value">
              {{ maskName(user?.name) || '-' }}
            </span>

          </div>



          <!-- 이메일 -->
<div class="info-row">

  <span class="info-label">
    이메일
  </span>


  <span class="info-value">
    {{ maskEmail(user?.email) }}
  </span>

</div>



          <!-- 가입일 -->
          <div class="info-row">

            <span class="info-label">
              가입일
            </span>


            <span class="info-value">
              {{ joinedDate }}
            </span>

          </div>


        </div>

      </div>




      <!-- 보안 설정 -->
<div class="section-group">

  <div class="section-title">
    로그인 및 보안
  </div>


  <div class="menu-list">


    <!-- 앱 잠금 -->
    <div class="menu-item">

      <span class="menu-label">
        앱 잠금
      </span>


      <!-- 앱 잠금 -->
<button
  class="toggle"
  :class="{ active: appLock }"
  @click="appLock = !appLock"
>
  <span></span>
</button>

    </div>



    <!-- 자동 로그인 -->
    <div class="menu-item">

      <span class="menu-label">
        자동 로그인
      </span>


      <!-- 자동 로그인 -->
<button
  class="toggle"
  :class="{ active: autoLogin }"
  @click="autoLogin = !autoLogin"
>
  <span></span>
</button>

    </div>



    <!-- 비밀번호 변경 -->
    <div
      class="menu-item"
      @click="showPasswordVerify = true"
    >

      <span class="menu-label">
        비밀번호 변경
      </span>


      <span class="chevron-icon">
        ›
      </span>

    </div>



    <!-- 간편비밀번호 -->
    <div
      class="menu-item"
<<<<<<< HEAD
      @click="openPinVerify"
=======
      @click="goPinChange"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    >

      <span class="menu-label">
        간편비밀번호 변경
      </span>


      <span class="chevron-icon">
        ›
      </span>

    </div>


  </div>

</div>



<!-- 계정 관리 -->
<div class="section-group">

  <div class="section-title">
    계정 관리
  </div>


  <div class="menu-list">


          <!-- 회원 탈퇴 -->
          <div
            class="menu-item withdraw-item"
            @click="navigateTo('/settings/withdrawal')"
          >

            <span class="menu-label">
              회원탈퇴
            </span>


            <span class="chevron-icon">
              ›
            </span>

          </div>


        </div>


      </div>


    </div>



    <!-- 비밀번호 인증 모달 -->
    <AuthVerifyModal
<<<<<<< HEAD
      v-if="showPasswordVerify"
      @close="showPasswordVerify = false"
      @verify-success="goPasswordChange"
    />

    <!-- 간편비밀번호 인증 모달 -->
    <AuthVerifyModal
      v-if="showPinVerify"
      @close="closePinVerify"
      @verify-success="handlePinVerifySuccess"
    />

    <!-- 간편비밀번호 변경 모달 -->
    <PinChangeModal
      v-if="showPinChangeModal"
      @close="closePinChangeModal"
      @success="handlePinChangeSuccess"
    />
=======

      v-if="showPasswordVerify"

      @close="showPasswordVerify = false"

      @verify-success="goPasswordChange"

    />

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  </div>
</template>

<style scoped>

.toggle {
  width: 48px;
  height: 28px;

  border: none;
<<<<<<< HEAD
  border-radius: var(--radius-full);

  background: var(--color-text-tertiary);
=======
  border-radius: 20px;

  background: #ddd;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  padding: 3px;

  display: flex;
  align-items: center;

  cursor: pointer;

<<<<<<< HEAD
  transition: var(--transition-normal);
=======
  transition: 0.2s;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


.toggle span {
  width: 22px;
  height: 22px;

<<<<<<< HEAD
  background: var(--color-surface);

  border-radius: var(--radius-full);

  transition: var(--transition-normal);
=======
  background: white;

  border-radius: 50%;

  transition: 0.2s;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


.toggle.active {
<<<<<<< HEAD
  background: var(--color-primary);
=======
  background: #4f46e5;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


.toggle.active span {
  transform: translateX(20px);
}

.account-info-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
<<<<<<< HEAD
  background-color: var(--color-bg);
=======
  background-color: #f9f9f9;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.content-container {
  flex: 1;
<<<<<<< HEAD
  padding: var(--space-lg);
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

.section-group {
  margin-bottom: var(--space-xl);
}

.section-title {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-sm);
  font-weight: var(--font-semibold);
  padding-left: var(--space-xxs);
}

/* 가입 정보 (Soft Glassmorphism) */
.info-card {
  border-radius: var(--radius-lg);
  padding: var(--space-sm) var(--space-md);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .info-card {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.2) 0%, rgba(var(--color-primary-dark-rgb), 0.07) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);
=======
  padding: 20px;
}

.section-group {
  margin-bottom: 24px;
}

.section-title {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 8px;
  font-weight: 600;
  padding-left: 4px;
}

.info-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  padding: 16px 20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
<<<<<<< HEAD
  padding: var(--space-sm) 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
=======
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}
.info-row:last-child {
  border-bottom: none;
}

<<<<<<< HEAD
[data-theme="dark"] .info-row {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.info-label {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.info-value {
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);
}

.menu-list {
  background-color: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
=======
.info-label {
  font-size: 0.95rem;
  color: #666;
}

.info-value {
  font-size: 0.95rem;
  color: #333;
  font-weight: 500;
}

.menu-list {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
<<<<<<< HEAD
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-bg);
  cursor: pointer;
  transition: background-color var(--transition-normal);
=======
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-item:hover {
<<<<<<< HEAD
  background-color: var(--color-bg);
}

.menu-label {
  font-size: var(--font-md);
  color: var(--color-text-primary);
}

.withdraw-item .menu-label {
  color: var(--color-coral);
}

.chevron-icon {
  color: var(--color-text-tertiary);
=======
  background-color: #fafafa;
}

.menu-label {
  font-size: 1rem;
  color: #333;
}

.withdraw-item .menu-label {
  color: #e53935; /* 탈퇴 메뉴는 눈에 띄게 붉은 계열 포인트 */
}

.chevron-icon {
  color: #888;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}
</style>
<!-- 07_25 연동 변경: 계정 정보를 회원 API에서 조회하고 수정 결과를 반영한다. -->
