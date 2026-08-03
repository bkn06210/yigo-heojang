<script setup>

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


</script>

<template>


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

</template>

<style scoped>


.overlay{

  position:fixed;

  inset:0;

  display:flex;

  justify-content:center;

  align-items:center;

  background:rgba(0,0,0,0.45);

  z-index:1000;

}

.modal{

  width:320px;

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

  align-items:center;

  font-size:32px;

}

.payment-info{

  margin:24px 0;

}

.payment-info strong{

  font-size:24px;

}

.benefit-box{

  padding:16px;

  border-radius:14px;

  margin-bottom:16px;

}

.point-button{

  width:100%;

  height:44px;

  border-radius:10px;

}

.notice{

  font-size:14px;

  line-height:1.5;

  margin:20px 0;

}

.confirm-button{

  width:100%;

  height:48px;

  border-radius:12px;

}


</style>