<script setup>


import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';


// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue';
import BottomNav from '@/components/layout/BottomNavigation.vue';
import PullToRefresh from '@/components/common/PullToRefresh.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';


// 혜택 컴포넌트
import FinancialPointCard from '@/components/point/FinancialPointCard.vue';
import FinancialPointBottomSheet from '@/components/point/FinancialPointBottomSheet.vue';

import MembershipCard from '@/components/point/MembershipCard.vue';

import BenefitReportCard from '@/components/point/BenefitReportCard.vue';
import BenefitReportBottomSheet from '@/components/point/BenefitReportBottomSheet.vue';


// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNavigation from '@/components/layout/BottomNavigation.vue'
import PullToRefresh from '@/components/common/PullToRefresh.vue'


// 포인트 컴포넌트
import FinancialPointCard from '@/components/point/FinancialPointCard.vue'
import MembershipCard from '@/components/point/MembershipCard.vue'


import { useRouter } from 'vue-router'



const router = useRouter()


// =========================
// 로그인 / 카드 상태
// =========================


// TODO: Pinia 또는 로그인 API 연결
const isLogin = ref(false)


// TODO: 카드 조회 API 연결
// GET /api/cards
//
// 로그인 + 카드 없음 -> []
// 카드 있음 -> 카드 데이터 배열

const cardList = ref([]);



// 카드 등록 이동
const goCardRegister = () => {

  router.push('/cards/register');

};


// 로그인 이동
const goLogin = () => {

  router.push('/auth/login');

};





// =========================
// 혜택 리포트
// =========================


// TODO: API 연결
// GET /api/benefits/report

const benefitReport = {

  totalBenefit: 12500,

  maxCategory: '구독/콘텐츠',

  categories: [

    {
      categoryName: '구독/콘텐츠',

      benefitAmount: 5000,

      details: [

        {
          merchantName: '넷플릭스',
          cardName: 'KB 카드',
          benefitType: '할인',
          amount: 2000,
        },

        {
          merchantName: '디즈니+',
          cardName: 'KB 카드',
          benefitType: '할인',
          amount: 1500,
        },

      ],

    },


    {
      categoryName: '외식',

      benefitAmount: 3000,

      details: [

        {
          merchantName: '스타벅스',
          cardName: 'KB 카드',
          benefitType: '할인',
          amount: 1000,
        },

      ],

    },

  ],

};



// 혜택 리포트 상세 바텀시트

const showBenefitReport = ref(false);



const openBenefitReport = () => {

  showBenefitReport.value = true;

};



const closeBenefitReport = () => {

  showBenefitReport.value = false;

};







// =========================
// 금융 포인트
// =========================


const showPointSheet = ref(false);


const selectedPoint = ref(null);



// 금융 포인트 상세 열기

const openPointSheet = (point) => {

  selectedPoint.value = point;

  showPointSheet.value = true;

};



// 닫기

const closePointSheet = () => {

  showPointSheet.value = false;

};



// 새로고침
// TODO: API 연결 시 실제 데이터 재조회

const refreshPoint = async () => {

  console.log('포인트 데이터 갱신 시작')


  console.log('혜택 데이터 갱신');

};





// =========================
// 멤버십
// =========================


// TODO: API 연결
// GET /api/user-memberships
//
// 로그인 후 등록한 멤버십만 내려옴
//
// 없음 -> []
// 있음 -> 멤버십 배열


const membershipList = ref([]);



const goMembershipRegister = () => {


  router.push('/memberships/register');

};




// 멤버십 더보기

const SHOW_COUNT = 3;


const visibleCount = ref(SHOW_COUNT);



const visibleMemberships = computed(() => {

  return membershipList.value.slice(
    0,
    visibleCount.value
  );

});



const showMoreMembership = () => {

  visibleCount.value += SHOW_COUNT;

};


  router.push('/memberships/register')

}

const membershipList = [
  {
    id: 1,
    name: 'CJ ONE',
    link: 'https://www.cjone.com'
  },
  {
    id: 2,
    name: '해피포인트',
    link: 'https://www.happypointcard.com'
  }
]


