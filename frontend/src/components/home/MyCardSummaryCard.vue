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



  // 대표 카드 데이터

  card: {

    type:Object,

    default:null

  }


})




// =========================
// Emits
// =========================

const emit = defineEmits([

  'click-card',

  'click-more'

])


</script>





<template>


<section class="card-summary">





  <!-- =========================
       Header
       ========================= -->

  <div class="card-header">


    <h2>

      내 카드

    </h2>



    <button

      type="button"

      @click="emit('click-more')"

    >

      더보기

    </button>


  </div>









  <!-- =========================
       1. 비로그인
       ========================= -->

  <div

    v-if="!props.isLogin"

    class="empty-content"

  >

    <p>

      로그인하면

      <br>

      내 카드 혜택과 실적을 확인할 수 있어요.

    </p>


  </div>









  <!-- =========================
       2. 로그인 + 카드 미등록
       ========================= -->

  <div

    v-else-if="!props.card"

    class="empty-content"

  >


    <p>

      등록된 카드가 없습니다.

    </p>



    <button

      type="button"

      @click="emit('click-more')"

    >

      카드 등록

    </button>


  </div>









  <!-- =========================
       3. 로그인 + 카드 등록
       ========================= -->

  <div

    v-else

    class="card-content"

    @click="emit('click-card', props.card)"

  >



    <!-- 카드 이미지 -->

    <div class="card-image">


      <img

        v-if="props.card.image"

        :src="props.card.image"

        alt="카드 이미지"

      >


      <div

        v-else

        class="image-placeholder"

      >

        CARD

      </div>


    </div>









    <!-- 카드 정보 -->

    <div class="card-info">


      <h3>

        {{ props.card.cardName }}

      </h3>




      <p>

        이번 달 실적

      </p>




      <strong>

        {{ props.card.achievementRate }}%

      </strong>





      <p>

        남은 혜택

        {{ props.card.remainBenefit?.toLocaleString() ?? 0 }}원

      </p>



    </div>



  </div>






</section>


</template>







<style scoped>


.card-summary{


  background:white;


  border-radius:20px;


  padding:20px;


  box-shadow:0 4px 12px rgba(0,0,0,0.05);


}




.card-header{


  display:flex;


  justify-content:space-between;


  align-items:center;


  margin-bottom:16px;


}




.card-header h2{


  margin:0;


  font-size:18px;


}




.card-header button{


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


  gap:12px;


  text-align:center;


  color:#555;


}




.empty-content button{


  padding:10px 20px;


  border:none;


  border-radius:20px;


  cursor:pointer;


}





.card-content{


  display:flex;


  gap:16px;


  cursor:pointer;


}





.card-image{


  width:100px;


  height:60px;


}





.card-image img{


  width:100%;


  height:100%;


  object-fit:contain;


}





.image-placeholder{


  width:100%;


  height:100%;


  background:#eee;


  border-radius:10px;


  display:flex;


  justify-content:center;


  align-items:center;


}





.card-info h3{


  margin:0 0 8px;


}





.card-info p{


  margin:4px 0;


  font-size:14px;


}



</style>