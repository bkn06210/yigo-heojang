<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';

import { usePaymentStore } from '@/stores/payment';
import { useAuthStore } from '@/stores/authStore';
import { usePersonalizationStore } from '@/stores/personalization';

import { storeToRefs } from 'pinia';
import { useCardStore } from '@/stores/cardStore';

import { Swiper, SwiperSlide } from 'swiper/vue';
import { Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/pagination';

// 테스트용 mock 카드
const addMockCards = () => {
  if (cards.value.length === 0) {
    cardStore.addCard({
      id: 1,
      name: 'KB My WE:SH 카드',
      cardNumber: '4111111111111111',
      company: 'KB국민카드',
      image: 'https://via.placeholder.com/280x177?text=KB',
      pinned: false
    });
  }
};

import PageHeader from '@/components/common/PageHeader.vue';

import PaymentPasswordModal from '@/components/payment/PaymentPasswordModal.vue';
import PaymentRecommendationDetailSheet from '@/components/payment/PaymentRecommendationDetailSheet.vue';
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
const personalizationStore = usePersonalizationStore();

// 개인화 설정에서 등록한 관심 카테고리가 하나도 없을 때만 안내 문구 노출
const hasNoPersonalizedCategories = computed(
  () => personalizationStore.getActiveCategories().length === 0
);

// 개인화 설정 이동
const goPersonalizationSetting = () => {
  router.push('/settings/personalization');
};


// 로그인 상태 (authStore 기준)
const isLogin = computed(() => !!authStore.user);


// 로그인 안내 모달
const showLoginModal = ref(false);


// 카드 등록 안내 모달
const showCardRegisterModal = ref(false);



// 결제 금액
const paymentAmount = ref(0);


// 바로 결제 모드
// true: 추천 없이 보유 카드 중 바로 선택해서 결제
const isQuickPay = ref(false);

// 바로 결제 모드 내에서 전체 목록으로 볼지 여부
// false: 캐러셀, true: 전체 목록
const showAllCards = ref(false);

const openQuickPay = () => {

  isQuickPay.value = true;

  showAllCards.value = false;

  selectedCard.value = null;

  isRecommended.value = false;

};

const closeQuickPay = () => {

  isQuickPay.value = false;

  showAllCards.value = false;

  selectedCard.value = null;

};


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



// 가맹점 - 멤버십 매핑 (mock)
// TODO : 실제로는 사용자가 등록한 멤버십 목록과 가맹점을 비교해서 매칭해야 함
const merchantMembershipMap = {

  '올리브영': { name: 'CJ ONE', route: '/point/cj-one', message: 'CJ ONE 멤버십이 등록되어 있어 올리브영 이용 시 포인트 적립이 가능합니다.' },
  '뚜레쥬르': { name: 'CJ ONE', route: '/point/cj-one', message: 'CJ ONE 멤버십이 등록되어 있어 뚜레쥬르 이용 시 포인트 적립이 가능합니다.' },
  'CGV': { name: 'CJ ONE', route: '/point/cj-one', message: 'CJ ONE 멤버십이 등록되어 있어 CGV 이용 시 포인트 적립이 가능합니다.' },

  '파리바게뜨': { name: '해피포인트', route: '/point/happy-point', message: '해피포인트 멤버십이 등록되어 있어 파리바게뜨 이용 시 포인트 적립이 가능합니다.' },
  '던킨': { name: '해피포인트', route: '/point/happy-point', message: '해피포인트 멤버십이 등록되어 있어 던킨 이용 시 포인트 적립이 가능합니다.' },
  '배스킨라빈스': { name: '해피포인트', route: '/point/happy-point', message: '해피포인트 멤버십이 등록되어 있어 배스킨라빈스 이용 시 포인트 적립이 가능합니다.' },

};

// 추천 결과 멤버십 정보
// 가맹점을 실제로 선택했고, 매칭되는 멤버십이 있을 때만 값을 가짐
const membershipBenefit = computed(() => {

  if (!selectedMerchant.value) {
    return null;
  }

  return merchantMembershipMap[selectedMerchant.value] ?? null;

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

  },

]);



