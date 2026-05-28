<template>
  <view class="user-container">
    <!-- Hero Header -->
    <view class="hero-section">
      <view class="hero-bg"></view>

      <!-- Header Actions -->
      <view class="header-actions">
        <view class="action-btn message-entry" @click="goMessageCenter">
          <uni-icons type="email" size="22" color="#fff"></uni-icons>
          <view v-if="messageUnread > 0" class="message-badge">
            {{ messageUnread > 99 ? '99+' : messageUnread }}
          </view>
        </view>
        <view class="action-btn" @click="uni.navigateTo({ url: '/pages/user/settings' })">
          <uni-icons type="gear" size="22" color="#fff"></uni-icons>
        </view>
      </view>

      <!-- Profile Card -->
      <view class="profile-card" @click="uni.navigateTo({ url: '/pages/user/profile' })">
        <view class="avatar-ring">
          <image :src="userInfo.avatar" class="avatar" mode="aspectFill"></image>
        </view>
        <view class="profile-info">
          <text class="nickname">{{ userInfo.nickname }}</text>
          <view class="role-badge">
            <text>{{ roleLabel }}</text>
          </view>
          <text class="user-id">ID: {{ userInfo.id }}</text>
        </view>
        <view class="profile-arrow">
          <uni-icons type="right" size="18" color="rgba(255,255,255,0.6)"></uni-icons>
        </view>
      </view>

      <!-- Stats Row -->
      <view class="stats-bar">
        <view class="stat-item" @click="uni.navigateTo({ url: '/pages/user/wallet' })">
          <text class="stat-value">¥{{ walletAmount }}</text>
          <text class="stat-label">余额</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="goFavoriteModels">
          <text class="stat-value">{{ favoriteCount }}</text>
          <text class="stat-label">收藏</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="goPrintTasks">
          <text class="stat-value">{{ printTaskCount }}</text>
          <text class="stat-label">打印</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="uni.navigateTo({ url: '/pages/user/points' })">
          <text class="stat-value">{{ pointAmount }}</text>
          <text class="stat-label">积分</text>
        </view>
      </view>
    </view>

    <!-- Orders Section -->
    <view class="section-wrap">
      <view class="section-card">
        <view class="section-header" @click="goOrderList()">
          <view class="header-left">
            <view class="header-icon">
              <uni-icons type="shop" size="20" color="#00bfff"></uni-icons>
            </view>
            <text class="header-title">我的订单</text>
          </view>
          <view class="header-right">
            <text class="view-all">全部订单</text>
            <uni-icons type="right" size="14" color="#c0c8d4"></uni-icons>
          </view>
        </view>

        <view class="order-grid">
          <view
            class="order-item"
            v-for="(item, index) in orderStatus"
            :key="index"
            @click="goOrderList(item)"
            :style="{ animationDelay: `${index * 0.08}s` }"
          >
            <view class="order-icon-wrap">
              <view class="icon-bg" :class="item.iconClass">
                <uni-icons :type="item.icon" size="24" color="#fff"></uni-icons>
              </view>
              <view v-if="item.badge > 0" class="order-badge">
                {{ item.badge > 99 ? '99+' : item.badge }}
              </view>
            </view>
            <text class="order-name">{{ item.name }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Menu Section -->
    <view class="section-wrap">
      <view class="menu-card">
        <view
          class="menu-item"
          v-for="(menu, idx) in menus"
          :key="idx"
          @click="goMenu(menu)"
          :style="{ animationDelay: `${idx * 0.04}s` }"
        >
          <view class="menu-icon-wrap" :style="{ background: (menu.iconColor || '#00bfff') + '12' }">
            <uni-icons :type="menu.icon" size="22" :color="menu.iconColor || '#00bfff'"></uni-icons>
          </view>
          <text class="menu-name">{{ menu.name }}</text>
          <view class="menu-arrow">
            <uni-icons type="right" size="16" color="#d0d5dd"></uni-icons>
          </view>
        </view>
      </view>
    </view>

    <!-- Logout Button -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">
        <uni-icons type="poweroff" size="18" color="#ff4d6d"></uni-icons>
        <text>退出登录</text>
      </button>
    </view>

    <!-- #ifdef APP-PLUS -->
    <AppTabbar />
    <!-- #endif -->
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'
import { doLogout, ensureLoginOrRedirect } from '../../utils/auth'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'
import { getWalletAccountApi } from '../../api/wallet'
import { getPointAccountApi } from '../../api/point'
import { getMyFavoriteModelIdsApi } from '../../api/model'
import { getMyAfterSaleListApi, getMyOrdersApi } from '../../api/order'
import { getMyBuyUsedOrderPageApi, getMyUsedAfterSalePageApi } from '../../api/used'
import { refreshNotificationSummary, NOTIFICATION_SUMMARY_EVENT } from '../../utils/notificationRuntime'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const userInfo = ref({
  nickname: 'Admin',
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
  id: '3D_88889999',
  role: 'user'
})

const walletAmount = ref('0.00')
const pointAmount = ref(0)
const favoriteCount = ref(0)
const printTaskCount = ref(0)
const messageUnread = ref(0)

const userRole = ref('user')
const roleLabel = computed(() => (isDesignerRole(userRole.value) ? '设计者' : '普通用户'))

onShow(() => {
  if (!ensureLoginOrRedirect()) return

  const saved = uni.getStorageSync('user_profile')
  const role = getStoredUserRole()
  if (saved) {
    userInfo.value.nickname = saved.nickname
    userInfo.value.avatar = saved.avatar
    userInfo.value.id = saved.id || userInfo.value.id
    userInfo.value.role = role
    uni.setStorageSync('user_profile', {
      ...saved,
      role
    })
  }
  userRole.value = role
  loadOverview()
  refreshNotificationSummary()
})

const loadOverview = async () => {
  await Promise.all([loadAssetOverview(), loadOrderOverview()])
}

const handleNotificationSummary = (summary = {}) => {
  messageUnread.value = Number(summary?.totalUnread || 0)
}

uni.$on(NOTIFICATION_SUMMARY_EVENT, handleNotificationSummary)

onUnload(() => {
  uni.$off(NOTIFICATION_SUMMARY_EVENT, handleNotificationSummary)
})

const getPageTotal = (pageData) => {
  const total = Number(pageData?.total)
  if (Number.isFinite(total) && total >= 0) return total
  const records = Array.isArray(pageData?.records) ? pageData.records : []
  return records.length
}

const safePageTotal = async (loader) => {
  try {
    const result = await loader()
    return getPageTotal(result)
  } catch (_) {
    return 0
  }
}

const loadAssetOverview = async () => {
  try {
    const [wallet, point, favoriteIds] = await Promise.all([
      getWalletAccountApi(),
      getPointAccountApi(),
      getMyFavoriteModelIdsApi()
    ])
    walletAmount.value = Number(wallet?.availableBalance || 0).toFixed(2)
    pointAmount.value = Number(point?.availablePoints || 0)
    favoriteCount.value = Array.isArray(favoriteIds) ? favoriteIds.length : 0
  } catch (_) {
    // 资产数据加载失败时不阻塞页面
  }
}

const orderBadge = ref({
  waitPay: 0,
  waitReceive: 0,
  finished: 0,
  afterSale: 0
})

const orderStatus = computed(() => ([
  { name: '待付款', icon: 'wallet', badge: orderBadge.value.waitPay, status: 1, iconClass: 'warning' },
  { name: '待收货', icon: 'gift', badge: orderBadge.value.waitReceive, status: 2, iconClass: 'primary' },
  { name: '已完成', icon: 'checkbox-filled', badge: 0, status: 3, iconClass: 'success' },
  { name: '售后', icon: 'help', badge: orderBadge.value.afterSale, path: '/pages/user/after-sale-list', iconClass: 'danger' }
]))

const loadOrderOverview = async () => {
  try {
    // 主订单售后未完成状态: 0-APPLIED, 1-REVIEWING, 2-PROCESSING, 3-REFUNDING
    // 二手售后未完成状态: 0-APPLIED, 1-APPROVED, 3-PLATFORM_INTERVENTION
    const afterSalePendingStatuses = [0, 1, 2, 3]
    const usedAfterSalePendingStatuses = [0, 1, 3]

    const [waitPay, producing, waitReceiveMain, finishedMain, afterSaleMain, usedWaitPay, usedWaitShip, usedWaitReceive, usedFinished, usedAfterSale] = await Promise.all([
      safePageTotal(() => getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 0 })),
      safePageTotal(() => getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 1 })),
      safePageTotal(() => getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 2 })),
      safePageTotal(() => getMyOrdersApi({ pageNum: 1, pageSize: 1, orderStatus: 3 })),
      safePageTotal(() => getMyAfterSaleListApi({ pageNum: 1, pageSize: 1, statuses: afterSalePendingStatuses })),
      safePageTotal(() => getMyBuyUsedOrderPageApi({ pageNum: 1, pageSize: 1, status: 0 })),
      safePageTotal(() => getMyBuyUsedOrderPageApi({ pageNum: 1, pageSize: 1, status: 1 })),
      safePageTotal(() => getMyBuyUsedOrderPageApi({ pageNum: 1, pageSize: 1, status: 2 })),
      safePageTotal(() => getMyBuyUsedOrderPageApi({ pageNum: 1, pageSize: 1, status: 3 })),
      safePageTotal(() => getMyUsedAfterSalePageApi({ pageNum: 1, pageSize: 1, statuses: usedAfterSalePendingStatuses }))
    ])

    const waitReceive = waitReceiveMain + usedWaitShip + usedWaitReceive
    const finished = finishedMain + usedFinished
    const afterSale = afterSaleMain + usedAfterSale

    printTaskCount.value = producing
    orderBadge.value = {
      waitPay: waitPay + usedWaitPay,
      waitReceive: producing + waitReceive,
      finished,
      afterSale
    }
  } catch (_) {
    printTaskCount.value = 0
    orderBadge.value = {
      waitPay: 0,
      waitReceive: 0,
      finished: 0,
      afterSale: 0
    }
  }
}

