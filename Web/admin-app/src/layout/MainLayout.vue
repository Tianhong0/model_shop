<template>
  <div class="app-wrapper">
    <!-- Sidebar -->
    <aside
      class="sidebar-container"
      :class="{ 'sidebar-light': !configStore.sidebarDark }"
      :style="{ width: isCollapse ? '64px' : '260px' }"
    >
      <div class="sidebar-logo">
        <div class="logo-icon">
          <img v-if="configStore.siteIconUrl" :src="configStore.siteIconUrl" class="logo-image" alt="logo" />
          <el-icon v-else :size="24"><component :is="configStore.siteIcon || 'Cpu'" /></el-icon>
        </div>
        <span v-show="!isCollapse" class="logo-text">{{ configStore.siteName }}</span>
      </div>
      
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          :collapse="isCollapse"
          router
        >
          <!-- 动态菜单（如果有权限数据） -->
          <template v-if="authStore.menus && authStore.menus.length > 0">
            <template v-for="menu in authStore.menus" :key="menu.id || menu.permissionCode">
              <!-- 有子菜单 -->
              <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.permissionCode || String(menu.id)">
                <template #title>
                  <el-icon><component :is="getMenuIcon(menu)" /></el-icon>
                  <span>{{ menu.permissionName }}</span>
                </template>
                <el-menu-item
                  v-for="child in menu.children"
                  :key="child.id || child.permissionCode"
                  :index="child.menuPath"
                >
                  <el-icon><component :is="getMenuIcon(child)" /></el-icon>
                  <span>{{ child.permissionName }}</span>
                </el-menu-item>
              </el-sub-menu>
              <!-- 无子菜单 -->
              <el-menu-item v-else :index="menu.menuPath">
                <el-icon><component :is="getMenuIcon(menu)" /></el-icon>
                <template #title>{{ menu.permissionName }}</template>
              </el-menu-item>
            </template>
          </template>

          <!-- 静态菜单（后备，无权限数据时显示） -->
          <template v-else>
            <el-menu-item index="/dashboard">
              <el-icon><monitor /></el-icon>
              <template #title>仪表盘</template>
            </el-menu-item>

            <el-sub-menu index="user-mgmt">
              <template #title>
                <el-icon><user-filled /></el-icon>
                <span>用户中心</span>
              </template>
              <el-menu-item index="/users/list">
                <el-icon><user /></el-icon>
                <span>用户管理</span>
              </el-menu-item>
              <el-menu-item index="/system/roles">
                <el-icon><avatar /></el-icon>
                <span>角色管理</span>
              </el-menu-item>
              <el-menu-item index="/users/deletion-requests">
                <el-icon><document-delete /></el-icon>
                <span>注销申请管理</span>
              </el-menu-item>
              <el-menu-item index="/users/admin-register-requests">
                <el-icon><user-filled /></el-icon>
                <span>管理员注册审核</span>
              </el-menu-item>
              <el-menu-item index="/users/designer-apply-requests">
                <el-icon><user-filled /></el-icon>
                <span>设计者申请审核</span>
              </el-menu-item>
              <el-menu-item index="/users/admins">
                <el-icon><avatar /></el-icon>
                <span>管理员管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="model-mgmt">
              <template #title>
                <el-icon><goods /></el-icon>
                <span>模型管理</span>
              </template>
              <el-menu-item index="/models/category">
                <el-icon><Menu /></el-icon>
                <span>模型分类</span>
              </el-menu-item>
              <el-menu-item index="/models/list">
                <el-icon><List /></el-icon>
                <span>模型管理</span>
              </el-menu-item>
              <el-menu-item index="/models/model-lists">
                <el-icon><Collection /></el-icon>
                <span>清单管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="order-mgmt">
              <template #title>
                <el-icon><ticket /></el-icon>
                <span>订单系统</span>
              </template>
              <el-menu-item index="/orders/list">
                <el-icon><document /></el-icon>
                <span>订单管理</span>
              </el-menu-item>
              <el-menu-item index="/orders/after-sales">
                <el-icon><service /></el-icon>
                <span>售后管理</span>
              </el-menu-item>
              <el-menu-item index="/orders/logistics">
                <el-icon><van /></el-icon>
                <span>物流管理</span>
              </el-menu-item>
              <el-menu-item index="/orders/reviews">
                <el-icon><chat-line-square /></el-icon>
                <span>订单评价</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="used-mgmt">
              <template #title>
                <el-icon><tickets /></el-icon>
                <span>二手交易</span>
              </template>
              <el-menu-item index="/used/listings">
                <el-icon><goods /></el-icon>
                <span>商品管理</span>
              </el-menu-item>
              <el-menu-item index="/used/orders">
                <el-icon><document /></el-icon>
                <span>订单管理</span>
              </el-menu-item>
              <el-menu-item index="/used/reports">
                <el-icon><warning /></el-icon>
                <span>举报处理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="print-mgmt">
              <template #title>
                <el-icon><clock /></el-icon>
                <span>打印排产</span>
              </template>
              <el-menu-item index="/print-queue">
                <span>任务排产</span>
              </el-menu-item>
              <el-menu-item index="/print/printers">
                <span>打印机管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="bounty-mgmt">
              <template #title>
                <el-icon><collection /></el-icon>
                <span>悬赏管理</span>
              </template>
              <el-menu-item index="/bounty">
                <el-icon><document /></el-icon>
                <span>任务悬赏</span>
              </el-menu-item>
              <el-menu-item index="/bounty/appeal">
                <el-icon><warning /></el-icon>
                <span>评价申诉</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="interaction">
              <template #title>
                <el-icon><promotion /></el-icon>
                <span>社区模块</span>
              </template>
              <el-menu-item index="/community/posts">
                <el-icon><chat-dot-round /></el-icon>
                <span>帖子管理</span>
              </el-menu-item>
              <el-menu-item index="/community/replies">
                <el-icon><calendar /></el-icon>
                <span>回复管理</span>
              </el-menu-item>
              <el-menu-item index="/community/categories">
                <el-icon><Menu /></el-icon>
                <span>分类管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="operation-mgmt">
              <template #title>
                <el-icon><setting /></el-icon>
                <span>运营管理</span>
              </template>
              <el-menu-item index="/operation/banners">
                <el-icon><PictureFilled /></el-icon>
                <span>轮播管理</span>
              </el-menu-item>
              <el-menu-item index="/operation/notices">
                <el-icon><notification /></el-icon>
                <span>公告管理</span>
              </el-menu-item>
              <el-menu-item index="/coupon/templates">
                <el-icon><ticket /></el-icon>
                <span>优惠券管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="group-buy-mgmt">
              <template #title>
                <el-icon><goods /></el-icon>
                <span>拼团管理</span>
              </template>
              <el-menu-item index="/group-buy/activities">
                <el-icon><list /></el-icon>
                <span>拼团活动</span>
              </el-menu-item>
              <el-menu-item index="/group-buy/batch-discount">
                <el-icon><price-tag /></el-icon>
                <span>批量打印折扣</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="event-mgmt">
              <template #title>
                <el-icon><calendar /></el-icon>
                <span>活动赛事</span>
              </template>
              <el-menu-item index="/events">
                <el-icon><calendar /></el-icon>
                <span>活动管理</span>
              </el-menu-item>
              <el-menu-item index="/event-submissions">
                <el-icon><star-filled /></el-icon>
                <span>作品管理</span>
              </el-menu-item>
              <el-menu-item index="/event-participations">
                <el-icon><user /></el-icon>
                <span>报名管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="finance-mgmt">
              <template #title>
                <el-icon><money /></el-icon>
                <span>资金管理</span>
              </template>
              <el-menu-item index="/finance/withdraws">
                <el-icon><tickets /></el-icon>
                <span>提现管理</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="system-mgmt">
              <template #title>
                <el-icon><setting /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item index="/config">
                <el-icon><operation /></el-icon>
                <span>界面配置</span>
              </el-menu-item>
              <el-menu-item index="/system/config">
                <el-icon><tools /></el-icon>
                <span>系统配置</span>
              </el-menu-item>
              <el-menu-item index="/system/logs">
                <el-icon><document /></el-icon>
                <span>操作日志</span>
              </el-menu-item>
              <el-menu-item index="/system/promotion-config">
                <el-icon><Share /></el-icon>
                <span>推广配置</span>
              </el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="statistics-mgmt">
              <template #title>
                <el-icon><data-analysis /></el-icon>
                <span>统计报表</span>
              </template>
              <el-menu-item index="/statistics/orders">
                <el-icon><document /></el-icon>
                <span>订单统计</span>
              </el-menu-item>
              <el-menu-item index="/statistics/users">
                <el-icon><user /></el-icon>
                <span>用户统计</span>
              </el-menu-item>
              <el-menu-item index="/statistics/models">
                <el-icon><goods /></el-icon>
                <span>模型统计</span>
              </el-menu-item>
              <el-menu-item index="/statistics/finance">
                <el-icon><money /></el-icon>
                <span>财务统计</span>
              </el-menu-item>
              <el-menu-item index="/statistics/bounty">
                <el-icon><collection /></el-icon>
                <span>悬赏统计</span>
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/customer-service">
              <el-icon><service /></el-icon>
              <template #title>客服管理</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <!-- AI 智能工具按钮 - 放在菜单外部 -->
      <div class="ai-assistant-btn" :class="{ 'collapsed': isCollapse }" @click="openAIAssistant">
        <el-icon><magic-stick /></el-icon>
        <span v-show="!isCollapse" class="ai-text">AI智能工具</span>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="main-layout">
      <header class="header-bar">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon><component :is="isCollapse ? 'expand' : 'fold'" /></el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理系统</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-space :size="20">
            <div class="status-indicator">
              <el-tag :type="configStore.siteStatus ? 'success' : 'danger'" effect="dark" round>
                {{ configStore.siteStatus ? '正在营业' : '系统维护' }}
              </el-tag>
            </div>
            <el-badge :value="totalUnreadCount" :hidden="totalUnreadCount <= 0" class="item" :max="99">
              <el-icon :size="20" style="cursor:pointer" @click="handleShowMessages"><bell /></el-icon>
            </el-badge>
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-info-dropdown">
                <el-avatar :size="36" :src="displayAvatar">
                  {{ displayName.slice(0, 1) }}
                </el-avatar>
                <div class="user-meta">
                  <div class="user-name">{{ displayName }}</div>
                  <div class="user-role">{{ displayRole }}</div>
                </div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="security">安全设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出系统</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-space>
        </div>
      </header>

      <!-- 个人信息弹窗 -->
      <el-dialog v-model="profileVisible" title="个人信息" width="500px">
        <el-form :model="profileForm" label-width="100px">
          <el-form-item label="用户名">
            <el-input :model-value="profileForm.userName" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.mobile" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <el-space style="width: 100%">
              <el-input v-model="profileForm.emailCode" placeholder="修改邮箱时必填" maxlength="6" />
              <el-button :disabled="profileCodeCountdown > 0" :loading="profileCodeSending" @click="handleSendProfileEmailCode">
                {{ profileCodeCountdown > 0 ? `${profileCodeCountdown}s后重发` : '发送验证码' }}
              </el-button>
            </el-space>
          </el-form-item>
          <el-form-item label="头像">
            <el-space>
              <el-avatar :size="56" :src="profileForm.avatar">
                {{ displayName.slice(0, 1) }}
              </el-avatar>
              <el-button :loading="avatarUploading" @click="triggerAvatarUpload">上传头像</el-button>
            </el-space>
            <input
              ref="avatarFileInputRef"
              type="file"
              accept="image/*"
              style="display:none"
              @change="handleAvatarFileChange"
            />
          </el-form-item>
          <el-form-item label="角色">
            <el-input :model-value="displayRole" disabled />
          </el-form-item>
          <el-form-item label="注册时间">
            <el-input :model-value="formatDateTime(profileForm.createTime)" disabled />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="profileVisible = false">取消</el-button>
          <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">保存</el-button>
        </template>
      </el-dialog>

      <!-- 安全设置弹窗 -->
      <el-dialog v-model="securityVisible" title="安全设置" width="500px">
        <el-form :model="securityForm" label-width="100px">
          <el-form-item label="旧密码">
            <el-input v-model="securityForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <el-space style="width: 100%">
              <el-input v-model="securityForm.emailCode" placeholder="请输入6位邮箱验证码" maxlength="6" />
              <el-button :disabled="securityCodeCountdown > 0" :loading="securityCodeSending" @click="handleSendSecurityCode">
                {{ securityCodeCountdown > 0 ? `${securityCodeCountdown}s后重发` : '发送验证码' }}
              </el-button>
            </el-space>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="securityForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="securityForm.confirmNewPassword" type="password" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="securityVisible = false">取消</el-button>
          <el-button type="primary" :loading="securitySaving" @click="handleSaveSecurity">保存修改</el-button>
        </template>
      </el-dialog>

      <!-- 消息中心抽屉 -->
      <el-drawer v-model="messageVisible" title="消息通知" size="350px">
        <el-tabs>
          <el-tab-pane :label="`系统消息 (${messages.length})`">
            <el-list>
              <el-empty v-if="messages.length === 0" description="暂无新消息" />
              <div v-for="msg in messages" :key="msg.key || msg.id" class="message-item">
                <div class="msg-header">
                  <el-tag :type="msg.type" size="small">{{ msg.tag }}</el-tag>
                  <span class="msg-time">{{ msg.time }}</span>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
                <el-divider />
              </div>
            </el-list>
          </el-tab-pane>
          <el-tab-pane :label="`客服通知 (${notificationStore.notifications.length})`">
            <el-list>
              <el-empty v-if="notificationStore.notifications.length === 0" description="暂无通知" />
              <div
                v-for="notification in notificationStore.notifications"
                :key="notification.id"
                class="message-item"
                :class="{ 'unread': !notification.read }"
                @click="handleNotificationClick(notification)"
              >
                <div class="msg-header">
                  <el-tag :type="notification.type" size="small">
                    {{ notification.type === 'success' ? '成功' : notification.type === 'warning' ? '警告' : '通知' }}
                  </el-tag>
                  <span class="msg-time">{{ formatNotificationTime(notification.createdAt) }}</span>
                </div>
                <div class="msg-title">{{ notification.title }}</div>
                <div v-if="notification.message" class="msg-content">{{ notification.message }}</div>
                <el-divider />
              </div>
            </el-list>
          </el-tab-pane>
        </el-tabs>
        <template #footer>
          <el-button style="width: 100%" @click="handleMarkAllRead">全部标记为已读</el-button>
        </template>
      </el-drawer>

      <!-- 顶部冒泡通知 -->
      <TransitionGroup name="toast" tag="div" class="toast-container">
        <div
          v-for="notification in activeToasts"
          :key="notification.id"
          class="toast-notification"
          :class="`toast-${notification.type}`"
          @click="handleToastClick(notification)"
        >
          <div class="toast-icon">
            <el-icon v-if="notification.type === 'success'"><CircleCheckFilled /></el-icon>
            <el-icon v-else-if="notification.type === 'warning'"><WarningFilled /></el-icon>
            <el-icon v-else-if="notification.type === 'error'"><CircleCloseFilled /></el-icon>
            <el-icon v-else><InfoFilled /></el-icon>
          </div>
          <div class="toast-content">
            <div class="toast-title">{{ notification.title }}</div>
            <div v-if="notification.message" class="toast-message">{{ notification.message }}</div>
          </div>
          <el-icon class="toast-close" @click.stop="closeToast(notification.id)"><Close /></el-icon>
        </div>
      </TransitionGroup>

      <div class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  CircleCheckFilled,
  WarningFilled,
  CircleCloseFilled,
  InfoFilled,
  Close
} from '@element-plus/icons-vue'
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConfigStore } from '../store/config'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDashboardMessages, markAllDashboardMessagesRead } from '../api/dashboard'
import { getAdminOperationStatus } from '../api/operation'
import { getUserDetail, sendChangeEmailCode, sendChangePasswordEmailCode, updatePassword, updateUserProfile, uploadUserAvatar } from '../api/user'

