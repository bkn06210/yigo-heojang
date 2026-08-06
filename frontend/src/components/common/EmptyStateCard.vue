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

  padding: var(--space-lg);

  border-radius: var(--radius-lg);

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);

  display:flex;

  flex-direction:column;

  align-items:center;

  justify-content:center;

  gap:var(--space-sm);

  overflow:hidden;

  word-break:break-word;

  box-sizing:border-box;

}



.empty-title{

  margin:0 0 0 var(--space-xs);

  color:var(--color-text-primary);

  font-size:var(--font-md);

  font-weight:var(--font-semibold);

  line-height: 1.4;

}



.empty-description{

  margin:0 0 0 var(--space-xs);

  color:var(--color-text-secondary);

  font-size:var(--font-sm);

  line-height: 1.5;

}



.button-group{

  display:flex;

  gap:var(--space-xs);

  margin-top:var(--space-sm);

}



button{

  border:none;

  padding:var(--space-xs) var(--space-lg);

  border-radius:var(--radius-full);

  background:var(--color-primary);

  color:var(--color-btn-primary-text);

  font-size:var(--font-sm);

  font-weight:var(--font-semibold);

  cursor:pointer;

  transition:var(--transition-fast);

}



button:hover{

  opacity:0.9;

}


</style>