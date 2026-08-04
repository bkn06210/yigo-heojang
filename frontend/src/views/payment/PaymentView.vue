<script setup>

import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePaymentStore } from '@/stores/payment';
import { computed } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';

import PaymentQR from '@/components/payment/PaymentQR.vue';
import PaymentTimer from '@/components/payment/PaymentTimer.vue';
import PaymentSecurity from '@/components/payment/PaymentSecurity.vue';

import PaymentResultModal from '@/components/payment/PaymentResultModal.vue';
import PaymentExpireModal from '@/components/payment/PaymentExpireModal.vue';


const paymentStore = usePaymentStore();

const router = useRouter();

const amount = computed(() => paymentStore.paymentAmount);

const card = computed(() => paymentStore.selectedCard);

const membershipBenefit = computed(() => paymentStore.membershipBenefit);

// 결제 결과 모달 상태
const showResult = ref(false);

const qrSeconds = ref(60);
let qrTimer = null;

const showExpireModal = ref(false);

const success = ref(true);

/*
| QR 타이머 시작
|
| 1초마다 감소
|
| 0초가 되면 QR 만료 처리
*/
const startQRTimer = () => {


  // 기존 타이머 제거
  clearInterval(qrTimer);

  qrTimer = setInterval(() => {

    qrSeconds.value--;

    // QR 만료
    if(qrSeconds.value <= 0){


      clearInterval(qrTimer);


      showExpireModal.value = true;

    }

  },1000);


};


// 결제 처리
//
// 현재는 테스트용
// 실제 연결 시:
// 결제 API 호출
// ↓
// 응답 성공/실패 처리
const payment = ()=>{


  setTimeout(()=>{


    success.value = true;


    showResult.value = true;


  },1000);


};

/*
| 결제 취소
*/
const cancelPayment = () => {

  showExpireModal.value = false;

  router.back();

};

// QR 재발급
const refreshQR = () => {

  showExpireModal.value = false;

  qrSeconds.value = 60;

  startQRTimer();

};

/*
| 페이지 진입 시 QR 타이머 시작
*/
onMounted(()=>{

  startQRTimer();

});

/*
| 페이지 종료 시 타이머 제거
*/
onUnmounted(()=>{

  clearInterval(qrTimer);

});


</script>

<template>

<div class="payment-page">

<PageHeader title="결제" :show-back="false"/>


<main>


<section class="amount">

<p>
결제 금액
</p>

<h1>
{{amount.toLocaleString()}}원
</h1>

</section>

<!--
  선택한 카드 정보

  card가 없는 상태에서 화면이 먼저 렌더링될 수 있으므로
  v-if로 null 체크

  API 연결 후에도 동일하게 사용 가능
-->
<section
  v-if="card"
  class="card"
>

  <img
    :src="card.image"
    :alt="card.name"
  />

  <p>
    {{ card.name }}
  </p>

</section>


<!-- 부채꼴 액션 버튼 -->
<section class="fan-actions">
  <button class="fan-btn btn-refresh" @click="refreshQR" title="새로고침">
    새로고침
  </button>

  <button class="fan-btn btn-primary" @click="payment">
    결제하기
  </button>

  <button class="fan-btn btn-cancel" @click="cancelPayment" title="취소">
    취소
  </button>
</section>

<PaymentQR />


<PaymentTimer

  :seconds="qrSeconds"

/>

<p class="scan-message">

QR 코드를 스캔해 결제하세요

</p>


<PaymentSecurity />

</main>


<button
class="cancel"

@click="cancelPayment"
>
결제 취소
</button>


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

/* 결제 금액 (Display Typography) */
.amount {

  width: 100%;
  text-align: center;

  margin-top: var(--space-lg);

}

.amount p {

  margin: 0 0 var(--space-xs);

  font-size: var(--font-sm);
  color: var(--color-text-secondary);

}

.amount h1 {

  margin: 0;

  font-size: var(--typo-display-large-size);
  font-weight: var(--typo-display-large-weight);
  line-height: var(--typo-display-large-line-height);
  letter-spacing: var(--typo-display-large-letter-spacing);

  color: var(--color-text-primary);

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

.scan-message {

  margin: var(--space-md) 0 0;

  font-size: var(--font-sm);
  color: var(--color-text-secondary);

}

/* 결제 취소 */
.cancel {

  width: 100%;
  height: 52px;

  margin-top: var(--space-2xl);

  border-radius: var(--radius-md);

  background: none;
  border: 1px solid var(--color-border);

  color: var(--color-text-secondary);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.cancel:hover {

  background: var(--color-surface);
  color: var(--color-text-primary);

}

/* 부채꼴 액션 버튼 */
.fan-actions {
  position: relative;
  width: 100%;
  height: 280px;
  margin: var(--space-2xl) 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.fan-btn {
  position: absolute;
  bottom: 0;
  width: 80px;
  height: 80px;
  border: none;
  border-radius: 50%;
  font-size: var(--font-xs);
  font-weight: var(--font-bold);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

/* 중앙: 결제하기 (주요 버튼) */
.btn-primary {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #FFD60A, #FFC300);
  color: #000;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  box-shadow: 0 12px 32px rgba(255, 195, 0, 0.35);
}

.btn-primary:hover {
  transform: translateX(-50%) scale(1.1);
  box-shadow: 0 16px 40px rgba(255, 195, 0, 0.45);
}

.btn-primary:active {
  transform: translateX(-50%) scale(0.95);
}

/* 왼쪽: 새로고침 */
.btn-refresh {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.9) 0%, rgba(var(--color-primary-dark-rgb), 0.7) 100%);
  color: white;
  left: 10%;
  bottom: 40px;
  z-index: 90;
  transform: translateX(-50%);
}

.btn-refresh:hover {
  transform: translateX(-50%) scale(1.05);
}

/* 오른쪽: 취소 */
.btn-cancel {
  background: linear-gradient(135deg, rgba(var(--color-text-secondary-rgb), 0.8) 0%, rgba(var(--color-text-secondary-rgb), 0.6) 100%);
  color: white;
  right: 10%;
  bottom: 40px;
  z-index: 90;
  transform: translateX(50%);
  left: auto;
}

.btn-cancel:hover {
  transform: translateX(50%) scale(1.05);
}

</style>