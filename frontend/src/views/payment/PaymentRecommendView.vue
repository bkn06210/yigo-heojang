<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

import { usePaymentStore } from '@/stores/payment';
import { useAuthStore } from '@/stores/authStore';

import { getCardRecommendations } from '@/api/walletApi';

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
const hasRegisteredCards = ref(true);



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



const membershipBenefit = ref(null);
const recommendedCards = ref([]);

const categoryIds = {
  카페: 102,
  음식점: 101,
  쇼핑: 2,
  교통: 3,
  문화: 5,
};



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
const recommendCard = async () => {


  // 비로그인
  if (!isLogin.value) {

    showLoginModal.value = true;

    return;

  }



  selectedCard.value = null;


  isLoading.value = true;



  try {
    const response = await getCardRecommendations({
      expectedAmount: Number(paymentAmount.value),
      categoryId: categoryIds[selectedCategory.value] || undefined,
      paymentType: 'CARD',
    });

    recommendedCards.value = (response.recommendations || []).map((item) => ({
      id: Number(item.userCardId),
      name: item.cardName,
      image: '',
      benefit: Number(item.expectedBenefit || 0),
      benefitAmount: Number(item.expectedBenefit || 0),
      reasons: item.reason ? [item.reason] : [],
      membershipBenefit: null,
    }));
    membershipBenefit.value = response.membershipEarn?.[0] || null;
    isRecommended.value = true;
    if (recommendedCards.value.length === 0) {
      hasRegisteredCards.value = false;
      showCardRegisterModal.value = true;
    }
  } catch (error) {
    alert(error.response?.data?.message || error.message || '카드 추천에 실패했습니다.');
  } finally {
    isLoading.value = false;
  }
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



  showPasswordModal.value = true;


};





// 비밀번호 인증 성공
const onPasswordSuccess = () => {


  showPasswordModal.value = false;



  paymentStore.setPaymentInfo(

    selectedCard.value,

    paymentAmount.value,

    membershipBenefit.value,
    {
      categoryId: categoryIds[selectedCategory.value] || 1,
      merchantName: selectedMerchant.value || selectedCategory.value || '일반 가맹점',
    }

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

      v-if="!hasRegisteredCards"

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

    v-if="isLogin && hasRegisteredCards"

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
<!-- 07_25 연동 변경: 기존 추천 UI를 유지하면서 실제 카드 추천 API를 호출한다. -->
