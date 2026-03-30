<template>
	<view class="event-detail-container">
		<!-- Banner -->
		<view class="banner-wrap">
			<image :src="event.bannerUrl" class="banner" mode="aspectFill"></image>
			<view class="banner-overlay"></view>
		</view>

		<view class="content-section">
			<!-- 活动头部信息 -->
			<view class="event-header">
				<view class="status-tag" :class="getStatusClass(event.status)">{{event.statusName}}</view>
				<text class="title">{{event.title}}</text>
				<view class="meta">
					<view class="item">
						<uni-icons type="calendar" size="14" color="#999"></uni-icons>
						<text>{{formatTime(event.startTime)}} ~ {{formatTime(event.endTime)}}</text>
					</view>
					<view class="item" v-if="event.location">
						<uni-icons type="location" size="14" color="#999"></uni-icons>
						<text>{{event.location}}</text>
					</view>
					<view class="item">
						<uni-icons type="person" size="14" color="#999"></uni-icons>
						<text>{{event.currentParticipants || 0}}人已报名</text>
					</view>
				</view>
			</view>

			<!-- 设计竞赛快捷入口 -->
			<view class="contest-entry" v-if="event.eventType === 1 && event.status >= 1">
				<view class="entry-item" @tap="goSubmissions">
					<view class="entry-icon entry-icon--blue">
						<uni-icons type="medal" size="22" color="#00bfff"></uni-icons>
					</view>
					<view class="entry-info">
						<text class="entry-title">参赛作品</text>
						<text class="entry-desc">查看所有参赛作品</text>
					</view>
					<uni-icons type="right" size="16" color="#ccc"></uni-icons>
				</view>
				<view class="entry-item" v-if="event.signedUp" @tap="goMySubmission">
					<view class="entry-icon entry-icon--green">
						<uni-icons type="compose" size="22" color="#10b981"></uni-icons>
					</view>
					<view class="entry-info">
						<text class="entry-title">我的作品</text>
						<text class="entry-desc">提交或修改您的参赛作品</text>
					</view>
					<uni-icons type="right" size="16" color="#ccc"></uni-icons>
				</view>
			</view>

			<!-- 详情卡片 -->
			<view class="detail-card">
				<view class="sec-title">活动介绍</view>
				<text class="desc">{{event.description || '暂无介绍'}}</text>

				<view class="sec-title" v-if="event.rewards && event.rewards.length">奖励设置</view>
				<view class="reward-list" v-if="event.rewards && event.rewards.length">
					<view class="reward-item" v-for="(reward, idx) in event.rewards" :key="idx">
						<text class="rank">{{reward.rankName}}</text>
						<text class="prize">{{reward.prizeContent}}</text>
					</view>
				</view>

				<view class="sec-title" v-if="event.rules">参赛要求</view>
				<text class="desc" v-if="event.rules">{{event.rules}}</text>
			</view>
		</view>

		<!-- 底部操作栏 -->
		<view class="bottom-bar">
			<view class="share-btn" @click="handleShare">
				<uni-icons type="redo" size="20" color="#999"></uni-icons>
				<text>分享</text>
			</view>
			<button
				class="join-btn"
				:class="{ 'signed-up': event.signedUp }"
				:disabled="!canSignup && !event.signedUp"
				:loading="loading"
				@click="handleJoin"
			>
				{{getButtonText}}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getEventDetailApi, signupEventApi, cancelSignupApi } from '../../api/event'

const event = ref({
	id: null,
	title: '',
	bannerUrl: '',
	status: 0,
	statusName: '',
	eventType: 1,
	startTime: '',
	endTime: '',
	location: '',
	description: '',
	rewards: [],
	rules: '',
	signedUp: false,
	currentParticipants: 0
})

const loading = ref(false)
const eventId = ref(null)

const canSignup = computed(() => {
	return event.value.status === 1 && !event.value.signedUp
})

