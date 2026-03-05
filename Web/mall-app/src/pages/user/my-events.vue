<template>
	<view class="my-events-container">
		<view class="event-list" v-if="events.length > 0">
			<view class="event-card card" v-for="(item, index) in events" :key="index" @click="goDetail(item.id)">
				<image :src="item.banner" class="banner" mode="aspectFill"></image>
				<view class="info">
					<view class="title-row">
						<text class="title">{{item.title}}</text>
						<text class="status" :class="item.status">{{statusMap[item.status]}}</text>
					</view>
					<view class="meta">
						<text>报名时间: {{item.joinTime}}</text>
						<text class="type">{{item.type}}</text>
					</view>
					<view class="footer">
						<text class="result" v-if="item.result">{{item.result}}</text>
						<view class="btn" v-if="item.status === 'ongoing'">上传作品</view>
					</view>
				</view>
			</view>
		</view>
		<view class="empty-state" v-else>
			<uni-icons type="calendar" size="64" color="#cbd5e1"></uni-icons>
			<text>还没有参加过活动</text>
			<button class="go-btn" @click="goCommunity">去看看活动</button>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'

const statusMap = {
	ongoing: '进行中',
	ended: '已结束',
	judging: '评审中'
}

const events = ref([
	{
		id: 501,
		title: '第一届“创意无限”3D打印设计大赛',
		banner: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=400',
		status: 'ongoing',
		joinTime: '2026-01-20',
		type: '设计竞赛',
		result: ''
	},
	{
		id: 502,
		title: '线下沙龙：成都站模型展示会',
		banner: 'https://images.unsplash.com/photo-1540575861501-7ad05823c95b?w=400',
		status: 'ended',
		joinTime: '2026-01-15',
		type: '线下活动',
		result: '已现场签到'
	}
])

const goDetail = (id) => {
	uni.navigateTo({
		url: '/pages/community/event-detail?id=' + id
	})
}

const goCommunity = () => {
	uni.switchTab({
		url: '/pages/community/index'
	})
}
</script>

<style scoped lang="scss">
.my-events-container {
	min-height: 100vh;
	background-color: #f8fafc;
	padding: 20rpx;
}

.event-card {
	overflow: hidden;
	margin-bottom: 24rpx;
	padding: 0;
	.banner {
		width: 100%;
		height: 240rpx;
	}
	.info {
		padding: 24rpx;
		.title-row {
			display: flex;
			justify-content: space-between;
			align-items: flex-start;
			.title { flex: 1; font-size: 30rpx; font-weight: 700; color: #1e293b; margin-right: 20rpx; }
			.status {
				font-size: 20rpx;
				padding: 2rpx 12rpx;
				border-radius: 6rpx;
				&.ongoing { background-color: #dcfce7; color: #16a34a; }
				&.ended { background-color: #f1f5f9; color: #94a3b8; }
				&.judging { background-color: #e0e7ff; color: #4f46e5; }
			}
		}
		.meta {
			margin-top: 16rpx;
			display: flex;
			justify-content: space-between;
			font-size: 24rpx;
			color: #94a3b8;
		}
		.footer {
			margin-top: 24rpx;
			display: flex;
			justify-content: space-between;
			align-items: center;
			.result { font-size: 24rpx; color: #4f46e5; font-weight: 700; }
			.btn {
				padding: 8rpx 24rpx;
				background-color: #4f46e5;
				color: #ffffff;
				font-size: 24rpx;
				border-radius: 30rpx;
			}
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
	color: #94a3b8;
	text { margin: 20rpx 0 40rpx; font-size: 28rpx; }
	.go-btn {
		background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
		color: #ffffff;
		border-radius: 50rpx;
		font-size: 28rpx;
		padding: 0 60rpx;
	}
}
</style>
