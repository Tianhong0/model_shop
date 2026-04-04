<template>
  <view class="cs-page">
    <!-- 头部 -->
    <view class="cs-header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="header-left" @click="goBack">
        <u-icon name="arrow-left" size="20" color="#333"></u-icon>
      </view>
      <view class="header-title">
        <text>{{ conversationStatusText }}</text>
      </view>
      <view class="header-right">
        <text v-if="conversation && conversation.status === 1" class="end-btn" @click="handleEndConversation">结束</text>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      class="message-list"
      scroll-y
      :scroll-into-view="scrollIntoView"
      @scrolltoupper="loadMoreMessages"
      :enhanced="true"
      :bounces="false"
      show-scrollbar="false"
    >
      <view v-if="loadingMore" class="loading-tip">加载中...</view>
      <view v-if="!hasMore && messages.length > 0" class="loading-tip">没有更多消息了</view>

      <view
        v-for="(msg, index) in messages"
        :key="msg.id || index"
        :id="'msg-' + (msg.id || index)"
        class="message-item"
        :class="{
          'message-self': isSelf(msg),
          'message-system': msg.messageType == 4,
          'message-ai': msg.senderRole === 'AI'
        }"
      >
        <!-- 系统消息 -->
        <view v-if="msg.messageType == 4" class="system-msg">
          <text>{{ msg.content }}</text>
        </view>

        <!-- 用户/客服消息 -->
        <template v-else>
          <view class="msg-avatar">
            <image v-if="msg.senderAvatar" :src="msg.senderAvatar" mode="aspectFill" class="avatar-img"></image>
            <text v-else>{{ getAvatarText(msg) }}</text>
          </view>
          <view class="msg-content">
            <view class="msg-meta">
              <text class="sender-name">{{ getSenderName(msg) }}</text>
              <text v-if="msg.senderRole === 'AI'" class="ai-tag">AI</text>
              <text class="msg-time">{{ formatTime(msg.createTime) }}</text>
            </view>

            <!-- 文本消息 -->
            <view v-if="msg.messageType == 1" class="msg-bubble">
              <text>{{ msg.content }}</text>
            </view>

            <!-- 图片消息 -->
            <view v-else-if="msg.messageType == 2" class="msg-bubble msg-media" @click="previewImage(msg.attachments)">
              <image :src="msg.attachments" mode="widthFix" class="msg-image"></image>
            </view>

            <!-- 视频消息 -->
            <view v-else-if="msg.messageType == 3" class="msg-bubble msg-media">
              <video
                :src="msg.attachments"
                class="msg-video"
                controls
                enable-danmu="false"
                show-center-play-btn="false"
                @touchmove.stop.prevent
                @touchstart.stop
              ></video>
            </view>
          </view>
        </template>
      </view>
    </scroll-view>

    <!-- 输入区域 -->
    <view v-if="canSend" class="input-area">
      <!-- AI 模式下显示转人工按钮 -->
      <view v-if="isAiMode" class="transfer-btn" @click="handleTransferToHuman">
        <text>人工</text>
      </view>
      <!-- 等待人工客服时显示等待提示 -->
      <view v-if="isWaitingForHuman" class="waiting-indicator">
        <text>等待接入...</text>
      </view>
      <view class="media-btn" @click="showMediaMenu = true">+</view>
      <input
        v-model="inputText"
        class="msg-input"
        type="text"
        placeholder="输入消息..."
        confirm-type="send"
        @confirm="sendTextMessage"
      />
      <view class="send-btn" @click="sendTextMessage">发送</view>
    </view>

    <view v-else class="input-area ended">
      <text>会话已结束</text>
      <view class="restart-btn" @click="startNewConversation">重新咨询</view>
    </view>

    <!-- 媒体选择菜单 -->
    <view v-if="showMediaMenu" class="media-mask" @click="showMediaMenu = false">
      <view class="media-menu" @click.stop>
        <view class="media-option" @click="chooseImage">
          <u-icon name="photo" size="24"></u-icon>
          <text>图片</text>
        </view>
        <view class="media-option" @click="chooseVideo">
          <u-icon name="video-camera" size="24"></u-icon>
          <text>视频</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onLoad, onUnload, onShow } from '@dcloudio/uni-app'
import { getApiBaseUrl } from '../../utils/apiBase'
import {
  getCsConversationStatusApi,
  startCsConversationApi,
  getCsMessagesApi,
  sendCsMessageApi,
  markCsMessageReadApi,
  endCsConversationApi,
  uploadCsMediaApi
} from '../../api/customer-service'

