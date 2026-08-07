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
    <PageHeader title="멤버십 상세" />



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

  padding-bottom: 80px;

}



.content {

  padding: 20px;

}



.membership-title h1 {

  font-size: 24px;

  margin-bottom: 24px;

}



.info-section {

  margin-bottom: 28px;

}



.info-section h2 {

  font-size: 16px;

  margin-bottom: 12px;

}



.info-section p,
.info-section li {

  font-size: 15px;

  color: #333;

}



.official-button {

  width: 100%;

  height: 48px;

  border-radius: 12px;

  background: #1d4ed8;

  color: white;

}



.delete-button {

  width: 100%;

  height: 48px;

  margin-top: 12px;

  border-radius: 12px;

  background: white;

  color: #ef4444;

  border: 1px solid #ef4444;

}

.modal-overlay {

  position: fixed;

  inset: 0;

  background: rgba(0,0,0,0.4);

  display: flex;

  justify-content: center;

  align-items: center;

  z-index: 100;

}


.modal {

  width: calc(100% - 40px);

  background: white;

  border-radius: 16px;

  padding: 24px;

}


.modal h3 {

  font-size: 18px;

  margin-bottom: 12px;

}


.modal p {

  font-size: 14px;

  color: #666;

  line-height: 1.5;

  margin-bottom: 24px;

}


.modal-buttons {

  display: flex;

  gap: 12px;

}


.modal-buttons button {

  flex: 1;

  height: 44px;

  border-radius: 10px;

}
 

.cancel-button {

  background: #f3f4f6;

}


.confirm-button {

  background: #ef4444;

  color: white;

}


</style>
<!-- 07_25 연동 변경: 멤버십 상세 API와 공식 사이트·주요 사용처 정보를 연결한다. -->
