<template>
  <view class="group-detail">
    <scroll-view scroll-y class="scroll-area">
      <!-- 状态卡片 -->
      <view class="status-card" :class="statusClass">
        <view class="status-header">
          <view class="status-icon">
            <uni-icons :type="statusIcon" size="32" color="#fff" />
          </view>
          <text class="status-text">{{ statusText }}</text>
        </view>

        <view class="progress-bar" v-if="group.status === 0">
          <view class="progress" :style="{ width: progressPercent + '%' }" />
        </view>
        <view class="people-info">
          <text>已参与 {{ group.currentPeople }}/{{ group.targetPeople }} 人</text>
          <text class="remaining" v-if="group.status === 0 && remainingTime">
            剩余 {{ remainingTime }} 结束
          </text>
        </view>
      </view>

      <!-- 参与者列表 -->
      <view class="participants-card">
        <view class="section-title">拼团成员</view>
        <view class="participant-list">
          <view class="participant" v-for="p in participants" :key="p.id">
            <image :src="p.userAvatar || '/static/default-avatar.png'" class="avatar" />
            <view class="info">
              <text class="name">{{ p.userNickname || '用户' }}</text>
              <view class="badge leader" v-if="p.isLeader">团长</view>
            </view>
            <text class="status" :class="'status-' + p.status">{{ participantStatusText(p.status) }}</text>
          </view>
        </view>
      </view>

      <!-- 商品信息 -->
      <view class="product-card" v-if="activity">
        <image :src="activity.modelImage" class="product-img" mode="aspectFill" />
        <view class="product-info">
          <text class="name">{{ activity.activityName }}</text>
          <view class="price-row">
            <text class="price">¥{{ activity.groupPrice }}</text>
            <text class="original">¥{{ activity.originalPrice }}</text>
          </view>
        </view>
      </view>

      <!-- 我的订单信息 -->
      <view class="order-card" v-if="myParticipant && myParticipant.totalAmount">
        <view class="section-title">我的订单</view>
        <view class="order-info">
          <view class="order-row">
            <text class="order-label">单价</text>
            <text class="order-value">¥{{ myParticipant.unitPrice }}</text>
          </view>
          <view class="order-row">
            <text class="order-label">数量</text>
            <text class="order-value">× {{ myParticipant.quantity }}</text>
          </view>
          <view class="order-row total">
            <text class="order-label">应付金额</text>
            <text class="order-value">¥{{ myParticipant.totalAmount }}</text>
          </view>
        </view>
      </view>

      <!-- 分享提示 -->
      <view class="share-tip" v-if="group.status === 0 && !hasJoined">
        <text>邀请好友一起拼团，{{ group.targetPeople }}人即可成功</text>
      </view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <template v-if="group.status === 0 && !hasJoined">
        <button class="join-btn" @click="handleJoin">参与拼团</button>
      </template>
      <template v-else-if="group.status === 0 && hasJoined && myParticipant?.status === 0">
        <view class="price-info">
          <text class="label">待支付：</text>
          <text class="amount">¥{{ myParticipant?.totalAmount || '0.00' }}</text>
        </view>
        <button class="pay-btn" @click="handlePay">立即支付</button>
      </template>
      <template v-else-if="group.status === 1">
        <button class="view-order-btn" @click="viewOrder">查看订单</button>
      </template>
      <template v-else-if="group.status === 2 || group.status === 3">
        <button class="retry-btn" @click="goActivityDetail">重新拼团</button>
      </template>

      <button class="share-btn" v-if="group.status === 0" @click="handleShare">
        <uni-icons type="redo" size="18" />
        <text>邀请好友</text>
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { getGroupDetailApi, getGroupByShareCodeApi, joinGroupBuyApi, createOrderForParticipantApi } from '@/api/groupBuy'
import {
  createAlipayAppPayApi,
  payOrderByWalletApi,
  getOrderPayStatusApi,
  syncOrderPayStatusApi
} from '@/api/order'
import { getWalletAccountApi } from '@/api/wallet'
import { getApiBaseUrl } from '@/utils/apiBase'

const groupId = ref(null)
const shareCode = ref(null)
const group = ref({})
const activity = ref(null)
const participants = ref([])
const hasJoined = ref(false)
const myParticipant = ref(null)
const remainingTime = ref('')
const paying = ref(false)
let timer = null

