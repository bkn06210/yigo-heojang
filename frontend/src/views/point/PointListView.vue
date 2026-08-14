<script setup>
import { ref, computed, onMounted } from 'vue';
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

// 혜택 리포트 API
import { getBenefitReport } from '@/api/benefitApi';

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
const pointList = computed(() => cardStore.points);

// 카드 등록 이동
const goCardRegister = () => {
  router.push('/cards/register');
};

// 로그인 이동
const goLogin = () => {
  router.push('/auth/login');
};

// 혜택 리포트
// GET /api/benefits/report — 이번 달
//
// 카드(합계)와 바텀시트(부문별·거래별)가 같은 응답을 나눠 쓴다.
// 따로 부르면 그 사이 결제가 일어났을 때 합계와 상세가 어긋난다.
const benefitReportData = ref(null);

// 카드가 쓰는 형태로 옮긴다. 조회 전이거나 받은 혜택이 없는 달이면 0원으로 그린다
// (혜택이 없는 것은 에러가 아니라 정상 상태다 — topCategoryName이 null로 온다).
const benefitReport = computed(() => ({
  totalBenefit: benefitReportData.value?.totalBenefitAmount ?? 0,
  maxCategory: benefitReportData.value?.topCategoryName ?? '아직 없어요',
}));

const loadBenefitReport = async () => {
  try {
    benefitReportData.value = await getBenefitReport();
  } catch (error) {
    console.error('혜택 리포트 조회 실패:', error);
  }
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

const refreshPoint = async () => {
  await cardStore.loadCards();
  await cardStore.loadPoints();
  await cardStore.loadMemberships();
  await loadBenefitReport();
};

// 페이지 로드 시 데이터 조회
onMounted(async () => {
  if (!isLogin.value) return;

  await cardStore.loadCards();
  await cardStore.loadPoints();
  await cardStore.loadMemberships();
  await loadBenefitReport();
});

// TODO: API 연결
// GET /api/user-memberships
//
// 로그인 후 등록한 멤버십만 내려옴
//
// 없음 -> []
// 있음 -> 멤버십 배열
//
// 등록된 멤버십 목록 - cardStore에서 로드
const membershipList = computed(() => cardStore.memberships);

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

    <!-- 비로그인 -->
    <main v-if="!isLogin" style="flex: 1; display: flex; align-items: center; justify-content: center;">
      <div style="text-align: center; display: flex; flex-direction: column; gap: 16px;">
        <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">로그인이 필요합니다</h2>
        <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">혜택을 받으려면 로그인해주세요.</p>
        <button @click="goLogin" style="padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer;">로그인</button>
      </div>
    </main>

    <!-- 로그인 -->
    <div v-else class="pull-container">
      <PullToRefresh @refresh="refreshPoint">
        <main class="content">
        <!-- 혜택 리포트 -->
        <section class="benefit-report-section">
          <h2>혜택 리포트</h2>

          <!-- 카드 없음 -->
          <EmptyStateCard
            v-if="cardList.length === 0"
            title="아직 혜택 정보가 없어요"
            description="카드를 등록하고 실제 사용하면, 혜택 리포트를 확인할 수 있습니다."
            buttonText="카드 등록"
            @click="goCardRegister"
          />

          <!-- 카드 있음 -->
          <BenefitReportCard
            v-else-if="benefitReport"
            :report="benefitReport"
            @open="openBenefitReport"
          />
        </section>

        <!-- 금융 포인트 -->
        <section class="point-section">
          <h2>금융 포인트</h2>

          <!-- 카드 없음 -->
          <EmptyStateCard
            v-if="cardList.length === 0"
            title="카드를 등록해 주세요"
            description="카드를 등록하면 각 은행의 금융 포인트를 관리할 수 있습니다."
            buttonText="카드 등록"
            @click="goCardRegister"
          />

          <!-- 포인트 없음 -->
          <EmptyStateCard
            v-else-if="pointList.length === 0"
            title="아직 포인트 정보가 없어요"
            description="등록한 카드를 사용하면 포인트가 쌓입니다."
          />

          <!-- 카드 있고 포인트도 있음 -->
          <FinancialPointCard v-else :points="pointList" @click="openPointSheet" />
        </section>

        <!-- 멤버십 -->
        <section class="membership-section">
          <div class="section-header">
            <h2>멤버십</h2>

            <button
              v-if="membershipList.length > 0"
              class="add-button"
              @click="goMembershipRegister"
            >
              + 추가
            </button>
          </div>

          <!-- 멤버십 없음 -->
          <EmptyStateCard
            v-if="membershipList.length === 0"
            title="아직 등록한 멤버십이 없어요"
            description="자주 방문하는 카페, 마트, 쇼핑몰의 멤버십을 등록해 혜택을 챙겨보세요."
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
    </div>

    <BottomNav />

    <!-- 금융 포인트 상세 -->
    <FinancialPointBottomSheet
      v-show="showPointSheet"
      v-if="selectedPoint"
      :point="selectedPoint"
      @close="closePointSheet"
    />

    <!-- 혜택 리포트 상세 (카드와 같은 응답을 그대로 넘긴다) -->
    <BenefitReportBottomSheet
      v-if="showBenefitReport"
      :report="benefitReportData"
      @close="closeBenefitReport"
    />
  </div>
</template>

<style scoped>
/* 전체 페이지 */

.point-page {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  overflow: visible;
}

.pull-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

main.login-required {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

/* 섹션 공통 */

section {
  width: 100%;
  padding: var(--space-md);
  box-sizing: border-box;
  border-radius: var(--radius-lg);
  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
}

section h2 {
  margin: 0 0 var(--space-md);
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  letter-spacing: -0.3px;
  color: var(--color-text-primary);
}

/* 혜택 리포트 */

.benefit-report-section {
  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  margin: 0;
}

.benefit-report-section h2 {
  margin-top: 0;
  margin-bottom: var(--space-md);
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
  background: rgba(var(--color-primary-rgb), 0.04);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  margin: 0;
}

.point-section :deep(.financial-point-card) {
  border-radius: var(--radius-lg);
}

/* 멤버십 */
.membership-section {
  background: rgba(var(--color-primary-rgb), 0.05);
  border: 1px solid rgba(var(--color-primary-rgb), 0.12);
  margin: 0;
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

/* 더보기 버튼 - 아웃라인 스타일 */

.more-button {
  width: 100%;

  margin-top: var(--space-md);

  height: 44px;

  border: 1.5px solid var(--color-primary);

  background: transparent;

  color: var(--color-primary-dark);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  border-radius: var(--radius-md);

  cursor: pointer;

  transition: var(--transition-fast);
}

.more-button:hover {
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border-color: var(--color-primary);
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
