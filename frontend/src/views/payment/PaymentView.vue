<script setup>

import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePaymentStore } from '@/stores/payment';
import { computed } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';
import PaymentResultModal from '@/components/payment/PaymentResultModal.vue';


const paymentStore = usePaymentStore();

const router = useRouter();

const amount = computed(() => paymentStore.paymentAmount);

const card = computed(() => paymentStore.selectedCard);

const membershipBenefit = computed(() => paymentStore.membershipBenefit);

// 결제 결과 모달 상태
const showResult = ref(false);
const success = ref(true);


// 결제하기 → PaymentRecommendView로 이동
const payment = () => {
  router.push({ name: 'PaymentRecommend' });
};

// 뒤로가기
const goBack = () => {
  router.back();
};


</script>

<template>

<div class="payment-page">

<PageHeader title="결제" :show-back="false"/>


<main>

<!-- 카드 정보 -->
<section v-if="card" class="card">
  <img :src="card.image" :alt="card.name" />
  <p>{{ card.name }}</p>
</section>

<!-- 부채꼴 카드 배치 -->
<section class="card-fan">
  <!-- 왼쪽 위 -->
  <div class="card-item card-1"></div>

  <!-- 왼쪽 아래 -->
  <div class="card-item card-2"></div>

  <!-- 중앙: 결제하기 (큰 카드) -->
  <button class="card-item card-primary" @click="payment">
    결제하기
  </button>

  <!-- 오른쪽 아래 -->
  <div class="card-item card-3"></div>

  <!-- 오른쪽 위 -->
  <div class="card-item card-4"></div>
</section>

</main>


<!--
  결제 결과 모달

  close 이벤트:
  모달 닫기 처리
-->
<PaymentResultModal

  v-if="showResult"

  :success="success"

  :amount="amount"

  :card="card"

  :membershipBenefit="membershipBenefit"

  @close="showResult = false"

/>

<!--
  QR 유효시간 종료 모달
-->
<PaymentExpireModal

  v-if="showExpireModal"

  @refresh="refreshQR"

  @cancel="cancelPayment"

/>



</div>


</template>


<style scoped>

.payment-page {

  min-height: 100vh;

  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl));

  background: var(--color-bg);

  margin: 0 auto;
  max-width: 480px;

  box-sizing: border-box;
  overflow: hidden visible;

}

main {

  display: flex;
  flex-direction: column;
  align-items: center;

}

/* 선택 카드 (Soft Glassmorphism) */
.card {

  display: flex;
  align-items: center;
  gap: var(--space-sm);

  width: 100%;
  margin-top: var(--space-lg);
  padding: var(--space-sm) var(--space-md);

  border-radius: var(--radius-md);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  box-sizing: border-box;

}

[data-theme="dark"] .card {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.06) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);

}

.card img {

  width: 48px;
  height: 30px;

  border-radius: var(--radius-xs);

  object-fit: cover;

}

.card p {

  margin: 0;

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

/* 부채꼴 액션 영역 */
.fan-actions-wrapper {
  position: relative;
  width: 100%;
  height: 420px;
  margin-top: var(--space-2xl);
}

/* 배경: 부채꼴 액션 버튼 */
.fan-actions {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

/* 기본 부채꼴 버튼 */
.fan-btn {
  position: absolute;
  width: 70px;
  height: 70px;
  border: none;
  border-radius: 50%;
  font-size: var(--font-xs);
  font-weight: var(--font-bold);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

/* 중앙: 결제하기 (주요 버튼) */
.btn-primary {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #FFD60A, #FFC300);
  color: #000;
  z-index: 100;
  box-shadow: 0 12px 32px rgba(255, 195, 0, 0.35);
}

.btn-primary:hover {
  transform: scale(1.1);
  box-shadow: 0 16px 40px rgba(255, 195, 0, 0.45);
}

.btn-primary:active {
  transform: scale(0.95);
}

/* 부채꼴 카드 영역 */
.card-fan {
  position: relative;
  width: 100%;
  height: 500px;
  margin-top: var(--space-2xl);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 기본 카드 스타일 */
.card-item {
  position: absolute;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.15) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: all var(--transition-fast);
  cursor: pointer;
}

.card-item:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

/* 작은 카드 (70x110px, 신용카드 비율 1.586:1) */
.card-1, .card-2, .card-3, .card-4 {
  width: 70px;
  height: 110px;
  opacity: 0.8;
}

/* 중앙: 결제하기 카드 (100x160px) */
.card-primary {
  width: 100px;
  height: 160px;
  background: linear-gradient(135deg, #FFD60A, #FFC300);
  color: #000;
  border: none;
  font-size: var(--font-sm);
  font-weight: var(--font-bold);
  z-index: 100;
  box-shadow: 0 12px 32px rgba(255, 195, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-primary:hover {
  transform: scale(1.08);
  box-shadow: 0 16px 40px rgba(255, 195, 0, 0.45);
}

.card-primary:active {
  transform: scale(0.95);
}

/* 부채꼴 배치 */
.card-1 {
  left: calc(50% - 140px);
  top: calc(50% - 120px);
  z-index: 40;
}

.card-2 {
  left: calc(50% - 100px);
  top: calc(50% + 100px);
  z-index: 30;
}

.card-3 {
  left: calc(50% + 100px);
  top: calc(50% + 100px);
  z-index: 30;
}

.card-4 {
  left: calc(50% + 140px);
  top: calc(50% - 120px);
  z-index: 40;
}


</style>