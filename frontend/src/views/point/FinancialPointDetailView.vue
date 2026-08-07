<script setup>
import PageHeader from '@/components/common/PageHeader.vue'


// 임시 데이터
// 추후 금융 포인트 상세 API 응답으로 교체
const pointData = {
  name: 'KB Pay 포인트',
  totalPoint: 12300,

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

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

  margin: var(--space-xs) 0;

  line-height: 1.5;

}


</style>

// 기존 금융 포인트 상세 페이지 라우트 유지
// UI 변경으로 현재는 바텀시트에서 동일 API 데이터를 호출함
// 추후 API 연동 시 상세 페이지 대신 바텀시트 데이터로 매핑