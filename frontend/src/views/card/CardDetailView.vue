<script setup>

import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCardStore } from '@/stores/cardStore';
import { getUserCardDetail } from '@/api/cardApi';
import { getCardMonthlyStatus, getTransactions } from '@/api/walletApi';

import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { useToast } from '@/composables/useToast';

const cardStore = useCardStore();

const route = useRoute();

// 라우터
const router = useRouter();
const { showToast } = useToast();

// 더보기 메뉴 표시
const showMenu = ref(false);

// 별칭/메모 입력 모달
const showEditModal = ref(false);

// 삭제 확인 모달
const showDeleteModal = ref(false);
const isLandscapeImage = ref(false);

const handleCardImageLoad = (event) => {
  const image = event.currentTarget;
  isLandscapeImage.value = image.naturalWidth > image.naturalHeight;
};

// 수정 대상
const editType = ref('');

// 카드 메모 (별칭 + 메모 통합)
const cardMemo = ref('');

// 입력 임시 값
const editValue = ref('');

// 메뉴 열기
const openMenu = () => {
  showMenu.value = !showMenu.value;
};

// 메모 추가/수정 열기
const editMemo = () => {
  editType.value = 'memo';

  editValue.value = cardMemo.value;

  showEditModal.value = true;

  showMenu.value = false;
};

// 메모 삭제
const deleteMemo = () => {
  cardMemo.value = '';
};

// 수정 저장
const saveEdit = () => {
  if (editType.value === 'memo') {
    cardMemo.value = editValue.value;
  }

  showEditModal.value = false;
};

// 수정 취소
const closeEdit = () => {
  showEditModal.value = false;
};

// 삭제
const deleteCard = () => {
  showDeleteModal.value = true;
};

// 삭제 취소
const closeDelete = () => {
  showDeleteModal.value = false;
};

// 삭제 확인
  /*
    실제 API

    DELETE /cards/{cardId}

  */
const confirmDelete = () => {

  const cardId = Number(route.params.id);


  cardStore.removeCard(cardId);


  showDeleteModal.value = false;


  showToast('success', '카드가 삭제되었습니다');


  router.push('/cards');

};

const card = ref({
  id: route.params.id,
  name: '',
  company: '',
  owner: '본인',
  cardNumber: '',
  image: '/images/cards/shinhan.png',
});

const recentTransactions = ref([]);

// 주요 혜택 달성 막대 — GET /api/cards/{userCardId}/monthly-status 의 benefits[]
const benefitProgress = ref([]);

// 최대 몇 줄까지 보여줄지. 원래 화면이 세 줄이었던 걸 그대로 유지한다.
const BENEFIT_PROGRESS_LIMIT = 3;

/**
 * 혜택 이용현황을 진행률 막대가 쓰는 형태로 옮긴다.
 *
 * - monthlyLimit이 없는 혜택은 usageRate가 null로 온다. 한도가 없으면 "몇 % 썼다"가
 *   성립하지 않으므로 막대로 그리지 않고 뺀다.
 * - limitGroupCode가 같은 혜택들은 한도를 공유해서 서버가 같은 값을 내려준다.
 *   그대로 두면 같은 수치가 여러 줄 반복되므로 코드당 한 줄로 묶는다.
 * - 이용률이 높은 것부터 보여준다. 한도가 임박한 혜택이 먼저 눈에 들어와야 한다.
 */
const toBenefitProgress = (benefits) => {
  const seenGroups = new Set();
  const rows = [];

  for (const benefit of benefits) {
    if (benefit?.usageRate == null) {
      continue;
    }

    if (benefit.limitGroupCode) {
      if (seenGroups.has(benefit.limitGroupCode)) {
        continue;
      }
      seenGroups.add(benefit.limitGroupCode);
    }

    rows.push({
      key: benefit.limitGroupCode || `benefit-${benefit.benefitId}`,
      name: benefit.benefitName,
      // 한도를 넘겨 100%를 초과해 오더라도 막대가 칸을 삐져나가지 않게 자른다.
      rate: Math.min(100, Math.max(0, Math.round(Number(benefit.usageRate)))),
    });
  }

  return rows
    .sort((a, b) => b.rate - a.rate)
    .slice(0, BENEFIT_PROGRESS_LIMIT);
};

