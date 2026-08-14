<script setup>
import { ref, computed, onMounted } from 'vue';
import { useCardStore } from '@/stores/cardStore';

const emit = defineEmits([
  'close',
  'select',
]);

const cardStore = useCardStore();

const maskCardNumber = (number) => {
  if (!number) return '';

  const lastFour = number.slice(-4);

  return `${lastFour.slice(0, 3)}*`;
};

// 회원이 실제로 등록한 보유카드(GET /api/user-cards)를 그대로 쓴다.
// id 0은 "전체"를 뜻하는 화면 전용 값이라 서버 userCardId와 겹치지 않는다(서버 id는 1부터).
const cards = computed(() => [
  {
    id: 0,
    name: '전체',
    isAll: true,
  },
  ...cardStore.cards.map((card) => ({
    id: card.id,
    name: card.name,
    company: card.company,
    // 보유카드는 모두 로그인 회원 본인 명의라 소유자 표기는 고정이다.
    owner: '본인',
    number: maskCardNumber(card.cardNumber),
    image: card.image,
  })),
]);

// 이미 목록을 받아둔 화면에서 열렸으면 다시 부르지 않는다.
onMounted(() => {
  if (!cardStore.cards.length) {
    cardStore.loadCards().catch((error) => {
      console.error('보유카드 목록 조회 실패:', error);
    });
  }
});

const selectedCardId = ref(0);

const selectCard = (card) => {
  selectedCardId.value = card.id;
};

const apply = () => {
  const selected = cards.value.find(
    (card) => card.id === selectedCardId.value,
  );

  emit('select', selected);
};

const close = () => {
  emit('close');
};
</script>

<template>
  <div class="overlay" @click.self="close">

    <div class="bottom-sheet">

      <div class="handle" />

      <h2>카드 선택</h2>

      <div
        v-for="card in cards"
        :key="card.id"
        class="card-option"
        :class="{ selected: selectedCardId === card.id }"
        @click="selectCard(card)"
      >

        <!-- 전체 -->
        <template v-if="card.isAll">
          <span class="all-text">
            전체
          </span>
        </template>

        <!-- 카드 -->
        <template v-else>

          <img
            :src="card.image"
            class="card-image"
          />

          <div class="card-info">

            <div class="card-name">
              {{ card.name }}
            </div>

            <div class="company">
              {{ card.company }}
            </div>

            <div class="number">
              {{ card.owner }}
              {{ card.number }}
            </div>

          </div>

        </template>

      </div>

      <button
        class="apply-button"
        @click="apply"
      >
        적용
      </button>

    </div>

  </div>
</template>

<style scoped>
.overlay{
    position:fixed;
    inset:0;
    width:100%;
    max-width:480px;
    left:50%;
    transform:translateX(-50%);
    margin:0 auto;
    background:rgba(0,0,0,.35);

    display:flex;
    justify-content:center;
    align-items:flex-end;

    z-index:999;
}

.bottom-sheet{
    width:100%;
    max-width:480px;
    max-height:80vh;

    background:white;

    border-radius:24px 24px 0 0;

    padding:20px;

    overflow:auto;

    box-sizing: border-box;
}

.handle{
    width:42px;
    height:5px;

    background:#ddd;

    border-radius:99px;

    margin:0 auto 18px;
}

h2{
    margin-bottom:20px;
}

.card-option{

    display:flex;
    align-items:center;

    gap:16px;

    padding:16px;

    margin-bottom:14px;

    border:1px solid #E5E5E5;

    border-radius:16px;

    cursor:pointer;

    transition:.2s;
}

.card-option.selected{

    border:2px solid #4F46E5;

    box-shadow:0 3px 12px rgba(0,0,0,.08);

}

.card-image{

    width:72px;

    border-radius:10px;

}

.card-info{

    display:flex;
    flex-direction:column;
    gap:4px;

}

.card-name{

    font-weight:700;

}

.company{

    color:#666;
    font-size:14px;

}

.number{

    color:#888;
    font-size:13px;

}

.all-text{

    font-weight:600;
    font-size:16px;

}

.apply-button{

    width:100%;

    height:48px;

    border:none;

    border-radius:12px;

    margin-top:20px;

    color:white;

    background:#4F46E5;

    cursor:pointer;

}
</style>