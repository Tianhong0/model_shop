<template>
	<view class="publish-container">
		<view v-if="canPublish" class="form-card card">
			<view class="form-item">
				<text class="label">需求标题</text>
				<input class="input" v-model="form.title" placeholder="请输入需求标题" />
			</view>
			<view class="form-item">
				<text class="label">需求类型</text>
				<picker :range="categories" @change="onCategoryChange">
					<view class="picker-val">{{form.category || '请选择'}} </view>
				</picker>
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
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
				<uni-icons type="right" size="14" color="#cbd5e1"></uni-icons>
			</view>
			<view class="form-item column">
				<text class="label">需求描述</text>
				<textarea class="textarea" v-model="form.content" placeholder="请描述需求细节、尺寸、材料、交付格式等"></textarea>
			</view>
			<view class="form-item column">
				<text class="label">附件与参考</text>
				<view class="upload-box" @click="chooseAndUploadFile">
					<uni-icons type="plusempty" size="24" color="#94a3b8"></uni-icons>
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
			<button class="submit-btn" @click="submit">发布悬赏</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createBountyTaskApi, uploadBountyAttachmentApi } from '../../api/reward'
import { getStoredUserRole, isDesignerRole } from '../../utils/role'

const userRole = ref(getStoredUserRole())
const canPublish = computed(() => !isDesignerRole(userRole.value))

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
		await createBountyTaskApi({
			title: form.value.title,
			description: form.value.content,
			category: form.value.category,
			tags: form.value.category,
			budgetAmount: Number(form.value.price),
			expectedDays,
			deadlineTime: `${form.value.deadline} 23:59:59`,
			attachments: form.value.files
		})
		uni.showToast({ title: '发布成功', icon: 'success' })
		setTimeout(() => {
			uni.navigateBack()
		}, 800)
	} catch (error) {
		uni.showToast({ title: error?.message || '发布失败', icon: 'none' })
	}
}
</script>

<style scoped lang="scss">
.publish-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding-bottom: 120rpx;
}

.form-card {
	margin: 20rpx 30rpx;
	padding: 20rpx 30rpx;
	.form-item {
		height: 100rpx;
		display: flex;
		align-items: center;
		border-bottom: 2rpx solid #f1f5f9;
		&.column {
			flex-direction: column;
			align-items: flex-start;
			height: auto;
			padding: 20rpx 0;
		}
		.label { width: 160rpx; font-size: 26rpx; color: #475569; }
		.input { flex: 1; font-size: 26rpx; color: #1e293b; text-align: right; }
		.picker-val { flex: 1; font-size: 26rpx; color: #1e293b; text-align: right; margin-right: 10rpx; }
		.unit { font-size: 24rpx; color: #94a3b8; margin-left: 10rpx; }
		.textarea { width: 100%; min-height: 160rpx; background-color: #f8fafc; border-radius: 12rpx; padding: 16rpx; font-size: 24rpx; }
		.upload-box {
			width: 100%;
			height: 120rpx;
			border: 2rpx dashed #e2e8f0;
			display: flex;
			flex-direction: column;
			align-items: center;
			justify-content: center;
			color: #94a3b8;
			border-radius: 12rpx;
			text { font-size: 22rpx; margin-top: 8rpx; }
		}
		.file-list { margin-top: 12rpx; display: flex; gap: 12rpx; flex-wrap: wrap; }
		.thumb-item {
			position: relative;
			width: 120rpx;
			height: 120rpx;
			border-radius: 12rpx;
			overflow: hidden;
			background-color: #eef2ff;
		}
		.thumb-img {
			width: 120rpx;
			height: 120rpx;
			display: block;
		}
		.thumb-remove {
			position: absolute;
			right: 6rpx;
			top: 6rpx;
			width: 32rpx;
			height: 32rpx;
			line-height: 32rpx;
			text-align: center;
			font-size: 24rpx;
			color: #fff;
			background-color: rgba(0, 0, 0, 0.45);
			border-radius: 50%;
		}
	}
}

.blocked {
	margin: 40rpx 30rpx;
	padding: 40rpx 30rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	.blocked-title { font-size: 30rpx; font-weight: 700; color: #1e293b; }
	.blocked-desc { font-size: 24rpx; color: #64748b; margin-top: 16rpx; line-height: 1.6; }
	.back-btn {
		margin-top: 30rpx;
		height: 80rpx;
		padding: 0 40rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 40rpx;
		font-size: 26rpx;
	}
}

.submit-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 20rpx 40rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 20rpx);
	background-color: #ffffff;
	box-shadow: 0 -4rpx 12rpx rgba(0,0,0,0.05);
	.submit-btn {
		height: 88rpx;
		background-color: #f59e0b;
		color: #ffffff;
		border-radius: 44rpx;
		font-size: 30rpx;
		font-weight: 700;
	}
}
</style>