const getButtonText = computed(() => {
	if (event.value.signedUp) {
		return '取消报名'
	}
	if (event.value.status === 1) {
		return '立即报名'
	}
	return '报名已结束'
})

const getStatusClass = (status) => {
	const map = {
		0: 'upcoming',
		1: 'ongoing',
		2: 'ongoing',
		3: 'judging',
		4: 'ended'
	}
	return map[status] || 'ended'
}

const formatTime = (time) => {
	if (!time) return ''
	return time.substring(0, 10)
}

const fetchDetail = async () => {
	try {
		const res = await getEventDetailApi(eventId.value)
		event.value = { ...event.value, ...res }
	} catch (error) {
		uni.showToast({ title: error.message || '加载失败', icon: 'none' })
	}
}

// 跳转到参赛作品列表
const goSubmissions = () => {
	uni.navigateTo({
		url: `/pages/event/submissions?eventId=${eventId.value}`
	})
}

// 跳转到我的作品
const goMySubmission = () => {
	uni.navigateTo({
		url: `/pages/event/my-submission?eventId=${eventId.value}`
	})
}

const handleJoin = async () => {
	if (event.value.signedUp) {
		// 取消报名
		uni.showModal({
			title: '取消报名',
			content: '确定取消报名吗？',
			success: async (res) => {
				if (res.confirm) {
					loading.value = true
					try {
						await cancelSignupApi(eventId.value)
						uni.showToast({ title: '已取消报名', icon: 'success' })
						await fetchDetail()
					} catch (error) {
						uni.showToast({ title: error.message || '操作失败', icon: 'none' })
					} finally {
						loading.value = false
					}
				}
			}
		})
	} else {
		// 报名
		uni.showModal({
			title: '报名确认',
			content: '确定报名参加《' + event.value.title + '》吗？',
			success: async (res) => {
				if (res.confirm) {
					loading.value = true
					try {
						await signupEventApi(eventId.value)
						uni.showToast({ title: '报名成功', icon: 'success' })
						await fetchDetail()
					} catch (error) {
						uni.showToast({ title: error.message || '报名失败', icon: 'none' })
					} finally {
						loading.value = false
					}
				}
			}
		})
	}
}

const handleShare = () => {
	uni.showToast({ title: '分享功能开发中', icon: 'none' })
}

onLoad((options) => {
	if (options.id) {
		eventId.value = options.id
		fetchDetail()
	}
})
</script>

<style scoped lang="scss">
.event-detail-container {
	min-height: 100vh;
	background-color: #f8f8f8;
	padding-bottom: 140rpx;
	position: relative;
}

.banner-wrap {
	position: relative;
	width: 100%;
	height: 450rpx;
	overflow: hidden;
}

.banner {
	width: 100%;
	height: 100%;
	transition: opacity 0.4s ease;
}

