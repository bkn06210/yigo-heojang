<script setup>

import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteMembership as removeMembership, getMembership } from '@/api/walletApi'


// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNavigation from '@/components/layout/BottomNavigation.vue'

const route = useRoute()
const router = useRouter()

// 멤버십 데이터
const membership = ref({ providerName: '', totalPoint: 0, usagePlaces: [] })
const errorMessage = ref('')

// 멤버십 조회
const loadMembership = async () => {
  try {
    const data = await getMembership(route.params.id)
    membership.value = {
      ...data,
      usagePlaces: data.usagePlaces || [],
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '멤버십 정보를 불러오지 못했습니다.'
  }
}

// 최근 3개월간 이 멤버십 사용처에서 결제한 내역. 서버가 우리 소비내역으로 계산해 준다.
const recentUse = computed(() => {
  const count = Number(membership.value.recentUseCount || 0)
  if (count === 0) return '이용 내역 없음'
  const amount = Number(membership.value.recentUseAmount || 0)
  return `${count}건 · ${amount.toLocaleString('ko-KR')}원`
})

// 공식 사이트 이동
const goOfficialWebsite = () => {
  if (membership.value.officialSiteUrl) {
    window.open(membership.value.officialSiteUrl, '_blank', 'noopener,noreferrer')
  }
}

// 삭제 팝업 상태
const showDeleteModal = ref(false)


// 삭제 버튼 클릭
const openDeleteModal = () => {

  showDeleteModal.value = true

}


// 팝업 닫기
const closeDeleteModal = () => {

  showDeleteModal.value = false

}


// 삭제 처리
const deleteMembership = async () => {
  try {
    await removeMembership(route.params.id)
    router.replace('/points')
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '멤버십 삭제에 실패했습니다.'
  }
}

onMounted(loadMembership)


</script>



<template>

  <div class="membership-detail-page">


    <!-- 헤더 -->
    <PageHeader title="멤버십 상세" @back="router.back()" />



    <main class="content">

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <!-- 멤버십 이미지 & 이름 -->
      <section class="membership-title">
        <img
          v-if="membership.logoImage"
          :src="membership.logoImage"
          :alt="`${membership.providerName} 로고`"
          class="membership-logo-large"
        />

        <h1>
          {{ membership.providerName }}
        </h1>

      </section>



      <!-- 연동 정보 -->
      <section class="info-section">

        <h2>
          연동 정보
        </h2>


        <p v-if="membership.recommendMessage" class="provider-summary">
          {{ membership.recommendMessage }}
        </p>


        <dl class="info-rows">

          <div class="info-row">
            <dt>가맹점 이용현황</dt>
            <dd>{{ recentUse }}</dd>
          </div>

        </dl>

      </section>




      <!-- 주요 사용처 -->
      <section v-if="membership.usagePlaces?.length" class="info-section">

        <h2>
          주요 사용처
        </h2>


        <ul class="usage-place-list">

          <li v-for="place in membership.usagePlaces" :key="place.usagePlaceId || place.placeName">
            {{ place.placeName }}
          </li>

        </ul>


      </section>




      <!-- 공식 사이트 -->
      <button
        v-if="membership.officialSiteUrl"
        class="official-button"
        @click="goOfficialWebsite"
      >

        공식 사이트 이동

      </button>



      <!-- 삭제 -->
      <button
  class="delete-button"
  @click="openDeleteModal"
>

  멤버십 삭제

</button>

    </main>



    <BottomNavigation />


  </div>

  <!-- 삭제 확인 팝업 -->
<div
  v-if="showDeleteModal"
  class="modal-overlay"
>

  <div class="modal">


    <h3>
      {{ membership.providerName }} 멤버십을 삭제하시겠어요?
    </h3>


    <p>
      삭제하면 결제 추천에서 {{ membership.providerName }} 혜택 안내를 받을 수 없습니다.
    </p>


    <div class="modal-buttons">

      <button
        class="cancel-button"
        @click="closeDeleteModal"
      >
        취소
      </button>


      <button
        class="confirm-button"
        @click="deleteMembership"
      >
        삭제
      </button>

    </div>

  </div>

</div>


</template>




<style scoped>

.membership-detail-page {

  min-height: 100vh;

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;

}



.content {

  padding: var(--space-lg);

}



.membership-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-bottom: var(--space-xl);
}

