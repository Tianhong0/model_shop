<template>
	<view class="coupons-page">
		<!-- Tab切换 -->
		<view class="tabs">
			<view
				class="tab-item"
				:class="{ active: activeTab === 'available' }"
				@click="activeTab = 'available'"
			>
				可兑换
			</view>
			<view
				class="tab-item"
				:class="{ active: activeTab === 'my' }"
				@click="activeTab = 'my'"
			>
				我的优惠券
			</view>
		</view>

		<!-- 可兑换列表 -->
		<view v-if="activeTab === 'available'" class="coupon-list">
			<view v-if="templateList.length === 0" class="empty-card">暂无可兑换优惠券</view>
			<view
				class="coupon-item"
				v-for="item in templateList"
				:key="item.id"
			>
				<view class="coupon-left">
					<text class="value">{{ formatValue(item) }}</text>
					<text class="condition">{{ formatCondition(item) }}</text>
				</view>
				<view class="coupon-right">
					<text class="name">{{ item.name }}</text>
					<text class="point">需 {{ item.pointCost }} 积分</text>
					<view
						class="exchange-btn"
						:class="{ disabled: item.remainingStock <= 0 || item.userReceivedCount >= item.perUserLimit }"
						@click="handleExchange(item)"
					>
						{{ getExchangeBtnText(item) }}
					</view>
				</view>
			</view>
		</view>

		<!-- 我的优惠券列表 -->
		<view v-else class="coupon-list">
			<view v-if="myCouponList.length === 0" class="empty-card">暂无优惠券</view>
			<view
				class="coupon-item"
				:class="{ used: item.status !== 0 }"
				v-for="item in myCouponList"
				:key="item.id"
			>
				<view class="coupon-left">
					<text class="value">{{ formatValue(item) }}</text>
					<text class="condition">{{ formatCondition(item) }}</text>
				</view>
				<view class="coupon-right">
					<text class="name">{{ item.name }}</text>
					<text class="expire">{{ formatExpire(item) }}</text>
					<text class="status-tag" :class="getStatusClass(item)">
						{{ getStatusText(item) }}
					</text>
				</view>
			</view>
		</view>

		<!-- 积分信息 -->
		<view class="points-info">
			<text>可用积分：{{ availablePoints }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
	getAvailableTemplatesApi,
	exchangeCouponApi,
	getMyCouponsApi
} from '../../api/coupon'
import { getPointAccountApi } from '../../api/point'

const activeTab = ref('available')
const templateList = ref([])
const myCouponList = ref([])
const availablePoints = ref(0)

// 监听tab切换，重新加载数据
watch(activeTab, async () => {
	await loadData()
})

onShow(async () => {
	if (!ensureLoginOrRedirect()) return
	await loadPoints()
	await loadData()
})

const loadPoints = async () => {
	try {
		const data = await getPointAccountApi()
		availablePoints.value = Number(data?.availablePoints || 0)
	} catch (error) {
		console.error('加载积分失败', error)
	}
}

const loadData = async () => {
	if (activeTab.value === 'available') {
		await loadTemplates()
	} else {
		await loadMyCoupons()
	}
}

const loadTemplates = async () => {
	try {
		const data = await getAvailableTemplatesApi()
		templateList.value = Array.isArray(data?.records) ? data.records : []
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	}
}

const loadMyCoupons = async () => {
	try {
		const data = await getMyCouponsApi({ pageNum: 1, pageSize: 50 })
		myCouponList.value = Array.isArray(data?.records) ? data.records : []
	} catch (error) {
		uni.showToast({ title: error?.message || '加载失败', icon: 'none' })
	}
}

const handleExchange = async (item) => {
	if (item.remainingStock <= 0) {
		uni.showToast({ title: '已抢光', icon: 'none' })
		return
	}
	if (item.userReceivedCount >= item.perUserLimit) {
		uni.showToast({ title: '已达到领取上限', icon: 'none' })
		return
	}
	if (availablePoints.value < item.pointCost) {
		uni.showToast({ title: '积分不足', icon: 'none' })
		return
	}

	uni.showModal({
		title: '确认兑换',
		content: `确定使用 ${item.pointCost} 积分兑换「${item.name}」？`,
		success: async (res) => {
			if (res.confirm) {
				try {
					await exchangeCouponApi(item.id)
					uni.showToast({ title: '兑换成功', icon: 'success' })
					await loadPoints()
					// 刷新两个列表
					await loadTemplates()
					await loadMyCoupons()
				} catch (error) {
					uni.showToast({ title: error?.message || '兑换失败', icon: 'none' })
				}
			}
		}
	})
}

