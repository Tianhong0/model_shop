import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import {
  adminLoginApi,
  sendAdminRegisterEmailCodeApi,
  submitAdminRegisterRequestApi,
  sendForgotPasswordEmailCodeApi,
  resetPasswordByEmailApi
} from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: (() => {
      try {
        const raw = localStorage.getItem('user')
        return raw ? JSON.parse(raw) : null
      } catch (_) {
        return null
      }
    })(),
    isAuthenticated: !!localStorage.getItem('token')
  }),

  getters: {
    isLoggedIn: (state) => state.isAuthenticated
  },

  actions: {
    async login(username, password, rememberMe = false) {
      try {
        const response = await adminLoginApi({
          userName: username,
          password: password
        })

        // 响应拦截器已返回 res.data（即 LoginResponse 对象），无需再解构
        this.token = response.token
        this.user = {
          userId: response.userId,
          userName: response.userName,
          nickname: response.nickname,
          avatar: response.avatar
        }
        this.isAuthenticated = true

        // 存储到 localStorage
        localStorage.setItem('token', response.token)
        localStorage.setItem('user', JSON.stringify(this.user))

        // 如果选择记住我，则保存用户名和密码
        if (rememberMe) {
          localStorage.setItem('rememberedUser', username)
          localStorage.setItem('rememberedPassword', password)
        } else {
          localStorage.removeItem('rememberedUser')
          localStorage.removeItem('rememberedPassword')
        }

        ElMessage.success('登录成功')
        return true
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error.response?.data?.message || '登录失败，请检查账号/邮箱和密码')
        return false
      }
    },

    async register(username, password, email, emailCode, nickname = null, confirmPassword = null, mobile = null) {
      try {
        await submitAdminRegisterRequestApi({
          userName: username,
          password: password,
          confirmPassword: confirmPassword || password,
          nickname: nickname || username,
          mobile,
          email,
          emailCode
        })

        ElMessage.success('注册申请已提交，请等待管理员审核通过后登录')
        return true
      } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error(error.response?.data?.message || '注册失败，请检查输入信息')
        return false
      }
    },

    async sendAdminRegisterEmailCode(email) {
      try {
        await sendAdminRegisterEmailCodeApi({ email })
        ElMessage.success('验证码已发送，请查收邮箱')
        return true
      } catch (error) {
        ElMessage.error(error?.message || error.response?.data?.message || '验证码发送失败')
        return false
      }
    },

    async sendForgotPasswordEmailCode(userName, email) {
      try {
        await sendForgotPasswordEmailCodeApi({ userName, email })
        ElMessage.success('验证码已发送，请查收邮箱')
        return true
      } catch (error) {
        ElMessage.error(error?.message || error.response?.data?.message || '验证码发送失败')
        return false
      }
    },

    async resetPasswordByEmail(payload) {
      try {
        await resetPasswordByEmailApi(payload)
        ElMessage.success('密码重置成功，请重新登录')
        return true
      } catch (error) {
        ElMessage.error(error?.message || error.response?.data?.message || '密码重置失败')
        return false
      }
    },

    async logout(clearRemembered = false) {
      try {
        // 调用后端退出登录接口
        await request.post('/api/auth/logout')
        
        // 清除前端状态
        this.token = ''
        this.user = null
        this.isAuthenticated = false

        // 清除 localStorage
        localStorage.removeItem('token')
        localStorage.removeItem('user')

        // 如果需要清除记住的登录信息
        if (clearRemembered) {
          this.clearRememberedCredentials()
        }

        ElMessage.success('退出登录成功')
        return true
      } catch (error) {
        console.error('退出登录失败:', error)
        ElMessage.error(error.response?.data?.message || '退出登录失败')
        
        // 即使接口调用失败，也清除本地状态
        this.token = ''
        this.user = null
        this.isAuthenticated = false
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        
        // 如果需要清除记住的登录信息
        if (clearRemembered) {
          this.clearRememberedCredentials()
        }
        
        return false
      }
    },

    // 获取记住的用户名
    getRememberedUser() {
      return localStorage.getItem('rememberedUser') || ''
    },

    // 获取记住的密码
    getRememberedPassword() {
      return localStorage.getItem('rememberedPassword') || ''
    },

    // 清除记住的用户名和密码
    clearRememberedCredentials() {
      localStorage.removeItem('rememberedUser')
      localStorage.removeItem('rememberedPassword')
    },

    async checkAuth() {
      if (!this.token) return false

      try {
        // 可以添加 token 验证逻辑
        return true
      } catch (error) {
        this.logout()
        return false
      }
    }
  }
})