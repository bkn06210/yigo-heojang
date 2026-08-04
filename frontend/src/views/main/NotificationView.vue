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

  background: var(--color-bg);

  padding: var(--space-md);

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

}



.notification-header {

  display:flex;

  align-items:center;

  gap: var(--space-sm);

  margin-bottom: var(--space-xl);

}



.notification-header h1 {

  font-size: var(--font-title);

  font-weight: var(--font-bold);

}



.back-button {

  border:none;

  background:none;

  font-size: var(--font-2xl);

  cursor:pointer;

  color: var(--color-text-primary);

}



.count {

  display:flex;

  align-items:center;

  justify-content:center;

  width:24px;

  height:24px;

  border-radius: var(--radius-full);

  background: var(--color-coral);

  color: var(--color-btn-primary-text);

  font-size: var(--font-xs);

}



.notification-list {

  display:flex;

  flex-direction:column;

  gap: var(--space-sm);

}



.notification-item {

  display:flex;

  justify-content:space-between;

  align-items:flex-start;

  padding: var(--space-md);

  background: var(--color-surface);

  border-radius: var(--radius-md);

  color: var(--color-text-secondary);

  transition: var(--transition-normal);

}



.notification-item.unread {

  color: var(--color-text-primary);

  font-weight: var(--font-semibold);

}



.notification-content h2 {

  margin: 0 0 var(--space-xs);

  font-size: var(--font-md);

  font-weight: var(--font-semibold);

}



.notification-content p {

  margin: 0 0 var(--space-xs);

  font-size: var(--font-sm);

  font-weight: var(--font-regular);

}



.notification-content span {

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

}



.delete-button {

  border:none;

  background:none;

  font-size: var(--font-lg);

  color: var(--color-text-tertiary);

  cursor:pointer;

}



.notification-actions {

  position:fixed;

  right: var(--space-md);

  bottom: var(--space-lg);

  display:flex;

  flex-direction:column;

  gap: var(--space-xs);

}



.notification-actions button {

  padding: var(--space-sm) var(--space-md);

  border:none;

  border-radius: var(--radius-lg);

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  cursor:pointer;

}



.empty {

  text-align:center;

  color: var(--color-text-secondary);

  padding: calc(var(--space-2xl) * 2.5) 0;

}

</style>
