<template>
	<view class="profile-container">
		<view class="avatar-section">
			<image :src="displayAvatar" class="avatar" @click="changeAvatar"></image>
			<text class="tip">点击修改头像</text>
		</view>

		<view class="form-section container">
			<view class="form-card card">
				<view class="form-item">
					<text class="label">昵称</text>
					<input class="input" v-model="user.nickname" placeholder="请输入昵称" />
				</view>
				<view class="form-item">
					<text class="label">手机号</text>
					<input class="input" v-model="user.mobile" placeholder="请输入手机号" maxlength="11" type="number" />
				</view>
				<view class="form-item">
					<text class="label">邮箱</text>
					<input class="input" v-model="user.email" placeholder="请输入邮箱" />
				</view>
				<view class="form-item code-item">
					<text class="label">验证码</text>
					<input class="input" v-model="emailCode" maxlength="6" placeholder="修改邮箱时必填" />
					<button class="code-btn" :disabled="emailCodeSending || emailCodeCountDown > 0" @click="sendChangeEmailCode">
						{{ emailCodeCountDown > 0 ? `${emailCodeCountDown}s` : '发送验证码' }}
					</button>
				</view>
				<view class="form-item">
					<text class="label">性别</text>
					<picker :range="sexRange" @change="sexChange">
						<view class="picker-val">{{sexRange[user.sex] || '请选择'}}</view>
					</picker>
					<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
				</view>
				<view class="form-item">
					<text class="label">头像</text>
					<view class="picker-val">点击顶部头像可修改</view>
				</view>
			</view>
		</view>

		<view class="save-btn container">
			<button class="btn" @click="saveProfile">保存修改</button>
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

const sexChange = (e) => {
	user.value.sex = Number(e.detail.value)
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
.profile-container {
	min-height: 100vh;
	background-color: #f8fafc;
}

.avatar-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 60rpx 0;
	background-color: #ffffff;
	.avatar {
		width: 160rpx;
		height: 160rpx;
		border-radius: 80rpx;
		margin-bottom: 20rpx;
		border: 4rpx solid #f1f5f9;
	}
	.tip { font-size: 24rpx; color: #94a3b8; }
}

.form-section {
	margin-top: 20rpx;
	.form-card {
		padding: 0 30rpx;
	}
	.form-item {
		display: flex;
		align-items: center;
		min-height: 100rpx;
		border-bottom: 2rpx solid #f1f5f9;
		padding: 20rpx 0;
		&:last-child { border-bottom: none; }
		.label { width: 160rpx; font-size: 28rpx; color: #475569; }
		.input { flex: 1; font-size: 28rpx; color: #1e293b; text-align: right; }
		.picker-val { flex: 1; font-size: 28rpx; color: #1e293b; text-align: right; margin-right: 10rpx; }
		.textarea { flex: 1; font-size: 28rpx; color: #1e293b; text-align: right; min-height: 60rpx; }
	}
	.code-item {
		.code-btn {
			width: 170rpx;
			height: 60rpx;
			line-height: 60rpx;
			font-size: 22rpx;
			border-radius: 30rpx;
			background-color: #4f46e5;
			color: #fff;
			padding: 0;
			margin: 0;
		}
		.code-btn[disabled] {
			background-color: #a5b4fc;
			color: #eef2ff;
		}
	}
}

.save-btn {
	margin-top: 60rpx;
	.btn {
		background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
		color: #ffffff;
		border-radius: 20rpx;
		font-size: 30rpx;
		font-weight: 600;
	}
}
</style>
