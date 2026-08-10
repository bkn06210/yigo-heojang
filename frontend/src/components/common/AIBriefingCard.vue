<script setup>
import { useRouter } from 'vue-router'


const router = useRouter()


const props = defineProps({

  isLogin: {
    type: Boolean,
    default: false
  },


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
        ✨ 두리 브리핑
      </h3>


      <p>
        맞춤 혜택 분석을 위해 로그인하고 회원가입해봐요.
      </p>


      <div class="buttons">

        <button @click="goSignup">
          회원가입/로그인
        </button>

      </div>


    </template>



    <!-- 로그인 후 -->
    <template v-else>

      <h3>
        ✨ 두리 브리핑
      </h3>


      <p>
        {{ props.message }}
      </p>


    </template>


  </section>

</template>


<style scoped>

.ai-briefing-card {

  background: linear-gradient(135deg, rgba(248, 243, 212, 0.7) 0%, rgba(245, 239, 201, 0.4) 100%);

  border: 1px solid rgba(255, 255, 255, 0.3);

  border-radius: var(--radius-lg);

  padding: var(--space-xl);

  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    inset 0 1px 1px rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 rgba(0, 0, 0, 0.05);

  backdrop-filter: blur(12px);

  -webkit-backdrop-filter: blur(12px);

  overflow: hidden;

  word-break: break-word;

  box-sizing: border-box;

  position: relative;

}


[data-theme="dark"] .ai-briefing-card {

  background: linear-gradient(135deg, rgba(60, 64, 70, 0.5) 0%, rgba(46, 49, 54, 0.3) 100%);

  border: 1px solid rgba(255, 255, 255, 0.2);

  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 1px rgba(255, 255, 255, 0.1),
    inset 0 -1px 0 rgba(0, 0, 0, 0.3);

}


h3 {

  margin: 0 0 var(--space-md);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-semibold);

  text-align: left;

}



p {

  margin: 0;

  font-size: var(--font-sm);

  line-height: 1.6;

  color: var(--color-text-primary);

  text-align: left;

}



.buttons {

  display: flex;

  gap: var(--space-lg);

  margin-top: var(--space-md);

  justify-content: flex-start;

}



button {

  background: none;

  border: none;

  padding: 0;

  cursor: pointer;

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  color: var(--color-btn-text-on-light-bg);

  transition: var(--transition-fast);

}



button:hover {

  opacity: 0.7;

}

</style>
