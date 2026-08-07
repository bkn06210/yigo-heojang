<script setup>

defineProps({
  modelValue: Boolean,
  label: String
});


const emit = defineEmits([
  'update:modelValue'
]);


const emit = defineEmits([
  'update:modelValue'
]);


</script>


<template>

<label class="checkbox">


  <input
    type="checkbox"
    :checked="modelValue"
    @change="
      emit(
        'update:modelValue',
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
  display: flex;

  gap: var(--space-xs);

  align-items: center;

  color: var(--color-text-primary);

  font-size: var(--font-sm);
  display:flex;
  gap:8px;
  align-items:center;
  font-size:14px;
}


input {
  width: 18px;

  height: 18px;

  accent-color: var(--color-primary);

  cursor: pointer;
}


  width:18px;
  height:18px;
}

</style>