<script setup>
import { computed } from 'vue';


// 부모(NotificationView)에서 전달받는 알림 데이터
const props = defineProps({

  notification: {
    type: Object,
    required: true,
  },

});


// 부모에게 전달하는 이벤트
const emit = defineEmits([
  'close',
  'read',
]);


// 날짜 표시
const createdText = computed(() => {

  if (!props.notification?.createdAt) {
    return '';
  }

  return props.notification.createdAt;

});


// 닫기
const closeModal = () => {

  emit('close');

};


// 확인 버튼
// TODO: 추후 PATCH /notifications/{id}/read 연결
const confirmNotification = () => {

  emit('read', props.notification.id);

  emit('close');

};

</script>


<template>

<div class="modal-overlay">


  <section class="notification-modal">


    <!-- 닫기 -->
    <button
      type="button"
      class="close-button"
      @click="closeModal"
    >
      ×
    </button>



    <!-- 내용 -->
    <div class="modal-content">


      <h2>
        {{ notification.title }}
      </h2>


      <div class="detail-area">


  <!-- 카드 한도 알림 -->
  <template v-if="notification.detail.cardName">

    <p>
      카드명:
      {{ notification.detail.cardName }}
    </p>


    <p>
      사용 금액:
      {{ notification.detail.usedAmount.toLocaleString() }}원
    </p>


    <p>
      한도:
      {{ notification.detail.limitAmount.toLocaleString() }}원
    </p>


  </template>



  <!-- AI 분석 -->
  <template v-if="notification.detail.summary">

    <p>
      {{ notification.detail.summary }}
    </p>


    <p>
      추천:
      {{ notification.detail.recommendation }}
    </p>

  </template>



  <!-- 결제 취소 -->
  <template v-if="notification.detail.storeName">

    <p>
      {{ notification.detail.storeName }}
    </p>


    <p>
      취소 금액:
      {{ notification.detail.amount.toLocaleString() }}원
    </p>


  </template>



  <p>
    {{ notification.detail.message }}
  </p>


</div>


      <span class="date">
        {{ createdText }}
      </span>


    </div>



    <!-- 확인 -->
    <button
      type="button"
      class="confirm-button"
      @click="confirmNotification"
    >
      확인
    </button>



  </section>


</div>

</template>



<style scoped>


.modal-overlay {

  position:fixed;

  inset:0;

  display:flex;

  align-items:center;

  justify-content:center;

  background:rgba(0,0,0,0.35);

  z-index:1000;

}



.notification-modal {

  position:relative;

  width:calc(100% - 40px);

  max-width:360px;

  padding:24px;

  background:var(--color-surface);
  background:white;

  border-radius:20px;

  box-shadow:0 10px 30px rgba(0,0,0,0.15);

}



.close-button {

  position:absolute;

  top:12px;

  right:16px;

  border:none;

  background:none;

  font-size:24px;

  color:var(--color-text-tertiary);
  color:#999;

  cursor:pointer;

}



.modal-content h2 {

  margin:0 0 16px;

  font-size: var(--font-lg);

  font-weight: var(--font-bold);
  font-size:20px;

  font-weight:700;

}



.message {

  margin-bottom:16px;

  line-height:1.6;

  font-size:15px;

  color:var(--color-text-primary);
  color:#333;

}



.date {

  font-size:13px;

  color:var(--color-text-tertiary);
  color:#999;

}



.confirm-button {

  width:100%;

  margin-top:24px;

  padding:14px;

  border:none;

  border-radius:12px;

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color:var(--color-btn-primary-text);
  background:#222;

  color:white;

  font-size:15px;

  font-weight:600;

  cursor:pointer;

}


</style>