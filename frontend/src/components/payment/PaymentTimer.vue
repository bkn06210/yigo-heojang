<script setup>

/*
| QR 타이머 표시 컴포넌트
|
| 역할:
| - 부모에서 전달받은 시간을 화면에 표시만 함
|
| 시간 감소 로직은 PaymentView.vue에서 관리
|
| 이유:
| - QR 재생성 시 타이머 초기화 필요
| - API 연결 시 부모에서 QR 상태 관리 필요
*/


defineProps({

  // 현재 남은 시간(초)
  seconds: {

    type:Number,

    default:60

  }

});


</script>


<template>

  <p class="timer">

    QR 유효시간

    <strong>
      {{ seconds }}초
    </strong>

  </p>


</template>


<style scoped>

.timer{

  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);

  width: fit-content;
  margin: 0 auto;

  padding: var(--space-xs) var(--space-md);

  border-radius: var(--radius-full);

  background: linear-gradient(135deg, rgba(230, 217, 77, 0.18) 0%, rgba(230, 217, 77, 0.06) 100%);
  border: 1px solid rgba(230, 217, 77, 0.3);

  font-size: var(--font-sm);
  color: var(--color-text-secondary);

}

[data-theme="dark"] .timer{

  background: linear-gradient(135deg, rgba(228, 218, 103, 0.18) 0%, rgba(228, 218, 103, 0.06) 100%);
  border: 1px solid rgba(228, 218, 103, 0.26);
  margin-top:16px;

  font-size:14px;

}


.timer strong{

  color: var(--color-gold-text);
  font-weight: var(--font-bold);
  margin-left:6px;

}


</style>