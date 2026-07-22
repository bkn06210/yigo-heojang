<script setup>

// 공통 컴포넌트
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'
import PullToRefresh from '@/components/common/PullToRefresh.vue'


// 포인트 컴포넌트
import FinancialPointCard from '@/components/point/FinancialPointCard.vue'
import MembershipCard from '@/components/point/MembershipCard.vue'


import { useRouter } from 'vue-router'


const router = useRouter()

// 포인트 데이터 새로고침 함수
const refreshPoint = async () => {

  console.log('포인트 데이터 갱신 시작')

  // 지금은 테스트용
  await new Promise(resolve => {
    setTimeout(resolve, 2000)
  })

  console.log('포인트 데이터 갱신 완료')

}

const refreshPoint = async () => {
  console.log('포인트 데이터 갱신')
}



// 멤버십 추가 화면 이동
const goMembershipRegister = () => {

  router.push('/memberships/register')

}

const membershipList = [
  {
    id: 1,
    name: 'CJ ONE',
    link: 'https://www.cjone.com'
  },
  {
    id: 2,
    name: '해피포인트',
    link: 'https://www.happypointcard.com'
  }
]

</script>




<template>

  <PullToRefresh @refresh="refreshPoint">

  <div class="point-page">


    <!-- 페이지 제목 -->
    <PageHeader title="포인트" />



    <main class="content">


      <!-- 금융 포인트 영역 -->
      <section class="point-section">

        <h2>
          금융 포인트
        </h2>


        <FinancialPointCard />

      </section>




      <!-- 멤버십 영역 -->
<section class="membership-section">

  <div class="section-header">

    <h2>
      멤버십
    </h2>


    <button
      class="add-button"
      @click="goMembershipRegister"
    >
      + 추가
    </button>

  </div>


  <MembershipCard
  v-for="membership in membershipList"
  :key="membership.id"
  :membership="membership"
/>

<p class="notice">
  ※ 멤버십 상세 페이지에서 사용처 및 이용 정보를 확인할 수 있습니다.
</p>


</section>
    






    </main>



    <BottomNav />


  </div>

  </PullToRefresh>




</template>




<style scoped>


.point-page {

  min-height: 100vh;

  padding-bottom: 80px;

}



.content {

  padding: 20px;

}



.point-section {

  margin-bottom: 32px;

}



.membership-section {

  margin-bottom: 32px;

}



h2 {

  font-size: 18px;

  margin-bottom: 14px;

}



button {

  border: none;

  background: none;

  cursor: pointer;

  font-size: 14px;

}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}


.section-header h2 {
  margin-bottom: 0;
}


.add-button {
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 600;
}

.notice {

  margin-top: 14px;

  font-size: 12px;

  color: #777;

  line-height: 1.5;

}


</style>