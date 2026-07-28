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

  // 포인트 리스트 이동
  'click-more',


  // 포인트 상세 이동
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

    class="empty"

  >


    <p>

      로그인하면

      <br>

      등록된 금융 포인트를 확인할 수 있어요.

    </p>


  </div>










  <!-- =========================
       로그인 + 포인트 없음
       ========================= -->

  <div

    v-else-if="!props.points.length"

    class="empty"

  >


    <p>

      등록된 금융 포인트가 없습니다.

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


      <div class="point-icon">

        P

      </div>



      <div class="point-info">


        <p class="point-name">

          {{ point.name }}

        </p>



        <p class="point-balance">

          {{ point.balance.toLocaleString() }} P

        </p>


      </div>


    </article>



  </div>



</section>


</template>








<style scoped>


.point-summary{


  background:white;


  border-radius:20px;


  padding:20px;


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





.empty{


  min-height:120px;


  display:flex;


  justify-content:center;


  align-items:center;


  text-align:center;


  color:#666;


}





.point-list{


  display:flex;


  flex-direction:column;


  gap:12px;


}





.point-item{


  display:flex;


  align-items:center;


  gap:12px;


  padding:14px;


  border-radius:14px;


  background:#f7f7f7;


  cursor:pointer;


}





.point-icon{


  width:40px;


  height:40px;


  border-radius:50%;


  display:flex;


  justify-content:center;


  align-items:center;


  background:#eee;


}




.point-info{


  display:flex;


  flex-direction:column;


  gap:4px;


}





.point-name{


  margin:0;


  font-size:14px;


}




.point-balance{


  margin:0;


  font-weight:bold;


}





</style>