const statusClass = computed(() => {
  const status = group.value.status
  if (status === 0) return 'status-progress'
  if (status === 1) return 'status-success'
  return 'status-failed'
})

const statusIcon = computed(() => {
  const status = group.value.status
  if (status === 0) return 'info'
  if (status === 1) return 'checkmarkempty'
  return 'closeempty'
})

const statusText = computed(() => {
  const status = group.value.status
  if (status === 0) return '拼团中'
  if (status === 1) return '拼团成功'
  if (status === 2) return '拼团失败'
  return '已取消'
})

const progressPercent = computed(() => {
  if (!group.value.targetPeople) return 0
  return Math.min(100, (group.value.currentPeople / group.value.targetPeople) * 100)
})

const participantStatusText = (status) => {
  const map = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款' }
  return map[status] || '未知'
}

const loadDetail = async () => {
  try {
    let res
    if (groupId.value) {
      res = await getGroupDetailApi(groupId.value)
    } else if (shareCode.value) {
      res = await getGroupByShareCodeApi(shareCode.value)
    } else {
      return
    }

    group.value = res || {}
    activity.value = res?.activity || null
    participants.value = res?.participants || []
    hasJoined.value = res?.hasJoined || false
    myParticipant.value = res?.myParticipant || null
  } catch (e) {
    console.error('加载拼团详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const startCountdown = () => {
  const updateRemaining = () => {
    if (!group.value.expireTime || group.value.status !== 0) {
      remainingTime.value = ''
      return
    }

    const now = Date.now()
    const expire = new Date(group.value.expireTime).getTime()
    const diff = expire - now

    if (diff <= 0) {
      remainingTime.value = ''
      return
    }

    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)

    if (hours > 0) {
      remainingTime.value = `${hours}小时${minutes}分`
    } else if (minutes > 0) {
      remainingTime.value = `${minutes}分${seconds}秒`
    } else {
      remainingTime.value = `${seconds}秒`
    }
  }

  updateRemaining()
  timer = setInterval(updateRemaining, 1000)
}

const handleJoin = () => {
  uni.navigateTo({
    url: `/pages/group-buy/join?groupId=${group.value.id}`
  })
}

// 支付相关方法
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const isAppPlus = () => {
  // #ifdef APP-PLUS
  return true
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
}

const getPaymentProviders = () => {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    if (typeof plus !== 'undefined' && plus?.payment?.getChannels) {
      plus.payment.getChannels(
        (channels) => resolve(channels.map(c => c.id || c.name || '').filter(Boolean)),
        () => resolve([])
      )
      return
    }
    // #endif

    uni.getProvider({
      service: 'payment',
      success: (res) => resolve(res?.providers || []),
      fail: () => resolve([])
    })
  })
}

const ensureAlipayAvailable = async () => {
  if (!isAppPlus()) {
    uni.showToast({ title: '请在App端支付', icon: 'none' })
    return false
  }
  const providers = await getPaymentProviders()
  if (!providers.includes('alipay')) {
    uni.showToast({ title: '未检测到支付宝通道', icon: 'none' })
    return false
  }
  return true
}

const withSandboxOrderInfo = (orderInfo) => {
  const raw = String(orderInfo || '').trim()
  if (!raw) return raw
  if (/([?&])bizcontext=/.test(raw)) return raw
  const bizContext = encodeURIComponent(JSON.stringify({ appenv: 'system' }))
  return `${raw}&bizcontext=${bizContext}`
}

const ensureAlipaySandboxEnv = () => {
  // #ifdef APP-PLUS
  if (typeof plus === 'undefined') return false
  if (plus?.os?.name !== 'Android') return true
  try {
    const EnvUtils = plus.android.importClass('com.alipay.sdk.app.EnvUtils')
    EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
    return true
  } catch (error) {
    console.error('设置支付宝沙箱环境失败:', error)
    return false
  }
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
}

const pollPayStatus = async (orderId, maxRetry = 6) => {
  for (let i = 0; i < maxRetry; i += 1) {
    try {
      const status = await getOrderPayStatusApi(orderId)
      if (status?.payStatus === 1 || status?.orderStatus === 1) {
        return true
      }
    } catch (error) {
      // 忽略单次轮询异常
    }
    await sleep(2000)
  }
  return false
}

