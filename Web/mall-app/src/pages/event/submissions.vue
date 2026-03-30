<template>
	<view class="submissions-page">
		<!-- 头部信息 -->
		<view class="page-header" v-if="eventInfo.title">
			<view class="header-inner">
				<text class="event-title">{{eventInfo.title}}</text>
				<view class="event-sub-wrap">
					<view class="sub-dot"></view>
					<text class="event-sub">参赛作品 {{total}}件</text>
				</view>
			</view>
		</view>

		<!-- 作品列表 -->
		<scroll-view scroll-y class="submission-list" @scrolltolower="loadMore" lower-threshold="100">
			<view v-if="loading && !submissions.length" class="loading-wrap">
				<view class="loading-spinner"></view>
				<text>加载中...</text>
			</view>

			<view v-else-if="!submissions.length" class="empty-wrap">
				<view class="empty-icon-wrap">
					<uni-icons type="medal" size="48" color="#ccc"></uni-icons>
				</view>
				<text class="empty-text">暂无参赛作品</text>
			</view>

			<view v-else class="submission-grid">
				<view
					class="submission-card"
					v-for="(item, index) in submissions"
					:key="item.id"
					:style="{ animationDelay: `${index * 0.06}s` }"
					@tap="goSubmissionDetail(item)"
				>
					<view class="card-cover">
						<image
							v-if="item.imageUrls && item.imageUrls.length"
							:src="item.imageUrls[0]"
							mode="aspectFill"
							class="cover-img"
						></image>
						<view v-else class="cover-placeholder">
							<uni-icons type="image" size="36" color="#ddd"></uni-icons>
						</view>
						<view class="mine-badge" v-if="item.isMine">
							<text>我的</text>
						</view>
					</view>
					<view class="card-info">
						<text class="card-title">{{item.title}}</text>
						<view class="card-meta">
							<image class="author-avatar" :src="item.authorAvatar || '/static/default-avatar.png'" mode="aspectFill"></image>
							<text class="author-name">{{item.authorName || '匿名'}}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-if="loadingMore" class="loading-more">
				<view class="loading-spinner-sm"></view>
				<text>加载中...</text>
			</view>

			<view class="safe-area-bottom"></view>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="bottom-bar" v-if="eventInfo.eventType === 1 && eventInfo.signedUp">
			<view class="btn-mine" @tap="goMySubmission">
				<uni-icons type="person" size="18" color="#00bfff"></uni-icons>
				<text>我的作品</text>
			</view>
			<button class="btn-submit" v-if="!hasSubmitted" @tap="goSubmitWork">提交作品</button>
			<view class="btn-submitted" v-else>
				<uni-icons type="checkbox-filled" size="16" color="#10b981"></uni-icons>
				<text>已提交作品</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getEventSubmissionsApi, getEventDetailApi } from '../../api/event'

const eventId = ref(null)
const eventInfo = ref({
	title: '',
	eventType: 0,
	signedUp: false,
	status: 0
})
const submissions = ref([])
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(true)
const hasSubmitted = ref(false)

const fetchEventInfo = async () => {
	try {
		const res = await getEventDetailApi(eventId.value)
		eventInfo.value = {
			title: res.title,
			eventType: res.eventType,
			signedUp: res.signedUp,
			status: res.status
		}
	} catch (error) {
		console.error('获取活动信息失败:', error)
	}
}

const fetchSubmissions = async (isRefresh = false) => {
	if (isRefresh) {
		page.value = 1
		hasMore.value = true
	}

	if (!hasMore.value) return

	if (isRefresh) {
		loading.value = true
	} else {
		loadingMore.value = true
	}

	try {
		const res = await getEventSubmissionsApi(eventId.value, {
			page: page.value,
			size: 20
		})

		const list = res?.records || []
		if (isRefresh) {
			submissions.value = list
		} else {
			submissions.value = [...submissions.value, ...list]
		}
		total.value = res?.total || 0
		hasMore.value = list.length >= 20
		page.value++

		// 检查是否有自己的作品
		hasSubmitted.value = submissions.value.some(s => s.isMine)
	} catch (error) {
		uni.showToast({ title: error.message || '加载失败', icon: 'none' })
	} finally {
		loading.value = false
		loadingMore.value = false
	}
}

const loadMore = () => {
	if (!loadingMore.value && hasMore.value) {
		fetchSubmissions()
	}
}

const goSubmissionDetail = (item) => {
	if (item.isMine) {
		// 自己的作品去编辑页
		uni.navigateTo({
			url: `/pages/event/my-submission?eventId=${eventId.value}`
		})
	} else {
		// 别人的作品跳转详情页
		uni.navigateTo({
			url: `/pages/event/submission-detail?id=${item.id}`
		})
	}
}