const statusBarHeight = ref(0)
const conversation = ref(null)
const messages = ref([])
const inputText = ref('')
const scrollIntoView = ref('')
const showMediaMenu = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const pageSize = 20
const socketConnected = ref(false)
const pollTimer = ref(null)
const socketTask = ref(null)
const intentionalClose = ref(false)
const reconnectTimer = ref(null)
const lastMessageId = ref(null)

const canSend = computed(() => {
  // 进行中或等待人工客服时都可以发送消息
  return conversation.value && (conversation.value.status === 1 || conversation.value.status === 0)
})

// 是否为 AI 模式（没有人工客服接入且状态为进行中）
const isAiMode = computed(() => {
  return conversation.value && conversation.value.status === 1 && !conversation.value.adminId
})

// 是否正在等待人工客服接入
const isWaitingForHuman = computed(() => {
  return conversation.value && conversation.value.status === 0
})

const conversationStatusText = computed(() => {
  if (!conversation.value) return '智能客服'
  if (conversation.value.status === 0) return '等待人工客服接入...'
  if (conversation.value.status === 1) {
    // 如果有 adminId 说明是人工客服
    if (conversation.value.adminId) return '在线客服'
    return '智能客服'
  }
  return '会话已结束'
})

onLoad(() => {
  const sys = uni.getSystemInfoSync()
  statusBarHeight.value = sys.statusBarHeight || 20

  checkConversation()
})

// 页面显示时刷新消息
onShow(() => {
  if (conversation.value && conversation.value.status === 1) {
    // 刷新消息列表
    loadMessages()
  }
})

onUnload(() => {
  closeSocket()
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }
})

const goBack = () => {
  uni.navigateBack()
}

const checkConversation = async () => {
  try {
    const res = await getCsConversationStatusApi()
    if (res) {
      conversation.value = res
      // 状态 0=等待人工客服 1=进行中(AI或人工) 2=已结束
      if (res.status === 1) {
        loadMessages()
        connectSocket()
      } else if (res.status === 0) {
        // 等待人工客服接入中
        loadMessages()
        connectSocket()
        pollForAssignment()
      } else if (res.status === 2) {
        // 已结束，创建新会话
        startNewConversation()
      }
    } else {
      // 没有会话，创建新会话
      startNewConversation()
    }
  } catch (e) {
    console.error('获取会话状态失败', e)
    startNewConversation()
  }
}

const startNewConversation = async () => {
  try {
    const res = await startCsConversationApi()
    conversation.value = res
    // 状态 0=等待人工客服 1=进行中(AI或人工) 2=已结束
    if (res.status === 1) {
      // AI 模式或人工客服已接入
      loadMessages()
      connectSocket()
    } else if (res.status === 0) {
      // 等待人工客服接入中
      loadMessages()
      connectSocket()
      pollForAssignment()
    } else if (res.status === 2) {
      // 已结束，可以重新发起
      const newRes = await startCsConversationApi()
      conversation.value = newRes
      if (newRes.status === 1) {
        loadMessages()
        connectSocket()
      } else if (newRes.status === 0) {
        loadMessages()
        connectSocket()
        pollForAssignment()
      }
    }
  } catch (e) {
    console.error('发起会话失败', e)
    uni.showToast({ title: '连接客服失败', icon: 'none' })
  }
}

const pollForAssignment = () => {
  // 如果已经有轮询中，不重复创建
  if (pollTimer.value) return

  pollTimer.value = setInterval(async () => {
    // 轮询检查会话状态
    try {
      const res = await getCsConversationStatusApi()
      if (res) {
        conversation.value = res
        // 人工客服已接入：状态为进行中且有 adminId
        if (res.status === 1 && res.adminId) {
          clearInterval(pollTimer.value)
          pollTimer.value = null
          loadMessages()
          connectSocket()
          uni.showToast({ title: '客服已接入', icon: 'success' })
        } else if (res.status === 2) {
          // 会话已结束
          clearInterval(pollTimer.value)
          pollTimer.value = null
        }
      }
    } catch (e) {
      console.error('轮询检查会话失败', e)
    }
  }, 3000)
}

