<<<<<<< HEAD
﻿<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()
=======
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTerms } from '@/api/authApi'

const router = useRouter()
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


// 전체 동의
const agreeAll = ref(false)


<<<<<<< HEAD
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
=======
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
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


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


<<<<<<< HEAD
// 상세보기
// 추후 백 API 연결 위치
const openDetail = (term) => {

  console.log('약관 상세:', term.id)

=======
const openDetail = (term) => {
  alert(term.content || '약관 내용이 없습니다.')
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


// 다음
const goSignup = () => {

  if (!canNext.value) {
<<<<<<< HEAD
    showToast('warning', '필수 약관에 동의해주세요.')
=======
    alert('필수 약관에 동의해주세요.')
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    return
  }


<<<<<<< HEAD
=======
  sessionStorage.setItem('termsAgreements', JSON.stringify(
    terms.value.map((term) => ({
      termsVersionId: term.versionId,
      agreed: term.checked,
    }))
  ))

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  router.push('/auth/signup')

}

<<<<<<< HEAD
=======
onMounted(loadTerms)

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</script>


<template>
<<<<<<< HEAD
  <div class="terms-page">
    <div class="terms-container">

      <h1>약관 동의</h1>

=======
  <div class="terms">

    <h1>약관 동의</h1>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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



<<<<<<< HEAD
      <button
        :disabled="!canNext"
        @click="goSignup"
      >
        다음
      </button>

    </div>
=======
    <button
      :disabled="!canNext"
      @click="goSignup"
    >
      다음
    </button>


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  </div>
</template>

 
<style scoped>

<<<<<<< HEAD
.terms-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 15% 15%, rgba(var(--color-primary-dark-rgb), 0.2) 0%, transparent 45%),
    radial-gradient(circle at 85% 85%, rgba(var(--color-primary-dark-rgb), 0.15) 0%, transparent 45%),
    var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  box-sizing: border-box;
}

.terms-container {
  width: 100%;
  max-width: 480px;
  background: var(--color-surface);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-sizing: border-box;
  box-shadow: var(--shadow-lg);
}

.terms-container > button {
  margin-top: var(--space-lg);
  padding: var(--space-md) var(--space-lg);
  font-size: var(--font-md);
}

h1 {
  font-size: var(--font-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2xl);
}

label {
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
}

label input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.term-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-lg);
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.term-item label {
  gap: var(--space-sm);
}

.term-item button {
  padding: 6px 12px;
  font-size: var(--font-xs);
  white-space: nowrap;
}

button {
  cursor: pointer;
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border: none;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-weight: var(--font-semibold);
  transition: all var(--transition-fast);
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
}

button:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
=======
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
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

</style>
<!-- 07_25 연동 변경: 약관 동의 결과를 회원가입 API 입력에 포함하도록 보완했다. -->
