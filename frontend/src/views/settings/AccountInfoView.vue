<script setup>
import { computed, ref, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import PageHeader from '@/components/common/PageHeader.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';
import SimplePasswordVerifyModal from '@/components/auth/SimplePasswordVerifyModal.vue';
import PinChangeModal from '@/components/auth/PinChangeModal.vue';
import { getMyInfo } from '@/api/memberApi';

const authStore = useAuthStore();

const { user } = storeToRefs(authStore);

const router = useRouter();

const showPasswordVerify = ref(false);
const showPinVerify = ref(false);
const showPinChangeModal = ref(false);

// 이메일 인증을 통과해야 받을 수 있는 일회용 토큰. 이걸 들고 있어야 저장 모달을 열 수 있다.
const simplePasswordChangeToken = ref('');

// 간편비밀번호를 이미 설정했는지. 서버가 GET /api/members/me의 simplePasswordSet으로 알려준다.
// 설정 여부에 따라 메뉴 라벨과 모달 문구가 "설정"/"변경"으로 갈린다.
const simplePasswordSet = ref(false);

const simplePasswordMenuLabel = computed(() =>
  simplePasswordSet.value ? '간편비밀번호 변경' : '간편비밀번호 설정'
);

const loadSimplePasswordState = async () => {
  try {
    const info = await getMyInfo();
    simplePasswordSet.value = Boolean(info?.simplePasswordSet);
  } catch (error) {
    // 조회에 실패해도 화면은 떠야 한다. 라벨만 기본값(설정)으로 남는다.
    console.error('간편비밀번호 설정 여부 조회 실패:', error);
  }
};

const goPasswordChange = () => {
  showPasswordVerify.value = false;
  router.push('/auth/password-change');
};

const openPinVerify = () => {
  showPinVerify.value = true;
};

const closePinVerify = () => {
  showPinVerify.value = false;
};

// 이메일 인증 성공 → 변경 토큰을 받아 저장 모달로 넘긴다.
const handlePinVerifySuccess = (changeToken) => {
  simplePasswordChangeToken.value = changeToken ?? '';
  showPinVerify.value = false;

  if (!simplePasswordChangeToken.value) {
    return;
  }

  showPinChangeModal.value = true;
};

const closePinChangeModal = () => {
  showPinChangeModal.value = false;
  // 토큰은 한 번만 쓸 수 있으므로 모달을 닫을 때 버린다.
  simplePasswordChangeToken.value = '';
};

const handlePinChangeSuccess = () => {
  simplePasswordSet.value = true;
};

const joinedDate = '2026.07.16';

// 보안 설정
const appLock = ref(false);

const autoLogin = ref(true);

// 초기 로드 시 localStorage에서 보안 설정 복원
onMounted(() => {
  loadSimplePasswordState();

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
      @click="openPinVerify"
    >

      <span class="menu-label">
        {{ simplePasswordMenuLabel }}
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
      v-if="showPasswordVerify"
      @close="showPasswordVerify = false"
      @verify-success="goPasswordChange"
    />

    <!-- 간편비밀번호 이메일 인증 모달 -->
    <SimplePasswordVerifyModal
      v-if="showPinVerify"
      @close="closePinVerify"
      @verified="handlePinVerifySuccess"
    />

    <!-- 간편비밀번호 입력·저장 모달 -->
    <PinChangeModal
      v-if="showPinChangeModal"
      :change-token="simplePasswordChangeToken"
      :is-first-time-setup="!simplePasswordSet"
      @close="closePinChangeModal"
      @success="handlePinChangeSuccess"
    />

    <!-- 간편비밀번호 변경 모달 -->
    <PinChangeModal
      v-if="showPinChangeModal"
      @close="closePinChangeModal"
      @success="handlePinChangeSuccess"
    />

  </div>
</template>

<style scoped>

.toggle {
  width: 48px;
  height: 28px;

  border: none;
  border-radius: var(--radius-full);

  background: var(--color-text-tertiary);

  padding: 3px;

  display: flex;
  align-items: center;

  cursor: pointer;

  transition: var(--transition-normal);
}


.toggle span {
  width: 22px;
  height: 22px;

  background: var(--color-surface);

  border-radius: var(--radius-full);

  transition: var(--transition-normal);
}


.toggle.active {
  background: var(--color-primary);
}


.toggle.active span {
  transform: translateX(20px);
}

.account-info-view {
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
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}
.info-row:last-child {
  border-bottom: none;
}

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
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-bg);
  cursor: pointer;
  transition: background-color var(--transition-normal);
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-item:hover {
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
}
</style>
