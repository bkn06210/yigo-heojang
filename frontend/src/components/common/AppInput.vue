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

    <!-- 입력 제목 -->
    <label v-if="label">
      {{ label }}
    </label>


    <!-- 입력창 -->
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


    <!-- 안내 문구 -->
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
  margin-bottom: 16px;
}


label {
  display: block;
  margin-bottom: 8px;

  font-size: 14px;
  font-weight: 600;
}


input {
  width: 100%;
  height: 48px;

  padding: 0 14px;

  border: 1px solid #ddd;
  border-radius: 10px;

  font-size: 14px;

  box-sizing: border-box;
}


input:focus {
  outline: none;
  border-color: #2563eb;
}


input.error {
  border-color: #ef4444;
}


.message {
  margin-top: 6px;

  font-size: 12px;
  color: #666;
}


.errorText {
  color: #ef4444;
}
</style>