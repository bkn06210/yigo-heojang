<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTransactions } from '@/api/walletApi';
import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDatePickerBottomSheet from '@/components/transaction/TransactionDatePickerBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
import Icon from '@/components/common/Icon.vue';


const route = useRoute();
const router = useRouter();

// 카드 상세에서 들어온 경우에만 존재 — 있으면 해당 카드 소비내역만, 없으면 전체
const filterCardId = computed(() =>
  route.query.cardId ? Number(route.query.cardId) : null,
);

const filterCardName = computed(() => route.query.cardName || '');

// 조회조건 표시
const showFilter = ref(false);


// 날짜 선택 표시
const showDatePicker = ref(false);


// 상세 모달 표시
const showDetail = ref(false);


// 선택한 사용내역
const selectedTransaction = ref(null);


// 선택 날짜
const selectedDate = ref({
  startDate: '',
  endDate: '',
});

// 적용된 필터
const appliedFilter = ref({
  approval: '승인',
  cardType: '전체',
  card: '전체',
  cardId: null,
  category: '전체',
  categoryId: null,
  region: '전체',
  transactionType: '전체',
  period: '이번달',
  startDate: '',
  endDate: '',
});

// 서버에서 10건씩 끊어 받는다. 전체를 한 번에 받아 프론트에서 자르지 않는다 —
// 내역이 쌓일수록 첫 응답이 무거워지고 화면에 그리는 DOM도 같이 커진다.
const PAGE_SIZE = 10;

// 다음에 요청할 페이지 번호(0부터). 서버가 hasNext로 더 있는지 알려준다.
const nextPage = ref(0);
const hasNext = ref(false);

// 첫 조회와 추가 조회를 구분한다 — 추가 조회일 때만 목록 아래 "+ 더보기"가 뜬다.
const isLoading = ref(false);
const isFirstLoad = ref(true);

// 스크롤만으로 자동으로 이어붙이는 횟수. 이 횟수를 다 쓰면 "+ 더보기"를 눌러야 더 받는다.
// 끝없이 자동으로 붙으면 사용자가 목록의 끝을 못 만나고, 뒤로 갔다 돌아왔을 때
// 어디까지 봤는지도 알 수 없다. 처음 두 번만 자동으로 받아 스크롤을 편하게 하고,
// 그 다음부터는 사용자가 의도를 밝히게 한다.
const MAX_AUTO_LOADS = 2;
const autoLoadedCount = ref(0);

// 목록 맨 아래에 두는 감지용 요소. 이게 화면에 들어오면 다음 10건을 부른다.
const loadMoreAnchor = ref(null);
let observer = null;

// 자동 로드 횟수를 다 썼고 더 받을 게 남아 있으면 버튼을 보여준다.
const showLoadMoreButton = computed(
  () => hasNext.value && autoLoadedCount.value >= MAX_AUTO_LOADS,
);


// 조회조건에 띄울 카드 목록 — 회원이 실제로 등록한 보유카드(GET /api/user-cards)를 쓴다.
// 서버는 카드 종류를 CREDIT/CHECK로 주고 필터 UI는 한국어 라벨로 고른다.
// 여기서 라벨로 바꿔두면 아래 CARD_TYPE_PARAM이 그대로 서버 값으로 되돌린다.
const cardStore = useCardStore();

const CARD_TYPE_LABEL = {
  CREDIT: '신용카드',
  CHECK: '체크카드',
};

const cards = computed(() =>
  cardStore.cards.map((card) => ({
    id: card.id,
    name: card.name,
    // 모르는 종류를 그대로 넘기면 서버 조건이 어긋나므로 '전체'로 눕힌다.
    type: CARD_TYPE_LABEL[card.cardType] || '전체',
    imageUrl: card.image,
  })),
);


// 지금까지 받아온 내역이 순서대로 쌓인다. 필터·카드가 바뀌면 비우고 다시 받는다.
const transactions = ref([]);

// 보유카드 id → 마스킹된 카드번호(****-****-****-1015).
const maskedNumberByCardId = computed(
  () => new Map(cardStore.cards.map((card) => [Number(card.id), card.cardNumber || ''])),
);

// 화면에 그릴 내역. 카드번호 뒷자리를 여기서 붙인다.
//
// 소비내역 응답(TransactionResponse)에는 카드번호가 아예 없고 cardName만 있다.
// 예전에는 그 이름을 잘라 쓰고 있어서 "신한카드 핏(Fit)" → "본인 Fit*"처럼
// 엉뚱한 값이 찍혔다. 번호는 보유카드 목록에서 가져오는 게 맞다.
//
// 목록 조회와 보유카드 조회는 끝나는 순서가 정해져 있지 않으므로,
// 받아올 때 한 번 박아넣지 않고 그릴 때마다 다시 맞춘다.
const displayTransactions = computed(() =>
  transactions.value.map((transaction) => ({
    ...transaction,
    cardLastDigits: (maskedNumberByCardId.value.get(Number(transaction.cardId)) || '')
      .slice(-4),
  })),
);

