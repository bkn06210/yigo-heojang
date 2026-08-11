<template>

<div class="loading">

<span class="spinner"></span>

<p>
혜택을 분석하고 있어요...
</p>

</div>

</template>


<style scoped>

.loading{

  display: flex;

  flex-direction: column;

  align-items: center;

  gap: var(--space-md);

  padding: var(--space-2xl) 0;

}

.spinner{

  width: 32px;

  height: 32px;

  border-radius: 50%;

  border: 3px solid var(--color-border);

  border-top-color: var(--color-primary);

  animation: spin 0.8s linear infinite;

}

.loading p{

  margin: 0;

  font-size: var(--font-sm);

  font-weight: var(--font-medium);

  color: var(--color-text-secondary);

}

@keyframes spin{

  to{

    transform: rotate(360deg);

  }

}

</style>