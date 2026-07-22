<script setup>

import { ref } from 'vue'


// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'


// 삭제 팝업 상태
const showDeleteModal = ref(false)


// 삭제 버튼 클릭
const openDeleteModal = () => {

  showDeleteModal.value = true

}


// 팝업 닫기
const closeDeleteModal = () => {

  showDeleteModal.value = false

}


// 삭제 처리
const deleteMembership = () => {

  // 추후 백 연결
  // DELETE /api/user-memberships/{id}

  showDeleteModal.value = false

}


</script>



<template>

  <div class="membership-detail-page">


    <!-- 헤더 -->
    <PageHeader title="멤버십 상세" />



    <main class="content">


      <!-- 멤버십 이름 -->
      <section class="membership-title">

        <h1>
          CJ ONE
        </h1>

      </section>



      <!-- 등록 정보 -->
      <section class="info-section">

        <h2>
          등록 정보
        </h2>


        <p>
          CJ ONE
        </p>

      </section>




      <!-- 주요 사용처 -->
      <section class="info-section">

        <h2>
          주요 사용처
        </h2>


        <ul>

          <li>
            뚜레쥬르
          </li>

          <li>
            올리브영
          </li>

          <li>
            CGV
          </li>

        </ul>


      </section>




      <!-- 공식 사이트 -->
      <button class="official-button">

        공식 사이트 이동

      </button>



      <!-- 삭제 -->
      <button
  class="delete-button"
  @click="openDeleteModal"
>

  멤버십 삭제

</button>

    </main>



    <BottomNav />


  </div>

  <!-- 삭제 확인 팝업 -->
<div
  v-if="showDeleteModal"
  class="modal-overlay"
>

  <div class="modal">


    <h3>
      CJ ONE 멤버십을 삭제하시겠어요?
    </h3>


    <p>
      삭제하면 결제 추천에서 CJ ONE 혜택 안내를 받을 수 없습니다.
    </p>


    <div class="modal-buttons">

      <button
        class="cancel-button"
        @click="closeDeleteModal"
      >
        취소
      </button>


      <button
        class="confirm-button"
        @click="deleteMembership"
      >
        삭제
      </button>

    </div>


  </div>

</div>


</template>




<style scoped>

.membership-detail-page {

  min-height: 100vh;

  padding-bottom: 80px;

}



.content {

  padding: 20px;

}



.membership-title h1 {

  font-size: 24px;

  margin-bottom: 24px;

}



.info-section {

  margin-bottom: 28px;

}



.info-section h2 {

  font-size: 16px;

  margin-bottom: 12px;

}



.info-section p,
.info-section li {

  font-size: 15px;

  color: #333;

}



.official-button {

  width: 100%;

  height: 48px;

  border-radius: 12px;

  background: #1d4ed8;

  color: white;

}



.delete-button {

  width: 100%;

  height: 48px;

  margin-top: 12px;

  border-radius: 12px;

  background: white;

  color: #ef4444;

  border: 1px solid #ef4444;

}

.modal-overlay {

  position: fixed;

  inset: 0;

  background: rgba(0,0,0,0.4);

  display: flex;

  justify-content: center;

  align-items: center;

  z-index: 100;

}


.modal {

  width: calc(100% - 40px);

  background: white;

  border-radius: 16px;

  padding: 24px;

}


.modal h3 {

  font-size: 18px;

  margin-bottom: 12px;

}


.modal p {

  font-size: 14px;

  color: #666;

  line-height: 1.5;

  margin-bottom: 24px;

}


.modal-buttons {

  display: flex;

  gap: 12px;

}


.modal-buttons button {

  flex: 1;

  height: 44px;

  border-radius: 10px;

}


.cancel-button {

  background: #f3f4f6;

}


.confirm-button {

  background: #ef4444;

  color: white;

}


</style>