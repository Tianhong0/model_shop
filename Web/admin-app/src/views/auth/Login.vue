<template>
  <div class="login-container" :class="{ 'dark-mode': isDarkMode }">
    <!-- 主题切换按钮 -->
    <button type="button" class="theme-toggle" @click="isDarkMode = !isDarkMode" :title="isDarkMode ? '切换白天模式' : '切换夜晚模式'">
      <span class="theme-toggle-track">
        <span class="theme-toggle-thumb">
          <svg class="theme-icon sun-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <svg class="theme-icon moon-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </span>
      </span>
    </button>

    <!-- 动态背景层 -->
    <div class="bg-layer">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
      <div class="grid-overlay"></div>
    </div>

    <!-- 浮动粒子 -->
    <div class="particles">
      <span v-for="i in 12" :key="i" class="particle" :style="getParticleStyle(i)"></span>
    </div>

    <div class="login-box" :class="{ 'is-register': isRegister }">
      <!-- 左侧品牌区域 -->
      <div class="login-left">
        <div class="left-content">
          <div class="logo-ring">
            <el-icon :size="42" :color="isDarkMode ? '#fff' : '#fff'"><cpu /></el-icon>
          </div>
          <h1>3D Pro</h1>
          <p>打印定制商城后台管理系统</p>
          <div class="deco-line"></div>
        </div>
        <div class="floating-shapes">
          <span class="shape shape-1"></span>
          <span class="shape shape-2"></span>
          <span class="shape shape-3"></span>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="login-right">
        <!-- 动画切换按钮 -->
        <div class="mode-switcher">
          <button type="button" class="switch-btn" :class="{ 'to-register': isRegister }" @click="isRegister = !isRegister">
            <span class="switch-track">
              <span class="switch-thumb">
                <svg class="switch-icon login-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                  <polyline points="10 17 15 12 10 7"/>
                  <line x1="15" y1="12" x2="3" y2="12"/>
                </svg>
                <svg class="switch-icon register-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="8.5" cy="7" r="4"/>
                  <line x1="20" y1="8" x2="20" y2="14"/>
                  <line x1="23" y1="11" x2="17" y2="11"/>
                </svg>
              </span>
            </span>
            <span class="switch-labels">
              <span class="label-login" :class="{ active: !isRegister }">登录</span>
              <span class="label-register" :class="{ active: isRegister }">注册</span>
            </span>
          </button>
        </div>

        <!-- 表单内容 -->
        <div class="form-wrapper">
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
const isDarkMode = ref(false)
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

// 粒子样式生成
const getParticleStyle = (_index) => {
  const size = 4 + Math.random() * 8
  const left = Math.random() * 100
  const top = Math.random() * 100
  const delay = Math.random() * 20
  const duration = 15 + Math.random() * 20
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

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
/* ========== 主容器 - 白天模式（默认） ========== */
.login-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 50%, #f0f9ff 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Segoe UI', system-ui, sans-serif;
  transition: background 0.5s ease;
}

.login-container.dark-mode {
  background: #0a0e17;
}

