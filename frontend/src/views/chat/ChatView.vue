<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import ChatMessage from '@/components/chat/ChatMessage.vue';
import QuickQuestion from '@/components/chat/QuickQuestion.vue';

// 챗봇 질의 API
import { askChat } from '@/api/chatApi';


const router = useRouter();


// 채팅 메시지
const messages = ref([

  {
    id:1,
    sender:'ai',
    message:
      '안녕하세요. 두리 AI예요 👀\n금융 혜택과 소비 관리를 도와드릴게요.',
    time:'방금'
  }

]);



// 추천 질문
// TODO: AI 서버에서 사용자 상황 기반 추천 질문 제공 가능
const quickQuestions = ref([

  '이번 달 혜택 알려줘',

  '내 카드 추천해줘',

  '소비 분석해줘',

]);



// 입력값
const inputMessage = ref('');



// 답변 대기 상태
// LLM 호출이 두 번 들어가 응답이 수 초 걸린다. 그동안 중복 전송을 막고 상태를 보여준다.
const isLoading = ref(false);



// 되물음 맥락
// 서버가 되물으면서 pendingContext를 함께 내려준다. 다음 질문에 그대로 실어 보내야
// 앞 대화가 이어진다 (예: 금액만 말한 뒤 가맹점을 답하는 경우).
// 내용을 해석하거나 고치지 않는다 — 주제가 바뀌면 서버가 알아서 버린다.
const pendingContext = ref(null);



const pushMessage = (sender, message) => {

  messages.value.push({

    id:Date.now() + Math.random(),

    sender,

    message,

    time:'방금'

  });

};



// 에러를 사용자 문장으로 바꾼다.
// 응답 본문이 success:false로 온 경우(api 모듈이 던짐)와 HTTP 에러로 온 경우 둘 다 code가 다른 자리에 있다.
// 응답 본문을 먼저 본다 — AxiosError 는 자체 code('ERR_BAD_RESPONSE' 등)를 갖고 있어서
// error.code 를 먼저 보면 서버가 준 코드가 가려진다.
const errorMessage = (error) => {

  const code = error.response?.data?.code || error.code;


  if (code === 'CHATBOT_UNAVAILABLE') {
    return '지금 두리가 자리를 비웠어요. 잠시 후 다시 물어봐 주세요.';
  }


  if (code === 'INPUT_INVALID') {
    return error.response?.data?.message || '질문을 다시 입력해 주세요.';
  }


  // 응답 자체가 없으면 네트워크 문제이거나 답변이 너무 오래 걸린 경우다.
  if (!error.response) {
    return '답변을 받아오지 못했어요. 잠시 후 다시 시도해 주세요.';
  }


  return error.response?.data?.message || '답변을 준비하지 못했어요.';

};



// 메시지 전송
const sendMessage = async () => {


  if(!inputMessage.value.trim()) return;


  // 답변을 기다리는 동안은 다음 질문을 받지 않는다.
  // 되물음 맥락이 있는 상태에서 두 질문이 겹치면 어느 쪽 답인지 알 수 없다.
  if(isLoading.value) return;



  // 사용자 메시지 추가
  pushMessage('user', inputMessage.value);



  const question = inputMessage.value;


  inputMessage.value='';


  isLoading.value = true;



  try {

    // POST /api/chat
    const result = await askChat(question, pendingContext.value);


    pushMessage('ai', result.answer);


    // 되물었으면 맥락을 들고 있다가 다음 질문에 실어 보낸다.
    // 되묻지 않았으면(pendingContext가 null) 대화가 끝난 것이므로 비운다.
    pendingContext.value = result.pendingContext ?? null;


  } catch (error) {

    console.log('챗봇 에러:', error);


    pushMessage('ai', errorMessage(error));


    // 실패한 턴의 맥락을 남겨두면 다음 질문이 엉뚱한 맥락으로 해석된다.
    pendingContext.value = null;


  } finally {

    isLoading.value = false;

  }


};



// 추천 질문 클릭
const selectQuestion = (question)=>{

  // 답변을 기다리는 중이면 입력창만 채워지고 전송은 막혀 어색해진다. 아예 받지 않는다.
  if(isLoading.value) return;

  inputMessage.value = question;

  sendMessage();

};



// 뒤로가기
const goBack = ()=>{

  router.back();

};


</script>



<template>

<div class="chat-view">


  <PageHeader title="두리 AI" />



  <main class="chat-container">


    <!-- 메시지 영역 -->

    <section class="message-area">


      <ChatMessage

        v-for="message in messages"

        :key="message.id"

        :message="message"

      />


      <!-- 답변 대기 표시 (LLM 호출이 들어가 수 초 걸린다) -->

      <div

        v-if="isLoading"

        class="typing"

      >

        두리가 답변을 준비하고 있어요…

      </div>


    </section>



    <!-- 추천 질문 -->

    <section class="quick-area">


      <p>
        추천 질문
      </p>


      <div class="quick-list">


        <QuickQuestion

          v-for="question in quickQuestions"

          :key="question"

          :question="question"

          @click="selectQuestion(question)"

        />


      </div>


    </section>



  </main>



  <!-- 입력 영역 -->

  <footer class="input-area">


    <input

      v-model="inputMessage"

      :placeholder="isLoading ? '답변을 기다리는 중이에요' : '두리에게 물어보세요'"

      :disabled="isLoading"

      @keyup.enter="sendMessage"

    >



    <button

      :disabled="isLoading"

      @click="sendMessage"

    >

      ➤

    </button>


  </footer>



</div>

</template>



<style scoped>


.chat-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
}




.chat-container {
  flex: 1;
  padding: 20px;
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}



.message-area{

  display:flex;

  flex-direction:column;

  gap:14px;

}



.typing{

  align-self:flex-start;

  font-size:14px;

  color:#777;

  padding:10px 14px;

}



.quick-area{

  margin-top:24px;

}



.quick-area p{

  font-size:14px;

  color:#777;

  margin-bottom:10px;

}



.quick-list{

  display:flex;

  gap:8px;

  overflow-x:auto;

}




.input-area{

  position:fixed;

  bottom:0;

  left:0;

  right:0;


  display:flex;

  gap:10px;


  padding:14px 20px;


  background:white;


  border-top:1px solid #eee;

}



.input-area input{


  flex:1;


  height:48px;


  border-radius:24px;


  border:1px solid #ddd;


  padding:0 18px;


  font-size:15px;


}



.input-area button{


  width:48px;

  height:48px;


  border:none;

  border-radius:50%;


  background:#4F46E5;


  color:white;


  font-size:18px;


}


</style> 
