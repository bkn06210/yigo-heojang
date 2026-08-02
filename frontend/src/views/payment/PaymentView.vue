<script setup>
import { useRouter } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'

const router = useRouter()
</script>

<template>
  <div class="payment-view">
    <h1>결제</h1>
    <p>결제하기 전에 가장 혜택이 큰 카드를 확인해보세요.</p>
    <AppButton text="최적 카드 추천받기" @click="router.push('/cards/recommend')" />
  </div>
</template>

<style scoped>
.payment-view { padding: 24px; }
p { margin: 16px 0 24px; color: #666; }
</style>
