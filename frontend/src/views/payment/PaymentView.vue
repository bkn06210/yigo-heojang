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


<PageHeader title="결제"/>


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
