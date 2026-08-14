<template>
  <div class="illustration">
    <!-- 사용자 메시지 버블 -->
    <div class="user-bubble">이번 달 카페에 얼마 썼어?</div>

    <!-- AI 응답 버블 -->
    <div class="ai-bubble">
      <div class="ai-amount">42,300원</div>
      <div class="ai-divider"></div>
      <div class="ai-stats">
        <div class="stat-row">
          <span class="stat-label">지난달 대비</span>
          <span class="stat-value">−18%</span>
        </div>
        <div class="stat-row">
          <span class="stat-label">다음 달 예상</span>
          <span class="stat-value">51,000원</span>
        </div>
      </div>
    </div>

    <!-- 제안 칩 -->
    <div class="suggestion-chips">
      <div class="chip">다음 달 예상은?</div>
      <div class="chip">추천 카드</div>
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
  justify-content: center;
  gap: 12px;
  padding: 0;
  overflow: hidden;
}

.user-bubble {
  align-self: flex-end;
  max-width: 250px;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  background: var(--color-primary);
  font: 500 14px / 1.5 'Pretendard', sans-serif;
  color: #24242A;
  word-wrap: break-word;
}

[data-theme='dark'] .user-bubble {
  background: #86d9a8;
  color: #16130f;
}

.ai-bubble {
  align-self: flex-start;
  width: 270px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(36, 36, 42, 0.06);
}

[data-theme='dark'] .ai-bubble {
  background: #211c16;
  border: 1px solid rgba(244, 241, 234, 0.09);
  box-shadow: none;
}

.ai-amount {
  font: 700 24px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
  line-height: 1.2;
  letter-spacing: -0.01em;
}

[data-theme='dark'] .ai-amount {
  color: #f4f1ea;
}

.ai-divider {
  height: 1px;
  background: var(--color-border);
}

[data-theme='dark'] .ai-divider {
  background: rgba(244, 241, 234, 0.1);
}

.ai-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
}

.stat-label {
  font: 400 12px 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
}

[data-theme='dark'] .stat-label {
  color: rgba(244, 241, 234, 0.5);
}

.stat-value {
  font: 600 13px 'Pretendard', sans-serif;
  color: var(--color-primary);
}

[data-theme='dark'] .stat-value {
  color: #86d9a8;
}

.stat-row:last-child .stat-value {
  font: 500 13px 'Pretendard', sans-serif;
  color: var(--color-text-primary);
}

[data-theme='dark'] .stat-row:last-child .stat-value {
  color: rgba(244, 241, 234, 0.85);
}

.suggestion-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
  align-self: flex-start;
}

.chip {
  padding: 8px 12px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  font: 400 12px 'Pretendard', sans-serif;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
}

[data-theme='dark'] .chip {
  background: #211c16;
  border: 1px solid rgba(244, 241, 234, 0.18);
  color: rgba(244, 241, 234, 0.7);
}
</style>
