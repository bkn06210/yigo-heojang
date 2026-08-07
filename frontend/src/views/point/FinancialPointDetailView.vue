<<<<<<< HEAD
﻿<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
=======
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/common/PageHeader.vue'
import AIBriefingCard from '@/components/common/AIBriefingCard.vue'
import { getPointHistory, getPoints, getPointUsagePlaces } from '@/api/walletApi'
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


// 금융 포인트 목록·이용처·이용내역 API 응답을 화면 데이터로 조합
const route = useRoute()
const pointData = ref({ name: '', totalPoint: 0, aiMessage: '', usageMethods: [], histories: [] })
const errorMessage = ref('')

<<<<<<< HEAD
  usageMethods: [
    '카드 결제',
    '간편결제',
    '포인트 전환'
  ]
=======
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
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

  padding: var(--space-md);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;

}



.point-summary {

  border-radius: var(--radius-lg);

  padding: var(--space-xl);

  margin-bottom: var(--space-lg);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

}

[data-theme="dark"] .point-summary {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.15) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.25);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);

}



.label {

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

  font-weight: var(--font-regular);

}



.point {

  margin-top: var(--space-xs);

  font-size: var(--typo-display-large-size);
  font-weight: var(--typo-display-large-weight);
  line-height: var(--typo-display-large-line-height);
  letter-spacing: var(--typo-display-large-letter-spacing);
  color: var(--color-text-primary);

}



.usage-section,
.notice-section {

  background-color: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-md);

  margin-top: var(--space-md);

  box-shadow: var(--shadow-card);

}



h2 {

  margin: 0 0 var(--space-md);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  letter-spacing: -0.3px;

}



ul {

  list-style: none;

  padding: 0;

}



li {

  padding: var(--space-sm) 0;

  border-bottom: 1px solid var(--color-border);

  color: var(--color-text-primary);

}



li:last-child {

  border-bottom: none;

}



.notice-section p {

<<<<<<< HEAD
  font-size: var(--font-sm);
=======
  font-size: 14px; 
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  color: var(--color-text-secondary);

  margin: var(--space-xs) 0;

  line-height: 1.5;

}


</style>

// 기존 금융 포인트 상세 페이지 라우트 유지
<<<<<<< HEAD
// UI 변경으로 현재는 바텀시트에서 동일 API 데이터를 호출함
// 추후 API 연동 시 상세 페이지 대신 바텀시트 데이터로 매핑
=======
// 혜택 목록의 바텀시트와 동일한 포인트 API 데이터를 사용한다.
<!-- 07_25 연동 변경: 금융포인트 잔액·이력·사용처 API를 상세 화면에 표시한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
