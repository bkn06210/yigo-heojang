import { createRouter, createWebHistory } from 'vue-router'

// Auth
import TermsView from '@/views/auth/TermsView.vue'
import SignupView from '@/views/auth/SignupView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import PasswordChangeView from '@/views/auth/PasswordChangeView.vue'

// Onboarding
import OnboardingView from '@/views/onboarding/OnboardingView.vue'

// Main
import HomeView from '@/views/main/HomeView.vue'
import AIChatRoomView from '@/views/main/AIChatRoomView.vue'
import NotificationView from '@/views/main/NotificationView.vue'

// Card
import CardListView from '@/views/card/CardListView.vue'
import CardDetailView from '@/views/card/CardDetailView.vue'
import CardRegisterView from '@/views/card/CardRegisterView.vue'
import BenefitDetailView from '@/views/card/BenefitDetailView.vue'

// Payment
import PaymentView from '@/views/payment/PaymentView.vue'
import PaymentRecommendView from '@/views/payment/PaymentRecommendView.vue'

// Transaction
import TransactionListView from '@/views/transaction/TransactionListView.vue'
import TransactionDetailView from '@/views/transaction/TransactionDetailView.vue'

// Point / Membership
import FinancialPointDetailView from '@/views/point/FinancialPointDetailView.vue'
import PointListView from '@/views/point/PointListView.vue'
import MembershipDetailView from '@/views/point/MembershipDetailView.vue'
import MembershipRegisterView from '@/views/point/MembershipRegisterView.vue'

// Settings
import SettingsView from '@/views/settings/SettingsView.vue'
import ProfileView from '@/views/settings/ProfileView.vue'
import AccountInfoView from '@/views/settings/AccountInfoView.vue'
import SecurityPasswordChangeView from '@/views/settings/SecurityPasswordChangeView.vue'
import NotificationSettingView from '@/views/settings/NotificationSettingView.vue'
import PersonalizationSettingView from '@/views/settings/PersonalizationSettingView.vue'
import WithdrawalView from '@/views/settings/WithdrawalView.vue'


const routes = [
  // Default
  {
    path: '/',
    redirect: '/home',
  },

  // Auth
  { path: '/auth/terms', component: TermsView },
  { path: '/auth/signup', component: SignupView },
  { path: '/auth/login', component: LoginView },
  { path: '/auth/password-change', component: PasswordChangeView },

  // Onboarding
  { path: '/onboarding', component: OnboardingView },

  // Main
  { path: '/home', component: HomeView },
  { path: '/ai/chat', component: AIChatRoomView },
  { path: '/notifications', component: NotificationView },

  // Card — static paths before dynamic :id
  { path: '/cards', component: CardListView },
  { path: '/cards/register', component: CardRegisterView },
  { path: '/cards/:id', component: CardDetailView },
  { path: '/benefits/:id', component: BenefitDetailView },

  // Payment
  { path: '/payment', name: 'Payment', component: PaymentView },
  { path: '/payment/recommend', name: 'PaymentRecommend', component: PaymentRecommendView },

  // Transaction
  { path: '/transactions', component: TransactionListView },
  { path: '/transactions/:id', component: TransactionDetailView },

  // Point
  { path: '/points', component: PointListView },
  { path: '/points/financial/:id', component: FinancialPointDetailView },

  // Membership — static paths before dynamic :id
  { path: '/memberships/register', component: MembershipRegisterView },
  { path: '/memberships/:id', component: MembershipDetailView },

  // Settings
  { path: '/settings', component: SettingsView },
  { path: '/settings/profile', component: ProfileView },
  { path: '/settings/account', component: AccountInfoView },
  { path: '/settings/security/password', component: SecurityPasswordChangeView },
  { path: '/settings/notifications', component: NotificationSettingView },
  { path: '/settings/personalization', component: PersonalizationSettingView },
  { path: '/settings/withdrawal', component: WithdrawalView },

  // 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
]


const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 하단 네비게이션 탭의 루트 경로 — 탭 간 이동은 슬라이드 대신 페이드로 처리
const TAB_ROOTS = ['/home', '/cards', '/payment', '/points', '/settings']

// 현재까지 쌓인 이동 경로 스택 — router.back() 시 이전 경로와 비교해 뒤로가기 여부를 판단
let pathStack = []

router.beforeEach((to, from) => {
  const toIsTab = TAB_ROOTS.includes(to.path)
  const fromIsTab = TAB_ROOTS.includes(from.path)

  // 탭 ↔ 탭 이동은 방향성이 없는 수평 전환이라 페이드로 처리하고 스택을 새로 시작
  if (toIsTab && fromIsTab) {
    to.meta.transition = 'fade'
    pathStack = [to.path]
    return
  }

  const isBack = pathStack.length >= 2 && pathStack[pathStack.length - 2] === to.path

  if (isBack) {
    pathStack.pop()
    to.meta.transition = 'slide-back'
  } else {
    pathStack.push(to.path)
    to.meta.transition = 'slide-forward'
  }
})

export default router
