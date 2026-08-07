<script setup>

import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteMembership as removeMembership, getMembership } from '@/api/walletApi'
import { getOfficialSiteUrl } from '@/utils/partnerSites'
import { getPartnerUsagePlaces } from '@/utils/partnerUsagePlaces'
import { getPartnerLogo } from '@/utils/partnerLogos'


// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNavigation from '@/components/layout/BottomNavigation.vue'


// 삭제 팝업 상태
const showDeleteModal = ref(false)
const route = useRoute()
const router = useRouter()
const membership = ref({ providerName: '', registeredAt: '', totalPoint: 0, usagePlaces: [] })
const errorMessage = ref('')

const loadMembership = async () => {
  try {
    const data = await getMembership(route.params.id)
    membership.value = {
      ...data,
      logo: getPartnerLogo(data.providerName, data.logoImageUrl),
      usagePlaces: getPartnerUsagePlaces(data.providerName, data.usagePlaces),
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '멤버십 상세 정보를 불러오지 못했습니다.'
  }
}


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
  } finally {
    showDeleteModal.value = false
  }
}

const openOfficialSite = () => {
  const url = getOfficialSiteUrl(membership.value.providerName, membership.value.officialSiteUrl)
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(loadMembership)


</script>



<template>

  <div class="membership-detail-page">


    <!-- 헤더 -->
    <PageHeader title="멤버십 상세" @back="router.back()" />



    <main class="content">

      <p v-if="errorMessage">{{ errorMessage }}</p>


      <!-- 멤버십 이름 -->
      <section class="membership-title">

        <img
          v-if="membership.logo"
          :src="membership.logo"
          :alt="`${membership.providerName} 로고`"
          class="membership-logo"
        />

        <h1>
          {{ membership.providerName }}
        </h1>

      </section>



      <!-- 등록 정보 -->
      <section class="info-section">

        <h2>
          등록 정보
        </h2>


        <p>
          {{ membership.registeredAt || '등록 정보 없음' }}
        </p>

      </section>




      <!-- 주요 사용처 -->
      <section class="info-section">

        <h2>
          주요 사용처
        </h2>


        <ul>

          <li v-for="place in membership.usagePlaces" :key="place.usagePlaceId || place.placeName">
            {{ place.placeName }}
          </li>

        </ul>


      </section>




      <!-- 공식 사이트 -->
      <button class="official-button" @click="openOfficialSite">

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



    <BottomNav />


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

    <BottomNavigation />


  </div>

</div>


</template>




<style scoped>

.membership-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.membership-logo {
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  border-radius: 14px;
  object-fit: contain;
  background: #fff;
}

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



.membership-title h1 {

  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);

  margin-bottom: var(--space-xl);

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
<<<<<<< HEAD
=======
<!-- 07_25 연동 변경: 멤버십 상세 API와 공식 사이트·주요 사용처 정보를 연결한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
