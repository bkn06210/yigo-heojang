<script setup>

defineProps({
  title: {
    type: String,
    default: ''
  }
});

</script>


<template>

<header class="page-header">

  <h1>
    {{ title }}
  </h1>


  <div class="actions">

    <slot />

  </div>


</header>

</template>


<style scoped>

.page-header {
  padding: 20px 0;
  font-size: 20px;
  font-weight: 700;
}

.menu-wrapper {
  margin-left: auto;
  position: relative;
}

h1 {

  margin:0;

  font-size:20px;

  font-weight:700;

}


.actions {

  display:flex;

  align-items:center;

}

</style>