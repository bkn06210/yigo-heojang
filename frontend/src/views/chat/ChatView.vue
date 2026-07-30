<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import ChatMessage from '@/components/chat/ChatMessage.vue';
import QuickQuestion from '@/components/chat/QuickQuestion.vue';


const router = useRouter();


// 채팅 메시지
// TODO: AI 서버 연결 후 GET/POST 응답 데이터로 교체
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



// 메시지 전송
const sendMessage = () => {


  if(!inputMessage.value.trim()) return;



  // 사용자 메시지 추가
  messages.value.push({

    id:Date.now(),

    sender:'user',

    message:inputMessage.value,

    time:'방금'

  });



  const question = inputMessage.value;


  inputMessage.value='';



  /*
    TODO: AI 서버 연결 위치

    POST /api/chat

    request:
    {
      message: question
    }

    response:
    {
      answer:"",
      recommendedQuestions:[]
    }

  */



  setTimeout(()=>{


    messages.value.push({

      id:Date.now(),

      sender:'ai',

      message:
        `"${question}"에 대한 답변을 준비하고 있어요.`,

      time:'방금'

    });


  },500);


};



// 추천 질문 클릭
const selectQuestion = (question)=>{

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

      placeholder="두리에게 물어보세요"

      @keyup.enter="sendMessage"

    >



    <button

      @click="sendMessage"

    >

      ➤

    </button>


  </footer>



</div>

</template>



<style scoped>


.chat-view{

  min-height:100vh;

  background:#f8f8fb;

  padding-bottom:90px;

}




.chat-container{

  padding:20px;

}



.message-area{

  display:flex;

  flex-direction:column;

  gap:14px;

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