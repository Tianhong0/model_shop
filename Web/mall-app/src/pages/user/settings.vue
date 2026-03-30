<template>
	<view class="settings-container">
		<view class="group-title">账号安全</view>
		<view class="menu-card">
			<view class="menu-item" @click="goProfile">
				<text class="name">个人资料修改</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="goAddress">
				<text class="name">收货地址管理</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="goProfile">
				<text class="name">手机号绑定</text>
				<text class="val">{{mobileMasked}}</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="openPasswordEditor">
				<text class="name">登录密码修改</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="goDesignerApply">
				<text class="name">申请成为设计者</text>
				<text class="val">{{ designerApplyStatusText }}</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="openDeletionEditor">
				<text class="name">账号注销</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
		</view>

		<view class="group-title">系统设置</view>
		<view class="menu-card">
			<view class="menu-item" @click="clearCache">
				<text class="name">清除缓存</text>
				<text class="val">{{ cacheSize }}</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="checkUpdate">
				<text class="name">检查更新</text>
				<text class="val">{{ versionText }}</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
		</view>

		<view class="group-title">关于</view>
		<view class="menu-card">
			<view class="menu-item" @click="goAbout">
				<text class="name">关于我们</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="goUserAgreement">
				<text class="name">用户协议</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item" @click="goPrivacyPolicy">
				<text class="name">隐私政策</text>
				<uni-icons type="right" size="14" color="#ccc"></uni-icons>
			</view>
			<view class="menu-item">
				<text class="name">当前版本</text>
				<text class="val">v1.0.2</text>
			</view>
		</view>

		<view class="logout-section">
			<button class="logout-btn" @click="handleLogout">退出登录</button>
		</view>

		<view class="editor-mask" v-if="passwordEditorVisible" @click="closePasswordEditor"></view>
		<view class="editor-panel" v-if="passwordEditorVisible">
			<view class="editor-title">修改登录密码</view>
			<view class="deletion-tip">已绑定邮箱将接收验证码，修改成功后需重新登录。</view>
			<view class="form-item">
				<text class="label">原密码</text>
				<input class="input" type="password" v-model="passwordForm.oldPassword" placeholder="请输入原密码" />
			</view>
			<view class="form-item code-row">
				<text class="label">验证码</text>
				<input class="input" type="text" v-model="passwordForm.emailCode" maxlength="6" placeholder="请输入邮箱验证码" />
				<button class="mini-btn" :disabled="codeSending || codeCountDown > 0" @click="sendChangePasswordCode">
					{{ codeCountDown > 0 ? `${codeCountDown}s` : '发送验证码' }}
				</button>
			</view>
			<view class="form-item">
				<text class="label">新密码</text>
				<input class="input" type="password" v-model="passwordForm.newPassword" placeholder="8-20位，含大小写字母和数字" />
			</view>
			<view class="form-item">
				<text class="label">确认密码</text>
				<input class="input" type="password" v-model="passwordForm.confirmNewPassword" placeholder="请再次输入新密码" />
			</view>
			<view class="editor-actions">
				<button class="action-btn cancel" @click="closePasswordEditor">取消</button>
				<button class="action-btn save" @click="submitPasswordChange">确认修改</button>
			</view>
		</view>

		<view class="editor-mask" v-if="deletionEditorVisible" @click="closeDeletionEditor"></view>
		<view class="editor-panel" v-if="deletionEditorVisible">
			<view class="editor-title">账号注销申请</view>
			<view class="deletion-tip">提交后将进入管理员审核流程，请谨慎操作。</view>
			<view class="form-item textarea-item">
				<textarea class="textarea" v-model="deletionReason" maxlength="500" placeholder="请输入注销原因" />
			</view>
			<view class="editor-actions">
				<button class="action-btn cancel" @click="closeDeletionEditor">取消</button>
				<button class="action-btn danger" @click="submitDeletionRequest">提交申请</button>
			</view>
		</view>

	</view>
</template>

<script setup>
import { computed, ref, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
	changePasswordApi,
	getMyDesignerApplyStatusApi,
	requestAccountDeletionApi,
	sendChangePasswordEmailCodeApi
} from '../../api/user'
import { doLogout, ensureLoginOrRedirect } from '../../utils/auth'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const userProfile = ref({})
const passwordEditorVisible = ref(false)
const deletionEditorVisible = ref(false)
const latestDesignerApply = ref(null)
const cacheSize = ref('计算中...')
const versionText = ref('已是最新版本')

