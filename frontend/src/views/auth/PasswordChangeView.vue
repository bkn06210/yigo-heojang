<script setup>
import { ref, watch } from 'vue'


import AppButton from '@/components/common/AppButton.vue'
import PasswordInput from '@/components/common/PasswordInput.vue'


// 비밀번호 입력
const password = ref('')
const passwordConfirm = ref('')


// 조건 상태
const passwordValid = ref(true)


// 확인 오류
const passwordConfirmError = ref('')



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
const changePassword = () => {

  if (!passwordValid.value ||
      passwordConfirmError.value) {

    return

  }


  console.log('비밀번호 변경 API 연결 예정')

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



<AppButton
 text="확인"
 @click="changePassword"
/>


</div>


</template>



<style scoped>

.password-change {

padding: var(--space-xl);

}


h1 {

margin-bottom: var(--space-2xl);
font-size: var(--font-2xl);
font-weight: var(--font-bold);
color: var(--color-text-primary);

}


label {

display:block;
font-size: var(--font-sm);
font-weight: var(--font-semibold);
margin-bottom: var(--space-xs);
color: var(--color-text-primary);

}


.guide {

font-size: var(--font-xs);
color: var(--color-text-tertiary);
margin-top: var(--space-xs);

} 


.guide.invalid {

color: var(--color-coral);

}


.error {

font-size: var(--font-xs);
color: var(--color-coral);
margin-top: var(--space-xs);

}


</style>
