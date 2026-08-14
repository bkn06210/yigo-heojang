import { createRouter, createWebHistory } from 'vue-router'

// Auth
import TermsView from '@/views/auth/TermsView.vue'
import SignupView from '@/views/auth/SignupView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import PasswordChangeView from '@/views/auth/PasswordChangeView.vue'

// Onboarding
import OnboardingView from '@/views/onboarding/OnboardingView.vue'

// Main
// import GuestMainView from '@/views/main/GuestMainView.vue'
// import HomeEmptyView from '@/views/main/HomeEmptyView.vue'
import HomeView from '@/views/main/HomeView.vue'
import AIChatRoomView from '@/views/main/AIChatRoomView.vue'
import NotificationView from '@/views/main/NotificationView.vue'

// Card
import CardListView from '@/views/card/CardListView.vue'
import CardDetailView from '@/views/card/CardDetailView.vue'
import CardRegisterView from '@/views/card/CardRegisterView.vue'

// Payment
import PaymentView from '@/views/payment/PaymentView.vue'
import PaymentRecommendView from '@/views/payment/PaymentRecommendView.vue' //주소 명칭 변경
import PaymentQrView from '@/views/payment/PaymentQrView.vue'
// import CardRecommendResultView from '@/views/card/CardRecommendResultView.vue' //모달로 뺐음


// Transaction
import TransactionListView from '@/views/transaction/TransactionListView.vue'
import TransactionDetailView from '@/views/transaction/TransactionDetailView.vue'

// Point
import FinancialPointDetailView from '@/views/point/FinancialPointDetailView.vue'
import PointListView from '@/views/point/PointListView.vue'
import MembershipDetailView from '@/views/point/MembershipDetailView.vue'
import MembershipRegisterView from '@/views/point/MembershipRegisterView.vue'
// 혜택 상세는 views/card 쪽이 실제 화면이다 (views/point 쪽은 빈 껍데기).
import BenefitDetailView from '@/views/card/BenefitDetailView.vue'

// Settings
import SettingsView from '@/views/settings/SettingsView.vue'
import ProfileView from '@/views/settings/ProfileView.vue'
import AccountInfoView from '@/views/settings/AccountInfoView.vue'
// import SecuritySettingView from '@/views/settings/SecuritySettingView.vue'  //계정 및 보안으로 합침
import SecurityPasswordChangeView from '@/views/settings/SecurityPasswordChangeView.vue'
import NotificationSettingView from '@/views/settings/NotificationSettingView.vue'
// import DisplaySettingView from '@/views/settings/DisplaySettingView.vue'
import PersonalizationSettingView from '@/views/settings/PersonalizationSettingView.vue'
import WithdrawalView from '@/views/settings/WithdrawalView.vue'


const routes = [

    // Default
  {
    path: '/',
    redirect: '/home'
  },


  // Auth
  { path: '/auth/terms', component: TermsView },
  { path: '/auth/signup', component: SignupView },
  { path: '/auth/login', component: LoginView },
  { path: '/auth/password-change', component: PasswordChangeView },

  // Onboarding
  {path: '/onboarding', component: OnboardingView},

  // Main
  // { path: '/main/guest', component: GuestMainView },
  // { path: '/home/empty', component: HomeEmptyView },
  { path: '/home', component: HomeView },
  { path: '/ai/chat', component: AIChatRoomView },
  { path: '/notifications', component: NotificationView },

  // Card
  { path: '/cards', component: CardListView },
  { path: '/cards/:id', component: CardDetailView },
  { path: '/cards/register', component: CardRegisterView },
  // :id 는 userCardId (카드 상세의 '혜택 보기'가 여기로 보낸다)
  { path: '/benefits/:id', component: BenefitDetailView },

  // Payment
  // 결제 직전 AI 카드 추천 기능 포함이라 /card로 정의함
  { path: '/payment',  name: 'Payment', component: PaymentView },
  { path: '/payment/recommend', component: PaymentRecommendView }, //주소 명칭 변경
  { path: '/payments/qr', name: 'PaymentQr', component: PaymentQrView },
  // { path: '/cards/recommend/result', component: CardRecommendResultView }, //모달로 뺐음

  // Transaction
  { path: '/transactions', component: TransactionListView },
  { path: '/transactions/:id', component: TransactionDetailView },

  // Point
  { path: '/points', component: PointListView },
  { path: '/points/financial/:id', component: FinancialPointDetailView },

  // Membership
  { path: '/memberships/register', component: MembershipRegisterView },
  { path: '/memberships/:id', component: MembershipDetailView },
  
  // Settings
  { path: '/settings', component: SettingsView },
  { path: '/settings/profile', component: ProfileView },
  { path: '/settings/account', component: AccountInfoView },
  // { path: '/settings/security', component: SecuritySettingView },
  { path: '/settings/security/password', component: SecurityPasswordChangeView },
  { path: '/settings/notifications', component: NotificationSettingView },
  // { path: '/settings/display', component: DisplaySettingView },
  { path: '/settings/personalization', component: PersonalizationSettingView },
  { path: '/settings/withdrawal', component: WithdrawalView },

  
  
   // 404 처리
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]


const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
// 07_25 연동 변경: 추가된 API 시연 화면의 프론트 라우트를 등록한다.
