<script setup>


defineProps({

  question:String

});


</script>


<template>

<button class="quick-question">

{{ question }}

</button>

</template>


<style scoped>


.quick-question{


  border:none;


  background: var(--color-surface);

  color: var(--color-text-primary);
  background:white;


  padding:10px 14px;


  border-radius:20px;


  white-space:nowrap;


  box-shadow: 0 2px 8px rgba(0,0,0,.06);
  box-shadow:0 2px 8px rgba(0,0,0,.06);


  cursor:pointer;


}



.quick-question:active{

  transform:scale(.97);

}


</style>