/* ========== 主题切换按钮 ========== */
.theme-toggle {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50px;
  padding: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.theme-toggle:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.dark-mode .theme-toggle {
  background: rgba(30, 41, 59, 0.9);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.theme-toggle-track {
  width: 48px;
  height: 48px;
  position: relative;
}

.theme-toggle-thumb {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 15px rgba(251, 191, 36, 0.4);
}

.dark-mode .theme-toggle-thumb {
  transform: rotateY(180deg);
  background: linear-gradient(135deg, #22d3ee, #14b8a6);
  box-shadow: 0 4px 15px rgba(34, 211, 238, 0.4);
}

.theme-icon {
  width: 22px;
  height: 22px;
  position: absolute;
  transition: all 0.3s ease;
  color: #0a0e17;
}

.sun-icon {
  opacity: 1;
  transform: rotateY(0deg);
}

.moon-icon {
  opacity: 0;
  transform: rotateY(180deg);
}

.dark-mode .sun-icon {
  opacity: 0;
  transform: rotateY(-180deg);
}

.dark-mode .moon-icon {
  opacity: 1;
  transform: rotateY(0deg);
  color: #0a0e17;
}

/* ========== 动态背景层 - 白天模式 ========== */
.bg-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 1;
  opacity: 0.7;
  transition: opacity 0.5s ease;
}

.login-container.dark-mode .bg-layer {
  opacity: 1;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  transition: opacity 0.5s ease;
}

.login-container.dark-mode .gradient-orb {
  opacity: 0.6;
}

.orb-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #93c5fd 0%, transparent 70%);
  top: -200px;
  right: -100px;
  animation: float-orb 20s ease-in-out infinite;
}

.login-container.dark-mode .orb-1 {
  background: radial-gradient(circle, #14b8a6 0%, transparent 70%);
}

.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #bfdbfe 0%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation: float-orb 25s ease-in-out infinite reverse;
}

.login-container.dark-mode .orb-2 {
  background: radial-gradient(circle, #22d3ee 0%, transparent 70%);
}

.orb-3 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #60a5fa 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.3;
  animation: pulse-orb 15s ease-in-out infinite;
}

.login-container.dark-mode .orb-3 {
  background: radial-gradient(circle, #fbbf24 0%, transparent 70%);
  opacity: 0.3;
}

@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -40px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(40px, 30px) scale(1.02); }
}

@keyframes pulse-orb {
  0%, 100% { opacity: 0.2; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 0.4; transform: translate(-50%, -50%) scale(1.2); }
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

.login-container.dark-mode .grid-overlay {
  background-image:
    linear-gradient(rgba(34, 211, 238, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(34, 211, 238, 0.03) 1px, transparent 1px);
}

/* ========== 浮动粒子 ========== */
.particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: #60a5fa;
  border-radius: 50%;
  opacity: 0;
  animation: particle-float linear infinite;
  box-shadow: 0 0 10px rgba(96, 165, 250, 0.5);
}

.login-container.dark-mode .particle {
  background: #22d3ee;
  box-shadow: 0 0 10px #22d3ee;
}

@keyframes particle-float {
  0% {
    opacity: 0;
    transform: translateY(100vh) scale(0);
  }
  10% { opacity: 0.8; }
  90% { opacity: 0.8; }
  100% {
    opacity: 0;
    transform: translateY(-100vh) scale(1);
  }
}

/* ========== 登录卡片 - 白天模式 ========== */
.login-box {
  width: 960px;
  min-height: 600px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 28px;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.05),
    0 25px 50px -12px rgba(0, 0, 0, 0.15);
  display: flex;
  overflow: hidden;
  position: relative;
  z-index: 10;
  backdrop-filter: blur(20px);
  transition: all 0.5s ease;
}

.login-box::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 28px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), transparent 50%, rgba(96, 165, 250, 0.15));
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: exclude;
  pointer-events: none;
}

.login-container.dark-mode .login-box {
  background: rgba(15, 23, 42, 0.85);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.05),
    0 25px 50px -12px rgba(0, 0, 0, 0.5),
    0 0 100px -20px rgba(34, 211, 238, 0.4);
}

.login-container.dark-mode .login-box::before {
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.3), transparent 50%, rgba(251, 191, 36, 0.2));
}

.login-box.is-register {
  min-height: 720px;
  transition: min-height 0.3s ease;
}

/* ========== 左侧品牌区域 - 白天模式 ========== */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 50%, #93c5fd 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: background 0.5s ease;
}

.login-container.dark-mode .login-left {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.login-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.1) 0%, transparent 40%);
}

.login-container.dark-mode .login-left::before {
  background:
    radial-gradient(circle at 20% 30%, rgba(34, 211, 238, 0.15) 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(20, 184, 166, 0.1) 0%, transparent 40%);
}