const loadMessages = async (append = false) => {
  if (!conversation.value) return

  try {
    const res = await getCsMessagesApi(conversation.value.id, pageNum.value, pageSize)
    const records = res?.records || []
    if (append) {
      messages.value = [...records.reverse(), ...messages.value]
    } else {
      messages.value = records.reverse()
      // 更新最新消息ID
      if (records.length > 0) {
        lastMessageId.value = records[records.length - 1].id
      }
      nextTick(() => scrollToBottom())
    }
    hasMore.value = records.length >= pageSize
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

const loadMoreMessages = () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  pageNum.value++
  loadMessages(true).finally(() => {
    loadingMore.value = false
  })
}

const sendTextMessage = async () => {
  const text = inputText.value.trim()
  if (!text || !canSend.value) return

  inputText.value = ''

  try {
    await sendCsMessageApi({
      conversationId: conversation.value.id,
      messageType: 1,
      content: text
    })
    // 不再手动添加到列表，完全依赖 WebSocket 推送
  } catch (e) {
    console.error('发送消息失败', e)
    uni.showToast({ title: '发送失败', icon: 'none' })
  }
}

const chooseImage = async () => {
  showMediaMenu.value = false
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      if (!filePath) return

      uni.showLoading({ title: '上传中...' })
      try {
        const url = await uploadCsMediaApi(filePath, 'image')
        await sendCsMessageApi({
          conversationId: conversation.value.id,
          messageType: 2,
          content: '[图片]',
          attachments: url
        })
        uni.hideLoading()
        // 不再手动添加到列表，完全依赖 WebSocket 推送
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    }
  })
}

const chooseVideo = async () => {
  showMediaMenu.value = false
  uni.chooseVideo({
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePath
      if (!filePath) return

      uni.showLoading({ title: '上传中...' })
      try {
        const url = await uploadCsMediaApi(filePath, 'video')
        await sendCsMessageApi({
          conversationId: conversation.value.id,
          messageType: 3,
          content: '[视频]',
          attachments: url
        })
        uni.hideLoading()
        // 不再手动添加到列表，完全依赖 WebSocket 推送
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    }
  })
}

const handleEndConversation = async () => {
  uni.showModal({
    title: '确认结束',
    content: '确定要结束当前会话吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await endCsConversationApi(conversation.value.id)
          conversation.value.status = 2
          closeSocket()
          stopPolling()
        } catch (e) {
          uni.showToast({ title: '结束失败', icon: 'none' })
        }
      }
    }
  })
}

/**
 * 停止轮询
 */
const stopPolling = () => {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

/**
 * 转人工客服
 */
const handleTransferToHuman = () => {
  uni.showModal({
    title: '转人工客服',
    content: '确定要转接人工客服吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          // 发送转人工消息
          await sendCsMessageApi({
            conversationId: conversation.value.id,
            messageType: 1,
            content: '人工客服'
          })
          // 不再手动添加到列表，完全依赖 WebSocket 推送
          // 开始轮询等待人工客服接入
          pollForAssignment()
        } catch (e) {
          uni.showToast({ title: '转人工失败', icon: 'none' })
        }
      }
    }
  })
}

// WebSocket
const buildWsUrl = () => {
  const token = uni.getStorageSync('token')
  const baseUrl = getApiBaseUrl().replace(/\/+$/, '')
  const wsBase = baseUrl.startsWith('https://')
    ? baseUrl.replace('https://', 'wss://')
    : baseUrl.replace('http://', 'ws://')
  const convId = conversation.value?.id || ''
  return `${wsBase}/ws/cs/message?userRole=USER&conversationId=${convId}&token=${encodeURIComponent(token)}`
}

const connectSocket = () => {
  closeSocket()
  intentionalClose.value = false

  const task = uni.connectSocket({
    url: buildWsUrl(),
    complete: () => {}
  })
  socketTask.value = task

  task.onOpen(() => {
    socketConnected.value = true
    if (conversation.value) {
      markCsMessageReadApi(conversation.value.id).catch(() => {})
    }
  })

  task.onMessage((res) => {
    try {
      const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data

      if (data.eventType === 'NEW_MESSAGE') {
        const msg = data.message
        const msgIdStr = String(msg.id)

        // 去重：检查消息ID是否已存在
        const existsById = messages.value.some(m => String(m.id) === msgIdStr)

        if (!existsById) {
          messages.value.push(msg)
          nextTick(scrollToBottom)
          markCsMessageReadApi(conversation.value.id).catch(() => {})
        }
      } else if (data.eventType === 'CONVERSATION_UPDATE') {
        const updatedConv = data.conversation
        conversation.value = { ...conversation.value, ...updatedConv }

        // 如果状态变为等待人工客服，开始轮询
        if (updatedConv.status === 0 && !updatedConv.adminId) {
          pollForAssignment()
        }
        // 如果人工客服已接入，停止轮询
        if (updatedConv.status === 1 && updatedConv.adminId) {
          stopPolling()
          uni.showToast({ title: '客服已接入', icon: 'success' })
        }
      }
    } catch (e) {
      console.error('[WS] 处理消息失败:', e)
    }
  })

  task.onClose(() => {
    socketConnected.value = false
    // 非主动关闭时才重连
    if (!intentionalClose.value && canSend.value) {
      reconnectTimer.value = setTimeout(connectSocket, 3000)
    }
  })

  task.onError(() => {
    socketConnected.value = false
  })
}

