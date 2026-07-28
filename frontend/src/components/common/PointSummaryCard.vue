<script setup>


// =========================
// Props
// =========================

const props = defineProps({

  // 로그인 여부
  isLogin: {

    type:Boolean,

    default:false

  },


  // 금융 포인트 목록
  points: {

    type:Array,

    default:()=>[]

  }

})





// =========================
// Emits
// =========================

const emit = defineEmits([

  'click-more',

  'click-item'

])




</script>





<template>


<section class="point-summary">





  <!-- =========================
       Header
       ========================= -->

  <div class="point-header">


    <h2>

      금융 포인트

    </h2>



    <button

      type="button"

      @click="emit('click-more')"

    >

      더보기

    </button>


  </div>








  <!-- =========================
       비로그인
       ========================= -->

  <div

    v-if="!props.isLogin"

    class="empty-content"

  >


    <p>

      로그인하면

      <br>

      보유 금융 포인트를 확인할 수 있어요.

    </p>


  </div>









  <!-- =========================
       로그인 + 포인트 없음
       ========================= -->

  <div

    v-else-if="props.points.length === 0"

    class="empty-content"

  >


    <p>

      등록된 금융 포인트가 없습니다.

      <br>

      카드를 등록하면 금융 포인트를 관리할 수 있어요.

    </p>


  </div>









  <!-- =========================
       로그인 + 포인트 있음
       ========================= -->

  <div

    v-else

    class="point-list"

  >



    <article

      v-for="point in props.points"

      :key="point.id"

      class="point-item"

      @click="emit('click-item', point)"

    >



      <span class="point-name">

        {{ point.name }}

      </span>




      <span class="point-balance">

        {{ point.balance?.toLocaleString() ?? 0 }}P

      </span>



    </article>



  </div>





</section>


</template>








<style scoped>


.point-summary{


  background:white;


  border-radius:20px;


  padding:20px;


  box-shadow:0 4px 12px rgba(0,0,0,0.05);


}




.point-header{


  display:flex;


  justify-content:space-between;


  align-items:center;


  margin-bottom:16px;


}




.point-header h2{


  margin:0;


  font-size:18px;


}




.point-header button{


  border:none;


  background:none;


  cursor:pointer;


}




.empty-content{


  min-height:120px;


  display:flex;


  flex-direction:column;


  justify-content:center;


  align-items:center;


  text-align:center;


  gap:12px;


  color:#555;


}




.point-list{


  display:flex;


  flex-direction:column;


  gap:12px;


}




.point-item{


  display:flex;


  justify-content:space-between;


  align-items:center;


  padding:14px;


  background:#f8f8f8;


  border-radius:14px;


  cursor:pointer;


}




.point-name{


  font-size:15px;


}




.point-balance{


  font-weight:600;


}



</style>