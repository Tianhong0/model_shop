<template>
  <view class="page" v-if="detail">
    <!-- Hero Product Card -->
    <view class="hero-card fadeInUp">
      <image :src="detail.coverUrl || (detail.imageUrls || [])[0]" class="hero-cover" mode="aspectFill" />
      <view class="hero-main">
        <view class="hero-topline">
          <text class="hero-tag">闲置沟通</text>
          <text class="hero-status">{{ detail.owner ? '我是卖家' : '和卖家沟通中' }}</text>
        </view>
        <view class="hero-title">{{ detail.title }}</view>
        <view class="hero-meta">{{ detail.conditionLevel || '成色未知' }} · {{ detail.location || '未知地区' }}</view>
        <view class="hero-bottom">
          <view>
            <text class="hero-price">￥{{ Number(detail.price || 0).toFixed(2) }}</text>
            <text v-if="detail.originalPrice" class="hero-origin">原价￥{{ Number(detail.originalPrice || 0).toFixed(2) }}</text>
          </view>
          <button v-if="!detail.owner" class="hero-order-btn" @click="createDirectOrder">直接下单</button>
        </view>
      </view>
    </view>

    <!-- Offer Panel -->
    <view class="offer-panel fadeInUp">
      <view class="panel-head offer-panel-head" @click="toggleOfferCollapse">
        <view>
          <text class="panel-title">议价区</text>
          <text class="panel-sub">像闲鱼一样，把报价、还价、聊天都放在一个地方</text>
        </view>
        <view class="offer-head-right">
          <view class="count-badge">{{ offers.length }} 条</view>
          <text class="offer-collapse-icon" :class="{ expanded: !offerCollapsed }">⌄</text>
        </view>
      </view>

      <view v-if="!offerCollapsed && !detail.owner" class="offer-composer" @click.stop>
        <view class="quick-offers">
          <view class="quick-card" @click="fillOfferPrice(detail.price)">
            <text class="quick-label">按标价聊</text>
            <text class="quick-value">￥{{ Number(detail.price || 0).toFixed(2) }}</text>
          </view>
          <view class="quick-card quick-card-warm" @click="fillOfferPrice(suggestedOfferPrice)">
            <text class="quick-label">参考出价</text>
            <text class="quick-value">￥{{ Number(suggestedOfferPrice || 0).toFixed(2) }}</text>
          </view>
        </view>
        <view class="offer-input-row">
          <text class="offer-currency">￥</text>
          <input v-model="offerForm.offerAmount" class="offer-input" type="digit" placeholder="输入心理价位" />
        </view>
        <textarea v-model="offerForm.remark" class="offer-remark-input" placeholder="补一句，比如：可立刻付款、希望包邮、今晚可确认" />
        <button class="offer-submit" @click="submitOffer">发起议价</button>
      </view>

      <view v-if="!offerCollapsed && offers.length" class="offer-list">
        <view v-for="offer in offers" :key="offer.id" class="offer-card" :class="offerCardClass(offer)" @click="focusOfferConversation(offer)">
          <view class="offer-card-top">
            <view>
              <text class="offer-user">{{ detail.owner ? (offer.buyerNickname || '买家') : '我的报价' }}</text>
              <text class="offer-time">{{ formatDateTime(offer.createTime) }}</text>
            </view>
            <text class="offer-price">￥{{ Number(offer.offerAmount || 0).toFixed(2) }}</text>
          </view>
          <view class="offer-card-remark">{{ offer.remark || '这次报价没有附加留言。' }}</view>
          <view class="offer-card-bottom">
            <text class="offer-status" :class="statusClass(offer.status)">{{ offerStatusText(offer.status) }}</text>
            <view v-if="detail.owner && Number(offer.status) === 0" class="offer-actions">
              <button class="mini-btn ghost" @click="respondOffer(offer.id, false)">拒绝</button>
              <button class="mini-btn primary" @click="respondOffer(offer.id, true)">接受</button>
            </view>
            <button v-else-if="!detail.owner && Number(offer.status) === 1" class="mini-btn primary" @click="createOrderWithOffer(offer.id)">按此价格下单</button>
          </view>
        </view>
      </view>
      <view v-else-if="!offerCollapsed" class="offer-empty">还没有议价记录，先发一句问候或直接出价吧。</view>
    </view>

    <!-- Session Panel (Seller Only) -->
    <view v-if="detail.owner" class="session-panel fadeInUp">
      <view class="panel-head">
        <view>
          <text class="panel-title">联系过的买家</text>
          <text class="panel-sub">卖家可在这里切换不同买家，继续回复私聊消息</text>
        </view>
        <view class="count-badge">{{ sessions.length }} 人</view>
      </view>
      <scroll-view class="session-scroll" scroll-x>
        <view class="session-list">
          <view
            v-for="session in sessions"
            :key="session.counterpartId"
            class="session-card"
            :class="{ active: String(counterpartId || '') === String(session.counterpartId || '') }"
            @click="focusSession(session.counterpartId)"
          >
            <image v-if="session.counterpartAvatar" :src="session.counterpartAvatar" class="session-avatar" mode="aspectFill" />
            <view v-else class="session-avatar placeholder">{{ (session.counterpartNickname || '买').slice(0, 1) }}</view>
            <view class="session-main">
              <text class="session-name">{{ session.counterpartNickname || '买家' }}</text>
              <text class="session-preview">{{ session.lastMessage || '发起了会话' }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Chat Panel -->
    <view class="chat-panel fadeInUp">
      <view class="panel-head chat-head">
        <view>
          <text class="panel-title">聊天记录</text>
          <text class="panel-sub">{{ currentChatHint }}</text>
        </view>
        <view class="online-pill">{{ chatStatusLabel }}</view>
      </view>

      <scroll-view class="msg-list" scroll-y>
        <view v-if="counterpartId && messages.length">
          <view
            v-for="item in messages"
            :key="item.id"
            class="msg-row"
            :class="{ self: String(item.senderId) === String(myId), system: Number(item.isSystem) === 1 || Number(item.messageType) === 3 }"
          >
            <view v-if="!(Number(item.isSystem) === 1 || Number(item.messageType) === 3)" class="msg-avatar">
              {{ (item.senderNickname || item.senderRole || '聊').slice(0, 1) }}
            </view>
            <view class="msg-body">
              <view v-if="!(Number(item.isSystem) === 1 || Number(item.messageType) === 3)" class="msg-meta">
                {{ item.senderNickname || item.senderRole }} · {{ formatDateTime(item.createTime) }}
              </view>
              <view class="msg-bubble" :class="messageBubbleClass(item)">
                <image
                  v-if="isImageMessage(item)"
                  :src="resolveAttachmentUrl(item)"
                  class="msg-image"
                  mode="widthFix"
                  @click="previewMessageImage(item)"
                />
                <video
                  v-else-if="isVideoMessage(item)"
                  :src="resolveAttachmentUrl(item)"
                  class="msg-video"
                  controls
                  object-fit="cover"
                  :show-center-play-btn="true"
                ></video>
                <text v-else>{{ item.content }}</text>
              </view>
            </view>
          </view>
        </view>
        <view v-else class="msg-empty">{{ counterpartId ? '先聊两句吧，成交通常从一句"还在吗"开始。' : '先在上方选中一条议价记录，再进入对应买家的沟通。' }}</view>
      </scroll-view>
    </view>

    <!-- Input Bar -->
    <view class="input-bar">
      <view class="media-actions">
        <button class="media-btn media-btn-plus" :disabled="!counterpartId || mediaUploading" @click="openMediaPicker">+</button>
      </view>
      <textarea v-model="messageContent" class="msg-textarea" maxlength="300" auto-height :placeholder="counterpartId ? '输入你想说的话，比如：还在吗？支持同城自提吗？' : '请先在上方选择一个沟通对象'" :disabled="!counterpartId" />
      <button class="send-btn" :disabled="!counterpartId || mediaUploading" @click="sendMessage">{{ mediaUploading ? '上传中' : '发送' }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getApiBaseUrl } from '../../utils/apiBase'
import { createUsedOfferApi, createUsedOrderApi, getUsedListingDetailApi, getUsedMessagePageApi, getUsedMessageSessionListApi, respondUsedOfferApi, sendUsedMessageApi, uploadUsedChatMediaApi } from '../../api/used'

const listingId = ref('')
const counterpartId = ref('')
const myId = ref('')
const detail = ref(null)
const messageContent = ref('')
const messages = ref([])
const sessions = ref([])
const socketTask = ref(null)
const socketManualClose = ref(false)
const socketConnected = ref(false)
const reconnectTimer = ref(null)
const refreshTimer = ref(null)
const mediaUploading = ref(false)
const offerCollapsed = ref(true)
const offerForm = reactive({ offerAmount: '', remark: '' })

const offers = computed(() => detail.value?.offers || [])
const currentChatHint = computed(() => {
  if (!counterpartId.value) {
    return detail.value?.owner ? '收到的议价会集中在这里，点任一报价即可进入和该买家的对话。' : '支持实时消息，系统会自动同步议价与订单进展'
  }
  if (detail.value?.owner) {
    const target = offers.value.find((item) => String(item.buyerId) === String(counterpartId.value))
    return `当前正在与${target?.buyerNickname || '买家'}沟通，支持边聊边处理报价。`
  }
  return '支持实时消息，系统会自动同步议价与订单进展'
})
const chatStatusLabel = computed(() => {
  if (!counterpartId.value) return '待选择'
  return socketConnected.value ? '在线中' : '连接中'
})
const suggestedOfferPrice = computed(() => {
  const price = Number(detail.value?.price || 0)
  if (!Number.isFinite(price) || price <= 0) return 0
  return Math.max(1, Number((price * 0.88).toFixed(2)))
})

const toggleOfferCollapse = () => {
  offerCollapsed.value = !offerCollapsed.value
}

const resolveAttachmentUrl = (message) => {
  const raw = String(message?.attachments || '').trim()
  if (!raw) return ''
  if ((raw.startsWith('[') && raw.endsWith(']')) || (raw.startsWith('{') && raw.endsWith('}'))) {
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        const first = parsed.find(Boolean)
        return typeof first === 'string' ? first : (first?.url || '')
      }
      if (parsed && typeof parsed === 'object') {
        return parsed.url || parsed.mediaUrl || ''
      }
    } catch (_) {
      // ignore
    }
  }
  return raw.split(',').map(item => item.trim()).filter(Boolean)[0] || ''
}