const closeSocket = () => {
  intentionalClose.value = true
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }
  if (socketTask.value) {
    try {
      socketTask.value.close()
    } catch (e) {}
    socketTask.value = null
  }
  socketConnected.value = false
}

// 工具函数
const isSelf = (msg) => msg.senderRole === 'USER'

const getAvatarText = (msg) => {
  if (msg.senderRole === 'AI') return 'AI'
  const name = msg.senderNickname || (msg.senderRole === 'USER' ? '我' : '客服')
  return name.slice(0, 1)
}

const getSenderName = (msg) => {
  if (msg.senderRole === 'AI') return msg.senderNickname || '智能助手'
  return msg.senderNickname || (msg.senderRole === 'USER' ? '我' : '客服')
}

const formatTime = (time) => {
  if (!time) return ''
  return String(time).substring(5, 16).replace('T', ' ')
}

const scrollToBottom = () => {
  if (messages.value.length > 0) {
    const last = messages.value[messages.value.length - 1]
    nextTick(() => {
      scrollIntoView.value = 'msg-' + (last.id || messages.value.length - 1)
    })
  }
}

const previewImage = (url) => {
  uni.previewImage({ urls: [url] })
}
</script>

<style scoped lang="scss">
.cs-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.cs-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 20rpx 32rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;

  .header-left, .header-right {
    width: 100rpx;
  }

  .header-right {
    text-align: right;
  }

  .header-title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333;
  }

  .end-btn {
    color: #f59e0b;
    font-size: 28rpx;
  }
}

.message-list {
  flex: 1;
  padding: 120rpx 24rpx 160rpx;
  box-sizing: border-box;
}

.loading-tip {
  text-align: center;
  color: #999;
  font-size: 24rpx;
  padding: 20rpx;
}

.message-item {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;

  &.message-self {
    flex-direction: row-reverse;
    .msg-meta { text-align: right; }
    .msg-bubble {
      background: #007aff;
      color: #fff;
    }
  }

  &.message-system {
    justify-content: center;
    .system-msg {
      background: rgba(0, 0, 0, 0.05);
      padding: 12rpx 24rpx;
      border-radius: 20rpx;
      text { font-size: 24rpx; color: #999; }
    }
  }
}

.msg-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #007aff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.message-self .msg-avatar {
  background: #07c160;
}

/* AI 消息样式 */
.message-ai .msg-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.message-ai .msg-bubble {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #065f46;
}

.msg-content {
  max-width: 70%;
}

.msg-meta {
  font-size: 22rpx;
  color: #999;
  margin-bottom: 8rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;

  .msg-time {
    margin-left: 4rpx;
  }
}

.ai-tag {
  background: #10b981;
  color: #fff;
  font-size: 18rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
}

.msg-bubble {
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

  text { font-size: 28rpx; line-height: 1.5; }

  &.msg-media {
    padding: 8rpx;
  }
}

.msg-image {
  max-width: 400rpx;
  border-radius: 8rpx;
}

.msg-video {
  max-width: 400rpx;
  border-radius: 8rpx;
}

.input-area {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
  background: #fff;
  border-top: 1rpx solid #eee;

  &.ended {
    justify-content: center;
    color: #999;
    .restart-btn {
      margin-left: 20rpx;
      color: #007aff;
    }
  }
}

.media-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.transfer-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.waiting-indicator {
  padding: 0 20rpx;
  height: 72rpx;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #f59e0b;
    font-size: 24rpx;
  }
}

.msg-input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 120rpx;
  height: 72rpx;
  border-radius: 36rpx;
  background: #007aff;
  color: #fff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;
}

.media-menu {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx;
}

.media-option {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 30rpx;
  font-size: 32rpx;
  border-bottom: 1rpx solid #eee;
}
</style>