const route = useRoute()
const router = useRouter()
const configStore = useConfigStore()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const isCollapse = ref(false)

// 用户菜单控制
const profileVisible = ref(false)
const securityVisible = ref(false)
const messageVisible = ref(false)
const messageLoading = ref(false)
const profileSaving = ref(false)
const securitySaving = ref(false)
const securityCodeSending = ref(false)
const securityCodeCountdown = ref(0)
let securityCodeTimer = null
const profileCodeSending = ref(false)
const profileCodeCountdown = ref(0)
let profileCodeTimer = null
const avatarUploading = ref(false)
const avatarFileInputRef = ref(null)

const profileForm = ref({
  id: '',
  userName: '',
  nickname: '',
  mobile: '',
  email: '',
  emailCode: '',
  avatar: '',
  createTime: ''
})

const securityForm = ref({
  oldPassword: '',
  emailCode: '',
  newPassword: '',
  confirmNewPassword: ''
})

const messages = ref([])
const unreadMessageCount = computed(() => messages.value.filter(item => item?.unread).length)
const totalUnreadCount = computed(() => unreadMessageCount.value + notificationStore.unreadCount)
const displayName = computed(() => String(profileForm.value.nickname || authStore.user?.nickname || authStore.user?.userName || '管理员'))
const displayAvatar = computed(() => String(profileForm.value.avatar || authStore.user?.avatar || ''))
const displayRole = computed(() => '系统管理员')

