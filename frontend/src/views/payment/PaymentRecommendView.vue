<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

import { usePaymentStore } from '@/stores/payment';
import { useAuthStore } from '@/stores/authStore';

import { storeToRefs } from 'pinia';
import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';

import PaymentPasswordModal from '@/components/payment/PaymentPasswordModal.vue';
import PaymentRecommendationDetailSheet from '@/components/payment/PaymentRecommendationDetailSheet.vue';
import PaymentResultSheet from '@/components/payment/PaymentResultSheet.vue';
import PaymentAmountInput from '@/components/payment/PaymentAmountInput.vue';
import MerchantSelector from '@/components/payment/MerchantSelector.vue';
import PaymentLoading from '@/components/payment/PaymentLoading.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';

import EmptyStateCard from '@/components/common/EmptyStateCard.vue';


// 라우터
const router = useRouter();

const cardStore = useCardStore();

const { cards } = storeToRefs(cardStore);


// Store
const paymentStore = usePaymentStore();
const authStore = useAuthStore();


// 로그인 상태
// TODO : 실제 로그인 상태는 Pinia 기준 사용
const isLogin = ref(false);


// 로그인 안내 모달
const showLoginModal = ref(false);


// 카드 등록 안내 모달
const showCardRegisterModal = ref(false);



// 결제 금액
const paymentAmount = ref(30000);


// 선택 데이터
const selectedCard = ref(null);

const selectedCategory = ref(null);

const selectedMerchant = ref(null);



// 추천 상태
const isLoading = ref(false);


// 추천받기 전에는 결과 숨김
const isRecommended = ref(false);



// 비밀번호 모달
const showPasswordModal = ref(false);


// 추천 상세 바텀시트
const showDetailSheet = ref(false);

const detailCard = ref(null);



// 추천 결과 멤버십 정보
// TODO : 추천 API 응답 데이터로 교체
const membershipBenefit = ref({

  name: 'CJ ONE',

  route: '/point/cj-one',

});



// 추천 카드 데이터
// TODO : POST /api/payment/recommend 연결 후 교체
const recommendedCards = ref([

  {
    id: 1,

    name: 'KB My WE:SH 카드',

    image: '/images/cards/kb-wesh.png',

    benefit: 7000,


    reasons: [
      '카페 할인 혜택 적용',
      '올리브영 결제 혜택',
      '생활 영역 할인 가능',
    ],


    membershipBenefit: {

      matched: true,

      name: 'CJ ONE',

      message:
        'CJ ONE 멤버십이 등록되어 있어 올리브영 이용 시 포인트 적립이 가능합니다.',

    },

  },


  {
    id: 2,

    name: '신한 SOL Pay 카드',

    image: '/images/cards/shinhan.png',

    benefit: 5000,


    reasons: [

      '온라인 결제 할인',

      '편의점 할인',

    ],


    membershipBenefit: null,

  },


  {
    id: 3,

    name: '삼성 iD 카드',

    image: '/images/cards/samsung.png',

    benefit: 3000,


    reasons: [

      '간편결제 할인',

      '커피 할인',

    ],


    membershipBenefit: null,

  },

]);



// 로그인 상태 확인
onMounted(() => {

  isLogin.value = authStore.isLogin();


  // 로그인하지 않은 경우
  if (!isLogin.value) {

    showLoginModal.value = true;

  }

});




// 카드 등록 화면 이동
const goCardRegister = () => {

  router.push('/cards/register');

};



// 로그인 화면 이동
const goLogin = () => {

  router.push('/auth/login');

};



// 홈 이동
const goHome = () => {

  showLoginModal.value = false;

  router.push('/home');

};



// 추천 실행
const recommendCard = () => {


  // 비로그인
  if (!isLogin.value) {

    showLoginModal.value = true;

    return;

  }



  // 카드 없음
  if (cards.value.length === 0) {

  showCardRegisterModal.value = true;

  return;

}



  selectedCard.value = null;


  isLoading.value = true;



  // TODO
  // POST /api/payment/recommend


  setTimeout(() => {


    isLoading.value = false;


    isRecommended.value = true;


  }, 500);


};




