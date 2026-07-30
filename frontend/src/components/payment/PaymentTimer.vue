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

  margin-top:16px;

  font-size:14px;

}


.timer strong{

  margin-left:6px;

}


</style>