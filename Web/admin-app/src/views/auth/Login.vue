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
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.05) 0%, transparent 70%);
  top: -400px;
  right: -400px;
}

.login-box {
  width: 900px;
  height: 550px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
  transition: all 0.3s ease;
  z-index: 10;
}

.login-left {
  flex: 1;
  background: #4f46e5;
  background-image: url('https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800');
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-left::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(79, 70, 229, 0.8), rgba(67, 56, 202, 0.9));
}

.brand {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}

.brand h1 {
  font-size: 32px;
  font-weight: 800;
  margin-top: 16px;
  letter-spacing: 1px;
}

.brand p {
  opacity: 0.8;
  margin-top: 8px;
}

.login-right {
  width: 450px;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.form-header p {
  color: #64748b;
  margin-top: 8px;
  font-size: 14px;
}

.login-form {
  margin-top: 32px;
}

:deep(.el-input__wrapper) {
  padding: 8px 16px !important;
  border-radius: 12px !important;
  background-color: #f8fafc !important;
  box-shadow: none !important;
  border: 1px solid #e2e8f0 !important;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #4f46e5 !important;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: #4f46e5 !important;
  border: none !important;
  box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.3);
}

.form-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

/* 注册模式调整高度 */
.is-register {
  height: 650px;
}
</style>
