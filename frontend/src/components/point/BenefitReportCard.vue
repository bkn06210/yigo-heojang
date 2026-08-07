<script setup>
<<<<<<< HEAD
import BaseCard from '@/components/common/BaseCard.vue'

defineProps({
  report: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open'])

const openDetail = () => {
  emit('open')
}
</script>

<template>
  <BaseCard clickable class="benefit-report-card" @click="openDetail">
    <p class="title">이번 달 받은 혜택</p>
    <strong class="amount">{{ report.totalBenefit.toLocaleString() }}원</strong>

    <div class="max-benefit">
      <span>최대 혜택 영역</span>
      <b>{{ report.maxCategory }}</b>
    </div>

    <p class="detail-button">자세히 보기 →</p>
  </BaseCard>
</template>

<style scoped>
.benefit-report-card {
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.18);
}

[data-theme="dark"] .benefit-report-card {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.07) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.26);
}

.title {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
}

.amount {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
}

.max-benefit {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  margin-top: var(--space-md);
  font-size: var(--font-sm);
}

.max-benefit span {
  color: var(--color-text-secondary);
}

.max-benefit b {
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
}

.detail-button {
  text-align: right;
  margin: var(--space-md) 0 0;
  color: var(--color-primary-dark);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
}
</style>
=======

const emit = defineEmits([
  'open'
])


const openDetail = () => {

  emit('open')

}

</script>


<template>

  <button
    class="benefit-report-card"
    @click="openDetail"
  >


    <p class="title">
      이번 달 받은 혜택
    </p>


    <strong>
      12,500원
    </strong>


    <div class="max-benefit">

      <span>
        최대 혜택 영역
      </span>


      <b>
        구독/콘텐츠
      </b>

    </div>



    <p class="detail-button">
      자세히 보기 →
    </p>


  </button>

</template>


<style scoped>

.benefit-report-card {

  width: 100%;

  background: white;

  border-radius: 16px;

  padding: 20px;

  text-align: left;

  border: none;

  box-shadow:
    0 2px 8px rgba(0,0,0,0.08);

  cursor: pointer;

}



.title {

  font-size: 14px;

  color: #666;

}



.benefit-report-card strong {

  display: block;

  font-size: 28px;

  margin: 10px 0;

}



.max-benefit {

  display: flex;

  flex-direction: column;

  gap: 6px;

}



.max-benefit span {

  font-size: 13px;

  color: #777;

}



.max-benefit b {

  font-size: 16px;

}



.detail-button {

  text-align: right;

  margin-top: 16px;

  color: #2563eb;

  font-size: 13px;

  font-weight: 600;

}


</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