const isImageMessage = (message) => Number(message?.messageType) === 2 && !!resolveAttachmentUrl(message)
const isVideoMessage = (message) => Number(message?.messageType) === 4 && !!resolveAttachmentUrl(message)
const messageBubbleClass = (message) => ({ media: isImageMessage(message) || isVideoMessage(message) })

const previewMessageImage = (message) => {
  const url = resolveAttachmentUrl(message)
  if (!url) return
  uni.previewImage({ urls: [url], current: url })
}

const closeSocket = () => {
  socketManualClose.value = true
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }
  if (socketTask.value) {
    try { socketTask.value.close({ code: 1000, reason: 'manual-close' }) } catch (_) {}
  }
  socketTask.value = null
  socketConnected.value = false
}

const stopRefreshLoop = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

const buildWsUrl = () => {
  const token = uni.getStorageSync('token') || ''
  const base = String(getApiBaseUrl() || '').trim().replace(/\/+$/, '')
  const wsBase = base.startsWith('https://') ? base.replace('https://', 'wss://') : base.replace('http://', 'ws://')
  return `${wsBase}/ws/used/message?listingId=${encodeURIComponent(String(listingId.value))}&counterpartId=${encodeURIComponent(String(counterpartId.value))}&token=${encodeURIComponent(token)}`
}

