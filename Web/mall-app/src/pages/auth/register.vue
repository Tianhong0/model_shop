<template>
	<view class="register-page">
		<!-- Decorative background orbs -->
		<view class="bg-orb bg-orb--top"></view>
		<view class="bg-orb bg-orb--bottom"></view>

		<!-- Header -->
		<view class="register-header">
			<view class="header-icon">
				<text class="header-icon__emoji">&#x1F680;</text>
			</view>
			<text class="header-title">创建账号</text>
			<text class="header-subtitle">注册后即可体验3D打印定制服务</text>
		</view>

		<!-- Form card -->
		<view class="form-card">
			<view class="input-group">
				<view class="input-row">
					<uni-icons type="person" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="form.userName" placeholder="登录账户（4-20位字母数字下划线）" />
				</view>
				<view class="input-row">
					<uni-icons type="contact" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="form.nickname" placeholder="昵称" />
				</view>
				<view class="input-row">
					<uni-icons type="phone" size="20" color="#8a9aaa"></uni-icons>
					<input type="number" v-model="form.mobile" placeholder="手机号（可选）" maxlength="11" />
				</view>
				<view class="input-row">
					<uni-icons type="email" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="form.email" placeholder="邮箱（必填）" />
				</view>
				<view class="input-row input-row--code">
					<uni-icons type="compose" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="form.emailCode" placeholder="邮箱验证码" maxlength="6" />
					<button class="code-btn" :disabled="codeSending || codeCountDown > 0" @click="sendEmailCode">
						{{ codeCountDown > 0 ? `${codeCountDown}s后重发` : '发送验证码' }}
					</button>
				</view>
				<view class="input-row">
					<uni-icons type="locked" size="20" color="#8a9aaa"></uni-icons>
					<input type="password" v-model="form.password" placeholder="密码（8-20位，含大小写字母和数字）" />
				</view>
				<view class="input-row">
					<uni-icons type="locked" size="20" color="#8a9aaa"></uni-icons>
					<input type="password" v-model="form.confirmPassword" placeholder="确认密码" />
				</view>
				<view class="input-row">
					<uni-icons type="gift" size="20" color="#8a9aaa"></uni-icons>
					<input type="text" v-model="form.inviteCode" placeholder="邀请码（可选，填写后双方可获积分奖励）" />
				</view>
			</view>

			<button class="btn-primary" @click="handleRegister">注册并登录</button>

			<view class="login-link">
				<text>已有账号? </text>
				<text class="link" @click="goLogin">返回登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginApi, registerApi, sendRegisterEmailCodeApi } from '../../api/auth'

const form = reactive({
	userName: '',
	nickname: '',
	mobile: '',
	email: '',
	emailCode: '',
	password: '',
	confirmPassword: '',
	inviteCode: ''
})

// 从URL参数中读取邀请码
onLoad((options) => {
	if (options?.inviteCode) {
		form.inviteCode = options.inviteCode
	}
})

const codeCountDown = ref(0)
const codeSending = ref(false)
let codeTimer = null

const goLogin = () => {
	uni.navigateBack()
}

const validate = () => {
	if (!form.userName || !form.nickname || !form.email || !form.emailCode || !form.password || !form.confirmPassword) {
		uni.showToast({ title: '请填写完整注册信息', icon: 'none' })
		return false
	}

	if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.userName)) {
		uni.showToast({ title: '登录账户格式不正确', icon: 'none' })
		return false
	}

	if (form.mobile && !/^1[3-9]\d{9}$/.test(form.mobile)) {
		uni.showToast({ title: '手机号格式不正确', icon: 'none' })
		return false
	}

	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
		uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
		return false
	}

	if (!/^\d{6}$/.test(form.emailCode)) {
		uni.showToast({ title: '请输入6位邮箱验证码', icon: 'none' })
		return false
	}

	if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/.test(form.password)) {
		uni.showToast({ title: '密码需含大小写字母和数字', icon: 'none' })
		return false
	}

	if (form.password !== form.confirmPassword) {
		uni.showToast({ title: '两次密码不一致', icon: 'none' })
		return false
	}

	return true
}

