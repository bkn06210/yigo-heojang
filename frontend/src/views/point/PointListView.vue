<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';

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

const router = useRouter();

const authStore = useAuthStore();

// 로그인 상태
const isLogin = computed(() => authStore.isLogin());

// TODO: 카드 조회 API 연결
// GET /api/cards
//
// 로그인 + 카드 없음 -> []
// 카드 있음 -> 카드 데이터 배열
const cardStore = useCardStore();

const cardList = computed(() => cardStore.cards);

// 카드 등록 이동
const goCardRegister = () => {
  router.push('/cards/register');
};

// 로그인 이동
const goLogin = () => {
  router.push('/auth/login');
};

// 혜택 리포트

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

// 금융 포인트

const showPointSheet = ref(false);

const selectedPoint = ref(null);

// 금융 포인트 상세 열기

const openPointSheet = (point) => {
  if (!point) return;

  selectedPoint.value = {
    ...point,
    totalPoint: point.point,
  };

  showPointSheet.value = true;
};

// 닫기

const closePointSheet = () => {
  showPointSheet.value = false;
};

// 새로고침
// TODO: API 연결 시 실제 데이터 재조회

const refreshPoint = async () => {
  console.log('혜택 데이터 갱신');
};



// TODO: API 연결
// GET /api/user-memberships
//
// 로그인 후 등록한 멤버십만 내려옴
//
// 없음 -> []
// 있음 -> 멤버십 배열
//
// MembershipRegisterView.vue의 addMembership()에서 "추가"를 눌러도
// 이 배열과 연결되어 있지 않아 여기 목록에는 반영되지 않음 (같은 TODO 참고)
const membershipList = ref([]);

const goMembershipRegister = () => {
  router.push('/memberships/register');
};

// 멤버십 더보기

const SHOW_COUNT = 3;

const visibleCount = ref(SHOW_COUNT);

const visibleMemberships = computed(() => {
  return membershipList.value.slice(0, visibleCount.value);
});

const showMoreMembership = () => {
  visibleCount.value += SHOW_COUNT;
};
</script>

<template>
  <div class="point-page">
    <PageHeader title="혜택" :show-back="false" @back="router.back()" />

    <PullToRefresh @refresh="refreshPoint">
      <main class="content">
        <!-- 혜택 리포트 -->
        <section class="benefit-report-section">
          <h2>혜택 리포트</h2>

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
          <h2>금융 포인트</h2>

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
          <FinancialPointCard v-else @click="openPointSheet" />
        </section>

        <!-- 멤버십 -->
        <section class="membership-section">
          <div class="section-header">
            <h2>멤버십</h2>

            <button
              v-if="isLogin && membershipList.length > 0"
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
    </PullToRefresh>

    <BottomNav />

    <!-- 금융 포인트 상세 -->
    <FinancialPointBottomSheet
      v-show="showPointSheet"
      v-if="selectedPoint"
      :point="selectedPoint"
      @close="closePointSheet"
    />

    <!-- 혜택 리포트 상세 -->
    <BenefitReportBottomSheet
      v-if="showBenefitReport"
      @close="closeBenefitReport"
    />
  </div>
</template>

<style scoped>
/* 전체 페이지 */

.point-page {
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  box-sizing: border-box;
  overflow: hidden visible;
}

.content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2xl);
}

/* 섹션 공통 */

section {
  width: 100%;
}

h2 {
  margin: 0 0 var(--space-md);

  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  letter-spacing: -0.3px;

  color: var(--color-text-primary);
}

/* 혜택 리포트 */

.benefit-report-section {
  margin-bottom: 0;
}

/* BenefitReportCard 내부 카드 느낌 */

.benefit-report-card {
  width: 100%;

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-md);

  box-sizing: border-box;

  box-shadow: var(--shadow-card);
}

/* 금융 포인트 */

.point-section {
  margin-top: 0;
}

.point-section :deep(.financial-point-card) {
  border-radius: var(--radius-lg);
}

/* 멤버십 */
.membership-section {
  margin-top: 0;
}

.section-header {
  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: var(--space-md);
}

.section-header h2 {
  margin-bottom: 0;
}

.add-button {
  border: none;

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  padding: var(--space-xs) var(--space-sm);

  border-radius: var(--radius-lg);

  font-size: var(--font-xs);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);
}

.add-button:hover {
  opacity: 0.9;
}

/* 멤버십 카드 사이 간격 */

.membership-section :deep(.membership-card) {
  margin-bottom: var(--space-sm);
}

/* 더보기 버튼 */

.more-button {
  width: 100%;

  margin-top: var(--space-xs);

  height: 44px;

  border: none;

  background: none;

  color: var(--color-text-secondary);

  font-size: var(--font-sm);

  font-weight: var(--font-medium);

  cursor: pointer;

  transition: var(--transition-fast);
}

.more-button:hover {
  color: var(--color-text-primary);
}

/* 안내 문구 */

.notice {
  margin: var(--space-md) 0 0;

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

  line-height: 1.5;
}

/* EmptyStateCard */

:deep(.empty-card) {
  width: 100%;

  min-height: 150px;

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-2xl) var(--space-md);

  box-sizing: border-box;

  box-shadow: var(--shadow-card);

  display: flex;

  flex-direction: column;

  justify-content: center;

  align-items: center;

  text-align: center;
}

:deep(.empty-title) {
  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);
}

:deep(.empty-description) {
  margin-top: var(--space-xs);

  font-size: var(--font-xs);

  color: var(--color-text-secondary);

  line-height: 1.5;
}

:deep(.button-group) {
  margin-top: var(--space-lg);
}

:deep(.empty-card button) {
  min-width: 110px;

  height: 42px;

  border: none;

  border-radius: var(--radius-full);

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;
}

/* Bottom Sheet 대응 */
:deep(.bottom-sheet) {
  border-radius: 24px 24px 0 0;
}

/* 모바일 화면 보정 */
@media (max-width: 480px) {
  .point-page {
    padding: var(--space-sm);
  }

  h2 {
    font-size: var(--font-lg);
  }

  .benefit-report-card {
    padding: var(--space-md);
  }
}
</style>
