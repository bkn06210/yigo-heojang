<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';
import { getTransactionsSummary } from '@/api/walletApi';
import { getBenefitReport } from '@/api/benefitApi';

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

// briefing 은 GET /api/cards/monthly-status 응답에 함께 오고 cardStore 가 채운다.
const { cards, points, memberships, briefing } = storeToRefs(cardStore);

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
  () => (memberships.value?.length ?? 0) > 0,
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
    return { message: `좋은 아침이에요, ${nickname}님!`, emoji: '☀️' };
  } else if (hour >= 12 && hour < 18) {
    return { message: `좋은 오후예요, ${nickname}님!`, emoji: '🌤️' };
  } else if (hour >= 18 && hour < 22) {
    return { message: `좋은 저녁이에요, ${nickname}님!`, emoji: '🌙' };
  } else {
    return { message: `늦은 시간이네요, 푹 쉬세요!`, emoji: '😴' };
  }
});

// 현재 날짜 포맷
const currentDate = computed(() => {
  const today = new Date();
  const options = { year: 'numeric', month: 'long', day: 'numeric' };
  return today.toLocaleDateString('ko-KR', options);
});

// 두리 브리핑
// 문장은 서버가 숫자까지 끼워 완성해 내려준다. 화면에서 다시 조립하지 않는다 —
// 조립을 양쪽에서 하면 서버가 판단한 상황과 화면에 뜬 문장이 갈린다.
// 보유 카드가 0장이어도 카드 등록을 안내하는 브리핑이 내려오므로 여기서 따로 만들 것이 없다.
//
// 아래 고정 문구는 조회 자체가 실패했을 때만 쓰는 폴백이다.
const briefingMessage = computed(() => {
  if (!user.value) {
    return '로그인 후 나의 카드 혜택과 포인트 분석을 기반으로 맞춤형 추천을 받을 수 있어요.';
  }

  if (briefing.value?.message) {
    return briefing.value.message;
  }

  const nickname = user.value.nickname || user.value.name || '사용자';
  return `${nickname}님, 이번 달 카드 실적을 확인해보세요.`;
});

// 상황 구분값(NO_CARD·UNUSED_BENEFIT·PERFORMANCE_NEAR·ALL_ACHIEVED·SPENDING_INSIGHT·GETTING_STARTED).
// 화면 분기는 이 값으로 한다. 문장을 뜯어 분기하면 문구가 바뀔 때마다 깨진다.
const briefingType = computed(() => briefing.value?.type ?? null);

// 홈 데이터
const homeData = ref({
  hasUnreadNotification: true,

  briefing: {
    content: '',
  },
});

// 혜택 리포트
// GET /api/benefits/report — 이번 달
//
// 홈 카드는 총액과 최대 부문만 쓴다. 부문별 목록·거래 상세는 같은 응답의
// categories[]에 들어 있고 혜택 화면(PointListView)의 바텀시트가 쓴다.
const benefitReportData = ref(null);

// 조회 전과 조회 실패에도 null을 내지 않는다. 카드가 report를 필수로 받는 데다,
// 여기서 null을 내면 아래 v-if 사슬이 로그인 안내 문구로 떨어져 로그인한 사용자에게
// 엉뚱한 안내가 나간다. 값이 없을 때는 0원으로 그린다.
const benefitReport = computed(() => ({
  totalBenefit: benefitReportData.value?.totalBenefitAmount ?? 0,
  // 받은 혜택이 없는 달이면 topCategoryName이 null로 온다 (에러가 아니다).
  maxCategory: benefitReportData.value?.topCategoryName ?? '아직 없어요',
}));

