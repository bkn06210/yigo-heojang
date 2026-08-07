<script setup>

import { computed } from 'vue';

const props = defineProps({
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

const displayValue = computed({
  get() {
    return props.modelValue ? props.modelValue.toLocaleString() : '';
  },
  set(value) {
    const numValue = parseInt(value.replace(/[^0-9]/g, '')) || 0;
    emit('update:modelValue', numValue);
  }
});

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
 v-model="displayValue"
 type="text"
 placeholder="금액을 입력해주세요"
 inputmode="numeric"
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


<style scoped>

h3{

  margin: 0 0 var(--space-sm);

  font-size: var(--font-md);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

input{

  width: 100%;

  height: 56px;

  padding: 0 var(--space-md);

  box-sizing: border-box;

  border-radius: var(--radius-md);

  border: 1px solid var(--color-input-border);

  background: var(--color-surface);

  color: var(--color-text-primary);

  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  letter-spacing: var(--typo-display-medium-letter-spacing);

  transition: var(--transition-fast);

}

input:focus{

  outline: none;

  border-color: var(--color-input-focus);

}

input::placeholder{

  font-size: var(--font-md);
  font-weight: var(--font-regular);
  color: var(--color-text-tertiary);

}

.quick-list{

  display: flex;

  flex-wrap: wrap;

  gap: var(--space-xs);

  margin-top: var(--space-sm);

}

.quick-list button{

  padding: var(--space-xs) var(--space-sm);

  border-radius: var(--radius-full);

  background: linear-gradient(135deg, rgba(230, 217, 77, 0.18) 0%, rgba(230, 217, 77, 0.06) 100%);
  border: 1px solid rgba(230, 217, 77, 0.3);

  color: var(--color-text-primary);

  font-size: var(--font-xs);
  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.quick-list button:hover{

  background: linear-gradient(135deg, rgba(230, 217, 77, 0.28) 0%, rgba(230, 217, 77, 0.1) 100%);

}

[data-theme="dark"] .quick-list button{

  background: linear-gradient(135deg, rgba(228, 218, 103, 0.18) 0%, rgba(228, 218, 103, 0.06) 100%);
  border: 1px solid rgba(228, 218, 103, 0.26);

}

</style>