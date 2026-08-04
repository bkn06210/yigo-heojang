<script setup>

import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';

const cardStore = useCardStore();

const route = useRoute();

// 라우터
const router = useRouter();

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


  alert('카드가 삭제되었습니다');


  router.push('/cards');

};

// 임시 데이터
// 추후 카드 상세 API 연결
const card = {
  id: route.params.id,

  name: 'Deep Dream (체크)',

  company: '신한카드',

  owner: '본인',

  cardNumber: '1234567890127034',

  image: '/images/cards/shinhan.png',
};

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
  </div>
</template>

<style scoped>
/* ---------------- 공통 ---------------- */

.card-detail-page {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* ---------------- 카드 이미지 ---------------- */

.card-image-section {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.card-image {
  width: 220px;
  border-radius: 16px;
}

/* ---------------- 카드 정보 ---------------- */

.card-info {
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

  font-size: 28px;

  cursor: pointer;
}

.menu-popover {
  position: absolute;

  top: 36px;
  right: 0;

  width: 150px;

  background: #fff;

  border-radius: 12px;

  overflow: hidden;

  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);

  z-index: 100;
}

.menu-popover button {
  width: 100%;

  padding: 14px 16px;

  border: none;

  background: white;

  text-align: left;

  cursor: pointer;
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

  border-radius: 12px;

  background: #4f46e5;

  color: white;

  font-size: 15px;

  margin-bottom: 28px;
}

/* ---------------- 혜택 ---------------- */

h2 {
  font-size: 18px;

  margin-bottom: 16px;
}

.benefit-item {
  margin-bottom: 18px;
}

.benefit-title {
  display: flex;

  justify-content: space-between;

  margin-bottom: 8px;
}

.progress-bar {
  height: 8px;

  background: #eee;

  border-radius: 10px;

  overflow: hidden;
}

.progress {
  height: 100%;

  background: #4f46e5;
}

/* ---------------- 소비내역 ---------------- */

.transaction-section {
  margin-top: 32px;
}

.transaction-item {
  display: flex;

  justify-content: space-between;

  padding: 12px 0;

  border-bottom: 1px solid #eee;
}

.more-button {
  width: 100%;

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

  z-index: 3000;
}

.edit-modal {
  width: 85%;

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

  border: 1px solid #ddd;

  border-radius: 12px;

  padding: 0 12px;

  font-size: 15px;
}

.modal-buttons {
  display: flex;

  gap: 10px;

  margin-top: 20px;
}

.modal-buttons button {
  flex: 1;

  height: 44px;

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