const menus = computed(() => {
  const isDesigner = isDesignerRole(userRole.value)
  const list = [
    { name: '推广中心', icon: 'staff', path: '/pages/promotion/index', iconColor: '#ff6b35' },
    { name: '活动赛事', icon: 'flag', path: '/pages/event/list', iconColor: '#667eea' },
    { name: '二手好物广场', icon: 'shop', path: '/pages/used/index', iconColor: '#00bfff' },
    { name: isDesigner ? '悬赏任务广场' : '我的悬赏任务', icon: 'fire', path: '/pages/reward/index', iconColor: '#fbbf24' },
    { name: '我的模型', icon: 'image', path: '/pages/user/models', role: 'designer', iconColor: '#00bfff' },
    { name: '我的帖子', icon: 'chat', path: '/pages/community/my-posts', iconColor: '#00bfff' },
    { name: '我的互动', icon: 'heart', path: '/pages/community/my-interactions', iconColor: '#ff4d6d' },
    { name: '我的清单', icon: 'list', path: '/pages/user/params', role: 'user', iconColor: '#5a6a7a' },
    { name: '在线客服', icon: 'headphones', iconColor: '#00bfff', path: '/pages/custom/customer-service' },

  ]
  return list.filter(item => !item.role || item.role === userRole.value)
})

