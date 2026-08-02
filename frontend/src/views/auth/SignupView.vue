<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'


// 공통 컴포넌트
import AppInput from '@/components/common/AppInput.vue'
import AppButton from '@/components/common/AppButton.vue'
import PasswordInput from '@/components/common/PasswordInput.vue'


// API
import { signup } from '@/api/authApi'


const router = useRouter()



// 입력값
const name = ref('')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')


// 비밀번호 조건 상태
const passwordValid = ref(true)


// 비밀번호 확인 오류
const passwordConfirmError = ref('')



// 비밀번호 조건 검사
const validatePassword = () => {

  const passwordRule =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/


  if (!password.value) {

    passwordValid.value = true
    return

  }

  if (!passwordConfirm.value) {
    alert('비밀번호 확인을 입력해주세요.')
    return
  }


  passwordValid.value =
    passwordRule.test(password.value)

}



// 비밀번호 확인 검사
const validatePasswordConfirm = () => {


  if (!passwordConfirm.value) {

    passwordConfirmError.value = ''
    return

  }


  if (password.value !== passwordConfirm.value) {

    passwordConfirmError.value =
      '비밀번호가 일치하지 않습니다.'

  } else {

    passwordConfirmError.value = ''

  }

}



// 입력 감지
watch(password, () => {

  validatePassword()
  validatePasswordConfirm()

})


watch(passwordConfirm, () => {

  validatePasswordConfirm()

})



// 로그인 이동
const goLogin = () => {

  router.push('/auth/login')

}



// 나중에 하기
const skip = () => {

  router.push('/home')

}



// 회원가입
const nextStep = async () => {


  if (!name.value) {

    alert('이름을 입력해주세요.')
    return

  }


  if (!email.value) {

    alert('이메일을 입력해주세요.')
    return

  }


  if (!password.value) {

    alert('비밀번호를 입력해주세요.')
    return

  }


  if (!passwordValid.value || passwordConfirmError.value) {

    return

  }



  try {

    const savedAgreements = sessionStorage.getItem('signupTermsAgreements')
    const termsAgreements = savedAgreements ? JSON.parse(savedAgreements) : []

    if (!termsAgreements.length) {
      alert('약관 동의를 먼저 진행해주세요.')
      router.push('/auth/terms')
      return
    }


    const userData = {

      name: name.value,
      email: email.value,
      password: password.value,
      termsAgreements

    }


    await signup(userData)

    sessionStorage.removeItem('signupTermsAgreements')


    alert('회원가입이 완료되었습니다.')


    router.push('/auth/login')



  } catch(error) {


    if (error.response) {

      alert(
        error.response.data.message
      )

    } else {

      alert(
        '서버와 연결할 수 없습니다.'
      )

    }

  }

}

</script>


<template>

<div class="signup">


<h1>
회원가입
</h1>


<section>


<label>이름</label>

<AppInput
  v-model="name"
  placeholder="이름을 입력해주세요"
/>



<label>이메일</label>

<AppInput
  v-model="email"
  placeholder="이메일을 입력해주세요"
/>




<label>비밀번호</label>

<PasswordInput
 v-model="password"
/>


<p
 class="guide"
 :class="{ invalid: !passwordValid }"
>
8~20자의 영문, 숫자, 특수문자 조합으로 입력해 주세요.
</p>




<label>비밀번호 확인</label>

<PasswordInput
 v-model="passwordConfirm"
/>


<p
 v-if="passwordConfirmError"
 class="error"
>
{{ passwordConfirmError }}
</p>



</section>



<div class="buttons">


<AppButton
 text="다음"
 @click="nextStep"
/>


<p>

이미 계정이 있으신가요?

<span @click="goLogin">
로그인
</span>

</p>



<AppButton
 text="나중에 하기"
 type="secondary"
 @click="skip"
/>


</div>



<footer>

<div></div>

<p>
입력하신 개인정보는 안전하게 암호화되어 보호됩니다.
</p>

</footer>


</div>

</template>



<style scoped>

.signup {

padding:24px;

}


h1 {

margin-bottom:40px;

}


section {

display:flex;
flex-direction:column;
gap:14px;

}


label {

font-size:14px;
font-weight:600;

}


.email-box {

display:flex;
gap:8px;

}


.check-button {

width:90px;
border:1px solid #ddd;
background:white;
border-radius:10px;

}


.guide {

font-size:12px;
color:#888;
margin-top:-8px;

}


.guide.invalid {

color:#ef4444;

}


.error {

font-size:12px;
color:#ef4444;
margin-top:-8px;

}


.buttons {

margin-top:40px;

}


.buttons p {

text-align:center;
font-size:14px;
margin:18px 0;

}


.buttons span {

cursor:pointer;
text-decoration:underline;

}


footer {

margin-top:50px;
text-align:center;
color:#888;
font-size:12px;

}


footer div {

border-top:1px solid #ddd;
margin-bottom:16px;

}

</style>