// 冒泡通知相关
const activeToasts = computed(() => {
  return notificationStore.notifications.filter(n => !n.read).slice(0, 3) // 最多显示3个
})

const handleToastClick = (notification) => {
  notificationStore.markAsRead(notification.id)
  // 如果有回调数据，可以跳转
  if (notification.data && notification.route) {
    router.push(notification.route)
  }
}

const closeToast = (id) => {
  notificationStore.markAsRead(id)
}

const handleNotificationClick = (notification) => {
  notificationStore.markAsRead(notification.id)
  if (notification.route) {
    messageVisible.value = false
    router.push(notification.route)
  }
}

const handleMarkAllRead = async () => {
  await clearMessages()
  notificationStore.markAllAsRead()
}

const formatNotificationTime = (createdAt) => {
  if (!createdAt) return ''
  const date = new Date(createdAt)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const resolveUploadUrl = (response) => {
  if (!response) return ''
  if (typeof response === 'string') return response
  if (typeof response === 'object') {
    if (typeof response.data === 'string') return response.data
    if (typeof response.url === 'string') return response.url
    if (typeof response.message === 'string') return response.message
  }
  return ''
}

const triggerAvatarUpload = () => {
  avatarFileInputRef.value?.click()
}

const handleAvatarFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过5MB')
    event.target.value = ''
    return
  }

  avatarUploading.value = true
  try {
    const response = await uploadUserAvatar(file)
    const url = resolveUploadUrl(response)
    if (!url) {
      throw new Error('未获取到头像地址')
    }
    profileForm.value.avatar = url
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error(error?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
    event.target.value = ''
  }
}

