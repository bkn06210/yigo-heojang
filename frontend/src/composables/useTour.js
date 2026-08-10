export const useTour = () => {
  const isTourCompleted = (pageKey) => {
    return localStorage.getItem(`tour-completed-${pageKey}`) === 'true';
  };

  const markTourCompleted = (pageKey) => {
    localStorage.setItem(`tour-completed-${pageKey}`, 'true');
  };

  const startTour = (pageKey, elements) => {
    // 투어 오버레이 생성
    const overlay = document.createElement('div');
    overlay.className = 'tour-overlay';
    overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      z-index: 9999;
    `;

    // 투어 레이블들 생성
    elements.forEach((element) => {
      const el = document.querySelector(element.selector);
      if (!el) return;

      const rect = el.getBoundingClientRect();
      const label = document.createElement('div');
      label.className = 'tour-label';
      label.style.cssText = `
        position: fixed;
        top: ${rect.top + rect.height / 2 - 25}px;
        left: ${rect.left + rect.width + 10}px;
        background: white;
        padding: 8px 12px;
        border-radius: 8px;
        font-size: 12px;
        font-weight: 600;
        color: #24242A;
        z-index: 10000;
        white-space: nowrap;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      `;
      label.textContent = element.label;
      overlay.appendChild(label);

      // 요소 하이라이트
      el.style.boxShadow = `0 0 0 3px #EFDD39`;
      el.style.transition = 'box-shadow 0.3s ease';
    });

    // 닫기 버튼
    const closeBtn = document.createElement('button');
    closeBtn.textContent = '닫기';
    closeBtn.style.cssText = `
      position: fixed;
      bottom: 30px;
      left: 50%;
      transform: translateX(-50%);
      padding: 12px 24px;
      background: #EFDD39;
      color: #24242A;
      border: none;
      border-radius: 20px;
      font-weight: 600;
      cursor: pointer;
      z-index: 10001;
      font-size: 14px;
    `;

    closeBtn.addEventListener('click', () => {
      overlay.remove();
      elements.forEach((element) => {
        const el = document.querySelector(element.selector);
        if (el) {
          el.style.boxShadow = '';
        }
      });
      markTourCompleted(pageKey);
    });

    overlay.appendChild(closeBtn);
    document.body.appendChild(overlay);
  };

  const startHomeTour = () => {
    startTour('home', [
      { selector: '[data-tour="chat-button"]', label: '💬 채팅봇' },
      { selector: '[data-tour="notification-button"]', label: '🔔 알림' },
    ]);
  };

  const startCardListTour = () => {
    startTour('cardList', [
      { selector: '[data-tour="card-register"]', label: '➕ 카드 등록' },
      { selector: '[data-tour="card-item"]', label: '👆 좌측 스와이프로 고정' },
    ]);
  };

  const startPaymentTour = () => {
    startTour('payment', [
      { selector: '[data-tour="card-carousel"]', label: '👈👉 좌우 스와이프' },
      { selector: '[data-tour="card-flip"]', label: '☝️ 위로 스와이프 = 비밀번호' },
      { selector: '[data-tour="recommend-button"]', label: '✨ AI 추천 카드' },
    ]);
  };

  return {
    isTourCompleted,
    markTourCompleted,
    startHomeTour,
    startCardListTour,
    startPaymentTour,
  };
};