</script>




<template>

  <PullToRefresh @refresh="refreshPoint">

    <div class="point-page">

      <PageHeader title="혜택" />

      <main class="content">

        <!-- 혜택 리포트 -->
        <section class="benefit-report-section">

          <h2>
            혜택 리포트
          </h2>


          <!-- 비로그인 -->
          <EmptyStateCard
            v-if="!isLogin"
            title="로그인 후 이용할 수 있어요"
            description="로그인하면 카드 혜택 분석과 리포트를 확인할 수 있습니다."
            buttonText="로그인"
            @click="goLogin"
          />


          <!-- 로그인 + 카드 없음 -->
          <EmptyStateCard
            v-else-if="cardList.length === 0"
            title="등록된 카드가 없어요"
            description="카드를 등록하면 혜택 리포트를 확인할 수 있습니다."
            buttonText="카드 등록"
            @click="goCardRegister"
          />


          <!-- 카드 있음 -->
          <BenefitReportCard
            v-else
            :report="benefitReport"
            @open="openBenefitReport"
          />

        </section>



        <!-- 금융 포인트 -->
        <section class="point-section">

          <h2>
            금융 포인트
          </h2>


          <!-- 비로그인 -->
          <EmptyStateCard
            v-if="!isLogin"
            title="로그인 후 이용할 수 있어요"
            description="로그인하면 금융 포인트를 확인할 수 있습니다."
            buttonText="로그인"
            @click="goLogin"
          />


          <!-- 로그인 + 카드 없음 -->
          <EmptyStateCard
            v-else-if="cardList.length === 0"
            title="카드를 등록해 주세요"
            description="카드 등록 후 금융 포인트를 확인할 수 있습니다."
            buttonText="카드 등록"
            @click="goCardRegister"
          />


          <!-- 카드 있음 -->
          <FinancialPointCard
            v-else
            @click="openPointSheet"
          />


        </section>





        <!-- 멤버십 -->
        <section class="membership-section">


          <div class="section-header">

            <h2>
              멤버십
            </h2>


            <button
              v-if="isLogin"
              class="add-button"
              @click="goMembershipRegister"
            >
              + 추가
            </button>

          </div>



          <!-- 비로그인 -->
          <EmptyStateCard
            v-if="!isLogin"
            title="로그인 후 이용할 수 있어요"
            description="로그인하면 멤버십을 등록하고 관리할 수 있습니다."
            buttonText="로그인"
            @click="goLogin"
          />



          <!-- 로그인 + 멤버십 없음 -->
          <EmptyStateCard
            v-else-if="membershipList.length === 0"
            title="등록된 멤버십이 없어요"
            description="멤버십을 등록하면 포인트와 혜택을 관리할 수 있습니다."
            buttonText="멤버십 등록"
            @click="goMembershipRegister"
          />



          <!-- 멤버십 있음 -->
          <template v-else>


            <MembershipCard
              v-for="membership in visibleMemberships"
              :key="membership.id"
              :membership="membership"
            />



            <button
              v-if="visibleCount < membershipList.length"
              class="more-button"
              @click="showMoreMembership"
            >
              더보기
            </button>


          </template>



          <p class="notice">
            ※ 멤버십 상세 페이지에서 사용처 및 이용 정보를 확인할 수 있습니다.
          </p>


        </section>


      </main>



      <BottomNav />



      <!-- 금융 포인트 상세 -->
      <FinancialPointBottomSheet
        v-if="showPointSheet"
        :point="selectedPoint"
        @close="closePointSheet"
      />


    </div>


  <div class="point-page">


    <!-- 페이지 제목 -->
    <PageHeader title="포인트" />



    <main class="content">


      <!-- 금융 포인트 영역 -->
      <section class="point-section">

        <h2>
          금융 포인트
        </h2>


        <FinancialPointCard />

      </section>




      <!-- 멤버십 영역 -->
