<template>
	<view class="order-list-container">
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

		<scroll-view scroll-y class="order-scroll">
			<view v-if="displayOrders.length === 0" class="empty-orders">
				<view class="empty-icon">📦</view>
				<text>暂无相关订单</text>
			</view>

			<view class="order-card" v-for="(order, index) in displayOrders" :key="index" @click="goDetail(order)">
				<view class="order-header">
					<text class="order-no">订单号: {{order.no}}</text>
					<text class="status">{{order.statusText}}</text>
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
.order-list-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #f8fafc;
}

.tabs {
	display: flex;
	background-color: #ffffff;
	padding: 0 20rpx;
	.tab-item {
		flex: 1;
		height: 90rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28rpx;
		color: #64748b;
		position: relative;
		&.active {
			color: #4f46e5;
			font-weight: 700;
			&::after {
				content: '';
				position: absolute;
				bottom: 0;
				left: 30%;
				right: 30%;
				height: 4rpx;
				background-color: #4f46e5;
				border-radius: 2rpx;
			}
		}
	}
}

.order-scroll {
	flex: 1;
}

.empty-orders {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
	.empty-icon { font-size: 100rpx; margin-bottom: 20rpx; }
	text { font-size: 28rpx; color: #94a3b8; }
}

.order-card {
	background-color: #ffffff;
	margin: 20rpx;
	border-radius: 20rpx;
	padding: 24rpx;
	.order-header {
		display: flex;
		justify-content: space-between;
		padding-bottom: 20rpx;
		border-bottom: 2rpx solid #f1f5f9;
		.order-no { font-size: 24rpx; color: #94a3b8; }
		.status { font-size: 24rpx; color: #4f46e5; font-weight: 700; }
	}
	.goods-list {
		padding: 20rpx 0;
		.goods-item {
			display: flex;
			margin-bottom: 20rpx;
			&:last-child { margin-bottom: 0; }
			.goods-img { width: 120rpx; height: 120rpx; border-radius: 12rpx; background-color: #f1f5f9; }
			.goods-info {
				flex: 1;
				margin-left: 20rpx;
				.name { font-size: 26rpx; font-weight: 600; color: #1e293b; display: block; }
				.params { font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; display: block; }
				.price-row {
					margin-top: 10rpx;
					display: flex;
					justify-content: space-between;
					.price { font-size: 26rpx; font-weight: 700; color: #1e293b; }
					.num { font-size: 24rpx; color: #94a3b8; }
				}
			}
		}
	}
	.order-footer {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 20rpx;
		border-top: 2rpx solid #f1f5f9;
		.total {
			font-size: 24rpx;
			color: #64748b;
			.amount { font-size: 32rpx; color: #1e293b; font-weight: 700; margin-left: 10rpx; }
		}
		.btns {
			display: flex;
			gap: 16rpx;
			.btn {
				margin: 0;
				height: 60rpx;
				padding: 0 30rpx;
				font-size: 24rpx;
				border-radius: 30rpx;
				display: flex;
				align-items: center;
				&.primary {
					background-color: #4f46e5;
					color: #ffffff;
				}
				&.disabled {
					background-color: #cbd5e1;
					color: #f8fafc;
				}
			}
		}
	}
}
</style>
