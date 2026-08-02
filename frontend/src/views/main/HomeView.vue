<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';

import HomeHeader from '@/components/home/HomeHeader.vue';
import AIBriefingCard from '@/components/common/AIBriefingCard.vue';
import MyCardSummaryCard from '@/components/home/MyCardSummaryCard.vue';
import PointSummaryCard from '@/components/home/PointSummaryCard.vue';
import MembershipSummaryCard from '@/components/home/MembershipSummaryCard.vue';
import AssistantCard from '@/components/home/AssistantCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';


const router = useRouter();

// 로그인 정보
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);



// 헤더 이벤트
const goChatBot = () => {
  router.push('/ai/chat');
};


const goNotification = () => {
  router.push('/notifications');
};


const goProfile = () => {
  router.push('/settings');
};


// 카드 이벤트

const goCardDetail = () => {
  console.log('카드 상세');
};


const goCardList = () => {
  console.log('카드 목록');
};


// 홈 Mock 데이터
const homeData = ref({

  hasUnreadNotification: true,


  briefing: {
    title: '이번 달 가장 큰 혜택은 신한카드입니다.',
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
      name:'해피포인트',
    },

    {
      id:2,
      name:'CJ ONE',
    },
  ],

});


// 이동

const goPointList = () => {
  console.log('포인트 목록');
};


const goPointDetail = (item)=>{
  console.log('포인트 상세', item);
};


const goMembershipDetail = (item)=>{
  console.log('멤버십 상세', item);
};

</script>


<template>

<div class="home-view">


  <main class="home-content">


    <HomeHeader

      :has-unread-notification="homeData.hasUnreadNotification"

      :user="user"

      @chat="goChatBot"

      @notification="goNotification"

      @profile="goProfile"

    />



  <!-- 로그인 여부에 따른 브리핑 -->

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

  
    <MyCardSummaryCard

      v-if="user"

      :is-login="true"

      :card="homeData.myCard"

      @click-card="goCardDetail"

      @click-more="goCardList"

    />

    <EmptyStateCard

      v-else

      title="등록된 카드가 없습니다."

    />


  <!-- 금융 포인트 -->

   <PointSummaryCard

      v-if="
        user &&
        homeData.financialPoints.length
      "

      :is-login="true"

      :points="homeData.financialPoints"

      @click-more="goPointList"

      @click-item="goPointDetail"

    />


    <EmptyStateCard

      v-else

      title="등록된 금융 포인트가 없습니다."

    />


  <!-- 멤버십 -->

    <MembershipSummaryCard

      v-if="
        user &&
        homeData.memberships.length
      "

      :is-login="true"

      :memberships="homeData.memberships"

      @click-more="goPointList"

      @click-item="goMembershipDetail"

    />

     <EmptyStateCard

      v-else

      title="등록된 멤버십이 없습니다."

      button-text="멤버십 등록"

    />

    <AssistantCard />


  </main>


<!-- 하단 네비게이션 -->
 <BottomNavigation />


</div>

</template>



<style scoped>

.home-view {

  padding:16px;

  padding-bottom:90px;

}

</style>
