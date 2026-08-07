<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';

import HomeHeader from '@/components/home/HomeHeader.vue';
import Icon from '@/components/common/Icon.vue';
import AIBriefingCard from '@/components/common/AIBriefingCard.vue';
import MyCardSummaryCard from '@/components/home/MyCardSummaryCard.vue';
import PointSummaryCard from '@/components/home/PointSummaryCard.vue';
import MembershipSummaryCard from '@/components/home/MembershipSummaryCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';
import BenefitReportCard from '@/components/point/BenefitReportCard.vue';
import SpendingSummaryCard from '@/components/home/SpendingSummaryCard.vue';
import ToastNotification from '@/components/common/ToastNotification.vue';

const router = useRouter();

// 로그인 상태
// TODO: 실제 API / Pinia 연결
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);

// 카드 store 연결
const cardStore = useCardStore();

const { cards, points } = storeToRefs(cardStore);

// 금융 포인트 - 제일 많은 순서로 3개까지
const topPoints = computed(() =>
  points.value
    .sort((a, b) => b.balance - a.balance)
    .slice(0, 3)
);

// 토스트 알림 상태
const toastType = ref('success');
const toastMessage = ref('');
const showToast = ref(false);

const showNotification = (type, message) => {
  toastType.value = type;
  toastMessage.value = message;
  showToast.value = true;
};

const hasMembership = computed(
  () => (homeData.value.memberships?.length ?? 0) > 0,
);

const hasCard = computed(() => cards.value.length > 0);

// 알림 상태: 비로그인이면 false, 로그인했을 때만 실제 알림 상태 표시
const hasUnreadNotification = computed(() => {
  return user.value ? homeData.value.hasUnreadNotification : false;
});

// 홈 "내 카드"에는 카드 목록에서 고정(pinned)한 카드만 노출 (최대 3개까지 고정 가능)
const pinnedCards = computed(() => cards.value.filter((card) => card.pinned));
const hasPinnedCard = computed(() => pinnedCards.value.length > 0);

// 시간대별 인삿말
const timeGreeting = computed(() => {
  const hour = new Date().getHours();
  const nickname = user.value?.nickname || user.value?.name || '사용자';

  if (hour >= 6 && hour < 12) {
    return { message: `좋은 아침이에요, ${nickname}님! ☀️`, emoji: '☀️' };
  } else if (hour >= 12 && hour < 18) {
    return { message: `좋은 오후예요, ${nickname}님! 🌤️`, emoji: '🌤️' };
  } else if (hour >= 18 && hour < 22) {
    return { message: `좋은 저녁이에요, ${nickname}님! 🌙`, emoji: '🌙' };
  } else {
    return { message: `늦은 시간이네요, 푹 쉬세요! 😴`, emoji: '😴' };
  }
});

// 현재 날짜 포맷
const currentDate = computed(() => {
  const today = new Date();
  const options = { year: 'numeric', month: 'long', day: 'numeric' };
  return today.toLocaleDateString('ko-KR', options);
});

// 두리 브리핑 메시지 - 동적 생성
const briefingMessage = computed(() => {
  if (!user.value) {
    return '로그인 후 나의 카드 혜택과 포인트 분석을 기반으로 맞춤형 추천을 받을 수 있어요.';
  }

  const nickname = user.value.nickname || user.value.name || '사용자';
  return `${nickname}님, 카드를 등록하면 맞춤 혜택 분석을 받을 수 있어요.`;
});

// 홈 데이터
const homeData = ref({
  hasUnreadNotification: true,

  briefing: {
    content: '',
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

  memberships: [],
});

// 혜택 리포트
// TODO: GET /benefits/report 연결

const benefitReport = {
  totalBenefit: 12500,

  maxCategory: '구독/콘텐츠',
};

// 이번 달 소비내역 요약 (홈 화면 소비내역 카드용 데이터)
// TODO: GET /transactions/summary 연결 — 현재는 임시 데이터
// API 연동 시:
//   - 엔드포인트: GET /transactions/summary
//   - 응답 필드:
//     - count: 이번 달 총 결제 건수 (정수)
//     - totalAmount: 이번 달 총 소비액 (KRW, 정수, 십원 단위)
//   - 응답 형식: { success: true, code: "SUCCESS", data: { count, totalAmount }, message: null }
//   - 쿼리 파라미터 처리: cardId가 없으면 전체, 있으면 해당 카드만 반환
//     (예: /transactions/summary?cardId=1 → cardId가 1인 카드의 이번 달 요약)
const spendingSummary = {
  count: 8,

  totalAmount: 342000,
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

const goCardDetail = (card) => {
  router.push(`/cards/${card.id}`);
};

const goCardList = () => {
  router.push('/cards');
};

// 혜택 이동

const goBenefit = () => {
  router.push('/benefits');
};

// 소비내역 이동 (홈에서는 전체 카드 소비내역)
const goSpending = () => {
  router.push('/transactions');
};

// 포인트

const goPointList = () => {
  router.push('/points');
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

// 로그인 페이지 이동
const goLogin = () => {
  router.push('/auth/login');
};

// 회원가입 페이지 이동
const goSignup = () => {
  router.push('/auth/signup');
};

// 데이터 조회

const loadHome = async () => {
  if (!user.value && !localStorage.getItem('token')) return;
  try {
    await cardStore.loadMonthlyStatuses();
    homeData.value.myCard = cards.value[0] || null;
    if (cardBriefing.value?.message) {
      homeData.value.briefing.content = cardBriefing.value.message;
    }
  } catch (error) {
    console.error('홈 카드 현황 조회 실패', error);
  }
};

const addMockData = () => {
  if (!user.value) {
    authStore.setLogin('dummy-token', {
      id: 1,
      email: 'test@example.com',
      name: '테스트',
      nickname: '테스트유저'
    });
  }
  if (cards.value.length === 0) {
    cardStore.addCard({
      id: 1,
      name: '신한 Mr.Life',
      cardNumber: '4111111111111111',
      company: '신한카드',
      image: '',
      pinned: false,
      achievementRate: 68,
      benefits: ['카페 10% 할인', '식당 5% 캐시백', '교통비 2배 적립']
    });
  } else {
    // 이미 있는 카드에 benefits 추가
    cards.value.forEach(card => {
      if (!card.benefits) {
        card.benefits = ['카페 10% 할인', '식당 5% 캐시백', '교통비 2배 적립'];
      }
    });
  }
};

onMounted(async () => {
  await loadHome();
  addMockData();
});
</script>



<template>
</template>



<style scoped>

/* 홈 전체 */
.home-view {
  display: flex;

  flex-direction: column;

</style>
<!-- 07_25 연동 변경: 홈 요약 데이터를 카드·포인트·멤버십 API에서 조회한다. -->
