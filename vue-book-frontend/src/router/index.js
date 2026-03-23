import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      redirect: '/admin/home',
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/home',
      children: [
        {
          path: 'home',
          name: 'AdminHome',
          component: () => import('@/views/admin/Home.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'books',
          name: 'BookManagement',
          component: () => import('@/views/admin/books/BookList.vue'),
          meta: { title: '图书管理', icon: 'Reading' },
        },
        {
          path: 'booktypes',
          name: 'BookTypeManagement',
          component: () => import('@/views/admin/books/BookTypeList.vue'),
          meta: { title: '分类管理', icon: 'List' },
        },
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/admin/users/UserList.vue'),
          meta: { title: '借阅证管理', icon: 'User' },
        },
        {
          path: 'rules',
          name: 'RuleManagement',
          component: () => import('@/views/admin/rules/RuleList.vue'),
          meta: { title: '规则管理', icon: 'Document' },
        },
        {
          path: 'bookadmins',
          name: 'BookAdminManagement',
          component: () => import('@/views/admin/bookadmins/BookAdminList.vue'),
          meta: { title: '管理员管理', icon: 'UserFilled' },
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/admin/statistics/StatisticChart.vue'),
          meta: { title: '数据统计', icon: 'DataAnalysis' },
        },
      ],
    },
    {
      path: '/bookadmin',
      name: 'BookAdmin',
      component: () => import('@/layouts/BookAdminLayout.vue'),
      redirect: '/bookadmin/home',
      children: [
        {
          path: 'home',
          name: 'BookAdminHome',
          component: () => import('@/views/bookadmin/Home.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'borrow',
          name: 'BorrowManagement',
          component: () => import('@/views/bookadmin/borrow/BorrowBook.vue'),
          meta: { title: '借书管理', icon: 'TakeawayBox' },
        },
        {
          path: 'return',
          name: 'ReturnManagement',
          component: () => import('@/views/bookadmin/return/ReturnBook.vue'),
          meta: { title: '还书管理', icon: 'Back' },
        },
        {
          path: 'statements',
          name: 'StatementManagement',
          component: () => import('@/views/bookadmin/statements/StatementList.vue'),
          meta: { title: '报表管理', icon: 'Document' },
        },
        {
          path: 'notices',
          name: 'NoticeManagement',
          component: () => import('@/views/bookadmin/notices/NoticeList.vue'),
          meta: { title: '公告管理', icon: 'Bell' },
        },
      ],
    },
    {
      path: '/user',
      name: 'User',
      component: () => import('@/layouts/UserLayout.vue'),
      redirect: '/user/home',
      children: [
        {
          path: 'home',
          name: 'UserHome',
          component: () => import('@/views/user/Home.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'search',
          name: 'SearchBooks',
          component: () => import('@/views/user/books/SearchBooks.vue'),
          meta: { title: '图书查询', icon: 'Search' },
        },
        {
          path: 'borrow',
          name: 'MyBorrow',
          component: () => import('@/views/user/borrow/MyBorrow.vue'),
          meta: { title: '我的借阅', icon: 'ReadingLamp' },
        },
        {
          path: 'violation',
          name: 'MyViolation',
          component: () => import('@/views/user/violation/MyViolation.vue'),
          meta: { title: '违章查询', icon: 'Warning' },
        },
        {
          path: 'ai',
          name: 'AIRecommend',
          component: () => import('@/views/user/ai/AIRecommend.vue'),
          meta: { title: 'AI 推荐', icon: 'ChatDotRound' },
        },
        {
          path: 'profile',
          name: 'UserProfile',
          component: () => import('@/views/user/profile/Profile.vue'),
          meta: { title: '个人中心', icon: 'User' },
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  if (to.path === '/login') {
    next()
  } else {
    if (!token) {
      next('/login')
    } else {
      // 检查角色权限
      if (to.path.startsWith('/admin') && userRole !== 'admin') {
        next('/login')
      } else if (to.path.startsWith('/bookadmin') && userRole !== 'bookadmin') {
        next('/login')
      } else if (to.path.startsWith('/user') && userRole !== 'user') {
        next('/login')
      } else {
        next()
      }
    }
  }
})

export default router
