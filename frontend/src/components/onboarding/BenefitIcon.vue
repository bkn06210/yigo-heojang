<template>
  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg" class="icon">
    <!-- 별 배경 원 -->
    <circle cx="60" cy="60" r="50" fill="url(#benefitGradient)" stroke="var(--color-primary)" stroke-width="2"/>

    <!-- 별 -->
    <polygon points="60,30 68,50 90,50 73,62 81,82 60,70 39,82 47,62 30,50 52,50"
             fill="var(--color-primary)" opacity="0.8"/>

    <!-- 작은 별들 -->
    <circle cx="35" cy="40" r="4" fill="var(--color-primary)" opacity="0.5"/>
    <circle cx="85" cy="75" r="4" fill="var(--color-primary)" opacity="0.5"/>
    <circle cx="45" cy="85" r="3" fill="var(--color-primary)" opacity="0.4"/>

    <defs>
      <linearGradient id="benefitGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:var(--color-primary);stop-opacity:0.15" />
        <stop offset="100%" style="stop-color:var(--color-primary);stop-opacity:0.05" />
      </linearGradient>
    </defs>
  </svg>
</template>

<style scoped>
.icon {
  width: 120px;
  height: 120px;
}
</style>