const loadCard = async () => {
  try {
    const data = await getUserCardDetail(route.params.id);
    card.value = {
      ...card.value,
      ...data,
      id: data.userCardId,
      name: data.cardName,
      company: data.issuerName,
      image: data.imageUrl,
    };

    // 이 카드의 거래 데이터 로드
    const params = { userCardId: card.value.userCardId };
    const transactionResponse = await getTransactions(params);
    const transactions = transactionResponse?.transactions || [];
    recentTransactions.value = transactions.slice(0, 3).map((t) => ({
      date: t.paymentDate.split('T')[0].slice(5),
      merchant: t.merchantName || t.categoryName,
      amount: `-${t.paymentAmount.toLocaleString()}원`,
    }));
  } catch (error) {
    console.error('카드 상세 조회 실패:', error);
  }

  // 카드 상세 조회가 실패했으면 카드 id를 모르는 상태라 여기서 멈춘다.
  if (!card.value.userCardId) {
    return;
  }

  // 혜택 현황은 별도 API라 따로 잡는다. 이쪽이 실패해도 카드 정보와 이용내역은 남는다.
  try {
    const status = await getCardMonthlyStatus(card.value.userCardId);
    benefitProgress.value = toBenefitProgress(status?.benefits || []);
  } catch (error) {
    console.error('카드 혜택 현황 조회 실패:', error);
    benefitProgress.value = [];
  }
};

onMounted(loadCard);

// 카드번호 표시
const maskCardNumber = (number) => {
  if (!number) return '';

  const lastFour = number.slice(-4);

  return `${lastFour.slice(0, 3)}*`;
};

// 혜택 상세 이동
// card 는 ref 라 스크립트에서는 .value 를 거쳐야 한다. 템플릿은 자동 언랩이라
// card.id 로 써도 보이지만, 여기서 빠뜨리면 /benefits/undefined 로 이동한다.
const goBenefitDetail = () => {
  router.push(`/benefits/${card.value.id}`);
};

// 소비내역 이동 (이 카드의 소비내역만 필터링해서 보여줌)
const goTransaction = () => {
  router.push({
    path: '/transactions',
    query: { cardId: card.value.userCardId, cardName: card.value.name },
  });
};

</script>



<template>
  <div class="card-detail-page">

    <!-- 헤더 -->
<div class="detail-header">

  <PageHeader title="카드 상세" @back="router.back()" />

  <div class="menu-wrapper">

    <button
      class="menu-button"
      @click="openMenu"
    >
      ⋮
    </button>


    <!-- 메뉴 -->
    <div
      v-if="showMenu"
      class="menu-popover"
    >

      <button @click="editMemo">
        {{ cardMemo ? '메모 수정' : '메모 추가' }}
      </button>


      <button
        class="delete"
        @click="deleteCard"
      >
        카드 삭제
      </button>

    </div>

  </div>


</div>

    <!-- 카드 이미지 -->
    <section class="card-image-section">
      <div v-if="card.image" class="card-image-frame">
        <img
          :src="card.image"
          :alt="card.name"
          class="card-image"
          :class="{ portrait: !isLandscapeImage }"
          @load="handleCardImageLoad"
        />
      </div>
    </section>

    <!-- 카드 정보 -->
<section class="card-info">

  <div class="card-header">
    <div class="card-title-row">
      <div class="card-left">
        <h1 class="card-name">
          {{ card.name }}
        </h1>
        <span class="number">
          [{{ card.owner }}] {{ maskCardNumber(card.maskedCardNumber) }}
        </span>
      </div>

      <div class="card-divider">|</div>

      <div class="card-right">
        <!-- 카드사명 항상 표시 -->
        <div class="company-display">
          {{ card.company }}
        </div>
      </div>
    </div>

    <!-- 메모가 있으면 아래에 메모 + 삭제버튼 표시 -->
    <div v-if="cardMemo" class="memo-display">
      <span>{{ cardMemo }}</span>
      <button class="delete-memo-btn" @click="deleteMemo">×</button>
    </div>
  </div>

  <div class="card-details">
    <p class="company">
      {{ card.company }}
    </p>
  </div>

