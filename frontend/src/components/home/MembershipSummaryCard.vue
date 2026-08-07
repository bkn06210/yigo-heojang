<script setup>
<<<<<<< HEAD
import BaseCard from '@/components/common/BaseCard.vue'

const props = defineProps({
  memberships: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['click-more', 'click-item'])
</script>

<template>
  <BaseCard class="membership-summary">
    <div class="membership-list" :class="{ multiple: props.memberships.length > 1 }">
      <article
        v-for="membership in props.memberships.slice(0, 3)"
        :key="membership.id"
        class="membership-item"
        @click="emit('click-item', membership)"
      >
        <div class="membership-icon">M</div>
        <div class="membership-info">
          <p class="membership-name">{{ membership.name }}</p>
        </div>
      </article>
    </div>
  </BaseCard>
</template>

<style scoped>
.membership-summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.membership-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  width: 100%;
  justify-content: flex-start;
  flex: 1;
}

.membership-list.multiple {
  justify-content: flex-start;
}

.membership-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  border-radius: var(--radius-md);
  background: var(--color-membership-bg);
  cursor: pointer;
  min-width: 0;
  transition: var(--transition-fast);
}

.membership-item:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

.membership-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-membership-icon);
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: var(--font-bold);
  font-size: var(--font-sm);
}

.membership-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs);
  min-width: 0;
}

.membership-name {
  margin: 0;
  font-size: var(--font-xs);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);
  word-break: break-word;
}
</style>
=======


// =========================
// Props
// =========================

const props = defineProps({

  // 로그인 여부
  isLogin: {

    type:Boolean,

    default:false

  },


  // 멤버십 목록
  memberships: {

    type:Array,

    default:()=>[]

  }

})




// =========================
// Emits
// =========================

const emit = defineEmits([

  // 포인트 리스트 이동
  // 금융 포인트와 멤버십은 동일한 PointListView 사용
  'click-more',


  // 멤버십 상세 이동
  'click-item',


  // 멤버십 등록 이동
  'click-register'

])


</script>





<template>


<section class="membership-summary">



  <!-- =========================
       Header
       ========================= -->

  <div class="membership-header">


    <h2>

      멤버십

    </h2>



    <!--
      금융 포인트와 동일하게
      PointListView 이동
    -->

    <button

      type="button"

      @click="emit('click-more')"

    >

      더보기

    </button>


  </div>







  <!-- =========================
       비로그인
       ========================= -->

  <div

    v-if="!props.isLogin"

    class="empty"

  >


    <p>

      로그인하면

      <br>

      등록된 멤버십을 확인할 수 있어요.

    </p>


  </div>









  <!-- =========================
       로그인 + 멤버십 미등록
       ========================= -->

  <div

    v-else-if="!props.memberships.length"

    class="empty"

  >


    <p>

      등록된 멤버십이 없습니다.

    </p>



    <button

      type="button"

      class="register-button"

      @click="emit('click-register')"

    >

      멤버십 등록

    </button>


  </div>









  <!-- =========================
       로그인 + 멤버십 등록
       ========================= -->

  <div

    v-else

    class="membership-list"

  >



    <article

      v-for="membership in props.memberships"

      :key="membership.id"

      class="membership-item"

      @click="emit('click-item', membership)"

    >



      <!-- 임시 아이콘 -->

      <div class="membership-icon">

        M

      </div>




      <div class="membership-info">


        <p class="membership-name">

          {{ membership.name }}

        </p>


      </div>



    </article>



  </div>





</section>


</template>








<style scoped>


.membership-summary{


  background:white;


  border-radius:20px;


  padding:20px;


  box-shadow:0 4px 12px rgba(0,0,0,0.05);


}






.membership-header{


  display:flex;


  justify-content:space-between;


  align-items:center;


  margin-bottom:16px;


}






.membership-header h2{


  margin:0;


  font-size:18px;


}






.membership-header button{


  border:none;


  background:none;


  cursor:pointer;


}









.empty{


  min-height:120px;


  display:flex;


  flex-direction:column;


  justify-content:center;


  align-items:center;


  gap:16px;


  text-align:center;


  color:#666;


}






.empty p{


  margin:0;


}






.register-button{


  border:none;


  padding:10px 20px;


  border-radius:20px;


  cursor:pointer;


}









.membership-list{


  display:flex;


  flex-direction:column;


  gap:12px;


}






.membership-item{


  display:flex;


  align-items:center;


  gap:12px;


  padding:14px;


  border-radius:14px;


  background:#f7f7f7;


  cursor:pointer;


}






.membership-icon{


  width:40px;


  height:40px;


  border-radius:50%;


  background:#eee;


  display:flex;


  justify-content:center;


  align-items:center;


}






.membership-name{


  margin:0;


  font-size:14px;


}



</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
