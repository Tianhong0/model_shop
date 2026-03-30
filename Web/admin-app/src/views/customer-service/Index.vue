<template>
  <div class="cs-admin-container">
    <!-- 状态栏 -->
    <div class="status-bar">
      <el-card>
        <div class="status-header">
          <h3>客服状态</h3>
          <el-switch v-model="isOnline" @change="handleToggleOnline" />
        </div>
        <div class="status-info">
          <span>当前会话: {{ currentConversationCount }}</span>
          <span>累计服务: {{ totalServedCount }}</span>
        </div>
      </el-card>
    </div>

    <div class="main-content">
      <!-- 会话列表 -->
      <div class="conversation-panel">
        <div class="panel-header">
          <h3>会话列表</h3>
          <el-radio-group v-model="statusFilter" size="small" @change="loadConversations">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">等待中</el-radio-button>
            <el-radio-button :label="1">进行中</el-radio-button>
            <el-radio-button :label="2">已结束</el-radio-button>
          </el-radio-group>
        </div>

        <el-table :data="conversationList" @row-click="openConversation" highlight-current-row>
          <el-table-column prop="sessionNo" label="会话编号" width="180" />
          <el-table-column label="用户" width="120">
            <template #default="{ row }">
              <span>{{ row.userNickname || '用户' + row.userId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="未读" width="60">
            <template #default="{ row }">
              <el-badge :value="row.unreadAdminCount" :hidden="row.unreadAdminCount === 0" :max="99" />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" type="primary" size="small" @click.stop="acceptConversation(row.id)">
                接单
              </el-button>
              <el-button v-if="row.status === 1" type="danger" size="small" @click.stop="endConversation(row.id)">
                结束
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadConversations"
        />
      </div>

      <!-- 聊天窗口 -->
      <div class="chat-panel" v-if="currentConversation">
        <div class="chat-header">
          <div class="chat-title">
            <span>与 {{ currentConversation.userNickname || '用户' }} 的会话</span>
            <el-tag :type="getStatusType(currentConversation.status)" size="small">
              {{ getStatusText(currentConversation.status) }}
            </el-tag>
          </div>
          <el-button v-if="currentConversation.status === 1" type="danger" @click="endConversation(currentConversation.id)">
            结束会话
          </el-button>
        </div>

        <div class="message-list" ref="messageListRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-item"
            :class="{ 'message-self': msg.senderRole === 'ADMIN', 'message-system': msg.messageType == 4 }"
          >
            <template v-if="msg.messageType == 4">
              <div class="system-msg">{{ msg.content }}</div>
            </template>
            <template v-else>
              <div class="message-avatar">
                <img v-if="msg.senderAvatar" :src="msg.senderAvatar" />
                <template v-else>{{ getAvatarText(msg) }}</template>
              </div>
              <div class="message-content">
                <div class="message-meta">
                  <span class="sender-name">{{ getSenderName(msg) }}</span>
                  <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                </div>
                <div v-if="msg.messageType == 1" class="message-bubble">
                  {{ msg.content }}
                </div>
                <div v-else-if="msg.messageType == 2" class="message-bubble">
                  <img :src="msg.attachments" @click="previewImage(msg.attachments)" />
                </div>
                <div v-else-if="msg.messageType == 3" class="message-bubble">
                  <video :src="msg.attachments" controls />
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="input-area" v-if="currentConversation.status === 1">
          <el-button @click="showMediaMenu = true">+</el-button>
          <el-input
            v-model="messageContent"
            placeholder="输入消息..."
            @keyup.enter="sendMessage"
          />
          <el-button type="primary" @click="sendMessage">发送</el-button>
        </div>
      </div>

      <div v-else class="chat-panel empty">
        <el-empty description="选择一个会话开始聊天" />
      </div>
    </div>

    <!-- 媒体选择弹窗 -->
    <el-dialog v-model="showMediaMenu" title="选择媒体" width="400px">
      <div class="media-options">
        <el-button @click="uploadImage">上传图片</el-button>
        <el-button @click="uploadVideo">上传视频</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import JSONBigFactory from 'json-bigint'

const JSONBig = JSONBigFactory({ storeAsString: true })
import {
  getCsConversationsApi,
  acceptCsConversationApi,
  endCsConversationApi,
  getCsMessagesApi,
  sendCsMessageApi,
  setCsAdminStatusApi,
  markCsAdminReadApi,
  uploadCsMediaApi
} from '@/api/customer-service'

const isOnline = ref(true)
const statusFilter = ref(null)
const conversationList = ref([])
const currentConversation = ref(null)
const messages = ref([])
const messageContent = ref('')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const messageListRef = ref(null)
const showMediaMenu = ref(false)
const currentConversationCount = ref(0)
const totalServedCount = ref(0)

// ==================== WebSocket ====================
let ws = null
let reconnectTimer = null
let heartbeatTimer = null
const wsConnected = ref(false)

const buildWsUrl = () => {
  const token = localStorage.getItem('token')
  // 从 REST API 基地址推导 WebSocket 地址，保证走同一个后端实例
  const restBase = 'http://127.0.0.1:9999'.replace(/\/+$/, '')
  const wsBase = restBase.startsWith('https://')
    ? restBase.replace('https://', 'wss://')
    : restBase.replace('http://', 'ws://')
  return `${wsBase}/ws/cs/message?userRole=ADMIN&token=${encodeURIComponent(token)}`
}

const connectWebSocket = () => {
  closeWebSocket()

  ws = new WebSocket(buildWsUrl())

  ws.onopen = () => {
    wsConnected.value = true
    console.log('客服WebSocket已连接')
    // 启动心跳（30秒）
    heartbeatTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send('ping')
      }
    }, 30000)
  }

  ws.onmessage = (event) => {
    if (event.data === 'pong') return

    try {
      const data = JSONBig.parse(event.data)
      handleWsEvent(data)
    } catch (e) {
      console.warn('解析WebSocket消息失败', e)
    }
  }

  ws.onclose = () => {
    wsConnected.value = false
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
    // 自动重连（3秒后）
    if (isOnline.value) {
      reconnectTimer = setTimeout(connectWebSocket, 3000)
    }
  }

  ws.onerror = () => {
    wsConnected.value = false
  }
}

