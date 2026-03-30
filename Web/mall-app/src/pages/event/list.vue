<template>
	<view class="event-page">
		<!-- 头部 -->
		<view class="page-header">
			<view class="header-bg"></view>
			<view class="header-content">
				<view class="header-row">
					<view class="header-text">
						<text class="header-title">活动赛事</text>
						<text class="header-sub">参与精彩活动，赢取丰厚奖励</text>
					</view>
					<view class="header-action" @tap="goMyEvents">
						<u-icon name="list" size="20" color="#fff"></u-icon>
						<text class="action-text">我的活动</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 状态筛选 -->
		<view class="status-tabs">
			<view
				v-for="(tab, index) in statusTabs"
				:key="index"
				class="status-tab"
				:class="{ active: currentStatus === tab.value }"
				@tap="currentStatus = tab.value"
			>
				<text class="tab-text">{{ tab.label }}</text>
				<view v-if="currentStatus === tab.value" class="tab-indicator"></view>
			</view>
		</view>

		<!-- 活动列表 -->
		<scroll-view scroll-y class="event-list" @scrolltolower="loadMore" lower-threshold="100">
			<view v-if="loading && !events.length" class="loading-wrap">
				<u-skeleton :rows="3" :loading="true" title avatar></u-skeleton>
			</view>

			<view v-else-if="!events.length" class="empty-wrap">
				<view class="empty-icon">
					<u-icon name="calendar" size="60" color="#ccc"></u-icon>
				</view>
				<text class="empty-text">暂无活动</text>
			</view>

			<view v-else class="event-cards-wrap">
				<view
					v-for="(event, index) in events"
					:key="event.id"
					class="event-card"
					:style="{ animationDelay: `${index * 0.1}s` }"
					@tap="goDetail(event.id)"
				>
					<view class="event-cover">
						<image :src="event.bannerUrl || defaultCover" mode="aspectFill" class="cover-img"></image>
						<view class="status-badge" :class="getStatusClass(event.status)">
							{{ getStatusText(event.status) }}
						</view>
					</view>
					<view class="event-info">
						<text class="event-title">{{ event.title }}</text>
						<text class="event-desc">{{ event.description || '暂无描述' }}</text>
						<view class="event-meta">
							<view class="meta-item">
								<u-icon name="clock" size="14" color="#999"></u-icon>
								<text>{{ formatDate(event.startTime) }}</text>
							</view>
							<view class="meta-item">
								<u-icon name="account" size="14" color="#999"></u-icon>
								<text>{{ event.currentParticipants || 0 }}人参与</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view v-if="loadingMore" class="loading-more">
				<u-loadmore status="loading" loadingText="加载中..."></u-loadmore>
			</view>

			<view class="safe-area-bottom"></view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getEventListApi } from '../../api/event'

const defaultCover = 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800'
const loading = ref(false)
const loadingMore = ref(false)
const events = ref([])
const currentStatus = ref('')
const page = ref(1)
const hasMore = ref(true)

const statusTabs = [
	{ label: '全部', value: '' },
	{ label: '进行中', value: 'ongoing' },
	{ label: '即将开始', value: 'upcoming' },
	{ label: '已结束', value: 'ended' }
]

const fetchEvents = async (isRefresh = false) => {
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
		const res = await getEventListApi({
			page: page.value,
			size: 10,
			status: currentStatus.value
		})

		const list = res?.records || res || []

		if (isRefresh) {
			events.value = list
		} else {
			events.value = [...events.value, ...list]
		}

		hasMore.value = list.length >= 10
		page.value++
	} catch (error) {
		uni.showToast({
			title: error.message || '加载失败',
			icon: 'none'
		})
	} finally {
		loading.value = false
		loadingMore.value = false
	}
}

const loadMore = () => {
	if (!loadingMore.value && hasMore.value) {
		fetchEvents()
	}
}

const getStatusClass = (status) => {
	// 后端状态: 0-未开始, 1-报名中, 2-进行中, 3-评审中, 4-已结束
	const map = {
		0: 'status-upcoming',
		1: 'status-ongoing',
		2: 'status-ongoing',
		3: 'status-ongoing',
		4: 'status-ended'
	}
	return map[status] || 'status-ongoing'
}

const getStatusText = (status) => {
	const map = {
		0: '即将开始',
		1: '报名中',
		2: '进行中',
		3: '评审中',
		4: '已结束'
	}
	return map[status] || '进行中'
}