const connectSocket = () => {
  if (!listingId.value || !counterpartId.value) return
  closeSocket()
  socketManualClose.value = false
  const task = uni.connectSocket({ url: buildWsUrl(), complete: () => {} })
  task.onOpen(() => { socketConnected.value = true })
  task.onMessage((res) => {
    try {
      const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
      if (payload?.eventType !== 'NEW_MESSAGE') return
      const incoming = payload.message
      if (!incoming?.id) return
      if (messages.value.some((item) => String(item.id) === String(incoming.id))) return
      messages.value.push(incoming)
      if (detail.value?.owner) {
        loadSessions().catch(() => {})
      }
    } catch (_) {}
  })
  task.onClose(() => {
    socketConnected.value = false
    if (!socketManualClose.value) {
      reconnectTimer.value = setTimeout(connectSocket, 2500)
    }
  })
  socketTask.value = task
}

const ensureActiveSession = () => {
  if (!detail.value?.owner) return
  if (counterpartId.value) {
    const exists = sessions.value.some((item) => String(item.counterpartId) === String(counterpartId.value))
    if (exists) return
  }
  const firstSession = sessions.value[0]
  counterpartId.value = firstSession?.counterpartId ? String(firstSession.counterpartId) : ''
}

const loadMessages = async () => {
  if (!counterpartId.value) {
    messages.value = []
    return
  }
  const res = await getUsedMessagePageApi(listingId.value, counterpartId.value)
  messages.value = res.records || []
}