<section class="membership-section">

  <div class="section-header">

    <h2>
      멤버십
    </h2>


    <button
      class="add-button"
      @click="goMembershipRegister"
    >
      + 추가
    </button>

  </div>


  <MembershipCard
  v-for="membership in membershipList"
  :key="membership.id"
  :membership="membership"
/>

<p class="notice">
  ※ 멤버십 상세 페이지에서 사용처 및 이용 정보를 확인할 수 있습니다.
</p>


</section>
    


    </main>



    <BottomNavigation />


  </div>


  </PullToRefresh>




  <!-- 혜택 리포트 상세 -->
  <BenefitReportBottomSheet
    v-if="showBenefitReport"
    @close="closeBenefitReport"
  />


</template>




<style scoped>


   /* 전체 페이지 */

.point-page {
  padding: 20px;
  padding-bottom: 90px;
}


.content {
  display: flex;
  flex-direction: column;
  gap: 28px;
}


   /* 섹션 공통 */

section {
  width: 100%;
}


h2 {
  margin: 0 0 14px;



.point-page {

  min-height: 100vh;

  padding-bottom: 80px;

}



.content {

  padding: 20px;

}



.point-section {

  margin-bottom: 32px;

}



.membership-section {

  margin-bottom: 32px;

}



h2 {


  font-size: 18px;
  font-weight: 700;

  color: #222;
}


   /* 혜택 리포트 */

.benefit-report-section {
  margin-bottom: 0;
}


/* BenefitReportCard 내부 카드 느낌 */

.benefit-report-card {

  width: 100%;

  background: white;

  border-radius: 18px;

  padding: 20px;

  box-sizing: border-box;


  box-shadow:
    0 4px 12px rgba(0,0,0,0.06);

}




   /* 금융 포인트 */

.point-section {

  margin-top: 0;


}

  margin-bottom: 14px;

}



button {

  border: none;



.point-section :deep(.financial-point-card) {

  border-radius: 18px;



  font-size: 14px;


}



   /* 멤버십 */
.membership-section {

  margin-top: 0;

}



.section-header {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:14px;

}



.section-header h2 {

  margin-bottom:0;

}



.add-button {

  border:none;

  background:#4F46E5;

  color:white;


  padding:9px 14px;

  border-radius:20px;


  font-size:13px;

  font-weight:600;


  cursor:pointer;

}



/* 멤버십 카드 사이 간격 */

.membership-section :deep(.membership-card) {

  margin-bottom:12px;

}



/* 더보기 버튼 */

.more-button {

  width:100%;


  margin-top:12px;


  height:44px;


  border:none;

  border-radius:12px;


  background:#f3f4f6;


  color:#555;


  font-size:14px;


  cursor:pointer;

}


/* 안내 문구 */

.notice {

  margin-top: 14px;


  margin:16px 0 0;


  font-size:12px;

  color:#888;

  line-height:1.5;

}




   /* EmptyStateCard */

:deep(.empty-card) {

  width:100%;

  min-height:150px;


  background:#f8f8f8;


  border-radius:18px;


  padding:32px 20px;


  box-sizing:border-box;


  display:flex;

  flex-direction:column;

  justify-content:center;

  align-items:center;


  text-align:center;

}



:deep(.empty-title) {

  font-size:15px;

  font-weight:600;

  color:#222;

}



:deep(.empty-description) {

  margin-top:8px;

  font-size:13px;

  color:#777;

  line-height:1.5;

}



:deep(.button-group) {

  margin-top:18px;

}



:deep(.empty-card button) {

  min-width:110px;

  height:42px;


  border:none;

  border-radius:21px;


  background:#4F46E5;


  color:white;


  font-size:14px;

  font-weight:600;


  cursor:pointer;

}



   /* Bottom Sheet 대응 */
:deep(.bottom-sheet) {

  border-radius:24px 24px 0 0;

}




   /* 모바일 화면 보정 */
@media (max-width:480px){

  .point-page {

    padding:16px;

  }


  h2 {

    font-size:17px;

  }


  .benefit-report-card {

    padding:18px;

  }

}


</style>


  line-height: 1.5;

}


</style>
