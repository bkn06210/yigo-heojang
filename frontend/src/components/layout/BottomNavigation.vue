<template>
  <nav class="bottom-nav">
    <RouterLink to="/">홈</RouterLink>
    <RouterLink to="/card">카드</RouterLink>
    <RouterLink to="/payment">결제</RouterLink>
    <RouterLink to="/point">포인트</RouterLink>
    <RouterLink to="/setting">설정</RouterLink>
  </nav>
</template>

<script setup>
// 현재는 구조만 구현
</script>

<style scoped>
.bottom-nav {
  height: 70px;

  display: flex;
  justify-content: space-around;
  align-items: center;

  border-top: 1px solid #ddd;
  background: white;

  position: sticky;
  bottom: 0;
}

.bottom-nav a {
  text-decoration: none;
  color: #555;
  font-size: 14px;
  font-weight: 500;
}

.router-link-active {
  font-weight: bold;
}
</style>

//나중에 할 일

아이콘 추가
활성화 색상
배지(알림 개수)
가운데 결제 버튼 디자인