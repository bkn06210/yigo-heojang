<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import AppInput from '@/components/common/AppInput.vue';
import AppButton from '@/components/common/AppButton.vue';
import AuthVerifyModal from '@/components/auth/AuthVerifyModal.vue';

// 로그인 API 함수
import { login as loginApi } from '@/api/authApi';

// 비밀번호 입력 공통 컴포넌트
import PasswordInput from '@/components/common/PasswordInput.vue';

// 로그인 상태 관리 Store
import { useAuthStore } from '@/stores/authStore';


const router = useRouter();


// 로그인 상태 저장소
const authStore = useAuthStore();


const email = ref('');
const password = ref('');
const passwordValid = ref(true);


// 로그인 요청 처리 함수
// 입력값 검증 후 백엔드 로그인 API 호출
const login = async () => {

  // 이메일 입력 확인
  if (!email.value) {
    alert('이메일을 입력해주세요.');
    return;
  }


  // 비밀번호 입력 확인
  if (!password.value) {
    alert('비밀번호를 입력해주세요.');
    return;
  }


  try {

    // 백엔드로 전달할 로그인 데이터
    // 비밀번호 확인 값은 없기 때문에 email, password만 전달
    const loginData = {
      email: email.value,
      password: password.value,
    };


    console.log(
      '요청 데이터:',
      loginData
    );


    // POST /api/auth/login 호출
    const response = await loginApi(loginData);


    console.log(
      '로그인 성공 응답:',
      response.data
    );


    // 로그인 성공 데이터
    const accessToken =
      response.data.data.accessToken;


    const member =
      response.data.data.member;



    // Pinia 저장
    // token + 로그인 사용자 정보 저장
    authStore.setLogin(
      accessToken,
      member
    );


    console.log(
      '저장된 사용자:',
      authStore.user
    );


    // 홈 이동
    console.log(
      '홈 이동'
    );

    router.push('/home');



  } catch (error) {

    console.log(
      '로그인 에러:',
      error
    );


    // 서버에서 내려준 오류 처리
    if (error.response) {

      alert(
        error.response.data.message
      );


    } else {

      alert(
        '서버와 연결할 수 없습니다.'
      );

    }

  }

};



const goSignup = () => {

  router.push('/auth/signup');

};



// 비밀번호 찾기 팝업 표시 여부
const showPasswordFind = ref(false);



// 비밀번호 찾기 버튼 클릭
const findPassword = () => {

  showPasswordFind.value = true;

};



// 본인인증 완료
const verifyPasswordFind = () => {

  showPasswordFind.value = false;

  router.push('/auth/password-change');

};

</script>



<template>

  <div class="login">

    <h1>로그인</h1>


    <section>

      <label>이메일</label>

      <AppInput
        v-model="email"
        placeholder="이메일을 입력해주세요"
      />


      <label>비밀번호</label>

      <PasswordInput
        v-model="password"
      />

    </section>



    <div class="buttons">

      <AppButton
        text="로그인"
        @click="login"
      />



      <p>

        계정이 없으신가요?

        <span @click="goSignup">

          회원가입

        </span>

      </p>



      <p
        class="password"
        @click="findPassword"
      >

        비밀번호를 잊으셨나요?

      </p>


    </div>


  </div>



  <AuthVerifyModal

    v-if="showPasswordFind"

    @close="showPasswordFind=false"

    @success="verifyPasswordFind"

  />


</template>



<style scoped>

.login {

  padding: 24px;

}


h1 {

  margin-bottom: 40px;

}



section {

  display: flex;

  flex-direction: column;

  gap: 14px;

}



.buttons {

  margin-top: 40px;

}



.buttons p {

  text-align: center;

  font-size: 14px;

  margin-top: 20px;

}



span,
.password {

  cursor: pointer;

  text-decoration: underline;

}

</style>