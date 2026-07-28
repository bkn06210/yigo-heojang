<script setup>




// Props

defineProps({
  // 카드 제목
  title: {
    type: String,
    required: true,
  },

  // 목록 데이터
  items: {
    type: Array,
    default: () => [],
  },
})


// Emits

const emit = defineEmits([
  'click-more',
  'click-item',
])
</script>

<template>
  <section class="summary-card">

    <!-- 헤더 -->
    <div class="summary-header">

      <h2>{{ title }}</h2>

      <button
        type="button"
        @click="$emit('click-more')"
      >
        더보기 >
      </button>

    </div>

    <!-- 목록 -->
    <div
      v-for="item in items"
      :key="item.id"
      class="summary-item"
      @click="$emit('click-item', item)"
    >

      <!-- 왼쪽 -->
      <div class="left">

        {{ item.name }}

      </div>

      <!-- 오른쪽 -->
      <div class="right">

        <template v-if="item.balance !== undefined">

          <span>총 잔액</span>

          <strong>
            {{ item.balance.toLocaleString() }}P
          </strong>

        </template>

        <template v-else>

          <button
            type="button"
            @click.stop="$emit('click-item', item)"
          >
            상세 보기 >
          </button>

        </template>

      </div>

    </div>

  </section>
</template>

<style scoped>
.summary-card{
    display:flex;
    flex-direction:column;
    gap:12px;

    padding:16px;
}

.summary-header{
    display:flex;
    justify-content:space-between;
    align-items:center;
}

.summary-item{
    display:flex;
    justify-content:space-between;
    align-items:center;

    cursor:pointer;

    padding:12px 0;

    border-top:1px solid #eee;
}

.left{
    font-weight:600;
}

.right{
    display:flex;
    flex-direction:column;
    align-items:flex-end;
    gap:4px;
}
</style>