.left-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.logo-ring {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.logo-ring::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
  opacity: 0.5;
  animation: ring-expand 3s ease-in-out infinite;
}

.login-container.dark-mode .logo-ring {
  background: linear-gradient(135deg, #22d3ee, #14b8a6);
}

.login-container.dark-mode .logo-ring::before {
  border-color: #22d3ee;
}

@keyframes ring-expand {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.2; }
}

.left-content h1 {
  font-size: 42px;
  font-weight: 800;
  letter-spacing: 3px;
  color: #fff;
  margin-bottom: 12px;
}

.login-container.dark-mode .left-content h1 {
  background: linear-gradient(135deg, #fff 0%, #22d3ee 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.left-content p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 400;
  letter-spacing: 0.5px;
}

.login-container.dark-mode .left-content p {
  color: #94a3b8;
}

.deco-line {
  width: 60px;
  height: 3px;
  background: rgba(255, 255, 255, 0.5);
  margin: 28px auto 0;
  border-radius: 2px;
  position: relative;
}

.login-container.dark-mode .deco-line {
  background: linear-gradient(90deg, #22d3ee, #14b8a6);
}

.deco-line::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: dot-pulse 2s ease-in-out infinite;
}

.login-container.dark-mode .deco-line::after {
  background: #22d3ee;
}

@keyframes dot-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  50% { transform: translate(-50%, -50%) scale(1.5); opacity: 0.5; }
}

/* 浮动装饰 */
.floating-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.shape {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.15);
  animation: shape-float 20s ease-in-out infinite;
}

.login-container.dark-mode .shape {
  border-color: rgba(34, 211, 238, 0.2);
}

.shape-1 {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  top: 15%;
  left: 10%;
}

.shape-2 {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  bottom: 20%;
  right: 15%;
  animation-delay: -5s;
}

.shape-3 {
  width: 40px;
  height: 40px;
  top: 60%;
  left: 20%;
  animation-delay: -10s;
  transform: rotate(45deg);
}

@keyframes shape-float {
  0%, 100% { transform: translateY(0) rotate(0deg); opacity: 0.3; }
  50% { transform: translateY(-30px) rotate(180deg); opacity: 0.6; }
}

/* ========== 右侧表单区域 - 白天模式 ========== */
.login-right {
  width: 480px;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: transparent;
  position: relative;
}

/* ========== 动画切换按钮 - 白天模式 ========== */
.mode-switcher {
  position: absolute;
  top: 30px;
  right: 30px;
  z-index: 20;
}

.switch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.switch-btn:hover {
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.15);
}

.login-container.dark-mode .switch-btn {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.1);
}

.login-container.dark-mode .switch-btn:hover {
  border-color: rgba(34, 211, 238, 0.3);
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.2);
}

.switch-track {
  width: 44px;
  height: 44px;
  position: relative;
}

