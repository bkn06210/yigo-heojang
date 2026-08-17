import { defineStore } from 'pinia';
import { ref } from 'vue';
import { deleteUserCard, getCardMonthlyStatuses, getUserCards, getPoints, getMemberships, getPointUsagePlaces } from '@/api/walletApi';


export const useCardStore = defineStore(
  'card',
  () => {


    // 등록된 카드 목록
    const cards = ref([]);

    // 금융 포인트 목록
    const points = ref([]);

    // 등록된 멤버십 목록
    const memberships = ref([]);

    // PR #25 연동: 홈과 카드 목록이 공유하는 서버 브리핑 및 조회 상태다.
    const briefing = ref(null);
    const loading = ref(false);
    const error = ref('');

    // PR #25 연동: 카드 현황 API 응답을 기존 카드 UI가 사용하는 필드 형태로 변환한다.
    const toCardViewModel = (status, previousCard) => ({
      // PR #32 연동: 목록 API에서 받은 카드 유형·등록일 등 기본 필드를 실적 결합 후에도 유지한다.
      ...previousCard,
      id: status.userCardId,
      name: status.cardName,
      company: previousCard?.company || '보유카드',
      owner: previousCard?.owner || '',
      cardNumber: previousCard?.cardNumber || '',
      image: previousCard?.image || '',
      pinned: previousCard?.pinned || false,
      currentAmount: status.currentPerformanceAmount,
      targetAmount: status.targetPerformance,
      remainAmount: status.remainingPerformance,
      achievementRate: status.achievementRate,
      performanceMet: status.performanceMet,
      sharedLimit: status.sharedLimit,
      sharedLimitUsed: status.sharedLimitUsed,
      benefits: status.benefitsSummary || [],
      yearMonth: status.yearMonth,
    });

    // PR #32 연동: 기본 목록 API 응답을 기존 카드 UI의 필드 구조로 변환한다.
    const toBasicCardViewModel = (card) => ({
      id: card.userCardId,
      cardId: card.cardId,
      name: card.cardName,
      company: card.issuerName,
      cardType: card.cardType,
      owner: '',
      cardNumber: card.maskedCardNumber,
      image: card.imageUrl || '',
      pinned: Boolean(card.representative),
      registeredAt: card.registeredAt,
      currentAmount: 0,
      targetAmount: 0,
      remainAmount: 0,
      achievementRate: 0,
      performanceMet: false,
      benefits: [],
    });

    // PR #32 연동: 기본 카드 목록에 PR #25의 실적 결과를 userCardId 기준으로 결합한다.
    const loadCards = async (params = {}) => {
      loading.value = true;
      error.value = '';
      try {
        const basicResponse = await getUserCards();
        const basicCards = (basicResponse?.userCards || []).map(toBasicCardViewModel);
        cards.value = basicCards;

        try {
          const statusResponse = await getCardMonthlyStatuses(params);
          const statusById = new Map(
            (statusResponse?.cards || []).map((status) => [Number(status.userCardId), status])
          );
          cards.value = basicCards.map((card) => {
            const status = statusById.get(Number(card.id));
            return status ? toCardViewModel(status, card) : card;
          });
          briefing.value = statusResponse?.briefing || null;
        } catch (statusError) {
          // PR #32 연동: 실적 API가 실패해도 기본 보유카드 목록은 화면에 유지한다.
          console.error('카드 실적 조회 실패', statusError);
        }
        return basicResponse;
      } catch (requestError) {
        error.value = requestError?.response?.data?.message || requestError?.message || '보유카드 목록을 불러오지 못했습니다.';
        throw requestError;
      } finally {
        loading.value = false;
      }
    };

    // PR #25 연동: GET /api/cards/monthly-status 결과로 카드 Store를 갱신한다.
    const loadMonthlyStatuses = async (params = {}) => {
      loading.value = true;
      error.value = '';
      try {
        const response = await getCardMonthlyStatuses(params);
        const previousById = new Map(cards.value.map((card) => [Number(card.id), card]));
        cards.value = (response?.cards || []).map((status) =>
          toCardViewModel(status, previousById.get(Number(status.userCardId)))
        );
        briefing.value = response?.briefing || null;
        return response;
      } catch (requestError) {
        error.value = requestError?.response?.data?.message || requestError?.message || '카드 현황을 불러오지 못했습니다.';
        throw requestError;
      } finally {
        loading.value = false;
      }
    };

    // 금융 포인트 조회
    const loadPoints = async () => {
      loading.value = true;
      error.value = '';
      try {
        const response = await getPoints();
        points.value = (response?.points || response?.pointWallets || response || [])
          .filter((point) => point.providerType === 'FINANCIAL_POINT')
          .map((point) => ({
            id: point.pointWalletId,
            name: point.providerName,
            point: point.totalPoint,
          }));
        return response;
      } catch (requestError) {
        error.value = requestError?.response?.data?.message || requestError?.message || '포인트를 불러오지 못했습니다.';
        throw requestError;
      } finally {
        loading.value = false;
      }
    };

    // 멤버십 목록 조회
    const loadMemberships = async () => {
      loading.value = true;
      error.value = '';
      try {
        const response = await getMemberships();
        const rawMemberships = response?.memberships || response || [];

        // 각 멤버십별로 usagePlaces 조회
        const membershipsWithPlaces = await Promise.all(
          rawMemberships.map(async (m) => {
            let usagePlaces = m.usagePlaces || [];

            // usagePlaces가 없으면 API로 조회
            if (usagePlaces.length === 0 && m.pointProviderId) {
              try {
                const placesResponse = await getPointUsagePlaces(m.pointProviderId);
                usagePlaces = placesResponse?.usagePlaces || placesResponse || [];
              } catch (err) {
                console.warn(`usagePlaces 조회 실패 (providerId: ${m.pointProviderId}):`, err);
              }
            }

            return {
              id: m.membershipRegisterId,
              providerId: m.pointProviderId,
              name: m.providerName,
              providerName: m.providerName,
              point: m.totalPoint || 0,
              logoImageUrl: m.logoImageUrl,
              usagePlaces: usagePlaces,
              partnerWebsiteUrl: m.partnerWebsiteUrl,
            };
          })
        );

        memberships.value = membershipsWithPlaces;
        return response;
      } catch (requestError) {
        error.value = requestError?.response?.data?.message || requestError?.message || '멤버십을 불러오지 못했습니다.';
        throw requestError;
      } finally {
        loading.value = false;
      }
    };



    // 카드 추가
    const addCard = (card) => {
      // 카드 등록 API가 기존 목카드를 반환해도 화면에 같은 카드가 중복 표시되지 않도록 갱신한다.
      const existingIndex = cards.value.findIndex(
        existingCard => Number(existingCard.id) === Number(card.id)
      );

      if (existingIndex >= 0) {
        cards.value.splice(existingIndex, 1, card);
        return;
      }

      cards.value.push(card);

    };



    // PR #34 연동: 서버의 소프트 삭제가 성공한 뒤에만 로컬 카드 목록에서도 제거한다.
    const removeCard = async (id) => {
      await deleteUserCard(id);
      cards.value = cards.value.filter(
        card => Number(card.id) !== Number(id)
      );
    };

    



    // 카드 목록 초기화
    const clearCards = () => {

      cards.value = [];

    };

    // 카드를 추천 순서대로 재정렬
    const reorderCardsByRecommendation = (recommendedCardIds) => {
      if (!recommendedCardIds || recommendedCardIds.length === 0) return;

      // 숫자 타입으로 통일 (c.id가 문자열일 수 있으므로)
      const recommendedSet = new Set(recommendedCardIds.map(id => Number(id)));
      const reorderedCards = [
        ...recommendedCardIds
          .map(id => cards.value.find(c => Number(c.id) === Number(id)))
          .filter(Boolean),
        ...cards.value.filter(c => !recommendedSet.has(Number(c.id)))
      ];
      cards.value = reorderedCards;
    };

    return {

      cards,
      points,
      memberships,

      // PR #25 연동 상태와 조회 함수를 화면에서 함께 사용한다.
      briefing,
      loading,
      error,
      loadMonthlyStatuses,
      loadPoints,
      loadMemberships,
      // PR #32 연동: 카드 목록 화면 전용 기본정보·실적 결합 조회 함수다.
      loadCards,

      addCard,

      removeCard,

      clearCards,

      reorderCardsByRecommendation,

    };


  }
);
// 07_25 연동 변경: 보유카드 목록·등록·실적 API 결과를 하나의 화면 상태로 병합한다.