const loadSessions = async () => {
  const result = await getUsedMessageSessionListApi(listingId.value)
  sessions.value = Array.isArray(result) ? result : []
  ensureActiveSession()
}

const loadDetail = async () => {
  detail.value = await getUsedListingDetailApi(listingId.value)
  if (!counterpartId.value && detail.value?.owner && Array.isArray(detail.value?.offers) && detail.value.offers.length) {
    counterpartId.value = String(detail.value.offers[0].buyerId || '')
  }
}

const reloadAll = async () => {
  await Promise.all([loadSessions(), loadDetail()])
  ensureActiveSession()
  await loadMessages()
}

const refreshLoop = async () => {
  if (!listingId.value) return
  try {
    if (detail.value?.owner) {
      await loadSessions()
      if (counterpartId.value && !socketConnected.value) {
        connectSocket()
      }
      if (!socketConnected.value && counterpartId.value) {
        await loadMessages()
      }
      return
    }
    if (!socketConnected.value && counterpartId.value) {
      await loadMessages()
    }
  } catch (_) {
    // ignore background refresh errors
  }
}

const startRefreshLoop = () => {
  stopRefreshLoop()
  refreshTimer.value = setInterval(refreshLoop, 3000)
}

const formatDateTime = (value) => {
  if (!value) return '刚刚'
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

const offerStatusText = (status) => ({ 0: '待卖家处理', 1: '卖家已接受', 2: '卖家已拒绝', 3: '已撤回', 4: '已过期' }[Number(status)] || '未知状态')
const statusClass = (status) => ({ pending: Number(status) === 0, success: Number(status) === 1, reject: Number(status) === 2 })
const offerCardClass = (offer) => ({ mine: !detail.value?.owner, pending: Number(offer.status) === 0, accepted: Number(offer.status) === 1, active: String(counterpartId.value || '') === String(offer.buyerId || '') })

const focusOfferConversation = async (offer) => {
  if (!detail.value?.owner || !offer?.buyerId) return
  const nextId = String(offer.buyerId)
  if (nextId === String(counterpartId.value || '')) return
  counterpartId.value = nextId
  await loadMessages()
  connectSocket()
}

const focusSession = async (nextId) => {
  if (!nextId) return
  const normalized = String(nextId)
  if (normalized === String(counterpartId.value || '')) return
  counterpartId.value = normalized
  await loadMessages()
  connectSocket()
}

const fillOfferPrice = (price) => {
  const num = Number(price || 0)
  if (!Number.isFinite(num) || num <= 0) return
  offerForm.offerAmount = num.toFixed(2)
}

const submitOffer = async () => {
  if (!offerForm.offerAmount) {
    uni.showToast({ title: '请输入议价金额', icon: 'none' })
    return
  }
  try {
    await createUsedOfferApi({ listingId: listingId.value, offerAmount: Number(offerForm.offerAmount), remark: offerForm.remark })
    uni.showToast({ title: '议价已发送', icon: 'success' })
    offerForm.offerAmount = ''
    offerForm.remark = ''
    await reloadAll()
  } catch (error) {
    uni.showToast({ title: error.message || '议价失败', icon: 'none' })
  }
}

const respondOffer = async (offerId, approved) => {
  try {
    await respondUsedOfferApi({ offerId, approved })
    uni.showToast({ title: approved ? '已接受议价' : '已拒绝议价', icon: 'success' })
    await reloadAll()
  } catch (error) {
    uni.showToast({ title: error.message || '处理失败', icon: 'none' })
  }
}

const createOrderWithOffer = async (offerId) => {
  try {
    await createUsedOrderApi({
      listingId: listingId.value,
      offerId,
      receiverName: '默认收货人',
      receiverPhone: '13800000000',
      receiverAddress: '请在订单详情中确认地址'
    })
    uni.showToast({ title: '订单已创建', icon: 'success' })
    uni.navigateTo({ url: '/pages/used/my-purchases' })
  } catch (error) {
    uni.showToast({ title: error.message || '下单失败', icon: 'none' })
  }
}

const createDirectOrder = async () => {
  try {
    await createUsedOrderApi({
      listingId: listingId.value,
      receiverName: '默认收货人',
      receiverPhone: '13800000000',
      receiverAddress: '请在订单详情中确认地址'
    })
    uni.showToast({ title: '订单已创建', icon: 'success' })
    uni.navigateTo({ url: '/pages/used/my-purchases' })
  } catch (error) {
    uni.showToast({ title: error.message || '下单失败', icon: 'none' })
  }
}

const sendMediaMessage = async (messageType, attachmentUrl) => {
  const mediaLabel = messageType === 4 ? '视频' : '图片'
  try {
    await sendUsedMessageApi({
      listingId: listingId.value,
      counterpartId: counterpartId.value,
      content: `[${mediaLabel}]`,
      attachments: attachmentUrl,
      messageType
    })
    if (!socketConnected.value) {
      await loadMessages()
    }
    await loadSessions()
  } catch (error) {
    uni.showToast({ title: error.message || `${mediaLabel}发送失败`, icon: 'none' })
  }
}

const openMediaPicker = () => {
  if (!counterpartId.value || mediaUploading.value) return
  uni.showActionSheet({
    itemList: ['发送图片', '发送视频'],
    success: async (res) => {
      if (res?.tapIndex === 0) {
        await chooseImage()
        return
      }
      if (res?.tapIndex === 1) {
        await chooseVideo()
      }
    },
    fail: () => {
      // ignore cancel
    }
  })
}

const chooseImage = async () => {
  if (!counterpartId.value || mediaUploading.value) return
  try {
    const chooseRes = await new Promise((resolve, reject) => {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: resolve,
        fail: reject
      })
    })
    const filePath = chooseRes?.tempFilePaths?.[0]
    if (!filePath) return
    mediaUploading.value = true
    uni.showLoading({ title: '上传图片中...' })
    const mediaUrl = await uploadUsedChatMediaApi(filePath, 'image')
    await sendMediaMessage(2, mediaUrl)
    uni.hideLoading()
  } catch (error) {
    uni.hideLoading()
    if (String(error?.errMsg || '').includes('cancel')) return
    uni.showToast({ title: error.message || '图片发送失败', icon: 'none' })
  } finally {
    mediaUploading.value = false
  }
}

