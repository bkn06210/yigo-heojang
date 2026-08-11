<script setup>
import Icon from '@/components/common/Icon.vue';
</script>

<template>

<div class="security">

<Icon name="lock" size="xs" />
안전한 결제를 위해
<br>
QR은 1분 후 자동으로 만료됩니다.

</div>


</template>


<style scoped>

.security{

  margin-top: var(--space-xl);

  padding: var(--space-sm) var(--space-md);

  border-radius: var(--radius-md);

  background: var(--color-surface);
  border: 1px solid var(--color-border);

  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  line-height: 1.6;
  text-align: center;

}

.security :deep(svg) {
  vertical-align: -2px;
  margin-right: 2px;
}

</style>