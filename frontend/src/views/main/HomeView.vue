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
import SpendingSummaryCard from '@/components/home/SpendingSummaryCard.vue';
import ToastNotification from '@/components/common/ToastNotification.vue';

const router = useRouter();

// 로그인 상태
// TODO: 실제 API / Pinia 연결
const authStore = useAuthStore();

const { user } = storeToRefs(authStore);

// 카드 store 연결
const cardStore = useCardStore();

const { cards } = storeToRefs(cardStore);

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

// 홈 "내 카드"에는 카드 목록에서 고정(pinned)한 카드만 노출 (최대 3개까지 고정 가능)
const pinnedCards = computed(() => cards.value.filter((card) => card.pinned));
const hasPinnedCard = computed(() => pinnedCards.value.length > 0);

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
      achievementRate: 68
    });
  }
};

onMounted(async () => {
  await loadHome();
  // addMockData();
});
</script>

<template>
  <div class="home-view">
    <!-- 토스트 알림 -->
    <ToastNotification
      v-if="showToast"
      :type="toastType"
      :message="toastMessage"
      :duration="3000"
      @close="showToast = false"
    />

    <main class="home-content">
      <!-- Phase 3 테스트 버튼 -->
      <div
        style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap"
      >
        <button
          @click="showNotification('success', '인증이 완료되었습니다')"
          style="
            padding: 8px 12px;
            background: var(--color-accent-green);
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 12px;
          "
        >
          Success
        </button>
        <button
          @click="showNotification('error', '작업을 완료할 수 없습니다')"
          style="
            padding: 8px 12px;
            background: var(--color-accent-coral);
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 12px;
          "
        >
          Error
        </button>
        <button
          @click="showNotification('info', '새로운 알림 1개가 있습니다')"
          style="
            padding: 8px 12px;
            background: var(--color-accent-green);
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 12px;
          "
        >
          Info
        </button>
        <button
          @click="showNotification('warning', '이 작업은 되돌릴 수 없습니다')"
          style="
            padding: 8px 12px;
            background: var(--color-primary);
            color: var(--color-text-primary);
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 12px;
          "
        >
          Warning
        </button>
      </div>

      <HomeHeader
        :has-unread-notification="homeData.hasUnreadNotification"
        :user="user"
        @chat="goChatBot"
        @click-notification="goNotification"
        @profile="goProfile"
      />

      <!-- AI 브리핑 -->

      <AIBriefingCard v-if="user" :is-login="true" :message="briefingMessage" />

      <AIBriefingCard v-else :is-login="false" :message="briefingMessage" />

      <!-- 혜택 리포트 -->

      <section class="home-section benefit-section">
        <div class="section-header-row">
          <h2>혜택 리포트</h2>
        </div>

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
          buttonText="로그인"
          @click="goLogin"
        />
      </section>

      <!-- 카드 상세 내역 -->

      <section class="home-section spending-section">
        <div class="section-header-row">
          <h2>카드 상세 내역</h2>
        </div>

        <SpendingSummaryCard
          :count="spendingSummary.count"
          :total-amount="spendingSummary.totalAmount"
          :has-card="hasCard"
          :is-logged-in="!!user"
          @open="goSpending"
          @go-card-list="goCardList"
        />
      </section>

      <!-- 카드 -->

      <section class="home-section">
        <div class="section-header-row">
          <h2>내 카드</h2>
          <button
            v-if="user && hasCard"
            type="button"
            class="section-more-btn"
            @click="goCardList"
          >
            더보기
          </button>
        </div>

        <!-- 카드 목록에서 고정(pinned)한 카드만 표시 -->
        <MyCardSummaryCard
          v-if="user && hasPinnedCard"
          :is-login="true"
          :cards="pinnedCards"
          @click-card="goCardDetail"
          @click-more="goCardList"
        />

        <!-- 카드는 있지만 고정한 카드가 없는 경우: 카드 등록 유도가 아니라 '고정' 유도 -->
        <EmptyStateCard
          v-else-if="user && hasCard && !hasPinnedCard"
          title="고정된 카드가 없어요"
          description="카드 목록에서 카드를 고정하면 여기에 표시됩니다."
          buttonText="카드 고정하러 가기"
          @click="goCardList"
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
          buttonText="로그인"
          @click="goLogin"
        />
      </section>

      <!-- 금융 포인트 & 멤버십 (Bento UI 2열 레이아웃) -->

      <div class="home-grid">
        <!-- 금융 포인트 -->

        <section class="home-section point-section">
          <div class="section-header-row">
            <h2>금융 포인트</h2>
            <button
              v-if="user && hasCard"
              type="button"
              class="section-more-btn"
              @click="goPointList"
            >
              더보기
            </button>
          </div>

          <PointSummaryCard
            v-if="user && hasCard"
            :is-login="true"
            :points="homeData.financialPoints"
            @click-item="goPointDetail"
          />

          <EmptyStateCard
            v-else
            title="금융 포인트를 확인할 수 없어요"
            description="카드 등록 후 포인트를 관리할 수 있습니다."
          />
        </section>

        <!-- 멤버십 -->

        <section class="home-section membership-section">
          <div class="section-header-row">
            <h2>멤버십</h2>
          </div>

          <MembershipSummaryCard
            v-if="user && hasMembership"
            :memberships="homeData.memberships"
            @click-item="goMembershipDetail"
            @click-more="goPointList"
          />

          <EmptyStateCard
            v-else-if="user && !hasMembership"
            title="등록된 멤버십이 없어요"
            description="멤버십을 등록하면 혜택을 함께 관리할 수 있습니다."
            buttonText="멤버십 등록"
            @click="goMembershipRegister"
          />

          <EmptyStateCard
            v-else
            title="로그인 후 이용할 수 있어요"
            description="로그인하면 멤버십을 관리할 수 있습니다."
          />
        </section>
      </div>
    </main>

    <BottomNavigation />
  </div>