const loadBenefitReport = async () => {
  try {
    benefitReportData.value = await getBenefitReport();
  } catch (error) {
    console.error('혜택 리포트 조회 실패:', error);
  }
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
const spendingSummary = ref({
  count: 0,
  totalAmount: 0,
});

const loadSpendingSummary = async () => {
  try {
    // 현재 월을 "2026-08" 형식으로 계산
    const today = new Date();
    const currentMonth = String(today.getMonth() + 1).padStart(2, '0');
    const yearMonth = `${today.getFullYear()}-${currentMonth}`;

    const summary = await getTransactionsSummary({ yearMonth });
    spendingSummary.value = summary;
  } catch (error) {
    console.error('거래 요약 조회 실패:', error);
  }
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
  router.push('/points');
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

const goMembershipDetail = (membership) => {
  router.push(`/memberships/${membership.membershipRegisterId}`);
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

onMounted(async () => {
  await cardStore.loadCards();
  await cardStore.loadPoints();
  await cardStore.loadMemberships();
  await loadHome();
  await loadSpendingSummary();
  await loadBenefitReport();

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
      <!-- 프로필 섹션 -->
      <div class="profile-briefing-section">
        <!-- 프로필 헤더 섹션 -->
        <section v-if="user" class="profile-header-section">
          <div class="profile-avatar-large" @click="goProfile">
            <img v-if="user.profileImageUrl" :src="user.profileImageUrl" alt="프로필" class="profile-image" />
            <span v-else>{{ user.nickname?.charAt(0) || user.name?.charAt(0) || '👤' }}</span>
          </div>
          <p class="profile-date-text">{{ currentDate }}</p>
          <p class="greeting-text">{{ timeGreeting.message }}</p>
        </section>

        <!-- 우측 상단 버튼들 -->
        <div class="profile-header-buttons">
          <button @click="goChatBot" class="header-btn" title="AI 챗봇" data-tour="chat-button">
            <Icon name="aibot" size="md" />
          </button>
          <button @click="goNotification" class="header-btn notification-btn" :class="{ 'has-notification': hasUnreadNotification }" title="알림" data-tour="notification-button">
            <Icon name="bell" size="md" />
            <span v-if="hasUnreadNotification" class="notification-dot" />
          </button>
        </div>
      </div>

      <!-- AI 브리핑 -->
      <section class="home-section">
        <AIBriefingCard
          v-if="user"
          :is-login="true"
          :message="briefingMessage"
          :briefing-type="briefingType"
          @register-card="goCardList"
        />

        <AIBriefingCard v-else :is-login="false" :message="briefingMessage" />
      </section>

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
          <h2>카드 이용 내역</h2>
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
            :points="topPoints"
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
            <button
              v-if="user && hasMembership"
              type="button"
              class="section-more-btn"
              @click="goMembershipRegister"
            >
              더보기
            </button>
          </div>

          <MembershipSummaryCard
            v-if="user && hasMembership"
            :memberships="memberships"
            @click-item="goMembershipDetail"
            @click-more="goMembershipRegister"
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

  margin-bottom: var(--space-lg);
}


/* 혜택 리포트 (더 큰 간격) */
.benefit-section {
  margin-top: var(--space-lg);
  margin-bottom: var(--space-xl);
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
  height: var(--font-lg);
  display: flex;
  align-items: center;
  margin-top: calc(var(--font-lg) * -0.3);
}

/* 프로필 섹션 */
.profile-briefing-section {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-lg);
  margin: 0;
  padding: var(--space-lg) var(--space-md) var(--space-md) 0;
  background: transparent;
}


/* 프로필 헤더 섹션 */
.profile-header-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-sm);
  margin-bottom: 0;
  padding: 0;
  background: transparent;
  border-radius: 0;
  border: none;
  box-shadow: none;
}

/* 프로필 사진 */
.profile-avatar-large {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: var(--font-bold);
  color: white;
  flex-shrink: 0;
  cursor: pointer;
  transition: var(--transition-fast);
  overflow: hidden;
}

.profile-avatar-large:hover {
  opacity: 0.8;
  transform: scale(1.05);
}

.profile-avatar-large .profile-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-large span {
  color: white;
}

[data-theme="dark"] .profile-avatar-large span {
  color: black;
}

/* 프로필 헤더 우측 버튼들 */
.profile-header-buttons {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-left: auto;
  margin-right: -20px;
}

.header-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-fast);
  color: var(--color-text-primary);
  padding: 0;
}

.header-btn:hover {
  opacity: 0.7;
}

.header-btn.notification-btn {
  position: relative;
}

.notification-dot {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
}

/* 날짜 */
.profile-date-text {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

/* 인사말 */
.greeting-text {
  margin: 0;
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: 1.4;
  white-space: nowrap;
  max-width: 100%;
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

  justify-content: center;

  min-height: 200px;
}

:deep(.empty-card button:first-child) {
  background: var(--color-primary);

  color: var(--color-btn-primary-text);
}
</style>