// 카드 선택
const selectCard = (card) => {

  selectedCard.value = card;

};




// 카드 상세 보기
const openDetail = (card) => {

  detailCard.value = {

    ...card,

    merchant: selectedMerchant.value,

    category: selectedCategory.value,

  };

  showDetailSheet.value = true;

};




// 결제하기
const clickPayment = () => {


  if (!isLogin.value) {

    showLoginModal.value = true;

    return;

  }



  if (cards.value.length === 0) {

  showCardRegisterModal.value = true;

  return;

}



  showPasswordModal.value = true;


};





// 비밀번호 인증 성공
const onPasswordSuccess = () => {


  showPasswordModal.value = false;



  paymentStore.setPaymentInfo(

    selectedCard.value,

    paymentAmount.value,

    membershipBenefit.value

  );



  router.push({

    name: 'Payment',

  });


};




// 새로고침
const refreshPayment = async () => {


  console.log('결제 추천 데이터 갱신');


  await new Promise((resolve)=>{

    setTimeout(resolve,500);

  });


};

</script>  

<template>

<div class="payment-page">


  <PageHeader title="결제 추천" />



  <!-- 비로그인 상태 -->
  <main v-if="!isLogin">

    <EmptyStateCard

      title="로그인 후 이용할 수 있어요"

      description="로그인하면 결제 혜택 추천 서비스를 이용할 수 있습니다."

      buttonText="로그인"

      @click="goLogin"

    />

  </main>




  <!-- 로그인 상태 -->
  <main v-else>


    <!-- 카드 미등록 상태 -->

    <EmptyStateCard

      v-if="cards.length === 0"

      title="등록된 카드가 없어요"

      description="카드를 등록하면 결제 상황에 맞는 최적의 카드를 추천해 드립니다."

      buttonText="카드 등록"

      @click="goCardRegister"

    />




    <!-- 카드 등록 완료 -->

    <template v-else>



      <!-- 금액 입력 -->

      <section class="payment-section">


        <h2>
          결제 금액
        </h2>


        <PaymentAmountInput

          v-model="paymentAmount"

        />


      </section>



      <!-- 장소 선택 -->

      <section class="payment-section">


        <h2>
          이용 장소
        </h2>


        <MerchantSelector

          v-model:category="selectedCategory"

          v-model:merchant="selectedMerchant"

        />


      </section>




      <!-- 추천 버튼 -->

      <button

        class="recommend-button"

        @click="recommendCard"

      >

        추천받기

      </button>




      <!-- 추천 로딩 -->

      <PaymentLoading

        v-if="isLoading"

      />




      <!-- 추천 결과 -->

      <PaymentResultSheet

        v-if="isRecommended"

        :cards="recommendedCards"

        :selectedCard="selectedCard"

        @select="selectCard"

        @detail="openDetail"

      />



    </template>



  </main>




  <!-- 하단 결제 버튼 -->

  <button

    v-if="isLogin && cards.length > 0"

    class="payment-button"

    :disabled="!selectedCard"

    @click="clickPayment"

  >

    결제하기

  </button>





  <!-- 비밀번호 인증 -->

  <PaymentPasswordModal


    :visible="showPasswordModal"


    @success="onPasswordSuccess"


    @close="showPasswordModal = false"


  />




  <!-- 로그인 안내 모달 -->


  <div

    v-if="showLoginModal"

    class="login-modal-overlay"

    @click.self="showLoginModal = false"

  >


    <div class="login-modal">


      <h3>
        로그인이 필요합니다
      </h3>



      <p

        class="login-link"

        @click="goLogin"

      >

        로그인 또는 회원가입 후 이용해주세요.

      </p>



      <button

        @click="goHome"

      >

        닫기

      </button>


    </div>


  </div>




  <!-- 카드 등록 안내 모달 -->

  <div

    v-if="showCardRegisterModal"

    class="login-modal-overlay"

    @click.self="showCardRegisterModal = false"

  >


    <div class="login-modal">


      <h3>
        카드 등록이 필요합니다
      </h3>



      <p>

        카드를 등록하면 결제 혜택을 추천받을 수 있어요.

      </p>



      <button

        @click="goCardRegister"

      >

        카드 등록

      </button>


    </div>


  </div>





  <!-- 추천 카드 상세 -->

  <PaymentRecommendationDetailSheet


    :visible="showDetailSheet"


    :card="detailCard"


    @close="showDetailSheet = false"


    @select="selectCard"


  />

