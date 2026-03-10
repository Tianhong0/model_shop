<template>
	<view class="cart-container">
		<view class="cart-header">
			<text class="title">购物车 ({{cartItems.length}})</text>
			<view class="actions">
				<uni-icons type="redo" size="22" color="#64748b" @click="shareCart"></uni-icons>
				<text class="edit" @click="isEditing = !isEditing">{{isEditing ? '完成' : '编辑'}}</text>
			</view>
		</view>

		<scroll-view scroll-y class="cart-scroll">
			<view v-if="cartItems.length === 0" class="empty-cart">
				<uni-icons type="cart" size="64" color="#cbd5e1"></uni-icons>
				<text>购物车空空如也</text>
				<button class="go-mall" @click="goHome">去逛逛</button>
			</view>

			<view class="cart-list" v-else>
				<view class="cart-card" v-for="(item, index) in cartItems" :key="index">
					<checkbox :checked="item.selected" @click="toggleSelect(index)" color="#4f46e5" />
					<image :src="item.image" mode="aspectFill" class="item-img"></image>
					<view class="item-info">
						<view class="name-row">
							<text class="name">{{item.name}}</text>
							<uni-icons v-if="isEditing" type="trash" size="20" color="#ef4444" @click="deleteItem(index)"></uni-icons>
						</view>
						<text class="params">{{item.params}}</text>
						<view class="price-row">
							<text class="price">￥{{item.price}}</text>
							<view class="num-box">
								<text class="minus" @click="changeNum(index, -1)">-</text>
								<text class="num">{{item.num}}</text>
								<text class="plus" @click="changeNum(index, 1)">+</text>
							</view>
						</view>
					</view>
				</view>
			</view>
			
		
		</scroll-view>

		<!-- 结算栏 -->
		<view class="bottom-bar" v-if="cartItems.length > 0">
			<view class="all-select">
				<checkbox :checked="isAllSelected" @click="toggleAll" color="#4f46e5" />
				<text>全选</text>
			</view>
			<view class="total-info">
				<text class="label">合计:</text>
				<text class="symbol">￥</text>
				<text class="amount">{{totalAmount}}</text>
			</view>
			<view class="settle-btn" :class="{ 'delete-mode': isEditing }" @click="goCheckout">
				{{isEditing ? '删除所选' : '去结算(' + selectedCount + ')'}}
			</view>
		</view>

		<!-- #ifdef APP-PLUS -->
		<AppTabbar />
		<!-- #endif -->
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const isEditing = ref(false)
const cartItems = ref([])

// 使用 onShow 确保每次切换到购物车页面时都会刷新数据
onShow(() => {
	loadCartData()
})

const loadCartData = () => {
	const data = uni.getStorageSync('cart_list') || []
	cartItems.value = data
}

const saveCartData = () => {
	uni.setStorageSync('cart_list', cartItems.value)
}

const totalAmount = computed(() => {
	return cartItems.value
		.filter(item => item.selected)
		.reduce((sum, item) => sum + parseFloat(item.price) * item.num, 0)
		.toFixed(2)
})

const selectedCount = computed(() => {
	return cartItems.value.filter(item => item.selected).length
})

const isAllSelected = computed(() => {
	return cartItems.value.length > 0 && cartItems.value.every(item => item.selected)
})

const toggleSelect = (index) => {
	cartItems.value[index].selected = !cartItems.value[index].selected
	saveCartData()
}

const toggleAll = () => {
	const all = !isAllSelected.value
	cartItems.value.forEach(item => item.selected = all)
	saveCartData()
}

const changeNum = (index, delta) => {
	const item = cartItems.value[index]
	if (item.num + delta >= 1) {
		item.num += delta
		saveCartData()
	}
}

const deleteItem = (index) => {
	uni.showModal({
		title: '提示',
		content: '确定移除该商品吗？',
		success: (res) => {
			if (res.confirm) {
				cartItems.value.splice(index, 1)
				saveCartData()
			}
		}
	})
}

const goHome = () => {
	uni.switchTab({ url: '/pages/index/index' })
}

const goCheckout = () => {
	if (isEditing.value) {
		const selectedIndices = []
		cartItems.value.forEach((item, index) => {
			if (item.selected) selectedIndices.push(index)
		})
		
		if (selectedIndices.length === 0) {
			uni.showToast({ title: '请选择商品', icon: 'none' })
			return
		}
		
		uni.showModal({
			title: '提示',
			content: `确定删除这 ${selectedIndices.length} 个商品吗？`,
			success: (res) => {
				if (res.confirm) {
					cartItems.value = cartItems.value.filter(item => !item.selected)
					saveCartData()
					isEditing.value = false
				}
			}
		})
	} else {
		const selectedItems = cartItems.value.filter(item => item.selected)
		if (selectedItems.length === 0) {
			uni.showToast({ title: '请选择商品', icon: 'none' })
			return
		}
		uni.setStorageSync('checkout_items', selectedItems)
		uni.setStorageSync('checkout_from', 'cart')
		uni.navigateTo({ url: '/pages/cart/checkout' })
	}
}