const chooseVideo = async () => {
  if (!counterpartId.value || mediaUploading.value) return
  try {
    const chooseRes = await new Promise((resolve, reject) => {
      uni.chooseVideo({
        sourceType: ['album', 'camera'],
        compressed: true,
        maxDuration: 60,
        success: resolve,
        fail: reject
      })
    })
    const filePath = chooseRes?.tempFilePath
    if (!filePath) return
    mediaUploading.value = true
    uni.showLoading({ title: '上传视频中...' })
    const mediaUrl = await uploadUsedChatMediaApi(filePath, 'video')
    await sendMediaMessage(4, mediaUrl)
    uni.hideLoading()
  } catch (error) {
    uni.hideLoading()
    if (String(error?.errMsg || '').includes('cancel')) return
    uni.showToast({ title: error.message || '视频发送失败', icon: 'none' })
  } finally {
    mediaUploading.value = false
  }
}

const sendMessage = async () => {
  const content = String(messageContent.value || '').trim()
  if (!content) {
    uni.showToast({ title: '请输入消息', icon: 'none' })
    return
  }
  try {
    await sendUsedMessageApi({ listingId: listingId.value, counterpartId: counterpartId.value, content })
    messageContent.value = ''
    if (!socketConnected.value) {
      await loadMessages()
    }
    await loadSessions()
  } catch (error) {
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  }
}

