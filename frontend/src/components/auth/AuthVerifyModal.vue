<script setup>

import { ref } from 'vue'
import { requestPasswordResetCode, verifyPasswordResetCode } from '@/api/authApi'


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

const getErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback

const sendCode = async () => {
  emailError.value = ''
  if (!email.value) {
    emailError.value = '이메일을 입력해주세요.'
    return
  }
  loading.value = true
  try {
    await requestPasswordResetCode(email.value.trim())
    codeSent.value = true
    alert('인증 코드를 전송했습니다. 이메일을 확인해주세요.')
  } catch (error) {
    emailError.value = getErrorMessage(error, '인증 코드 전송에 실패했습니다.')
  } finally {
    loading.value = false
  }
}


// 인증 완료
const verify = async () => {
  authCodeError.value = ''
  if (!codeSent.value) {
    authCodeError.value = '먼저 인증 코드를 요청해주세요.'
    return
  }
  if (!/^\d{6}$/.test(authCode.value)) {
    authCodeError.value = '인증번호 6자리를 입력해주세요.'
    return
  }
  loading.value = true
  try {
    const response = await verifyPasswordResetCode(email.value.trim(), authCode.value)
    emit('success', response.data.data.passwordResetToken)
  } catch (error) {
    authCodeError.value = getErrorMessage(error, '인증번호 확인에 실패했습니다.')
  } finally {
    loading.value = false
  }
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

<p v-if="emailError" class="error">{{ emailError }}</p>

<button class="send" :disabled="loading" @click="sendCode">
{{ codeSent ? '인증번호 재전송' : '인증번호 전송' }}
</button>



<label>
인증번호
</label>


<input
v-model="authCode"
placeholder="인증번호를 입력해주세요"
maxlength="6"
/>

<p v-if="authCodeError" class="error">{{ authCodeError }}</p>



<button
class="confirm"
:disabled="loading"
@click="verify"
>
{{ loading ? '처리 중...' : '확인' }}
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

.send { width:100%; height:42px; margin:-8px 0 16px; border:1px solid #ddd; border-radius:8px; background:white; }
.error { margin:-10px 0 12px; color:#ef4444; font-size:12px; }

</style>
<!-- 07_25 연동 변경: 본인인증 요청·검증을 실제 백엔드 API와 연결한 컴포넌트다. -->
