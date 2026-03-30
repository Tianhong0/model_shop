<template>
	<view class="publish-container">
		<view v-if="canPublish" class="form-card card">
			<view class="reject-banner" v-if="isEditMode && rejectReason">
				<text class="reject-label">驳回原因：</text>
				<text class="reject-text">{{rejectReason}}</text>
			</view>
			<view class="form-item">
				<text class="label">需求标题</text>
				<input class="input" v-model="form.title" placeholder="请输入需求标题" />
			</view>
			<view class="form-item">
				<text class="label">需求类型</text>
				<picker :range="categories" @change="onCategoryChange">
					<view class="picker-val">{{form.category || '请选择'}} </view>
				</picker>
				<uni-icons type="right" size="14" color="#8a9aaa"></uni-icons>
			</view>
			<view class="form-item">
				<text class="label">预算赏金</text>
				<input class="input" type="number" v-model="form.price" placeholder="例如 200" />
				<text class="unit">元</text>
			</view>
			<view class="form-item">
				<text class="label">交付周期</text>
				<picker mode="date" @change="onDeadlineChange">
					<view class="picker-val">{{form.deadline || '请选择截止日期'}} </view>
				</picker>
				<uni-icons type="right" size="14" color="#8a9aaa"></uni-icons>
			</view>
			<view class="form-item column">
				<text class="label">需求描述</text>
				<textarea class="textarea" v-model="form.content" placeholder="请描述需求细节、尺寸、材料、交付格式等"></textarea>
			</view>
			<view class="form-item column">
				<text class="label">附件与参考</text>
				<view class="upload-box" @click="chooseAndUploadFile">
					<uni-icons type="plusempty" size="24" color="#8a9aaa"></uni-icons>
					<text>上传参考图</text>
				</view>
				<view class="file-list" v-if="form.files.length">
					<view class="thumb-item" v-for="(fileUrl, i) in form.files" :key="fileUrl + i">
						<image class="thumb-img" :src="fileUrl" mode="aspectFill" @click="previewAttachment(i)" />
						<view class="thumb-remove" @click="removeAttachment(i)">×</view>
					</view>
				</view>
			</view>
			<view class="form-item">
				<text class="label">联系方式</text>
				<input class="input" v-model="form.contact" placeholder="手机号 / 微信" />
			</view>
		</view>

		<view v-else class="blocked card">
			<text class="blocked-title">当前角色无权限发布悬赏</text>
			<text class="blocked-desc">仅普通用户可发布悬赏需求，设计者请在悬赏中心参与竞标。</text>
			<button class="back-btn" @click="uni.navigateBack()">返回</button>
		</view>

		<view class="submit-bar" v-if="canPublish">
			<button class="submit-btn" @click="submit">{{isEditMode ? '重新提交审核' : '发布悬赏'}}</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { createBountyTaskApi, resubmitBountyTaskApi, getBountyTaskDetailApi, uploadBountyAttachmentApi } from '../../api/reward'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const userRole = ref(getStoredUserRole())
const canPublish = computed(() => !isDesignerRole(userRole.value))

const editTaskId = ref(null)
const isEditMode = computed(() => !!editTaskId.value)
const rejectReason = ref('')

onLoad((options) => {
	userRole.value = getStoredUserRole()
	if (options?.taskId) {
		editTaskId.value = options.taskId
		loadTaskForEdit(editTaskId.value)
	}
})

onShow(() => {
	userRole.value = getStoredUserRole()
})

const categories = ['模型建模', '结构设计', '打印服务', '渲染展示', '装配改造']

const form = ref({
	title: '',
	category: '',
	price: '',
	deadline: '',
	content: '',
	files: [],
	contact: ''
})

const loadTaskForEdit = async (taskId) => {
	try {
		uni.showLoading({ title: '加载中...' })
		const data = await getBountyTaskDetailApi(taskId)
		uni.hideLoading()
		form.value.title = data.title || ''
		form.value.content = data.description || ''
		form.value.category = data.category || ''
		form.value.price = String(data.budgetAmount || '')
		form.value.files = Array.isArray(data.attachments) ? data.attachments : []
		rejectReason.value = data.closeReason || ''
		if (data.deadlineTime) {
			const dateStr = String(data.deadlineTime).substring(0, 10)
			form.value.deadline = dateStr
		}
		uni.setNavigationBarTitle({ title: '修改悬赏' })
	} catch (error) {
		uni.hideLoading()
		uni.showToast({ title: error?.message || '加载任务失败', icon: 'none' })
	}
}

const onCategoryChange = (e) => {
	form.value.category = categories[e.detail.value]
}

const onDeadlineChange = (e) => {
	form.value.deadline = e.detail.value
}

const chooseAndUploadFile = async () => {
	try {
		const chooseRes = await new Promise((resolve, reject) => {
			uni.chooseImage({
				count: 5,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: resolve,
				fail: reject
			})
		})
		const filePaths = chooseRes?.tempFilePaths || []
		if (!filePaths.length) {
			return
		}
		uni.showLoading({ title: '上传中...' })
		for (const filePath of filePaths) {
			const url = await uploadBountyAttachmentApi(filePath, 'postImg')
			form.value.files.push(url)
		}
		uni.hideLoading()
		uni.showToast({ title: '上传成功', icon: 'success' })
	} catch (error) {
		uni.hideLoading()
		if (String(error?.errMsg || '').includes('cancel')) {
			return
		}
		uni.showToast({ title: error?.message || '上传失败', icon: 'none' })
	}
}

