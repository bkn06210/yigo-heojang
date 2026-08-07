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


<<<<<<< HEAD
  background: var(--color-surface);

  color: var(--color-text-primary);
=======
  background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


  padding:10px 14px;


  border-radius:20px;


  white-space:nowrap;


<<<<<<< HEAD
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
=======
  box-shadow:0 2px 8px rgba(0,0,0,.06);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


  cursor:pointer;


}



.quick-question:active{

  transform:scale(.97);

}


</style>