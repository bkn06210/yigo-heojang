<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';

import HomeHeader from '@/components/home/HomeHeader.vue';
import AIBriefingCard from '@/components/common/AIBriefingCard.vue';
import MyCardSummaryCard from '@/components/home/MyCardSummaryCard.vue';
import PointSummaryCard from '@/components/home/PointSummaryCard.vue';
import MembershipSummaryCard from '@/components/home/MembershipSummaryCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';
import BenefitReportCard from '@/components/point/BenefitReportCard.vue';


const router = useRouter();


// 로그인 상태
// TODO: 실제 API / Pinia 연결
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);


// 카드 store 연결
const cardStore = useCardStore();

const { cards } = storeToRefs(cardStore);


const hasMembership = ref(false);


const hasCard = computed(() => cards.value.length > 0);


// 홈 데이터
const homeData = ref({

  hasUnreadNotification: true,


  briefing: {

    content:
      '약 18,000원의 혜택을 받을 수 있습니다.',

  },


  myCard: {

    image: '',

    cardName: '신한 Mr.Life',

    currentAmount: 300000,

    targetAmount: 500000,

    remainAmount: 200000,

    remainBenefit: 13500,

    achievementRate: 60,

  },


  financialPoints: [

    {
      id:1,
      name:'마이신한포인트',
      balance:2234,
    },

    {
      id:2,
      name:'포인트리',
      balance:4456,
    },

  ],


  memberships:[

    {
      id:1,
      name:'CJ ONE',
    },

    {
      id:2,
      name:'해피포인트',
    },

  ],


});



// 혜택 리포트
// TODO: GET /benefits/report 연결

const benefitReport = {

  totalBenefit: 12500,

  maxCategory:'구독/콘텐츠',

};




// 헤더

// 챗봇 이동
const goChatBot = () => {

  router.push('/ai/chat');

};

// 알림 이동
const goNotification = () => {

  router.push('/notifications');

};

//프로필 이동
const goProfile = () => {

  router.push('/settings');

};



// 카드 이동

const goCardDetail = () => {

  router.push('/cards/1');

};


const goCardList = () => {

  router.push('/cards');

};



// 혜택 이동

const goBenefit = () => {

  router.push('/benefits');

};



// 포인트

const goPointList = () => {

  router.push('/benefits');

};


const goPointDetail = (item) => {

  console.log('포인트 상세', item);

};



// 멤버십

const goMembershipDetail = (item) => {

  console.log(item);

};


const goMembershipRegister = () => {

  router.push('/memberships/register');

};



// 데이터 조회

const loadHome = async () => {

  /*
    추후

    GET /home

    응답 예:
    {
      hasCard:true,
      card:{},
      points:[],
      memberships:[]
    }

  */


};



onMounted(async()=>{

  await loadHome();

});

</script>



<template>

<div class="home-view">


<main class="home-content">


<HomeHeader

  :has-unread-notification="homeData.hasUnreadNotification"

  :user="user"

  @chat="goChatBot"

  @click-notification="goNotification"

  @profile="goProfile"

/>



<!-- AI 브리핑 -->

<AIBriefingCard

  v-if="user"

  :is-login="true"

  :message="homeData.briefing.content"

/>


<AIBriefingCard

  v-else

  :is-login="false"

  message="로그인하면 맞춤 금융 혜택을 확인할 수 있어요."

/>





<!-- 카드 -->

<section class="home-section">

<h2>
내 카드
</h2>


<MyCardSummaryCard

  v-if="user && hasCard"

  :is-login="true"

  :card="homeData.myCard"

  @click-card="goCardDetail"

  @click-more="goCardList"

/>



<EmptyStateCard

  v-else-if="user && !hasCard"

  title="등록된 카드가 없어요"

  description="카드를 등록하면 맞춤 혜택을 확인할 수 있습니다."

  buttonText="카드 등록"

  @click="goCardList"

/>



<EmptyStateCard

  v-else

  title="로그인 후 이용할 수 있어요"

  description="로그인하면 내 카드를 관리할 수 있습니다."

/>


</section>





<!-- 혜택 리포트 -->

<section class="home-section">

<h2>
혜택 리포트
</h2>


<BenefitReportCard

  v-if="user && hasCard"

  :report="benefitReport"

  @open="goBenefit"

/>


<EmptyStateCard

  v-else-if="user && !hasCard"

  title="등록된 카드가 없어요"

  description="카드를 등록하면 혜택 리포트를 확인할 수 있습니다."

  buttonText="카드 등록"

  @click="goCardList"

/>


<EmptyStateCard

  v-else

  title="로그인 후 이용할 수 있어요"

  description="로그인하면 맞춤 혜택을 확인할 수 있습니다."

/>


</section>





<!-- 금융 포인트 -->

<section class="home-section">


<h2>
금융 포인트
</h2>


<PointSummaryCard

  v-if="user && hasCard"

  :is-login="true"

  :points="homeData.financialPoints"

  @click-more="goPointList"

  @click-item="goPointDetail"

/>


<EmptyStateCard

  v-else

  title="금융 포인트를 확인할 수 없어요"

  description="카드 등록 후 포인트를 관리할 수 있습니다."

/>


</section>






<!-- 멤버십 -->

<section class="home-section">


<h2>
멤버십
</h2>

<MembershipSummaryCard

  :is-login="!!user"

  :memberships="useMockMembership 
    ? homeData.memberships 
    : []"

  @click-item="goMembershipDetail"

  @click-register="goMembershipRegister"

  @click-more="goPointList"

/>


</section>




</main>


<BottomNavigation />


</div>

</template>



<style scoped>

/* 홈 전체 */
.home-view {

  min-height: 100vh;

  padding: 16px;

  padding-bottom: 90px;

  background: #fafafa;

}



/* 콘텐츠 영역 */
.home-content {

  display: flex;

  flex-direction: column;

  gap: 20px;

}



/* 각 섹션 카드 간격 */
.home-section {

  width: 100%;

}



/* EmptyStateCard가 들어갔을 때 */
.home-section :deep(.empty-card) {

  margin-top: 8px;

}



/* 카드/포인트/멤버십 공통 카드 느낌 */
.home-section :deep(section),
.home-section :deep(.summary-card) {

  border-radius: 16px;

}



/* 제목 영역 */
.home-section h2 {

  margin-bottom: 12px;

  font-size: 18px;

  font-weight: 700;

}



/* 버튼이 있는 Empty 상태 */
.home-section :deep(.button-group) {

  margin-top: 8px;

}



/* 하단 네비 공간 */
.home-view {

  box-sizing: border-box;

}

.home-section {

  margin-top:24px;

}


.home-section h2 {

  font-size:18px;

  margin-bottom:12px;

  font-weight:700;

}


.home-content {

  display:flex;

  flex-direction:column;

  gap:20px;

}


</style>
