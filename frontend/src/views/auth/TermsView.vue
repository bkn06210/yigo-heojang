<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getTerms } from '@/api/authApi'
import AppButton from '@/components/common/AppButton.vue'
import AppModal from '@/components/common/AppModal.vue'

const router = useRouter()
const terms = ref([])
const agreeAll = ref(false)
const selectedTerm = ref(null)
const isLoading = ref(false)
const errorMessage = ref('')

const canNext = computed(() => (
  terms.value.length > 0 && terms.value
    .filter((term) => term.required)
    .every((term) => term.checked)
))

const toggleAll = () => {
  terms.value.forEach((term) => {
    term.checked = agreeAll.value
  })
}

const updateAgreeAll = () => {
  agreeAll.value = terms.value.length > 0 && terms.value.every((term) => term.checked)
}

const loadTerms = async () => {
  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await getTerms()
    terms.value = (response.data.data?.terms ?? []).map((term) => ({
      ...term,
      checked: false,
    }))
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '약관을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

const goSignup = () => {
  if (!canNext.value) return

  const agreements = terms.value.map((term) => ({
    termsVersionId: term.termsVersionId,
    agreed: term.checked,
  }))
  sessionStorage.setItem('signupTermsAgreements', JSON.stringify(agreements))
  router.push('/auth/signup')
}

onMounted(loadTerms)
</script>

<template>
  <div class="terms-view">
    <h1>약관 동의</h1>
    <p class="description">회원가입을 위해 약관을 확인하고 동의해주세요.</p>

    <p v-if="isLoading">약관을 불러오는 중입니다.</p>
    <div v-else-if="errorMessage" class="error-box">
      <p>{{ errorMessage }}</p>
      <button type="button" @click="loadTerms">다시 시도</button>
    </div>

    <template v-else>
      <label class="all-agreement">
        <input v-model="agreeAll" type="checkbox" @change="toggleAll">
        전체 동의
      </label>

      <div v-for="term in terms" :key="term.termsVersionId" class="term-item">
        <label>
          <input v-model="term.checked" type="checkbox" @change="updateAgreeAll">
          {{ term.required ? '(필수)' : '(선택)' }} {{ term.termsName }}
        </label>
        <button type="button" @click="selectedTerm = term">더보기</button>
      </div>

      <AppButton text="다음" :disabled="!canNext" @click="goSignup" />
    </template>

    <AppModal
      :visible="Boolean(selectedTerm)"
      :title="selectedTerm?.termsName"
      @close="selectedTerm = null"
    >
      <p class="term-content">{{ selectedTerm?.content }}</p>
    </AppModal>
  </div>
</template>

<style scoped>
.terms-view { padding: 24px; }
.description { margin-bottom: 28px; color: #666; }
.all-agreement { display: block; padding: 18px 0; border-bottom: 1px solid #ddd; font-weight: 700; }
.term-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 0; }
input { width: 18px; height: 18px; vertical-align: middle; }
button { cursor: pointer; }
.term-item button { border: 0; background: none; color: #666; text-decoration: underline; }
.error-box { margin: 24px 0; color: #d93025; }
.term-content { max-height: 50vh; overflow: auto; white-space: pre-wrap; line-height: 1.6; }
</style>
