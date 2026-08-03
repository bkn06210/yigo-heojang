<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';

import NotificationModal from '@/components/notification/NotificationModal.vue';
  
// 뒤로가기용
const router = useRouter();


// 알림 데이터
// TODO: GET /notifications API 연결 예정
const notifications = ref([

  {
    id: 1,

    title: '카드 사용 한도 초과',

    // 목록에서 보여줄 짧은 내용
    preview:
      '이번 달 카드 사용 금액이 한도를 초과했습니다.',

    // 상세 모달 내용
    detail: {
      cardName: '신한 Mr.Life',
      usedAmount: 520000,
      limitAmount: 500000,
      message:
        '설정한 카드 한도를 초과했습니다. 소비 내역을 확인하고 관리해보세요.',
    },

    createdAt: '10분 전',

    isRead: false,

  },


  {
    id: 2,

    title: 'AI 소비 분석 완료',

    preview:
      '이번 달 소비 패턴 분석 결과가 생성되었습니다.',

    detail: {
      summary:
        '이번 달 구독 서비스 이용 비중이 높아요.',
      recommendation:
        '사용하지 않는 구독 서비스를 확인해보세요.',
    },

    createdAt: '어제',

    isRead: true,

  },


  {
    id: 3,

    title: '결제 취소 완료',

    preview:
      '결제 취소 처리가 완료되었습니다.',

    detail: {
      storeName: '올리브영',
      amount: 32000,
      message:
        '취소 금액이 카드 승인 취소 처리되었습니다.',
    },

    createdAt: '7월 29일',

    isRead: false,

  },

]);
const selectedNotification = ref(null);

const openNotification = (item) => {
  selectedNotification.value = item;
};




// 읽지 않은 알림 개수
const unreadCount = computed(() => {

  return notifications.value.filter(
    item => !item.isRead
  ).length;

});



// 뒤로가기
const goBack = () => {

  router.back();

};



// 알림 클릭
// 읽음 처리
const readNotification = (id) => {

  const target = notifications.value.find(
    item => item.id === id
  );


  if(target){

    target.isRead = true;

  }

};


// 모두 읽음
const readAll = () => {

  notifications.value.forEach(item => {

    item.isRead = true;

  });

};



// 전체 삭제
const deleteAll = () => {

  notifications.value = [];

};



// 개별 삭제
const deleteNotification = (id) => {

  notifications.value =
    notifications.value.filter(
      item => item.id !== id
    );

};

</script>



<template>

<div class="notification-view">


  <!-- 헤더 -->
  <header class="notification-header">

    <button
      type="button"
      class="back-button"
      @click="goBack"
    >
      ←
    </button>


    <h1>
      알림
    </h1>


    <span
      v-if="unreadCount"
      class="count"
    >
      {{ unreadCount }}
    </span>

  </header>




  <!-- 알림 리스트 -->
  <main class="notification-list">


    <div
      v-if="notifications.length === 0"
      class="empty"
    >
      새로운 알림이 없습니다.
    </div>



    <article
      v-for="item in notifications"
      :key="item.id"
      class="notification-item"
      :class="{
        unread: !item.isRead
      }"

      @click="openNotification(item)"
    >


      <div class="notification-content">


        <h2>
          {{ item.title }}
        </h2>


        <p>
          {{ item.message }}
        </p>


        <span>
          {{ item.createdAt }}
        </span>


      </div>



      <!-- 삭제 버튼 -->
      <button
        type="button"
        class="delete-button"
        @click.stop="deleteNotification(item.id)"
      >
        ×
      </button>


    </article>


  </main>




  <!-- 하단 버튼 -->
  <footer
    v-if="notifications.length"
    class="notification-actions"
  >

    <button
      type="button"
      @click="readAll"
    >
      모두 읽음
    </button>


    <button
      type="button"
      @click="deleteAll"
    >
      전체 삭제
    </button>


  </footer>

  <NotificationModal
  v-if="selectedNotification"
  :notification="selectedNotification"
  @close="selectedNotification=null"
/>

<NotificationModal

  v-if="selectedNotification"

  :notification="selectedNotification"

  @close="selectedNotification = null"

  @read="readNotification"

/>



</div>

</template>



<style scoped>

.notification-view {

  min-height:100vh;

  background:#fafafa;

  padding:20px;

  padding-bottom:100px;

}



.notification-header {

  display:flex;

  align-items:center;

  gap:12px;

  margin-bottom:24px;

}



.notification-header h1 {

  font-size:22px;

  font-weight:700;

}



.back-button {

  border:none;

  background:none;

  font-size:24px;

  cursor:pointer;

}



.count {

  display:flex;

  align-items:center;

  justify-content:center;

  width:22px;

  height:22px;

  border-radius:50%;

  background:#ff4d4f;

  color:white;

  font-size:12px;

}



.notification-list {

  display:flex;

  flex-direction:column;

  gap:12px;

}



.notification-item {

  display:flex;

  justify-content:space-between;

  align-items:flex-start;

  padding:18px;

  background:white;

  border-radius:16px;

  color:#999;

  transition:.2s;

}



.notification-item.unread {

  color:#222;

  font-weight:600;

}



.notification-content h2 {

  margin:0 0 8px;

  font-size:16px;

}



.notification-content p {

  margin:0 0 10px;

  font-size:14px;

  font-weight:400;

}



.notification-content span {

  font-size:12px;

  color:#aaa;

}



.delete-button {

  border:none;

  background:none;

  font-size:22px;

  color:#aaa;

  cursor:pointer;

}



.notification-actions {

  position:fixed;

  right:20px;

  bottom:30px;

  display:flex;

  flex-direction:column;

  gap:10px;

}



.notification-actions button {

  padding:12px 18px;

  border:none;

  border-radius:20px;

  background:#222;

  color:white;

  cursor:pointer;

}



.empty {

  text-align:center;

  color:#999;

  padding:60px 0;

}

</style>
