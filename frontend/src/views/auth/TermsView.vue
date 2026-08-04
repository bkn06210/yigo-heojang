<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()


// 전체 동의
const agreeAll = ref(false)


// 약관 목록
// 추후 백 API 응답 데이터로 교체
const terms = ref([
  {
    id: 1,
    title: '서비스 이용약관',
    required: true,
    checked: false
  },
  {
    id: 2,
    title: '개인정보 수집 및 이용',
    required: true,
    checked: false
  },
  {
    id: 3,
    title: '마케팅 정보 수신',
    required: false,
    checked: false
  }
])


// 전체 동의 클릭
const toggleAll = () => {

  terms.value.forEach(term => {
    term.checked = agreeAll.value
  })

}


// 개별 선택 변경 시 전체 동의 상태 변경
const updateAgreeAll = () => {

  agreeAll.value = terms.value.every(
    term => term.checked
  )

}


// 필수 약관 체크 여부
const canNext = computed(() => {

  return terms.value
    .filter(term => term.required)
    .every(term => term.checked)

})


// 상세보기
// 추후 백 API 연결 위치
const openDetail = (term) => {

  console.log('약관 상세:', term.id)

}


// 다음
const goSignup = () => {

  if (!canNext.value) {
    alert('필수 약관에 동의해주세요.')
    return
  }


  router.push('/auth/signup')

}

</script>


<template>
  <div class="terms">

    <h1>약관 동의</h1>


    <label>
      <input
        type="checkbox"
        v-model="agreeAll"
        @change="toggleAll"
      >

      전체 동의
    </label>



    <div
      v-for="term in terms"
      :key="term.id"
      class="term-item"
    >

      <label>

        <input
          type="checkbox"
          v-model="term.checked"
          @change="updateAgreeAll"
        >


        {{ term.required ? '(필수)' : '(선택)' }}

        {{ term.title }}

      </label>


      <button
        type="button"
        @click="openDetail(term)"
      >
        더보기
      </button>

    </div>



    <button
      :disabled="!canNext"
      @click="goSignup"
    >
      다음
    </button>


  </div>
</template>

 
<style scoped>

.terms {
  padding:24px;
}


.term-item {
  display:flex;
  justify-content:space-between;
  margin-top:16px;
}


button {
  cursor:pointer;
}

</style>
