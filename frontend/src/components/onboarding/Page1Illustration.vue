<template>
  <div class="illustration">
    <!-- 상단 행 (카드 썸네일) -->
    <div class="top-row">
      <div class="card-thumbnail"></div>
      <div class="text-skeleton">
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>

    <!-- 메인 카드 타일 -->
    <div class="main-card">
      <div class="card-info">
        <div class="card-issuer">신한 체크카드</div>
        <div class="card-chip"></div>
      </div>
      <div class="card-number">•••• 4412</div>
      <div class="card-alias">본인 카드</div>
    </div>

    <!-- 하단 물러난 행 -->
    <div class="bottom-row-1"></div>
    <div class="bottom-row-2"></div>
  </div>
</template>

<style scoped>
.illustration {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  transform: translateY(180px);
  margin-bottom: -180px;
}

.top-row {
  position: absolute;
  left: 16px;
  top: 12px;
  right: -40px;
  height: 48px;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  box-sizing: border-box;
  box-shadow: 0 2px 8px rgba(36, 36, 42, 0.06);
}

[data-theme='dark'] .top-row {
  background: #211c16;
  border-color: rgba(244, 241, 234, 0.08);
  box-shadow: none;
}

.card-thumbnail {
  width: 36px;
  height: 24px;
  border-radius: 4px;
  background: var(--color-bg-subtle);
  flex-shrink: 0;
}

[data-theme='dark'] .card-thumbnail {
  background: rgba(244, 241, 234, 0.16);
}

.text-skeleton {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}

.skeleton-line {
  height: 8px;
  border-radius: 4px;
  background: var(--color-border);
}

[data-theme='dark'] .skeleton-line {
  background: rgba(244, 241, 234, 0.22);
}

.skeleton-line.short {
  width: 60%;
  background: var(--color-bg-secondary);
}

[data-theme='dark'] .skeleton-line.short {
  background: rgba(244, 241, 234, 0.12);
}

.main-card {
  position: absolute;
  left: 14px;
  top: 62px;
  width: 296px;
  height: 140px;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  padding: 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 4px 12px rgba(36, 36, 42, 0.08);
}

[data-theme='dark'] .main-card {
  background: #2a231b;
  border: 1px solid rgba(244, 241, 234, 0.12);
  box-shadow: 0 22px 46px rgba(0, 0, 0, 0.5);
}

.card-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-issuer {
  font: 500 13px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
}

[data-theme='dark'] .card-issuer {
  color: #f4f1ea;
}

.card-chip {
  width: 32px;
  height: 20px;
  border-radius: 4px;
  background: var(--color-primary);
}

[data-theme='dark'] .card-chip {
  background: #86d9a8;
}

.card-number {
  font: 500 16px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
  letter-spacing: 0.1em;
}

[data-theme='dark'] .card-number {
  color: #f4f1ea;
}

.card-alias {
  font: 400 12px 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
}

[data-theme='dark'] .card-alias {
  color: rgba(244, 241, 234, 0.6);
}

.bottom-row-1 {
  display: none;
}

.bottom-row-2 {
  display: none;
}
</style>