const closeWebSocket = () => {
  clearTimeout(reconnectTimer)
  reconnectTimer = null
  clearInterval(heartbeatTimer)
  heartbeatTimer = null
  if (ws) {
    ws.onclose = null // 防止触发重连
    ws.close()
    ws = null
  }
  wsConnected.value = false
}

// 处理WebSocket推送事件
const handleWsEvent = (data) => {
  if (data.eventType === 'NEW_MESSAGE') {
    const msg = data.message
    const convId = data.conversationId

    // 如果是当前打开的会话，追加消息
    if (currentConversation.value && String(currentConversation.value.id) === String(convId)) {
      if (!messages.value.some(m => String(m.id) === String(msg.id))) {
        messages.value.push(msg)
        nextTick(scrollToBottom)
        // 自动标记已读
        markCsAdminReadApi(convId).catch(() => {})
      }
    } else {
      // 不是当前会话，更新会话列表中的未读数
      const conv = conversationList.value.find(c => String(c.id) === String(convId))
      if (conv) {
        conv.unreadAdminCount = (conv.unreadAdminCount || 0) + 1
        conv.lastMessageTime = msg.createTime
      }
    }
  } else if (data.eventType === 'CONVERSATION_UPDATE') {
    const updatedConv = data.conversation
    const idx = conversationList.value.findIndex(c => String(c.id) === String(updatedConv.id))
    if (idx >= 0) {
      // 更新已有会话
      conversationList.value[idx] = { ...conversationList.value[idx], ...updatedConv }
    } else {
      // 新会话，插入列表头部
      conversationList.value.unshift(updatedConv)
      total.value++
    }
    // 如果是当前打开的会话，同步状态
    if (currentConversation.value && String(currentConversation.value.id) === String(updatedConv.id)) {
      currentConversation.value = { ...currentConversation.value, ...updatedConv }
    }
    // 更新统计
    updateStats()
  }
}

// 从会话列表统计当前会话数
const updateStats = () => {
  currentConversationCount.value = conversationList.value.filter(c => c.status === 1).length
}

// ==================== 数据加载 ====================

// 加载会话列表（仅初始化和手动刷新时使用）
const loadConversations = async () => {
  try {
    const res = await getCsConversationsApi({
      pageNum: pageNum.value,
      pageSize: pageSize,
      status: statusFilter.value
    })
    conversationList.value = res.records || []
    total.value = res.total || 0
    updateStats()
  } catch (e) {
    // ignore
  }
}

