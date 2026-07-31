<script setup>

import { ref } from 'vue'


// 부모에게 이벤트 전달
const emit = defineEmits([
  'close',<script setup>

import { ref } from 'vue'


// 부모에게 이벤트 전달
const emit = defineEmits([
  'close',
  'success'
])


// 입력값
const email = ref('')
const authCode = ref('')

const emailError = ref('')
const authCodeError = ref('')

const codeSent = ref(false)

const timer = ref(180)

const canResend = ref(false)

const loading = ref(false)


// 인증 완료
const verify = () => {


  console.log('본인인증 확인')


  // 추후 API 연결 위치
  emit('success')

}


// 닫기
const close = () => {

  emit('close')

}

</script>


<template>

<div class="overlay">


<div class="modal">


<button
class="close"
@click="close"
>
×
</button>


<h2>
본인인증
</h2>


<label>
아이디(E-mail)
</label>


<input
v-model="email"
placeholder="이메일을 입력해주세요"
/>



<label>
인증번호
</label>


<input
v-model="authCode"
placeholder="인증번호를 입력해주세요"
/>



<button
class="confirm"
@click="verify"
>
확인
</button>


</div>


</div>


</template>


<style scoped>

.overlay {

position:fixed;
inset:0;
background:rgba(0,0,0,0.4);

display:flex;
justify-content:center;
align-items:center;

}


.modal {

width:320px;
background:white;
border-radius:16px;
padding:24px;
position:relative;

}


.close {

position:absolute;
right:16px;
top:12px;
border:none;
background:none;
font-size:24px;

}


input {

width:100%;
height:44px;
margin-bottom:16px;

}


.confirm {

width:100%;
height:48px;

}

</style>
  'success'
])


// 입력값
const email = ref('')
const authCode = ref('')

const emailError = ref('')
const authCodeError = ref('')

const codeSent = ref(false)

const timer = ref(180)

const canResend = ref(false)

const loading = ref(false)


// 인증 완료
const verify = () => {


  console.log('본인인증 확인')


  // 추후 API 연결 위치
  emit('success')

}


// 닫기
const close = () => {

  emit('close')

}

</script>


<template>

<div class="overlay">


<div class="modal">


<button
class="close"
@click="close"
>
×
</button>


<h2>
본인인증
</h2>


<label>
아이디(E-mail)
</label>


<input
v-model="email"
placeholder="이메일을 입력해주세요"
/>



<label>
인증번호
</label>


<input
v-model="authCode"
placeholder="인증번호를 입력해주세요"
/>



<button
class="confirm"
@click="verify"
>
확인
</button>


</div>


</div>


</template>


<style scoped>

.overlay {

position:fixed;
inset:0;
background:rgba(0,0,0,0.4);

display:flex;
justify-content:center;
align-items:center;

}


.modal {

width:320px;
background:white;
border-radius:16px;
padding:24px;
position:relative;

}


.close {

position:absolute;
right:16px;
top:12px;
border:none;
background:none;
font-size:24px;

}


input {

width:100%;
height:44px;
margin-bottom:16px;

}


.confirm {

width:100%;
height:48px;

}

</style>
