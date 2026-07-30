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
 position:fixed;
 inset:0;

 background:rgba(0,0,0,0.4);

 display:flex;
 justify-content:center;
 align-items:center;
}


.modal {
 background:white;

 width:320px;
 padding:24px;

 border-radius:16px;
}


button {
 width:100%;
 margin-top:20px;
 height:40px;
}

</style>