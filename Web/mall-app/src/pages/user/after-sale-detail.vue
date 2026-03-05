<template>
  <view class="page">
    <view class="card" v-if="detail">
      <view class="row"><text class="label">售后单号</text><text class="value">{{ detail.afterSaleSn }}</text></view>
      <view class="row clickable" @click="goOrderDetail"><text class="label">订单号</text><text class="value">{{ detail.orderSn }} ></text></view>
      <view class="row"><text class="label">类型</text><text class="value">{{ typeText(detail.type) }}</text></view>
      <view class="row"><text class="label">状态</text><text class="value status">{{ statusText(detail.status) }}</text></view>
      <view class="row"><text class="label">原因</text><text class="value">{{ detail.reason }}</text></view>
      <view class="row"><text class="label">审核备注</text><text class="value">{{ detail.adminRemark || '-' }}</text></view>
      <view class="row"><text class="label">申请金额</text><text class="value">￥{{ Number(detail.requestedAmount || 0).toFixed(2) }}</text></view>
    </view>

    <view class="card">
      <view class="title">协商记录</view>
      <view class="msg-list" v-if="messages.length">
        <view class="msg-item" v-for="item in messages" :key="item.id">
          <view class="meta">{{ item.senderRole }} · {{ item.createTime }}</view>
          <view class="content">{{ item.content }}</view>
        </view>
      </view>
      <view class="empty" v-else>暂无协商记录</view>

      <textarea class="textarea" v-model="messageContent" placeholder="输入留言内容" />
      <button class="btn" @click="sendMessage">发送留言</button>
    </view>

    <button class="btn cancel" v-if="detail && (detail.status === 0 || detail.status === 1)" @click="cancelAfterSale">撤销申请</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { cancelAfterSaleApi, cancelAfterSaleBySnApi, getAfterSaleMessagesApi, getMyAfterSaleDetailApi, getMyAfterSaleDetailBySnApi, sendAfterSaleMessageApi } from '../../api/order'
import { getApiBaseUrl } from '../../utils/apiBase'

const afterSaleId = ref(null)
const afterSaleSn = ref('')
const detail = ref(null)
const messages = ref([])
const messageContent = ref('')
const socketTask = ref(null)
const socketConnected = ref(false)
const socketManualClose = ref(false)
const reconnectTimer = ref(null)

const closeSocket = () => {
  socketManualClose.value = true
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }
  if (socketTask.value) {
    try {
      socketTask.value.close({ code: 1000, reason: 'manual-close' })
    } catch (_) {
      // ignore
    }
  }
  socketTask.value = null
  socketConnected.value = false
}

const buildWsUrl = () => {
  const token = uni.getStorageSync('token') || ''
  const base = String(getApiBaseUrl() || '').trim().replace(/\/+$/, '')
  const wsBase = base.startsWith('https://') ? base.replace('https://', 'wss://') : base.replace('http://', 'ws://')
  return `${wsBase}/ws/after-sale/message?afterSaleId=${encodeURIComponent(String(afterSaleId.value || ''))}&token=${encodeURIComponent(token)}`
}

const connectSocket = () => {
  if (!afterSaleId.value) return
  closeSocket()
  socketManualClose.value = false

  const task = uni.connectSocket({
    url: buildWsUrl(),
    complete: () => {}
  })

  task.onOpen(() => {
    socketConnected.value = true
  })

  task.onMessage((res) => {
    if (!res?.data) return
    try {
      const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
      if (payload?.eventType !== 'NEW_MESSAGE') return
      if (String(payload?.afterSaleId || '') !== String(afterSaleId.value || '')) return
      const incoming = payload.message
      if (!incoming?.id) return
      if (messages.value.some(item => String(item.id || '') === String(incoming.id || ''))) return
      messages.value.push(incoming)
    } catch (_) {
      // ignore
    }
  })

  task.onClose(() => {
    socketConnected.value = false
    if (!socketManualClose.value) {
      reconnectTimer.value = setTimeout(() => {
        connectSocket()
      }, 2500)
    }
  })

  task.onError(() => {
    socketConnected.value = false
  })

  socketTask.value = task
}

