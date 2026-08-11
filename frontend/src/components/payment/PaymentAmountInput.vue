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

const clearAmount = () => {
  emit('update:modelValue', 0);
};


</script>


<template>

<section>


<h3>
얼마를 결제하시나요?
</h3>


<div class="input-wrapper">
  <input
   v-model="displayValue"
   type="text"
   placeholder="금액을 입력해주세요"
   inputmode="numeric"
  />
  <button
   v-if="props.modelValue > 0"
   @click="clearAmount"
   class="clear-btn"
   type="button"
   aria-label="금액 초기화"
  >
    ✕
  </button>
</div>


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

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

.input-wrapper {
  position: relative;
  width: 100%;
}

input{

  width: 100%;

  height: 56px;

  padding: 0 var(--space-md);
  padding-right: 44px;

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

.clear-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);

  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: rgba(var(--color-text-secondary-rgb, 128, 128, 128), 0.1);
  color: var(--color-text-secondary);
  font-size: 18px;
  font-weight: 300;
  line-height: 1;
  cursor: pointer;

  display: flex;
  align-items: center;
  justify-content: center;

  transition: all 0.2s;
  padding: 0;
}

.clear-btn:hover {
  background: rgba(var(--color-text-secondary-rgb, 128, 128, 128), 0.2);
  color: var(--color-text-primary);
}

.clear-btn:active {
  transform: translateY(-50%) scale(0.9);
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