</section>

    <!-- 혜택 상세 -->
    <button class="benefit-button" @click="goBenefitDetail">
      혜택 자세히 보기
    </button>

    <!-- 혜택 달성 -->
    <section v-if="benefitProgress.length" class="benefit-progress">
      <h2>주요 혜택 달성</h2>

      <div
        v-for="benefit in benefitProgress"
        :key="benefit.key"
        class="benefit-item"
      >
        <div class="benefit-title">
          <span> {{ benefit.name }} </span>

          <span> {{ benefit.rate }}% </span>
        </div>

        <div class="progress-bar">
          <div class="progress" :style="{ width: benefit.rate + '%' }" />
        </div>
      </div>
    </section>

    <!-- 카드이용내역 -->
    <section class="transaction-section">
      <h2>카드이용내역</h2>

      <div v-for="(transaction, index) in recentTransactions" :key="index" class="transaction-item">
        <span> {{ transaction.date }} </span>

        <span> {{ transaction.merchant }} </span>

        <span> {{ transaction.amount }} </span>
      </div>

      <button class="more-button" @click="goTransaction">+ 더보기</button>
    </section>

    <!-- 메모 추가/수정 모달 -->
    <div v-if="showEditModal" class="modal-overlay">
      <div class="edit-modal">
        <h3>메모</h3>

        <input
          v-model="editValue"
          placeholder="내용을 입력해주세요"
          maxlength="100"
        />

        <div class="input-info">
          <span
            :class="{ 'warning': editValue.length > 100 }"
          >
            {{ editValue.length }}/100
          </span>
        </div>

        <div class="modal-buttons">
          <button @click="closeEdit">취소</button>

          <button class="confirm" @click="saveEdit">저장</button>
        </div>
      </div>
    </div>

    <!-- 삭제 확인 -->
    <div v-if="showDeleteModal" class="modal-overlay">
      <div class="edit-modal">
        <h3>카드 삭제</h3>

        <p>이 카드를 삭제하시겠습니까?</p>

        <div class="modal-buttons">
          <button @click="closeDelete">취소</button>

          <button class="delete-confirm" @click="confirmDelete">삭제</button>
        </div>
      </div>
    </div>
  <BottomNavigation/>
  </div>
</template>

<style scoped>
/* ---------------- 공통 ---------------- */

.card-detail-page {
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg);
  padding: var(--space-md) 0;
}

/* ---------------- 카드 이미지 ---------------- */

.card-image-section {
  display: flex;
  justify-content: center;
  margin: var(--space-sm) 0 var(--space-xl);
  min-height: 176px;
}

.card-image-frame {
  width: min(100%, 286px);
  aspect-ratio: 1.586 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--radius-lg);
  filter: drop-shadow(0 12px 20px rgba(0, 0, 0, 0.14));
}

.card-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: var(--radius-lg);
}

.card-image.portrait {
  width: 176px;
  height: 286px;
  max-width: none;
  transform: rotate(90deg);
}

/* ---------------- 카드 정보 ---------------- */

.card-info {
  margin-bottom: var(--space-md);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.card-title-row {
  display: flex;
  align-items: stretch;
  gap: 2px;
  width: 100%;
}

.card-left {
  flex: 1;
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  gap: var(--space-xs);
}

.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: var(--space-xs);
  position: relative;
}

.card-divider {
  color: var(--color-border);
  font-size: var(--font-md);
  display: flex;
  align-items: center;
}

.card-name {
  margin: 0;
  font-weight: var(--font-semibold);
}

.alias-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  position: relative;
}

.card-info h1 {
  margin: 0;
  font-size: var(--font-md);
  color: var(--color-text-primary);
  font-weight: var(--font-bold);
  flex-shrink: 0;
}

.card-details {
  margin-top: 0;
  padding-top: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  text-align: center;
}

.company {
  display: none;
}

.company-display {
  color: var(--color-text-secondary);
  font-size: var(--font-md);
  font-weight: var(--font-semibold);
  white-space: nowrap;
}

.card-company {
  margin-top: var(--space-xs);
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
  white-space: nowrap;
}

.number {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
  white-space: nowrap;
}

/* ---------------- 메모 ---------------- */

.memo-display {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-text-secondary);
  font-size: var(--font-md);
  font-weight: var(--font-semibold);
  white-space: nowrap;
}