const goMenu = (menu) => {
  if (menu.path) {
    if (menu.path.includes('index') && (menu.path.includes('community') || menu.path.includes('mall'))) {
      uni.switchTab({ url: menu.path })
    } else {
      uni.navigateTo({ url: menu.path })
    }
  } else {
    uni.showToast({ title: menu.name + '功能演示中', icon: 'none' })
  }
}

const goOrderList = (item = {}) => {
  if (item.path) {
    uni.navigateTo({ url: item.path })
    return
  }
  uni.navigateTo({
    url: '/pages/user/orders?status=' + (item.status || 0)
  })
}

const goFavoriteModels = () => {
  uni.navigateTo({ url: '/pages/user/favorite-models' })
}

const goPrintTasks = () => {
  uni.navigateTo({ url: '/pages/user/orders?status=2' })
}

const goMessageCenter = () => {
  uni.navigateTo({ url: '/pages/user/message-center' })
}

const handleLogout = () => {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    confirmColor: '#ff4d6d',
    success: async (res) => {
      if (!res.confirm) return
      uni.showLoading({ title: '正在退出...' })
      await doLogout()
      uni.hideLoading()
      uni.reLaunch({
        url: '/pages/auth/login'
      })
    }
  })
}
</script>

<style scoped lang="scss">
$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;

$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;
$danger: #ff4d6d;

$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$gradient-primary: linear-gradient(135deg, $sky-blue 0%, $sky-light 100%);

.user-container {
  min-height: 100vh;
  background-color: $surface;
  padding-bottom: 140rpx;
}

/* —— Hero 头部 —— */
.hero-section {
  position: relative;
  padding: 0 28rpx;
  padding-top: calc(env(safe-area-inset-top) + 20rpx);
  padding-bottom: 44rpx;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(145deg, $sky-blue 0%, $sky-deep 100%);
  z-index: 0;

  &::after {
    content: '';
    position: absolute;
    top: -80rpx;
    right: -80rpx;
    width: 360rpx;
    height: 360rpx;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, transparent 70%);
    animation: bgGlow 4s ease-in-out infinite;
  }
}

@keyframes bgGlow {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

.header-actions {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-bottom: 32rpx;
  animation: fadeInDown 0.4s ease forwards;
}

@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-16rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.action-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 999rpx;
  backdrop-filter: blur(8px);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

  &:active {
    transform: scale(0.90);
    background: rgba(255, 255, 255, 0.25);
  }
}

.message-entry {
  position: relative;
}

.message-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: $danger;
  color: #fff;
  font-size: 20rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 109, 0.4);
  animation: badgePulse 2s ease infinite;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