// 接单
const acceptConversation = async (id) => {
  try {
    await acceptCsConversationApi(id)
    ElMessage.success('接单成功')
    loadConversations()
  } catch (e) {
    ElMessage.error('接单失败')
  }
}

// 结束会话
const endConversation = async (id) => {
  try {
    await endCsConversationApi(id)
    ElMessage.success('会话已结束')
    if (currentConversation.value?.id === id) {
      currentConversation.value = null
      messages.value = []
    }
    loadConversations()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 打开会话
const openConversation = async (row) => {
  currentConversation.value = row

  // 标记已读
  await markCsAdminReadApi(row.id)
  row.unreadAdminCount = 0

  // 加载历史消息
  try {
    const res = await getCsMessagesApi(row.id, { pageNum: 1, pageSize: 100 })
    messages.value = (res.records || []).reverse()
    nextTick(scrollToBottom)
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!messageContent.value.trim() || !currentConversation.value) return

  try {
    const res = await sendCsMessageApi({
      conversationId: currentConversation.value.id,
      messageType: 1,
      content: messageContent.value
    })
    messages.value.push(res)
    messageContent.value = ''
    nextTick(scrollToBottom)
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

// 客服状态切换
const handleToggleOnline = async (val) => {
  try {
    await setCsAdminStatusApi(val)
    ElMessage.success(val ? '已上线' : '已离线')
    if (val) {
      connectWebSocket()
    } else {
      closeWebSocket()
    }
  } catch (e) {
    ElMessage.error('状态设置失败')
  }
}

// 上传图片
const uploadImage = () => {
  showMediaMenu.value = false
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      ElMessage.info('上传中...')
      const url = await uploadCsMediaApi(file)

      // 发送图片消息
      const res = await sendCsMessageApi({
        conversationId: currentConversation.value.id,
        messageType: 2,
        content: '[图片]',
        attachments: url
      })
      messages.value.push(res)
      nextTick(scrollToBottom)
      ElMessage.success('发送成功')
    } catch (e) {
      ElMessage.error('上传失败')
    }
  }
  input.click()
}

// 上传视频
const uploadVideo = () => {
  showMediaMenu.value = false
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'video/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      ElMessage.info('上传中...')
      const url = await uploadCsMediaApi(file)

      // 发送视频消息
      const res = await sendCsMessageApi({
        conversationId: currentConversation.value.id,
        messageType: 3,
        content: '[视频]',
        attachments: url
      })
      messages.value.push(res)
      nextTick(scrollToBottom)
      ElMessage.success('发送成功')
    } catch (e) {
      ElMessage.error('上传失败')
    }
  }
  input.click()
}

// ==================== 工具函数 ====================

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '等待中', 1: '进行中', 2: '已结束' }
  return map[status] || '未知'
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

const getAvatarText = (msg) => {
  const name = msg.senderNickname || (msg.senderRole === 'USER' ? '用户' : '我')
  return name.slice(0, 1)
}

const getSenderName = (msg) => {
  return msg.senderNickname || (msg.senderRole === 'USER' ? '用户' : '我')
}

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const previewImage = (url) => {
  // 可以使用图片预览组件
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadConversations()
  connectWebSocket()
})

onUnmounted(() => {
  closeWebSocket()
})
</script>

<style scoped>
.cs-admin-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.status-bar {
  margin-bottom: 20px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.status-info {
  display: flex;
  gap: 24px;
  color: var(--text-secondary);
  font-size: 14px;
}

.main-content {
  display: flex;
  flex: 1;
  gap: 20px;
  min-height: 0;
}

.conversation-panel {
  width: 500px;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  min-width: 0;
}

.chat-panel.empty {
  justify-content: center;
  align-items: center;
  color: var(--text-muted);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg-secondary);
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-item.message-self {
  flex-direction: row-reverse;
}

.message-item.message-self .message-bubble {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  color: #fff;
}

.system-msg {
  margin: 12px auto;
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--text-muted);
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  font-weight: 600;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-item.message-self .message-avatar {
  background: linear-gradient(135deg, var(--success-color) 0%, #34d399 100%);
}

.message-content {
  max-width: 60%;
}

.message-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.message-meta .msg-time {
  margin-left: 8px;
}

.message-bubble {
  padding: 12px 16px;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  line-height: 1.5;
}

.message-bubble img,
.message-bubble video {
  max-width: 300px;
  border-radius: var(--radius-md);
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-primary);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.input-area .el-input {
  flex: 1;
}

.media-options {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