.switch-thumb {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.switch-btn.to-register .switch-thumb {
  transform: rotateY(180deg);
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 4px 15px rgba(37, 99, 235, 0.4);
}

.login-container.dark-mode .switch-thumb {
  background: linear-gradient(135deg, #22d3ee, #14b8a6);
  box-shadow: 0 4px 15px rgba(34, 211, 238, 0.4);
}

.login-container.dark-mode .switch-btn.to-register .switch-thumb {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  box-shadow: 0 4px 15px rgba(251, 191, 36, 0.4);
}

.switch-icon {
  width: 20px;
  height: 20px;
  position: absolute;
  transition: all 0.3s ease;
  color: #fff;
}

.login-icon {
  opacity: 1;
  transform: rotateY(0deg);
}

.register-icon {
  opacity: 0;
  transform: rotateY(180deg);
}

.switch-btn.to-register .login-icon {
  opacity: 0;
  transform: rotateY(-180deg);
}

.switch-btn.to-register .register-icon {
  opacity: 1;
  transform: rotateY(0deg);
}

.dark-mode .switch-icon {
  color: #0a0e17;
}

.switch-labels {
  display: flex;
  gap: 4px;
  padding-right: 12px;
}

.switch-labels span {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  transition: all 0.3s ease;
  padding: 4px 10px;
  border-radius: 20px;
}

.switch-labels span.active {
  color: #1e293b;
  background: rgba(59, 130, 246, 0.15);
}

.login-container.dark-mode .switch-labels span {
  color: #94a3b8;
}

.login-container.dark-mode .switch-labels span.active {
  color: #f1f5f9;
  background: rgba(34, 211, 238, 0.2);
}

.login-container.dark-mode .switch-btn.to-register .switch-labels span.active {
  background: rgba(251, 191, 36, 0.2);
}

/* ========== 表单动画容器 ========== */
.form-wrapper {
  opacity: 1;
}

/* ========== 表单头部 - 白天模式 ========== */
.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.5px;
  margin-bottom: 8px;
  transition: color 0.3s ease;
}

.login-container.dark-mode .form-header h2 {
  color: #f1f5f9;
}

.form-header p {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  transition: color 0.3s ease;
}

.login-container.dark-mode .form-header p {
  color: #94a3b8;
}

/* ========== 表单样式 - 白天模式 ========== */
.login-form {
  margin-top: 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 14px 18px !important;
  border-radius: 14px !important;
  background: rgba(0, 0, 0, 0.03) !important;
  box-shadow: none !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.3s ease !important;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 0, 0, 0.15) !important;
  background: rgba(0, 0, 0, 0.05) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
  background: #fff !important;
}

.login-container.dark-mode .login-form :deep(.el-input__wrapper) {
  background: rgba(30, 41, 59, 0.5) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

.login-container.dark-mode .login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.15) !important;
  background: rgba(30, 41, 59, 0.7) !important;
}

.login-container.dark-mode .login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #22d3ee !important;
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.15) !important;
  background: rgba(30, 41, 59, 0.8) !important;
}

.login-form :deep(.el-input__inner) {
  color: #1e293b !important;
  font-size: 14px;
}

.login-container.dark-mode .login-form :deep(.el-input__inner) {
  color: #f1f5f9 !important;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #94a3b8 !important;
}

.login-container.dark-mode .login-form :deep(.el-input__inner::placeholder) {
  color: #64748b !important;
}

.login-form :deep(.el-input__prefix) {
  color: #94a3b8;
}

.login-container.dark-mode .login-form :deep(.el-input__prefix) {
  color: #64748b;
}

.login-form :deep(.el-input__suffix) {
  color: #94a3b8;
}

/* ========== 验证码输入区域样式 ========== */
.login-form :deep(.el-input-group) {
  display: flex !important;
  align-items: stretch !important;
}

.login-form :deep(.el-input-group__prepend) {
  display: none !important;
}

.login-form :deep(.el-input-group__append) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin-left: 10px !important;
  box-shadow: none !important;
}

.login-form :deep(.el-input-group .el-input__wrapper) {
  flex: 1 !important;
  border-top-right-radius: 14px !important;
  border-bottom-right-radius: 14px !important;
  margin-right: 18px !important;
}

.login-form :deep(.el-input-group__append .el-button) {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600 !important;
  font-size: 12px !important;
  padding: 4 0px !important;
  height: 44px !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3) !important;
  transition: all 0.3s ease !important;
  white-space: nowrap !important;
  min-width: auto !important;
}

.login-form :deep(.el-input-group__append .el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4) !important;
}

.login-form :deep(.el-input-group__append .el-button:disabled) {
  background: rgba(0, 0, 0, 0.1) !important;
  color: #94a3b8 !important;
  box-shadow: none !important;
  transform: none;
}

.login-form :deep(.el-input-group__append .el-button.is-loading) {
  background: rgba(59, 130, 246, 0.5) !important;
}