// 초기화
onMounted(() => {

  // 테스트용 mock 카드 추가
  addMockCards();

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

  // 상세 시트에서 선택한 경우 시트 닫기
  showDetailSheet.value = false;

};




// 카드 상세 보기
const openDetail = (card) => {

  detailCard.value = {

    ...card,

    merchant: selectedMerchant.value,

    category: selectedCategory.value,

    // 가맹점 선택 여부에 따라 매칭되는 멤버십만 표시
    membershipBenefit: membershipBenefit.value,

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

  <PageHeader title="결제 추천" :show-back="false" @back="router.back()" />



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



      <!-- 바로 결제 전환 -->

      <button

        v-if="!isQuickPay"
        type="button"
        class="quick-pay-toggle"

        @click="openQuickPay"

      >

        바로 결제하기

      </button>



      <template v-if="!isQuickPay">


        <!-- 장소 선택 -->

        <section class="payment-section">


          <div class="section-title-row">

            <h2>
              이용 장소
            </h2>

            <!-- 개인화 설정에서 카테고리를 하나도 안 골랐으면 업종 버튼이 텅 비어
                 보이므로, 어디서 채울 수 있는지 짧게 안내 -->
            <button
              v-if="hasNoPersonalizedCategories"
              type="button"
              class="personalize-hint"
              @click="goPersonalizationSetting"
            >
              설정 → 개인화 설정
            </button>

          </div>


          <MerchantSelector

            v-model:category="selectedCategory"

            v-model:merchant="selectedMerchant"

          />

          <!-- 선택 정보 표시 -->
          <div v-if="selectedCategory" class="selection-info">
            <p class="selection-text">
              <template v-if="selectedMerchant">
                <strong>{{ selectedCategory }}</strong>의 <strong>{{ selectedMerchant }}</strong>
              </template>
              <template v-else>
                <strong>{{ selectedCategory }}</strong>
              </template>
            </p>
          </div>

        </section>




        <!-- 추천 버튼 -->

        <button

          class="recommend-button"
          :disabled="!selectedCategory"

          @click="recommendCard"

        >

          추천받기

        </button>




        <!-- 추천 로딩 -->

        <PaymentLoading

          v-if="isLoading"

        />


      </template>



      <!-- 카드 선택 / 추천 결과 — 바로결제든 이용장소 선택 후 추천받기든
           같은 캐러셀+목록 형식으로 통일. 바로결제는 버튼 없이 바로 혜택순으로 표시,
           추천받기는 클릭 후 같은 형식으로 결과 표시 -->
      <section
        v-if="isQuickPay || isRecommended"
        class="payment-section quick-pay-section"
      >

        <div class="quick-pay-header">

          <h2>
            {{ isQuickPay ? '카드 선택' : '추천 결과' }}
          </h2>

          <div class="quick-pay-actions">

            <button
              type="button"
              class="text-toggle"
              @click="showAllCards = !showAllCards"
            >
              {{ showAllCards ? '캐러셀로 보기' : '전체보기' }}
            </button>

            <button
              v-if="isQuickPay"
              type="button"
              class="text-toggle cancel"
              @click="closeQuickPay"
            >
              취소
            </button>

          </div>

        </div>

        <!-- 캐러셀 -->
        <Swiper
          v-if="!showAllCards"
          :modules="[Pagination]"
          :slides-per-view="1.4"
          :centered-slides="true"
          :space-between="12"
          :pagination="{ clickable: true }"
          :grab-cursor="true"
          class="quick-card-carousel"
        >
          <SwiperSlide
            v-for="card in recommendedCards"
            :key="card.id"
            class="quick-card-slide"
          >
            <button
              type="button"
              class="quick-card-slide-button"
              :class="{ selected: selectedCard?.id === card.id }"
              @click="selectCard(card)"
            >

              <img
                :src="card.image"
                :alt="card.name"
              />

              <span>{{ card.name }}</span>

              <span class="quick-card-detail-link" @click.stop="openDetail(card)">
                상세보기
              </span>

            </button>
          </SwiperSlide>
        </Swiper>

        <!-- 전체 목록 -->
        <div v-else class="quick-card-list">

          <button

            v-for="(card, idx) in recommendedCards"
            :key="card.id"

            type="button"
            class="quick-card-item"
            :class="{ selected: selectedCard?.id === card.id }"
            :style="{ animationDelay: (idx * 60) + 'ms' }"

            @click="selectCard(card)"

          >

            <img
              :src="card.image"
              :alt="card.name"
            />

            <span>{{ card.name }}</span>

            <span class="quick-card-detail-link" @click.stop="openDetail(card)">
              상세보기
            </span>

          </button>

        </div>

      </section>



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

  background: var(--color-bg);

  padding: var(--space-md);
  /* 하단 고정 "결제하기" 버튼(bottom:80px, height:56px)에 콘텐츠가 가려지지
     않도록 버튼 전체 높이만큼 여유를 둠 */
  padding-bottom: calc(80px + 56px + var(--space-lg));

  margin: 0 auto;

  max-width: 480px;

  box-sizing: border-box;

  overflow: hidden visible;

}



/* 본문 */
main {

  display: block;

}



/* 섹션 카드 느낌 */
.payment-section {

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: 20px;

  margin-bottom: 18px;

  box-shadow: var(--shadow-card);

}



.payment-section h2 {

  margin: 0 0 16px;

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  color: var(--color-text-primary);

}

.section-title-row {

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: var(--space-sm);

  margin: 0 0 16px;

}

.section-title-row h2 {

  margin: 0;

}

.personalize-hint {

  flex-shrink: 0;

  background: none;

  border: none;

  padding: 0;

  font-size: var(--font-xs);

  color: var(--color-text-secondary);

  cursor: pointer;

  text-decoration: underline;

  text-underline-offset: 2px;

  transition: var(--transition-fast);

}

.personalize-hint:hover {

  color: var(--color-primary-dark);

}



/* 선택 정보 박스 */
.selection-info {
  background: linear-gradient(135deg, rgba(200, 220, 240, 0.4) 0%, rgba(220, 240, 255, 0.2) 100%);
  border: 1px solid rgba(100, 150, 200, 0.15);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin-top: var(--space-md);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.selection-info .selection-text {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  line-height: 1.6;
}

.selection-info strong {
  color: var(--color-primary-dark);
  font-weight: var(--font-bold);
}

[data-theme="dark"] .selection-info {
  background: linear-gradient(135deg, rgba(100, 120, 150, 0.2) 0%, rgba(80, 100, 140, 0.15) 100%);
  border: 1px solid rgba(150, 170, 200, 0.15);
}

/* 바로 결제 전환 버튼 */

.quick-pay-toggle {

  display: block;

  margin: var(--space-sm) 0 0 auto;

  padding: var(--space-xs) 0;

  border: none;

  background: none;

  color: var(--color-primary-dark);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  text-decoration: underline;

  cursor: pointer;

}

.quick-pay-toggle:hover {

  opacity: 0.8;

}



/* 바로 결제 섹션 헤더 */

.quick-pay-header {

  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: var(--space-md);

}

.quick-pay-header h2 {

  margin: 0;

}

.quick-pay-actions {

  display: flex;
  align-items: center;
  gap: var(--space-md);

}

.text-toggle {

  border: none;
  background: none;
  padding: 0;

  color: var(--color-primary-dark);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);

  cursor: pointer;

}

.text-toggle.cancel {

  color: var(--color-text-tertiary);

}

.text-toggle:hover {

  opacity: 0.8;

}



/* 바로 결제: 캐러셀 */

.quick-card-carousel {

  padding-bottom: var(--space-xl);

}

.quick-card-slide {

  height: auto;

}

.quick-card-slide-button {

  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);

  width: 100%;
  padding: var(--space-lg) var(--space-md);

  border-radius: var(--radius-lg);
  border: 2px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);

  cursor: pointer;
  transition: var(--transition-fast);

}

