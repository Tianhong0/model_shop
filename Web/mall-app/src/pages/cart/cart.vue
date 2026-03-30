<template>
	<view class="cart-container">
		<view class="cart-header">
			<text class="title">购物车 ({{cartItems.length}})</text>
			<view class="actions">
				<uni-icons type="redo" size="22" color="#94a3b8" @click="shareCart"></uni-icons>
				<text class="edit" @click="isEditing = !isEditing">{{isEditing ? '完成' : '编辑'}}</text>
			</view>
		</view>

		<scroll-view scroll-y class="cart-scroll">
			<view v-if="cartItems.length === 0" class="empty-cart">
				<uni-icons type="cart" size="64" color="#c0c8d4"></uni-icons>
				<text>购物车空空如也</text>
				<button class="go-mall" @click="goHome">去逛逛</button>
			</view>

			<view class="cart-list" v-else>
				<view class="cart-card" v-for="(item, index) in cartItems" :key="index" :style="{ animationDelay: `${index * 0.06}s` }">
					<checkbox :checked="item.selected" @click="toggleSelect(index)" color="#00bfff" />
					<view class="item-img-wrap">
						<image :src="item.image" mode="aspectFill" class="item-img"></image>
					</view>
					<view class="item-info">
						<view class="name-row">
							<text class="name">{{item.name}}</text>
							<uni-icons v-if="isEditing" type="trash" size="20" color="#ff4d6d" @click="deleteItem(index)"></uni-icons>
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

			<view class="footer-placeholder"></view>
		</scroll-view>

		<view class="bottom-bar" v-if="cartItems.length > 0">
			<view class="all-select">
				<checkbox :checked="isAllSelected" @click="toggleAll" color="#00bfff" />
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
$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;

$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;

$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$gradient-primary: linear-gradient(135deg, $sky-blue 0%, $sky-light 100%);
$danger: #ff4d6d;

.cart-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $surface;
}

/* —— 毛玻璃头部 —— */
.cart-header {
	background: rgba(255, 255, 255, 0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	padding: 32rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-top: calc(env(safe-area-inset-top) + 24rpx);
	animation: fadeInDown 0.4s ease forwards;

	.title {
		font-size: 36rpx;
		font-weight: 700;
		color: $text-primary;
	}

	.actions {
		display: flex;
		align-items: center;
		gap: 28rpx;
	}

	.edit {
		font-size: 28rpx;
		color: $sky-deep;
		font-weight: 600;
		padding: 8rpx 20rpx;
		border-radius: 999rpx;
		background: rgba(0, 191, 255, 0.06);
		transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);

		&:active {
			transform: scale(0.92);
		}
	}
}

@keyframes fadeInDown {
	from { opacity: 0; transform: translateY(-16rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.cart-scroll {
	flex: 1;
	padding-bottom: 130rpx;
}

/* —— 空购物车 —— */
.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 240rpx;
	animation: fadeIn 0.6s ease forwards;
	opacity: 0;

	text {
		font-size: 28rpx;
		color: $text-muted;
		margin-top: 32rpx;
	}

	.go-mall {
		margin-top: 48rpx;
		width: 260rpx;
		height: 84rpx;
		background: $gradient-primary;
		color: $surface-raised;
		border-radius: 999rpx;
		font-size: 28rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 32rpx rgba(0, 191, 255, 0.3);
		transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);

		&:active {
			transform: scale(0.94);
		}
	}
}

@keyframes fadeIn {
	to { opacity: 1; }
}

/* —— 购物车列表 —— */
.cart-list {
	padding: 20rpx 32rpx;
}

.cart-card {
	background-color: $surface-raised;
	border-radius: 24rpx;
	padding: 24rpx;
	display: flex;
	align-items: center;
	margin-bottom: 20rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.4s ease forwards;
	opacity: 0;
	transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);

	&:active {
		transform: scale(0.985);
	}
}

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(20rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.item-img-wrap {
	width: 160rpx;
	height: 160rpx;
	border-radius: 20rpx;
	overflow: hidden;
	background: #f0f2f5;
	margin-left: 16rpx;
	flex-shrink: 0;

	.item-img {
		width: 100%;
		height: 100%;
		animation: imgFadeIn 0.5s ease forwards;
		opacity: 0;
	}
}

@keyframes imgFadeIn {
	from { opacity: 0; transform: scale(0.97); }
	to { opacity: 1; transform: scale(1); }
}

.item-info {
	flex: 1;
	margin-left: 24rpx;
	min-width: 0;

	.name-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12rpx;
	}

	.name {
		font-size: 28rpx;
		font-weight: 600;
		color: $text-primary;
		flex: 1;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.params {
		font-size: 22rpx;
		color: $text-muted;
		margin-top: 8rpx;
		display: block;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.price-row {
		margin-top: 20rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;

		.price {
			font-size: 32rpx;
			font-weight: 800;
			color: $sky-deep;
		}

		.num-box {
			display: flex;
			align-items: center;
			background: $surface;
			border-radius: 999rpx;
			overflow: hidden;

			.minus, .plus {
				width: 52rpx;
				height: 52rpx;
				text-align: center;
				line-height: 50rpx;
				color: $text-secondary;
				font-size: 28rpx;
				transition: all 0.2s ease;

				&:active {
					background: rgba(0, 191, 255, 0.1);
					color: $sky-blue;
				}
			}

			.num {
				width: 56rpx;
				text-align: center;
				font-size: 26rpx;
				font-weight: 600;
				color: $text-primary;
			}
		}
	}
}

/* —— 底部结算栏 —— */
.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 1000;
	height: 110rpx;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);
	display: flex;
	align-items: center;
	padding: 0 32rpx;
	padding-bottom: env(safe-area-inset-bottom);
	animation: slideUp 0.5s ease forwards;

	/* #ifdef APP-PLUS */
	bottom: calc(116rpx + env(safe-area-inset-bottom));
	padding-bottom: 0;
	/* #endif */

	.all-select {
		display: flex;
		align-items: center;

		text {
			font-size: 26rpx;
			color: $text-secondary;
			margin-left: 10rpx;
		}
	}

	.total-info {
		margin-left: auto;
		margin-right: 24rpx;
		display: flex;
		align-items: baseline;

		.label {
			font-size: 26rpx;
			color: $text-primary;
		}

		.symbol {
			font-size: 26rpx;
			color: $sky-deep;
			font-weight: 700;
			margin-left: 8rpx;
		}

		.amount {
			font-size: 36rpx;
			color: $sky-deep;
			font-weight: 800;
		}
	}
}

@keyframes slideUp {
	from { opacity: 0; transform: translateY(20rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.settle-btn {
	min-width: 220rpx;
	height: 80rpx;
	background: $gradient-primary;
	color: $surface-raised;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	font-weight: 600;
	box-shadow: 0 6rpx 24rpx rgba(0, 191, 255, 0.3);
	transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
	padding: 0 32rpx;

	&:active {
		transform: scale(0.94);
	}

	&.delete-mode {
		background: linear-gradient(135deg, #ff6b8a 0%, $danger 100%);
		box-shadow: 0 6rpx 24rpx rgba(255, 77, 109, 0.3);
	}
}

.footer-placeholder {
	height: calc(110rpx + env(safe-area-inset-bottom) + 40rpx);
	/* #ifdef APP-PLUS */
	height: calc(110rpx + 116rpx + env(safe-area-inset-bottom) + env(safe-area-inset-bottom) + 40rpx);
	/* #endif */
}
</style>