const handlePay = async () => {
  if (paying.value) return
  if (!myParticipant.value?.id) {
    uni.showToast({ title: '订单信息错误', icon: 'none' })
    return
  }

  paying.value = true
  uni.showLoading({ title: '正在支付...' })

  try {
    const participantId = myParticipant.value.id
    let orderId = myParticipant.value.orderId
    const totalAmount = Number(myParticipant.value.totalAmount || 0)

    // 如果没有订单，先创建订单
    if (!orderId) {
      uni.showLoading({ title: '创建订单中...' })
      try {
        orderId = await createOrderForParticipantApi(participantId)
        // 更新本地数据
        myParticipant.value.orderId = orderId
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: e?.message || '创建订单失败', icon: 'none' })
        paying.value = false
        return
      }
    }

    uni.showLoading({ title: '正在支付...' })

    // 优先尝试余额支付
    let walletBalance = 0
    try {
      const wallet = await getWalletAccountApi()
      walletBalance = Number(wallet?.availableBalance || 0)
    } catch (e) {
      // 忽略
    }

    // 如果余额充足，优先使用余额
    if (walletBalance >= totalAmount) {
      try {
        await payOrderByWalletApi({ orderId })
        uni.hideLoading()
        uni.showToast({ title: '支付成功', icon: 'success' })
        await loadDetail()
        paying.value = false
        return
      } catch (walletError) {
        const msg = walletError?.message || walletError?.errMsg || ''
        if (!String(msg).includes('余额不足')) {
          throw walletError
        }
        // 余额不足，继续尝试支付宝
      }
    }

    // 支付宝支付
    const alipayReady = await ensureAlipayAvailable()
    if (!alipayReady) {
      uni.hideLoading()
      paying.value = false
      return
    }

    ensureAlipaySandboxEnv()

    const payResult = await createAlipayAppPayApi({ orderId })
    uni.hideLoading()

    await new Promise((resolve, reject) => {
      uni.requestPayment({
        provider: 'alipay',
        orderInfo: withSandboxOrderInfo(payResult.orderString),
        success: () => resolve(true),
        fail: (err) => reject(err)
      })
    })

    uni.showLoading({ title: '支付结果确认中...' })

    try {
      await syncOrderPayStatusApi(orderId)
    } catch (e) {
      // 忽略同步错误
    }

    const paid = await pollPayStatus(orderId)
    uni.hideLoading()

    if (!paid) {
      uni.showToast({ title: '支付结果确认超时', icon: 'none' })
    } else {
      uni.showToast({ title: '支付成功', icon: 'success' })
      await loadDetail()
    }
  } catch (error) {
    uni.hideLoading()
    const message = error?.errMsg || error?.message || '支付失败'
    if (String(message).includes('cancel')) {
      uni.showToast({ title: '您已取消支付', icon: 'none' })
    } else {
      uni.showToast({ title: message, icon: 'none' })
    }
  } finally {
    paying.value = false
  }
}

const viewOrder = () => {
  if (myParticipant.value?.orderId) {
    uni.navigateTo({
      url: `/pages/user/order-detail?id=${myParticipant.value.orderId}`
    })
  }
}

const goActivityDetail = () => {
  if (activity.value?.id) {
    uni.navigateTo({
      url: `/pages/group-buy/activity-detail?id=${activity.value.id}`
    })
  }
}

const handleShare = () => {
  if (!group.value?.shareCode) {
    uni.showToast({ title: '拼团信息加载中', icon: 'none' })
    return
  }

  // #ifdef APP-PLUS
  // App端使用原生分享
  const shareTitle = `快来和我一起拼团「${activity.value?.activityName || '好物'}」`
  const shareContent = `还差${group.value.targetPeople - group.value.currentPeople}人即可成团，拼团价¥${activity.value?.groupPrice}`
  const shareUrl = `${getApiBaseUrl()}/pages/group-buy/group-detail?shareCode=${group.value.shareCode}`

  uni.share({
    provider: 'weixin',
    scene: 'WXSceneSession',
    type: 0,
    title: shareTitle,
    summary: shareContent,
    href: shareUrl,
    imageUrl: activity.value?.modelImage || '',
    success: () => {
      uni.showToast({ title: '分享成功', icon: 'success' })
    },
    fail: (err) => {
      console.error('分享失败', err)
      // 尝试复制链接
      uni.setClipboardData({
        data: shareUrl,
        success: () => {
          uni.showToast({ title: '链接已复制', icon: 'success' })
        }
      })
    }
  })
  // #endif

  // #ifndef APP-PLUS
  // H5/小程序端复制分享链接
  const shareLink = `/pages/group-buy/group-detail?shareCode=${group.value.shareCode}`
  uni.setClipboardData({
    data: `${getApiBaseUrl()}${shareLink}`,
    success: () => {
      uni.showToast({ title: '分享链接已复制', icon: 'success' })
    }
  })
  // #endif
}