onLoad(async (options) => {
  listingId.value = options?.listingId || ''
  counterpartId.value = options?.counterpartId || ''
  myId.value = String(uni.getStorageSync('user_profile')?.id || '')
  try {
    await reloadAll()
    if (counterpartId.value) {
      connectSocket()
    }
    startRefreshLoop()
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
})

onUnload(() => {
  stopRefreshLoop()
  closeSocket()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

.fadeInUp { animation: fadeInUp 0.4s ease both; }

.page {
  min-height: 100vh;
  padding: 28rpx 28rpx 200rpx;
  background: $bg;
}

.hero-card,
.offer-panel,
.session-panel,
.chat-panel {
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.hero-card {
  display: flex;
  gap: 20rpx;
  padding: 24rpx;
}

.hero-cover {
  width: 190rpx;
  height: 190rpx;
  border-radius: 20rpx;
  background: #e0f2fe;
  flex-shrink: 0;
  opacity: 0;
  animation: fadeInUp 0.4s ease both;
}

.hero-main { flex: 1; min-width: 0; }

.hero-topline, .hero-bottom,
.panel-head, .offer-card-top,
.offer-card-bottom, .offer-actions,
.popup-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.hero-tag, .hero-status,
.count-badge, .online-pill {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.hero-tag { background: rgba(0, 191, 255, 0.1); color: $deep; }
.hero-status { background: rgba(0, 191, 255, 0.06); color: $text2; }

.hero-title {
  margin-top: 14rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: $text1;
  line-height: 1.35;
}

.hero-meta, .panel-sub, .offer-time,
.msg-meta, .offer-empty, .msg-empty {
  color: $text2;
  font-size: 22rpx;
}

.hero-meta { margin-top: 10rpx; }
.hero-bottom { margin-top: 20rpx; align-items: flex-end; }

.hero-price {
  font-size: 36rpx;
  line-height: 1;
  color: $deep;
  font-weight: 700;
}

.hero-origin {
  margin-left: 12rpx;
  font-size: 22rpx;
  color: $text3;
  text-decoration: line-through;
}

.hero-order-btn, .offer-submit, .send-btn, .mini-btn {
  border-radius: 999rpx;
  &:active { transform: scale(0.96); }
}

.hero-order-btn {
  margin: 0;
  padding: 0 28rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 24rpx;
  background: $gradient;
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(0, 191, 255, 0.2);
}

.offer-panel, .session-panel, .chat-panel {
  margin-top: 20rpx;
  padding: 28rpx;
}

.session-scroll { margin-top: 18rpx; white-space: nowrap; }

.session-list { display: inline-flex; gap: 14rpx; }

.session-card {
  width: 280rpx;
  padding: 18rpx;
  border-radius: 24rpx;
  background: rgba(0, 191, 255, 0.04);
  display: flex;
  gap: 14rpx;
  align-items: center;
  box-sizing: border-box;
  box-shadow: $shadow;
}

.session-card.active {
  box-shadow: 0 8rpx 28rpx rgba(0, 191, 255, 0.12);
  background: rgba(0, 191, 255, 0.08);
}

.session-avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: #e0f2fe;
  flex-shrink: 0;
}

.session-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $deep;
  font-size: 28rpx;
  font-weight: 700;
}

.session-main { min-width: 0; flex: 1; }

.session-name {
  display: block;
  color: $text1;
  font-size: 26rpx;
  font-weight: 700;
}

.session-preview {
  display: block;
  margin-top: 8rpx;
  color: $text2;
  font-size: 22rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-title {
  display: block;
  font-size: 30rpx;
  color: $text1;
  font-weight: 700;
}

.panel-sub {
  display: block;
  margin-top: 8rpx;
  line-height: 1.5;
}

.count-badge, .online-pill {
  background: rgba(0, 191, 255, 0.08);
  color: $deep;
}

.offer-panel-head { cursor: pointer; }

.offer-head-right {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.offer-collapse-icon {
  color: $text2;
  font-size: 30rpx;
  line-height: 1;
  transform: rotate(-90deg);
  transition: transform 0.2s ease;
}

.offer-collapse-icon.expanded { transform: rotate(0deg); }

.offer-composer {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fafbfc;
}

.quick-offers {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.quick-card {
  padding: 20rpx;
  border-radius: 20rpx;
  background: $card;
  box-shadow: $shadow;
}

.quick-card-warm {
  background: rgba(0, 191, 255, 0.06);
}

.quick-label { display: block; color: $text2; font-size: 22rpx; }

.quick-value {
  display: block;
  margin-top: 10rpx;
  color: $deep;
  font-size: 34rpx;
  font-weight: 700;
}

.offer-input-row {
  display: flex;
  align-items: center;
  margin-top: 18rpx;
  padding: 0 24rpx;
  height: 92rpx;
  border-radius: 24rpx;
  background: $card;
  box-shadow: $shadow;
}

.offer-currency {
  color: $deep;
  font-size: 36rpx;
  font-weight: 700;
  margin-right: 14rpx;
}

.offer-input, .offer-remark-input, .msg-textarea {
  width: 100%;
  box-sizing: border-box;
}

.offer-input {
  flex: 1;
  height: 92rpx;
  font-size: 30rpx;
  color: $text1;
}

.offer-remark-input {
  min-height: 120rpx;
  margin-top: 16rpx;
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: $card;
  font-size: 28rpx;
  color: $text1;
  box-shadow: $shadow;
}

.offer-submit {
  margin-top: 18rpx;
  background: $gradient;
  color: #fff;
  font-size: 28rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 191, 255, 0.2);
}

.offer-list { margin-top: 18rpx; }

.offer-card {
  margin-top: 14rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fafbfc;
}

.offer-card.mine { background: $card; }

.offer-card.accepted {
  background: rgba(0, 191, 255, 0.04);
}

.offer-card.active {
  box-shadow: 0 8rpx 28rpx rgba(0, 191, 255, 0.12);
}

.offer-user {
  display: block;
  font-size: 28rpx;
  color: $text1;
  font-weight: 700;
}

.offer-time { display: block; margin-top: 6rpx; }

.offer-price {
  color: $deep;
  font-size: 36rpx;
  font-weight: 700;
}

.offer-card-remark {
  margin-top: 16rpx;
  color: $text2;
  font-size: 24rpx;
  line-height: 1.6;
}

.offer-card-bottom { margin-top: 18rpx; }

.offer-status {
  display: inline-flex;
  align-items: center;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 191, 255, 0.06);
  color: $text2;
  font-size: 22rpx;
}

.offer-status.pending { background: rgba(0, 191, 255, 0.1); color: $deep; }
.offer-status.success { background: rgba(16, 185, 129, 0.1); color: $success; }
.offer-status.reject { background: rgba(255, 77, 109, 0.08); color: $danger; }

.mini-btn {
  margin: 0;
  min-width: 142rpx;
  height: 66rpx;
  line-height: 66rpx;
  padding: 0 24rpx;
  font-size: 24rpx;
}

.mini-btn.ghost {
  background: $card;
  color: $deep;
  box-shadow: $shadow;
}

.mini-btn.primary {
  background: $deep;
  color: #fff;
}

.offer-empty, .msg-empty {
  padding: 40rpx 0 18rpx;
  text-align: center;
  line-height: 1.7;
}

.chat-head { margin-bottom: 18rpx; }

.msg-list { max-height: 780rpx; }

.msg-row {
  display: flex;
  gap: 14rpx;
  margin-bottom: 18rpx;
  align-items: flex-start;
}

.msg-row.self { flex-direction: row-reverse; }
.msg-row.system { justify-content: center; }

.msg-avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0f2fe 0%, #b8e4f0 100%);
  color: $deep;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-body { max-width: calc(100% - 90rpx); }

.msg-meta {
  margin-bottom: 10rpx;
  padding: 0 8rpx;
}

.msg-bubble {
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: #fafbfc;
  color: $text1;
  font-size: 28rpx;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-bubble.media {
  padding: 10rpx;
  overflow: hidden;
}

.msg-image {
  display: block;
  width: 320rpx;
  max-width: 100%;
  border-radius: 18rpx;
  background: #f3f4f6;
  opacity: 0;
  animation: fadeInUp 0.4s ease both;
}

.msg-video {
  display: block;
  width: 360rpx;
  max-width: 100%;
  height: 240rpx;
  border-radius: 18rpx;
  background: #111827;
}

.msg-row.self .msg-bubble {
  background: rgba(0, 191, 255, 0.08);
}

.msg-row.system .msg-body { max-width: 100%; }

.msg-row.system .msg-bubble {
  background: rgba(0, 191, 255, 0.06);
  color: $text2;
  font-size: 24rpx;
  padding: 14rpx 18rpx;
  border-radius: 999rpx;
  text-align: center;
}

.input-bar {
  position: fixed;
  left: 24rpx; right: 24rpx; bottom: 24rpx;
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  padding: 16rpx 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.06);
}

.media-actions {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.media-btn {
  margin: 0;
  width: 72rpx;
  min-width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0;
  border-radius: 20rpx;
  background: rgba(0, 191, 255, 0.08);
  color: $deep;
  font-size: 24rpx;
}

.media-btn-plus {
  font-size: 40rpx;
  font-weight: 500;
  line-height: 68rpx;
}

.media-btn[disabled] {
  background: #f0f0f0;
  color: $text3;
}

.msg-textarea {
  flex: 1;
  min-height: 84rpx;
  max-height: 220rpx;
  padding: 18rpx 20rpx;
  border-radius: 20rpx;
  background: #fafbfc;
  font-size: 28rpx;
  color: $text1;
}

.send-btn {
  flex-shrink: 0;
  margin: 0;
  width: 144rpx;
  height: 84rpx;
  line-height: 84rpx;
  background: $gradient;
  color: #fff;
  font-size: 28rpx;
  box-shadow: 0 8rpx 20rpx rgba(0, 191, 255, 0.2);
}

.send-btn[disabled] {
  background: #e0f2fe;
  color: #fff;
}
</style>
