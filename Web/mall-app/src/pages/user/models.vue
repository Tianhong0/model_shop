<template>
	<view class="upload-container" v-if="canAccess">
		<view class="upload-list" v-if="models.length > 0">
			<view class="model-card card" v-for="(model, index) in models" :key="index">
				<image :src="model.preview" class="preview" mode="aspectFill"></image>
				<view class="info">
					<view class="name-row">
						<text class="name">{{model.name}}</text>
						<text class="status" :class="model.status">{{statusMap[model.status]}}</text>
					</view>
					<text class="time">上传于: {{model.time}}</text>
					<view class="stats">
						<text>大小: {{model.size}}</text>
						<text>格式: {{model.format}}</text>
					</view>
				</view>
				<view class="actions">
					<uni-icons type="more-filled" size="20" color="#94a3b8"></uni-icons>
				</view>
			</view>
		</view>
		
		<view class="empty-state" v-else>
			<uni-icons type="cloud-upload" size="64" color="#cbd5e1"></uni-icons>
			<text>还没有上传过模型</text>
		</view>

		<view class="bottom-btn">
			<button class="add-btn" @click="handleUpload">上传新模型 (.stl / .obj)</button>
		</view>
	</view>
	<view class="blocked card" v-else>
		<text class="blocked-title">当前角色无权限查看模型上传记录</text>
		<text class="blocked-desc">该功能仅对设计者开放。</text>
		<button class="back-btn" @click="uni.navigateBack()">返回</button>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { isDesignerRole } from '../../utils/role'

const userRole = ref(uni.getStorageSync('user_role') || 'user')
const canAccess = computed(() => isDesignerRole(userRole.value))

const statusMap = {
	pending: '审核中',
	approved: '已通过',
	rejected: '未通过'
}

const models = ref([
	{
		name: '多功能手机支架.stl',
		preview: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200',
		time: '2026-01-25 10:30',
		size: '4.2MB',
		format: 'STL',
		status: 'approved'
	},
	{
		name: '个性化钥匙扣.obj',
		preview: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=200',
		time: '2026-01-28 09:15',
		size: '1.5MB',
		format: 'OBJ',
		status: 'pending'
	}
])

const handleUpload = () => {
	uni.showToast({ title: '上传功能演示中', icon: 'none' })
}
</script>

<style scoped lang="scss">
.upload-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding: 20rpx;
	padding-bottom: 140rpx;
}

.model-card {
	display: flex;
	padding: 20rpx;
	margin-bottom: 20rpx;
	align-items: center;
	.preview {
		width: 140rpx;
		height: 140rpx;
		border-radius: 12rpx;
		background-color: #f1f5f9;
	}
	.info {
		flex: 1;
		margin-left: 24rpx;
		.name-row {
			display: flex;
			align-items: center;
			justify-content: space-between;
			.name { font-size: 28rpx; font-weight: 700; color: #1e293b; }
			.status {
				font-size: 20rpx;
				padding: 2rpx 12rpx;
				border-radius: 4rpx;
				&.pending { background-color: #fef3c7; color: #d97706; }
				&.approved { background-color: #dcfce7; color: #16a34a; }
				&.rejected { background-color: #fee2e2; color: #dc2626; }
			}
		}
		.time { font-size: 22rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
		.stats {
			display: flex;
			gap: 20rpx;
			margin-top: 10rpx;
			font-size: 22rpx;
			color: #64748b;
		}
	}
}

.bottom-btn {
	position: fixed;
	bottom: 40rpx;
	left: 40rpx;
	right: 40rpx;
	.add-btn {
		background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
		color: #ffffff;
		border-radius: 50rpx;
		font-size: 30rpx;
		font-weight: 600;
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
</style>
