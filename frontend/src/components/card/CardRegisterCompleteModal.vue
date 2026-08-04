<script setup>

const emit = defineEmits([
  'close',
  'confirm'
]);


const confirm = () => {

  emit('confirm');

};


const close = () => {

  emit('close');

};

</script>


<template>

<div class="overlay">

  <section class="modal">

    <h2>
      🎉 카드 등록 완료
    </h2>


    <p>
      카드 등록이 완료되었습니다.
    </p>


    <button
      @click="confirm"
    >
      확인
    </button>

  </section>

</div>

</template>


<style scoped>

.overlay {

position:fixed;
inset:0;

background:rgba(0,0,0,.4);

display:flex;
justify-content:center;
align-items:center;

z-index:3000;

}


.modal {

background:white;

width:80%;

padding:24px;

border-radius:20px;

text-align:center;

}


button {

width:100%;

height:48px;

margin-top:20px;

border:none;

border-radius:12px;

background:#4f46e5;

color:white;

}

</style>