const loadCurrentUser = async () => {
  const userId = authStore.user?.userId
  if (!userId) return
  try {
    const data = await getUserDetail(userId)
    profileForm.value = {
      id: String(data?.id || userId),
      userName: data?.userName || authStore.user?.userName || '',
      nickname: data?.nickname || '',
      mobile: data?.mobile || '',
      email: data?.email || '',
      emailCode: '',
      avatar: data?.avatar || '',
      createTime: data?.createTime || ''
    }
    authStore.user = {
      ...(authStore.user || {}),
      userId: data?.id || userId,
      userName: data?.userName || authStore.user?.userName,
      nickname: data?.nickname || '',
      avatar: data?.avatar || '',
      email: data?.email || ''
    }
    localStorage.setItem('user', JSON.stringify(authStore.user))
  } catch (error) {
    ElMessage.error(error?.message || '加载用户信息失败')
  }
}

const startProfileCodeCountdown = () => {
  profileCodeCountdown.value = 60
  if (profileCodeTimer) {
    clearInterval(profileCodeTimer)
  }
  profileCodeTimer = setInterval(() => {
    profileCodeCountdown.value -= 1
    if (profileCodeCountdown.value <= 0) {
      clearInterval(profileCodeTimer)
      profileCodeTimer = null
    }
  }, 1000)
}