.banner-overlay {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	height: 120rpx;
	background: linear-gradient(to top, #f8f8f8, transparent);
}

.content-section {
	margin-top: -48rpx;
	position: relative;
	z-index: 1;
	padding: 0 28rpx;
	display: flex;
	flex-direction: column;
	gap: 28rpx;
}

.event-header {
	background: #ffffff;
	padding: 36rpx 32rpx;
	border-radius: 24rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease forwards;

	.status-tag {
		display: inline-block;
		padding: 8rpx 24rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 600;
		margin-bottom: 20rpx;
		animation: breatheGlow 2.5s ease-in-out infinite;

		&.ongoing {
			background: rgba(16, 185, 129, 0.1);
			color: #10b981;
		}
		&.ended {
			background: rgba(156, 163, 175, 0.1);
			color: #9ca3af;
			animation: none;
		}
		&.upcoming {
			background: rgba(0, 191, 255, 0.1);
			color: #00bfff;
		}
		&.judging {
			background: rgba(255, 149, 0, 0.1);
			color: #ff9500;
		}
	}

	.title {
		font-size: 36rpx;
		font-weight: 700;
		color: #1a1a1a;
		display: block;
		line-height: 1.4;
	}

	.meta {
		margin-top: 28rpx;

		.item {
			display: flex;
			align-items: center;
			margin-bottom: 14rpx;

			text {
				font-size: 26rpx;
				color: #666;
				margin-left: 12rpx;
			}
		}
	}
}

@keyframes fadeInUp {
	from {
		opacity: 0;
		transform: translateY(24rpx);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

@keyframes breatheGlow {
	0%, 100% {
		box-shadow: 0 0 12rpx rgba(0, 191, 255, 0.15);
	}
	50% {
		box-shadow: 0 0 24rpx rgba(0, 191, 255, 0.35);
	}
}

/* 设计竞赛快捷入口 */
.contest-entry {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 24rpx 32rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease 0.1s forwards;
	opacity: 0;

	.entry-item {
		display: flex;
		align-items: center;
		padding: 24rpx 0;

		&:not(:last-child) {
			box-shadow: 0 1rpx 0 rgba(0, 0, 0, 0.04);
		}

		&:active {
			opacity: 0.7;
		}

		.entry-icon {
			width: 72rpx;
			height: 72rpx;
			border-radius: 20rpx;
			display: flex;
			align-items: center;
			justify-content: center;

			&--blue {
				background: rgba(0, 191, 255, 0.08);
			}

			&--green {
				background: rgba(16, 185, 129, 0.08);
			}
		}

		.entry-info {
			flex: 1;
			margin-left: 20rpx;

			.entry-title {
				font-size: 30rpx;
				font-weight: 600;
				color: #1a1a1a;
				display: block;
			}

			.entry-desc {
				font-size: 24rpx;
				color: #999;
				margin-top: 6rpx;
				display: block;
			}
		}
	}
}

.detail-card {
	background: #ffffff;
	border-radius: 24rpx;
	padding: 36rpx 32rpx;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease 0.2s forwards;
	opacity: 0;

	.sec-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a1a1a;
		margin-bottom: 20rpx;
		position: relative;
		padding-left: 20rpx;

		&::before {
			content: '';
			position: absolute;
			left: 0;
			top: 50%;
			transform: translateY(-50%);
			width: 6rpx;
			height: 28rpx;
			background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
			border-radius: 3rpx;
		}

		&:not(:first-child) {
			margin-top: 40rpx;
		}
	}

	.desc {
		font-size: 28rpx;
		color: #666;
		line-height: 1.7;
		white-space: pre-wrap;
	}

	.reward-list {
		display: flex;
		flex-direction: column;
		gap: 16rpx;

		.reward-item {
			background: #f8f8f8;
			padding: 24rpx 28rpx;
			border-radius: 16rpx;
			display: flex;
			justify-content: space-between;
			align-items: center;

			.rank {
				font-size: 26rpx;
				font-weight: 700;
				color: #0099cc;
			}

			.prize {
				font-size: 26rpx;
				color: #1a1a1a;
			}
		}
	}
}

.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 110rpx;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	display: flex;
	align-items: center;
	padding: 0 32rpx;
	box-shadow: 0 -8rpx 40rpx rgba(0, 0, 0, 0.04);
	z-index: 100;

	.share-btn {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-right: 36rpx;

		&:active {
			opacity: 0.7;
		}

		text {
			font-size: 20rpx;
			color: #666;
			margin-top: 4rpx;
		}
	}

	.join-btn {
		flex: 1;
		height: 84rpx;
		background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 700;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.3);
		transition: all 0.2s;

		&:active {
			transform: scale(0.96);
		}

		&.signed-up {
			background: #ffffff;
			color: #ff4d6d;
			box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
		}

		&[disabled] {
			opacity: 0.5;
			background: #f0f0f0;
			color: #999;
			box-shadow: none;
		}
	}
}
</style>
