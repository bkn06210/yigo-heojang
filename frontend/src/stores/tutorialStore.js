import { ref, computed } from 'vue';
import { defineStore } from 'pinia';

export const useTutorialStore = defineStore('tutorial', () => {
  const isCompleted = ref(localStorage.getItem('tutorialCompleted') === 'true');
  const isActive = ref(false);
  const currentStep = ref(0);

  const tutorialSteps = [
    {
      page: 'main',
      title: '당신의 카드, 똑똑하게 관리하세요',
      highlights: [
        {
          id: 'bottom-nav-payment',
          text: '결제 페이지에서\n카드와 추천을 확인하세요',
          position: 'top'
        },
        {
          id: 'bottom-nav-point',
          text: '포인트와 멤버십은\n혜택 페이지에서',
          position: 'top'
        }
      ]
    },
    {
      page: 'card',
      title: '카드를 등록하고 관리하세요',
      highlights: [
        {
          id: 'card-register-button',
          text: '버튼을 눌러\n카드를 추가하세요',
          position: 'bottom'
        },
        {
          id: 'card-list',
          text: '등록된 카드들을\n여기서 관리합니다',
          position: 'bottom'
        }
      ]
    },
    {
      page: 'payment',
      title: '똑똑한 결제, 지금 시작',
      highlights: [
        {
          id: 'cards-container',
          text: '카드를 스와이프해\n선택하세요',
          position: 'bottom'
        },
        {
          id: 'recommendation-section',
          text: 'AI가 최고의 카드를\n추천해드립니다',
          position: 'bottom'
        },
        {
          id: 'payment-info-section',
          text: '결제 정보를 입력하고\n결제하세요',
          position: 'bottom'
        }
      ]
    }
  ];

  const startTutorial = () => {
    if (!isCompleted.value) {
      isActive.value = true;
      currentStep.value = 0;
    }
  };

  const nextStep = () => {
    if (currentStep.value < tutorialSteps.length - 1) {
      currentStep.value += 1;
    } else {
      completeTutorial();
    }
  };

  const completeTutorial = () => {
    isActive.value = false;
    isCompleted.value = true;
    currentStep.value = 0;
    localStorage.setItem('tutorialCompleted', 'true');
  };

  const skipTutorial = () => {
    completeTutorial();
  };

  const resetTutorial = () => {
    isCompleted.value = false;
    isActive.value = false;
    currentStep.value = 0;
    localStorage.removeItem('tutorialCompleted');
  };

  const getCurrentStep = computed(() => tutorialSteps[currentStep.value]);

  return {
    isCompleted,
    isActive,
    currentStep,
    tutorialSteps,
    startTutorial,
    nextStep,
    completeTutorial,
    skipTutorial,
    resetTutorial,
    getCurrentStep
  };
});
