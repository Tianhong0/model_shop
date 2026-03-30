<template>
	<view class="my-events-container">
		<view class="event-list" v-if="records.length > 0">
			<view
				class="event-card"
				v-for="(item, index) in records"
				:key="index"
				:style="{ animationDelay: `${index * 0.08}s` }"
				@click="goDetail(item.eventId)"
			>
				<view class="card-banner-wrap">
					<image :src="item.eventBanner" class="banner" mode="aspectFill"></image>
					<view class="banner-overlay"></view>
					<view class="status-float" :class="getStatusClass(item.eventStatus)">{{item.eventStatusName}}</view>
				</view>
				<view class="info">
					<view class="title-row">
						<text class="title">{{item.eventTitle}}</text>
					</view>
					<view class="meta">
						<view class="meta-item">
							<text class="meta-label">报名时间</text>
							<text class="meta-value">{{formatTime(item.signupTime)}}</text>
						</view>
						<view class="type-tag">
							<text>{{item.eventTypeName}}</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="loading-state" v-if="loading">
			<view class="loading-spinner"></view>
			<text>加载中...</text>
		</view>

		<view class="load-more-hint" v-if="!loading && records.length && hasMore">
			<text>上拉加载更多</text>
		</view>

		<view class="empty-state" v-if="!loading && records.length === 0">
			<view class="empty-icon-wrap">
				<uni-icons type="calendar" size="52" color="#ccc"></uni-icons>
			</view>
			<text class="empty-text">还没有参加过活动</text>
			<button class="go-btn" @click="goCommunity">去看看活动</button>
		</view>
	</view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyParticipationsApi } from '../../api/event'

const loading = ref(false)
const hasMore = ref(true)
const records = ref([])

const query = reactive({
	pageNum: 1,
	pageSize: 10
})

const statusMap = {
	1: '已报名',
	2: '已签到',
	3: '已提交作品',
	4: '已获奖',
	5: '已取消'
}

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

const canSubmit = (item) => {
	// 设计竞赛类型且已报名未提交作品
	return item.eventType === 1 && item.status === 1 && item.eventStatus === 1
}

const fetchList = async (append = false) => {
	if (loading.value) return
	loading.value = true
	try {
		const res = await getMyParticipationsApi(query)
		const list = Array.isArray(res?.records) ? res.records : []
		records.value = append ? [...records.value, ...list] : list
		const total = Number(res?.total || 0)
		hasMore.value = records.value.length < total
	} catch (error) {
		uni.showToast({ title: error.message || '加载失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const goDetail = (id) => {
	uni.navigateTo({
		url: '/pages/event/event-detail?id=' + id
	})
}

const goCommunity = () => {
	uni.switchTab({
		url: '/pages/community/index'
	})
}

const handleUpload = (item) => {
	uni.showToast({ title: '作品上传功能开发中', icon: 'none' })
}

onShow(() => {
	query.pageNum = 1
	fetchList(false)
})
</script>

<style scoped lang="scss">
.my-events-container {
	min-height: 100vh;
	background: #f8f8f8;
	padding: 24rpx 28rpx;
}

.event-list {
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

	&:active {
		transform: scale(0.96);
	}

	.card-banner-wrap {
		position: relative;
		width: 100%;
		height: 260rpx;
		overflow: hidden;

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
			height: 80rpx;
			background: linear-gradient(to top, rgba(0, 0, 0, 0.15), transparent);
		}

		.status-float {
			position: absolute;
			top: 20rpx;
			right: 20rpx;
			font-size: 22rpx;
			font-weight: 600;
			padding: 8rpx 24rpx;
			border-radius: 999rpx;
			animation: breatheGlow 2.5s ease-in-out infinite;

			&.ongoing {
				background: rgba(16, 185, 129, 0.9);
				color: #ffffff;
			}
			&.ended {
				background: rgba(156, 163, 175, 0.85);
				color: #ffffff;
				animation: none;
			}
			&.judging {
				background: rgba(0, 191, 255, 0.9);
				color: #ffffff;
			}
			&.upcoming {
				background: rgba(0, 191, 255, 0.85);
				color: #ffffff;
			}
		}
	}

	.info {
		padding: 28rpx 32rpx 32rpx;

		.title-row {
			display: flex;
			align-items: flex-start;

			.title {
				flex: 1;
				font-size: 32rpx;
				font-weight: 700;
				color: #1a1a1a;
				line-height: 1.4;
			}
		}

		.meta {
			margin-top: 18rpx;
			display: flex;
			justify-content: space-between;
			align-items: center;

			.meta-item {
				display: flex;
				align-items: center;
				gap: 8rpx;

				.meta-label {
					font-size: 24rpx;
					color: #bbb;
				}

				.meta-value {
					font-size: 24rpx;
					color: #888;
				}
			}

			.type-tag {
				padding: 6rpx 18rpx;
				background: rgba(0, 191, 255, 0.08);
				border-radius: 999rpx;

				text {
					font-size: 22rpx;
					color: #00bfff;
					font-weight: 500;
				}
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

.loading-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 80rpx 0;
	gap: 18rpx;

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

@keyframes spin {
	to { transform: rotate(360deg); }
}

.load-more-hint {
	text-align: center;
	padding: 36rpx;

	text {
		font-size: 24rpx;
		color: #bbb;
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;
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
		margin-bottom: 40rpx;
	}

	.go-btn {
		background: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
		color: #ffffff;
		border-radius: 999rpx;
		font-size: 28rpx;
		font-weight: 600;
		padding: 0 64rpx;
		height: 88rpx;
		box-shadow: 0 8rpx 24rpx rgba(0, 191, 255, 0.3);

		&:active {
			transform: scale(0.96);
		}
	}
}
</style>