.quick-card-slide-button.selected {

  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.08) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);

}

[data-theme="dark"] .quick-card-slide-button.selected {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);

}

.quick-card-slide-button img {

  width: 100%;
  aspect-ratio: 1.586 / 1;

  border-radius: var(--radius-md);
  object-fit: contain;

}

.quick-card-slide-button span {

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

/* .quick-card-item span / .quick-card-slide-button span 보다 명시도가 높아야
   상세보기 링크가 카드명 스타일에 덮어써지지 않음 */
.quick-card-item .quick-card-detail-link,
.quick-card-slide-button .quick-card-detail-link {

  font-size: var(--font-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;
  transition: var(--transition-fast);

}

.quick-card-item .quick-card-detail-link:hover,
.quick-card-slide-button .quick-card-detail-link:hover {

  color: var(--color-primary-dark);

}



/* 추천 버튼 */

.recommend-button {

  width: 100%;

  height: 54px;


  margin-top: 8px;


  border: none;

  border-radius: 16px;


  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );


  color: var(--color-btn-primary-text);


  font-size: 16px;

  font-weight: 700;


  cursor: pointer;


  transition: .2s;

}

.recommend-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}



.recommend-button:active {

  transform: scale(0.98);

}



/* 바로 결제: 보유 카드 목록 */

