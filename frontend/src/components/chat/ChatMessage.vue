<script setup>

defineProps({

  message:{

    type:Object,

    required:true

  }

});

</script>


<template>

<div

  class="message"

  :class="message.sender"

>


  <div class="bubble">


    {{ message.message }}


  </div>


  <span>

    {{ message.time }}

  </span>


</div>

</template>


<style scoped>


.message{

  display:flex;

  flex-direction:column;

}



.message.user{

  align-items:flex-end;

}



.message.ai{

  align-items:flex-start;

}



.bubble{

  max-width:80%;

  padding:14px 16px;

  border-radius:18px;

  white-space:pre-line;

  line-height:1.5;

}



.ai .bubble{

<<<<<<< HEAD
  background: var(--color-surface);

  color: var(--color-text-primary);
=======
  background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.user .bubble{

<<<<<<< HEAD
  background: var(--color-primary);

  color: var(--color-btn-primary-text);
=======
  background:#4F46E5;

  color:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



span{

  font-size:12px;

<<<<<<< HEAD
  color: var(--color-text-tertiary);
=======
  color:#999;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  margin-top:4px;

}



</style>