<script setup>

// =========================
// Props
// =========================

defineProps({

  // 달성률 (%)
  rate: {

    type:Number,

    required:true

  }

})

</script>



<template>

<div class="donut-wrapper">


  <div

    class="donut"

    :style="{
      '--rate': `${rate}%`
    }"

  >


    <div class="donut-inner">


      <strong>
        {{ rate }}%
      </strong>


      <span>
        달성률
      </span>


    </div>


  </div>


</div>

</template>



<style scoped>


.donut-wrapper{

  display:flex;

  justify-content:center;

  align-items:center;

}



.donut{


  width:110px;

  height:110px;


  border-radius:50%;


  /*
    현재는 CSS conic-gradient 사용

    --rate 값만 바뀌면
    카드별 달성률 적용 가능
  */

  background:

    conic-gradient(

      var(--color-primary) var(--rate),

      var(--color-border) var(--rate)
      #4f46e5 var(--rate),

      #eeeeee var(--rate)

    );


  display:flex;

  justify-content:center;

  align-items:center;


}



.donut-inner{


  width:80px;

  height:80px;


  border-radius:50%;


  background:var(--color-surface);
  background:white;


  display:flex;

  flex-direction:column;

  justify-content:center;

  align-items:center;


}



.donut-inner strong{

  color:var(--color-text-primary);

  font-size:var(--font-lg);

  font-weight:var(--font-bold);

  font-size:20px;


}



.donut-inner span{


  color:var(--color-text-secondary);

  font-size:var(--font-xs);
  font-size:12px;


  color:#777;


}



</style>