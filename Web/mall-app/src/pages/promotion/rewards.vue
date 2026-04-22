<template>
	<view class="rewards-page">
		<!-- 统计概览 -->
		<view class="summary">
			<text>共获得 {{ totalPoints }} 积分</text>
		</view>

		<!-- 奖励记录列表 -->
		<view class="list-card" v-if="list.length">
			<view class="item" v-for="item in list" :key="item.id">
				<view class="left">
					<text class="type">{{ item.rewardTypeDesc }}</text>
					<text class="time">{{ formatTime(item.createTime) }}</text>
				</view>
				<text class="points">+{{ item.rewardPoints }}</text>
			</view>
		</view>

		<view v-else class="empty">
			<text>暂无奖励记录</text>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getRewardsPageApi } from '../../api/promotion'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(true)

const totalPoints = computed(() => {
	return list.value.reduce((sum, item) => sum + (item.rewardPoints || 0), 0)
})

onShow(async () => {
	if (!ensureLoginOrRedirect()) return
	await loadData(true)
})

onReachBottom(async () => {
	if (hasMore.value) {
		await loadData(false)
	}
})

const loadData = async (refresh = false) => {
	if (refresh) {
		pageNum.value = 1
		list.value = []
		hasMore.value = true
	}

	try {
		uni.showLoading({ title: '加载中...' })
		const data = await getRewardsPageApi({ pageNum: pageNum.value, pageSize })

		const records = data?.records || []
		if (refresh) {
			list.value = records
		} else {
			list.value = [...list.value, ...records]
		}

		total.value = data?.total || 0
		hasMore.value = list.value.length < total.value
		pageNum.value++
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const formatTime = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.rewards-page {
	min-height: 100vh;
	padding: 28rpx;
	background: $bg;
}

.summary {
	background: $card;
	border-radius: 24rpx;
	padding: 24rpx 32rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow;
	font-size: 28rpx;
	color: $text2;
}

.list-card {
	background: $card;
	border-radius: 24rpx;
	padding: 8rpx 32rpx;
	box-shadow: $shadow;

	.item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx 0;
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);

		&:last-child {
			border-bottom: none;
		}
	}

	.left {
		.type {
			display: block;
			font-size: 30rpx;
			font-weight: 500;
			color: $text1;
		}

		.time {
			display: block;
			font-size: 24rpx;
			color: $text3;
			margin-top: 8rpx;
		}
	}

	.points {
		font-size: 32rpx;
		font-weight: 700;
		color: $success;
	}
}

.empty {
	background: $card;
	border-radius: 24rpx;
	padding: 80rpx 32rpx;
	box-shadow: $shadow;
	text-align: center;
	font-size: 28rpx;
	color: $text3;
}
</style>
