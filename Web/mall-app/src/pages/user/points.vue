<template>
	<view class="points-page">
		<view class="summary-card">
			<text class="title">可用积分</text>
			<text class="amount">{{ account.availablePoints }}</text>
			<view class="meta-row">
				<text>累计获得 {{ account.totalEarned }}</text>
				<text>累计消耗 {{ account.totalSpent }}</text>
			</view>
			<view class="exchange-btn" @click="goToCoupons">
				兑换优惠券
			</view>
		</view>

		<view class="list-card" v-if="list.length">
			<view class="item" v-for="item in list" :key="item.id">
				<view class="left">
					<text class="name">{{ formatBiz(item) }}</text>
					<text class="time">{{ formatTime(item.createTime) }}</text>
				</view>
				<text class="value" :class="Number(item.direction) === 1 ? 'in' : 'out'">
					{{ Number(item.direction) === 1 ? '+' : '-' }}{{ item.points }}
				</text>
			</view>
		</view>
		<view v-else class="empty-card">暂无积分流水</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getPointAccountApi, getPointLedgerPageApi } from '../../api/point'

const account = ref({
	availablePoints: 0,
	totalEarned: 0,
	totalSpent: 0
})

const list = ref([])

onShow(async () => {
	if (!ensureLoginOrRedirect()) return
	await Promise.all([loadAccount(), loadLedger()])
})

const loadAccount = async () => {
	try {
		const data = await getPointAccountApi()
		account.value = {
			availablePoints: Number(data?.availablePoints || 0),
			totalEarned: Number(data?.totalEarned || 0),
			totalSpent: Number(data?.totalSpent || 0)
		}
	} catch (error) {
		uni.showToast({ title: error?.message || '积分账户加载失败', icon: 'none' })
	}
}

const loadLedger = async () => {
	try {
		const data = await getPointLedgerPageApi({ pageNum: 1, pageSize: 30 })
		list.value = Array.isArray(data?.records) ? data.records : []
	} catch (error) {
		uni.showToast({ title: error?.message || '积分流水加载失败', icon: 'none' })
	}
}

const formatBiz = (item) => {
	const map = {
		ORDER_PAY: '订单支付奖励',
		ORDER_POINT_DEDUCT: '订单积分抵扣',
		ORDER_POINT_REFUND: '订单取消返还积分',
		BOUNTY_RELEASE: '悬赏验收奖励',
		ECO_MATERIAL: '环保材料奖励',
		COUPON_EXCHANGE: '兑换优惠券',
		COUPON_REFUND: '优惠券返还',
		POST_REPLY_ADOPTED: '回答被采纳奖励',
		POST_REPLY_EXCELLENT: '优质回答奖励',
		USED_ORDER_SELL: '二手交易奖励'
	}
	return map[item?.bizType] || item?.remark || item?.bizType || '积分变动'
}

const formatTime = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 19)
}

const goToCoupons = () => {
	uni.navigateTo({ url: '/pages/user/coupons' })
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
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);

.points-page {
	min-height: 100vh;
	padding: 28rpx;
	background: $bg;
}

.summary-card {
	background: $gradient;
	border-radius: 24rpx;
	padding: 32rpx;
	padding-bottom: 100rpx;
	margin-bottom: 28rpx;
	box-shadow: 0 12rpx 40rpx rgba(0, 191, 255, 0.2);
	color: #ffffff;
	position: relative;
	.title { font-size: 24rpx; opacity: 0.9; }
	.amount { display: block; margin-top: 14rpx; font-size: 56rpx; font-weight: 700; }
	.meta-row {
		margin-top: 18rpx;
		display: flex;
		gap: 48rpx;
		font-size: 24rpx;
		opacity: 0.85;
	}
	.exchange-btn {
		position: absolute;
		right: 32rpx;
		bottom: 32rpx;
		background: rgba(255, 255, 255, 0.2);
		border: 1rpx solid rgba(255, 255, 255, 0.4);
		color: #ffffff;
		font-size: 24rpx;
		padding: 12rpx 24rpx;
		border-radius: 30rpx;
	}
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
		& + .item { border-top: 1rpx solid rgba(0,0,0,0.04); }
	}
	.name { display: block; font-size: 28rpx; color: $text1; font-weight: 500; }
	.time { display: block; margin-top: 8rpx; font-size: 22rpx; color: $text3; }
	.value { font-size: 30rpx; font-weight: 700; }
	.in { color: $success; }
	.out { color: $danger; }
}

.empty-card {
	background: $card;
	border-radius: 24rpx;
	padding: 40rpx 32rpx;
	box-shadow: $shadow;
	font-size: 28rpx;
	color: $text3;
	text-align: center;
}
</style>
