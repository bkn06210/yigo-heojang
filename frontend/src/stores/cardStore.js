import { defineStore } from 'pinia';
import { ref } from 'vue';


export const useCardStore = defineStore(
  'card',
  () => {


    // 등록된 카드 목록
    const cards = ref([]);

    // 포인트 목록
    const points = ref([]);



    // 카드 추가 (포인트 자동 생성)
    const addCard = (card) => {

      cards.value.push(card);

      // 카드 추가 시 자동으로 포인트 생성
      const newPoint = {
        id: `point-${card.id}`,
        name: `${card.company}포인트`,
        balance: Math.floor(Math.random() * 100000),
        cardId: card.id
      };
      points.value.push(newPoint);

    };



    // 카드 삭제
    const removeCard = (id) => {

      cards.value = cards.value.filter(
        card => card.id !== id
      );

      // 해당 카드의 포인트도 함께 삭제
      points.value = points.value.filter(
        point => point.cardId !== id
      );

    };

    



    // 카드 목록 초기화
    const clearCards = () => {

      cards.value = [];

    };



    return {

      cards,

      points,

      addCard,

      removeCard,

      clearCards,

    };


  }
);