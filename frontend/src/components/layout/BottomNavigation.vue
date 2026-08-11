<template>

  <nav class="bottom-nav">

    <RouterLink to="/home" class="nav-link">
      <Icon name="home" size="md" />
      <span>홈</span>
    </RouterLink>


    <RouterLink to="/cards" class="nav-link">
      <Icon name="card" size="md" />
      <span>카드</span>
    </RouterLink>


    <RouterLink to="/payment" class="nav-link" id="bottom-nav-payment">
      <Icon name="payment" size="md" />
      <span>결제</span>
    </RouterLink>


    <RouterLink to="/points" class="nav-link" id="bottom-nav-point">
      <Icon name="benefit" size="md" />
      <span>혜택</span>
    </RouterLink>


    <RouterLink to="/settings" class="nav-link">
      <Icon name="settings" size="md" />
      <span>설정</span>
    </RouterLink>

  </nav>

</template>


<script setup>
import Icon from '@/components/common/Icon.vue';
</script>


<style scoped>

.bottom-nav {

  height: 70px;

  display: flex;

  justify-content: space-around;

  align-items: center;


  border-top: 1px solid var(--color-border);

  background: var(--color-surface);


  position: fixed;

  bottom: 0;

  left: 50%;

  transform: translateX(-50%);

  width: 100%;

  max-width: 480px;


  z-index: var(--z-sticky);

}



.bottom-nav a {

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

  gap: 4px;

  color: var(--color-text-secondary);

  font-size: var(--font-xs);

  text-decoration: none;

  transition: var(--transition-fast);

}



.bottom-nav .router-link-active {

  color: var(--color-nav-active);

  font-weight: var(--font-bold);

}
</style>