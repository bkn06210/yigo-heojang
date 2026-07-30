<script setup>
import { ref, onMounted } from 'vue'
import { getTransactionsByMemberId } from '@/api/transactionApi'

const transactions = ref([])
const loading = ref(false)
const error = ref('')

const fetchTransactions = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await getTransactionsByMemberId(1)

    console.log('소비내역 목록 응답:', response.data)

    const data = response.data?.data || response.data || []

    transactions.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('소비내역 목록 조회 실패:', err)
    error.value = '소비내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchTransactions()
})
</script>
<template>
  <div>
    <h1>소비내역</h1>

    <p v-if="loading">불러오는 중...</p>
    <p v-if="error">{{ error }}</p>

    <div
        v-for="transaction in transactions"
        :key="transaction.expenseId"
        style="padding: 12px; border-bottom: 1px solid #ddd;"
    >
      <p>가맹점: {{ transaction.merchantName }}</p>
      <p>카테고리: {{ transaction.categoryName }}</p>
      <p>금액: {{ transaction.paymentAmount }}원</p>
      <p>날짜: {{ transaction.paymentDate }}</p>
    </div>
  </div>
</template>

<style scoped>

</style>
