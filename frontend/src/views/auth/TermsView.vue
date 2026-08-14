<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import { getTerms } from '@/api/authApi'

const router = useRouter()
const { showToast } = useToast()


// 전체 동의
const agreeAll = ref(false)


const terms = ref([])
const loading = ref(true)
const loadError = ref('')
const expandedTermId = ref(null)

const loadTerms = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const response = await getTerms()
    terms.value = (response?.terms ?? []).map((term) => ({
      ...term,
      id: term.termsId,
      title: term.termsName,
      checked: false,
    }))
  } catch (error) {
    loadError.value = error.response?.data?.message || '약관을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(loadTerms)


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
  expandedTermId.value = expandedTermId.value === term.id ? null : term.id
}


// 다음
const goSignup = () => {

  if (!canNext.value) {
    showToast('warning', '필수 약관에 동의해주세요.')
    return
  }


  sessionStorage.setItem(
    'signupTermsAgreements',
    JSON.stringify(terms.value.map((term) => ({
      termsVersionId: term.termsVersionId,
      agreed: term.checked,
    })))
  )

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



    <p v-if="loading" class="state-message">약관을 불러오는 중입니다...</p>
    <div v-else-if="loadError" class="state-block">
      <p class="state-message error">{{ loadError }}</p>
      <button type="button" class="retry-button" @click="loadTerms">다시 시도</button>
    </div>

    <div
      v-for="term in terms"
      :key="term.id"
      class="term-row"
    >
      <div class="term-item">
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
          class="term-toggle-button"
          :aria-expanded="expandedTermId === term.id"
          @click="openDetail(term)"
        >
          {{ expandedTermId === term.id ? '접기' : '더보기' }}
        </button>
      </div>

      <section
        v-if="expandedTermId === term.id"
        class="term-detail"
        :aria-label="`${term.title} 상세 내용`"
      >
        <div class="term-detail-meta">
          <strong>{{ term.title }}</strong>
          <span v-if="term.version">버전 {{ term.version }}</span>
        </div>
        <p>{{ term.content || '등록된 약관 내용이 없습니다.' }}</p>
      </section>
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

.term-row {
  border-bottom: 1px solid var(--color-border);
}

.term-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
  padding: var(--space-md) 0;
}

.term-item label {
  flex: 1;
  gap: var(--space-sm);
}

.term-toggle-button {
  padding: var(--space-xs) 0;
  font-size: var(--font-xs);
  white-space: nowrap;
  background: transparent;
  border: 0;
  border-radius: 0;
  color: var(--color-primary-dark);
  box-shadow: none;
}

.term-toggle-button:hover,
.term-toggle-button:focus-visible {
  background: transparent;
  color: var(--color-primary);
  text-decoration: underline;
  transform: none;
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

.state-block {
  text-align: center;
}

.state-message {
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

.state-message.error {
  color: var(--color-error, #e74c3c);
}

.retry-button {
  margin-top: var(--space-sm);
}

.term-detail {
  margin-bottom: var(--space-md);
  padding: var(--space-md);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.18);
  border-radius: var(--radius-md);
  background: rgba(var(--color-primary-rgb), 0.04);
}

.term-detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.term-detail-meta strong {
  color: var(--color-text-primary);
  font-size: var(--font-sm);
}

.term-detail-meta span {
  color: var(--color-text-tertiary);
  font-size: var(--font-xs);
  white-space: nowrap;
}

.term-detail p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

</style>
