<script setup>

/*
| QR 만료 모달
| QR 유효시간(60초)이 끝났을 때 표시되는 모달
|
| emit
| - refresh : QR 다시 생성
| - cancel : 결제 취소
|
*/

const emit = defineEmits([
  'refresh',
  'cancel'
]);

</script>

<template>

  <div class="overlay">

    <div class="modal">

      <!-- 안내 아이콘 -->
      <div class="icon">
        ⏰
      </div>

      <h2>
        QR 코드가 만료되었습니다.
      </h2>

      <p class="description">
        보안을 위해 QR 코드는 1분 후 자동으로 만료됩니다.
        <br>
        QR을 다시 생성하여 결제를 진행해주세요.
      </p>

      <div class="button-group">

        <!-- QR 재생성 -->
        <button
          class="refresh-button"
          @click="emit('refresh')"
        >
          QR 다시 생성
        </button>

        <!-- 결제 취소 -->
        <button
          class="cancel-button"
          @click="emit('cancel')"
        >
          결제 취소
        </button>

      </div>

    </div>

  </div>

</template>

<style scoped>

.overlay{

  position:fixed;
  inset:0;

  background:rgba(0,0,0,.45);

  display:flex;
  justify-content:center;
  align-items:center;

  z-index:1000;

}

.modal{

  width:320px;

  background:white;

  border-radius:20px;

  padding:28px;

  text-align:center;

}

.icon{

  font-size:42px;

  margin-bottom:16px;

}

.description{

  margin:20px 0;

  line-height:1.6;

  font-size:14px;

}

.button-group{

  display:flex;

  flex-direction:column;

  gap:12px;

}

.refresh-button,
.cancel-button{

  height:48px;

  border:none;

  border-radius:12px;

  cursor:pointer;

}

</style>