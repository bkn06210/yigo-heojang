<template>

  <nav class="bottom-nav">

<<<<<<< HEAD
    <RouterLink to="/home" class="nav-link">
      <Icon name="home" size="md" />
      <span>홈</span>
    </RouterLink>


    <RouterLink to="/cards" class="nav-link">
      <Icon name="card" size="md" />
      <span>카드</span>
    </RouterLink>


    <RouterLink to="/payment" class="nav-link">
      <Icon name="payment" size="md" />
      <span>결제</span>
    </RouterLink>


    <RouterLink to="/points" class="nav-link">
      <Icon name="benefit" size="md" />
      <span>혜택</span>
    </RouterLink>


    <RouterLink to="/settings" class="nav-link">
      <Icon name="settings" size="md" />
      <span>설정</span>
=======
    <RouterLink to="/home">
      홈
    </RouterLink>


    <RouterLink to="/cards">
      카드
    </RouterLink>


    <RouterLink to="/payment/recommend">
      결제
    </RouterLink>


    <RouterLink to="/points">
      혜택
    </RouterLink>


    <RouterLink to="/settings">
      설정
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

  left: 0;

  width: 100%;


  z-index: var(--z-sticky);

}

<<<<<<< HEAD


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
=======
.bottom-nav {

  height:70px;

  display:flex;

  justify-content:space-around;

  align-items:center;


  border-top:1px solid #ddd;

  background:white;


  position:fixed;

  bottom:0;

  left:0;

  width:100%;


  z-index:1000;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.bottom-nav .router-link-active {

  color: var(--color-nav-active);

  font-weight: var(--font-bold);

}
<<<<<<< HEAD
</style>
=======
</style>

//나중에 할 일

아이콘 추가
활성화 색상
배지(알림 개수)
가운데 결제 버튼 디자인

/* 공통 레이어 기준 */

<!-- BottomNavigation
z-index: 100

Dropdown
z-index: 200

Modal
z-index: 500

BottomSheet
z-index: 1000

Alert
z-index: 2000 -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