const APP_VERSION = 'v1.0.2'

const passwordForm = ref({
	oldPassword: '',
	emailCode: '',
	newPassword: '',
	confirmNewPassword: ''
})
const deletionReason = ref('')
const codeCountDown = ref(0)
const codeSending = ref(false)
let codeTimer = null

const mobileMasked = computed(() => {
	const mobile = String(userProfile.value?.mobile || '')
	if (!mobile) return '未绑定'
	if (mobile.length < 7) return mobile
	return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`
})

onShow(() => {
	if (!ensureLoginOrRedirect()) return
	const role = getStoredUserRole()
	const profile = uni.getStorageSync('user_profile') || {}
	userProfile.value = {
		...profile,
		role
	}
	uni.setStorageSync('user_profile', {
		...profile,
		role
	})
	loadDesignerApplyStatus()
	calculateCacheSize()
})

const designerApplyStatusText = computed(() => {
	if (isDesignerRole(userProfile.value.role)) {
		return '已通过'
	}
	const status = latestDesignerApply.value?.status
	if (!status) return '未申请'
	return renderDesignerStatus(status)
})

const renderDesignerStatus = (status) => {
	const map = {
		pending: '审核中',
		approved: '已通过',
		rejected: '已拒绝'
	}
	return map[String(status || '').toLowerCase()] || '未知状态'
}

const formatDateTime = (value) => {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 19)
}

const loadDesignerApplyStatus = async () => {
	try {
		const data = await getMyDesignerApplyStatusApi()
		latestDesignerApply.value = data?.latestApply || null
		if (data?.alreadyDesigner) {
			const profile = uni.getStorageSync('user_profile') || {}
			uni.setStorageSync('user_role', 'designer')
			uni.setStorageSync('user_profile', {
				...profile,
				role: 'designer'
			})
			userProfile.value = {
				...profile,
				role: 'designer'
			}
		}
	} catch (_) {
		latestDesignerApply.value = null
	}
}

const goProfile = () => {
	uni.navigateTo({ url: '/pages/user/profile' })
}

const goAddress = () => {
	uni.navigateTo({ url: '/pages/user/address' })
}

const openPasswordEditor = () => {
	passwordForm.value = {
		oldPassword: '',
		emailCode: '',
		newPassword: '',
		confirmNewPassword: ''
	}
	passwordEditorVisible.value = true
}

const closePasswordEditor = () => {
	passwordEditorVisible.value = false
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

const sendChangePasswordCode = async () => {
	if (codeSending.value || codeCountDown.value > 0) return
	codeSending.value = true
	uni.showLoading({ title: '发送中...' })
	try {
		await sendChangePasswordEmailCodeApi()
		uni.hideLoading()
		uni.showToast({ title: '验证码已发送', icon: 'none' })
		startCountDown()
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '发送失败', icon: 'none' })
	} finally {
		codeSending.value = false
	}
}

const openDeletionEditor = () => {
	deletionReason.value = ''
	deletionEditorVisible.value = true
}

const closeDeletionEditor = () => {
	deletionEditorVisible.value = false
}

const goDesignerApply = () => {
	uni.navigateTo({ url: '/pages/user/designer-apply' })
}

// 计算缓存大小
const calculateCacheSize = () => {
	try {
		// 需要保留的关键数据
		const keepKeys = [
			'token', 'token_expire_time', 'user_role', 'user_profile',
			'remember_password', 'auto_login', 'saved_username', 'saved_password'
		]
		let totalSize = 0

		// 获取所有存储的键
		const res = uni.getStorageInfoSync()
		const keys = res.keys || []

		keys.forEach(key => {
			if (!keepKeys.includes(key)) {
				try {
					const value = uni.getStorageSync(key)
					if (value !== null && value !== undefined && value !== '') {
						// 估算数据大小（字节）
						const str = typeof value === 'string' ? value : JSON.stringify(value)
						totalSize += new Blob([str]).size
					}
				} catch (_) {}
			}
		})

		// 转换为可读格式
		if (totalSize < 1024) {
			cacheSize.value = `${totalSize}B`
		} else if (totalSize < 1024 * 1024) {
			cacheSize.value = `${(totalSize / 1024).toFixed(1)}KB`
		} else {
			cacheSize.value = `${(totalSize / (1024 * 1024)).toFixed(1)}MB`
		}
	} catch (_) {
		cacheSize.value = '0KB'
	}
}

// 清除缓存
const clearCache = () => {
	uni.showModal({
		title: '清除缓存',
		content: '确定要清除缓存吗？这不会影响您的账号信息和登录状态。',
		success: (res) => {
			if (res.confirm) {
				// 需要保留的关键数据
				const keepKeys = [
					'token', 'token_expire_time', 'user_role', 'user_profile',
					'remember_password', 'auto_login', 'saved_username', 'saved_password'
				]
				const keepValues = {}

				// 保存关键数据
				keepKeys.forEach(key => {
					try {
						keepValues[key] = uni.getStorageSync(key)
					} catch (_) {}
				})

				// 清除所有存储
				try {
					uni.clearStorageSync()
				} catch (_) {}

				// 恢复关键数据
				Object.entries(keepValues).forEach(([key, value]) => {
					if (value !== null && value !== undefined && value !== '') {
						try {
							uni.setStorageSync(key, value)
						} catch (_) {}
					}
				})

				cacheSize.value = '0KB'
				uni.showToast({ title: '缓存已清除', icon: 'success' })
			}
		}
	})
}

// 检查更新
const checkUpdate = () => {
	uni.showLoading({ title: '检查中...' })
	setTimeout(() => {
		uni.hideLoading()
		uni.showModal({
			title: '检查更新',
			content: `当前版本 ${APP_VERSION}，已是最新版本。`,
			showCancel: false
		})
	}, 1000)
}

// 关于我们
const goAbout = () => {
	uni.showModal({
		title: '关于我们',
		content: '3D打印小物件定制商城\n您的个性化3D打印定制专家\n\n我们致力于为您提供高质量的3D打印定制服务，让您创意无限，打印未来。',
		showCancel: false,
		confirmText: '知道了'
	})
}

// 用户协议
const goUserAgreement = () => {
	uni.navigateTo({
		url: '/pages/common/webview?type=userAgreement&title=' + encodeURIComponent('用户协议'),
		fail: (err) => {
			console.error('跳转失败:', err)
			uni.showToast({ title: '跳转失败', icon: 'none' })
		}
	})
}

// 隐私政策
const goPrivacyPolicy = () => {
	uni.navigateTo({
		url: '/pages/common/webview?type=privacyPolicy&title=' + encodeURIComponent('隐私政策'),
		fail: (err) => {
			console.error('跳转失败:', err)
			uni.showToast({ title: '跳转失败', icon: 'none' })
		}
	})
}

const submitPasswordChange = async () => {
	const payload = {
		oldPassword: String(passwordForm.value.oldPassword || '').trim(),
		emailCode: String(passwordForm.value.emailCode || '').trim(),
		newPassword: String(passwordForm.value.newPassword || '').trim(),
		confirmNewPassword: String(passwordForm.value.confirmNewPassword || '').trim()
	}

	if (!payload.oldPassword || !payload.emailCode || !payload.newPassword || !payload.confirmNewPassword) {
		uni.showToast({ title: '请填写完整密码信息', icon: 'none' })
		return
	}
	if (!/^\d{6}$/.test(payload.emailCode)) {
		uni.showToast({ title: '请输入6位邮箱验证码', icon: 'none' })
		return
	}
	if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/.test(payload.newPassword)) {
		uni.showToast({ title: '新密码需含大小写字母和数字', icon: 'none' })
		return
	}
	if (payload.newPassword !== payload.confirmNewPassword) {
		uni.showToast({ title: '两次新密码不一致', icon: 'none' })
		return
	}

	uni.showLoading({ title: '提交中...' })
	try {
		await changePasswordApi(payload)
		closePasswordEditor()
		await doLogout({ clearRemember: true })
		uni.hideLoading()
		uni.showToast({ title: '密码修改成功，请重新登录', icon: 'none' })
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/auth/login' })
		}, 300)
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '修改失败', icon: 'none' })
	}
}

const submitDeletionRequest = async () => {
	const reason = String(deletionReason.value || '').trim()
	if (!reason) {
		uni.showToast({ title: '请输入注销原因', icon: 'none' })
		return
	}

	uni.showLoading({ title: '提交中...' })
	try {
		await requestAccountDeletionApi({ reason })
		uni.hideLoading()
		uni.showToast({ title: '注销申请已提交', icon: 'success' })
		closeDeletionEditor()
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error.message || '提交失败', icon: 'none' })
	}
}

const handleLogout = () => {
	uni.showModal({
		title: '提示',
		content: '确定要退出登录吗？',
		success: async (res) => {
			if (res.confirm) {
				uni.showLoading({ title: '正在退出...' })
				await doLogout()
				uni.hideLoading()
				uni.reLaunch({ url: '/pages/auth/login' })
			}
		}
	})
}

onUnmounted(() => {
	if (codeTimer) {
		clearInterval(codeTimer)
		codeTimer = null
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

.settings-container {
	min-height: 100vh;
	background-color: $bg;
	padding: 8rpx 28rpx 28rpx;
}

.group-title {
	font-size: 24rpx;
	color: $text3;
	margin: 32rpx 0 16rpx 8rpx;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 2rpx;
}

.menu-card {
	padding: 0 32rpx;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;
	.menu-item {
		height: 104rpx;
		display: flex;
		align-items: center;
		& + .menu-item { border-top: 1rpx solid rgba(0,0,0,0.04); }
		.name { font-size: 28rpx; color: $text1; font-weight: 500; }
		.val { font-size: 26rpx; color: $text3; margin-left: auto; margin-right: 12rpx; }
	}
}

.logout-section {
	margin-top: 60rpx;
	.logout-btn {
		height: 96rpx;
		background-color: $card;
		color: $danger;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		box-shadow: $shadow;
		&:active { transform: scale(0.96); }
	}
}

.editor-mask {
	position: fixed;
	inset: 0;
	background-color: rgba(0, 0, 0, 0.35);
	z-index: 1000;
}

.editor-panel {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: $card;
	border-radius: 32rpx 32rpx 0 0;
	padding: 36rpx 32rpx calc(env(safe-area-inset-bottom) + 32rpx);
	z-index: 1001;
	box-shadow: 0 -8rpx 40rpx rgba(0, 0, 0, 0.08);
	.editor-title {
		font-size: 36rpx;
		font-weight: 700;
		color: $text1;
		margin-bottom: 12rpx;
	}
	.deletion-tip {
		font-size: 24rpx;
		color: $text2;
		margin-bottom: 20rpx;
	}
	.form-item {
		display: flex;
		align-items: center;
		min-height: 84rpx;
		.label {
			width: 130rpx;
			font-size: 28rpx;
			color: $text2;
			font-weight: 500;
		}
		.input {
			flex: 1;
			font-size: 28rpx;
			color: $text1;
			height: 72rpx;
			background: $bg;
			border-radius: 16rpx;
			padding: 0 20rpx;
		}
		&.textarea-item { display: block; }
		.textarea {
			width: 100%;
			min-height: 160rpx;
			font-size: 28rpx;
			color: $text1;
			padding: 20rpx;
			background-color: $bg;
			border-radius: 16rpx;
		}
	}
	.code-row {
		.mini-btn {
			width: 180rpx;
			height: 64rpx;
			line-height: 64rpx;
			font-size: 24rpx;
			border-radius: 999rpx;
			background: $gradient;
			color: #ffffff;
			padding: 0;
			margin: 0;
			&:active { transform: scale(0.96); }
		}
		.mini-btn[disabled] {
			background: $bg;
			color: $text3;
		}
	}
	.editor-actions {
		display: flex;
		gap: 20rpx;
		margin-top: 28rpx;
		.action-btn {
			flex: 1;
			height: 84rpx;
			border-radius: 999rpx;
			font-size: 30rpx;
			font-weight: 600;
			&:active { transform: scale(0.96); }
			&.cancel {
				background-color: $bg;
				color: $text2;
			}
			&.save {
				background: $gradient;
				color: #ffffff;
				box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);
			}
			&.danger {
				background-color: $danger;
				color: #ffffff;
			}
		}
	}
}
</style>
