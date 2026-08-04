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

  background: var(--color-surface);

  color: var(--color-text-primary);

}



.user .bubble{

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

}



span{

  font-size:12px;

  color: var(--color-text-tertiary);

  margin-top:4px;

}



</style>