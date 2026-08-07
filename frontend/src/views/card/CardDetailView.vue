<script setup>

import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { useToast } from '@/composables/useToast';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCardStore } from '@/stores/cardStore';
import { getCardMonthlyStatus } from '@/api/walletApi';

import PageHeader from '@/components/common/PageHeader.vue';

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

// PR #34 연동: 중복 삭제 요청을 막고 처리 중 상태를 버튼에 표시한다.
const deleteLoading = ref(false);

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

// PR #34 연동: DELETE /api/user-cards/{userCardId}가 204를 반환한 뒤 목록으로 이동한다.
const confirmDelete = async () => {
  const cardId = Number(route.params.id);
  deleteLoading.value = true;
  try {
    await cardStore.removeCard(cardId);
    showDeleteModal.value = false;
    alert('카드가 삭제되었습니다.');
    router.push('/cards');
  } catch (error) {
    alert(error?.response?.data?.message || error?.message || '카드를 삭제하지 못했습니다.');
  } finally {
    deleteLoading.value = false;
  }
};

// 임시 데이터
// 추후 카드 상세 API 연결
const card = {
const card = ref({
  id: route.params.id,

  name: 'Deep Dream (체크)',

  company: '신한카드',

  owner: '본인',

  cardNumber: '1234567890127034',

  image: '/images/cards/shinhan.png',
};
});

// PR #25 연동: 선택한 카드의 월 실적과 혜택 사용 현황을 서버 응답으로 보관한다.
const cardStatus = ref(null);
const statusError = ref('');

// PR #25 연동: API 혜택 배열을 기존 진행률 UI에서 바로 사용할 수 있게 정규화한다.
const benefitStatuses = computed(() => (cardStatus.value?.benefits || []).map((benefit) => ({
  ...benefit,
  usagePercent: benefit.usageRate == null ? 0 : Number(benefit.usageRate),
})));

// PR #25 연동: GET /api/cards/{userCardId}/monthly-status로 카드 상세 현황을 조회한다.
onMounted(async () => {
  try {
    cardStatus.value = await getCardMonthlyStatus(Number(route.params.id));
    card.value = {
      ...card.value,
      id: cardStatus.value.userCardId,
      name: cardStatus.value.cardName,
    };
  } catch (error) {
    statusError.value = error?.response?.data?.message || error?.message || '카드 현황을 불러오지 못했습니다.';
  }
});

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
    query: { cardId: card.id, cardName: card.name },
  });
  router.push(`/benefits/${card.value.id}`);
};

