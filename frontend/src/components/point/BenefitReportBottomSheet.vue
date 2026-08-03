<script setup>
import { ref } from 'vue'


// 바텀시트 닫기 이벤트
const emit = defineEmits([
  'close'
])


// 선택한 카테고리
// null이면 전체 리포트 화면
const selectedCategory = ref(null)



// 임시 데이터
// 추후 혜택 리포트 API 응답으로 교체 예정
const benefitCategories = [
  {
    id: 1,
    name: '구독/콘텐츠',
    amount: 50000,

    details: [
      {
        name: '넷플릭스',
        payment: 17000,
        benefit: 'KB카드 2,000원 할인'
      },
      {
        name: '디즈니 플러스',
        payment: 15000,
        benefit: 'KB카드 1,500원 할인'
      }
    ]
  },

  {
    id: 2,
    name: '외식',
    amount: 18000,

    details: [
      {
        name: '스타벅스',
        payment: 6500,
        benefit: '카페 할인 500원'
      }
    ]
  },

  {
    id: 3,
    name: '편의점',
    amount: 5000,

    details: [
      {
        name: 'GS25',
        payment: 5000,
        benefit: '멤버십 적립'
      }
    ]
  }
]



// 카테고리 선택
const selectCategory = (category) => {

  selectedCategory.value = category

}


// 이전 화면
const backToCategory = () => {

  selectedCategory.value = null

}

</script>


<template>

  <div class="overlay"
       @click.self="emit('close')">


    <div class="bottom-sheet">


      <!-- 상세 화면 -->
      <template v-if="selectedCategory">


        <button
          class="back-button"
          @click="backToCategory"
        >
          ← 혜택 리포트
        </button>



        <h2>
          {{ selectedCategory.name }}
        </h2>



        <p class="total">
          {{ selectedCategory.amount.toLocaleString() }}원
        </p>




        <div
          v-for="detail in selectedCategory.details"
          :key="detail.name"
          class="detail-item"
        >

          <div>

            <strong>
              {{ detail.name }}
            </strong>


            <p>
              {{ detail.payment.toLocaleString() }}원 사용
            </p>

          </div>


          <span>
            {{ detail.benefit }}
          </span>


        </div>


      </template>




      <!-- 카테고리 목록 -->
      <template v-else>


        <h2>
          이번 달 혜택 리포트
        </h2>



        <div
          v-for="category in benefitCategories"
          :key="category.id"
          class="category-item"
          @click="selectCategory(category)"
        >

          <span>
            {{ category.name }}
          </span>


          <strong>
            {{ category.amount.toLocaleString() }}원
          </strong>


        </div>


      </template>



      <button
        class="close-button"
        @click="emit('close')"
      >
        닫기
      </button>



    </div>


  </div>

</template>


<style scoped>

.overlay {

  position: fixed;

  inset: 0;

  width: 100%;

  overflow: hidden;

  background: rgba(0,0,0,0.4);

  z-index: 1000;

}



.bottom-sheet {

  position: absolute;

  bottom: 0;

  width: 100%;

  box-sizing: border-box;

  background: white;

  border-radius: 24px 24px 0 0;

  padding: 24px;

  max-height: 70vh;

  overflow-y: auto;

}


h2 {

  font-size: 20px;

  margin-bottom: 20px;

}



.category-item {

  display: flex;

  justify-content: space-between;

  padding: 16px 0;

  border-bottom: 1px solid #eee;

  cursor: pointer;

}



.detail-item {

  padding: 16px 0;

  border-bottom: 1px solid #eee;

}



.detail-item p {

  margin-top: 6px;

  color: #666;

}



.detail-item span {

  display: block;

  margin-top: 8px;

  color: #2563eb;

}



.total {

  font-size: 28px;

  font-weight: 700;

}



.back-button {

  margin-bottom: 16px;

}



.close-button {

  width: 100%;

  margin-top: 20px;

  height: 44px;

  border-radius: 12px;

  background: #f3f4f6;

}


</style>