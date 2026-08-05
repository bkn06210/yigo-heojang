<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword } from '@/api/authApi'


import AppButton from '@/components/common/AppButton.vue'
import PasswordInput from '@/components/common/PasswordInput.vue'


// 비밀번호 입력
const password = ref('')
const passwordConfirm = ref('')


// 조건 상태
const passwordValid = ref(true)


// 확인 오류
const passwordConfirmError = ref('')
const submitError = ref('')
const loading = ref(false)
const router = useRouter()
const passwordResetToken = window.history.state?.passwordResetToken || ''



// 비밀번호 검사
const validatePassword = () => {

  const passwordRule =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/


  if (!password.value) {

    passwordValid.value = true
    return

  }


  passwordValid.value =
    passwordRule.test(password.value)

}



// 확인 검사
const validatePasswordConfirm = () => {


  if (!passwordConfirm.value) {

    passwordConfirmError.value = ''
    return

  }


  passwordConfirmError.value =
    password.value !== passwordConfirm.value
      ? '비밀번호가 일치하지 않습니다.'
      : ''

}



// 입력 감지
watch(password, () => {

  validatePassword()
  validatePasswordConfirm()

})


watch(passwordConfirm, () => {

  validatePasswordConfirm()

})



// 변경 버튼
const changePassword = async () => {

  submitError.value = ''

  if (!password.value || !passwordConfirm.value) {
    submitError.value = '새 비밀번호와 비밀번호 확인을 입력해주세요.'
    return
  }

  if (!passwordValid.value ||
      passwordConfirmError.value) {

    submitError.value = '비밀번호 입력값을 확인해주세요.'
    return

  }


  if (!passwordResetToken) {
    submitError.value = '비밀번호 인증 정보가 없습니다. 로그인 화면에서 다시 인증해주세요.'
    return
  }

  loading.value = true
  try {
    await resetPassword(passwordResetToken, password.value)
    alert('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.')
    router.replace('/auth/login')
  } catch (error) {
    submitError.value = error?.response?.data?.message || error?.message || '비밀번호 변경에 실패했습니다.'
  } finally {
    loading.value = false
  }

}

</script>


<template>

<div class="password-change">


<h1>
비밀번호 변경
</h1>



<label>
새 비밀번호
</label>


<PasswordInput
 v-model="password"
/>


<p
 class="guide"
 :class="{ invalid: !passwordValid }"
>
8~20자의 영문, 숫자, 특수문자 조합으로 입력해 주세요.
</p>



<label>
비밀번호 확인
</label>


<PasswordInput
 v-model="passwordConfirm"
/>


<p
 v-if="passwordConfirmError"
 class="error"
>
{{ passwordConfirmError }}
</p>

<p v-if="submitError" class="error">{{ submitError }}</p>



<AppButton
 :text="loading ? '변경 중...' : '확인'"
 @click="changePassword"
/>


</div>


</template>



<style scoped>

.password-change {

padding:24px;

}


h1 {

margin-bottom:40px;

}


label {

display:block;
font-size:14px;
font-weight:600;
margin-bottom:8px;

}


.guide {

font-size:12px;
color:#888;
margin-top:8px;

} 


.guide.invalid {

color:#ef4444;

}


.error {

font-size:12px;
color:#ef4444;

}


</style>
<!-- 07_25 연동 변경: 비밀번호 재설정 인증·변경 API 흐름을 화면에 연결한다. -->
