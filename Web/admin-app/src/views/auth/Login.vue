<template>
  <div class="login-container">
    <div class="login-box" :class="{ 'is-register': isRegister }">
      <div class="login-left">
        <div class="brand">
          <el-icon :size="48" color="#fff"><cpu /></el-icon>
          <h1>3D Pro</h1>
          <p>打印定制商城后台管理系统</p>
        </div>
      </div>

      <div class="login-right">
        <div class="form-header">
          <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
          <p>{{ isRegister ? '加入 3D 打印定制管理平台' : '请输入您的账号或邮箱以登录系统' }}</p>
        </div>

        <el-form :model="loginForm" class="login-form" @submit.prevent="handleAction">
          <el-form-item>
            <el-input v-model="loginForm.username" placeholder="登录账号 / 邮箱" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister">
            <el-input v-model="loginForm.nickname" placeholder="昵称" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister">
            <el-input v-model="loginForm.email" placeholder="电子邮箱" clearable>
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister">
            <el-input v-model="loginForm.emailCode" placeholder="邮箱验证码" maxlength="6" clearable>
              <template #prefix><el-icon><Message /></el-icon></template>
              <template #append>
                <el-button :disabled="emailCodeCountdown > 0" :loading="emailCodeSending" @click="handleSendRegisterCode">
                  {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password clearable>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="isRegister">
            <el-input v-model="loginForm.confirmPassword" type="password" placeholder="确认密码" show-password clearable>
              <template #prefix><el-icon><CircleCheck /></el-icon></template>
            </el-input>
          </el-form-item>

          <div v-if="!isRegister" class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-button link type="primary" @click="handleForgotPassword">忘记密码？</el-button>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleAction">
            {{ isRegister ? '提交注册申请' : '登录系统' }}
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
          <el-button link type="primary" @click="isRegister = !isRegister">
            {{ isRegister ? '返回登录' : '立即注册' }}
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="forgotVisible" title="邮箱找回密码" width="520px">
      <el-form :model="forgotForm" label-width="100px">
        <el-form-item label="登录账户">
          <el-input v-model="forgotForm.userName" placeholder="请输入登录账户" clearable />
        </el-form-item>
        <el-form-item label="绑定邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入绑定邮箱" clearable />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <el-space style="width: 100%">
            <el-input v-model="forgotForm.emailCode" placeholder="请输入6位验证码" maxlength="6" />
            <el-button :disabled="forgotCodeCountdown > 0" :loading="forgotCodeSending" @click="handleSendForgotCode">
              {{ forgotCodeCountdown > 0 ? `${forgotCodeCountdown}s` : '发送验证码' }}
            </el-button>
          </el-space>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="forgotForm.confirmNewPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="forgotSaving" @click="handleResetPasswordByEmail">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Message, Lock, CircleCheck, Cpu } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const isRegister = ref(false)
const loading = ref(false)
const rememberMe = ref(false)
const emailCodeSending = ref(false)
const emailCodeCountdown = ref(0)
const forgotVisible = ref(false)
const forgotCodeSending = ref(false)
const forgotCodeCountdown = ref(0)
const forgotSaving = ref(false)
let registerCodeTimer = null
let forgotCodeTimer = null

const loginForm = reactive({
  username: '',
  password: '',
  email: '',
  emailCode: '',
  confirmPassword: '',
  nickname: ''
})

const forgotForm = reactive({
  userName: '',
  email: '',
  emailCode: '',
  newPassword: '',
  confirmNewPassword: ''
})

// 页面加载时检查是否有记住的用户名和密码
onMounted(() => {
  const rememberedUser = authStore.getRememberedUser()
  const rememberedPassword = authStore.getRememberedPassword()
  if (rememberedUser) {
    loginForm.username = rememberedUser
    loginForm.password = rememberedPassword
    rememberMe.value = true
  }
})

const handleAction = async () => {
  if (!loginForm.username || !loginForm.password) {
    return ElMessage.warning('请完整填写信息')
  }

  if (isRegister.value) {
    if (!loginForm.email || !loginForm.emailCode || !loginForm.confirmPassword || !loginForm.nickname) {
      return ElMessage.warning('请完整填写注册信息')
    }
  }

  if (isRegister.value && loginForm.password !== loginForm.confirmPassword) {
    return ElMessage.warning('两次密码输入不一致')
  }

  loading.value = true

  try {
    let success = false
    if (isRegister.value) {
      success = await authStore.register(
        loginForm.username, 
        loginForm.password, 
        loginForm.email, 
        loginForm.emailCode,
        loginForm.nickname,
        loginForm.confirmPassword
      )
    } else {
      success = await authStore.login(loginForm.username, loginForm.password, rememberMe.value)
    }

    if (success && !isRegister.value) {
      router.push('/dashboard')
    }
    if (success && isRegister.value) {
      isRegister.value = false
      loginForm.password = ''
      loginForm.confirmPassword = ''
      loginForm.emailCode = ''
      ElMessage.info('申请已提交，审核通过后可登录后台')
    }
  } catch (error) {
    console.error('操作失败:', error)
  } finally {
    loading.value = false
  }
}

const startCountdown = (target, assignTimer) => {
  target.value = 60
  const timer = setInterval(() => {
    target.value -= 1
    if (target.value <= 0) {
      clearInterval(timer)
      assignTimer(null)
    }
  }, 1000)
  assignTimer(timer)
}

const clearRegisterTimer = () => {
  if (registerCodeTimer) {
    clearInterval(registerCodeTimer)
    registerCodeTimer = null
  }
}

const clearForgotTimer = () => {
  if (forgotCodeTimer) {
    clearInterval(forgotCodeTimer)
    forgotCodeTimer = null
  }
}

const handleSendRegisterCode = async () => {
  const email = String(loginForm.email || '').trim().toLowerCase()
  if (!email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (emailCodeCountdown.value > 0 || emailCodeSending.value) {
    return
  }
  emailCodeSending.value = true
  try {
    const success = await authStore.sendAdminRegisterEmailCode(email)
    if (success) {
      loginForm.email = email
      clearRegisterTimer()
      startCountdown(emailCodeCountdown, (timer) => {
        registerCodeTimer = timer
      })
    }
  } finally {
    emailCodeSending.value = false
  }
}

// 处理忘记密码
const handleForgotPassword = () => {
  forgotForm.userName = loginForm.username || ''
  forgotForm.email = ''
  forgotForm.emailCode = ''
  forgotForm.newPassword = ''
  forgotForm.confirmNewPassword = ''
  forgotVisible.value = true
}

const handleSendForgotCode = async () => {
  const userName = String(forgotForm.userName || '').trim()
  const email = String(forgotForm.email || '').trim().toLowerCase()
  if (!userName || !email) {
    ElMessage.warning('请输入账号和邮箱')
    return
  }
  if (forgotCodeCountdown.value > 0 || forgotCodeSending.value) {
    return
  }
  forgotCodeSending.value = true
  try {
    const success = await authStore.sendForgotPasswordEmailCode(userName, email)
    if (success) {
      forgotForm.email = email
      clearForgotTimer()
      startCountdown(forgotCodeCountdown, (timer) => {
        forgotCodeTimer = timer
      })
    }
  } finally {
    forgotCodeSending.value = false
  }
}

const handleResetPasswordByEmail = async () => {
  if (!forgotForm.userName || !forgotForm.email || !forgotForm.emailCode || !forgotForm.newPassword || !forgotForm.confirmNewPassword) {
    ElMessage.warning('请完整填写找回信息')
    return
  }
  forgotSaving.value = true
  try {
    const success = await authStore.resetPasswordByEmail({
      userName: forgotForm.userName,
      email: forgotForm.email,
      emailCode: forgotForm.emailCode,
      newPassword: forgotForm.newPassword,
      confirmNewPassword: forgotForm.confirmNewPassword
    })
    if (success) {
      forgotVisible.value = false
      loginForm.password = ''
    }
  } finally {
    forgotSaving.value = false
  }
}

onUnmounted(() => {
  clearRegisterTimer()
  clearForgotTimer()
})
</script>

<style scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #f1f5f9 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.08) 0%, transparent 70%);
  top: -300px;
  right: -200px;
  animation: pulse 8s ease-in-out infinite;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.06) 0%, transparent 70%);
  bottom: -200px;
  left: -100px;
  animation: pulse 10s ease-in-out infinite reverse;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.login-box {
  width: 900px;
  height: 560px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 25px 80px -12px rgba(0, 0, 0, 0.15),
              0 0 0 1px rgba(0, 0, 0, 0.02);
  display: flex;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
}