<BottomNavigation/>


</div>


</template>



<style scoped>

/* 전체 화면 */
.payment-page {

  min-height: 100vh;

  background: #f8f8fb;

  padding-bottom: 120px;

}



/* 본문 */
main {

  padding: 20px;

}



/* 섹션 카드 느낌 */
.payment-section {

  background: white;

  border-radius: 20px;

  padding: 20px;

  margin-bottom: 18px;

  box-shadow:

    0 3px 12px rgba(0,0,0,0.05);

}



.payment-section h2 {

  margin: 0 0 16px;

  font-size: 18px;

  font-weight: 700;

}



/* 추천 버튼 */

.recommend-button {

  width: 100%;

  height: 54px;


  margin-top: 8px;


  border: none;

  border-radius: 16px;


  background: #4F46E5;


  color: white;


  font-size: 16px;

  font-weight: 700;


  cursor: pointer;


  transition: .2s;

}



.recommend-button:active {

  transform: scale(0.98);

}




/* 하단 결제 버튼 */

.payment-button {

  position: fixed;


  left: 20px;

  right: 20px;


  bottom: 80px;


  height: 56px;


  border: none;


  border-radius: 18px;


  background: #4F46E5;


  color: white;


  font-size: 16px;


  font-weight: 700;


  box-shadow:

    0 8px 20px rgba(79,70,229,0.25);



  z-index: 1000;


}


.payment-button:disabled {


  background: #d1d5db;


  color: #888;


  box-shadow: none;


  cursor: not-allowed;


}



/* 로그인 / 카드 등록 모달 */

.login-modal-overlay {


  position: fixed;


  inset: 0;


  background: rgba(0,0,0,0.45);


  display:flex;


  align-items:center;


  justify-content:center;


  z-index:5000;


}





.login-modal {


  width: calc(100% - 48px);


  max-width:360px;


  background:white;


  border-radius:24px;


  padding:28px 24px;


  text-align:center;


  box-shadow:


    0 10px 30px rgba(0,0,0,0.15);


}




.login-modal h3 {


  margin:0 0 14px;


  font-size:20px;


  font-weight:700;


}




.login-modal p {


  margin-bottom:24px;


  color:#666;


  font-size:14px;


  line-height:1.5;


}




/* 로그인 이동 */

.login-link {


  color:#4F46E5;


  text-decoration:underline;


  cursor:pointer;


}



/* 모달 버튼 */


.login-modal button {


  width:100%;


  height:46px;


  border:none;


  border-radius:14px;


  background:#f1f2f7;


  font-size:15px;


  font-weight:600;


  cursor:pointer;


}


.login-modal button:active {


  transform:scale(.98);


}




/* 추천 결과 카드 */

:deep(.payment-result-sheet) {


  margin-top:24px;


}



/* 공통 카드 스타일 */

:deep(.card) {


  background:white;


  border-radius:20px;


  box-shadow:


    0 3px 12px rgba(0,0,0,0.06);


}





/* 바텀시트 */

:deep(.bottom-sheet) {


  border-radius:24px 24px 0 0;


}



/* EmptyStateCard 보정 */

:deep(.empty-state-card) {


  background:white;


  border-radius:20px;


  padding:24px;


  box-shadow:


    0 3px 12px rgba(0,0,0,0.05);


}


</style>