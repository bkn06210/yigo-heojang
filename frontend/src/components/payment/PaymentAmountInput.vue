<script setup>

defineProps({
  modelValue:{
    type:Number,
    default:0
  }
});


const emit = defineEmits([
  'update:modelValue'
]);


const quickAmounts=[
  5000,
  10000,
  30000,
  50000
];


const updateAmount=(amount)=>{

 emit(
  'update:modelValue',
  amount
 );

};


</script>


<template>

<section>


<h3>
얼마를 결제하시나요?
</h3>


<input
 :value="modelValue"
 type="number"
 placeholder="금액 입력"
 @input="$emit('update:modelValue', Number($event.target.value))"
/>


<div class="quick-list">

<button
 v-for="amount in quickAmounts"
 :key="amount"
 @click="updateAmount(amount)"
>
+{{amount.toLocaleString()}}원
</button>


</div>


</section>


</template>