.delete-memo-btn {
  border: none;
  background: none;
  color: var(--color-text-tertiary);
  font-size: var(--font-lg);
  cursor: pointer;
  padding: 0;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-memo-btn:hover {
  color: var(--color-coral);
}

/* ---------------- 메모 (포스트잇) ---------------- */

.memo-box {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: var(--space-xs);
  max-width: calc(100vw - var(--space-md) * 2);

  padding: var(--space-xs);

  border-radius: var(--radius-sm);

  background: rgba(242, 193, 78, 0.15);

  border: 1px solid var(--color-gold-fill);

  color: var(--color-text-primary);

  font-size: var(--font-xs);

  line-height: 1.5;

  word-break: break-word;

  box-shadow: var(--shadow-card);

  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* ---------------- 메뉴 ---------------- */

.menu-wrapper {
  position: relative;
}

.menu-button {
  border: none;
  background: none;

  font-size: var(--font-2xl);

  cursor: pointer;
  color: var(--color-text-primary);
}

.menu-popover {
  position: absolute;

  top: 36px;
  right: 0;

  width: 150px;

  background: var(--color-surface);

  border-radius: var(--radius-sm);

  overflow: hidden;

  box-shadow: var(--shadow-card);

  z-index: var(--z-dropdown);
}

.menu-popover button {
  width: 100%;

  padding: var(--space-sm) var(--space-md);

  border: none;

  background: var(--color-surface);

  text-align: left;

  cursor: pointer;
  color: var(--color-text-primary);
}

.menu-popover button:hover {
  background: var(--color-bg);
}

.menu-popover .delete {
  color: var(--color-coral);
}

/* ---------------- 혜택 버튼 ---------------- */

.benefit-button {
  width: 100%;

  height: 48px;

  border: none;

  border-radius: var(--radius-sm);

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  margin-bottom: var(--space-xl);
  cursor: pointer;
}

/* ---------------- 혜택 ---------------- */

h2 {
  margin: 0 0 var(--space-md);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  letter-spacing: -0.3px;
}

.benefit-item {
  margin-bottom: var(--space-md);
}

.benefit-title {
  display: flex;

  justify-content: space-between;

  margin-bottom: var(--space-xs);
  font-size: var(--font-sm);
  color: var(--color-text-primary);
}

.progress-bar {
  height: 8px;

  background: var(--color-border);

  border-radius: var(--radius-sm);

  overflow: hidden;
}

.progress {
  height: 100%;

  background: var(--color-primary);
}

/* ---------------- 소비내역 ---------------- */

.transaction-section {
  margin-top: var(--space-2xl);
}

.transaction-item {
  display: flex;

  justify-content: space-between;

  padding: var(--space-sm) 0;

  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
}

.more-button {
  width: 100%;

  margin-top: var(--space-sm);

  border: none;

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  padding: var(--space-sm) var(--space-md);

  border-radius: var(--radius-sm);

  cursor: pointer;

  font-weight: var(--font-semibold);

  text-align: center;
}

/* ---------------- 모달 ---------------- */

.modal-overlay {
  position: fixed;
  inset: 0;
  width: 100%;
  max-width: 480px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0 auto;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
}

.edit-modal {
  width: 90%;
  max-width: 480px;
  background: var(--color-surface);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  box-sizing: border-box;
  max-height: 70vh;
  overflow-y: auto;
}

.edit-modal h3 {
  margin-bottom: var(--space-md);
  color: var(--color-text-primary);
  font-size: var(--font-md);
  font-weight: var(--font-semibold);
}

.edit-modal input {
  width: 100%;

  height: 48px;

  border: 1px solid var(--color-input-border);

  border-radius: var(--radius-sm);

  padding: 0 var(--space-sm);

  font-size: var(--font-sm);
  color: var(--color-text-primary);

  background: var(--color-surface);

  box-sizing: border-box;
}

.edit-modal input::placeholder {
  color: var(--color-text-tertiary);
}

.input-info {
  margin-top: var(--space-xs);
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
  text-align: right;
}

.input-info .warning {
  color: var(--color-coral);
  font-weight: var(--font-semibold);
}

.modal-buttons {
  display: flex;

  gap: var(--space-xs);

  margin-top: var(--space-md);
}

.modal-buttons button {
  flex: 1;

  height: 44px;

  border-radius: var(--radius-sm);

  font-weight: var(--font-semibold);
  cursor: pointer;
}

.modal-buttons button:not(.confirm):not(.delete-confirm) {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.confirm {
  border: none;
  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color: var(--color-btn-primary-text);
}

.delete-confirm {
  border: none;
  background: var(--color-coral);

  color: var(--color-btn-primary-text);
}
</style>