// 소비내역 이동
const goTransaction = () => {
  router.push('/transactions');
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
  <PageHeader title="카드 상세" />

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

  <div class="card-title-row">

    <!-- 왼쪽 카드 정보 -->
    <div class="title-area">

      <div class="name-row">

        <h1>
          {{ card.name }}
        </h1>


        <button
          class="alias-tag"
          @click="toggleMemo"
        >
          {{ cardAlias }}
        </button>

      </div>



      <div class="info-row">

        <div class="left-info">

          <p class="company">
            {{ card.company }}
          </p>


          <p class="number">

            {{ card.owner }}

            {{ maskCardNumber(card.cardNumber) }}

          </p>

        </div>



        <!-- 별칭 아래 메모 -->
        <div
          v-if="showMemo"
          class="memo-box"
        >
          {{ cardMemo }}
        </div>


      </div>


    </div>

  </div>

  <div class="card-details">

    <p class="company">
      {{ card.company }}
    </p>

    <p class="number">
      {{ card.owner }} {{ maskCardNumber(card.cardNumber) }}
    </p>

  </div>

</section>

    <!-- 혜택 상세 -->
</section>

    <!-- 혜택 상세 -->
    <!-- PR #25 연동: API가 계산한 카드 실적과 혜택별 사용률을 상세 화면에 표시한다. -->
    <section v-if="cardStatus" class="benefit-progress">
      <h2>이번 달 카드 현황</h2>
      <p>
        실적 {{ Number(cardStatus.currentPerformanceAmount).toLocaleString() }}원 /
        {{ Number(cardStatus.targetPerformance).toLocaleString() }}원
      </p>
      <p>
        {{ cardStatus.achievementRate == null ? '실적 조건 없음' : `달성률 ${cardStatus.achievementRate}%` }}
      </p>

      <!-- PR #25 연동: 한도 없는 혜택은 퍼센트 대신 '한도 없음'으로 구분한다. -->
      <div v-for="benefit in benefitStatuses" :key="benefit.benefitId" class="benefit-item">
        <div class="benefit-title">
          <span>{{ benefit.benefitName }}</span>
          <span>{{ benefit.usageRate == null ? '한도 없음' : `${benefit.usagePercent}%` }}</span>
        </div>
        <div v-if="benefit.usageRate != null" class="progress-bar">
          <div class="progress" :style="{ width: `${benefit.usagePercent}%` }" />
        </div>
      </div>
    </section>

    <!-- PR #25 연동: 조회 실패 시 서버 오류 메시지를 표시한다. -->
    <p v-if="statusError" class="error">{{ statusError }}</p>

    <button class="benefit-button" @click="goBenefitDetail">
      혜택 자세히 보기
    </button>

    <!-- 혜택 달성 -->
    <section class="benefit-progress">
    <section v-if="!cardStatus" class="benefit-progress">
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

    <!-- 최근 소비내역 -->
    <section class="transaction-section">
      <h2>최근 소비내역</h2>

      <div class="transaction-item">
        <span> 07.28 </span>

        <span> 스타벅스 </span>

        <span> -6,200원 </span>
      </div>

      <div class="transaction-item">
        <span> 07.27 </span>

        <span> CU </span>

        <span> -4,500원 </span>
      </div>

      <div class="transaction-item">
        <span> 07.26 </span>

        <span> 올리브영 </span>

        <span> -28,900원 </span>
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
          {{ editType === 'alias' ? '별칭 수정' : '메모 수정' }}
        </h3>

        <input v-model="editValue" placeholder="내용을 입력해주세요" />

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
          <!-- PR #34 연동: 삭제 API 처리 중에는 재클릭을 차단한다. -->
          <button class="delete-confirm" :disabled="deleteLoading" @click="confirmDelete">
            {{ deleteLoading ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>
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
  padding: 20px;
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
  margin-bottom: 20px;
}

.card-image {
  width: 220px;
  border-radius: var(--radius-md);
  border-radius: 16px;
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
  margin-bottom: 20px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-area {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
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
  font-size: 22px;
}

.company {
  margin-top: 8px;
  color: #666;
  font-size: 14px;
}

.number {
  margin-top: 12px;
  color: #555;
  font-size: 14px;
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
  background: #f3f4f6;

  color: #555;

  border-radius: 999px;

  padding: 5px 12px;

  font-size: 12px;

  cursor: pointer;

  display: flex;
  align-items: center;
  gap: 4px;
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
  margin-top: 8px;
  max-width: 180px;

  padding: 8px 10px;

  border-radius: 10px;

  background: #fff8bf;

  border: 1px solid #f5df6d;

  color: #444;

  font-size: 12px;

  line-height: 1.5;

  word-break: break-word;

  box-shadow: var(--shadow-card);

  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0,0,0,.08);
}

.left-info {
  display: flex;
  flex-direction: column;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  font-size: 28px;

  cursor: pointer;
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
  background: #fff;

  border-radius: 12px;

  overflow: hidden;

  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);

  z-index: 100;
}

.menu-popover button {
  width: 100%;

  padding: var(--space-sm) var(--space-md);

  border: none;

  background: var(--color-surface);
  padding: 14px 16px;

  border: none;

  background: white;

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

.menu-popover button:hover {
  background: #f5f5f5;
}

.menu-popover .delete {
  color: #ef4444;
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
  border-radius: 12px;

  background: #4f46e5;

  color: white;

  font-size: 15px;

  margin-bottom: 28px;
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
  font-size: 18px;

  margin-bottom: 16px;
}

.benefit-item {
  margin-bottom: 18px;
}

.benefit-title {
  display: flex;

  justify-content: space-between;

  margin-bottom: var(--space-xs);
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.progress-bar {
  height: 8px;

  background: var(--color-border);

  border-radius: var(--radius-sm);
  background: #eee;

  border-radius: 10px;

  overflow: hidden;
}

.progress {
  height: 100%;

  background: var(--color-primary);
  background: #4f46e5;
}

/* ---------------- 소비내역 ---------------- */

.transaction-section {
  margin-top: var(--space-2xl);
  margin-top: 32px;
}

.transaction-item {
  display: flex;

  justify-content: space-between;

  padding: var(--space-sm) 0;

  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  padding: 12px 0;

  border-bottom: 1px solid #eee;
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
  margin-top: 12px;

  border: none;

  background: none;

  padding: 12px;

  cursor: pointer;
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
  z-index: 3000;
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
  background: white;

  border-radius: 20px;

  padding: 24px;
}

.edit-modal h3 {
  margin-bottom: 20px;
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
  border: 1px solid #ddd;

  border-radius: 12px;

  padding: 0 12px;

  font-size: 15px;
}

.modal-buttons {
  display: flex;

  gap: var(--space-xs);

  margin-top: var(--space-md);
  gap: 10px;

  margin-top: 20px;
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
  border: none; 

  border-radius: 12px;

  background: #eee;
}

.confirm {
  background: #4f46e5 !important;

  color: white;
}

.delete-confirm {
  background: #ef4444 !important;

  color: white;
}
</style>
<!-- 07_25 연동 변경: 카드 상세·실적·혜택 API 응답을 기존 UI에 표시한다. -->
