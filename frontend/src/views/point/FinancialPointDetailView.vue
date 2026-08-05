<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/common/PageHeader.vue'
import AIBriefingCard from '@/components/common/AIBriefingCard.vue'
import { getPointHistory, getPoints, getPointUsagePlaces } from '@/api/walletApi'


// 금융 포인트 목록·이용처·이용내역 API 응답을 화면 데이터로 조합
const route = useRoute()
const pointData = ref({ name: '', totalPoint: 0, aiMessage: '', usageMethods: [], histories: [] })
const errorMessage = ref('')

const loadPoint = async () => {
  try {
    const points = await getPoints()
    const point = (points?.points || []).find((item) => Number(item.pointProviderId) === Number(route.params.id))
    if (!point) throw new Error('포인트 정보를 찾을 수 없습니다.')
    const [usage, history] = await Promise.all([
      getPointUsagePlaces(route.params.id).catch(() => ({ usagePlaces: [] })),
      getPointHistory({ pointWalletId: point.pointWalletId }).catch(() => ({ histories: [] })),
    ])
    pointData.value = {
      name: point.providerName,
      totalPoint: Number(point.totalPoint || 0),
      aiMessage: `${point.providerName} ${Number(point.totalPoint || 0).toLocaleString()}P를 보유하고 있습니다.`,
      usageMethods: (usage?.usagePlaces || []).map((place) => place.placeName),
      histories: history?.histories || [],
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '포인트 상세 정보를 불러오지 못했습니다.'
  }
}

onMounted(loadPoint)

</script>


<template>

  <div class="financial-point-detail">


    <!-- 페이지 제목 -->
    <PageHeader
      :title="pointData.name || '금융 포인트'"
    />

    <p v-if="errorMessage">{{ errorMessage }}</p>


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




    <section class="usage-section">
      <h2>포인트 이용내역</h2>
      <ul v-if="pointData.histories.length">
        <li v-for="history in pointData.histories" :key="history.pointHistoryId">
          {{ history.content || history.providerName }}
          {{ history.pointType === 'USE' ? '-' : '+' }}{{ Number(history.pointAmount || 0).toLocaleString() }}P
        </li>
      </ul>
      <p v-else>포인트 이용내역이 없습니다.</p>
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

// 기존 금융 포인트 상세 페이지 라우트 유지
// 혜택 목록의 바텀시트와 동일한 포인트 API 데이터를 사용한다.
<!-- 07_25 연동 변경: 금융포인트 잔액·이력·사용처 API를 상세 화면에 표시한다. -->
