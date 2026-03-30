<template>
	<view class="order-list-container">
		<!-- Glass Tab Bar -->
		<view class="tabs-wrap">
			<view class="tabs">
				<view
					v-for="(tab, index) in tabs"
					:key="index"
					class="tab-item"
					:class="{ active: activeTab === index }"
					@click="activeTab = index"
				>
					{{tab}}
				</view>
			</view>
		</view>

		<scroll-view scroll-y class="order-scroll">
			<view v-if="displayOrders.length === 0" class="empty-orders">
				<view class="empty-icon">📦</view>
				<text class="empty-text">暂无相关订单</text>
			</view>

			<view class="order-card fadeInUp" v-for="(order, index) in displayOrders" :key="index" @click="goDetail(order)">
				<view class="order-header">
					<text class="order-no">订单号: {{order.no}}</text>
					<view class="status-badge">
						<text class="status">{{order.statusText}}</text>
					</view>
				</view>
				<view class="goods-list">
					<view class="goods-item" v-for="(item, idx) in order.items" :key="idx">
						<image :src="item.image" mode="aspectFill" class="goods-img"></image>
						<view class="goods-info">
							<text class="name">{{item.name}}</text>
							<text class="params">{{item.params}}</text>
							<view class="price-row">
								<text class="price">￥{{item.price}}</text>
								<text class="num">x{{item.num}}</text>
							</view>
						</view>
					</view>
				</view>
				<view class="order-footer">
					<text class="total">合计: <text class="amount">￥{{order.totalPrice}}</text></text>
					<view class="btns">
						<template v-if="order.status === 0">
							<button class="btn" @click.stop="handleAction('cancel', order, index)">取消订单</button>
							<button class="btn primary" @click.stop="handleAction('pay', order, index)">去支付</button>
						</template>
						<template v-else-if="order.status === 2">
							<button class="btn" @click.stop="goLogistics(order)">{{ order.allowLogistics ? '查看物流' : '查看详情' }}</button>
							<button class="btn primary" @click.stop="handleAction('confirm', order, index)">确认收货</button>
						</template>
						<template v-else-if="order.status === 3">
							<button class="btn" @click.stop="handleAction(order.allowDelete ? 'delete' : 'detail', order, index)">{{ order.allowDelete ? '删除订单' : '查看详情' }}</button>
							<button v-if="order.allowComment" class="btn primary" :disabled="order.hasComment" :class="{ disabled: order.hasComment }" @click.stop="handleAction('comment', order, index)">{{ order.hasComment ? '已评价' : '立即评价' }}</button>
							<button v-else class="btn primary" @click.stop="goDetail(order)">再次查看</button>
						</template>
						<template v-else-if="order.status === 4">
							<button class="btn" @click.stop="handleAction(order.allowDelete ? 'delete' : 'detail', order, index)">{{ order.allowDelete ? '删除订单' : '查看详情' }}</button>
						</template>
						<template v-else-if="order.status === 1">
							<button class="btn" @click.stop="goDetail(order)">查看详情</button>
						</template>
						<template v-else>
							<button class="btn" @click.stop="goDetail(order)">查看详情</button>
						</template>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getMyOrdersApi, cancelOrderApi, deleteOrderApi, confirmOrderReceiveApi, getMyOrderCommentsApi } from '../../api/order'
import { cancelUsedOrderApi, confirmUsedOrderApi, getMyBuyUsedOrderPageApi } from '../../api/used'

const tabs = ['全部', '待付款', '待收货', '已完成', '已取消']
const activeTab = ref(0)
const loading = ref(false)

const orders = ref([])

const resolveTabByStatus = (statusValue) => {
	const status = Number(statusValue)
	if (status === 0) return 0
	if (status === 1) return 1
	if (status === 2) return 2
	if (status === 3) return 3
	if (status === 4) return 4
	return 0
}

onLoad((options) => {
	activeTab.value = resolveTabByStatus(options?.status)
})

onShow(() => {
	if (!ensureLoginOrRedirect()) return
	loadOrders()
})

watch(activeTab, () => {
	loadOrders()
})

const formatStatusText = (status) => {
	switch (Number(status)) {
		case 0: return '待付款'
		case 1: return '生产中'
		case 2: return '待收货'
		case 3: return '已完成'
		case 4: return '已取消'
		default: return '未知状态'
	}
}

const formatUsedStatusText = (status) => {
	switch (Number(status)) {
		case 0: return '待付款'
		case 1: return '待发货'
		case 2: return '待收货'
		case 3: return '已完成'
		case 4: return '已取消'
		case 5: return '售后中'
		default: return '未知状态'
	}
}