.login-box:hover {
  transform: translateY(-4px);
  box-shadow: 0 35px 100px -12px rgba(0, 0, 0, 0.2),
              0 0 0 1px rgba(0, 0, 0, 0.02);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 50%, #818cf8 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
}

.login-left::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
}

.brand {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.brand .el-icon {
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
}

.brand h1 {
  font-size: 36px;
  font-weight: 800;
  margin-top: 20px;
  letter-spacing: 2px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.brand p {
  opacity: 0.9;
  margin-top: 12px;
  font-size: 15px;
  font-weight: 500;
}

.login-right {
  width: 450px;
  padding: 60px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #fff;
}

.form-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.form-header p {
  color: var(--text-secondary);
  margin-top: 10px;
  font-size: 14px;
}

.login-form {
  margin-top: 36px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 10px 16px !important;
  border-radius: 12px !important;
  background-color: var(--bg-secondary) !important;
  box-shadow: none !important;
  border: 1px solid var(--border-color) !important;
  transition: all 0.2s ease !important;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--border-dark) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 3px var(--primary-lighter) !important;
  background-color: #fff !important;
}

.login-form :deep(.el-input__prefix) {
  color: var(--text-muted);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary-color) 0%, #6366f1 100%) !important;
  border: none !important;
  box-shadow: 0 8px 20px -4px rgba(79, 70, 229, 0.4);
  transition: all 0.3s ease !important;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px -4px rgba(79, 70, 229, 0.5) !important;
}

.submit-btn:active {
  transform: translateY(0);
}

.form-footer {
  margin-top: 28px;
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 注册模式调整高度 */
.is-register {
  height: 680px;
}

/* 响应式 */
@media (max-width: 960px) {
  .login-box {
    width: 90%;
    max-width: 450px;
    height: auto;
  }

  .login-left {
    display: none;
  }

  .login-right {
    width: 100%;
    padding: 40px 30px;
  }
}
</style>
