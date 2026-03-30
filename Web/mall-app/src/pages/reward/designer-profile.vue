<template>
	<view class="profile-container">
		<view class="section card" v-if="reputation">
			<view class="rep-header">
				<view class="avatar">{{ designerName.slice(0, 1) }}</view>
				<view class="rep-info">
					<text class="name">{{ designerName }}</text>
					<view class="rep-stats">
						<view class="stat">
							<text class="stat-val">{{ reputation.reputationScore || 0 }}</text>
							<text class="stat-label">信誉分</text>
						</view>
						<view class="stat">
							<text class="stat-val">{{ reputation.totalTasks || 0 }}</text>
							<text class="stat-label">完成任务</text>
						</view>
						<view class="stat">
							<text class="stat-val">{{ reputation.avgScore || '0.0' }}</text>
							<text class="stat-label">平均评分</text>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="section card" v-if="reputation">
			<view class="sec-title">评分分布</view>
			<view class="star-row" v-for="i in 5" :key="i">
				<text class="star-label">{{ 6 - i }}星</text>
				<view class="bar-bg">
					<view class="bar-fill" :style="{ width: starPercent(6 - i) + '%' }"></view>
				</view>
				<text class="star-count">{{ starCount(6 - i) }}</text>
			</view>
		</view>

		<view class="section card">
			<view class="sec-title">评价列表</view>
			<view v-if="ratingList.length === 0" class="empty-tip">暂无评价</view>
			<view class="rating-item" v-for="item in ratingList" :key="item.id">
				<view class="rating-top">
					<text class="publisher">{{ item.isAnonymous === 1 ? '匿名用户' : (item.publisherName || '用户') }}</text>
					<view class="stars-display">
						<text v-for="s in 5" :key="s" class="star" :class="{ active: s <= item.score }">★</text>
					</view>
					<text class="time">{{ formatTime(item.createTime) }}</text>
				</view>
				<text class="task-title" v-if="item.taskTitle">任务：{{ item.taskTitle }}</text>
				<text class="comment" v-if="item.comment">{{ item.comment }}</text>
				<view class="rating-images" v-if="item.images && item.images.length">
					<image
						v-for="(img, idx) in parseImages(item.images)"
						:key="idx"
						:src="img"
						class="rating-img"
						mode="aspectFill"
						@click="previewImage(parseImages(item.images), idx)"
					/>
				</view>
			</view>
			<view v-if="hasMore" class="load-more" @click="loadMore">
				<text>加载更多</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getDesignerReputationApi, getDesignerRatingsApi } from '../../api/reward'

const designerId = ref(null)
const designerName = ref('设计者')
const reputation = ref(null)
const ratingList = ref([])
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(false)

const loadReputation = async () => {
	try {
		const data = await getDesignerReputationApi(designerId.value)
		reputation.value = data
		if (data?.designerName) {
			designerName.value = data.designerName
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '加载信誉失败', icon: 'none' })
	}
}

const loadRatings = async () => {
	try {
		const data = await getDesignerRatingsApi(designerId.value, pageNum.value, pageSize)
		const records = data?.records || []
		if (pageNum.value === 1) {
			ratingList.value = records
		} else {
			ratingList.value.push(...records)
		}
		hasMore.value = records.length >= pageSize
	} catch (error) {
		uni.showToast({ title: error?.message || '加载评价失败', icon: 'none' })
	}
}

const loadMore = () => {
	pageNum.value++
	loadRatings()
}

const starCount = (star) => {
	if (!reputation.value) return 0
	const map = {
		5: reputation.value.fiveStarCount,
		4: reputation.value.fourStarCount,
		3: reputation.value.threeStarCount,
		2: reputation.value.twoStarCount,
		1: reputation.value.oneStarCount
	}
	return map[star] || 0
}

const starPercent = (star) => {
	const total = reputation.value?.totalRatings || 0
	if (total === 0) return 0
	return Math.round((starCount(star) / total) * 100)
}

const parseImages = (images) => {
	if (Array.isArray(images)) return images
	if (typeof images === 'string') return images.split(',').filter(Boolean)
	return []
}

const formatTime = (time) => {
	if (!time) return ''
	return String(time).substring(0, 10)
}

const previewImage = (urls, idx) => {
	uni.previewImage({ current: idx, urls })
}

onLoad((options) => {
	if (options?.designerId) {
		designerId.value = options.designerId
		designerName.value = `设计者#${options.designerId}`
		loadReputation()
		loadRatings()
	}
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
@keyframes jellyPop {
	0% { transform: scale(1); }
	30% { transform: scale(1.12); }
	50% { transform: scale(0.95); }
	70% { transform: scale(1.04); }
	100% { transform: scale(1); }
}

.profile-container {
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
	.sec-title { font-size: 30rpx; font-weight: 700; color: $text-primary; margin-bottom: 24rpx; }
}

.rep-header {
	display: flex;
	gap: 28rpx;
	align-items: center;
	.avatar {
		width: 108rpx;
		height: 108rpx;
		border-radius: 50%;
		background: $gradient;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		font-size: 42rpx;
		font-weight: 700;
		box-shadow: 0 8rpx 24rpx rgba(0,191,255,0.2);
		animation: jellyPop 0.6s ease-out;
	}
	.rep-info { flex: 1; }
	.name { font-size: 32rpx; font-weight: 700; color: $text-primary; display: block; margin-bottom: 16rpx; }
}

.rep-stats {
	display: flex;
	gap: 32rpx;
	.stat { text-align: center; }
	.stat-val { font-size: 32rpx; font-weight: 700; color: $primary; display: block; }
	.stat-label { font-size: 22rpx; color: $text-muted; margin-top: 4rpx; }
}

.star-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
	margin-bottom: 16rpx;
	.star-label { font-size: 24rpx; color: $text-secondary; width: 64rpx; }
	.bar-bg {
		flex: 1;
		height: 18rpx;
		background-color: $bg;
		border-radius: 999rpx;
		overflow: hidden;
	}
	.bar-fill {
		height: 100%;
		background: $gradient;
		border-radius: 999rpx;
		transition: width 0.4s ease-out;
	}
	.star-count { font-size: 22rpx; color: $text-muted; width: 56rpx; text-align: right; }
}

.empty-tip { font-size: 26rpx; color: $text-muted; text-align: center; padding: 48rpx 0; }

.rating-item {
	padding: 24rpx 0;
	& + .rating-item {
		border-top: 1rpx solid rgba(0,0,0,0.04);
	}
	.rating-top {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-bottom: 12rpx;
	}
	.publisher { font-size: 28rpx; color: $text-primary; font-weight: 600; }
	.stars-display { display: flex; gap: 4rpx; }
	.star { font-size: 28rpx; color: #e0e0e0; &.active { color: #ffc107; } }
	.time { font-size: 22rpx; color: $text-muted; margin-left: auto; }
	.task-title { font-size: 24rpx; color: $primary; display: block; margin-bottom: 8rpx; }
	.comment { font-size: 26rpx; color: $text-secondary; line-height: 1.6; display: block; }
}

.rating-images {
	display: flex; gap: 12rpx; flex-wrap: wrap; margin-top: 16rpx;
	.rating-img {
		width: 120rpx; height: 120rpx; border-radius: 16rpx;
		opacity: 0; animation: fadeInUp 0.3s ease-out forwards;
	}
}

.load-more {
	text-align: center;
	padding: 24rpx 0;
	text { font-size: 26rpx; color: $primary; font-weight: 500; }
	&:active { opacity: 0.7; }
}
</style>