const handleSendProfileEmailCode = async () => {
  if (profileCodeSending.value || profileCodeCountdown.value > 0) {
    return
  }
  const email = String(profileForm.value.email || '').trim().toLowerCase()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('请输入正确的邮箱')
    return
  }
  profileCodeSending.value = true
  try {
    await sendChangeEmailCode(email)
    profileForm.value.email = email
    ElMessage.success('验证码已发送，请查收邮箱')
    startProfileCodeCountdown()
  } catch (error) {
    ElMessage.error(error?.message || '验证码发送失败')
  } finally {
    profileCodeSending.value = false
  }
}

const loadOperationStatus = async () => {
  try {
    const data = await getAdminOperationStatus()
    configStore.siteStatus = Boolean(data?.operating)
  } catch (error) {
    ElMessage.error(error?.message || '获取运营状态失败')
  }
}

const loadMessages = async () => {
  messageLoading.value = true
  try {
    const data = await getDashboardMessages()
    messages.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error(error?.message || '加载消息失败')
  } finally {
    messageLoading.value = false
  }
}

const handleUserCommand = async (command) => {
  if (command === 'profile') {
    await loadCurrentUser()
    profileVisible.value = true
  } else if (command === 'security') {
    securityForm.value = {
      oldPassword: '',
      emailCode: '',
      newPassword: '',
      confirmNewPassword: ''
    }
    securityVisible.value = true
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出系统吗？', '提示', {
      type: 'warning',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          ElMessageBox.confirm('是否同时清除保存的登录信息？', '清除登录信息', {
            confirmButtonText: '是',
            cancelButtonText: '否',
            type: 'info'
          }).then(() => {
            // 用户选择清除登录信息
            instance.confirmButtonLoading = true
            authStore.logout(true).then(() => {
              done()
              ElMessage.success('已安全退出并清除登录信息')
              router.push('/login')
            }).catch(() => {
              instance.confirmButtonLoading = false
            })
          }).catch(() => {
            // 用户选择不清除登录信息
            instance.confirmButtonLoading = true
            authStore.logout(false).then(() => {
              done()
              ElMessage.success('已安全退出')
              router.push('/login')
            }).catch(() => {
              instance.confirmButtonLoading = false
            })
          })
        } else {
          done()
        }
      }
    }).catch(() => {})
  }
}