// 화면의 필터 값을 서버 쿼리 파라미터로 옮긴다.
// 필터링을 프론트에서 하면 10건을 받아 걸러낸 뒤 3건만 남는 식이 되어
// "10건씩"이라는 약속이 깨지고, 다음 페이지 경계도 어긋난다.
const CARD_TYPE_PARAM = { 신용카드: 'CREDIT', 체크카드: 'CHECK' };
const TRANSACTION_TYPE_PARAM = { 일시불: 'LUMP_SUM', 할부: 'INSTALLMENT' };

// 화면은 2026.08.12, 서버는 2026-08-12 형식을 쓴다.
const toApiDate = (value) => (value ? value.replace(/\./g, '-') : undefined);

const buildQueryParams = () => {
  const filter = appliedFilter.value;

  const params = {};

  // 카드 상세에서 들어온 경우(route query)가 우선이다. 그 화면은 특정 카드의
  // 내역만 보여주기로 하고 들어온 것이라, 조회조건에서 다른 카드를 고를 수 없다.
  const selectedCardId = filterCardId.value ?? filter.cardId;

  if (selectedCardId != null) {
    params.userCardId = selectedCardId;
  }

  if (CARD_TYPE_PARAM[filter.cardType]) {
    params.cardType = CARD_TYPE_PARAM[filter.cardType];
  }

  if (filter.categoryId != null) {
    params.categoryId = filter.categoryId;
  }

  if (TRANSACTION_TYPE_PARAM[filter.transactionType]) {
    params.transactionType = TRANSACTION_TYPE_PARAM[filter.transactionType];
  }

  if (filter.startDate && filter.endDate) {
    params.startDate = toApiDate(filter.startDate);
    params.endDate = toApiDate(filter.endDate);
  }

  return params;
};


// 조회조건 열기
const openFilter = () => {
  showFilter.value = true;
};


// 조회조건 닫기
const closeFilter = () => {
  showFilter.value = false;
};


// 날짜 선택 열기
const openDatePicker = () => {
  showDatePicker.value = true;
};


// 날짜 선택 닫기
const closeDatePicker = () => {
  showDatePicker.value = false;
};


// 날짜 적용
const applyDate = (date) => {

  selectedDate.value = {
    startDate: date.startDate,
    endDate: date.endDate,
  };

  showDatePicker.value = false;

};


// 조회조건 적용 — 조건이 바뀌면 처음부터 다시 받는다.
const applyFilter = (filter) => {
  appliedFilter.value = filter;
  showFilter.value = false;
  reload();
};


// 사용내역 상세 열기
const openDetail = (transaction) => {

  selectedTransaction.value = transaction;

  showDetail.value = true;

};


// 사용내역 상세 닫기
const closeDetail = () => {

  showDetail.value = false;

  selectedTransaction.value = null;

};

// 현재 적용된 필터 태그
const activeFilters = computed(() => {
  const filters = [];

  if (appliedFilter.value.cardType !== '전체') {
    filters.push({ key: 'cardType', label: appliedFilter.value.cardType });
  }

  if (appliedFilter.value.transactionType !== '전체') {
    filters.push({ key: 'transactionType', label: appliedFilter.value.transactionType });
  }

  if (appliedFilter.value.categoryId != null) {
    filters.push({ key: 'category', label: appliedFilter.value.category });
  }

  // 카드 상세에서 들어왔을 때는 제목이 이미 카드명을 달고 있어 태그가 겹친다.
  if (!filterCardId.value && appliedFilter.value.cardId != null) {
    filters.push({ key: 'card', label: appliedFilter.value.card });
  }

  if (appliedFilter.value.startDate && appliedFilter.value.endDate) {
    filters.push({ key: 'date', label: `${appliedFilter.value.startDate} ~ ${appliedFilter.value.endDate}` });
  }

  return filters;
});

// 필터 제거
const removeFilter = (filterKey) => {
  if (filterKey === 'cardType') {
    appliedFilter.value.cardType = '전체';
  } else if (filterKey === 'transactionType') {
    appliedFilter.value.transactionType = '전체';
  } else if (filterKey === 'category') {
    appliedFilter.value.category = '전체';
    appliedFilter.value.categoryId = null;
  } else if (filterKey === 'card') {
    appliedFilter.value.card = '전체';
    appliedFilter.value.cardId = null;
    appliedFilter.value.cardType = '전체';
  } else if (filterKey === 'date') {
    appliedFilter.value.startDate = '';
    appliedFilter.value.endDate = '';
  }
  reload();
};

