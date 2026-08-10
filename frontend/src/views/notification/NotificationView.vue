<script setup>
import { ref } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';
import NotificationItem from '@/components/notification/NotificationItem.vue';


// 알림 목록
// TODO: GET /api/notifications 연결 후 교체
const notifications = ref([

  {
    id:1,

    title:'포인트 소멸 예정',

    content:
      'CJ ONE 포인트 1,200P가 곧 소멸될 예정이에요.',

    isRead:false,

    time:'10분 전',

  },


  {
    id:2,

    title:'카드 혜택 업데이트',

    content:
      'KB My WE:SH 카드 혜택 정보가 변경됐어요.',

    isRead:true,

    time:'어제',

  },


  {
    id:3,

    title:'이번 달 혜택 리포트',

    content:
      '이번 달 받은 혜택을 확인해 보세요.',

    isRead:false,

    time:'7월 29일',

  },

]);



// 읽음 처리
const readAll = ()=>{


  notifications.value = notifications.value.map(item=>({

    ...item,

    isRead:true

  }));

};



// 전체 삭제
const deleteAll = ()=>{


  notifications.value = [];

};



// 개별 삭제
const deleteNotification = (id)=>{


  notifications.value = notifications.value.filter(

    item=>item.id !== id

  );


};

</script>


<template>

<div class="notification-view">


  <PageHeader title="알림" />



  <main class="notification-container">


    <div

      v-if="notifications.length"

      class="notification-list"

    >


      <NotificationItem

        v-for="item in notifications"

        :key="item.id"

        :notification="item"

        @delete="deleteNotification"

      />


    </div>



    <div

      v-else

      class="empty"

    >

      새로운 알림이 없습니다.

    </div>



  </main>




  <!-- 하단 버튼 -->

  <div class="action-buttons">


    <button

      @click="readAll"

    >

      모두 읽음

    </button>



    <button

      class="delete"

      @click="deleteAll"

    >

      전체 삭제

    </button>


  </div>



</div>

</template>



<style scoped>


.notification-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
}



.notification-container {
  flex: 1;
  padding: 20px;
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}



.notification-list{

  display:flex;

  flex-direction:column;

  gap:12px;

}



.empty{

  margin-top:100px;

  text-align:center;

  color:#999;

}



.action-buttons{

  position:fixed;

  right:20px;

  bottom:90px;


  display:flex;

  gap:10px;

}



.action-buttons button{

  border:none;

  padding:12px 16px;

  border-radius:20px;

  background:#4F46E5;

  color:white;

  font-weight:600;

}



.action-buttons .delete{

  background:#ef4444;
 
}



</style>