const handleSaveProfile = async () => {
  const email = String(profileForm.value.email || '').trim().toLowerCase()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  const oldEmail = String(authStore.user?.email || '').trim().toLowerCase()
  if (email && email !== oldEmail && !/^\d{6}$/.test(String(profileForm.value.emailCode || '').trim())) {
    ElMessage.warning('修改邮箱需要输入6位验证码')
    return
  }

  profileSaving.value = true
  try {
    await updateUserProfile({
      id: Number(profileForm.value.id),
      nickname: profileForm.value.nickname,
      mobile: profileForm.value.mobile,
      email,
      emailCode: String(profileForm.value.emailCode || '').trim(),
      avatar: profileForm.value.avatar
    })
    await loadCurrentUser()
    ElMessage.success('个人信息已更新')
    profileVisible.value = false
  } catch (error) {
    ElMessage.error(error?.message || '个人信息更新失败')
  } finally {
    profileSaving.value = false
  }
}

const startSecurityCodeCountdown = () => {
  securityCodeCountdown.value = 60
  if (securityCodeTimer) {
    clearInterval(securityCodeTimer)
  }
  securityCodeTimer = setInterval(() => {
    securityCodeCountdown.value -= 1
    if (securityCodeCountdown.value <= 0) {
      clearInterval(securityCodeTimer)
      securityCodeTimer = null
    }
  }, 1000)
}

const handleSendSecurityCode = async () => {
  if (securityCodeSending.value || securityCodeCountdown.value > 0) {
    return
  }
  securityCodeSending.value = true
  try {
    await sendChangePasswordEmailCode()
    ElMessage.success('验证码已发送，请查收邮箱')
    startSecurityCodeCountdown()
  } catch (error) {
    ElMessage.error(error?.message || '验证码发送失败')
  } finally {
    securityCodeSending.value = false
  }
}

const handleSaveSecurity = async () => {
  if (!securityForm.value.oldPassword || !securityForm.value.emailCode || !securityForm.value.newPassword || !securityForm.value.confirmNewPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  securitySaving.value = true
  try {
    await updatePassword({
      oldPassword: securityForm.value.oldPassword,
      emailCode: securityForm.value.emailCode,
      newPassword: securityForm.value.newPassword,
      confirmNewPassword: securityForm.value.confirmNewPassword
    })
    authStore.token = ''
    authStore.user = null
    authStore.isAuthenticated = false
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('密码已修改，请重新登录')
    securityVisible.value = false
    await router.replace('/login')
  } catch (error) {
    ElMessage.error(error?.message || '密码修改失败')
  } finally {
    securitySaving.value = false
  }
}

const handleShowMessages = async () => {
  await loadMessages()
  messageVisible.value = true
}

const clearMessages = async () => {
  try {
    await markAllDashboardMessagesRead()
    await loadMessages()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error?.message || '标记已读失败')
  }
}

onMounted(async () => {
  loadOperationStatus()
  loadMessages()
  loadCurrentUser()
  // 应用主题
  configStore.applyTheme()
  // 刷新菜单数据确保图标正确
  if (authStore.isAuthenticated) {
    await authStore.fetchPermissions()
  }
  // 打印菜单数据用于调试
  console.log('Current menus:', JSON.stringify(authStore.menus, null, 2))
})

// 监听主题颜色变化
watch(() => configStore.themeColor, () => {
  configStore.applyTheme()
})

onUnmounted(() => {
  if (securityCodeTimer) {
    clearInterval(securityCodeTimer)
    securityCodeTimer = null
  }
  if (profileCodeTimer) {
    clearInterval(profileCodeTimer)
    profileCodeTimer = null
  }
})

// 菜单名称到图标的映射表
const menuIconMap = {
  '仪表盘': 'Monitor',
  '用户中心': 'UserFilled',
  '用户管理': 'User',
  '角色管理': 'Avatar',
  '注销申请管理': 'DocumentDelete',
  '管理员注册审核': 'UserFilled',
  '设计者申请审核': 'UserFilled',
  '管理员管理': 'Avatar',
  '模型管理': 'Goods',
  '模型分类': 'Menu',
  '模型管理': 'List',
  '清单管理': 'Collection',
  '订单系统': 'Ticket',
  '订单管理': 'Document',
  '售后管理': 'Service',
  '物流管理': 'Van',
  '订单评价': 'ChatLineSquare',
  '二手交易': 'Tickets',
  '商品管理': 'Goods',
  '举报处理': 'Warning',
  '打印排产': 'Clock',
  '任务排产': 'List',
  '打印机管理': 'Printer',
  '悬赏管理': 'Collection',
  '任务悬赏': 'Document',
  '评价申诉': 'Warning',
  '社区模块': 'Promotion',
  '帖子管理': 'ChatDotRound',
  '回复管理': 'Calendar',
  '分类管理': 'Menu',
  '运营管理': 'Setting',
  '轮播管理': 'PictureFilled',
  '公告管理': 'Notification',
  '优惠券管理': 'Ticket',
  '拼团管理': 'Goods',
  '拼团活动': 'List',
  '批量打印折扣': 'PriceTag',
  '活动赛事': 'Calendar',
  '活动管理': 'Calendar',
  '作品管理': 'StarFilled',
  '报名管理': 'User',
  '资金管理': 'Money',
  '提现管理': 'Tickets',
  '系统管理': 'Setting',
  '界面配置': 'Operation',
  '系统配置': 'Tools',
  '操作日志': 'Document',
  '统计报表': 'DataAnalysis',
  '订单统计': 'Document',
  '用户统计': 'User',
  '模型统计': 'Goods',
  '财务统计': 'Money',
  '悬赏统计': 'Collection',
  '客服管理': 'Service'
}

