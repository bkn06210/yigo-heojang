<template>
  <div class="illustration">
    <!-- 포인트 링 차트 -->
    <div class="progress-ring">
      <div class="ring-inner">
        <div class="ring-value">12,480</div>
        <div class="ring-label">포인트 · GOLD</div>
      </div>
    </div>

    <!-- 포인트 행 1 -->
    <div class="point-item">
      <span class="point-name">ALL point</span>
      <div class="point-right">
        <span class="point-value">8,200</span>
        <span class="arrow">›</span>
      </div>
    </div>

    <!-- 포인트 행 2 -->
    <div class="point-item">
      <span class="point-name">신한 마일리지</span>
      <div class="point-right">
        <span class="point-value">4,280</span>
        <span class="arrow">›</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.illustration {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  overflow: hidden;
}

.progress-ring {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: conic-gradient(var(--color-primary) 0 68%, var(--color-bg-secondary) 68% 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

[data-theme='dark'] .progress-ring {
  background: conic-gradient(#e5ab5a 0 68%, rgba(244, 241, 234, 0.12) 68% 100%);
}

.ring-inner {
  width: 136px;
  height: 136px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

[data-theme='dark'] .ring-inner {
  background: #16130f;
  border-color: transparent;
}

.ring-value {
  font: 700 26px 'Pretendard', sans-serif;
  color: var(--color-primary);
  letter-spacing: -0.01em;
}

[data-theme='dark'] .ring-value {
  color: #e5ab5a;
}

.ring-label {
  font: 500 10px 'Pretendard', sans-serif;
  color: var(--color-text-tertiary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

[data-theme='dark'] .ring-label {
  color: rgba(244, 241, 234, 0.5);
}

.point-item {
  width: 100%;
  max-width: 260px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 8px rgba(36, 36, 42, 0.04);
}

[data-theme='dark'] .point-item {
  background: #211c16;
  border: 1px solid rgba(244, 241, 234, 0.08);
  box-shadow: none;
}

.point-name {
  font: 400 13px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
}

[data-theme='dark'] .point-name {
  color: #f4f1ea;
}

.point-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.point-value {
  font: 600 14px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
}

[data-theme='dark'] .point-value {
  color: #f4f1ea;
}

.arrow {
  font: 400 18px 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
}

[data-theme='dark'] .arrow {
  color: rgba(244, 241, 234, 0.5);
}
</style>