.login-container.dark-mode .login-form :deep(.el-input-group__append .el-button) {
  background: linear-gradient(135deg, #22d3ee 0%, #14b8a6 100%) !important;
  color: #0a0e17 !important;
  box-shadow: 0 4px 15px rgba(34, 211, 238, 0.3) !important;
}

.login-container.dark-mode .login-form :deep(.el-input-group__append .el-button:hover) {
  box-shadow: 0 6px 20px rgba(34, 211, 238, 0.4) !important;
}

.login-container.dark-mode .login-form :deep(.el-input-group__append .el-button:disabled) {
  background: rgba(30, 41, 59, 0.8) !important;
}

.login-container.dark-mode .login-form :deep(.el-input-group__append .el-button.is-loading) {
  background: rgba(34, 211, 238, 0.5) !important;
}

/* ========== 表单选项 - 白天模式 ========== */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.form-options :deep(.el-checkbox__label) {
  color: #64748b;
  font-size: 13px;
}

.login-container.dark-mode .form-options :deep(.el-checkbox__label) {
  color: #94a3b8;
}

.form-options :deep(.el-checkbox__inner) {
  background: #fff;
  border-color: #d1d5db;
}

.login-container.dark-mode .form-options :deep(.el-checkbox__inner) {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.15);
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #3b82f6;
  border-color: #3b82f6;
}

.login-container.dark-mode .form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #22d3ee;
  border-color: #22d3ee;
}

.form-options :deep(.el-button--primary) {
  color: #3b82f6;
  font-weight: 500;
}

.login-container.dark-mode .form-options :deep(.el-button--primary) {
  color: #22d3ee;
}

.form-options :deep(.el-button--primary:hover) {
  color: #1e293b;
}

.login-container.dark-mode .form-options :deep(.el-button--primary:hover) {
  color: #f1f5f9;
}

/* ========== 提交按钮 - 白天模式 ========== */
.submit-btn {
  width: 100%;
  height: 54px;
  border-radius: 14px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%) !important;
  border: none !important;
  box-shadow: 0 8px 30px -4px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease !important;
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255, 255, 255, 0.2) 50%, transparent 100%);
  transform: translateX(-100%);
  transition: transform 0.5s ease;
}

.submit-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 40px -4px rgba(59, 130, 246, 0.5) !important;
}

.submit-btn:hover::before {
  transform: translateX(100%);
}

.submit-btn:active {
  transform: translateY(-1px);
}

.submit-btn :deep(span) {
  color: #fff;
}

.login-container.dark-mode .submit-btn {
  background: linear-gradient(135deg, #22d3ee 0%, #14b8a6 100%) !important;
  box-shadow: 0 8px 30px -4px rgba(34, 211, 238, 0.4);
}

.login-container.dark-mode .submit-btn:hover {
  box-shadow: 0 12px 40px -4px rgba(34, 211, 238, 0.5) !important;
}

.login-container.dark-mode .submit-btn :deep(span) {
  color: #0a0e17;
}

/* ========== 对话框样式 - 白天模式 ========== */
:deep(.el-dialog) {
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

:deep(.el-dialog__title) {
  color: #1e293b;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  color: #64748b;
}

:deep(.el-form-item__label) {
  color: #64748b;
}

.login-container.dark-mode :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
}

.login-container.dark-mode :deep(.el-dialog__title) {
  color: #f1f5f9;
}

.login-container.dark-mode :deep(.el-dialog__body) {
  color: #94a3b8;
}

.login-container.dark-mode :deep(.el-form-item__label) {
  color: #94a3b8;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .login-box {
    width: 92%;
    max-width: 500px;
    min-height: auto;
    flex-direction: column;
  }

  .login-left {
    display: none;
  }

  .login-right {
    width: 100%;
    padding: 40px 30px;
  }

  .mode-switcher {
    position: relative;
    top: 0;
    right: 0;
    margin-bottom: 30px;
    display: flex;
    justify-content: center;
  }

  .login-box.is-register {
    min-height: auto;
  }
}

@media (max-width: 480px) {
  .login-right {
    padding: 30px 20px;
  }

  .form-header h2 {
    font-size: 26px;
  }
}
</style>
