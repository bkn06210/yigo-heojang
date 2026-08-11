<script setup>
const translateX = ref(0);

const props = defineProps({

  notification:{

    type:Object,

    required:true

  }

});


const emit = defineEmits([

  'delete'

]);



const remove = ()=>{

  emit(

    'delete',

    props.notification.id

  );

};


</script>



<template>

<div

  class="notification-item"

  :class="{

    unread: !notification.isRead,

    read: notification.isRead

  }"

>



  <div class="content">


    <h3>

      {{ notification.title }}

    </h3>



    <p>

      {{ notification.content }}

    </p>



    <span>

      {{ notification.time }}

    </span>


  </div>



  <!-- 임시 삭제 버튼 -->
  <!-- 추후 swipe action으로 변경 -->

  <button

    class="delete-button"

    @click="remove"

  >

    삭제

  </button>

  <div class="notification-wrapper">
  <div class="delete-action">
    삭제
  </div>

  <div class="notification-content">
    알림 내용
  </div>
</div>


</div>

</template>



<style scoped>


.notification-item{

  background:white;

  border-radius:20px;

  padding:18px;

  display:flex;

  justify-content:space-between;

  align-items:center;

  box-shadow:

    0 3px 12px rgba(0,0,0,.05);

}



.content h3{

  margin:0 0 8px;

  font-size:16px;

}



.content p{

  margin:0 0 8px;

  font-size:14px;

}



.content span{

  font-size:12px;

}



.unread h3,

.unread p{

  color:#111827;

  font-weight:700;

}



.read h3,

.read p{

  color:#9ca3af;

  font-weight:400;

}



.content span{

  color:#aaa;

}



.delete-button{

  border:none;

  background:#fee2e2;

  color:#ef4444;

  border-radius:12px;

  padding:8px 12px;

}



</style>