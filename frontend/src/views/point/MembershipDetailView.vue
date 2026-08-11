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
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  object-fit: cover;
  margin-bottom: var(--space-md);
}

.membership-title h1 {

  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);

  margin: 0;

  color: var(--color-text-primary);

}



.info-section {

  margin-bottom: var(--space-xl);

  padding: var(--space-md);

  border-radius: var(--radius-lg);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.08) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.15);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03), inset 0 1px 0 rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

}

[data-theme="dark"] .info-section {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.22);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);

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

  gap: var(--space-xs) var(--space-md);

  margin: 0;

  padding-left: var(--space-lg);

}



.error-message {

  margin-bottom: var(--space-md);

  font-size: var(--font-sm);

  color: var(--color-coral);

}



.official-button {

  width: 100%;

  height: 48px;

  border-radius: var(--radius-md);

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  border: none;

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.official-button:hover {

  opacity: 0.9;

}



.delete-button {

  width: 100%;

  height: 48px;

  margin-top: var(--space-sm);

  border-radius: var(--radius-md);

  background: linear-gradient(135deg, rgba(168, 78, 104, 0.1) 0%, rgba(168, 78, 104, 0.03) 100%);

  color: var(--color-coral);

  border: 1px solid rgba(168, 78, 104, 0.25);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

  backdrop-filter: blur(6px);

  -webkit-backdrop-filter: blur(6px);

}

.delete-button:hover {

  background: linear-gradient(135deg, rgba(168, 78, 104, 0.16) 0%, rgba(168, 78, 104, 0.05) 100%);

}

[data-theme="dark"] .delete-button {

  background: linear-gradient(135deg, rgba(209, 123, 147, 0.16) 0%, rgba(209, 123, 147, 0.05) 100%);
  border: 1px solid rgba(209, 123, 147, 0.3);

}

.modal-overlay {

  position: fixed;

  inset: 0;

  background: rgba(0,0,0,0.4);

  display: flex;

  justify-content: center;

  align-items: center;

  z-index: var(--z-modal);

}


.modal {

  width: calc(100% - 40px);

  border-radius: var(--radius-lg);

  padding: var(--space-xl);

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, var(--color-surface) 60%);

  border: 1px solid rgba(255, 255, 255, 0.3);

  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  backdrop-filter: blur(12px);

  -webkit-backdrop-filter: blur(12px);

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