.quick-card-list {

  display: flex;
  flex-direction: column;
  gap: var(--space-sm);

}

.quick-card-item {

  display: flex;
  align-items: center;
  gap: var(--space-md);

  width: 100%;
  padding: var(--space-md);

  border-radius: var(--radius-lg);
  border: 2px solid var(--color-border);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);

  cursor: pointer;
  transition: var(--transition-fast);

  opacity: 0;
  animation: quickCardPop 0.35s ease both;

}

.quick-card-item.selected {

  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.08) 0%, rgba(var(--color-primary-dark-rgb), 0.02) 100%);

}

[data-theme="dark"] .quick-card-item.selected {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);

}

.quick-card-item img {

  width: 56px;
  height: 36px;

  border-radius: var(--radius-xs);
  object-fit: cover;

}

.quick-card-item span {

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

.quick-card-item .quick-card-detail-link {

  margin-left: auto;
  flex-shrink: 0;

}

@keyframes quickCardPop {

  from {
    opacity: 0;
    transform: translateY(14px) scale(0.96);
  }

  to {
    opacity: 1;
    transform: none;
  }

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


  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );


  color: var(--color-btn-primary-text);


  font-size: 16px;


  font-weight: 700;


  box-shadow:

    0 8px 20px rgba(var(--color-primary-dark-rgb),0.25);



  z-index: 1000;


}


.payment-button:disabled {


  background: var(--color-btn-disabled-bg);


  color: var(--color-text-tertiary);


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


  background: var(--color-surface);


  border-radius:24px;


  padding:28px 24px;


  text-align:center;


  box-shadow: var(--shadow-card);


}




.login-modal h3 {


  margin:0 0 14px;


  font-size: var(--font-lg);


  font-weight: var(--font-bold);


}




.login-modal p {


  margin-bottom:24px;


  color: var(--color-text-secondary);


  font-size:14px;


  line-height:1.5;


}




/* 로그인 이동 */

.login-link {


  color:var(--color-surface);


  text-decoration:underline;


  cursor:pointer;


}



/* 모달 버튼 */


.login-modal button {


  width:100%;


  height:46px;


  border:none;


  border-radius:14px;


  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );


  color:var(--color-btn-primary-text);


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


  background: var(--color-surface);


  border-radius: var(--radius-lg);


  box-shadow: var(--shadow-card);


}





/* 바텀시트 */

:deep(.bottom-sheet) {


  border-radius:24px 24px 0 0;


}



/* EmptyStateCard 보정 */

:deep(.empty-card) {


  background: var(--color-surface);


  border-radius: var(--radius-lg);


  padding:24px;


  box-shadow: var(--shadow-card);


}


</style>