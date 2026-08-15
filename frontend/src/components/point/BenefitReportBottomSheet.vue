<script setup>
import { ref, computed } from 'vue'


// 혜택 리포트 (GET /api/benefits/report 응답)
// 조회는 화면(PointListView)이 하고 여기는 받은 것만 그린다 —
// 카드와 바텀시트가 같은 응답을 나눠 써야 합계와 상세가 어긋나지 않는다.
const props = defineProps({
  report: {
    type: Object,
    default: null,
  },
})


// 바텀시트 닫기 이벤트
const emit = defineEmits([
  'close'
])


// 선택한 카테고리
// null이면 전체 리포트 화면
const selectedCategory = ref(null)



// API 응답을 화면이 쓰는 형태로 옮긴다.
// 부문명은 중분류 거래면 parentCategoryName이 있어 '문화여가 > 스포츠레저'로 보여줄 수 있다.
// 대분류 거래면 parentCategoryName이 null이라 부문명만 쓴다.
const benefitCategories = computed(() =>

  (props.report?.categories ?? []).map((category) => ({

    id: category.categoryId,

    name: category.parentCategoryName
      ? `${category.parentCategoryName} > ${category.categoryName}`
      : category.categoryName,

    amount: category.benefitAmount,

    details: (category.details ?? []).map((detail) => ({

      id: detail.expenseId,

      name: detail.merchantName,

      payment: detail.paymentAmount,

      // 오른쪽에 한 줄로 들어가는 자리라 '어느 카드로 얼마'까지만 담는다.
      // 혜택 이름(benefitName)은 길어서 줄바꿈이 생긴다.
      benefit: `${detail.cardName} ${detail.benefitAmount.toLocaleString()}원`,

    })),

  }))

)



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
          :key="detail.id"
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



        <!-- 받은 혜택이 없는 달도 에러가 아니라 빈 목록으로 온다 -->
        <p
          v-if="!benefitCategories.length"
          class="empty"
        >
          이번 달에 받은 혜택이 아직 없어요.
        </p>



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
  max-width: 480px;
  left: 50%;
  transform: translateX(-50%);
  margin: 0 auto;

  overflow: hidden;

  background: rgba(0,0,0,0.4);

  display: flex;
  justify-content: center;
  align-items: flex-end;

  z-index: 1000;

}



.bottom-sheet {

  position: absolute;

  bottom: 0;

  width: 100%;

  max-width: 480px;

  box-sizing: border-box;

  border-radius: var(--radius-xl) var(--radius-xl) 0 0;

  padding: var(--space-xl);

  max-height: 70vh;

  overflow-y: auto;

  /* 카드사용내역 상세 바텀시트와 동일하게 불투명 배경으로 통일 */
  background: var(--color-surface);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);

}

[data-theme="dark"] .bottom-sheet {

  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);

}


h2 {

  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  letter-spacing: -0.2px;
  color: var(--color-text-primary);

  margin: 0 0 var(--space-xl);

}



.empty {

  padding: var(--space-lg) 0;

  text-align: center;

  color: var(--color-text-secondary);

  font-size: var(--font-sm);

}

.category-item {

  display: flex;

  justify-content: space-between;

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

}



.detail-item {

  padding: var(--space-md);

  margin-bottom: var(--space-sm);

  border-radius: var(--radius-md);

  background: var(--color-surface);
  border: 1px solid var(--color-border);

}



.detail-item p {

  margin-top: 6px;

  color: var(--color-text-secondary);

}



.detail-item span {

  display: block;

  margin-top: 8px;

  color: var(--color-text-primary);

}



.total {

  font-size: var(--typo-display-large-size);
  font-weight: var(--typo-display-large-weight);
  line-height: var(--typo-display-large-line-height);
  letter-spacing: var(--typo-display-large-letter-spacing);
  color: var(--color-text-primary);

  margin: 0 0 var(--space-lg);

}



.back-button {

  margin-bottom: 16px;

  width: auto;

  height: auto;

  padding: 0;

  border: none;

  background: transparent;

  color: var(--color-text-primary);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.back-button:hover {

  opacity: 0.7;

}

.close-button {

  width: 100%;

  margin-top: 20px;

  height: 44px;

  border-radius: 12px;

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

}


</style>