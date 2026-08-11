<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },

  placeholder: {
    type: String,
    default: ''
  },

  type: {
    type: String,
    default: 'text'
  },

  label: {
    type: String,
    default: ''
  },

  message: {
    type: String,
    default: ''
  },

  error: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'update:modelValue'
])
</script>


<template>
  <div class="input-wrapper">

    <label v-if="label">
      {{ label }}
    </label>


    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :class="{ error }"
      @input="
        $emit(
          'update:modelValue',
          $event.target.value
        )
      "
    />


    <p
      v-if="message"
      class="message"
      :class="{ errorText: error }"
    >
      {{ message }}
    </p>

  </div>
</template>


<style scoped>
.input-wrapper {
  width: 100%;
  margin-bottom: var(--space-md);
}


label {
  display: block;

  margin-bottom: var(--space-xs);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  color: var(--color-text-primary);
}


input {
  width: 100%;
  height: 48px;

  padding: 0 var(--space-sm);

  box-sizing: border-box;

  border: 1px solid var(--color-input-border);
  border-radius: var(--radius-sm);

  background: var(--color-surface);

  color: var(--color-text-primary);

  font-size: var(--font-sm);

  transition: var(--transition-fast);
}


input::placeholder {
  color: var(--color-text-secondary);
}


input:focus {
  outline: none;

  border-color: var(--color-input-focus);
}


input.error {
  border-color: var(--color-input-error);
}


.message {
  margin-top: var(--space-xxs);

  font-size: var(--font-xs);

  color: var(--color-text-secondary);
}


.errorText {
  color: var(--color-input-error);
}
</style>