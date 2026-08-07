<script setup>

defineProps({
  modelValue: Boolean,
  label: String
});
<<<<<<< HEAD


const emit = defineEmits([
  'update:modelValue'
]);
=======


const emit = defineEmits([
  'update:modelValue'
]);

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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
<<<<<<< HEAD
  display: flex;

  gap: var(--space-xs);

  align-items: center;

  color: var(--color-text-primary);

  font-size: var(--font-sm);
=======
  display:flex;
  gap:8px;
  align-items:center;
  font-size:14px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


input {
<<<<<<< HEAD
  width: 18px;

  height: 18px;

  accent-color: var(--color-primary);

  cursor: pointer;
}


=======
  width:18px;
  height:18px;
}

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</style>