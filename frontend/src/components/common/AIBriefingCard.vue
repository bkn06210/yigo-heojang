<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'


const router = useRouter()


const props = defineProps({

  isLogin: {
    type: Boolean,
    default: false
  },


  message: {
    type: String,
    default: ''
  },


  // 서버가 판단한 브리핑 상황.
  // NO_CARD / UNUSED_BENEFIT / PERFORMANCE_NEAR / ALL_ACHIEVED / SPENDING_INSIGHT / GETTING_STARTED
  // 문장을 뜯어 분기하지 말고 이 값으로 분기한다 — 문구는 서버에서 바뀔 수 있다.
  briefingType: {
    type: String,
    default: null
  }

})


const emit = defineEmits(['register-card'])


// 카드가 없다는 안내에는 바로 할 수 있는 행동을 붙인다.
// 나머지 상황은 이미 가진 카드 이야기라 문구만 보여준다.
const showRegisterButton = computed(
  () => props.briefingType === 'NO_CARD'
)


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
        <Icon name="info" size="sm" style="margin-right: 6px;" />
        두리 브리핑
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
        <Icon name="info" size="sm" style="margin-right: 6px;" />
        두리 브리핑
      </h3>


      <p>
        {{ props.message }}
      </p>


      <!-- 카드가 없는 상황에만 등록 버튼을 붙인다 -->
      <div
        v-if="showRegisterButton"
        class="buttons"
      >

        <button @click="emit('register-card')">
          카드 등록하기
        </button>

      </div>


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

  display: flex;

  align-items: center;

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
