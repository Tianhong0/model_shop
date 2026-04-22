<template>
	<view class="invitees-page">
		<!-- 统计概览 -->
		<view class="summary">
			<text>共邀请 {{ total }} 位好友</text>
		</view>

		<!-- 邀请人列表 -->
		<view class="list-card" v-if="list.length">
			<view class="item" v-for="item in list" :key="item.userId">
				<image class="avatar" :src="item.avatar || '/static/default-avatar.png'" mode="aspectFill" />
				<view class="info">
					<text class="nickname">{{ item.nickname || '用户' }}</text>
					<text class="time">注册于 {{ formatTime(item.registerTime) }}</text>
					<view class="stats">
						<text>订单: {{ item.orderCount || 0 }}</text>
						<text>贡献: {{ item.contributedPoints || 0 }}积分</text>
					</view>
				</view>
			</view>
		</view>

		<view v-else class="empty">
			<text>暂无邀请记录</text>
			<text class="tip">分享邀请码给好友，邀请成功后即可获得奖励</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getInviteesPageApi } from '../../api/promotion'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(true)

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
		const data = await getInviteesPageApi({ pageNum: pageNum.value, pageSize })

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
	return String(value).replace('T', ' ').slice(0, 10)
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

.invitees-page {
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
		padding: 24rpx 0;
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);

		&:last-child {
			border-bottom: none;
		}
	}

	.avatar {
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		margin-right: 24rpx;
		background: #f0f0f0;
	}

	.info {
		flex: 1;

		.nickname {
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

		.stats {
			margin-top: 12rpx;
			display: flex;
			gap: 24rpx;
			font-size: 24rpx;
			color: $text2;
		}
	}
}

.empty {
	background: $card;
	border-radius: 24rpx;
	padding: 80rpx 32rpx;
	box-shadow: $shadow;
	text-align: center;

	text {
		display: block;
		font-size: 28rpx;
		color: $text3;
	}

	.tip {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: $text3;
	}
}
</style>
