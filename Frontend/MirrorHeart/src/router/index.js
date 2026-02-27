import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus' //引入element-plus的message组件

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/views/layout/LayoutPage.vue'),
      redirect: '/homePage',
      children: [
        {
          path: '/homePage',
          component: () => import('@/views/userUI/HomePage.vue'),
          meta: { keepAlive: true }
        },
        {
          path: '/userCommunity',
          component: () => import('@/views/userUI/UserCommunity.vue'),
          meta: { keepAlive: true }
        },
        {
          path: '/CBT',
          component: () => import('@/views/userUI/CBT.vue'),
          meta: { keepAlive: true }
        },
        {
          path: '/heart-voice',
          component: () => import('@/components/CBT/HeartVoice.vue')
        },
        {
          path: '/daily-question',
          component: () => import('@/components/homePage/DailyQuestionPage.vue')
        },
        {
          path: '/login',
          component: () => import('@/views/login/LoginPage.vue')
        },
        {
          path: '/findPassword',
          component: () => import('@/views/login/FindPassword.vue')
        },
        {
          path: '/user-profile/:userId',
          component: () => import('@/components/community/userProfilePage.vue'),
          props: true
        }
      ]
    },
    { path: '/login', component: () => import('@/views/login/LoginPage.vue') }
  ]
})

// 全局前置守卫,如果用户访问CBT页面时未登录，拦截跳转到首页
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.basicUser.token
  if (to.path === '/CBT' && token === '') {
    ElMessage.error('登录后才能访问CBT页面哦！')
    userStore.setIsLogin()
    next(from.path)
  } else {
    next()
  }
})

export default router
