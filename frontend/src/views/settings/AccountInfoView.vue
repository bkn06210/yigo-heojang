<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import PageHeader from '@/components/common/PageHeader.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';

const authStore = useAuthStore();

const { user } = storeToRefs(authStore);

const router = useRouter();

const showPasswordVerify = ref(false);

const goPasswordChange = () => {
  showPasswordVerify.value = false;
  router.push('/auth/password-change');
};

const joinedDate = '2026.07.16';

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
      title="계정 정보"
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




      <!-- 계정 관리 -->
      <div class="section-group">


        <div class="menu-list">


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


  </div>
</template>

<style scoped>
.account-info-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f9f9f9;
}

.content-container {
  flex: 1;
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
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.info-row:last-child {
  border-bottom: none;
}

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
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-item:hover {
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
}
</style>
