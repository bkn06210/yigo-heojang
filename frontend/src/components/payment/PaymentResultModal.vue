<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
  success: {
    type: Boolean,
    default: true,
  },
  amount: {
    type: Number,
    default: 0,
  },
  card: {
    type: Object,
    default: null,
  },
  membershipBenefit: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(['close']);

const router = useRouter();

const close = () => {
  emit('close');
};

const goToPoint = () => {
  if (props.membershipBenefit?.route) {
    router.push(props.membershipBenefit.route);
  }
};
</script>

<template>

  <div class="overlay">

    <div class="modal">

      <template v-if="success">

        <div class="success-icon">✓</div>

        <h2>결제가 완료되었습니다.</h2>

        <section class="payment-info">
          <p>{{ card?.name }}</p>
          <strong>{{ amount.toLocaleString() }}원</strong>
        </section>

        <!-- 결제한 가맹점과 매칭되는 멤버십이 있을 때만 안내 -->
        <section v-if="membershipBenefit" class="benefit-box">
          <p>{{ membershipBenefit.name }} 포인트 적립 내역은 포인트 관리에서 확인할 수 있어요.</p>
          <button class="point-button" @click="goToPoint">포인트 확인하기</button>
        </section>
        <p v-else class="notice">카드 혜택 적용 여부는 카드사 앱에서 확인해주세요.</p>

      </template>

      <template v-else>

        <div class="fail-icon">!</div>

        <h2>결제에 실패했습니다.</h2>

        <p class="notice">잠시 후 다시 시도해주세요.</p>

      </template>

      <button class="confirm-button" @click="close">확인</button>

    </div>

  </div>

</template>

<style scoped>

.overlay{

  position:fixed;
  inset:0;

  background:rgba(0,0,0,.45);

  display:flex;
  justify-content:center;
  align-items:center;

  z-index:1000;

}

.modal{

  width:320px;

  border-radius: var(--radius-xl);

  padding: var(--space-xl);

  text-align:center;

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

}

[data-theme="dark"] .modal{

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}

.modal h2{

  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;

}

.success-icon{

  width:60px;
  height:60px;

  margin: 0 auto var(--space-md);

  border-radius: var(--radius-full);

  display:flex;
  justify-content:center;
  align-items:center;

  font-size:32px;

  color: var(--color-primary-dark);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);

}

.fail-icon{

  width:60px;
  height:60px;

  margin: 0 auto var(--space-md);

  border-radius: var(--radius-full);

  display:flex;
  justify-content:center;
  align-items:center;

  font-size:32px;

  color: var(--color-coral);
  background: rgba(168, 78, 104, 0.12);

}

.payment-info{

  margin: var(--space-xl) 0;

}

.payment-info p{

  margin: 0 0 var(--space-xs);

  color: var(--color-text-secondary);

  font-size: var(--font-sm);

}

.payment-info strong{

  font-size: var(--font-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);

}

.benefit-box{

  margin-bottom: var(--space-md);

  padding: var(--space-md);

  border-radius: var(--radius-md);

  background: var(--color-bg);

}

.benefit-box p{

  margin: 0 0 var(--space-sm);

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

}

.point-button{

  width:100%;
  height:44px;

  border:none;

  border-radius: var(--radius-md);

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  cursor:pointer;

  transition: var(--transition-fast);

}

.point-button:hover{

  opacity: 0.9;

}

.notice{

  margin: var(--space-lg) 0;

  font-size: var(--font-sm);
  line-height:1.5;

  color: var(--color-text-secondary);

}

.confirm-button{

  width:100%;
  height:48px;

  margin-top: var(--space-sm);

  border:none;

  border-radius: var(--radius-md);

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  cursor:pointer;

  transition: var(--transition-fast);

}

.confirm-button:hover{

  opacity: 0.9;

}

</style>