// 카드타입 변환 — 서버(card.card_type)는 CREDIT/CHECK를 준다.
// DEBIT은 예전 표기라 남겨두고, 모르는 값은 원문 그대로 보여준다.
const cardTypeMap = {
  CREDIT: '신용',
  CHECK: '체크',
  DEBIT: '체크',
};

// 거래 구분 변환
const transactionTypeMap = {
  LUMP_SUM: '일시불',
  INSTALLMENT: '할부',
};

// 거래 상태 변환
const paymentStatusMap = {
  APPROVED: '승인',
  PENDING: '대기 중',
  CANCELLED: '취소됨',
  FAILED: '실패',
};

// 날짜 포맷팅 (YYYY.MM.DD HH:MM)
const formatDate = (dateStr) => {
  const d = new Date(dateStr);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hours}:${minutes}`;
};

const toViewModel = (t) => ({
  id: t.expenseId,
  cardId: t.userCardId,
  merchant: t.merchantName || t.categoryName,
  amount: t.paymentAmount,
  date: formatDate(t.paymentDate),
  category: t.categoryName,
  // 상세 바텀시트가 카드명을 그대로 보여준다. 안 넘기면 "이용카드"가 빈칸으로 뜬다.
  cardName: t.cardName,
  cardType: cardTypeMap[t.cardType] || t.cardType,
  installment: transactionTypeMap[t.transactionType] || t.transactionType,
  approvalNumber: String(t.expenseId).padStart(8, '0'),
  status: paymentStatusMap[t.paymentStatus] || t.paymentStatus,
});

// 다음 10건을 받아 목록 뒤에 붙인다.
const loadNextPage = async () => {
  // 이미 요청이 떠 있거나 더 받을 게 없으면 아무것도 하지 않는다.
  // 감시자가 스크롤 중 여러 번 불릴 수 있어 이 가드가 없으면 같은 페이지를 중복 요청한다.
  if (isLoading.value || (!hasNext.value && !isFirstLoad.value)) {
    return;
  }

  isLoading.value = true;

  try {
    const response = await getTransactions({
      ...buildQueryParams(),
      page: nextPage.value,
      size: PAGE_SIZE,
    });

    transactions.value.push(...(response?.transactions || []).map(toViewModel));
    hasNext.value = Boolean(response?.hasNext);
    nextPage.value += 1;
  } catch (error) {
    console.error('사용내역 조회 실패:', error);
    // 실패한 페이지를 자동으로 계속 재요청하면 에러가 무한히 반복된다. 여기서 멈춘다.
    hasNext.value = false;
  } finally {
    isLoading.value = false;
    isFirstLoad.value = false;
  }
};

// 조건이 바뀌었을 때 처음부터 다시 받는다.
const reload = async () => {
  transactions.value = [];
  nextPage.value = 0;
  hasNext.value = false;
  isFirstLoad.value = true;
  // 목록이 새로 시작되므로 자동 로드 횟수도 되돌린다.
  autoLoadedCount.value = 0;
  await loadNextPage();
};

onMounted(async () => {
  // 조회조건에 띄울 보유카드. 다른 화면에서 이미 받아뒀으면 다시 부르지 않는다.
  // 실패해도 내역 조회 자체는 막지 않는다 — 카드 목록은 필터의 보조 정보다.
  if (!cardStore.cards.length) {
    cardStore.loadCards().catch((error) => {
      console.error('보유카드 목록 조회 실패:', error);
    });
  }

  await reload();

  // 목록 끝의 감지용 요소가 화면에 들어오면 다음 페이지를 부른다.
  // rootMargin을 200px 줘서 바닥에 완전히 닿기 전에 미리 받아두면 끊김이 덜하다.
  observer = new IntersectionObserver(
    (entries) => {
      if (!entries[0].isIntersecting) {
        return;
      }

      // 자동 로드 횟수를 다 썼으면 감지되어도 받지 않는다 — 대신 버튼이 떠 있다.
      if (autoLoadedCount.value >= MAX_AUTO_LOADS) {
        return;
      }

      autoLoadedCount.value += 1;
      loadNextPage();
    },
    { rootMargin: '200px' },
  );

  if (loadMoreAnchor.value) {
    observer.observe(loadMoreAnchor.value);
  }
});

onBeforeUnmount(() => {
  // 화면을 떠난 뒤에도 감시자가 남아 있으면 사라진 요소를 계속 붙들고 있는다.
  observer?.disconnect();
});

// 쿼리 파라미터(카드 선택) 변경 시 다시 로드
watch(() => route.query.cardId, () => {
  reload();
});

</script>


<template>

<div class="transaction-page">


  <div class="header">

    <PageHeader
      :title="filterCardName ? `${filterCardName} 이용내역` : '카드이용내역'"
      @back="router.back()"
    />


    <button
      class="filter-icon"
      @click="openFilter"
    >
      <Icon name="filter" size="sm" />
    </button>

  </div>

  <!-- 적용된 필터 태그 -->
  <div v-if="activeFilters.length > 0" class="filter-tags">
    <span
      v-for="filter in activeFilters"
      :key="filter.key"
      class="filter-tag"
    >
      {{ filter.label }}
      <button
        class="remove-filter-btn"
        @click="removeFilter(filter.key)"
        type="button"
      >
        ×
      </button>
    </span>
  </div>




  <!-- 사용내역 리스트 -->

  <div
  v-for="transaction in displayTransactions"
  :key="transaction.id"
  class="transaction-item"
  @click="openDetail(transaction)"
>

    <!-- 매장명과 금액 (한 줄) -->
    <div class="transaction-header">
      <div class="merchant-title">
        {{ transaction.merchant }}
      </div>
      <span class="amount">
        {{ transaction.amount.toLocaleString() }}원
      </span>
    </div>

    <!-- 상세 정보 (작음) -->
    <div class="transaction-info">
      <span class="info-text">{{ transaction.date }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">본인 {{ transaction.cardLastDigits.slice(0, 3) }}*</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.category }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.installment }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.cardType }}</span>
    </div>


  </div>

  <!-- 내역 없음 -->
  <p
    v-if="!isLoading && !transactions.length"
    class="list-state"
  >
    조회된 이용내역이 없어요.
  </p>

  <!--
    목록 끝 감지용 요소. 화면에 들어오면 다음 10건을 부른다.
    v-if로 지웠다 그렸다 하지 않고 항상 둔다 — 최초 렌더 때 요소가 없으면
    onMounted에서 감시를 걸 대상이 없어 자동 로드가 아예 시작되지 않는다.
  -->
  <div
    ref="loadMoreAnchor"
    class="load-more"
  >
    <!-- 자동으로 받는 중에도 빈칸을 두지 않는다. 뭐가 일어나는지 보여야 한다. -->
    <span
      v-if="isLoading && !isFirstLoad"
      class="load-more-text"
    >
      불러오는 중…
    </span>

    <!-- 자동 로드 횟수를 다 쓴 뒤부터는 눌러야 더 받는다 -->
    <button
      v-else-if="showLoadMoreButton"
      type="button"
      class="load-more-button"
      @click="loadNextPage"
    >
      + 더보기
    </button>
  </div>






  <!-- 조회조건 -->

  <TransactionFilterBottomSheet

    v-if="showFilter"

    :cards="cards"

    :selectedDate="selectedDate"

    @close="closeFilter"

    @apply="applyFilter"

    @open-date-picker="openDatePicker"

  />





  <!-- 날짜 선택 -->

  <TransactionDatePickerBottomSheet

    v-if="showDatePicker"

    @close="closeDatePicker"

    @apply="applyDate"

  />





  <!-- 상세 -->

  <TransactionDetailBottomSheet

  v-if="showDetail && selectedTransaction"

  :transaction="selectedTransaction"

  @close="closeDetail"

/>

</div>

</template>


<style scoped>

.transaction-page {
  padding: var(--space-md);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;
  overflow: hidden visible;
}


.header {

display:flex;
justify-content:space-between;
align-items:center;

}


.filter-icon {

width:36px;
height:36px;
border:none;
background:none;
font-size: var(--font-lg);
cursor: pointer;
color: var(--color-text-primary);

}



.transaction-item {
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.transaction-item:first-of-type {
  margin-top: var(--space-md);
}

.transaction-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
}

.merchant-title {
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
  flex: 1;
}

.amount {
  color: var(--color-coral);
  font-weight: var(--font-bold);
  font-size: var(--font-md);
  flex-shrink: 0;
  white-space: nowrap;
}

.transaction-info {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-sm);
  color: var(--color-text-tertiary);
  flex-wrap: wrap;
}

.info-text {
  color: var(--color-text-secondary);
}

.info-dot {
  color: var(--color-text-tertiary);
}

/* 목록 끝 감지 영역. 불러오는 중이거나 "+ 더보기"가 필요할 때만 내용이 찬다. */
.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 44px;
  margin-bottom: var(--space-xl);
}

.load-more-text {
  color: var(--color-text-tertiary);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
}

.load-more-button {
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.load-more-button:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.list-state {
  padding: var(--space-2xl) 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  margin-top: var(--space-md);
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: 6px 12px;
  background: var(--color-point-bg);
  border-radius: var(--radius-full);
  font-size: var(--font-sm);
  color: var(--color-text-primary);
}

.remove-filter-btn {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 18px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-fast);
}

.remove-filter-btn:hover {
  color: var(--color-coral);
}

</style>