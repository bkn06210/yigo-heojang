<script setup>
import { useRouter } from 'vue-router'


const router = useRouter()


// 부모 컴포넌트에서 전달받는 값
const props = defineProps({

  // 로그인 여부
  isLogin: {
    type: Boolean,
    default: false
  },


  // 로그인 후 AI 메시지
  message: {
    type: String,
    default: ''
  }

})


// 로그인 이동
const goLogin = () => {
  router.push('/auth/login')
}


// 회원가입 이동
const goSignup = () => {
  router.push('/auth/signup')
}

</script>


<template>

  <section class="ai-briefing-card">


    <!-- 로그인 전 -->
    <template v-if="!props.isLogin">

      <h3>
        ✨ AI 브리핑
      </h3>


      <p>
        로그인 후 나의 카드 혜택과 포인트 분석을 기반으로
        맞춤형 AI 추천을 받을 수 있어요.
      </p>


      <div class="buttons">

        <button
          class="login-button"
          @click="goLogin"
        >
          로그인
        </button>


        <button
          class="signup-button"
          @click="goSignup"
        >
          회원가입
        </button>

      </div>


    </template>



    <!-- 로그인 후 -->
    <template v-else>

      <h3>
        ✨ AI 브리핑
      </h3>


      <p>
        {{ props.message }}
      </p>


    </template>


  </section>

</template>


<style scoped>

.ai-briefing-card {

  background: white;

  border-radius: 16px;

  padding: 20px;

  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.08);

}


h3 {

  margin: 0 0 12px;

  font-size: 17px;

  font-weight: 700;

}


p {

  font-size: 14px;

  line-height: 1.6;

  color: #555;

}


.buttons {

  display: flex;

  gap: 10px;

  margin-top: 16px;

}


button {

  flex: 1;

  height: 40px;

  border-radius: 10px;

  cursor: pointer;

  font-size: 14px;

}


.login-button {

  background: #1d4ed8;

  color: white;

  border: none;

}


.signup-button {

  background: white;

  border: 1px solid #ddd;

}


</style>