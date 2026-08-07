<script setup>


// 부모(PointListView)에서 바텀시트 열기 위해 이벤트 전달
const emit = defineEmits([
  'open'
])


// 임시 데이터
// 추후 금융 포인트 요약 API 응답으로 교체 예정
const summaryData = {

  totalPoint: 22000,

  mainPoint: 'KB Pay 포인트'

}



// 카드 클릭
const openSheet = () => {

  emit('open')

}


</script>



<template>

  <button
    class="financial-summary-card"
    @click="openSheet"
  >


    <p class="label">
      보유 금융 포인트
    </p>


    <strong class="total-point">
      {{ summaryData.totalPoint.toLocaleString() }}P
    </strong>



    <div class="main-point">


      <span>
        대표 포인트
      </span>


      <b>
        {{ summaryData.mainPoint }}
      </b>


    </div>



    <p class="guide">
      포인트 상세 보기 →
    </p>


  </button>


</template>




<style scoped>


.financial-summary-card {


  width: 100%;

  background: var(--color-surface);
  background: white;

  border-radius: 16px;

  padding: 20px;

  text-align: left;

  border: none;


  box-shadow:

    0 2px 8px rgba(0,0,0,0.08);


  cursor: pointer;


}



.label {

  font-size: 14px;

  color: var(--color-text-secondary);
  color: #666;

}



.total-point {

  display: block;

  margin-top: 10px;

  font-size: 30px;

  font-weight: 700;

}



.main-point {

  display: flex;

  justify-content: space-between;

  align-items: center;


  margin-top: 20px;

  padding-top: 16px;

  border-top: 1px solid #eee;


}



.main-point span {

  font-size: 14px;

  color: var(--color-text-secondary);
  color: #666;

}



.main-point b {

  font-size: 14px;

}



.guide {

  margin-top: 14px;

  font-size: 13px;

  color: var(--color-primary-dark);
  color: #2563eb;

}



</style>