<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
import AIBriefingCard from '@/components/common/AIBriefingCard.vue'


// 임시 데이터
// 추후 금융 포인트 상세 API 응답으로 교체
const pointData = {
  name: 'KB Pay 포인트',
  totalPoint: 12300,

  aiMessage:
    '현재 보유 포인트는 카드 결제에 활용하는 것을 추천해요.',

  usageMethods: [
    '카드 결제',
    '간편결제',
    '포인트 전환'
  ]
}

</script>


<template>

  <div class="financial-point-detail">


    <!-- 페이지 제목 -->
    <PageHeader
      title="KB Pay 포인트"
    />


    <!-- 총 보유 포인트 -->
    <section class="point-summary">

      <p class="label">
        총 보유 포인트
      </p>

      <p class="point">
        {{ pointData.totalPoint.toLocaleString() }}P
      </p>

    </section>



    <!-- AI 브리핑 -->
   <AIBriefingCard
     :isLogin="true"
     :message="pointData.aiMessage"
   />


    <!-- 포인트 활용 방법 -->
    <section class="usage-section">

      <h2>
        포인트 활용 방법
      </h2>


      <ul>

        <li
          v-for="method in pointData.usageMethods"
          :key="method"
        >
          {{ method }}
        </li>

      </ul>

    </section>




    <!-- 포인트 안내 -->
    <section class="notice-section">

      <h2>
        포인트 안내
      </h2>


      <p>
        1P = 1원
      </p>


      <p>
        사용 조건은 카드사 정책에 따라 다를 수 있습니다.
      </p>

    </section>



  </div>

</template>



<style scoped>

.financial-point-detail {

  padding: 20px;

}



.point-summary {

  background: white;

  border-radius: 16px;

  padding: 24px;

  margin-bottom: 20px;

}



.label {

  font-size: 14px;

  color: #666;

}



.point {

  margin-top: 10px;

  font-size: 32px;

  font-weight: 700;

}



.usage-section,
.notice-section {

  background: white;

  border-radius: 16px;

  padding: 20px;

  margin-top: 20px;

}



h2 {

  font-size: 17px;

  margin-bottom: 16px;

}



ul {

  list-style: none;

  padding: 0;

}



li {

  padding: 12px 0;

  border-bottom: 1px solid #eee;

}



li:last-child {

  border-bottom: none;

}



.notice-section p {

  font-size: 14px;

  color: #666;

  margin: 8px 0;

}


</style>