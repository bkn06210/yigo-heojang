import { defineStore } from 'pinia';
import { ref } from 'vue';


export const useCardStore = defineStore(
  'card',
  () => {


    // 등록된 카드 목록
    const cards = ref([]);



    // 카드 추가
    const addCard = (card) => {

      cards.value.push(card);

    };



    // 카드 삭제
    const removeCard = (id) => {

      cards.value = cards.value.filter(
        card => card.id !== id
      );

    };

    



    // 카드 목록 초기화
    const clearCards = () => {

      cards.value = [];

    };



    return {

      cards,

      addCard,

      removeCard,

      clearCards,

    };


  }
);