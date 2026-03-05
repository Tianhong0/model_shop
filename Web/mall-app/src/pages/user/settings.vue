<template>
	<view class="settings-container">
		<view class="group-title">账号安全</view>
		<view class="menu-card card">
			<view class="menu-item" @click="goProfile">
				<text class="name">个人资料修改</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="menu-item" @click="goAddress">
				<text class="name">收货地址管理</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="menu-item" @click="goProfile">
				<text class="name">手机号绑定</text>
				<text class="val">{{mobileMasked}}</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="menu-item" @click="openPasswordEditor">
				<text class="name">登录密码修改</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="menu-item" @click="goDesignerApply">
				<text class="name">申请成为设计者</text>
				<text class="val">{{ designerApplyStatusText }}</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="menu-item" @click="openDeletionEditor">
				<text class="name">账号注销</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
		</view>

		<view class="group-title">系统设置</view>
		<view class="menu-card card">
			<view class="menu-item">
				<text class="name">消息通知</text>
				<switch checked color="#4f46e5" scale="0.8" />
			</view>
			<view class="menu-item">
				<text class="name">清除缓存</text>
				<text class="val">24.5MB</text>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
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
.settings-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding: 20rpx;
}

.group-title {
	font-size: 26rpx;
	color: #94a3b8;
	margin: 30rpx 0 20rpx 20rpx;
}

.menu-card {
	padding: 0 30rpx;
	.menu-item {
		height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		border-bottom: 2rpx solid #f1f5f9;
		&:last-child { border-bottom: none; }
		.name { font-size: 28rpx; color: #1e293b; }
		.val { font-size: 26rpx; color: #94a3b8; margin-left: auto; margin-right: 10rpx; }
	}
}

.logout-section {
	margin-top: 80rpx;
	.logout-btn {
		height: 90rpx;
		background-color: #ffffff;
		color: #ef4444;
		border-radius: 20rpx;
		font-size: 30rpx;
		font-weight: 600;
		border: 2rpx solid #fee2e2;
	}
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
	.deletion-tip {
		font-size: 24rpx;
		color: #64748b;
		margin-bottom: 14rpx;
	}
	.form-item {
		display: flex;
		align-items: center;
		min-height: 76rpx;
		border-bottom: 2rpx solid #f1f5f9;
		.label {
			width: 120rpx;
			font-size: 26rpx;
			color: #64748b;
		}
		.input {
			flex: 1;
			font-size: 26rpx;
			color: #1e293b;
		}
		&.textarea-item {
			border-bottom: none;
		}
		.textarea {
			width: 100%;
			min-height: 160rpx;
			font-size: 26rpx;
			color: #1e293b;
			padding: 18rpx;
			background-color: #f8fafc;
			border-radius: 12rpx;
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
			&.danger {
				background-color: #ef4444;
				color: #ffffff;
			}
		}
	}
}
</style>
