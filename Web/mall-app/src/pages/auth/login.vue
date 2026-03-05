<template>
	<view class="login-container">
		<view class="login-header">
			<image src="https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200" class="logo"></image>
			<text class="title">3D打印小物件定制商城</text>
			<text class="subtitle">您的个性化3D打印定制专家</text>
		</view>

		<view class="form-box">
			<view class="input-item">
				<uni-icons type="person" size="20" color="#94a3b8"></uni-icons>
				<input type="text" v-model="username" placeholder="请输入登录账号或邮箱" />
			</view>
			<view class="input-item">
				<uni-icons type="lock" size="20" color="#94a3b8"></uni-icons>
				<input type="password" v-model="password" placeholder="请输入密码" />
			</view>
			
			<view class="options">
				<view class="left-options">
					<view class="opt-item">
						<switch :checked="rememberPassword" color="#4f46e5" @change="onRememberChange" />
						<text>记住密码</text>
					</view>
					<view class="opt-item">
						<switch :checked="autoLogin" color="#4f46e5" @change="onAutoLoginChange" />
						<text>自动登录</text>
					</view>
				</view>
				<text class="forgot" @click="openForgotPanel">忘记密码?</text>
			</view>

			<button class="login-btn" @click="handleLogin">登录</button>
			
			<view class="register-link">
				<text>没有账号? </text>
				<text class="link" @click="goRegister">立即注册</text>
			</view>
		</view>

		<view class="editor-mask" v-if="forgotVisible" @click="closeForgotPanel"></view>
		<view class="editor-panel" v-if="forgotVisible">
			<view class="editor-title">邮箱找回密码</view>
			<view class="form-item">
				<text class="label">账号</text>
				<input class="input" type="text" v-model="forgotForm.userName" placeholder="请输入登录账号或邮箱" />
			</view>
			<view class="form-item">
				<text class="label">邮箱</text>
				<input class="input" type="text" v-model="forgotForm.email" placeholder="请输入绑定邮箱" />
			</view>
			<view class="form-item code-row">
				<text class="label">验证码</text>
				<input class="input" type="text" v-model="forgotForm.emailCode" maxlength="6" placeholder="请输入邮箱验证码" />
				<button class="mini-btn" :disabled="forgotCodeSending || forgotCountDown > 0" @click="sendForgotCode">
					{{ forgotCountDown > 0 ? `${forgotCountDown}s` : '发送验证码' }}
				</button>
			</view>
			<view class="form-item">
				<text class="label">新密码</text>
				<input class="input" type="password" v-model="forgotForm.newPassword" placeholder="8-20位，含大小写字母和数字" />
			</view>
			<view class="form-item">
				<text class="label">确认</text>
				<input class="input" type="password" v-model="forgotForm.confirmNewPassword" placeholder="请再次输入新密码" />
			</view>
			<view class="editor-actions">
				<button class="action-btn cancel" @click="closeForgotPanel">取消</button>
				<button class="action-btn save" @click="submitForgotReset">重置密码</button>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { loginApi, sendForgotPasswordEmailCodeApi, resetPasswordByEmailApi } from '../../api/auth'
import { getDesignerListApi } from '../../api/user'
import { hasValidLogin } from '../../utils/auth'
import { normalizeUserRole } from '../../utils/role'

const username = ref('')
const password = ref('')
const rememberPassword = ref(false)
const autoLogin = ref(false)
const loggingIn = ref(false)
const forgotVisible = ref(false)
const forgotCodeSending = ref(false)
const forgotCountDown = ref(0)
let forgotTimer = null

const forgotForm = ref({
	userName: '',
	email: '',
	emailCode: '',
	newPassword: '',
	confirmNewPassword: ''
})

const goHome = () => {
	uni.switchTab({
		url: '/pages/index/index',
		fail: () => {
			uni.reLaunch({ url: '/pages/index/index' })
		}
	})
}

onMounted(() => {
	const remembered = uni.getStorageSync('remember_password') === true
	const savedUsername = String(uni.getStorageSync('saved_username') || '')
	const savedPassword = String(uni.getStorageSync('saved_password') || '')
	const auto = uni.getStorageSync('auto_login') === true

	rememberPassword.value = remembered
	autoLogin.value = auto
	if (remembered) {
		username.value = savedUsername
		password.value = savedPassword
	}

	if (auto && hasValidLogin()) {
		goHome()
		return
	}

	if (auto && remembered && savedUsername && savedPassword) {
		handleLogin({ silent: true })
	}
})

const goRegister = () => {
	uni.navigateTo({ url: '/pages/auth/register' })
}

const onRememberChange = (e) => {
	rememberPassword.value = !!e.detail.value
	if (!rememberPassword.value) {
		autoLogin.value = false
	}
}

