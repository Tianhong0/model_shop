<template>
	<view class="upload-container" v-if="canAccess">
		<view class="upload-list" v-if="models.length > 0">
			<view class="model-card" v-for="(model, index) in models" :key="index">
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
	<view class="blocked" v-else>
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
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.upload-container {
	min-height: 100vh;
	background: $bg;
	padding: 28rpx;
	padding-bottom: 160rpx;
}

.model-card {
	display: flex;
	padding: 24rpx;
	margin-bottom: 20rpx;
	align-items: center;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;
	animation: fadeInUp 0.4s ease both;
	.preview {
		width: 140rpx;
		height: 140rpx;
		border-radius: 16rpx;
		background-color: $bg;
	}
	.info {
		flex: 1;
		margin-left: 24rpx;
		.name-row {
			display: flex;
			align-items: center;
			justify-content: space-between;
			.name { font-size: 28rpx; font-weight: 700; color: $text1; }
			.status {
				font-size: 20rpx;
				padding: 4rpx 16rpx;
				border-radius: 999rpx;
				font-weight: 600;
				&.pending { background-color: #fef3c7; color: #d97706; }
				&.approved { background-color: #dcfce7; color: #16a34a; }
				&.rejected { background-color: #fee2e2; color: #dc2626; }
			}
		}
		.time { font-size: 22rpx; color: $text3; margin-top: 8rpx; display: block; }
		.stats {
			display: flex;
			gap: 20rpx;
			margin-top: 10rpx;
			font-size: 22rpx;
			color: $text2;
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;
	color: $text3;
	text { margin-top: 24rpx; font-size: 28rpx; }
}

.bottom-btn {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 24rpx 40rpx calc(env(safe-area-inset-bottom) + 24rpx);
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	.add-btn {
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		box-shadow: 0 8rpx 30rpx rgba(0, 191, 255, 0.25);
		&:active { transform: scale(0.96); }
	}
}

.blocked {
	margin: 40rpx 28rpx;
	padding: 48rpx 32rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	background: $card;
	border-radius: 24rpx;
	box-shadow: $shadow;
	.blocked-title { font-size: 30rpx; font-weight: 700; color: $text1; }
	.blocked-desc { font-size: 24rpx; color: $text2; margin-top: 16rpx; line-height: 1.6; }
	.back-btn {
		margin-top: 32rpx;
		height: 80rpx;
		padding: 0 48rpx;
		background: $gradient;
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 28rpx;
		&:active { transform: scale(0.96); }
	}
}
</style>
