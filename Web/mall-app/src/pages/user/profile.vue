<template>
  <view class="profile-container">
    <!-- Hero Header -->
    <view class="hero-section">
      <view class="hero-pattern"></view>
      <view class="avatar-section">
        <view class="avatar-ring" @click="changeAvatar">
          <image :src="displayAvatar" class="avatar" mode="aspectFill"></image>
          <view class="avatar-edit">
            <uni-icons type="camera" size="18" color="#fff"></uni-icons>
          </view>
        </view>
        <text class="avatar-tip">点击修改头像</text>
      </view>
    </view>

    <!-- Form Section -->
    <view class="form-section">
      <view class="form-card">
        <!-- Nickname -->
        <view class="form-item">
          <view class="item-icon">
            <uni-icons type="person" size="20" color="#00bfff"></uni-icons>
          </view>
          <text class="item-label">昵称</text>
          <input class="item-input" v-model="user.nickname" placeholder="请输入昵称" placeholder-class="input-placeholder" />
        </view>

        <!-- Mobile -->
        <view class="form-item">
          <view class="item-icon">
            <uni-icons type="phone" size="20" color="#00bfff"></uni-icons>
          </view>
          <text class="item-label">手机号</text>
          <input class="item-input" v-model="user.mobile" placeholder="请输入手机号" maxlength="11" type="number" placeholder-class="input-placeholder" />
        </view>

        <!-- Email -->
        <view class="form-item">
          <view class="item-icon">
            <uni-icons type="email" size="20" color="#00bfff"></uni-icons>
          </view>
          <text class="item-label">邮箱</text>
          <input class="item-input" v-model="user.email" placeholder="请输入邮箱" placeholder-class="input-placeholder" />
        </view>

        <!-- Email Code -->
        <view class="form-item code-item">
          <view class="item-icon">
            <uni-icons type="locked" size="20" color="#fbbf24"></uni-icons>
          </view>
          <text class="item-label">验证码</text>
          <input class="item-input code-input" v-model="emailCode" maxlength="6" placeholder="修改邮箱时必填" placeholder-class="input-placeholder" />
          <button class="code-btn" :class="{ disabled: emailCodeSending || emailCodeCountDown > 0 }" :disabled="emailCodeSending || emailCodeCountDown > 0" @click="sendChangeEmailCode">
            {{ emailCodeCountDown > 0 ? `${emailCodeCountDown}s` : '发送验证码' }}
          </button>
        </view>

        <!-- Gender -->
        <view class="form-item" @click="showSexPicker = true">
          <view class="item-icon">
            <uni-icons type="contact" size="20" color="#00bfff"></uni-icons>
          </view>
          <text class="item-label">性别</text>
          <text class="item-value">{{ sexRange[user.sex] || '请选择' }}</text>
          <uni-icons type="right" size="16" color="#8a9aaa"></uni-icons>
        </view>

        <!-- Avatar Hint -->
        <view class="form-item hint-item">
          <view class="item-icon">
            <uni-icons type="image" size="20" color="#8a9aaa"></uni-icons>
          </view>
          <text class="item-label">头像</text>
          <text class="item-hint">点击顶部头像可修改</text>
        </view>
      </view>
    </view>

    <!-- Save Button -->
    <view class="save-section">
      <button class="save-btn" @click="saveProfile">
        <uni-icons type="checkmarkempty" size="20" color="#fff"></uni-icons>
        <text>保存修改</text>
      </button>
    </view>

    <!-- Gender Picker -->
    <view class="picker-overlay" v-if="showSexPicker" @click="showSexPicker = false">
      <view class="picker-container" @click.stop>
        <view class="picker-header">
          <text class="picker-title">选择性别</text>
          <view class="picker-close" @click="showSexPicker = false">
            <uni-icons type="close" size="20" color="#5a6a7a"></uni-icons>
          </view>
        </view>
        <view class="picker-options">
          <view
            class="picker-option"
            v-for="(item, index) in sexRange"
            :key="index"
            :class="{ active: user.sex === index }"
            @click="selectSex(index)"
          >
            <text>{{ item }}</text>
            <view class="option-check" v-if="user.sex === index">
              <uni-icons type="checkmarkempty" size="18" color="#00bfff"></uni-icons>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getUserDetailApi, sendChangeEmailCodeApi, updateUserProfileApi, uploadAvatarApi } from '../../api/user'

const user = ref({
  id: null,
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
  nickname: '',
  sex: 0,
  mobile: '',
  email: ''
})

const avatarPreview = ref('')
const avatarServerUrl = ref('')
const avatarUploading = ref(false)
const originalEmail = ref('')
const emailCode = ref('')
const emailCodeSending = ref(false)
const emailCodeCountDown = ref(0)
const showSexPicker = ref(false)
let emailCodeTimer = null