// 获取菜单图标
const getMenuIcon = (menu) => {
  // 调试输出
  console.log('getMenuIcon called with:', menu.permissionName, 'icon:', menu.icon)

  // 优先使用数据库配置的图标（排除空字符串）
  if (menu.icon && menu.icon.trim()) {
    console.log('  -> using db icon:', menu.icon.trim())
    return menu.icon.trim()
  }
  // 根据菜单名称匹配图标
  const name = menu.permissionName?.trim()
  if (name && menuIconMap[name]) {
    console.log('  -> using mapped icon:', menuIconMap[name])
    return menuIconMap[name]
  }
  // 根据菜单路径匹配
  const path = menu.menuPath?.trim()
  if (path) {
    if (path.includes('banner') || path.includes('Banner')) {
      console.log('  -> using path matched icon: PictureFilled')
      return 'PictureFilled'
    }
    if (path.includes('notice') || path.includes('Notice')) return 'Notification'
    if (path.includes('coupon')) return 'Ticket'
  }
  // 默认图标
  console.log('  -> using default icon: Menu')
  return 'Menu'
}

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '后台管理')

// AI 智能工具
const openAIAssistant = () => {
  try {
    // 每次都调用 createPageAgent，它会检查实例是否已被销毁
    const agent = window.createPageAgent ? window.createPageAgent() : null
    if (agent && agent.panel) {
      agent.panel.show()
      agent.panel.expand()
    } else {
      ElMessage.warning('AI 助手尚未初始化，请刷新页面后重试')
    }
  } catch (error) {
    console.error('打开 AI 助手失败:', error)
    ElMessage.error('打开 AI 助手失败')
  }
}
</script>

<style scoped>
/* 主容器 */
.app-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--bg-secondary);
}

/* 侧边栏容器 */
.sidebar-container {
  height: 100vh;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #fff;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1001;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Logo区域 */
.sidebar-logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary-color) 0%, #818cf8 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
  overflow: hidden;
  flex-shrink: 0;
}

.logo-icon .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-image {
  width: 28px;
  height: 28px;
  object-fit: contain;
  display: block;
}

.logo-text {
  margin-left: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

/* 菜单样式 */
.el-menu-vertical {
  border-right: none !important;
  background: transparent !important;
  flex: 1;
  padding: 12px 0;
}

.el-menu-item, :deep(.el-sub-menu__title) {
  height: 48px !important;
  line-height: 48px !important;
  margin: 3px 10px !important;
  border-radius: 10px !important;
  color: rgba(255, 255, 255, 0.7) !important;
  display: flex !important;
  align-items: center !important;
  font-weight: 500;
  transition: all 0.2s ease !important;
}

.el-menu-item .el-icon, :deep(.el-sub-menu__title .el-icon) {
  margin-right: 12px !important;
  font-size: 18px !important;
  width: 24px !important;
  text-align: center !important;
  color: rgba(255, 255, 255, 0.6);
}

.el-menu-item:hover, :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.4);
}

.el-menu-item.is-active .el-icon {
  color: #fff;
}

/* 子菜单样式 */
:deep(.el-sub-menu .el-menu-item) {
  height: 44px !important;
  line-height: 44px !important;
  margin: 2px 10px 2px 20px !important;
  padding-left: 36px !important;
  font-size: 14px;
}

:deep(.el-menu--inline) {
  background: rgba(0, 0, 0, 0.15) !important;
  margin: 4px 0;
  border-radius: 10px;
}

:deep(.el-sub-menu .el-menu-item .el-icon) {
  margin-right: 10px !important;
  font-size: 16px !important;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff !important;
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: var(--primary-light) !important;
}

/* 主内容区 */
.main-layout {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: var(--bg-secondary);
}

/* 头部栏 */
.header-bar {
  height: 64px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  border-radius: 10px;
  transition: all 0.2s ease;
  background: transparent;
}

