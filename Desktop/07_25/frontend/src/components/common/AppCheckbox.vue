<script setup>

defineProps({
  checked:Boolean,
  label:String
})

defineEmits([
  'update:checked'
])

</script>


<template>

<label class="checkbox">

<input
 type="checkbox"
 :checked="checked"
 @change="
 $emit(
 'update:checked',
 $event.target.checked
 )
 "
/>

<span>
{{ label }}
</span>

</label>

</template>


<style scoped>

.checkbox {
 display:flex;
 gap:8px;
 align-items:center;
 font-size:14px;
}

input {
 width:18px;
 height:18px;
}

</style>