const displayAvatar = computed(() => avatarPreview.value || user.value.avatar)

const sexRange = ['男', '女', '保密']

onMounted(() => {
  loadUserProfile()
})

const loadUserProfile = async () => {
  const localProfile = uni.getStorageSync('user_profile') || {}
  const userId = localProfile?.id
  if (!userId) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const detail = await getUserDetailApi(userId)
    user.value = {
      id: detail?.id,
      avatar: detail?.avatar || localProfile?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
      nickname: detail?.nickname || detail?.userName || '',
      sex: Number(detail?.sex ?? 0),
      mobile: detail?.mobile || '',
      email: detail?.email || localProfile?.email || ''
    }
    avatarServerUrl.value = user.value.avatar || ''
    avatarPreview.value = ''
    originalEmail.value = String(user.value.email || '').trim().toLowerCase()
    emailCode.value = ''
    uni.setStorageSync('user_profile', {
      ...localProfile,
      id: detail?.id,
      nickname: user.value.nickname,
      avatar: user.value.avatar,
      mobile: user.value.mobile,
      sex: user.value.sex,
      email: user.value.email
    })
  } catch (error) {
    uni.showToast({ title: error.message || '加载资料失败', icon: 'none' })
  }
}

const startEmailCodeCountDown = () => {
  emailCodeCountDown.value = 60
  if (emailCodeTimer) clearInterval(emailCodeTimer)
  emailCodeTimer = setInterval(() => {
    emailCodeCountDown.value -= 1
    if (emailCodeCountDown.value <= 0) {
      clearInterval(emailCodeTimer)
      emailCodeTimer = null
    }
  }, 1000)
}

const sendChangeEmailCode = async () => {
  if (emailCodeSending.value || emailCodeCountDown.value > 0) return
  const email = String(user.value.email || '').trim().toLowerCase()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
    return
  }

  emailCodeSending.value = true
  uni.showLoading({ title: '发送中...' })
  try {
    await sendChangeEmailCodeApi(email)
    user.value.email = email
    uni.hideLoading()
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    startEmailCodeCountDown()
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error.message || '发送失败', icon: 'none' })
  } finally {
    emailCodeSending.value = false
  }
}

const selectSex = (index) => {
  user.value.sex = index
  showSexPicker.value = false
}

const changeAvatar = () => {
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择'],
    success: (actionRes) => {
      const sourceType = actionRes.tapIndex === 0 ? ['camera'] : ['album']
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType,
        success: async (chooseRes) => {
          const filePath = chooseRes?.tempFilePaths?.[0]
          if (!filePath) {
            uni.showToast({ title: '未选择图片', icon: 'none' })
            return
          }
          avatarPreview.value = filePath

          avatarUploading.value = true
          uni.showLoading({ title: '上传头像中...' })
          try {
            const avatarUrl = await uploadAvatarApi(filePath)
            avatarServerUrl.value = String(avatarUrl || '').trim()
            user.value.avatar = avatarServerUrl.value
            const localProfile = uni.getStorageSync('user_profile') || {}
            uni.setStorageSync('user_profile', {
              ...localProfile,
              avatar: avatarServerUrl.value
            })
            uni.hideLoading()
            uni.showToast({ title: '头像已更新', icon: 'success' })
          } catch (error) {
            avatarPreview.value = ''
            uni.hideLoading()
            uni.showToast({ title: error.message || '头像上传失败', icon: 'none' })
          } finally {
            avatarUploading.value = false
          }
        },
        fail: () => {
          uni.showToast({ title: '已取消选择', icon: 'none' })
        }
      })
    }
  })
}

const isMobileValid = (mobile) => {
  if (!mobile) return true
  return /^1[3-9]\d{9}$/.test(String(mobile))
}

const saveProfile = async () => {
  if (avatarUploading.value) {
    uni.showToast({ title: '头像上传中，请稍候', icon: 'none' })
    return
  }
  if (!user.value.nickname || !String(user.value.nickname).trim()) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }
  if (!isMobileValid(user.value.mobile)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' })
    return
  }
  const email = String(user.value.email || '').trim().toLowerCase()
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
    return
  }

  const localProfile = uni.getStorageSync('user_profile') || {}
  const oldEmail = String(originalEmail.value || localProfile?.email || '').trim().toLowerCase()
  if (email !== oldEmail && !/^\d{6}$/.test(String(emailCode.value || '').trim())) {
    uni.showToast({ title: '修改邮箱需要6位验证码', icon: 'none' })
    return
  }

  uni.showLoading({ title: '保存中...' })
  try {
    const avatarToSave = String(
      avatarServerUrl.value
        || user.value.avatar
        || localProfile?.avatar
        || ''
    ).trim()

    if (!avatarToSave) {
      uni.hideLoading()
      uni.showToast({ title: '请先上传头像后再保存', icon: 'none' })
      return
    }

    const payload = {
      nickname: String(user.value.nickname).trim(),
      sex: Number(user.value.sex || 0),
      mobile: user.value.mobile ? String(user.value.mobile).trim() : null,
      email,
      emailCode: String(emailCode.value || '').trim(),
      avatar: avatarToSave
    }

    await updateUserProfileApi(payload)
    user.value.avatar = avatarToSave
    avatarServerUrl.value = avatarToSave
    avatarPreview.value = ''
    uni.setStorageSync('user_profile', {
      ...localProfile,
      nickname: String(user.value.nickname).trim(),
      avatar: avatarToSave,
      mobile: user.value.mobile,
      sex: Number(user.value.sex || 0),
      email
    })
    originalEmail.value = email
    emailCode.value = ''
    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 500)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error.message || '保存失败', icon: 'none' })
  }
}

