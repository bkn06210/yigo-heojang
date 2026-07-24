<script setup>
import HomeHeader from '@/components/home/HomeHeader.vue'
import AIBriefingCard from '@/components/common/AIBriefingCard.vue'
import MyCardSummaryCard from '@/components/home/MyCardSummaryCard.vue'
import PointSummaryCard from '@/components/home/PointSummaryCard.vue'
import MembershipSummaryCard from '@/components/home/MembershipSummaryCard.vue'
import AssistantCard from '@/components/home/AssistantCard.vue'
import BottomNav from '@/components/common/BottomNav.vue'

import { ref, onMounted } from 'vue'

// 헤더 버튼 이벤트
const goChatBot = () => {
  console.log('챗봇')
}

const goNotification = () => {
  console.log('알림')
}

const goProfile = () => {
  console.log('프로필')
}


const goCardDetail = () => {
  console.log('카드 상세')
}

const goCardList = () => {
  console.log('카드 목록')
}

// 홈 화면 데이터 (현재 Mock)
// 나중에 API 응답으로 교체 예정
const homeData = ref({
  hasUnreadNotification: true,

  briefing: {
    title: '이번 달 가장 큰 혜택은 신한카드입니다.',
    content: '약 18,000원의 혜택을 받을 수 있습니다.',
  },

  myCard: {
    image: '', // 카드 이미지 URL (현재는 없음)

    cardName: '신한 Mr.Life',
    currentAmount: 300000,
    targetAmount: 500000,
    remainAmount: 200000,
    remainBenefit: 13500,
    achievementRate: 60,
  },

  financialPoints: [
    {
      id: 1,
      name: '마이신한포인트',
      balance: 2234,
    },
    {
      id: 2,
      name: '포인트리',
      balance: 4456,
    },
  ],

  memberships: [
    {
      id: 1,
      name: '해피포인트',
    },
    {
      id: 2,
      name: 'CJ ONE',
    },
  ],
})

// 홈 데이터 조회
// (나중에 API 연결)
const loadHome = async () => {
  console.log('홈 데이터 조회')
}

// Pull To Refresh
const refreshHome = async () => {
  await loadHome()
}

// 최초 진입
onMounted(async () => {
  await loadHome()
})

const goPointList = () => {
  console.log('포인트 목록 이동')
}


const goPointDetail = (item) => {
  console.log('금융 포인트 상세', item)
}


const goMembershipDetail = (item) => {
  console.log('멤버십 상세', item)
}
</script>



<template>
    <div class="home-view">
    <!-- 상단 헤더 -->
    <HomeHeader
      :has-unread-notification="homeData.hasUnreadNotification"
      @chat="goChatBot"
      @notification="goNotification"
      @profile="goProfile"
    />

    <!-- AI 브리핑 -->
    <AIBriefingCard
      :briefing="homeData.briefing"
    />

    <!-- 내 카드 -->
    <MyCardSummaryCard
      :card="homeData.myCard"
      @click-card="goCardDetail"
      @click-more="goCardList"
    />

    <!-- 금융 포인트 -->
    <PointSummaryCard
  v-if="homeData.financialPoints.length"
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
  v-if="homeData.memberships.length"
  :memberships="homeData.memberships"
  @click-more="goPointList"
  @click-item="goMembershipDetail"
/>

<EmptyStateCard
  v-else
  title="등록된 멤버십이 없습니다."
  button-text="멤버십 등록"
/>

    <!-- AI 소개 -->
    <AssistantCard />

    <!-- 하단 네비게이션 -->
    <BottomNav />
  </div>

</template>

<style scoped>
.home-view {
  padding: 16px;
  padding-bottom: 90px;
}

</style>