onLoad((options) => {
  groupId.value = options?.id
  shareCode.value = options?.shareCode
  loadDetail()
})

onMounted(() => {
  startCountdown()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

onShareAppMessage(() => {
  return {
    title: `快来和我一起拼团「${activity.value?.activityName || '好物'}」`,
    path: `/pages/group-buy/group-detail?shareCode=${group.value.shareCode}`
  }
})
</script>

<style lang="scss" scoped>
.group-detail {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 120rpx;
}

.scroll-area {
  height: calc(100vh - 120rpx);
}

.status-card {
  padding: 40rpx 30rpx;
  color: #fff;
}

.status-card.status-progress {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
}

.status-card.status-success {
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%);
}

.status-card.status-failed {
  background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
}

.status-header {
  display: flex;
  align-items: center;
}

.status-icon {
  width: 64rpx;
  height: 64rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  font-size: 36rpx;
  font-weight: bold;
  margin-left: 20rpx;
}

.progress-bar {
  height: 8rpx;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4rpx;
  margin-top: 30rpx;
  overflow: hidden;
}

.progress {
  height: 100%;
  background: #fff;
  border-radius: 4rpx;
  transition: width 0.3s;
}

.people-info {
  display: flex;
  justify-content: space-between;
  margin-top: 20rpx;
  font-size: 26rpx;
  opacity: 0.9;
}

.participants-card {
  background: #fff;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.participant-list {
  display: flex;
  flex-direction: column;
}

.participant {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f6f8;
}

.participant:last-child {
  border-bottom: none;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #f5f6f8;
}

.info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  align-items: center;
}

.name {
  font-size: 28rpx;
  color: #333;
}

.badge {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  margin-left: 12rpx;
}

.badge.leader {
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.status {
  font-size: 24rpx;
}

.status-0 {
  color: #ff6b6b;
}

.status-1 {
  color: #2ecc71;
}

.status-2,
.status-3 {
  color: #999;
}

.product-card {
  display: flex;
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.product-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.product-info .name {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
}

.product-info .price-row {
  display: flex;
  align-items: baseline;
  margin-top: 12rpx;
}

.product-info .price {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.product-info .original {
  font-size: 24rpx;
  color: #999;
  text-decoration: line-through;
  margin-left: 12rpx;
}

.order-card {
  background: #fff;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.order-info {
  display: flex;
  flex-direction: column;
}

.order-row {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.order-label {
  font-size: 26rpx;
  color: #666;
}

.order-value {
  font-size: 26rpx;
  color: #333;
}

.order-row.total {
  border-top: 1rpx solid #eee;
  margin-top: 8rpx;
  padding-top: 16rpx;
}

.order-row.total .order-label {
  font-weight: bold;
  color: #333;
}

.order-row.total .order-value {
  font-size: 32rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.share-tip {
  background: #fffbe6;
  padding: 20rpx 30rpx;
  text-align: center;
}

.share-tip text {
  font-size: 26rpx;
  color: #faad14;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 20rpx 30rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.join-btn,
.pay-btn,
.view-order-btn,
.retry-btn {
  flex: 1;
  height: 88rpx;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 44rpx;
}

.price-info {
  flex: 1;
  display: flex;
  align-items: baseline;
}

.price-info .label {
  font-size: 26rpx;
  color: #666;
}

.price-info .amount {
  font-size: 40rpx;
  font-weight: bold;
  color: #ff6b6b;
}

.share-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 30rpx;
  height: 88rpx;
  background: #f5f6f8;
  border-radius: 44rpx;
  font-size: 28rpx;
  color: #666;
}

.share-btn text {
  margin-left: 8rpx;
}
</style>