const formatValue = (item) => {
	const type = item.type
	const value = item.value
	if (type === 1) {
		return `¥${value}`
	} else if (type === 2) {
		return `${value * 10}折`
	} else if (type === 3) {
		return `¥${value}`
	}
	return `¥${value}`
}

const formatCondition = (item) => {
	const minAmount = item.minAmount || 0
	if (minAmount > 0) {
		return `满¥${minAmount}可用`
	}
	return '无门槛'
}

const formatExpire = (item) => {
	if (!item.endTime) return ''
	const end = new Date(item.endTime)
	const now = new Date()
	const diff = end - now
	const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
	if (days <= 0) return '已过期'
	if (days <= 7) return `${days}天后过期`
	return `${item.endTime?.slice(0, 10)}到期`
}

const getExchangeBtnText = (item) => {
	if (item.remainingStock <= 0) return '已抢光'
	if (item.userReceivedCount >= item.perUserLimit) return '已领取'
	return '立即兑换'
}

const getStatusClass = (item) => {
	if (item.status === 0) return 'unused'
	if (item.status === 1) return 'used'
	return 'expired'
}

const getStatusText = (item) => {
	if (item.status === 0) return '未使用'
	if (item.status === 1) return '已使用'
	return '已过期'
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

.coupons-page {
	min-height: 100vh;
	padding: 28rpx;
	padding-bottom: 120rpx;
	background: $bg;
}

.tabs {
	display: flex;
	background: $card;
	border-radius: 24rpx;
	padding: 8rpx;
	margin-bottom: 28rpx;
	box-shadow: $shadow;

	.tab-item {
		flex: 1;
		text-align: center;
		padding: 20rpx 0;
		font-size: 28rpx;
		color: $text2;
		border-radius: 16rpx;
		transition: all 0.3s;

		&.active {
			background: $gradient;
			color: #ffffff;
			font-weight: 600;
		}
	}
}

.coupon-list {
	.coupon-item {
		display: flex;
		background: $card;
		border-radius: 24rpx;
		margin-bottom: 20rpx;
		overflow: hidden;
		box-shadow: $shadow;

		&.used {
			opacity: 0.6;

			.coupon-left {
				background: #ccc;
			}
		}
	}

	.coupon-left {
		width: 200rpx;
		background: $gradient;
		padding: 28rpx 20rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		color: #ffffff;

		.value {
			font-size: 48rpx;
			font-weight: 700;
		}

		.condition {
			font-size: 22rpx;
			opacity: 0.9;
			margin-top: 8rpx;
		}
	}

	.coupon-right {
		flex: 1;
		padding: 24rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;

		.name {
			font-size: 28rpx;
			color: $text1;
			font-weight: 500;
		}

		.point {
			font-size: 24rpx;
			color: $text3;
			margin-top: 8rpx;
		}

		.expire {
			font-size: 24rpx;
			color: $text3;
			margin-top: 8rpx;
		}

		.exchange-btn {
			align-self: flex-end;
			background: $gradient;
			color: #ffffff;
			font-size: 24rpx;
			padding: 12rpx 28rpx;
			border-radius: 30rpx;
			margin-top: 12rpx;

			&.disabled {
				background: #ccc;
			}
		}

		.status-tag {
			align-self: flex-start;
			font-size: 22rpx;
			padding: 6rpx 16rpx;
			border-radius: 8rpx;
			margin-top: 12rpx;

			&.unused {
				background: rgba(0, 191, 255, 0.1);
				color: $primary;
			}

			&.used {
				background: rgba(16, 185, 129, 0.1);
				color: $success;
			}

			&.expired {
				background: rgba(255, 77, 109, 0.1);
				color: $danger;
			}
		}
	}
}

.empty-card {
	background: $card;
	border-radius: 24rpx;
	padding: 60rpx 32rpx;
	box-shadow: $shadow;
	font-size: 28rpx;
	color: $text3;
	text-align: center;
}

.points-info {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background: $card;
	padding: 24rpx 28rpx;
	box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
	font-size: 28rpx;
	color: $text2;
	text-align: center;
}
</style>
