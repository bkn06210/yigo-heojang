<template>
  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg" class="icon">
    <!-- 카드 배경 -->
    <rect x="20" y="30" width="80" height="60" rx="8" fill="url(#cardGradient)" stroke="var(--color-primary)" stroke-width="2"/>

    <!-- 칩 -->
    <rect x="30" y="40" width="12" height="16" rx="2" fill="var(--color-primary)"/>

    <!-- 텍스트 라인 -->
    <line x1="50" y1="45" x2="90" y2="45" stroke="var(--color-primary)" stroke-width="2" opacity="0.6"/>
    <line x1="30" y1="65" x2="90" y2="65" stroke="var(--color-primary)" stroke-width="1.5" opacity="0.4"/>
    <line x1="30" y1="75" x2="60" y2="75" stroke="var(--color-primary)" stroke-width="1.5" opacity="0.4"/>

    <defs>
      <linearGradient id="cardGradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:var(--color-primary);stop-opacity:0.1" />
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