</template>

<style scoped>

/* 홈 전체 */
.home-view {
  min-height: 100vh;

  padding: var(--space-md);

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

  background: var(--color-bg);

  box-sizing: border-box;

  width: 100%;

  max-width: 480px;

  margin: 0 auto;

  overflow: visible;
}

/* 콘텐츠 영역 */
.home-content {
  display: flex;

  flex-direction: column;

  gap: var(--space-xl);

  width: 100%;

  max-width: 100%;

  box-sizing: border-box;
}

/* 각 섹션 */
.home-section {
  width: 100%;

  box-sizing: border-box;

  display: flex;

  flex-direction: column;
}

/* 섹션 제목 */
.home-section h2 {
  margin: 0 0 var(--space-md);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  letter-spacing: -0.3px;
}

/* 제목 + 더보기를 한 줄에 배치하는 헤더 (카드 밖) */
.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header-row h2 {
  margin: 0 0 var(--space-md);
}

.section-more-btn {
  border: none;
  background: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

/* Empty 버튼 영역 */
.home-section :deep(.button-group) {
  margin-top: var(--space-xs);
}

/* Bento UI 그리드 - 금융 포인트 & 멤버십 2열 */
.home-grid {
  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: var(--space-md);

  grid-auto-rows: 1fr;

  width: 100%;

  max-width: 100%;

  box-sizing: border-box;
}

.home-grid .home-section {
  display: flex;

  flex-direction: column;

  height: 100%;
}

.home-grid .home-section h2 {
  margin-bottom: var(--space-md);

  flex-shrink: 0;
}

.home-grid .home-section :deep(.base-card),
.home-grid .home-section :deep(.empty-card) {
  flex: 1;

  display: flex;

  flex-direction: column;

  justify-content: flex-start;

  min-height: 200px;
}

:deep(.empty-card button:first-child) {
  background: var(--color-primary);

  color: var(--color-btn-primary-text);
}
</style>
