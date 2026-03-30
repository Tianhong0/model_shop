<template>
	<view class="login-page">
		<!-- Decorative background orbs -->
		<view class="bg-orb bg-orb--top"></view>
		<view class="bg-orb bg-orb--bottom"></view>

		<!-- Header area -->
		<view class="login-header">
			<view class="logo-wrapper">
				<image src="../../static/logo.png" class="logo" mode="aspectFill"></image>
			</view>
			<text class="header-subtitle">您的个性化3D打印定制专家</text>
		</view>

		<!-- Login form card -->
		<view class="form-card">
			<view class="input-group">
				<view class="input-row">
					<uni-icons type="person" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="username" placeholder="请输入登录账号或邮箱" />
				</view>
				<view class="input-row">
					<uni-icons type="lock" size="20" color="#8a9aaa"></uni-icons>
					<input type="password" v-model="password" placeholder="请输入密码" />
				</view>
			</view>

			<view class="options-row">
				<view class="options-left">
					<view class="switch-item">
						<switch :checked="rememberPassword" color="#00bfff" @change="onRememberChange" />
						<text>记住密码</text>
					</view>
					<view class="switch-item">
						<switch :checked="autoLogin" color="#00bfff" @change="onAutoLoginChange" />
						<text>自动登录</text>
					</view>
				</view>
				<text class="forgot-link" @click="openForgotPanel">忘记密码?</text>
			</view>

			<button class="btn-primary" @click="handleLogin">登录</button>

			<view class="register-link">
				<text>没有账号? </text>
				<text class="link" @click="goRegister">立即注册</text>
			</view>
		</view>

		<!-- Forgot password overlay -->
		<view class="overlay-mask" v-if="forgotVisible" @click="closeForgotPanel"></view>
		<view class="forgot-panel" v-if="forgotVisible">
			<view class="forgot-panel__header">
				<text class="forgot-panel__title">邮箱找回密码</text>
			</view>
			<view class="forgot-panel__body">
				<view class="panel-field">
					<text class="panel-field__label">账号</text>
					<input class="panel-field__input" type="text" v-model="forgotForm.userName" placeholder="请输入登录账号或邮箱" />
				</view>
				<view class="panel-field">
					<text class="panel-field__label">邮箱</text>
					<input class="panel-field__input" type="text" v-model="forgotForm.email" placeholder="请输入绑定邮箱" />
				</view>
				<view class="panel-field panel-field--code">
					<text class="panel-field__label">验证码</text>
					<input class="panel-field__input" type="text" v-model="forgotForm.emailCode" maxlength="6" placeholder="请输入邮箱验证码" />
					<button class="code-btn" :disabled="forgotCodeSending || forgotCountDown > 0" @click="sendForgotCode">
						{{ forgotCountDown > 0 ? `${forgotCountDown}s` : '发送验证码' }}
					</button>
				</view>
				<view class="panel-field">
					<text class="panel-field__label">新密码</text>
					<input class="panel-field__input" type="password" v-model="forgotForm.newPassword" placeholder="8-20位，含大小写字母和数字" />
				</view>
				<view class="panel-field">
					<text class="panel-field__label">确认</text>
					<input class="panel-field__input" type="password" v-model="forgotForm.confirmNewPassword" placeholder="请再次输入新密码" />
				</view>
			</view>
			<view class="forgot-panel__actions">
				<button class="btn-cancel" @click="closeForgotPanel">取消</button>
				<button class="btn-confirm" @click="submitForgotReset">重置密码</button>
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
/* ============================================
   3D Print Shop – Login
   Design System: Airy / Glass / Capsule
   ============================================ */

/* --- Tokens --- */
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);

$bg: #f8f8f8;
$card: #ffffff;
$text-1: #1a2030;
$text-2: #5a6a7a;
$text-3: #8a9aaa;
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$radius-card: 32rpx;
$radius-pill: 999rpx;

/* --- Animations --- */
@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to   { opacity: 1; transform: translateY(0); }
}
@keyframes jellyPop {
	0%   { transform: scale(0.7); opacity: 0; }
	60%  { transform: scale(1.08); }
	100% { transform: scale(1); opacity: 1; }
}
@keyframes floatOrb {
	0%, 100% { transform: translate(0, 0) scale(1); }
	50%      { transform: translate(10rpx, -18rpx) scale(1.06); }
}

/* --- Page --- */
.login-page {
	min-height: 100vh;
	background-color: $bg;
	padding: 0 48rpx;
	padding-bottom: env(safe-area-inset-bottom);
	position: relative;
	overflow: hidden;
}

/* Decorative orbs */
.bg-orb {
	position: fixed;
	border-radius: 50%;
	pointer-events: none;
	z-index: 0;
	animation: floatOrb 8s ease-in-out infinite;

	&--top {
		width: 520rpx;
		height: 520rpx;
		top: -160rpx;
		right: -120rpx;
		background: radial-gradient(circle, rgba(0, 191, 255, 0.08) 0%, transparent 70%);
	}
	&--bottom {
		width: 400rpx;
		height: 400rpx;
		bottom: -100rpx;
		left: -100rpx;
		background: radial-gradient(circle, rgba(92, 225, 255, 0.06) 0%, transparent 70%);
		animation-delay: 3s;
	}
}

