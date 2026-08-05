<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMembershipProviders, registerMembership } from '@/api/walletApi'
import { getPartnerUsagePlaces } from '@/utils/partnerUsagePlaces'
import { getPartnerLogo } from '@/utils/partnerLogos'


const router = useRouter()


// 검색어
const keyword = ref('')


// 팝업 상태
const showModal = ref(false)


// 선택한 멤버십
const selectedMembership = ref(null)


// 임시 멤버십 데이터
// 추후 API 응답 데이터로 교체 예정
const membershipList = ref([])
const errorMessage = ref('')

const loadProviders = async () => {
  try {
    const data = await getMembershipProviders()
    membershipList.value = (data?.providers || data || [])
      .filter((provider) => !provider.isRegistered)
      .map((provider) => ({
        id: Number(provider.pointProviderId),
        name: provider.providerName,
        logo: getPartnerLogo(provider.providerName, provider.logoImageUrl),
        mainUses: getPartnerUsagePlaces(provider.providerName).slice(0, 3).map((place) => place.placeName),
      }))
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '제휴 멤버십을 불러오지 못했습니다.'
  }
}


// 검색 결과
const searchResult = computed(() => {

  if (!keyword.value.trim()) {
    return []
  }


  return membershipList.value.filter((membership) =>
    membership.name
      .toLowerCase()
      .includes(keyword.value.toLowerCase())
  )

})

// 검색어 강조 표시
const highlightKeyword = (name, searchText) => {

  if (!searchText) {
    return name
  }


  const regex = new RegExp(`(${searchText})`, 'gi')


  return name.replace(
    regex,
    '<span class="highlight">$1</span>'
  )

}


// 많이 사용하는 멤버십
const popularMemberships = computed(() => {
  return membershipList.value
})


// 멤버십 추가 클릭
const addMembership = async (membership) => {
  try {
    await registerMembership(membership.id)
    selectedMembership.value = membership
    showModal.value = true
    membershipList.value = membershipList.value.filter((item) => item.id !== membership.id)
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '멤버십 추가에 실패했습니다.'
  }
}


// 팝업 닫기
const closeModal = () => {

  showModal.value = false

  selectedMembership.value = null

  keyword.value = ''

}

// 멤버십 목록 이동
const goMembershipList = () => {

  showModal.value = false

  keyword.value = ''

  router.push('/points')

}

//선택 함수 추가
const selectedSearchMembership = ref(null)

const selectMembership = (membership) => {

  selectedSearchMembership.value = membership

  keyword.value = membership.name

}

// 자동완성 선택
const selectSuggestion = (membership) => {

  keyword.value = membership.name

}

onMounted(loadProviders)

</script>


<template>

  <div class="membership-register">


    <!-- 헤더 -->
    <h1>
      멤버십 추가
    </h1>



    <!-- 안내 문구 -->
    <p class="description">
      자주 사용하는 멤버십을 추가해보세요
    </p>

    <p v-if="errorMessage" class="description">{{ errorMessage }}</p>



    <!-- 검색 -->
    <div class="search-box">

      <input
       :value="keyword"
        @input="keyword = $event.target.value"
         placeholder="멤버십 이름을 입력해주세요"
      />

    </div>



   <!-- 자동완성 제안 -->
<div
  v-if="searchResult.length"
  class="suggestion-box"
>

  <div
  v-for="membership in searchResult"
  :key="membership.id"
  class="suggestion-item"
  @click="selectSuggestion(membership)"
>

    <span
      v-html="highlightKeyword(membership.name, keyword)"
    ></span>


    <button
      @click.stop="addMembership(membership)"
    >
      추가
    </button>

  </div>

</div>




    <!-- 인기 멤버십 -->
    <section class="section">


      <h2>
        많이 사용하는 멤버십
      </h2>


      <div
        v-for="membership in popularMemberships"
        :key="membership.id"
        class="membership-item"
      >

        <span class="membership-label">
          <img
            v-if="membership.logo"
            :src="membership.logo"
            :alt="`${membership.name} 로고`"
            class="membership-logo"
          />
          <span>{{ membership.name }}</span>
        </span>


        <button
          @click="addMembership(membership)"
        >
          추가
        </button>


      </div>


    </section>




    <!-- 완료 팝업 -->
    <div
      v-if="showModal"
      class="modal-background"
    >

      <div class="modal">


        <h2>
          {{ selectedMembership.name }} 추가 완료!
        </h2>



        <p class="modal-title">
          주요 사용처
        </p>


        <ul>

          <li
            v-for="use in selectedMembership.mainUses"
            :key="use"
          >
            {{ use }}
          </li>

        </ul>


        <button
  class="confirm-button"
  @click="goMembershipList"
>
  멤버십 보러 가기
</button>


<button
  class="more-button"
  @click="closeModal"
>
  다른 멤버십 추가
</button>


      </div>

    </div>



  </div>

</template>



<style scoped>

.membership-register {

  padding: 20px;

}



h1 {

  font-size: 24px;

}



.description {

  margin: 16px 0;

  color: #555;

}



.search-box input {

  width: 100%;

  height: 44px;

  padding: 0 14px;

  border-radius: 10px;

  border: 1px solid #ddd;

}



.section {

  margin-top: 28px;

}



h2 {

  font-size: 17px;

  margin-bottom: 12px;

}



.membership-item {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 14px 0;

  border-bottom: 1px solid #eee;

}

.membership-label {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.membership-logo {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 12px;
  object-fit: contain;
  background: #fff;
}



button {

  border: none;

  background: #1d4ed8;

  color: white;

  border-radius: 8px;

  padding: 8px 14px;

}



.modal-background {

  position: fixed;

  inset: 0;

  background: rgba(0,0,0,0.4);

  display: flex;

  justify-content: center;

  align-items: center;

}



.modal {

  background: white;

  width: 80%;

  border-radius: 16px;

  padding: 24px;

}



.modal-title {

  margin-top: 20px;

  font-weight: 600;

}



.confirm-button {
  width: 100%;

  height: 44px;

  background: #1d4ed8;

  color: white;

}

.more-button {

  width: 100%;

  height: 44px;

  margin-top: 12px;

  background: #f3f4f6;

  color: #333;
  
}

.suggestion-box {

  margin-top: 4px;

  background: white;

  border: 1px solid #ddd;

  border-radius: 12px;

  overflow: hidden;

}


.suggestion-item {

  padding: 14px;

  border-bottom: 1px solid #eee;

  cursor: pointer;

}


.suggestion-item:last-child {

  border-bottom: none;

}


.suggestion-item:hover {

  background: #f8f8f8;
 
}


:deep(.highlight) {

  color: #1d4ed8;

  font-weight: 700;

}

.suggestion-item {

  display: flex;

  justify-content: space-between;

  align-items: center;

}

</style>
<!-- 07_25 연동 변경: 제휴 멤버십 목록 조회와 멤버십 추가 API를 연결한다. -->
