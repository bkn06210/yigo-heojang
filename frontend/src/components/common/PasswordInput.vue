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
  @click="showPassword = !showPassword"
>

{{ showPassword ? '👁' : '🙈' }}

</button>


</div>

</template>



<style scoped>

.password-input {

display:flex;
align-items:center;
width:100%;

}


input {

flex:1;
height:52px;
padding:0 16px;
border:1px solid #ddd;
border-radius:12px;

}


.toggle-button {

margin-left:-45px;
border:none;
background:none;
cursor:pointer;
font-size:18px;
position:relative;
z-index:10;

}

</style>