const startCountDown = () => {
	codeCountDown.value = 60
	if (codeTimer) clearInterval(codeTimer)
	codeTimer = setInterval(() => {
		codeCountDown.value -= 1
		if (codeCountDown.value <= 0) {
			clearInterval(codeTimer)
			codeTimer = null
		}
	}, 1000)
}

const sendEmailCode = async () => {
	const email = String(form.email || '').trim().toLowerCase()
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		uni.showToast({ title: '请先输入正确邮箱', icon: 'none' })
		return
	}
	if (codeSending.value || codeCountDown.value > 0) {
		return
	}

	codeSending.value = true
	try {
		await sendRegisterEmailCodeApi({ email })
		form.email = email
		uni.showToast({ title: '验证码已发送', icon: 'none' })
		startCountDown()
	} catch (error) {
		uni.showToast({ title: error.message || '发送失败', icon: 'none' })
	} finally {
		codeSending.value = false
	}
}

const handleRegister = async () => {
	if (!validate()) return

	uni.showLoading({ title: '注册中...' })
	try {
		const registerData = await registerApi({
			userName: form.userName.trim(),
			password: form.password,
			confirmPassword: form.confirmPassword,
			nickname: form.nickname.trim(),
			mobile: form.mobile || null,
			email: String(form.email || '').trim().toLowerCase(),
			emailCode: String(form.emailCode || '').trim(),
				inviteCode: form.inviteCode ? form.inviteCode.trim().toUpperCase() : null
		})

		let data = registerData
		if (!data?.token) {
			data = await loginApi({
				userName: form.userName.trim(),
				password: form.password
			})
		}

		uni.setStorageSync('token', data?.token || '')
		uni.setStorageSync('token_expire_time', data?.tokenExpireTime || 0)
		uni.setStorageSync('user_role', 'user')
		uni.setStorageSync('user_profile', {
			id: data?.userId,
			nickname: data?.nickname || data?.userName,
			avatar: data?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(data?.userName || form.userName)}`,
			userName: data?.userName,
			email: data?.email || String(form.email || '').trim().toLowerCase(),
			role: 'user'
		})

		uni.hideLoading()
		uni.showToast({ title: '注册成功' })
		setTimeout(() => {
			uni.switchTab({
				url: '/pages/index/index',
				fail: () => {
					uni.reLaunch({ url: '/pages/index/index' })
				}
			})
		}, 300)
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '注册失败', icon: 'none' })
	}
}

onUnmounted(() => {
	if (codeTimer) {
		clearInterval(codeTimer)
		codeTimer = null
	}
})
</script>

<style scoped lang="scss">
/* ============================================
   3D Print Shop – Register
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
.register-page {
	min-height: 100vh;
	background-color: $bg;
	padding: 0 48rpx;
	padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
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
		width: 480rpx;
		height: 480rpx;
		top: -140rpx;
		left: -100rpx;
		background: radial-gradient(circle, rgba(0, 191, 255, 0.07) 0%, transparent 70%);
	}
	&--bottom {
		width: 380rpx;
		height: 380rpx;
		bottom: -80rpx;
		right: -80rpx;
		background: radial-gradient(circle, rgba(92, 225, 255, 0.05) 0%, transparent 70%);
		animation-delay: 3s;
	}
}

/* --- Header --- */
.register-header {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	padding-top: 100rpx;
	position: relative;
	z-index: 1;
	animation: fadeInUp 0.6s ease-out both;

	.header-icon {
		width: 96rpx;
		height: 96rpx;
		border-radius: 28rpx;
		background: $gradient;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 10rpx 28rpx rgba(0, 191, 255, 0.22);
		animation: jellyPop 0.7s ease-out both;
		margin-bottom: 28rpx;

		&__emoji {
			font-size: 44rpx;
		}
	}

	.header-title {
		font-size: 36rpx;
		font-weight: 700;
		color: $text-1;
	}

	.header-subtitle {
		font-size: 24rpx;
		color: $text-3;
		margin-top: 12rpx;
	}
}

/* --- Form Card --- */
.form-card {
	margin-top: 48rpx;
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
	gap: 24rpx;
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

/* Code row */
.input-row--code {
	.code-btn {
		width: 200rpx;
		height: 64rpx;
		line-height: 64rpx;
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

/* --- Primary button --- */
.btn-primary {
	margin-top: 40rpx;
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

/* --- Login link --- */
.login-link {
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
</style>