.membership-logo-large {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-lg);
  object-fit: cover;
  margin-bottom: var(--space-lg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.membership-title h1 {

  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  line-height: 1.2;

  margin: 0;

  color: var(--color-text-primary);

}



.info-section {

  margin-bottom: var(--space-xl);

  padding: var(--space-md);

  border-radius: var(--radius-lg);

  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);

}

.info-section + .info-section {
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--color-border);
}



.info-section h2 {

  font-size: var(--font-md);

  margin-bottom: var(--space-sm);

  color: var(--color-text-primary);

  font-weight: var(--font-semibold);

}



.info-section p,
.info-section li {

  font-size: var(--font-sm);

  color: var(--color-text-primary);

}



.provider-summary {

  margin: 0 0 var(--space-md);

  padding-bottom: var(--space-md);

  border-bottom: 1px solid rgba(var(--color-primary-dark-rgb), 0.15);

  color: var(--color-text-secondary);

}



.info-rows {

  margin: 0;

}



.info-row {

  display: flex;

  align-items: baseline;

  justify-content: space-between;

  gap: var(--space-md);

}



.info-row + .info-row {

  margin-top: var(--space-xs);

}



.info-row dt {

  flex: 0 0 auto;

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

}



.info-row dd {

  margin: 0;

  text-align: right;

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

}



.usage-place-list {

  display: grid;

  grid-template-columns: repeat(2, minmax(0, 1fr));

  gap: var(--space-md);

  margin: 0;

  padding: 0;

  list-style: none;

}

.usage-place-list li {

  padding: var(--space-sm) var(--space-md);

  background: rgba(var(--color-primary-rgb), 0.03);

  border: 1px solid rgba(var(--color-primary-rgb), 0.08);

  border-radius: var(--radius-md);

  font-size: var(--font-sm);

  color: var(--color-text-primary);

}



.error-message {

  margin-bottom: var(--space-md);

  font-size: var(--font-sm);

  color: var(--color-coral);

}



.official-button {

  width: 100%;

  height: 48px;

  margin-top: var(--space-md);

  border-radius: var(--radius-md);

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  border: none;

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.official-button:hover {

  opacity: 0.85;
  transform: translateY(-2px);

}



.delete-button {

  width: 100%;

  height: 48px;

  margin-top: var(--space-md);

  border-radius: var(--radius-md);

  background: transparent;

  color: var(--color-coral);

  border: 1.5px solid var(--color-coral);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.delete-button:hover {

  background: rgba(255, 100, 100, 0.1);
  opacity: 0.85;

}

.modal-overlay {

  position: fixed;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: overlay-fade-in 0.3s ease-out;

}

@keyframes overlay-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}


.modal {

  width: 90%;
  max-width: 320px;
  padding: 24px;
  border-radius: var(--radius-xl);
  text-align: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: modal-emerge 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-sizing: border-box;
  max-height: 90vh;
  overflow-y: auto;

}

@keyframes modal-emerge {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(30px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

[data-theme="dark"] .modal {

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}


.modal h3 {

  font-size: var(--font-lg);

  margin-bottom: var(--space-sm);

  color: var(--color-text-primary);

  font-weight: var(--font-semibold);

}


.modal p {

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

  line-height: 1.5;

  margin-bottom: var(--space-xl);

}


.modal-buttons {

  display: flex;

  gap: var(--space-sm);

}


.modal-buttons button {

  flex: 1;

  height: 44px;

  border-radius: var(--radius-sm);

  font-weight: var(--font-semibold);

  border: none;

  cursor: pointer;

}
 

.cancel-button {

  background: var(--color-border);

  color: var(--color-text-primary);

}


.confirm-button {

  background: var(--color-coral);

  color: var(--color-btn-primary-text);

}


</style>
