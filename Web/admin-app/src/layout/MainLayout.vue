<template>
  <div class="app-wrapper">
    <!-- Sidebar -->
    <aside class="sidebar-container" :style="{ width: isCollapse ? '64px' : '260px' }">
      <div class="sidebar-logo">
        <el-icon :size="28" color="#4f46e5"><cpu /></el-icon>
        <span v-show="!isCollapse" class="logo-text">{{ configStore.siteName }}</span>
      </div>
      
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          :collapse="isCollapse"
          router
        >
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

          <el-menu-item index="/bounty">
            <el-icon><collection /></el-icon>
            <template #title>任务悬赏</template>
          </el-menu-item>

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
              <el-icon><picture /></el-icon>
              <span>轮播管理</span>
            </el-menu-item>
            <el-menu-item index="/operation/notices">
              <el-icon><notification /></el-icon>
              <span>公告管理</span>
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

          <el-menu-item index="/config">
            <el-icon><operation /></el-icon>
            <template #title>系统配置</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
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
            <el-badge :value="unreadMessageCount" :hidden="unreadMessageCount <= 0" class="item">
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
        <template #footer>
          <el-button style="width: 100%" @click="clearMessages">全部标记为已读</el-button>
        </template>
      </el-drawer>

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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConfigStore } from '../store/config'
import { useAuthStore } from '../stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDashboardMessages, markAllDashboardMessagesRead } from '../api/dashboard'
import { getAdminOperationStatus } from '../api/operation'
import { getUserDetail, sendChangeEmailCode, sendChangePasswordEmailCode, updatePassword, updateUserProfile, uploadUserAvatar } from '../api/user'

const route = useRoute()
const router = useRouter()
const configStore = useConfigStore()
const authStore = useAuthStore()
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
const displayName = computed(() => String(profileForm.value.nickname || authStore.user?.nickname || authStore.user?.userName || '管理员'))
const displayAvatar = computed(() => String(profileForm.value.avatar || authStore.user?.avatar || ''))
const displayRole = computed(() => '系统管理员')

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

onMounted(() => {
  loadOperationStatus()
  loadMessages()
  loadCurrentUser()
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

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '后台管理')
</script>

<style scoped>
.app-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar-container {
  height: 100vh;
  background: #1e293b;
  color: #fff;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1001;
  box-shadow: 4px 0 10px rgba(0,0,0,0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo-text {
  margin-left: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  white-space: normal;
  line-height: 1.4;
}

.status-indicator {
  display: flex;
  align-items: center;
}

.el-menu-vertical {
  border-right: none !important;
  background: transparent !important;
  flex: 1;
}

/* 菜单基础样式 */
.el-menu-item, :deep(.el-sub-menu__title) {
  height: 50px !important;
  line-height: 50px !important;
  margin: 4px 12px !important;
  border-radius: 8px !important;
  color: #cbd5e1 !important; /* 颜色更白一些，从 #94a3b8 改为 #cbd5e1 */
  display: flex !important;
  align-items: center !important;
}

/* 统一图标间距 */
.el-menu-item .el-icon, :deep(.el-sub-menu__title .el-icon) {
  margin-right: 12px !important;
  font-size: 18px !important;
  width: 24px !important;
  text-align: center !important;
}

/* 悬停与激活状态 */
.el-menu-item:hover, :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #fff !important;
}

.el-menu-item.is-active {
  background: #4f46e5 !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

/* 子菜单项样式统一 */
:deep(.el-sub-menu .el-menu-item) {
  height: 50px !important;
  line-height: 50px !important;
  margin: 4px 12px !important;
  padding-left: 48px !important;
}

/* 确保子菜单展开后的背景透明 */
:deep(.el-menu--inline) {
  background-color: transparent !important;
}

/* 移除子菜单项的内层默认 padding 以防对齐偏差 */
:deep(.el-sub-menu .el-menu-item .el-icon) {
  margin-right: 12px !important;
}

/* 修复子菜单激活时的父级样式 */
:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff !important;
}

/* 侧边栏折叠后的处理 */
.sidebar-container.is-collapse .el-menu-item,
.sidebar-container.is-collapse :deep(.el-sub-menu__title) {
  margin: 4px 6px !important;
  padding: 0 16px !important;
}

.main-layout {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #f8fafc;
}

.header-bar {
  height: 64px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #64748b;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: #f1f5f9;
  color: #4f46e5;
}

.user-info-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}

.user-meta {
  margin-left: 10px;
  text-align: left;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.user-role {
  font-size: 12px;
  color: #64748b;
}

.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.page-fade-enter-active, .page-fade-leave-active {
  transition: all 0.3s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateX(10px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

/* 消息项样式 */
.message-item {
  padding: 12px 0;
}
.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.msg-time {
  font-size: 12px;
  color: #94a3b8;
}
.msg-content {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.5;
}
</style>