.collapse-btn:hover {
  background: var(--bg-tertiary);
  color: var(--primary-color);
  transform: scale(1.05);
}

/* 用户下拉菜单 */
.user-info-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 12px;
  transition: background 0.2s ease;
}

.user-info-dropdown:hover {
  background: var(--bg-tertiary);
}

.user-meta {
  margin-left: 12px;
  text-align: left;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 状态指示器 */
.status-indicator {
  display: flex;
  align-items: center;
}

/* 内容区 */
.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 页面过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 消息项样式 */
.message-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: var(--bg-tertiary);
  margin: 0 -20px;
  padding: 14px 20px;
}

.message-item:last-child {
  border-bottom: none;
}

.message-item.unread {
  background: var(--primary-lighter);
  margin: 0 -20px;
  padding: 14px 20px;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.msg-time {
  font-size: 12px;
  color: var(--text-muted);
}

.msg-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.msg-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 侧边栏折叠状态 */
.sidebar-container[style*="64px"] .sidebar-logo {
  padding: 0;
  justify-content: center;
}

.sidebar-container[style*="64px"] .logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

.sidebar-container[style*="64px"] .el-menu-item,
.sidebar-container[style*="64px"] :deep(.el-sub-menu__title) {
  margin: 3px 6px !important;
  padding: 0 !important;
  justify-content: center !important;
}

.sidebar-container[style*="64px"] .el-menu-item .el-icon,
.sidebar-container[style*="64px"] :deep(.el-sub-menu__title .el-icon) {
  margin-right: 0 !important;
}

/* 浅色侧边栏样式 */
.sidebar-light {
  background: var(--bg-primary) !important;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.05) !important;
}

.sidebar-light .sidebar-logo {
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
}

.sidebar-light .logo-text {
  color: var(--text-primary);
}

.sidebar-light .el-menu-item,
.sidebar-light :deep(.el-sub-menu__title) {
  color: var(--text-secondary) !important;
}

.sidebar-light .el-menu-item .el-icon,
.sidebar-light :deep(.el-sub-menu__title .el-icon) {
  color: var(--text-muted);
}

.sidebar-light .el-menu-item:hover,
.sidebar-light :deep(.el-sub-menu__title:hover) {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.sidebar-light .el-menu-item.is-active {
  background: var(--primary-color) !important;
  color: #fff !important;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
}

.sidebar-light .el-menu-item.is-active .el-icon {
  color: #fff;
}

.sidebar-light :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--text-primary) !important;
}

.sidebar-light :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: var(--primary-color) !important;
}

.sidebar-light :deep(.el-menu--inline) {
  background: var(--bg-secondary) !important;
}

/* AI 智能工具按钮 */
.ai-assistant-btn {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 48px;
  margin: 12px 10px;
  border-radius: 10px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.3) 0%, rgba(79, 70, 229, 0.3) 100%);
  border: 1px solid rgba(129, 140, 248, 0.3);
}

.ai-assistant-btn:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.5) 0%, rgba(79, 70, 229, 0.5) 100%);
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.ai-assistant-btn .el-icon {
  font-size: 18px;
  margin-right: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.ai-assistant-btn:hover .el-icon {
  color: #fff;
}

.ai-assistant-btn.collapsed {
  padding: 0;
  justify-content: center;
}

.ai-assistant-btn.collapsed .el-icon {
  margin-right: 0;
}

/* 浅色主题下的 AI 按钮 */
.sidebar-light .ai-assistant-btn {
  background: linear-gradient(135deg, var(--primary-lighter) 0%, rgba(99, 102, 241, 0.2) 100%);
  border-color: var(--primary-lighter);
  color: var(--primary-color);
}

.sidebar-light .ai-assistant-btn:hover {
  background: linear-gradient(135deg, var(--primary-lighter) 0%, rgba(99, 102, 241, 0.3) 100%);
  color: var(--primary-dark);
}

.sidebar-light .ai-assistant-btn .el-icon {
  color: var(--primary-color);
}

/* 冒泡通知样式 */
.toast-container {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
}

.toast-notification {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  min-width: 320px;
  max-width: 450px;
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.3s ease;
}

.toast-notification:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.toast-success {
  border-left: 4px solid #10b981;
}

.toast-warning {
  border-left: 4px solid #f59e0b;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}

.toast-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.toast-success .toast-icon {
  color: #10b981;
}

.toast-warning .toast-icon {
  color: #f59e0b;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.toast-message {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.toast-close {
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.2s;
}

.toast-close:hover {
  color: var(--text-primary);
}

/* 通知动画 */
.toast-enter-active {
  animation: toast-in 0.3s ease;
}

.toast-leave-active {
  animation: toast-out 0.3s ease;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-20px);
  }
}
</style>
