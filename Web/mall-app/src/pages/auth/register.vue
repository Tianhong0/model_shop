<template>
	<view class="register-container">
		<view class="register-header">
			<text class="title">创建账号</text>
			<text class="subtitle">注册后即可体验3D打印定制服务</text>
		</view>

		<view class="form-box">
			<view class="input-item">
				<uni-icons type="person" size="20" color="#94a3b8"></uni-icons>
				<input type="text" v-model="form.userName" placeholder="登录账户（4-20位字母数字下划线）" />
			</view>
			<view class="input-item">
				<uni-icons type="contact" size="20" color="#94a3b8"></uni-icons>
				<input type="text" v-model="form.nickname" placeholder="昵称" />
			</view>
			<view class="input-item">
				<uni-icons type="phone" size="20" color="#94a3b8"></uni-icons>
				<input type="number" v-model="form.mobile" placeholder="手机号（可选）" maxlength="11" />
			</view>
			<view class="input-item">
				<uni-icons type="email" size="20" color="#94a3b8"></uni-icons>
				<input type="text" v-model="form.email" placeholder="邮箱（必填）" />
			</view>
			<view class="input-item code-item">
				<uni-icons type="compose" size="20" color="#94a3b8"></uni-icons>
				<input type="text" v-model="form.emailCode" placeholder="邮箱验证码" maxlength="6" />
				<button class="code-btn" :disabled="codeSending || codeCountDown > 0" @click="sendEmailCode">
					{{ codeCountDown > 0 ? `${codeCountDown}s后重发` : '发送验证码' }}
				</button>
			</view>
			<view class="input-item">
				<uni-icons type="locked" size="20" color="#94a3b8"></uni-icons>
				<input type="password" v-model="form.password" placeholder="密码（8-20位，含大小写字母和数字）" />
			</view>
			<view class="input-item">
				<uni-icons type="locked" size="20" color="#94a3b8"></uni-icons>
				<input type="password" v-model="form.confirmPassword" placeholder="确认密码" />
			</view>

			<button class="register-btn" @click="handleRegister">注册并登录</button>

			<view class="login-link">
				<text>已有账号? </text>
				<text class="link" @click="goLogin">返回登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { reactive, ref, onUnmounted } from 'vue'
import { loginApi, registerApi, sendRegisterEmailCodeApi } from '../../api/auth'

const form = reactive({
	userName: '',
	nickname: '',
	mobile: '',
	email: '',
	emailCode: '',
	password: '',
	confirmPassword: ''
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
			emailCode: String(form.emailCode || '').trim()
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
.register-container {
	padding: 60rpx;
	background-color: #ffffff;
	min-height: 100vh;
}

.register-header {
	padding-top: 80rpx;
	.title {
		display: block;
		font-size: 46rpx;
		font-weight: 800;
		color: #1e293b;
	}
	.subtitle {
		display: block;
		font-size: 24rpx;
		color: #94a3b8;
		margin-top: 14rpx;
	}
}

.form-box {
	margin-top: 60rpx;
	.input-item {
		height: 100rpx;
		background-color: #f8fafc;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		padding: 0 30rpx;
		margin-bottom: 24rpx;
		input {
			flex: 1;
			margin-left: 20rpx;
			font-size: 28rpx;
		}
	}
	.code-item {
		.code-btn {
			width: 190rpx;
			height: 64rpx;
			line-height: 64rpx;
			font-size: 22rpx;
			border-radius: 32rpx;
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

.register-btn {
	margin-top: 40rpx;
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

.login-link {
	margin-top: 36rpx;
	text-align: center;
	font-size: 26rpx;
	color: #64748b;
	.link { color: #4f46e5; font-weight: 600; margin-left: 10rpx; }
}
</style>
