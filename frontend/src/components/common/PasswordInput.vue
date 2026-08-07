<script setup>

import { ref, watch } from 'vue'


// 부모 v-model 연결
const props = defineProps({

  modelValue: {
    type: String,
    default: ''
  }

})


const emit = defineEmits([
  'update:modelValue'
])


// 내부 입력값
const password = ref(props.modelValue)


// 부모 값 변경 감지
watch(
  () => props.modelValue,
  (value) => {

    password.value = value

  }
)


// 내부 값 변경 → 부모 전달
watch(password, (value) => {

  emit(
    'update:modelValue',
    value
  )

})


// 보기/숨기기 상태
const showPassword = ref(false)


</script>


<template>

<div class="password-input">


<input
  :value="props.modelValue"
  :type="showPassword ? 'text' : 'password'"
  placeholder="비밀번호를 입력해주세요"
  @input="
    emit(
      'update:modelValue',
      $event.target.value
    )
  "
/>



<button
  type="button"
  class="toggle-button"
  :title="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
  @click="showPassword = !showPassword"
  :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
>
  <svg v-if="showPassword" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <!-- 열린 눈 아이콘 -->
    <path d="M12 5C7 5 2.73 8.11 1 12.46c1.73 4.35 6 7.54 11 7.54s9.27-3.19 11-7.54C21.27 8.11 17 5 12 5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" />
  </svg>
  <svg v-else viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <!-- 닫힌 눈 아이콘 (눈금 모양) -->
    <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.81-2.89 3.69-4.75-1.73-4.35-6-7.54-11-7.54-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 001 12c1.73 4.35 6 7.54 11 7.54 2.04 0 4.05-.37 5.91-1.04l.64.64L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zM11.84 9.02l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" />
  </svg>
</button>


</div>

</template>



<style scoped>

.password-input {

  display: flex;

  align-items: center;

  width: 100%;

}



input {

  flex: 1;

  height: 52px;

  padding: 0 var(--space-md);

  border: 1px solid var(--color-input-border);

  border-radius: var(--radius-md);

  background: var(--color-surface);

  color: var(--color-text-primary);

  font-size: var(--font-sm);

  box-sizing: border-box;

  transition: var(--transition-fast);

}


input::placeholder {

  color: var(--color-text-secondary);

}



input:focus {

  outline: none;

  border-color: var(--color-input-focus);

}



.toggle-button {

  margin-left: -45px;

  padding: 8px;

  border: none;

  background: transparent;

  cursor: pointer;

  position: relative;

  z-index: 10;

  display: flex;

  align-items: center;

  justify-content: center;

  transition: opacity var(--transition-fast);

}

.toggle-button:hover {

  opacity: 0.7;

}

.toggle-button svg {

  width: 20px;

  height: 20px;

  fill: var(--color-text-secondary);

  transition: fill var(--transition-fast);

}

.toggle-button:hover svg {

  fill: var(--color-text-primary);

}



</style>