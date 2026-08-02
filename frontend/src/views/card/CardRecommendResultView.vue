<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import AppButton from '@/components/common/AppButton.vue'
import PageHeader from '@/components/common/PageHeader.vue'

const router = useRouter()

const stored = (() => {
  try {
    return JSON.parse(sessionStorage.getItem('cardRecommendation'))
  } catch {
    return null
  }
})()

const recommendations = computed(() => stored?.result?.recommendations ?? [])
const amount = computed(() => stored?.request?.expectedAmount ?? 0)
const formatMoney = (value) => new Intl.NumberFormat('ko-KR').format(value ?? 0)

const benefitLabel = (kind) => ({
  DISCOUNT: '할인',
  POINT: '포인트',
  CASHBACK: '캐시백',
}[kind] ?? '혜택')
</script>

<template>
  <div class="result-view">
    <PageHeader title="추천 결과" @back="router.go(-1)" />

    <main class="content">
      <p v-if="amount" class="amount">{{ formatMoney(amount) }}원 결제 기준</p>

      <div v-if="recommendations.length" class="result-list">
        <article v-for="card in recommendations" :key="card.userCardId" class="card">
          <div class="rank">{{ card.rank }}위</div>
          <div class="card-info">
            <div class="title-row">
              <h2>{{ card.cardName }}</h2>
              <span v-if="card.dynamicSwitch" class="switch-badge">이번 달 최적</span>
            </div>
            <strong>{{ formatMoney(card.expectedBenefit) }}원 {{ benefitLabel(card.benefitKind) }}</strong>
            <p>{{ card.reason }}</p>
            <small>{{ card.isEstimate ? '예상 혜택' : '확정 혜택' }}</small>
          </div>
        </article>
      </div>

      <p v-else class="empty">추천 결과가 없습니다. 결제 정보를 다시 입력해주세요.</p>
      <AppButton text="조건 다시 입력" type="secondary" @click="router.push('/cards/recommend')" />
    </main>
  </div>
</template>

<style scoped>
.result-view { min-height: 100vh; background: #f7f8fa; }
.content { padding: 24px; }
.amount { margin: 0 0 16px; color: #666; }
.result-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
.card { display: flex; gap: 14px; padding: 18px; border-radius: 14px; background: white; box-shadow: 0 2px 8px rgba(0,0,0,.06); }
.rank { min-width: 38px; color: #4caf50; font-size: 18px; font-weight: 700; }
.card-info { flex: 1; }
.title-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
h2 { margin: 0 0 8px; font-size: 17px; }
strong { color: #222; }
p { line-height: 1.45; }
small { color: #888; }
.switch-badge { padding: 4px 7px; border-radius: 10px; background: #e8f5e9; color: #2e7d32; font-size: 11px; white-space: nowrap; }
.empty { padding: 40px 0; text-align: center; color: #777; }
</style>
