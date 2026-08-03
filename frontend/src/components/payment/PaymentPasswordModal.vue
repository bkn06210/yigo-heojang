<script setup>
import { computed, ref } from 'vue';


const props = defineProps({

  // 모달 표시 여부
  visible: {

    type: Boolean,

    default: false

  }

});


const emit = defineEmits([

  'success',

  'close'

]);




//비밀번호
const password = ref('');

const PASSWORD_LENGTH = 6;



const passwordDots = computed(() => {

  return Array.from(

    { length: PASSWORD_LENGTH },

    (_, index) => index < password.value.length

  );

});


//숫자 입력
const inputNumber = (number) => {

  if (password.value.length >= PASSWORD_LENGTH) {

    return;

  }

  password.value += number;

  if (password.value.length === PASSWORD_LENGTH) {

    setTimeout(() => {

      password.value = '';

      emit('success');

    }, 200);

  }

};


//삭제
const deleteNumber = () => {

  password.value = password.value.slice(0, -1);

};

//닫기
const closeModal = () => {

  password.value = '';

  emit('close');

};
</script>

<template>

  <div
    v-if="visible"
    class="overlay"
  >

    <div class="modal">

      <h2>

        간편 비밀번호 입력

      </h2>

      <p class="description">

        결제를 위해 비밀번호를 입력해주세요.

      </p>

      <!-- ● ● ● ● ● ● -->
      <div class="dots">

        <span

          v-for="(filled, index) in passwordDots"

          :key="index"

          :class="{ active: filled }"

        />

      </div>

      <!-- 숫자패드 -->
      <div class="keypad">

        <button

          v-for="number in 9"

          :key="number"

          @click="inputNumber(number)"

        >

          {{ number }}

        </button>

        <button
          class="empty"
        />

        <button @click="inputNumber(0)">

          0

        </button>

        <button @click="deleteNumber">

          ←

        </button>

      </div>

      <button

        class="cancel"

        @click="closeModal"

      >

        취소

      </button>

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

  z-index:9999;

}

.modal{

  width:340px;

  padding:28px;

  border-radius:20px;

  background:#fff;

  text-align:center;

}

.description{

  margin-top:8px;

}

.dots{

  display:flex;

  justify-content:center;

  gap:12px;

  margin:32px 0;

}

.dots span{

  width:14px;

  height:14px;

  border-radius:50%;

  background:#ddd;

}

.dots span.active{

  background:#222;

}

.keypad{

  display:grid;

  grid-template-columns:repeat(3,80px);

  justify-content:center;

  gap:16px;

}

.keypad button{

  width:80px;

  height:80px;

  border:none;

  border-radius:50%;

  font-size:24px;

}

.empty{

  visibility:hidden;

}

.cancel{

  width:100%;

  margin-top:24px;

  height:48px;

}

</style>