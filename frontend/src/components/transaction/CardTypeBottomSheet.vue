<script setup>
import { ref } from 'vue';

const emit = defineEmits([
  'close',
  'select',
]);

const types = [
  '전체',
  '신용카드',
  '체크카드',
  '선불카드',
  '기프트',
  '오픈뱅킹(계좌결제)',
];


const selected = ref('전체');


const selectType = (type) => {
  selected.value = type;
};


const apply = () => {
  emit('select', selected.value);
};


const close = () => {
  emit('close');
};
</script>


<template>
  <div
    class="overlay"
    @click.self="close"
  >

    <section class="bottom-sheet">

      <div class="handle"/>


      <h2>
        카드 구분
      </h2>


      <div
        v-for="type in types"
        :key="type"

        class="option"

        :class="{
          selected:selected === type
        }"

        @click="selectType(type)"
      >

        {{ type }}

      </div>



      <button
        class="apply"
        @click="apply"
      >
        적용
      </button>


    </section>

  </div>
</template>


<style scoped>

.overlay{
  position:fixed;
  inset:0;
  width:100%;
  max-width:480px;
  left:50%;
  transform:translateX(-50%);
  margin:0 auto;

  background:rgba(0,0,0,.35);

  display:flex;
  justify-content:center;
  align-items:flex-end;

  z-index:1100;
}


.bottom-sheet{

  width:100%;

  max-width:480px;

  background:white;

  border-radius:24px 24px 0 0;

  padding:20px;

  box-sizing: border-box;

}


.handle{

  width:40px;
  height:5px;

  background:#ddd;

  border-radius:10px;

  margin:0 auto 20px;

}


h2{
  margin-bottom:20px;
}



.option{

  padding:16px;

  border:1px solid #ddd;

  border-radius:14px;

  margin-bottom:12px;

}



.option.selected{

  border:2px solid #4F46E5;

  color:#4F46E5;

  font-weight:700;

}



.apply{

  width:100%;

  height:48px;

  margin-top:10px;

  border:none;

  border-radius:12px;

  background:#4F46E5;

  color:white;

}

</style>