const shareCart = () => {
	const selectedItems = cartItems.value.filter(item => item.selected)
	if (selectedItems.length === 0) {
		uni.showToast({ title: '请先选择要分享的商品', icon: 'none' })
		return
	}
	uni.showModal({
		title: '生成分享清单',
		content: `已为您选中 ${selectedItems.length} 件商品，生成公开清单后其他用户可直接购买。`,
		confirmText: '生成并分享',
		success: (res) => {
			if (res.confirm) {
				uni.showToast({ title: '清单分享链接已复制', icon: 'success' })
			}
		}
	})
}
</script>

<style scoped lang="scss">
.cart-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #f8fafc;
}

.cart-header {
	background-color: #ffffff;
	padding: 30rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-top: calc(env(safe-area-inset-top) + 20rpx);
	.title { font-size: 36rpx; font-weight: 700; color: #1e293b; }
	.actions {
		display: flex;
		align-items: center;
		gap: 30rpx;
	}
	.edit { font-size: 28rpx; color: #4f46e5; }
}

.cart-scroll {
	flex: 1;
}

.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
	text { font-size: 28rpx; color: #94a3b8; margin-top: 30rpx; }
	.go-mall {
		margin-top: 50rpx;
		width: 240rpx;
		height: 80rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 40rpx;
		font-size: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
}

.cart-list {
	padding: 20rpx 30rpx;
}

.cart-card {
	background-color: #ffffff;
	border-radius: 24rpx;
	padding: 24rpx;
	display: flex;
	align-items: center;
	margin-bottom: 24rpx;
	.item-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		background-color: #f1f5f9;
		margin-left: 20rpx;
	}
		.item-info {
			flex: 1;
			margin-left: 24rpx;
			.name-row {
				display: flex;
				justify-content: space-between;
				align-items: center;
			}
			.name { font-size: 28rpx; font-weight: 600; color: #1e293b; flex: 1; }

		.params { font-size: 22rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
		.price-row {
			margin-top: 24rpx;
			display: flex;
			justify-content: space-between;
			align-items: center;
			.price { font-size: 32rpx; font-weight: 700; color: #ef4444; }
			.num-box {
				display: flex;
				align-items: center;
				border: 2rpx solid #e2e8f0;
				border-radius: 8rpx;
				.minus, .plus { width: 50rpx; height: 50rpx; text-align: center; line-height: 46rpx; color: #64748b; }
				.num { width: 60rpx; text-align: center; font-size: 24rpx; border-left: 2rpx solid #e2e8f0; border-right: 2rpx solid #e2e8f0; }
			}
		}
	}
}

.recommend-section {
	padding: 40rpx 30rpx;
	.sec-title { font-size: 30rpx; font-weight: 700; color: #1e293b; margin-bottom: 30rpx; }
	.rec-list {
		display: flex;
		gap: 30rpx;
		.rec-item {
			flex: 1;
			background-color: #ffffff;
			border-radius: 20rpx;
			padding: 20rpx;
			image { width: 100%; height: 200rpx; border-radius: 12rpx; }
			text { font-size: 24rpx; display: block; margin-top: 10rpx; }
			.p { color: #ef4444; font-weight: 700; }
		}
	}
}

.bottom-bar {
	height: 110rpx;
	background-color: #ffffff;
	border-top: 2rpx solid #f1f5f9;
	display: flex;
	align-items: center;
	padding: 0 30rpx;
	padding-bottom: env(safe-area-inset-bottom);
	.all-select {
		display: flex;
		align-items: center;
		text { font-size: 24rpx; color: #64748b; margin-left: 10rpx; }
	}
	.total-info {
		margin-left: auto;
		margin-right: 24rpx;
		display: flex;
		align-items: baseline;
		.label { font-size: 24rpx; color: #1e293b; }
		.symbol { font-size: 24rpx; color: #ef4444; font-weight: 700; margin-left: 8rpx; }
		.amount { font-size: 36rpx; color: #ef4444; font-weight: 700; }
	}
	.settle-btn {
		width: 220rpx;
		height: 80rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28rpx;
		font-weight: 600;
		&.delete-mode {
			background-color: #ef4444;
		}
	}
}
</style>
