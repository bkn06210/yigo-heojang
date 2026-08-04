<script setup>

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