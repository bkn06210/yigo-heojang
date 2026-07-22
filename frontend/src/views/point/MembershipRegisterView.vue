<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()


// 검색어
const keyword = ref('')


// 팝업 상태
const showModal = ref(false)


// 선택한 멤버십
const selectedMembership = ref(null)


// 임시 멤버십 데이터
// 추후 API 응답 데이터로 교체 예정
const membershipList = ref([
  {
    id: 1,
    name: 'CJ ONE',
    mainUses: [
      '뚜레쥬르',
      '올리브영',
      'CGV'
    ]
  },
  {
    id: 2,
    name: '해피포인트',
    mainUses: [
      '파리바게뜨',
      '던킨',
      '배스킨라빈스'
    ]
  },
  {
    id: 3,
    name: 'KT 멤버십',
    mainUses: [
      '편의점',
      '영화관',
      '카페'
    ]
  }
])


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
const addMembership = (membership) => {

  selectedMembership.value = membership

  showModal.value = true

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

        <span>
          {{ membership.name }}
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