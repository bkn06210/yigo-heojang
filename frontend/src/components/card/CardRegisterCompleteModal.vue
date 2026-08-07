<script setup>
import Icon from '@/components/common/Icon.vue';
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

    <div class="success-icon">
      <Icon name="check" size="lg" />
    </div>

    <h2>
      카드 등록 완료
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

background:var(--color-surface);
background:white;

width:80%;

padding:24px;

border-radius:20px;

text-align:center;

}

.success-icon {

  width: 60px;
  height: 60px;
  margin: 0 auto 12px;

  border-radius: 50%;

  display: flex;
  justify-content: center;
  align-items: center;

  color: var(--color-primary-dark);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);

}


button {

width:100%;

height:48px;

margin-top:20px;

border:none;

border-radius:12px;

background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);
background:#4f46e5;

color:white;

}

</style>