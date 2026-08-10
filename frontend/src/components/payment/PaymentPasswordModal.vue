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
  <div class="overlay" @click="closeModal">
    <div class="modal" @click.stop>

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
  display:flex;
  justify-content:center;
  align-items:center;
  position: fixed;
  inset: 0;
  width: 100%;
  max-width: 480px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0 auto;
  z-index: 1000;
  animation: overlay-fade-in 0.3s ease-out;
}

@keyframes overlay-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal{
  width:90%;
  max-width:480px;
  padding: var(--space-xl);
  border-radius: var(--radius-xl);
  text-align:center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  animation: modal-emerge 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-sizing: border-box;
}

@keyframes modal-emerge {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(30px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

[data-theme="dark"] .modal{

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}

.modal h2{

  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;

}

.description{

  margin-top: var(--space-xs);

  font-size: var(--font-sm);
  color: var(--color-text-secondary);

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

  background:var(--color-border);

}

.dots span.active{

  background:var(--color-primary);

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

  border:1px solid var(--color-border);

  border-radius:50%;

  font-size:24px;

  background: var(--color-surface);

  color:var(--color-text-primary);

  font-weight:var(--font-semibold);

  cursor:pointer;

  transition:var(--transition-fast);

}

.keypad button:active{

  transform:scale(0.95);

}

.empty{

  visibility:hidden;

}

.cancel{

  width:100%;

  margin-top:24px;

  height:48px;

  border:none;

  border-radius:12px;

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color:var(--color-btn-primary-text);

  font-weight:var(--font-semibold);

  cursor:pointer;

  transition:var(--transition-fast);

}

</style>