const onAutoLoginChange = (e) => {
	autoLogin.value = !!e.detail.value
	if (autoLogin.value) {
		rememberPassword.value = true
	}
}

const openForgotPanel = () => {
	forgotForm.value = {
		userName: String(username.value || '').trim(),
		email: '',
		emailCode: '',
		newPassword: '',
		confirmNewPassword: ''
	}
	forgotVisible.value = true
}

const closeForgotPanel = () => {
	forgotVisible.value = false
}

const startForgotCountDown = () => {
	forgotCountDown.value = 60
	if (forgotTimer) clearInterval(forgotTimer)
	forgotTimer = setInterval(() => {
		forgotCountDown.value -= 1
		if (forgotCountDown.value <= 0) {
			clearInterval(forgotTimer)
			forgotTimer = null
		}
	}, 1000)
}

const sendForgotCode = async () => {
	const userName = String(forgotForm.value.userName || '').trim()
	const email = String(forgotForm.value.email || '').trim().toLowerCase()
	if (!userName) {
		uni.showToast({ title: '请输入登录账号或邮箱', icon: 'none' })
		return
	}
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		uni.showToast({ title: '请输入正确邮箱', icon: 'none' })
		return
	}
	if (forgotCodeSending.value || forgotCountDown.value > 0) return

	forgotCodeSending.value = true
	uni.showLoading({ title: '发送中...' })
	try {
		await sendForgotPasswordEmailCodeApi({ userName, email })
		forgotForm.value.email = email
		uni.hideLoading()
		uni.showToast({ title: '验证码已发送', icon: 'none' })
		startForgotCountDown()
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '发送失败', icon: 'none' })
	} finally {
		forgotCodeSending.value = false
	}
}

const submitForgotReset = async () => {
	const payload = {
		userName: String(forgotForm.value.userName || '').trim(),
		email: String(forgotForm.value.email || '').trim().toLowerCase(),
		emailCode: String(forgotForm.value.emailCode || '').trim(),
		newPassword: String(forgotForm.value.newPassword || '').trim(),
		confirmNewPassword: String(forgotForm.value.confirmNewPassword || '').trim()
	}
	if (!payload.userName || !payload.email || !payload.emailCode || !payload.newPassword || !payload.confirmNewPassword) {
		uni.showToast({ title: '请填写完整信息', icon: 'none' })
		return
	}
	if (!/^\d{6}$/.test(payload.emailCode)) {
		uni.showToast({ title: '请输入6位验证码', icon: 'none' })
		return
	}
	if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/.test(payload.newPassword)) {
		uni.showToast({ title: '新密码需含大小写字母和数字', icon: 'none' })
		return
	}
	if (payload.newPassword !== payload.confirmNewPassword) {
		uni.showToast({ title: '两次密码不一致', icon: 'none' })
		return
	}

	uni.showLoading({ title: '提交中...' })
	try {
		await resetPasswordByEmailApi(payload)
		uni.hideLoading()
		uni.showToast({ title: '重置成功，请重新登录', icon: 'none' })
		forgotVisible.value = false
		password.value = ''
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '重置失败', icon: 'none' })
	}
}

const persistLoginOptions = () => {
	uni.setStorageSync('remember_password', rememberPassword.value)
	uni.setStorageSync('auto_login', autoLogin.value)
	if (rememberPassword.value) {
		uni.setStorageSync('saved_username', String(username.value || '').trim())
		uni.setStorageSync('saved_password', String(password.value || ''))
	} else {
		uni.removeStorageSync('saved_username')
		uni.removeStorageSync('saved_password')
	}
}

const resolveUserRole = async (loginData) => {
	const responseRole = loginData?.roles || loginData?.role || loginData?.userRole || loginData?.roleName
	let normalizedRole = normalizeUserRole(responseRole || uni.getStorageSync('user_role'))
	if (normalizedRole === 'designer') {
		return normalizedRole
	}
	const loginUserId = String(loginData?.userId || '')
	if (!loginUserId) {
		return normalizedRole
	}
	try {
		const designers = await getDesignerListApi()
		const isDesigner = (designers || []).some(item => String(item?.id || '') === loginUserId)
		return isDesigner ? 'designer' : normalizedRole
	} catch (_) {
		return normalizedRole
	}
}

