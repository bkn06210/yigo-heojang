<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const { showToast } = useToast()


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
    showToast('warning', '필수 약관에 동의해주세요.')
    return
  }


  router.push('/auth/signup')

}

</script>


<template>
  <div class="terms-page">
    <div class="terms-container">

      <h1>약관 동의</h1>


    <label class="agree-all-label">
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

        <span :class="{ 'required-badge': term.required, 'optional-badge': !term.required }">
          {{ term.required ? '(필수)' : '(선택)' }}
        </span>

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
  </div>
</template>

 
<style scoped>

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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.terms-container > button {
  width: 100%;
  height: 48px;
  margin-top: var(--space-lg);
  font-size: var(--font-md);
  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
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

.agree-all-label {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md) 0;
  margin-bottom: var(--space-md);
  background: rgba(var(--color-primary-rgb), 0.05);
  border-radius: var(--radius-md);
  font-weight: var(--font-semibold);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
}

label input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.required-badge {
  font-weight: var(--font-bold);
  color: var(--color-coral);
}

.optional-badge {
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
}

.term-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.term-item label {
  flex: 1;
  gap: var(--space-sm);
}

.term-item button {
  padding: var(--space-xs) var(--space-sm);
  font-size: var(--font-xs);
  white-space: nowrap;
  background: transparent;
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.5);
  color: var(--color-primary-dark);
}

.term-item button:hover {
  background: rgba(var(--color-primary-dark-rgb), 0.08);
  border-color: rgba(var(--color-primary-dark-rgb), 0.7);
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
}

</style>
