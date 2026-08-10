import { defineStore } from 'pinia';
import { ref } from 'vue';

/*
| 결제 Store
| 결제 추천 화면 → QR 결제 화면 → 결제 완료 모달
|
| 결제 과정에서 필요한 데이터를 저장하는 Store
|
| 추후 API 연결 시에도 그대로 사용 가능
*/

export const usePaymentStore = defineStore('payment', () => {

  /*
  | 선택한 카드
  | 추천 결과에서 사용자가 선택한 카드
  |
  | 예)
  | {
  |   id: 1,
  |   name: 'KB My WE:SH',
  |   image: '/images/card.png'
  | }
  |
  */
  const selectedCard = ref(null);


  /*
  | 결제 금액
  */
  const paymentAmount = ref(0);


  /*
  | 멤버십 정보
  |
  | 추천 결과에서 판단된 멤버십 정보
  |
  | 없으면 null
  |
  */
  const membershipBenefit = ref(null);


  /*
  | 결제 정보 저장
  |
  | 추천 화면에서 결제하기 버튼 클릭 시 호출
  |
  */
  const setPaymentInfo = (card, amount, membership) => {

    selectedCard.value = card;

    paymentAmount.value = amount;

    membershipBenefit.value = membership;

  };


  /*
  | 결제 정보 초기화
  |
  | 결제 종료 후 호출 예정
  |
  */
  const clearPaymentInfo = () => {

    selectedCard.value = null;

    paymentAmount.value = 0;

    membershipBenefit.value = null;

  };


  return {

    selectedCard,

    paymentAmount,

    membershipBenefit,

    setPaymentInfo,

    clearPaymentInfo

  };

});