const handleLogin = async ({ silent = false } = {}) => {
	if (loggingIn.value) return
	if (!username.value || !password.value) {
		if (!silent) {
			uni.showToast({ title: '请输入账号或邮箱和密码', icon: 'none' })
		}
		return
	}

	loggingIn.value = true
	if (!silent) {
		uni.showLoading({ title: '登录中...' })
	}
	try {
		const data = await loginApi({
			userName: username.value.trim(),
			password: password.value
		})
		const role = await resolveUserRole(data)

		uni.setStorageSync('token', data?.token || '')
		uni.setStorageSync('token_expire_time', data?.tokenExpireTime || 0)
		uni.setStorageSync('user_role', role)
		uni.setStorageSync('user_profile', {
			id: data?.userId,
			nickname: data?.nickname || data?.userName,
			avatar: data?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(data?.userName || username.value)}`,
			userName: data?.userName,
			email: data?.email || '',
			role
		})
		persistLoginOptions()

		if (!silent) {
			uni.hideLoading()
			uni.showToast({ title: '登录成功' })
		}
		goHome()
	} catch (error) {
		if (!silent) {
			uni.hideLoading()
			uni.showToast({
				title: error.message || '登录失败，请稍后重试',
				icon: 'none'
			})
		}
		if (silent) {
			autoLogin.value = false
			uni.setStorageSync('auto_login', false)
		}
	} finally {
		if (!silent) {
			uni.hideLoading()
		}
		loggingIn.value = false
	}
}

onUnmounted(() => {
	if (forgotTimer) {
		clearInterval(forgotTimer)
		forgotTimer = null
	}
})
</script>

<style scoped lang="scss">
.login-container {
	padding: 60rpx;
	background-color: #ffffff;
	min-height: 100vh;
}

.login-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 100rpx;
	.logo {
		width: 160rpx;
		height: 160rpx;
		border-radius: 40rpx;
		box-shadow: 0 10rpx 20rpx rgba(79, 70, 229, 0.2);
	}
	.title {
		font-size: 44rpx;
		font-weight: 800;
		color: #1e293b;
		margin-top: 40rpx;
	}
	.subtitle {
		font-size: 24rpx;
		color: #94a3b8;
		margin-top: 10rpx;
	}
}

.form-box {
	margin-top: 100rpx;
	.input-item {
		height: 100rpx;
		background-color: #f8fafc;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		padding: 0 30rpx;
		margin-bottom: 30rpx;
		input {
			flex: 1;
			margin-left: 20rpx;
			font-size: 28rpx;
		}
	}
}

.options {
	display: flex;
	justify-content: space-between;
	align-items: center;
	.left-options {
		display: flex;
		gap: 20rpx;
	}
	.opt-item {
		display: flex;
		align-items: center;
		font-size: 24rpx;
		color: #64748b;
		switch {
			transform: scale(0.8);
			margin-right: 6rpx;
		}
	}
	.forgot { font-size: 24rpx; color: #4f46e5; }
}

.editor-mask {
	position: fixed;
	inset: 0;
	background-color: rgba(15, 23, 42, 0.45);
	z-index: 1000;
}

.editor-panel {
	position: fixed;
	left: 24rpx;
	right: 24rpx;
	bottom: 24rpx;
	background-color: #ffffff;
	border-radius: 20rpx;
	padding: 24rpx;
	z-index: 1001;
	.editor-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1e293b;
		margin-bottom: 14rpx;
	}
	.form-item {
		display: flex;
		align-items: center;
		min-height: 76rpx;
		border-bottom: 2rpx solid #f1f5f9;
		.label {
			width: 100rpx;
			font-size: 26rpx;
			color: #64748b;
		}
		.input {
			flex: 1;
			font-size: 26rpx;
			color: #1e293b;
		}
	}
	.code-row {
		.mini-btn {
			width: 180rpx;
			height: 60rpx;
			line-height: 60rpx;
			font-size: 22rpx;
			border-radius: 30rpx;
			background-color: #4f46e5;
			color: #ffffff;
			padding: 0;
			margin: 0;
		}
		.mini-btn[disabled] {
			background-color: #a5b4fc;
		}
	}
	.editor-actions {
		display: flex;
		gap: 20rpx;
		margin-top: 20rpx;
		.action-btn {
			flex: 1;
			height: 76rpx;
			border-radius: 38rpx;
			font-size: 28rpx;
			&.cancel {
				background-color: #f1f5f9;
				color: #475569;
			}
			&.save {
				background-color: #4f46e5;
				color: #ffffff;
			}
		}
	}
}

.login-btn {
	margin-top: 60rpx;
	height: 100rpx;
	background-color: #4f46e5;
	color: #ffffff;
	border-radius: 50rpx;
	font-size: 32rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 10rpx 20rpx rgba(79, 70, 229, 0.3);
}

.register-link {
	margin-top: 40rpx;
	text-align: center;
	font-size: 26rpx;
	color: #64748b;
	.link { color: #4f46e5; font-weight: 600; margin-left: 10rpx; }
}

.third-party {
	margin-top: 150rpx;
	.line-wrap {
		display: flex;
		align-items: center;
		.line { flex: 1; height: 2rpx; background-color: #f1f5f9; }
		text { font-size: 22rpx; color: #cbd5e1; margin: 0 30rpx; }
	}
	.icons {
		margin-top: 40rpx;
		display: flex;
		justify-content: center;
		gap: 60rpx;
	}
}
</style>