onUnmounted(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
    emailCodeTimer = null
  }
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.profile-container {
  min-height: 100vh;
  background-color: $bg;
}

.hero-section {
  position: relative;
  padding: 60rpx 24rpx 80rpx;
  background: $gradient;
  border-bottom-left-radius: 40rpx;
  border-bottom-right-radius: 40rpx;
  overflow: hidden;
}

.hero-pattern {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background:
    radial-gradient(circle at 0% 100%, rgba(255,255,255,0.1) 0%, transparent 50%),
    radial-gradient(circle at 100% 0%, rgba(255,255,255,0.08) 0%, transparent 50%);
  pointer-events: none;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 1;
}

.avatar-ring {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  padding: 6rpx;
  background: rgba(255,255,255,0.3);
  border-radius: 50%;
  margin-bottom: 20rpx;
  &:active { transform: scale(0.96); }
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 5rpx solid #ffffff;
  background-color: $bg;
}

.avatar-edit {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient;
  border-radius: 50%;
  border: 4rpx solid #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(0, 191, 255, 0.3);
}

.avatar-tip {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-section {
  padding: 0 28rpx;
  margin-top: -40rpx;
  position: relative;
  z-index: 2;
}

.form-card {
  background: $card;
  border-radius: 24rpx;
  padding: 8rpx 28rpx;
  box-shadow: $shadow;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  & + .form-item { border-top: 1rpx solid rgba(0,0,0,0.04); }
}

.item-icon {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 191, 255, 0.1);
  border-radius: 12rpx;
  margin-right: 16rpx;
}

.item-label {
  width: 140rpx;
  font-size: 28rpx;
  color: $text2;
  font-weight: 500;
}

.item-input {
  flex: 1;
  font-size: 28rpx;
  color: $text1;
  text-align: right;
}

.item-value {
  flex: 1;
  font-size: 28rpx;
  color: $text1;
  text-align: right;
}

.item-hint {
  flex: 1;
  font-size: 26rpx;
  color: $text3;
  text-align: right;
}

.input-placeholder {
  color: $text3;
}

.code-input {
  flex: 1;
}

.code-btn {
  width: 180rpx;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 24rpx;
  font-weight: 500;
  border-radius: 999rpx;
  background: $gradient;
  color: #ffffff;
  padding: 0;
  margin: 0;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(0, 191, 255, 0.25);
  &:active { transform: scale(0.96); }
  &.disabled {
    background: $bg;
    color: $text3;
    box-shadow: none;
  }
}

.hint-item {
  &:active {
    background: transparent;
    margin: 0;
    padding: 28rpx 0;
    border-radius: 0;
  }
}

.save-section {
  padding: 48rpx 28rpx;
}

.save-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  width: 100%;
  height: 96rpx;
  background: $gradient;
  border-radius: 999rpx;
  border: none;
  margin: 0;
  padding: 0;
  box-shadow: 0 8rpx 30rpx rgba(0, 191, 255, 0.3);
  text {
    font-size: 32rpx;
    color: #ffffff;
    font-weight: 600;
  }
  &:active {
    transform: scale(0.96);
  }
}

.picker-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.picker-container {
  width: 100%;
  background: $card;
  border-radius: 32rpx 32rpx 0 0;
  padding: 28rpx;
  padding-bottom: calc(28rpx + env(safe-area-inset-bottom));
}

.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 8rpx;
  margin-bottom: 16rpx;
}

.picker-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $text1;
}

.picker-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg;
  border-radius: 50%;
}

.picker-options {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.picker-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  background: $bg;
  border-radius: 16rpx;
  text {
    font-size: 30rpx;
    color: $text2;
    font-weight: 500;
  }
  &.active {
    background: rgba(0, 191, 255, 0.08);
    text {
      color: $primary;
      font-weight: 600;
    }
  }
  &:active { transform: scale(0.995); }
}

.option-check {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 191, 255, 0.15);
  border-radius: 50%;
}
</style>
