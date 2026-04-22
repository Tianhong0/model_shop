<template>
	<view class="rank-page">
		<!-- 排行榜列表 -->
		<view class="list-card" v-if="list.length">
			<view class="item" v-for="item in list" :key="item.userId">
				<view class="rank" :class="getRankClass(item.rank)">
					<text>{{ item.rank }}</text>
				</view>
				<image class="avatar" :src="item.avatar || '/static/default-avatar.png'" mode="aspectFill" />
				<view class="info">
					<text class="nickname">{{ item.nickname || '用户' }}</text>
					<text class="count">邀请 {{ item.inviteCount }} 人</text>
				</view>
				<text class="points">{{ item.totalPoints }} 积分</text>
			</view>
		</view>

		<view v-else class="empty">
			<text>暂无排行数据</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getRankListApi } from '../../api/promotion'

const list = ref([])

onShow(async () => {
	await loadData()
})

const loadData = async () => {
	try {
		uni.showLoading({ title: '加载中...' })
		const data = await getRankListApi(20, 'week')
		list.value = data || []
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	} finally {
		uni.hideLoading()
	}
}

const getRankClass = (rank) => {
	if (rank === 1) return 'first'
	if (rank === 2) return 'second'
	if (rank === 3) return 'third'
	return ''
}
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
$danger: #ff4d6d;
$gold: #ffd700;
$silver: #c0c0c0;
$bronze: #cd7f32;
$bg: #f8f8f8;
$card: #ffffff;
$text1: #1a2030;
$text2: #5a6a7a;
$text3: #8a9aaa;
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.rank-page {
	min-height: 100vh;
	padding: 28rpx;
	background: $bg;
}

.list-card {
	background: $card;
	border-radius: 24rpx;
	padding: 8rpx 32rpx;
	box-shadow: $shadow;

	.item {
		display: flex;
		align-items: center;
		padding: 24rpx 0;
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);

		&:last-child {
			border-bottom: none;
		}
	}

	.rank {
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 24rpx;
		font-weight: 700;
		color: $text2;
		background: #f0f0f0;
		margin-right: 20rpx;

		&.first {
			background: $gold;
			color: #ffffff;
		}

		&.second {
			background: $silver;
			color: #ffffff;
		}

		&.third {
			background: $bronze;
			color: #ffffff;
		}
	}

	.avatar {
		width: 72rpx;
		height: 72rpx;
		border-radius: 50%;
		margin-right: 20rpx;
		background: #f0f0f0;
	}

	.info {
		flex: 1;

		.nickname {
			display: block;
			font-size: 28rpx;
			font-weight: 500;
			color: $text1;
		}

		.count {
			display: block;
			font-size: 24rpx;
			color: $text3;
			margin-top: 4rpx;
		}
	}

	.points {
		font-size: 26rpx;
		font-weight: 600;
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