const mapOrderItem = (record) => {
	const totalPrice = Number(record?.orderPrice || 0).toFixed(2)
	return {
		orderType: 'normal',
		orderKey: `normal-${record?.id || record?.orderSn || ''}`,
		id: record?.id,
		no: record?.orderSn || '',
		status: Number(record?.orderStatus ?? -1),
		statusText: formatStatusText(record?.orderStatus),
		hasComment: false,
		allowDelete: true,
		allowComment: true,
		allowLogistics: true,
		sortTime: record?.createTime || record?.payTime || '',
		totalPrice,
		items: [
			{
				name: record?.modelName || '模型订单',
				params: record?.orderSn || '',
				price: totalPrice,
				num: 1,
				image: record?.mainImageUrl || 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200'
			}
		]
	}
}

const mapUsedOrderItem = (record) => {
	const totalPrice = Number(record?.orderAmount || 0).toFixed(2)
	const status = Number(record?.status ?? -1)
	const sellerName = record?.sellerNickname ? `卖家：${record.sellerNickname}` : '二手交易订单'
	return {
		orderType: 'used',
		orderKey: `used-${record?.id || record?.orderSn || ''}`,
		id: record?.id,
		no: record?.orderSn || '',
		status,
		statusText: formatUsedStatusText(status),
		hasComment: true,
		allowDelete: false,
		allowComment: false,
		allowLogistics: status === 2,
		sortTime: record?.createTime || record?.payTime || '',
		totalPrice,
		items: [
			{
				name: record?.listingTitle || '二手商品订单',
				params: sellerName,
				price: totalPrice,
				num: 1,
				image: record?.coverUrl || 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=200'
			}
		]
	}
}

const sortOrderList = (list = []) => {
	return [...list].sort((a, b) => {
		const aTime = a?.sortTime ? new Date(a.sortTime).getTime() : 0
		const bTime = b?.sortTime ? new Date(b.sortTime).getTime() : 0
		return bTime - aTime
	})
}

const buildMainOrderQuery = () => ({
	pageNum: 1,
	pageSize: 100,
	orderStatus: activeTab.value === 1
		? 0
		: activeTab.value === 3
			? 3
			: activeTab.value === 4
				? 4
				: undefined
})

const buildUsedOrderQuery = () => ({
	pageNum: 1,
	pageSize: 100,
	status: activeTab.value === 1
		? 0
		: activeTab.value === 3
			? 3
			: activeTab.value === 4
				? 4
				: undefined
})

const loadOrders = async () => {
	loading.value = true
	try {
		const [normalResult, usedResult] = await Promise.all([
			getMyOrdersApi(buildMainOrderQuery()),
			getMyBuyUsedOrderPageApi(buildUsedOrderQuery())
		])
		const normalRecords = Array.isArray(normalResult?.records) ? normalResult.records : []
		const usedRecords = Array.isArray(usedResult?.records) ? usedResult.records : []
		const mapped = sortOrderList([
			...normalRecords.map(mapOrderItem),
			...usedRecords.map(mapUsedOrderItem)
		])

		if (mapped.some(item => item.status === 3)) {
			const commentData = await getMyOrderCommentsApi({ pageNum: 1, pageSize: 500 })
			const commentRecords = Array.isArray(commentData?.records) ? commentData.records : []
			const commentedOrderSet = new Set(commentRecords.map(item => String(item?.orderId || '')))
			mapped.forEach(item => {
				if (item.orderType === 'normal') {
					item.hasComment = commentedOrderSet.has(String(item.id || ''))
				}
			})
		}

		orders.value = mapped
	} catch (error) {
		orders.value = []
		uni.showToast({ title: error.message || '加载订单失败', icon: 'none' })
	} finally {
		loading.value = false
	}
}

const displayOrders = computed(() => {
	if (activeTab.value === 2) {
		return orders.value.filter(o => o.status === 1 || o.status === 2)
	}
	return orders.value
})

const goDetail = (order) => {
	if (!order?.no) {
		uni.showToast({ title: '订单号无效', icon: 'none' })
		return
	}
	if (order.orderType === 'used') {
		uni.navigateTo({
			url: `/pages/used/order-detail?id=${encodeURIComponent(String(order.id || ''))}&role=buy`
		})
		return
	}
	uni.navigateTo({
		url: `/pages/user/order-detail?orderSn=${encodeURIComponent(order.no)}&id=${encodeURIComponent(order.id || '')}`
	})
}

const goLogistics = (order) => {
	if (order?.orderType === 'used') {
		goDetail(order)
		return
	}
	if (!order?.no) {
		uni.showToast({ title: '订单号无效', icon: 'none' })
		return
	}
	uni.navigateTo({
		url: `/pages/user/logistics-detail?orderSn=${encodeURIComponent(order.no)}`
	})
}