const typeText = (type) => ({ 1: '仅退款', 2: '退货退款', 3: '补打', 4: '换货' }[Number(type)] || '未知')
const statusText = (status) => ({ 0: '已申请', 1: '审核中', 2: '处理中', 3: '退款中', 4: '已完成', 5: '已拒绝', 6: '已取消' }[Number(status)] || '未知')

const loadDetail = async () => {
  if (afterSaleSn.value) {
    detail.value = await getMyAfterSaleDetailBySnApi(afterSaleSn.value)
  } else if (afterSaleId.value) {
    detail.value = await getMyAfterSaleDetailApi(afterSaleId.value)
  } else {
    return
  }
  if (detail.value?.id) {
    afterSaleId.value = String(detail.value.id)
  }
  if (detail.value?.afterSaleSn && !afterSaleSn.value) {
    afterSaleSn.value = String(detail.value.afterSaleSn)
  }
}

const loadMessages = async () => {
  if (!afterSaleId.value) return
  const data = await getAfterSaleMessagesApi({ afterSaleId: afterSaleId.value, pageNum: 1, pageSize: 100 })
  messages.value = (data?.records || []).slice().reverse()
}

const sendMessage = async () => {
  if (!messageContent.value) {
    uni.showToast({ title: '请输入留言', icon: 'none' })
    return
  }
  try {
    await sendAfterSaleMessageApi({
      afterSaleId: afterSaleId.value,
      content: messageContent.value,
      messageType: 1
    })
    messageContent.value = ''
    if (!socketConnected.value) {
      await loadMessages()
    }
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  }
}

const cancelAfterSale = () => {
  uni.showModal({
    title: '提示',
    content: '确定撤销当前售后申请吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        if (afterSaleSn.value) {
          await cancelAfterSaleBySnApi(afterSaleSn.value)
        } else {
          await cancelAfterSaleApi(afterSaleId.value)
        }
        uni.showToast({ title: '已撤销', icon: 'success' })
        await loadDetail()
      } catch (error) {
        uni.showToast({ title: error.message || '撤销失败', icon: 'none' })
      }
    }
  })
}

const goOrderDetail = () => {
  const orderSn = detail.value?.orderSn
  if (orderSn) {
    uni.navigateTo({ url: `/pages/user/order-detail?orderSn=${encodeURIComponent(orderSn)}&id=${encodeURIComponent(detail.value?.orderId || '')}` })
    return
  }
  if (detail.value?.orderId) {
    uni.navigateTo({ url: `/pages/user/order-detail?id=${encodeURIComponent(detail.value.orderId)}` })
    return
  }
  uni.showToast({ title: '订单信息缺失', icon: 'none' })
}

onLoad(async (options) => {
  afterSaleSn.value = options?.afterSaleSn ? String(options.afterSaleSn) : ''
  afterSaleId.value = options?.id ? String(options.id) : null
  if (!afterSaleSn.value && !afterSaleId.value) {
    uni.showToast({ title: '参数无效', icon: 'none' })
    return
  }
  try {
    await Promise.all([loadDetail(), loadMessages()])
    connectSocket()
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
})

onUnload(() => {
  closeSocket()
})
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f8fafc; padding: 20rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 22rpx; margin-bottom: 16rpx; }
.row { display: flex; justify-content: space-between; min-height: 70rpx; align-items: center; border-bottom: 1px solid #f1f5f9; }
.row.clickable:active { opacity: .7; }
.label { color: #64748b; font-size: 24rpx; }
.value { color: #1e293b; font-size: 24rpx; max-width: 500rpx; text-align: right; }
.value.status { color: #4f46e5; font-weight: 600; }
.title { color: #1e293b; font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; }
.msg-item { background: #f8fafc; border-radius: 10rpx; padding: 14rpx; margin-bottom: 10rpx; }
.meta { color: #64748b; font-size: 22rpx; margin-bottom: 6rpx; }
.content { color: #1e293b; font-size: 24rpx; white-space: pre-wrap; }
.empty { color: #94a3b8; font-size: 24rpx; text-align: center; margin: 20rpx 0; }
.textarea { width: 100%; min-height: 120rpx; background: #f8fafc; border-radius: 10rpx; padding: 12rpx; font-size: 24rpx; }
.btn { margin-top: 14rpx; background: #4f46e5; color: #fff; border-radius: 40rpx; }
.btn.cancel { background: #ef4444; }
</style>