const goMySubmission = () => {
	uni.navigateTo({
		url: `/pages/event/my-submission?eventId=${eventId.value}`
	})
}

const goSubmitWork = () => {
	uni.navigateTo({
		url: `/pages/event/my-submission?eventId=${eventId.value}`
	})
}

onLoad((options) => {
	if (options.eventId) {
		eventId.value = options.eventId
		fetchEventInfo()
		fetchSubmissions(true)
	}
})
</script>

<style scoped lang="scss">
.submissions-page {
	min-height: 100vh;
	background: #f8f8f8;
	display: flex;
	flex-direction: column;
}

.page-header {
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	padding: 32rpx 32rpx 28rpx;

	.header-inner {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
	}

	.event-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #1a1a1a;
		display: block;
	}

	.event-sub-wrap {
		display: flex;
		align-items: center;
		gap: 10rpx;
	}

	.sub-dot {
		width: 8rpx;
		height: 8rpx;
		border-radius: 50%;
		background: #00bfff;
	}

	.event-sub {
		font-size: 24rpx;
		color: #999;
		display: block;
	}
}

.submission-list {
	flex: 1;
	padding: 24rpx 28rpx;
}

.loading-wrap {
	padding: 120rpx 0;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 16rpx;

	text {
		font-size: 26rpx;
		color: #999;
	}
}

.loading-spinner {
	width: 48rpx;
	height: 48rpx;
	border: 4rpx solid #f0f0f0;
	border-top-color: #00bfff;
	border-radius: 50%;
	animation: spin 0.8s linear infinite;
}

.loading-spinner-sm {
	width: 32rpx;
	height: 32rpx;
	border: 3rpx solid #f0f0f0;
	border-top-color: #00bfff;
	border-radius: 50%;
	animation: spin 0.8s linear infinite;
}

@keyframes spin {
	to { transform: rotate(360deg); }
}

.empty-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 160rpx 0;
	animation: fadeInUp 0.5s ease forwards;

	.empty-icon-wrap {
		width: 120rpx;
		height: 120rpx;
		background: rgba(0, 191, 255, 0.06);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 24rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999;
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

.submission-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 20rpx;
}

.submission-card {
	width: calc(50% - 10rpx);
	background: #ffffff;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.4s ease forwards;
	opacity: 0;

	&:active {
		transform: scale(0.96);
	}

	.card-cover {
		position: relative;
		width: 100%;
		aspect-ratio: 1;

		.cover-img {
			width: 100%;
			height: 100%;
			transition: opacity 0.4s ease;
		}

		.cover-placeholder {
			width: 100%;
			height: 100%;
			background: #f8f8f8;
			display: flex;
			align-items: center;
			justify-content: center;
		}

		.mine-badge {
			position: absolute;
			top: 16rpx;
			right: 16rpx;
			background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
			padding: 6rpx 16rpx;
			border-radius: 999rpx;
			animation: breatheGlow 2.5s ease-in-out infinite;

			text {
				font-size: 20rpx;
				color: #fff;
				font-weight: 600;
			}
		}
	}

	.card-info {
		padding: 20rpx 24rpx 24rpx;

		.card-title {
			font-size: 28rpx;
			font-weight: 600;
			color: #1a1a1a;
			display: block;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		.card-meta {
			display: flex;
			align-items: center;
			margin-top: 14rpx;

			.author-avatar {
				width: 36rpx;
				height: 36rpx;
				border-radius: 50%;
				margin-right: 10rpx;
			}

			.author-name {
				font-size: 22rpx;
				color: #999;
			}
		}
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

.loading-more {
	padding: 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 12rpx;

	text {
		font-size: 24rpx;
		color: #999;
	}
}

.safe-area-bottom {
	height: calc(env(safe-area-inset-bottom) + 140rpx);
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

	.btn-mine {
		display: flex;
		align-items: center;
		gap: 8rpx;
		padding: 16rpx 28rpx;
		background: rgba(0, 191, 255, 0.08);
		border-radius: 999rpx;
		transition: all 0.2s;

		&:active {
			transform: scale(0.96);
		}

		text {
			font-size: 26rpx;
			color: #00bfff;
			font-weight: 500;
		}
	}

	.btn-submit {
		flex: 1;
		margin-left: 20rpx;
		height: 80rpx;
		background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
		color: #fff;
		border-radius: 999rpx;
		font-size: 28rpx;
		font-weight: 600;
		box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.3);

		&:active {
			transform: scale(0.96);
		}
	}

	.btn-submitted {
		flex: 1;
		margin-left: 20rpx;
		height: 80rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8rpx;
		background: rgba(16, 185, 129, 0.08);
		border-radius: 999rpx;

		text {
			font-size: 26rpx;
			color: #10b981;
			font-weight: 500;
		}
	}
}
</style>