const handleAction = async (type, order, index) => {
	if (type === 'pay') {
		goDetail(order)
	} else if (type === 'cancel') {
		uni.showModal({
			title: '提示',
			content: '确定取消该订单吗？',
			success: async (res) => {
				if (res.confirm) {
					try {
						if (order.orderType === 'used') {
							await cancelUsedOrderApi(order.id)
						} else {
							await cancelOrderApi(order.id)
						}
						uni.showToast({ title: '订单已取消', icon: 'success' })
						loadOrders()
					} catch (error) {
						uni.showToast({ title: error.message || '取消失败', icon: 'none' })
					}
				}
			}
		})
	} else if (type === 'confirm') {
		uni.showModal({
			title: '确认收货',
			content: '确认已收到货物并完成本次订单吗？',
			success: async (res) => {
				if (!res.confirm) {
					return
				}
				try {
					if (order.orderType === 'used') {
						await confirmUsedOrderApi(order.id)
					} else {
						await confirmOrderReceiveApi(order.no)
					}
					uni.showToast({ title: '已确认收货', icon: 'success' })
					await loadOrders()
				} catch (error) {
					uni.showToast({ title: error.message || '确认收货失败', icon: 'none' })
				}
			}
		})
	} else if (type === 'delete') {
		if (order.orderType === 'used') {
			goDetail(order)
			return
		}
		uni.showModal({
			title: '提示',
			content: '确认删除该订单吗？删除后不可恢复',
			success: async (res) => {
				if (!res.confirm) {
					return
				}
				try {
					await deleteOrderApi(order.id)
					orders.value = orders.value.filter(item => item.id !== order.id)
					uni.showToast({ title: '订单已删除', icon: 'success' })
					await loadOrders()
				} catch (error) {
					uni.showToast({ title: error.message || '删除失败', icon: 'none' })
				}
			}
		})
	} else if (type === 'comment') {
		if (order.orderType === 'used') {
			goDetail(order)
			return
		}
		if (order?.hasComment) {
			uni.showToast({ title: '该订单已评价', icon: 'none' })
			return
		}
		uni.navigateTo({ url: `/pages/user/order-comment-create?orderId=${order.id}` })
	} else if (type === 'detail') {
		goDetail(order)
	}
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

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.order-list-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $bg;
}

.tabs-wrap {
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
}

.tabs {
	display: flex;
	padding: 0 24rpx;

	.tab-item {
		flex: 1;
		height: 96rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28rpx;
		color: $text2;
		position: relative;
		transition: color 0.25s;

		&.active {
			color: $primary;
			font-weight: 700;

			&::after {
				content: '';
				position: absolute;
				bottom: 0;
				left: 30%;
				right: 30%;
				height: 6rpx;
				background: $gradient;
				border-radius: 999rpx;
			}
		}
	}
}

.order-scroll {
	flex: 1;
	padding: 8rpx 0;
}

.empty-orders {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;

	.empty-icon {
		font-size: 120rpx;
		margin-bottom: 24rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: $text3;
	}
}

.fadeInUp {
	animation: fadeInUp 0.4s ease both;
}

.order-card {
	background-color: $card;
	margin: 20rpx 28rpx;
	border-radius: 24rpx;
	padding: 32rpx;
	box-shadow: $shadow;

	.order-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-bottom: 24rpx;

		.order-no {
			font-size: 24rpx;
			color: $text3;
		}

		.status-badge {
			.status {
				font-size: 24rpx;
				color: $primary;
				font-weight: 700;
				background: rgba(0, 191, 255, 0.08);
				padding: 6rpx 20rpx;
				border-radius: 999rpx;
			}
		}
	}

	.goods-list {
		padding: 24rpx 0;

		.goods-item {
			display: flex;
			margin-bottom: 20rpx;

			&:last-child {
				margin-bottom: 0;
			}

			.goods-img {
				width: 120rpx;
				height: 120rpx;
				border-radius: 16rpx;
				background-color: $bg;
			}

			.goods-info {
				flex: 1;
				margin-left: 20rpx;

				.name {
					font-size: 28rpx;
					font-weight: 600;
					color: $text1;
					display: block;
				}

				.params {
					font-size: 24rpx;
					color: $text3;
					margin-top: 6rpx;
					display: block;
				}

				.price-row {
					margin-top: 12rpx;
					display: flex;
					justify-content: space-between;

					.price {
						font-size: 28rpx;
						font-weight: 700;
						color: $text1;
					}

					.num {
						font-size: 24rpx;
						color: $text3;
					}
				}
			}
		}
	}

	.order-footer {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 24rpx;

		.total {
			font-size: 24rpx;
			color: $text2;

			.amount {
				font-size: 32rpx;
				color: $text1;
				font-weight: 700;
				margin-left: 10rpx;
			}
		}

		.btns {
			display: flex;
			gap: 16rpx;

			.btn {
				margin: 0;
				height: 64rpx;
				padding: 0 32rpx;
				font-size: 24rpx;
				border-radius: 999rpx;
				display: flex;
				align-items: center;
				background: $bg;
				color: $text2;
				transition: transform 0.15s;

				&:active {
					transform: scale(0.96);
				}

				&.primary {
					background: $gradient;
					color: #ffffff;
					box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);
				}

				&.disabled {
					background-color: #e8e8e8;
					color: $text3;
					box-shadow: none;
				}
			}
		}
	}
}
</style>