const previewAttachment = (index) => {
	uni.previewImage({
		current: index,
		urls: form.value.files
	})
}

const removeAttachment = (index) => {
	form.value.files.splice(index, 1)
}

const calcExpectedDays = (deadlineDate) => {
	if (!deadlineDate) {
		return 1
	}
	const now = new Date()
	const end = new Date(`${deadlineDate}T23:59:59`)
	const diff = end.getTime() - now.getTime()
	if (!Number.isFinite(diff) || diff <= 0) {
		return 1
	}
	return Math.ceil(diff / (24 * 60 * 60 * 1000))
}

const submit = async () => {
	if (!form.value.title || !form.value.category || !form.value.price || !form.value.content || !form.value.deadline) {
		uni.showToast({ title: '请完善标题/类型/预算/描述/截止日期', icon: 'none' })
		return
	}
	try {
		const expectedDays = calcExpectedDays(form.value.deadline)
		const payload = {
			title: form.value.title,
			description: form.value.content,
			category: form.value.category,
			tags: form.value.category,
			budgetAmount: Number(form.value.price),
			expectedDays,
			deadlineTime: `${form.value.deadline} 23:59:59`,
			attachments: form.value.files
		}

		if (isEditMode.value) {
			await resubmitBountyTaskApi({ ...payload, taskId: editTaskId.value })
			uni.showToast({ title: '已重新提交审核', icon: 'success' })
			setTimeout(() => {
				uni.redirectTo({ url: `/pages/reward/detail?id=${editTaskId.value}` })
			}, 800)
		} else {
			const taskId = await createBountyTaskApi(payload)
			uni.showToast({ title: '发布成功，等待平台审核', icon: 'success' })
			setTimeout(() => {
				uni.redirectTo({ url: `/pages/reward/detail?id=${taskId}` })
			}, 800)
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '提交失败', icon: 'none' })
	}
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.publish-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 140rpx;
}

.form-card {
	margin: 28rpx 32rpx;
	padding: 28rpx 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out;
	.reject-banner {
		background: rgba(255,77,109,0.06);
		border-radius: 16rpx;
		padding: 20rpx 24rpx;
		margin-bottom: 20rpx;
		.reject-label { font-size: 24rpx; color: $text-muted; }
		.reject-text { font-size: 24rpx; color: $danger; font-weight: 500; }
	}
	.form-item {
		min-height: 100rpx;
		display: flex;
		align-items: center;
		padding: 8rpx 0;
		&.column {
			flex-direction: column;
			align-items: flex-start;
			height: auto;
			padding: 24rpx 0;
		}
		& + .form-item {
			border-top: 1rpx solid rgba(0,0,0,0.04);
		}
		.label { width: 160rpx; font-size: 28rpx; color: $text-secondary; font-weight: 500; }
		.input { flex: 1; font-size: 28rpx; color: $text-primary; text-align: right; }
		.picker-val { flex: 1; font-size: 28rpx; color: $text-primary; text-align: right; margin-right: 10rpx; }
		.unit { font-size: 24rpx; color: $text-muted; margin-left: 10rpx; }
		.textarea {
			width: 100%; min-height: 180rpx; background-color: $bg;
			border-radius: 16rpx; padding: 20rpx; font-size: 26rpx;
			margin-top: 12rpx; color: $text-primary;
		}
		.upload-box {
			width: 100%;
			height: 140rpx;
			border: 2rpx dashed rgba(0, 191, 255, 0.3);
			display: flex;
			flex-direction: column;
			align-items: center;
			justify-content: center;
			color: $text-muted;
			border-radius: 16rpx;
			margin-top: 12rpx;
			background: rgba(0,191,255,0.02);
			text { font-size: 22rpx; margin-top: 8rpx; }
			&:active { background: rgba(0,191,255,0.06); }
		}
		.file-list { margin-top: 16rpx; display: flex; gap: 16rpx; flex-wrap: wrap; }
		.thumb-item {
			position: relative;
			width: 130rpx;
			height: 130rpx;
			border-radius: 16rpx;
			overflow: hidden;
			background-color: rgba(0, 191, 255, 0.04);
		}
		.thumb-img {
			width: 130rpx;
			height: 130rpx;
			display: block;
		}
		.thumb-remove {
			position: absolute;
			right: 6rpx;
			top: 6rpx;
			width: 36rpx;
			height: 36rpx;
			line-height: 36rpx;
			text-align: center;
			font-size: 24rpx;
			color: #fff;
			background-color: rgba(0, 0, 0, 0.45);
			border-radius: 50%;
		}
	}
}

.blocked {
	margin: 40rpx 32rpx;
	padding: 48rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	.blocked-title { font-size: 30rpx; font-weight: 700; color: $text-primary; }
	.blocked-desc { font-size: 26rpx; color: $text-secondary; margin-top: 16rpx; line-height: 1.6; }
	.back-btn {
		margin-top: 36rpx;
		height: 80rpx;
		padding: 0 48rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 28rpx;
		&:active { transform: scale(0.96); }
	}
}

.submit-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 20rpx 40rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	box-shadow: 0 -4rpx 24rpx rgba(0,0,0,0.06);
	.submit-btn {
		height: 88rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 700;
		&:active { transform: scale(0.96); }
	}
}
</style>
