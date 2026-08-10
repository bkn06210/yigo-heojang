<script setup>

defineProps({
  visible:Boolean,
  title:String
})

defineEmits([
  'close'
])

</script>


<template>

<div 
v-if="visible"
class="overlay"
>

<div class="modal">

<h2>
{{title}}
</h2>


<div>
<slot />
</div>


<button
@click="$emit('close')"
>
확인
</button>


</div>

</div>

</template>


<style scoped>

.overlay {
 position: fixed;
 inset: 0;
 width: 100%;
 max-width: 480px;
 left: 50%;
 transform: translateX(-50%);
 margin: 0 auto;

 background: rgba(0,0,0,0.4);

 display: flex;
 justify-content: center;
 align-items: center;

 z-index: var(--z-modal);
}


.modal {
 background: var(--color-surface);

 width: 90%;
 max-width: 480px;

 padding: var(--space-xl);

 border-radius: var(--radius-lg);

 box-shadow: var(--shadow-card);

 box-sizing: border-box;
}


h2 {
 margin: 0 0 var(--space-md);

 color: var(--color-text-primary);

 font-size: var(--font-lg);
 font-weight: var(--font-semibold);
}


button {
 width: 100%;

 margin-top: var(--space-lg);

 height: 40px;

 border: none;

 border-radius: var(--radius-sm);

 background:
 linear-gradient(
   90deg,
   var(--color-btn-primary-start),
   var(--color-btn-primary-end)
 );

 color: var(--color-btn-primary-text);

 font-size: var(--font-md);
 font-weight: var(--font-semibold);

 cursor: pointer;

 transition: var(--transition-fast);
}


button:hover {
 opacity: 0.9;
}

</style>