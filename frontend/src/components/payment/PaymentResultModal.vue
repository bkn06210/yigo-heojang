<script setup>
<<<<<<< HEAD
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
=======

/*
| 결제 결과 모달
|
| 역할:
| - 결제 성공/실패 결과 표시
| - 결제 성공 후 혜택 관리 페이지 연결
|
| props
| - success : 결제 성공 여부
| - amount : 결제 금액
| - card : 선택 카드 정보
| - membershipBenefit : 멤버십 정보
*/


import { useRouter } from 'vue-router';


const router = useRouter();


const props = defineProps({

  // 결제 성공 여부
  success: {

    type:Boolean,

    default:true

  },


  // 결제 금액
  amount: {

    type:Number,

    default:0

  },


  // 선택한 카드 정보
  card: {

    type:Object,

    default:null

  },

  // 멤버십 정보
  //
  // 예:
  // {
  //   name:'CJ ONE',
  //   route:'/point/cj-one'
  // }
  membershipBenefit: {

    type:Object,

    default:null

  }


});

const emit = defineEmits([

  'close'

]);

// 모달 닫기
const closeModal = ()=>{


  emit('close');


};

// 포인트 관리 이동
const goPoint = ()=>{


  if(props.membershipBenefit?.route){


    router.push(
      props.membershipBenefit.route
    );

  }


};


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</script>

<template>

<<<<<<< HEAD
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

=======

<div class="overlay">


  <div class="modal">


    <!-- 성공 화면 -->

    <template v-if="success">


      <div class="success-icon">

        ✓

      </div>

      <h2>

        결제가 완료되었습니다.

      </h2>

      <section class="payment-info">


        <p>

          {{ card?.name }}

        </p>


        <strong>

          {{ amount.toLocaleString() }}원

        </strong>

      </section>

      <!-- 멤버십 등록된 경우 -->

      <section
        v-if="membershipBenefit"
        class="benefit-box"
      >

        <p>

          {{ membershipBenefit.name }}

          포인트 적립 내역은

          포인트 관리에서 확인할 수 있어요.

        </p>


        <button
          class="point-button"
          @click="goPoint"
        >

          포인트 확인하기

        </button>


      </section>


      <!-- 멤버십 없는 경우 -->

      <p
        v-else
        class="notice"
      >

        카드 혜택 적용 여부는
        카드사 앱에서 확인해주세요.

      </p>

    </template>

    <!-- 실패 화면 -->

    <template v-else>


      <div class="fail-icon">

        !

      </div>

      <h2>

        결제에 실패했습니다.

      </h2>

      <p class="notice">

        잠시 후 다시 시도해주세요.

      </p>


    </template>

    <button

      class="confirm-button"

      @click="closeModal"

    >

      확인

    </button>

  </div>


</div>

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</template>

<style scoped>

<<<<<<< HEAD
.overlay{

  position:fixed;
  inset:0;

  background:rgba(0,0,0,.45);

  display:flex;
  justify-content:center;
  align-items:center;

=======

.overlay{

  position:fixed;

  inset:0;

  display:flex;

  justify-content:center;

  align-items:center;

  background:rgba(0,0,0,0.45);

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  z-index:1000;

}

.modal{

  width:320px;

<<<<<<< HEAD
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
=======
  padding:28px;

  background:white;

  border-radius:20px;

  text-align:center;

}

.success-icon,
.fail-icon{

  width:60px;

  height:60px;

  margin:0 auto 16px;

  border-radius:50%;

  display:flex;

  justify-content:center;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  align-items:center;

  font-size:32px;

<<<<<<< HEAD
  color: var(--color-coral);
  background: rgba(168, 78, 104, 0.12);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.payment-info{

<<<<<<< HEAD
  margin: var(--space-xl) 0;

}

.payment-info p{

  margin: 0 0 var(--space-xs);

  color: var(--color-text-secondary);

  font-size: var(--font-sm);
=======
  margin:24px 0;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.payment-info strong{

<<<<<<< HEAD
  font-size: var(--font-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
=======
  font-size:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.benefit-box{

<<<<<<< HEAD
  margin-bottom: var(--space-md);

  padding: var(--space-md);

  border-radius: var(--radius-md);

  background: var(--color-bg);

}

.benefit-box p{

  margin: 0 0 var(--space-sm);

  font-size: var(--font-sm);

  color: var(--color-text-secondary);
=======
  padding:16px;

  border-radius:14px;

  margin-bottom:16px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.point-button{

  width:100%;
<<<<<<< HEAD
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
=======

  height:44px;

  border-radius:10px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.notice{

<<<<<<< HEAD
  margin: var(--space-lg) 0;

  font-size: var(--font-sm);
  line-height:1.5;

  color: var(--color-text-secondary);
=======
  font-size:14px;

  line-height:1.5;

  margin:20px 0;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.confirm-button{

  width:100%;
<<<<<<< HEAD
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
=======

  height:48px;

  border-radius:12px;

}


</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
