<script setup>

import { useRouter } from 'vue-router';
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


const router = useRouter();



const props = defineProps({

  success: {
    type:Boolean,
    default:true
  },


  amount:{
    type:Number,
    default:0
  },


  card:{
    type:Object,
    default:null
  },


  membershipBenefit:{
    type:Object,
    default:null
  }

});


// 확인 버튼
const closeModal = ()=>{

  // 추후 emit으로 변경 가능

};


// 포인트 페이지 이동
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


<!-- 성공 -->

<template v-if="success">


<div class="icon">
<<<<<<< HEAD
<Icon name="check" size="lg" />
=======
✓
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</div>


<h2>
결제가 완료되었습니다.
</h2>



<section class="payment-info">


<p>
{{card?.name}}
</p>


<strong>
{{amount.toLocaleString()}}원
</strong>


</section>



<!-- 멤버십 혜택 존재 -->

<div
v-if="membershipBenefit"
class="membership-guide"
>


<p>

{{membershipBenefit.name}}
포인트 적립 내역은
포인트 관리에서 확인할 수 있어요.

</p>



<button
@click="goPoint"
>

포인트 확인하기

</button>


</div>



<!-- 멤버십 없음 -->

<p
v-else
class="notice"
>

카드 혜택 적용 여부는
카드사 앱에서 확인해주세요.

</p>


</template>



<!-- 실패 -->

<template v-else>


<div class="icon fail">
!
</div>


<h2>
결제에 실패했습니다.
</h2>


<p>
잠시 후 다시 시도해주세요.
</p>


</template>




<button
class="confirm"
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

background:rgba(0,0,0,0.4);

display:flex;

align-items:center;

justify-content:center;

z-index:100;

}



.modal{

width:320px;

padding:28px;

<<<<<<< HEAD
background: var(--color-surface);
=======
background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border-radius:20px;

text-align:center;

}



.icon{

width:60px;

height:60px;

margin:auto;

border-radius:50%;

display:flex;

align-items:center;

justify-content:center;

font-size:32px;

}



.fail{

}



.payment-info{

margin:20px 0;

}



.payment-info strong{

font-size:24px;

}



.membership-guide{

margin-top:20px;

padding:16px;

border-radius:12px;

}



.membership-guide button{

width:100%;

height:44px;

border-radius:10px;

}



.notice{

margin:20px 0;

font-size:14px;

}


.confirm{

margin-top:20px;

width:100%;

height:48px;

border-radius:12px;

}


</style>