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
// 읽음 처리는 부모(NotificationView)가 PATCH /api/notifications/{id}/read 로 처리한다.
// 모달은 "확인했다"는 사실만 올려보내고 통신은 관여하지 않는다.
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



  <p class="message">
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
  width:100%;
  max-width:480px;
  left:50%;
  transform:translateX(-50%);
  margin:0 auto;

  display:flex;

  align-items:center;

  justify-content:center;

  background:rgba(0,0,0,0.35);

  z-index:1000;

}



.notification-modal {

  position:relative;

  width:calc(100% - 40px);

  max-width:480px;

  /* 다이제스트는 항목이 여러 줄이라 길어질 수 있다. 화면을 넘기면 확인 버튼이
     잘려서 모달을 닫을 방법이 × 밖에 안 남으므로 안에서 스크롤시킨다. */
  max-height:80vh;

  overflow-y:auto;

  padding:24px;

  background:var(--color-surface);

  border-radius:20px;

  box-shadow:0 10px 30px rgba(0,0,0,0.15);

  box-sizing: border-box;

}



.close-button {

  position:absolute;

  top:12px;

  right:16px;

  border:none;

  background:none;

  font-size:24px;

  color:var(--color-text-tertiary);

  cursor:pointer;

}



/* 오른쪽 여백은 닫기(×) 버튼 자리다. 없으면 긴 제목의 첫 줄이 버튼 밑으로 파고든다. */
.modal-content h2 {

  margin:0 0 var(--space-md);

  padding-right: var(--space-lg);

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  line-height:1.45;

  word-break: keep-all;

  overflow-wrap: break-word;

}



.detail-area {

  display:flex;

  flex-direction:column;

  gap: var(--space-xs);

}



/* 다이제스트 본문은 서버가 줄바꿈(CONCAT_WS(CHAR(10), ...))으로 항목을 나눠 보낸다.
   pre-line 이 없으면 HTML 이 그 줄바꿈을 공백으로 접어 한 문단으로 뭉개진다 —
   "소진 (100.0%) 마이핏카드(적립형) 묶음..." 처럼 항목 경계가 사라진다. */
.detail-area p {

  margin:0;

  white-space: pre-line;

  line-height:1.65;

  font-size: var(--font-sm);

  color:var(--color-text-primary);

  word-break: keep-all;

  overflow-wrap: break-word;

}



.date {

  display:block;

  margin-top: var(--space-md);

  font-size: var(--font-xs);

  color:var(--color-text-tertiary);

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

  font-size:15px;

  font-weight:600;

  cursor:pointer;

}


</style>