<template>
	<view class="appeals-container">
		<view v-if="appealList.length === 0 && !loading" class="empty-tip">
			<text>暂无申诉记录</text>
		</view>

		<view class="section card" v-for="item in appealList" :key="item.id">
			<view class="appeal-top">
				<text class="task-title">{{ item.taskTitle || '悬赏任务' }}</text>
				<text class="status-pill" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
			</view>

			<view class="original-rating">
				<text class="label">原始评价：</text>
				<view class="stars-display">
					<text v-for="s in 5" :key="s" class="star" :class="{ active: s <= item.ratingScore }">★</text>
				</view>
			</view>
			<text class="rating-comment" v-if="item.ratingComment">{{ item.ratingComment }}</text>

			<view class="appeal-reason">
				<text class="label">申诉原因：</text>
				<text class="reason-text">{{ item.reason }}</text>
			</view>

			<view class="evidence-images" v-if="item.evidence && item.evidence.length">
				<text class="label">证据材料：</text>
				<view class="img-row">
					<image
						v-for="(img, idx) in parseEvidence(item.evidence)"
						:key="idx"
						:src="img"
						class="evidence-img"
						mode="aspectFill"
						@click="previewImage(parseEvidence(item.evidence), idx)"
					/>
				</view>
			</view>

			<view class="admin-result" v-if="item.status !== 0">
				<view class="admin-row" v-if="item.adminRemark">
					<text class="label">管理员备注：</text>
					<text class="admin-text">{{ item.adminRemark }}</text>
				</view>
				<text class="processed-time" v-if="item.processedTime">处理时间：{{ formatTime(item.processedTime) }}</text>
			</view>

			<text class="create-time">提交时间：{{ formatTime(item.createTime) }}</text>
		</view>

		<view v-if="hasMore" class="load-more" @click="loadMore">
			<text>加载更多</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getMyBountyAppealsApi } from '../../api/reward'

const appealList = ref([])
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(false)
const loading = ref(false)

const statusText = (status) => {
	if (status === 0) return '待处理'
	if (status === 1) return '已通过'
	if (status === 2) return '已驳回'
	return '未知'
}

const statusClass = (status) => {
	if (status === 0) return 'pending'
	if (status === 1) return 'approved'
	if (status === 2) return 'rejected'
	return ''
}

const loadAppeals = async () => {
	loading.value = true
	try {
		const data = await getMyBountyAppealsApi(pageNum.value, pageSize)
		const records = data?.records || []
		if (pageNum.value === 1) {
			appealList.value = records
		} else {
			appealList.value.push(...records)
		}
		hasMore.value = records.length >= pageSize
	} catch (error) {
		uni.showToast({ title: error?.message || '加载申诉失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const loadMore = () => {
	pageNum.value++
	loadAppeals()
}

const parseEvidence = (evidence) => {
	if (Array.isArray(evidence)) return evidence
	if (typeof evidence === 'string') return evidence.split(',').filter(Boolean)
	return []
}

const formatTime = (time) => {
	if (!time) return ''
	return String(time).substring(0, 16).replace('T', ' ')
}

const previewImage = (urls, idx) => {
	uni.previewImage({ current: idx, urls })
}

onLoad(() => {
	loadAppeals()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$light: #5ce1ff;
$deep: #0099cc;
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
@keyframes breathGlow {
	0%, 100% { box-shadow: 0 0 12rpx rgba(0,191,255,0.15); }
	50% { box-shadow: 0 0 24rpx rgba(0,191,255,0.35); }
}

.appeals-container {
	min-height: 100vh;
	background-color: $bg;
	padding-bottom: 48rpx;
}

.section {
	margin: 28rpx 32rpx;
	padding: 32rpx;
	background-color: $card;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease-out both;
}

.empty-tip {
	text-align: center;
	padding: 140rpx 0;
	text { font-size: 28rpx; color: $text-muted; }
}

.appeal-top {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20rpx;
	.task-title { font-size: 30rpx; font-weight: 700; color: $text-primary; flex: 1; }
}

.status-pill {
	font-size: 22rpx;
	padding: 6rpx 20rpx;
	border-radius: 999rpx;
	font-weight: 500;
	flex-shrink: 0;
	margin-left: 16rpx;
	&.pending {
		background-color: rgba(255, 153, 0, 0.1); color: #ff9900;
		animation: breathGlow 2s ease-in-out infinite;
	}
	&.approved { background-color: rgba(16,185,129,0.1); color: $success; }
	&.rejected { background-color: rgba(255,77,109,0.1); color: $danger; }
}

.original-rating {
	display: flex;
	align-items: center;
	gap: 10rpx;
	margin-bottom: 12rpx;
	.label { font-size: 24rpx; color: $text-muted; }
}

.stars-display { display: flex; gap: 4rpx; }
.star { font-size: 26rpx; color: #e0e0e0; &.active { color: #ffc107; } }

.rating-comment {
	font-size: 26rpx;
	color: $text-secondary;
	line-height: 1.5;
	display: block;
	margin-bottom: 16rpx;
	padding: 16rpx 20rpx;
	background: $bg;
	border-radius: 16rpx;
}

.appeal-reason {
	margin-bottom: 16rpx;
	.label { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 8rpx; }
	.reason-text { font-size: 26rpx; color: $text-primary; line-height: 1.6; }
}

.evidence-images {
	margin-bottom: 16rpx;
	.label { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 12rpx; }
	.img-row { display: flex; gap: 14rpx; flex-wrap: wrap; }
	.evidence-img {
		width: 120rpx; height: 120rpx; border-radius: 16rpx;
		opacity: 0; animation: fadeInUp 0.3s ease-out forwards;
	}
}

.admin-result {
	margin-top: 16rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid rgba(0,0,0,0.04);
	.admin-row {
		margin-bottom: 10rpx;
		.label { font-size: 24rpx; color: $text-muted; }
		.admin-text { font-size: 26rpx; color: $text-primary; }
	}
	.processed-time { font-size: 22rpx; color: $text-muted; display: block; }
}

.create-time {
	font-size: 22rpx;
	color: $text-muted;
	display: block;
	margin-top: 12rpx;
}

.load-more {
	text-align: center;
	padding: 24rpx 0;
	text { font-size: 26rpx; color: $primary; font-weight: 500; }
	&:active { opacity: 0.7; }
}
</style>
