<script setup>

import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCardStore } from '@/stores/cardStore';
import { getUserCardDetail } from '@/api/cardApi';
import { getTransactions } from '@/api/walletApi';

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

// 수정 대상
const editType = ref('');

// 카드 별칭
const cardAlias = ref('월급 생활비 카드');

// 카드 메모
const cardMemo = ref('카페 할인용으로 사용하는 카드');

// 입력 임시 값
const editValue = ref('');

// 메뉴 열기
const openMenu = () => {
  showMenu.value = !showMenu.value;
};

// 별칭 수정 열기
const editAlias = () => {
  editType.value = 'alias';

  editValue.value = cardAlias.value;

  showEditModal.value = true;

  showMenu.value = false;
};

// 메모 수정 열기
const editMemo = () => {
  editType.value = 'memo';

  editValue.value = cardMemo.value;

  showEditModal.value = true;

  showMenu.value = false;
};

// 수정 저장
const saveEdit = () => {
  if (editType.value === 'alias') {
    cardAlias.value = editValue.value;
  }

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

const loadCard = async () => {
  try {
    const data = await getUserCardDetail(route.params.id);
    card.value = { ...card.value, ...data };

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
};

onMounted(loadCard);

// 카드번호 표시
const maskCardNumber = (number) => {
  if (!number) return '';

  const lastFour = number.slice(-4);

  return `${lastFour.slice(0, 3)}*`;
};

// 혜택 상세 이동
const goBenefitDetail = () => {
  router.push(`/benefits/${card.id}`);
};

// 소비내역 이동 (이 카드의 소비내역만 필터링해서 보여줌)
const goTransaction = () => {
  router.push({
    path: '/transactions',
    query: { cardId: card.value.userCardId, cardName: card.value.name },
  });
};

// 메모 표시 여부
const showMemo = ref(false);

// 별칭 클릭 시 메모 열기/닫기
const toggleMemo = () => {
  showMemo.value = !showMemo.value;
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

      <button @click="editAlias">
        별칭 수정
      </button>


      <button @click="editMemo">
        메모 수정
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
      <img :src="card.image" :alt="card.name" class="card-image" />
    </section>

    <!-- 카드 정보 -->
<section class="card-info">

  <div class="card-header">

    <h1>
      {{ card.name }}
    </h1>

    <div class="alias-section">

      <button
        class="alias-tag"
        @click="toggleMemo"
      >
        {{ cardAlias }}
      </button>

      <!-- 별칭 아래 메모 -->
      <div
        v-if="showMemo"
        class="memo-box"
      >
        {{ cardMemo }}
      </div>

    </div>

  </div>

  <div class="card-details">

    <p class="company">
      {{ card.company }}
    </p>

    <p class="number">
      {{ card.owner }} {{ maskCardNumber(card.maskedCardNumber) }}
    </p>

  </div>

</section>

    <!-- 혜택 상세 -->
    <button class="benefit-button" @click="goBenefitDetail">
      혜택 자세히 보기
    </button>

    <!-- 혜택 달성 -->
    <section class="benefit-progress">
      <h2>주요 혜택 달성</h2>

      <div class="benefit-item">
        <div class="benefit-title">
          <span> 스타벅스 </span>

          <span> 80% </span>
        </div>

        <div class="progress-bar">
          <div class="progress" style="width: 80%" />
        </div>
      </div>

      <div class="benefit-item">
        <div class="benefit-title">
          <span> 편의점 </span>

          <span> 60% </span>
        </div>

        <div class="progress-bar">
          <div class="progress" style="width: 60%" />
        </div>
      </div>

      <div class="benefit-item">
        <div class="benefit-title">
          <span> 온라인쇼핑 </span>

          <span> 40% </span>
        </div>

        <div class="progress-bar">
          <div class="progress" style="width: 40%" />
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

    <!-- 별칭/메모 수정 모달 -->
    <div v-if="showEditModal" class="modal-overlay">
      <div class="edit-modal">
        <h3>
          {{ editType === 'alias' ? '별칭' : '메모' }}
        </h3>

        <input
          v-model="editValue"
          placeholder="내용을 입력해주세요"
          :maxlength="editType === 'memo' ? 25 : editType === 'alias' ? 15 : undefined"
        />

        <div v-if="editType === 'memo'" class="input-info">
          <span
            :class="{ 'warning': editValue.length > 25 }"
          >
            {{ editValue.length }}/25
          </span>
        </div>

        <div v-if="editType === 'alias'" class="input-info">
          <span
            :class="{ 'warning': editValue.length > 15 }"
          >
            {{ editValue.length }}/15
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
  margin-bottom: var(--space-md);
}

.card-image {
  width: 220px;
  border-radius: var(--radius-md);
}

/* ---------------- 카드 정보 ---------------- */

.card-info {
  margin-bottom: var(--space-md);
}

.card-header {
  display: flex;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.alias-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  position: relative;
}

.card-info h1 {
  margin: 0;
  font-size: var(--font-xl);
  color: var(--color-text-primary);
  font-weight: var(--font-bold);
  flex-shrink: 0;
}

.card-details {
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  display: flex;
  gap: var(--space-sm);
  align-items: center;
}

.company {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

.number {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

/* ---------------- 별칭 ---------------- */

.alias-tag {
  border: none;
  background: var(--color-bg);

  color: var(--color-text-secondary);

  border-radius: var(--radius-full);

  padding: var(--space-xxs) var(--space-sm);

  font-size: var(--font-xs);

  cursor: pointer;

  display: inline-block;
  white-space: nowrap;
}

.arrow {
  font-size: 10px;
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

  background: rgba(0, 0, 0, 0.4);

  display: flex;

  align-items: center;

  justify-content: center;

  z-index: var(--z-modal);
}

.edit-modal {
  width: 85%;

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-xl);
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
