<template>
  <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg" class="icon">
    <!-- 채팅 말풍선 배경 -->
    <path d="M 20 20 L 100 20 Q 105 20 105 25 L 105 75 Q 105 80 100 80 L 40 80 L 25 95 L 35 80 L 25 80 Q 20 80 20 75 L 20 25 Q 20 20 25 20"
          fill="url(#chatGradient)" stroke="var(--color-primary)" stroke-width="2"/>

    <!-- 메시지 라인 -->
    <line x1="35" y1="35" x2="90" y2="35" stroke="var(--color-primary)" stroke-width="2" opacity="0.7"/>
    <line x1="35" y1="50" x2="85" y2="50" stroke="var(--color-primary)" stroke-width="2" opacity="0.5"/>
    <line x1="35" y1="65" x2="75" y2="65" stroke="var(--color-primary)" stroke-width="2" opacity="0.5"/>

    <defs>
      <linearGradient id="chatGradient" x1="0%" y1="0%" x2="100%" y2="100%">
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