/* --- Header --- */
.login-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 120rpx;
	position: relative;
	z-index: 1;
	animation: fadeInUp 0.6s ease-out both;

	.logo-wrapper {
		width: 160rpx;
		height: 160rpx;
		border-radius: 40rpx;
		overflow: hidden;
		box-shadow: 0 12rpx 36rpx rgba(0, 191, 255, 0.18);
		animation: jellyPop 0.7s ease-out both;
	}

	.logo {
		width: 160rpx;
		height: 160rpx;
		display: block;
	}

	.header-title {
		font-size: 36rpx;
		font-weight: 700;
		color: $text-1;
		margin-top: 36rpx;
	}

	.header-subtitle {
		font-size: 24rpx;
		color: $text-3;
		margin-top: 12rpx;
	}
}

/* --- Form Card --- */
.form-card {
	margin-top: 72rpx;
	background: $card;
	border-radius: $radius-card;
	box-shadow: $shadow-card;
	padding: 40rpx 36rpx;
	position: relative;
	z-index: 1;
	animation: fadeInUp 0.65s ease-out 0.15s both;
}

.input-group {
	display: flex;
	flex-direction: column;
	gap: 28rpx;
}

.input-row {
	display: flex;
	align-items: center;
	height: 100rpx;
	background: $bg;
	border-radius: 24rpx;
	padding: 0 32rpx;
	transition: box-shadow 0.3s ease;

	&:focus-within {
		box-shadow: 0 0 0 3rpx rgba(0, 191, 255, 0.18);
	}

	input {
		flex: 1;
		margin-left: 20rpx;
		font-size: 28rpx;
		color: $text-1;
	}
}

/* --- Options row --- */
.options-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 28rpx;
}

.options-left {
	display: flex;
	gap: 16rpx;
}

.switch-item {
	display: flex;
	align-items: center;
	font-size: 24rpx;
	color: $text-2;

	switch {
		transform: scale(0.75);
		margin-right: 4rpx;
	}
}

.forgot-link {
	font-size: 24rpx;
	color: $deep;
	font-weight: 600;

	&:active {
		opacity: 0.7;
	}
}

/* --- Primary button --- */
.btn-primary {
	margin-top: 48rpx;
	width: 100%;
	height: 100rpx;
	background: $gradient;
	color: #ffffff;
	border-radius: $radius-pill;
	font-size: 32rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 12rpx 32rpx rgba(0, 191, 255, 0.28);
	letter-spacing: 4rpx;

	&:active {
		transform: scale(0.96);
	}
}

/* --- Register link --- */
.register-link {
	margin-top: 36rpx;
	text-align: center;
	font-size: 28rpx;
	color: $text-2;

	.link {
		color: $deep;
		font-weight: 600;
		margin-left: 8rpx;

		&:active {
			opacity: 0.7;
		}
	}
}

/* ========== Forgot-password overlay ========== */
.overlay-mask {
	position: fixed;
	inset: 0;
	background: rgba(0, 0, 0, 0.35);
	z-index: 1000;
	backdrop-filter: blur(6px);
}

.forgot-panel {
	position: fixed;
	left: 32rpx;
	right: 32rpx;
	bottom: 32rpx;
	background: $card;
	border-radius: $radius-card;
	box-shadow: 0 -8rpx 60rpx rgba(0, 0, 0, 0.1);
	padding: 0;
	z-index: 1001;
	animation: fadeInUp 0.35s ease-out both;
	overflow: hidden;

	&__header {
		padding: 36rpx 36rpx 0 36rpx;
	}

	&__title {
		font-size: 30rpx;
		font-weight: 700;
		color: $text-1;
	}

	&__body {
		padding: 20rpx 36rpx 0 36rpx;
	}

	&__actions {
		display: flex;
		gap: 24rpx;
		padding: 28rpx 36rpx 36rpx 36rpx;
	}
}

/* Panel field rows */
.panel-field {
	display: flex;
	align-items: center;
	min-height: 84rpx;
	padding: 8rpx 0;

	& + .panel-field {
		box-shadow: inset 0 1rpx 0 0 rgba(0, 0, 0, 0.04);
	}

	&__label {
		width: 100rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: $text-2;
		flex-shrink: 0;
	}

	&__input {
		flex: 1;
		font-size: 26rpx;
		color: $text-1;
	}
}

.panel-field--code {
	.code-btn {
		width: 190rpx;
		height: 60rpx;
		line-height: 60rpx;
		font-size: 22rpx;
		border-radius: $radius-pill;
		background: $gradient;
		color: #ffffff;
		padding: 0;
		margin: 0;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.22);
		flex-shrink: 0;

		&:active {
			transform: scale(0.96);
		}
	}
	.code-btn[disabled] {
		background: $bg;
		color: $text-3;
		box-shadow: none;
	}
}

/* Panel action buttons */
.btn-cancel {
	flex: 1;
	height: 84rpx;
	border-radius: $radius-pill;
	font-size: 28rpx;
	font-weight: 600;
	background: $bg;
	color: $text-2;

	&:active {
		transform: scale(0.96);
	}
}

.btn-confirm {
	flex: 1;
	height: 84rpx;
	border-radius: $radius-pill;
	font-size: 28rpx;
	font-weight: 600;
	background: $gradient;
	color: #ffffff;
	box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);

	&:active {
		transform: scale(0.96);
	}
}
</style>
