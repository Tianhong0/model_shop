<template>
	<view class="points-page">
		<view class="summary card">
			<text class="title">可用积分</text>
			<text class="amount">{{ account.availablePoints }}</text>
			<view class="meta-row">
				<text>累计获得 {{ account.totalEarned }}</text>
				<text>累计消耗 {{ account.totalSpent }}</text>
			</view>
		</view>

		<view class="list card" v-if="list.length">
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
		<view v-else class="card empty">暂无积分流水</view>
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
		BOUNTY_RELEASE: '悬赏验收奖励'
	}
	return map[item?.bizType] || item?.remark || item?.bizType || '积分变动'
}

const formatTime = (value) => {
	if (!value) return '--'
	return String(value).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped lang="scss">
.points-page {
	min-height: 100vh;
	padding: 24rpx;
	background: #f8fafc;
}

.card {
	background: #fff;
	border-radius: 18rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
}

.summary {
	.title { font-size: 24rpx; color: #64748b; }
	.amount { display: block; margin-top: 14rpx; font-size: 56rpx; font-weight: 700; color: #1e293b; }
	.meta-row {
		margin-top: 14rpx;
		display: flex;
		justify-content: space-between;
		font-size: 24rpx;
		color: #64748b;
	}
}

.list {
	.item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 20rpx 0;
		border-bottom: 2rpx solid #f1f5f9;
		&:last-child { border-bottom: none; }
	}
	.name { display: block; font-size: 28rpx; color: #1e293b; }
	.time { display: block; margin-top: 6rpx; font-size: 22rpx; color: #94a3b8; }
	.value { font-size: 30rpx; font-weight: 700; }
	.in { color: #10b981; }
	.out { color: #ef4444; }
}

.empty {
	font-size: 24rpx;
	color: #94a3b8;
}
</style>
