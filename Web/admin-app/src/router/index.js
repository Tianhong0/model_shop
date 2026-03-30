import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '../layout/MainLayout.vue';
import { useAuthStore } from '../stores/auth';
import { createPinia } from 'pinia';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/login',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Index.vue'),
        meta: { title: '仪表盘', requiresAuth: true }
      },
      {
        path: 'users',
        name: 'UserCenter',
        meta: { title: '用户中心', requiresAuth: true },
        redirect: '/users/list',
        children: [
          {
            path: 'list',
            name: 'UserList',
            component: () => import('../views/users/UserList.vue'),
            meta: { title: '用户管理', requiresAuth: true }
          },
          {
            path: 'deletion-requests',
            name: 'DeletionRequests',
            component: () => import('../views/users/DeletionRequests.vue'),
            meta: { title: '注销申请管理', requiresAuth: true }
          },
          {
            path: 'admin-register-requests',
            name: 'AdminRegisterRequests',
            component: () => import('../views/users/AdminRegisterRequests.vue'),
            meta: { title: '管理员注册审核', requiresAuth: true }
          },
          {
            path: 'designer-apply-requests',
            name: 'DesignerApplyRequests',
            component: () => import('../views/users/DesignerApplyRequests.vue'),
            meta: { title: '设计者申请审核', requiresAuth: true }
          }
        ]
      },
      {
        path: 'models',
        name: 'ModelManagement',
        meta: { title: '模型管理', requiresAuth: true },
        redirect: '/models/list',
        children: [
          {
            path: 'category',
            name: 'ModelCategory',
            component: () => import('../views/models/CategoryList.vue'),
            meta: { title: '模型分类', requiresAuth: true }
          },
          {
            path: 'list',
            name: 'ModelList',
            component: () => import('../views/models/ModelMall.vue'),
            meta: { title: '模型管理', requiresAuth: true }
          },
          {
            path: 'model-lists',
            name: 'ModelListManage',
            component: () => import('../views/model-list/ModelListManage.vue'),
            meta: { title: '清单管理', requiresAuth: true }
          }
        ]
      },
      {
        path: 'orders',
        name: 'OrderSystem',
        meta: { title: '订单系统', requiresAuth: true },
        redirect: '/orders/list',
        children: [
          {
            path: 'list',
            name: 'OrderList',
            component: () => import('../views/orders/OrderList.vue'),
            meta: { title: '订单管理', requiresAuth: true }
          },
          {
            path: 'after-sales',
            name: 'AfterSales',
            component: () => import('../views/orders/AfterSales.vue'),
            meta: { title: '售后管理', requiresAuth: true }
          },
          {
            path: 'logistics',
            name: 'Logistics',
            component: () => import('../views/orders/Logistics.vue'),
            meta: { title: '物流管理', requiresAuth: true }
          },
          {
            path: 'reviews',
            name: 'OrderReview',
            component: () => import('../views/orders/ReviewList.vue'),
            meta: { title: '订单评价', requiresAuth: true }
          }
        ]
      },
      {
        path: 'used/listings',
        name: 'UsedListingManage',
        component: () => import('../views/used/ListingManage.vue'),
        meta: { title: '二手商品管理', requiresAuth: true }
      },
      {
        path: 'used/orders',
        name: 'UsedOrderManage',
        component: () => import('../views/used/OrderManage.vue'),
        meta: { title: '二手订单管理', requiresAuth: true }
      },
      {
        path: 'used/reports',
        name: 'UsedReportManage',
        component: () => import('../views/used/ReportManage.vue'),
        meta: { title: '二手举报处理', requiresAuth: true }
      },
      {
        path: 'print-queue',
        name: 'PrintQueue',
        component: () => import('../views/print/Queue.vue'),
        meta: { title: '打印排产', requiresAuth: true }
      },
      {
        path: 'print/printers',
        name: 'PrintPrinters',
        component: () => import('../views/print/Printers.vue'),
        meta: { title: '打印机管理', requiresAuth: true }
      },
      {
        path: 'bounty',
        name: 'BountyList',
        component: () => import('../views/bounty/BountyManage.vue'),
        meta: { title: '任务悬赏', requiresAuth: true }
      },
      {
        path: 'bounty/appeal',
        name: 'BountyAppeal',
        component: () => import('../views/bounty/BountyAppealManage.vue'),
        meta: { title: '评价申诉', requiresAuth: true }
      },
      {
        path: 'customer-service',
        name: 'CustomerService',
        component: () => import('../views/customer-service/Index.vue'),
        meta: { title: '客服管理', requiresAuth: true }
      },
      {
        path: 'community/posts',
        name: 'CommunityPostList',
        component: () => import('../views/community/ForumList.vue'),
        meta: { title: '社区帖子', requiresAuth: true }
      },
      {
        path: 'community/replies',
        name: 'CommunityReplyList',
        component: () => import('../views/community/EventList.vue'),
        meta: { title: '社区回复', requiresAuth: true }
      },
      {
        path: 'community/categories',
        name: 'CommunityCategoryList',
        component: () => import('../views/community/BountyList.vue'),
        meta: { title: '社区分类', requiresAuth: true }
      },
      {
        path: 'config',
        name: 'SystemConfig',
        component: () => import('../views/system/Config.vue'),
        meta: { title: '系统配置', requiresAuth: true }
      },
      {
        path: 'operation/banners',
        name: 'BannerManage',
        component: () => import('../views/system/BannerManage.vue'),
        meta: { title: '轮播管理', requiresAuth: true }
      },
      {
        path: 'operation/notices',
        name: 'NoticeManage',
        component: () => import('../views/system/NoticeManage.vue'),
        meta: { title: '公告管理', requiresAuth: true }
      },
      {
        path: 'finance/withdraws',
        name: 'FinanceWithdraws',
        component: () => import('../views/finance/WithdrawManage.vue'),
        meta: { title: '提现管理', requiresAuth: true }
      },
      {
        path: 'events',
        name: 'EventManage',
        component: () => import('../views/event/EventManage.vue'),
        meta: { title: '活动管理', requiresAuth: true }
      },
      {
        path: 'event-submissions',
        name: 'EventSubmissionManage',
        component: () => import('../views/event/SubmissionManage.vue'),
        meta: { title: '作品管理', requiresAuth: true }
      },
      {
        path: 'event-participations',
        name: 'EventParticipationManage',
        component: () => import('../views/event/ParticipationManage.vue'),
        meta: { title: '报名管理', requiresAuth: true }
      },
      {
        path: 'group-buy/activities',
        name: 'GroupBuyActivityList',
        component: () => import('../views/group-buy/ActivityList.vue'),
        meta: { title: '拼团活动', requiresAuth: true }
      },
      {
        path: 'group-buy/batch-discount',
        name: 'BatchDiscountConfig',
        component: () => import('../views/group-buy/BatchDiscount.vue'),
        meta: { title: '批量打印折扣', requiresAuth: true }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 真实的登录态拦截
router.beforeEach(async (to, from, next) => {
  const pinia = createPinia()
  const authStore = useAuthStore(pinia)

  // 检查是否需要登录
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && authStore.isLoggedIn) {
    // 已登录用户访问登录页，重定向到首页
    next('/dashboard')
  } else {
    next()
  }
})

export default router;
