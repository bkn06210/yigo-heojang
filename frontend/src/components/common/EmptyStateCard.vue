<script setup>



defineProps({

  title:{
    type:String,
    required:true
  },


  description:{
    type:String,
    default:''
  },


  buttonText:{
    type:String,
    default:''
  },


  secondaryButtonText:{
    type:String,
    default:''
  }

})



const emit = defineEmits([

  'click',

  'secondary-click'

])


</script>



<template>


<section class="empty-card">


  <p class="empty-title">

    {{ title }}

  </p>



  <p

    v-if="description"

    class="empty-description"

  >

    {{ description }}

  </p>




  <div class="button-group">


    <button

      v-if="buttonText"

      type="button"

      @click="emit('click')"

    >

      {{ buttonText }}

    </button>



    <button

      v-if="secondaryButtonText"

      type="button"

      @click="emit('secondary-click')"

    >

      {{ secondaryButtonText }}

    </button>



  </div>



</section>


</template>



<style scoped>


.empty-card{

  width:100%;

  padding:32px 20px;

  border-radius:16px;

  background:#f7f7f7;

  display:flex;

  flex-direction:column;

  align-items:center;

  gap:12px;

}



.empty-title{

  margin:0;

  font-size:15px;

}



.empty-description{

  margin:0;

  font-size:13px;

  color:#777;

}



.button-group{

  display:flex;

  gap:10px;

}



button{

  border:none;

  padding:10px 20px;

  border-radius:20px;

  cursor:pointer;

}



</style>