<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { getCardRecommendations } from '@/api/recommendationApi'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()
const expectedAmount = ref('')
const merchantId = ref('')
const categoryId = ref('')
const paymentType = ref('CARD')
const isLoading = ref(false)
const errorMessage = ref('')

const optionalNumber = (value) => value === '' ? undefined : Number(value)

const requestRecommendation = async () => {
  const amount = Number(expectedAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) {
    errorMessage.value = '예상 결제 금액을 0원보다 크게 입력해주세요.'
    return
  }

  const request = {
    expectedAmount: amount,
    paymentType: paymentType.value || undefined,
    merchantId: optionalNumber(merchantId.value),
    categoryId: optionalNumber(categoryId.value),
  }

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await getCardRecommendations(request)
    const result = response.data.data

    sessionStorage.setItem('cardRecommendation', JSON.stringify({ request, result }))
    router.push('/cards/recommend/result')
  } catch (error) {
    errorMessage.value = error.response?.data?.message ?? '카드 추천을 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="recommend-view">
    <PageHeader title="결제 카드 추천" @back="router.go(-1)" />

    <main class="content">
      <p class="description">결제 정보를 입력하면 보유 카드 중 예상 혜택이 큰 순서로 추천합니다.</p>

      <label>예상 결제 금액 *</label>
      <AppInput v-model="expectedAmount" type="number" placeholder="예: 10000" />

      <label>가맹점 ID (선택)</label>
      <AppInput v-model="merchantId" type="number" placeholder="예: 1" />

      <label>카테고리 ID (선택)</label>
      <AppInput v-model="categoryId" type="number" placeholder="예: 3" />

      <label for="payment-type">결제 수단</label>
      <select id="payment-type" v-model="paymentType">
        <option value="CARD">카드</option>
        <option value="">선택 안 함</option>
      </select>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <AppButton
        :text="isLoading ? '추천 계산 중...' : '추천받기'"
        :disabled="isLoading"
        @click="requestRecommendation"
      />
    </main>
  </div>
</template>

<style scoped>
.recommend-view { min-height: 100vh; background: #f7f8fa; }
.content { display: flex; flex-direction: column; gap: 12px; padding: 24px; }
.description { margin: 0 0 12px; color: #666; line-height: 1.5; }
label { margin-top: 8px; font-size: 14px; font-weight: 600; }
select { height: 44px; padding: 0 12px; border: 1px solid #ddd; border-radius: 8px; background: white; }
.error { color: #d93025; font-size: 14px; }
</style>
