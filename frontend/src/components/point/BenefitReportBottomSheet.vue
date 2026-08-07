<<<<<<< HEAD
﻿<script setup>
=======
<script setup>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;

  padding: var(--space-xl);
=======
  background: white;

  border-radius: 24px 24px 0 0;

  padding: 24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  max-height: 70vh;

  overflow-y: auto;

<<<<<<< HEAD
  /* 카드사용내역 상세 바텀시트와 동일하게 불투명 배경으로 통일 */
  background: var(--color-surface);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);

}

[data-theme="dark"] .bottom-sheet {

  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}


h2 {

<<<<<<< HEAD
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  letter-spacing: -0.2px;
  color: var(--color-text-primary);

  margin: 0 0 var(--space-xl);
=======
  font-size: 20px;

  margin-bottom: 20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.category-item {

  display: flex;

  justify-content: space-between;

<<<<<<< HEAD
  align-items: center;

  padding: var(--space-md);

  margin-bottom: var(--space-sm);

  border-radius: var(--radius-md);

  background: var(--color-surface);
  border: 1px solid var(--color-border);

  cursor: pointer;

  transition: var(--transition-fast);

}

.category-item:hover {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);

}

[data-theme="dark"] .category-item:hover {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.06) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);

=======
  padding: 16px 0;

  border-bottom: 1px solid #eee;

  cursor: pointer;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.detail-item {

<<<<<<< HEAD
  padding: var(--space-md);

  margin-bottom: var(--space-sm);

  border-radius: var(--radius-md);

  background: var(--color-surface);
  border: 1px solid var(--color-border);
=======
  padding: 16px 0;

  border-bottom: 1px solid #eee;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.detail-item p {

  margin-top: 6px;

<<<<<<< HEAD
  color: var(--color-text-secondary);
=======
  color: #666;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.detail-item span {

  display: block;

  margin-top: 8px;

<<<<<<< HEAD
  color: var(--color-text-primary);
=======
  color: #2563eb;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.total {

<<<<<<< HEAD
  font-size: var(--typo-display-large-size);
  font-weight: var(--typo-display-large-weight);
  line-height: var(--typo-display-large-line-height);
  letter-spacing: var(--typo-display-large-letter-spacing);
  color: var(--color-text-primary);

  margin: 0 0 var(--space-lg);
=======
  font-size: 28px;

  font-weight: 700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.back-button {

  margin-bottom: 16px;

<<<<<<< HEAD
  width: auto;

  height: 40px;

  padding: 0 var(--space-md);

  border: 1px solid var(--color-border);

  background: var(--color-surface);

  color: var(--color-text-primary);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.close-button {

  width: 100%;

  margin-top: 20px;

  height: 44px;

  border-radius: 12px;

<<<<<<< HEAD
  border: none;

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);
=======
  background: #f3f4f6;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


</style>