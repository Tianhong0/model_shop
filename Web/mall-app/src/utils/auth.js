import { logoutApi } from '../api/auth'

export const hasLogin = () => {
  const token = uni.getStorageSync('token')
  return !!token
}

export const hasValidLogin = () => {
  const token = uni.getStorageSync('token')
  if (!token) return false
  let expireTime = Number(uni.getStorageSync('token_expire_time') || 0)
  if (!expireTime) return true
  if (expireTime > 0 && expireTime < 1e12) {
    expireTime = expireTime * 1000
  }
  return Date.now() < expireTime
}

export const clearLoginStorage = () => {
  uni.removeStorageSync('token')
  uni.removeStorageSync('token_expire_time')
  uni.removeStorageSync('user_profile')
  uni.removeStorageSync('user_role')
}

export const clearRememberedLogin = () => {
  uni.removeStorageSync('remember_password')
  uni.removeStorageSync('saved_username')
  uni.removeStorageSync('saved_password')
  uni.removeStorageSync('auto_login')
}

export const doLogout = async (options = {}) => {
  const { clearRemember = false } = options
  try {
    await logoutApi()
  } catch (e) {
    // 忽略退出接口异常，保证本地态清理
  } finally {
    clearLoginStorage()
    uni.setStorageSync('auto_login', false)
    if (clearRemember) {
      clearRememberedLogin()
    }
  }
}

export const ensureLoginOrRedirect = () => {
  if (!hasValidLogin()) {
    clearLoginStorage()
    uni.reLaunch({ url: '/pages/auth/login' })
    return false
  }
  return true
}
