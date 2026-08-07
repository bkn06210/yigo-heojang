<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTerms } from '@/api/authApi'

const router = useRouter()


// 전체 동의
const agreeAll = ref(false)


const terms = ref([])
const errorMessage = ref('')

const loadTerms = async () => {
  try {
    const response = await getTerms()
    terms.value = (response.data?.data?.terms || []).map((term) => ({
      id: term.termsId,
      versionId: term.termsVersionId,
      title: term.termsName,
      content: term.content,
      required: Boolean(term.required),
      checked: false,
    }))
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '약관을 불러오지 못했습니다.'
  }
}


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


const openDetail = (term) => {
  alert(term.content || '약관 내용이 없습니다.')
}


// 다음
const goSignup = () => {

  if (!canNext.value) {
    alert('필수 약관에 동의해주세요.')
    return
  }


  sessionStorage.setItem('termsAgreements', JSON.stringify(
    terms.value.map((term) => ({
      termsVersionId: term.versionId,
      agreed: term.checked,
    }))
  ))

  router.push('/auth/signup')

}

onMounted(loadTerms)

</script>


<template>
  <div class="terms">

    <h1>약관 동의</h1>

    <p v-if="errorMessage">{{ errorMessage }}</p>


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
<!-- 07_25 연동 변경: 약관 동의 결과를 회원가입 API 입력에 포함하도록 보완했다. -->