const formatDate = (dateStr) => {
	if (!dateStr) return ''
	const date = new Date(dateStr)
	return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const goDetail = (id) => {
	if (!id) return
	// 防止重复点击
	if (goDetail.lock) return
	goDetail.lock = true
	uni.navigateTo({
		url: `/pages/event/event-detail?id=${id}`,
		complete: () => {
			setTimeout(() => {
				goDetail.lock = false
			}, 500)
		}
	})
}

const goMyEvents = () => {
	uni.navigateTo({
		url: '/pages/event/my-events'
	})
}

watch(currentStatus, () => {
	fetchEvents(true)
})

onShow(() => {
	fetchEvents(true)
})
</script>

<style scoped lang="scss">
.event-page {
	min-height: 100vh;
	background: #f8f8f8;
	display: flex;
	flex-direction: column;
}

.page-header {
	position: relative;
	padding: 48rpx 32rpx;
	background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
	overflow: hidden;
}

.header-bg {
	position: absolute;
	inset: 0;
	background: url('https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800') center/cover;
	opacity: 0.1;
}

.header-content {
	position: relative;
	z-index: 1;
}

.header-row {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
}

.header-text {
	flex: 1;
}

.header-title {
	display: block;
	font-size: 48rpx;
	font-weight: 700;
	color: #fff;
	margin-bottom: 10rpx;
}

.header-action {
	display: flex;
	align-items: center;
	gap: 8rpx;
	padding: 16rpx 28rpx;
	background: rgba(255, 255, 255, 0.22);
	border-radius: 999rpx;
	backdrop-filter: blur(16px);
	transition: all 0.2s;

	&:active {
		transform: scale(0.96);
		background: rgba(255, 255, 255, 0.35);
	}
}

.action-text {
	font-size: 24rpx;
	color: #fff;
	font-weight: 500;
}

.header-sub {
	font-size: 28rpx;
	color: rgba(255, 255, 255, 0.88);
}

.status-tabs {
	display: flex;
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	padding: 20rpx 28rpx;
	gap: 16rpx;
}

.status-tab {
	position: relative;
	padding: 14rpx 30rpx;
	border-radius: 999rpx;
	background: rgba(0, 0, 0, 0.03);
	transition: all 0.3s ease;

	&.active {
		background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.3);

		.tab-text {
			color: #fff;
			font-weight: 600;
		}
	}
}

.tab-text {
	font-size: 28rpx;
	color: #666;
}

.tab-indicator {
	display: none;
}

.event-list {
	flex: 1;
	padding: 24rpx 28rpx;
}

.event-cards-wrap {
	display: flex;
	flex-direction: column;
	gap: 28rpx;
}

.event-card {
	background: #ffffff;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
	animation: fadeInUp 0.5s ease forwards;
	opacity: 0;
	transform: translateY(24rpx);

	&:active {
		transform: scale(0.96);
	}
}

@keyframes fadeInUp {
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.event-cover {
	position: relative;
	height: 340rpx;
	overflow: hidden;
}

.cover-img {
	width: 100%;
	height: 100%;
	transition: opacity 0.4s ease;
}

.status-badge {
	position: absolute;
	top: 24rpx;
	right: 24rpx;
	padding: 8rpx 24rpx;
	border-radius: 999rpx;
	font-size: 22rpx;
	font-weight: 600;
	letter-spacing: 1rpx;
	animation: breatheGlow 2.5s ease-in-out infinite;
}

@keyframes breatheGlow {
	0%, 100% {
		box-shadow: 0 0 12rpx rgba(0, 191, 255, 0.15);
	}
	50% {
		box-shadow: 0 0 24rpx rgba(0, 191, 255, 0.35);
	}
}

.status-ongoing {
	background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
	color: #fff;
}

.status-upcoming {
	background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
	color: #fff;
}

.status-ended {
	background: rgba(156, 163, 175, 0.85);
	color: #fff;
	animation: none;
}

.event-info {
	padding: 28rpx 32rpx 32rpx;
}

.event-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: #1a1a1a;
	margin-bottom: 12rpx;
	line-height: 1.4;
}

.event-desc {
	display: -webkit-box;
	font-size: 28rpx;
	color: #888;
	margin-bottom: 20rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	line-height: 1.5;
}

.event-meta {
	display: flex;
	gap: 32rpx;
}

.meta-item {
	display: flex;
	align-items: center;
	gap: 8rpx;
	font-size: 24rpx;
	color: #999;
}

.loading-wrap {
	padding: 40rpx;
}

.empty-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 160rpx 0;
	animation: fadeInUp 0.5s ease forwards;
}

.empty-icon {
	width: 120rpx;
	height: 120rpx;
	background: rgba(0, 191, 255, 0.06);
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 28rpx;
}

.empty-text {
	font-size: 28rpx;
	color: #999;
}

.loading-more {
	padding: 30rpx;
}

.safe-area-bottom {
	height: calc(env(safe-area-inset-bottom) + 20rpx);
}
</style>
