<script setup>

defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  }
})

</script>


<template>
<header>

  <button 
    v-if="showBack"
    class="back"
  >
    ←
  </button>

  <h1>
    {{ title }}
  </h1>

</header>
</template>


<style scoped>

header {
  height: 56px;
  display:flex;
  align-items:center;
  position:relative;
}

.back {
  border:none;
  background:none;
  font-size:24px;
}

h1 {
  position:absolute;
  left:50%;
  transform:translateX(-50%);

  font-size:18px;
}

</style>