/* —— 个人信息卡 —— */
.profile-card {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 28rpx;
  backdrop-filter: blur(16px);
  margin-bottom: 24rpx;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;

  &:active {
    transform: scale(0.985);
  }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.avatar-ring {
  width: 112rpx;
  height: 112rpx;
  padding: 4rpx;
  background: $gradient-primary;
  border-radius: 999rpx;
  flex-shrink: 0;
  animation: avatarPop 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  opacity: 0;
}

@keyframes avatarPop {
  0% { opacity: 0; transform: scale(0.6); }
  100% { opacity: 1; transform: scale(1); }
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 999rpx;
  border: 4rpx solid $surface-raised;
  background-color: $surface;
}

.profile-info {
  flex: 1;
  margin-left: 24rpx;
}

.nickname {
  font-size: 36rpx;
  font-weight: 700;
  color: $surface-raised;
  display: block;
  margin-bottom: 8rpx;
}

.role-badge {
  display: inline-block;
  padding: 6rpx 18rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999rpx;
  margin-bottom: 8rpx;

  text {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.9);
    font-weight: 500;
  }
}

.user-id {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.65);
  display: block;
}

.profile-arrow {
  display: flex;
  align-items: center;
}

/* —— 数据统计 —— */
.stats-bar {
  position: relative;
  z-index: 2;
  display: flex;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 24rpx;
  padding: 28rpx 16rpx;
  backdrop-filter: blur(12px);
  animation: fadeInUp 0.5s ease 0.15s forwards;
  opacity: 0;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform 0.2s ease;

  &:active {
    transform: scale(0.94);
  }
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: $surface-raised;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
}

.stat-divider {
  width: 1rpx;
  height: 56rpx;
  background: rgba(255, 255, 255, 0.15);
  align-self: center;
}

/* —— 区块容器 —— */
.section-wrap {
  padding: 12rpx 28rpx;
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;

  &:nth-of-type(1) {
    animation-delay: 0.3s;
  }

  &:nth-of-type(2) {
    animation-delay: 0.4s;
  }
}

/* —— 订单卡片 —— */
.section-card {
  background: $surface-raised;
  border-radius: 28rpx;
  padding: 28rpx;
  box-shadow: $shadow-card;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.header-icon {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 191, 255, 0.08);
  border-radius: 14rpx;
}

.header-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $text-primary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.view-all {
  font-size: 24rpx;
  color: $text-muted;
}

/* —— 订单状态网格 —— */
.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.order-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 18rpx 8rpx;
  border-radius: 20rpx;
  background: $surface;
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: jellyPop 0.4s ease forwards;
  opacity: 0;

  &:active {
    transform: scale(0.93);
  }
}

@keyframes jellyPop {
  0% { opacity: 0; transform: scale(0.88); }
  60% { transform: scale(1.04); }
  100% { opacity: 1; transform: scale(1); }
}

.order-icon-wrap {
  position: relative;
  margin-bottom: 12rpx;
}

.icon-bg {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;

  &.primary {
    background: $gradient-primary;
    box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);
  }

  &.warning {
    background: linear-gradient(135deg, #fbbf24 0%, #fcd34d 100%);
    box-shadow: 0 6rpx 20rpx rgba(251, 191, 36, 0.25);
  }

  &.success {
    background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
    box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.25);
  }

  &.danger {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
    box-shadow: 0 6rpx 20rpx rgba(239, 68, 68, 0.25);
  }
}

.order-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: $danger;
  color: #fff;
  font-size: 20rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-name {
  font-size: 24rpx;
  color: $text-secondary;
  font-weight: 500;
}

/* —— 菜单卡片 —— */
.menu-card {
  background: $surface-raised;
  border-radius: 28rpx;
  padding: 8rpx 28rpx;
  box-shadow: $shadow-card;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  transition: transform 0.2s ease;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;

  &:not(:last-child) {
    border-bottom: 1rpx solid rgba(0, 0, 0, 0.03);
  }

  &:active {
    transform: translateX(8rpx);
  }
}

.menu-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
}

.menu-name {
  flex: 1;
  margin-left: 20rpx;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

.menu-arrow {
  display: flex;
  align-items: center;
}

/* —— 退出按钮 —— */
.logout-section {
  padding: 40rpx 28rpx;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  width: 100%;
  height: 96rpx;
  background: $surface-raised;
  border-radius: 999rpx;
  margin: 0;
  padding: 0;
  box-shadow: $shadow-card;

  text {
    font-size: 30rpx;
    color: $danger;
    font-weight: 600;
  }

  &:active {
    transform: scale(0.97);
    background: #fff5f7;
  }
}
</style>
