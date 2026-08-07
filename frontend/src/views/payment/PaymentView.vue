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
import QRCode from 'qrcode';
import { createPaymentQr, getPaymentQr } from '@/api/paymentQrApi';
import { getPayment } from '@/api/walletApi';


const paymentStore = usePaymentStore();

const router = useRouter();

const amount = computed(() => paymentStore.paymentAmount);

const card = computed(() => paymentStore.selectedCard);

const membershipBenefit = computed(() => paymentStore.membershipBenefit);

// 결제 결과 모달 상태
const showResult = ref(false);

const qrSeconds = ref(300);
let qrTimer = null;
let statusTimer = null;
const qrImage = ref('');
const qrToken = ref('');
const qrLoading = ref(false);

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
      clearInterval(statusTimer);


      showExpireModal.value = true;

    }

  },1000);


};


const checkQrStatus = async () => {
  if (!qrToken.value) return;
  try {
    const status = await getPaymentQr(qrToken.value);
    if (status.status === 'USED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      if (status.paymentId) await getPayment(status.paymentId);
      success.value = true;
      showResult.value = true;
    } else if (status.status === 'EXPIRED' || status.status === 'FAILED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      showExpireModal.value = true;
    }
  } catch (error) {
    console.error('QR 상태 조회 실패', error);
  }
};

const issueQr = async () => {
  if (!card.value?.id) {
    router.replace('/payment/recommend');
    return;
  }

  qrLoading.value = true;
  qrImage.value = '';
  clearInterval(statusTimer);
  try {
    const data = await createPaymentQr(Number(card.value.id), Number(amount.value)); // 07_25 연동 수정: QR 발급 시 결제금액을 확정한다.
    if (!data?.qrToken) throw new Error('QR 토큰을 발급받지 못했습니다.');
    qrToken.value = data.qrToken;
    qrImage.value = await QRCode.toDataURL(data.qrToken, { width: 220, margin: 2 }); // 07_25 연동 수정: QR 이미지는 프론트에서 직접 생성한다.
    qrSeconds.value = data.expiresAt
      ? Math.max(1, Math.floor((new Date(data.expiresAt).getTime() - Date.now()) / 1000))
      : 300;
    startQRTimer();
    statusTimer = setInterval(checkQrStatus, 2000);
  } catch (error) {
    alert(error.response?.data?.message || error.message || 'QR 생성에 실패했습니다.');
    router.replace('/payment/recommend');
  } finally {
    qrLoading.value = false;
  }
};

/*
| 결제 취소
*/
const cancelPayment = () => {

  showExpireModal.value = false;

  router.back();

};

// QR 재발급
const refreshQR = async () => {

  showExpireModal.value = false;

  await issueQr();

};

/*
| 페이지 진입 시 QR 타이머 시작
*/
onMounted(issueQr);

/*
| 페이지 종료 시 타이머 제거
*/
onUnmounted(()=>{

  clearInterval(qrTimer);
  clearInterval(statusTimer);

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
    v-if="card.image"
    :src="card.image"
    :alt="card.name"
  />

  <p>
    {{ card.name }}
  </p>

</section>


<PaymentQR :image="qrImage" :loading="qrLoading" />


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
<!-- 07_25 연동 변경: 결제 요청·추천·결과 정산 API를 기존 결제 UI에 연결한다. -->
