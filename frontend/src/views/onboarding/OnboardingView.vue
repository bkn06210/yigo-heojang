<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'


const router = useRouter()


// 현재 온보딩 단계
const currentIndex = ref(0)


// 온보딩 데이터
const onboardingList = [
  {
    title: '내 카드 혜택을\n한눈에 확인하세요',
    image: '/images/onboarding1.png'
  },
  {
    title: '포인트와 멤버십을\n쉽게 관리하세요',
    image: '/images/onboarding2.png'
  },
  {
    title: 'AI 금융 비서와\n똑똑한 금융 생활을\n시작하세요',
    image: '/images/onboarding3.png'
  }
]


// 현재 페이지 데이터
const currentPage = computed(() => {
  return onboardingList[currentIndex.value]
})


// 마지막 페이지 여부
const isLastPage = computed(() => {
  return currentIndex.value === onboardingList.length - 1
})


// 온보딩 완료 처리 후 회원가입 이동
const goSignup = () => {

  // 최초 실행 온보딩 완료 저장
  localStorage.setItem('onboardingDone', 'true')

  router.push('/auth/signup')
}


// 다음 버튼
const next = () => {

  // 마지막 페이지
  if(isLastPage.value){

    goSignup()

    return
  }


  // 다음 온보딩
  currentIndex.value++
}


// 건너뛰기
const skip = () => {

  goSignup()

}

</script>


<template>

  <div class="onboarding-container">


    <!-- 상단 영역 -->
    <header class="top-area">


      <!-- 진행 표시 -->
      <span class="progress">
        {{ currentIndex + 1 }} / {{ onboardingList.length }}
      </span>


      <!-- 건너뛰기 -->
      <button 
        class="skip-button"
        @click="skip"
      >
        건너뛰기
      </button>


    </header>



    <!-- 본문 영역 -->
    <main class="content-area">


      <!-- 안내 문구 -->
      <h1 class="title">
        {{ currentPage.title }}
      </h1>


      <!-- 이미지 영역 -->
      <div class="image-area">

        <img
          :src="currentPage.image"
          alt="온보딩 이미지"
        />

      </div>


    </main>



    <!-- 하단 버튼 -->
    <footer class="bottom-area">


      <button
        class="next-button"
        @click="next"
      >

        {{ isLastPage ? '시작하기' : '다음' }}

      </button>


    </footer>



  </div>

</template>



<style scoped>


.onboarding-container {

  width: 100%;
  height: 100vh;

  display: flex;
  flex-direction: column;

  padding: 24px;

  box-sizing: border-box;

}



/* 상단 영역 */

.top-area {

  display: flex;

  justify-content: space-between;

  align-items: center;

}


.progress {

  font-size: 14px;

  font-weight: 600;

}


.skip-button {

  border: none;

  background: none;

  color: #888;

  font-size: 14px;

  cursor: pointer;

}



/* 본문 영역 */

.content-area {

  flex: 1;

  display: flex;

  flex-direction: column;

  justify-content: center;

  align-items: center;

  text-align: center;

}


.title {

  white-space: pre-line;

  font-size: 28px;

  line-height: 1.4;

  font-weight: 700;

  margin-bottom: 40px;

}



.image-area {

  width: 280px;

  height: 280px;

  display: flex;

  justify-content: center;

  align-items: center;

}



.image-area img {

  width: 100%;

  height: 100%;

  object-fit: contain;

}



/* 하단 버튼 */

.bottom-area {

  padding-bottom: 20px;

}



.next-button {

  width: 100%;

  height: 56px;


  border-radius: 14px;

  border: none;


  background: #111;

  color: white;


  font-size: 16px;

  font-weight: 600;


  cursor: pointer;

}


</style>