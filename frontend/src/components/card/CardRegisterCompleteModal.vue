<script setup>
import Icon from '@/components/common/Icon.vue';

// 서버가 카드번호로 매칭한 카드 상품 정보.
// 사용자가 카드명을 직접 고르지 않으므로, 어떤 카드로 등록됐는지 여기서 확인시켜준다.
const props = defineProps({
  card: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  'close',
  'confirm'
]);


const confirm = () => {

  emit('confirm');

};


const close = () => {

  emit('close');

};

</script>


<template>

<div class="overlay">

  <section class="modal">

    <div class="success-icon">
      <Icon name="check" size="lg" />
    </div>

    <h2>
      카드 등록 완료
    </h2>


    <!-- 매칭된 카드 상품을 보여준다 -->
    <div v-if="props.card" class="matched-card">

      <img
        v-if="props.card.imageUrl"
        :src="props.card.imageUrl"
        :alt="props.card.cardName"
        class="card-image"
      />

      <p class="card-name">
        {{ props.card.cardName }}
      </p>

      <p class="card-meta">
        {{ props.card.issuerName }}
        <span v-if="props.card.maskedCardNumber">
          · {{ props.card.maskedCardNumber }}
        </span>
      </p>

    </div>


    <p>
      카드 등록이 완료되었습니다.
    </p>


    <button
      @click="confirm"
    >
      확인
    </button>

  </section>

</div>

</template>


<style scoped>

.overlay {

position:fixed;
inset:0;
width:100%;
max-width:480px;
left:50%;
transform:translateX(-50%);
margin:0 auto;

background:rgba(0,0,0,.4);

display:flex;
justify-content:center;
align-items:center;

z-index:3000;

}


.modal {

background:var(--color-surface);

width:90%;
max-width:480px;

padding:24px;

border-radius:20px;

text-align:center;

box-sizing: border-box;

}

.success-icon {

  width: 60px;
  height: 60px;
  margin: 0 auto 12px;

  border-radius: 50%;

  display: flex;
  justify-content: center;
  align-items: center;

  color: var(--color-primary-dark);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);

}


.matched-card {

  margin: 0 0 12px;
  padding: 16px;

  border-radius: 12px;
  border: 1px solid var(--color-border);

  background: var(--color-bg-subtle);

}

.modal > p {
  color: var(--color-text-primary);
}

.card-image {

  width: 96px;

  max-width: 100%;

  margin: 0 auto 8px;

  display: block;

  border-radius: 6px;

}

.card-name {

  margin: 0 0 4px;

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

}

.card-meta {

  margin: 0;

  font-size: var(--font-xs);

  color: var(--color-text-secondary);

}


button {

width:100%;

height:48px;

margin-top:20px;

border:none;

border-radius:12px;

background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);

}

</style>
