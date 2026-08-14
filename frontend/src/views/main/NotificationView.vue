<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/stores/authStore';
import {
  getNotifications,
  markNotificationAsRead,
  deleteNotification as deleteNotificationApi,
} from '@/api/notificationApi';

import PageHeader from '@/components/common/PageHeader.vue';
import NotificationModal from '@/components/notification/NotificationModal.vue';

// 뒤로가기용
const router = useRouter();

// 로그인 상태 확인
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

// 서버에서 받아온 알림 목록. GET /api/notifications
const notifications = ref([]);
const selectedNotification = ref(null);

// 서버는 한 번에 최대 10건까지만 준다. 이 화면에는 "더보기" 버튼이 없어서,
// hasNext가 있으면 몇 페이지까지는 이어서 받아 한 목록으로 보여준다.
// 무한정 받지 않도록 상한을 둔다 — 알림함은 최신 몇십 건이면 충분하고,
// 그보다 오래된 알림까지 끌어오면 첫 진입이 느려진다.
const PAGE_SIZE = 10;
const MAX_PAGES = 5;

// 서버는 생성 시각을 ISO 문자열로 준다. 화면은 '10분 전' 같은 상대 표기를 쓰므로
// 여기서 변환한다(표시 형식을 바꾸는 게 아니라 원래 형식에 맞춰 채우는 것).
const formatRelativeTime = (isoString) => {
  if (!isoString) return '';

  const created = new Date(isoString);
  if (Number.isNaN(created.getTime())) return '';

  const diffMs = Date.now() - created.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);

  if (diffMinutes < 1) return '방금 전';
  if (diffMinutes < 60) return `${diffMinutes}분 전`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}시간 전`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;

  return `${created.getMonth() + 1}월 ${created.getDate()}일`;
};

// 서버 응답을 이 화면이 쓰는 형태로 옮긴다.
//
// detail은 반드시 객체여야 한다 — 상세 모달이 notification.detail.cardName처럼
// 바로 접근해서, undefined면 렌더링 중 에러가 난다.
// 서버는 title/content 두 개만 주므로 본문을 message에 담아 모달의 기본 문구 자리에 채운다.
const toViewModel = (item) => ({
  id: item.notificationId,
  title: item.title,
  message: item.content,
  createdAt: formatRelativeTime(item.createdAt),
  isRead: Boolean(item.read),
  detail: {
    message: item.content,
  },
});

const loadNotifications = async () => {
  // 비로그인 상태에서는 호출하지 않는다. 어차피 401이고, 화면은 빈 목록을 보여준다.
  if (!user.value) {
    notifications.value = [];
    return;
  }

  try {
    const collected = [];

    for (let page = 0; page < MAX_PAGES; page += 1) {
      const response = await getNotifications({ page, size: PAGE_SIZE });

      collected.push(...(response?.notifications || []).map(toViewModel));

      if (!response?.hasNext) {
        break;
      }
    }

    notifications.value = collected;
  } catch (error) {
    console.error('알림 목록 조회 실패:', error);
    notifications.value = [];
  }
};

onMounted(loadNotifications);

// 비로그인 상태에서는 알림 없음 처리
const displayNotifications = computed(() => {
  return user.value ? notifications.value : [];
});

const openNotification = (item) => {
  selectedNotification.value = item;
};



// 뒤로가기
const goBack = () => {

  router.back();

};



// 알림 읽음 처리 — PATCH /api/notifications/{id}/read
//
// 화면을 먼저 바꾸고 서버에 보낸다. 응답을 기다렸다가 바꾸면 모달을 닫는 순간
// 목록이 잠깐 안 읽음으로 남아 깜빡인다. 실패하면 원래 상태로 되돌린다.
const readNotification = async (id) => {

  const target = notifications.value.find(
    item => item.id === id
  );


  if (!target || target.isRead) {

    return;

  }


  target.isRead = true;


  try {

    await markNotificationAsRead(id);

  } catch (error) {

    console.error('알림 읽음 처리 실패:', error);

    target.isRead = false;

  }

};


// 모두 읽음
// 한 번에 처리하는 API가 없어 안 읽은 것만 골라 각각 호출한다.
// 화면에 최대 50건이라 요청 수가 통제된다.
const readAll = async () => {

  const unread = notifications.value.filter(item => !item.isRead);


  if (!unread.length) {

    return;

  }


  unread.forEach(item => {
    item.isRead = true;
  });


  const results = await Promise.allSettled(
    unread.map(item => markNotificationAsRead(item.id)),
  );


  // 실패한 건만 원래대로 되돌린다. 성공한 건까지 되돌리면 서버와 화면이 어긋난다.
  results.forEach((result, index) => {

    if (result.status === 'rejected') {

      console.error('알림 읽음 처리 실패:', result.reason);

      unread[index].isRead = false;

    }

  });

};



// 개별 삭제 — DELETE /api/notifications/{id}
//
// 삭제는 되돌리기 어려운 동작이라 서버가 성공을 확인한 뒤에 목록에서 뺀다.
// 먼저 지웠다가 실패하면 사라진 알림이 다시 나타나 더 혼란스럽다.
const deleteNotification = async (id) => {

  try {

    await deleteNotificationApi(id);

  } catch (error) {

    console.error('알림 삭제 실패:', error);

    return;

  }


  notifications.value =
    notifications.value.filter(
      item => item.id !== id
    );


  // 지운 알림을 상세로 열어둔 상태였다면 같이 닫는다.
  if (selectedNotification.value?.id === id) {

    selectedNotification.value = null;

  }

};

</script>



<template>

<div class="notification-view">


  <!-- 헤더 -->
  <PageHeader title="알림" @back="goBack">
    <button @click="readAll" style="background: var(--color-primary); color: var(--color-btn-primary-text); border: none; padding: 12px 20px; border-radius: var(--radius-lg); font-weight: 600; font-size: var(--font-sm); cursor: pointer; height: 40px; display: flex; align-items: center;">
      모두 읽음
    </button>
  </PageHeader>




  <!-- 알림 리스트 -->
  <main class="notification-list">


    <div
      v-if="displayNotifications.length === 0"
      class="empty"
    >
      새로운 알림이 없습니다.
    </div>



    <article
      v-for="item in displayNotifications"
      :key="item.id"
      class="notification-item"
      :class="{
        unread: !item.isRead
      }"

      @click="openNotification(item)"
    >


      <div class="notification-content">


        <h2>
          {{ item.title }}
        </h2>


        <p>
          {{ item.message }}
        </p>


        <span>
          {{ item.createdAt }}
        </span>


      </div>



      <!-- 삭제 버튼 -->
      <button
        type="button"
        class="delete-button"
        @click.stop="deleteNotification(item.id)"
      >
        ×
      </button>


    </article>


  </main>

  <NotificationModal
  v-if="selectedNotification"
  :notification="selectedNotification"
  @close="selectedNotification=null"
/>

<NotificationModal

  v-if="selectedNotification"

  :notification="selectedNotification"

  @close="selectedNotification = null"

  @read="readNotification"

/>



</div>

</template>



<style scoped>

.notification-view {

  min-height:100vh;

  background: var(--color-bg);

  padding: var(--space-md);

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

}






.notification-list {
  margin-top: var(--space-md);
  display:flex;

  flex-direction:column;

  gap: var(--space-sm);

}



.notification-item {

  display:flex;

  justify-content:space-between;

  align-items:flex-start;

  padding: var(--space-md);

  background: var(--color-surface);

  border-radius: var(--radius-md);

  color: var(--color-text-secondary);

  transition: var(--transition-normal);

}



.notification-item.unread {

  color: var(--color-text-primary);

  font-weight: var(--font-semibold);

}



.notification-content h2 {

  margin: 0 0 var(--space-xs);

  font-size: var(--font-md);

  font-weight: var(--font-semibold);

}



.notification-content p {

  margin: 0 0 var(--space-xs);

  font-size: var(--font-sm);

  font-weight: var(--font-regular);

}



.notification-content span {

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

}



.delete-button {

  border:none;

  background:none;

  font-size: var(--font-lg);

  color: var(--color-text-tertiary);

  cursor:pointer;

}






.empty {

  text-align:center;

  color: var(--color-text-secondary);

  padding: calc(var(--space-2xl) * 2.5) 0;

}

</style>
