<template>
	<view class="checkout-container">
		<scroll-view scroll-y class="checkout-scroll">
			<!-- 收货地址 -->
			<view class="address-section" @click="selectAddress">
				<view class="address-icon">📍</view>
				<view class="address-info" v-if="address">
					<view class="user-info">
						<text class="name">{{address.name}}</text>
						<text class="phone">{{address.phone}}</text>
					</view>
					<text class="detail">{{address.province}}{{address.city}}{{address.district}}{{address.detail}}</text>
				</view>
				<view class="no-address" v-else>
					<text>请选择收货地址</text>
				</view>
				<view class="arrow">></view>
			</view>

			<!-- 商品列表 -->
			<view class="goods-section">
				<view class="section-title">订单商品</view>
				<view class="goods-item" v-for="(item, index) in orderItems" :key="index">
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

			<!-- 费用明细 -->
			<view class="fee-section">
				<view class="fee-row">
					<text class="label">基础费用</text>
					<text class="val">￥{{basePrice}}</text>
				</view>
				<view class="fee-row">
					<text class="label">材料费用</text>
					<text class="val">￥{{materialCost}}</text>
				</view>
				<view class="fee-row">
					<text class="label">商品总额</text>
					<text class="val">￥{{goodsTotal}}</text>
				</view>
				<view class="fee-row">
					<text class="label">运费</text>
					<text class="val">￥{{shippingFee}}</text>
				</view>
				<view class="fee-row">
					<text class="label">优惠减免</text>
					<text class="val discount">-￥{{discount}}</text>
				</view>
				<view class="fee-row points-row">
					<text class="label">积分抵扣 (可用{{ availablePoints }})</text>
					<input class="points-input" type="number" v-model="usePoints" @blur="sanitizeUsePoints" placeholder="输入积分" />
				</view>
				<view class="fee-row" v-if="Number(pointDiscountAmount) > 0">
					<text class="label">积分减免</text>
					<text class="val discount">-￥{{pointDiscountAmount}}</text>
				</view>
			</view>

			<!-- 支付方式 -->
			<view class="pay-section">
				<view class="section-title">支付方式</view>
				<radio-group @change="onPayChange">
					<!-- <label class="pay-item">
						<view class="pay-icon">🟢</view>
						<text>微信支付</text>
						<radio value="wx" checked color="#4f46e5" />
					</label> -->
					<label class="pay-item">
						<view class="pay-icon">🔵</view>
						<text>支付宝</text>
						<radio value="ali" color="#4f46e5" />
					</label>
					<label class="pay-item">
						<view class="pay-icon">💰</view>
						<text>余额支付 (可用￥{{ availableBalance }})</text>
						<radio value="wallet" color="#4f46e5" />
					</label>
				</radio-group>
			</view>
		</scroll-view>

		<!-- 底部提交栏 -->
		<view class="footer-bar">
			<view class="total-price">
				<text class="label">实付款:</text>
				<text class="symbol">￥</text>
				<text class="val">{{payAmount}}</text>
			</view>
			<view class="submit-btn" @click="handlePay">立即支付</view>
		</view>


	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
	createOrderApi,
	createAlipayAppPayApi,
	createAlipayBatchPayApi,
	payOrderByWalletApi,
	payBatchByWalletApi,
	getOrderPayStatusApi,
	getBatchPayStatusApi,
	syncOrderPayStatusApi,
	syncBatchPayStatusApi
} from '../../api/order'
import { getPointAccountApi } from '../../api/point'
import { getWalletAccountApi } from '../../api/wallet'
import { ensureLoginOrRedirect } from '../../utils/auth'

const ADDRESS_STORAGE_KEY = 'user_addresses'
const CHECKOUT_SELECTED_ADDRESS_KEY = 'checkout_selected_address_id'

const address = ref(null)

const orderItems = ref([])
const basePrice = ref('0.00')
const materialCost = ref('0.00')
const shippingFee = ref('0.00')
const discount = ref('0.00')
const goodsTotal = ref('0.00')
const payAmount = ref('0.00')
const pointDiscountAmount = ref('0.00')
const payMethod = ref('ali')
const availablePoints = ref(0)
const availableBalance = ref('0.00')
const usePoints = ref('0')
const checkoutFrom = ref('buyNow')
const paying = ref(false)
const preparingOrder = ref(false)
const pendingOrders = ref([])
const pendingBatchId = ref(null)
// const paySuccessPopup = ref(null)

onMounted(() => {
	if (!ensureLoginOrRedirect()) return
	loadSelectedAddress()

	// 获取结算商品，实际应用中通过 Vuex/Pinia 或 Storage 传递
	const items = uni.getStorageSync('checkout_items')
	checkoutFrom.value = uni.getStorageSync('checkout_from') || 'buyNow'
	if (items && items.length > 0) {
		orderItems.value = items
	} else {
		// 模拟数据
		orderItems.value = [
			{ name: '蒸汽朋克猫', params: '光敏树脂 / 复古铜', price: '298.50', num: 1, image: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200' }
		]
	}
	loadPointAccount()
	loadWalletAccount()
	prepareOrderPricing()
})

onShow(() => {
	if (!ensureLoginOrRedirect()) return
	loadSelectedAddress()
})

const normalizeAddress = (item) => {
	if (!item) return null
	return {
		id: item.id || '',
		name: item.name || '',
		phone: item.phone || '',
		province: item.province || '',
		city: item.city || '',
		district: item.district || '',
		detail: item.detail || '',
		isDefault: Boolean(item.isDefault)
	}
}

const loadSelectedAddress = () => {
	const list = uni.getStorageSync(ADDRESS_STORAGE_KEY)
	if (!Array.isArray(list) || list.length === 0) {
		address.value = null
		return
	}
	const addresses = list.map(normalizeAddress)
	const selectedId = uni.getStorageSync(CHECKOUT_SELECTED_ADDRESS_KEY)
	let selected = null
	if (selectedId) {
		selected = addresses.find((item) => String(item.id) === String(selectedId)) || null
	}
	if (!selected) {
		selected = addresses.find((item) => item.isDefault) || addresses[0]
	}
	address.value = selected
}

const onPayChange = (e) => {
	payMethod.value = e.detail.value
}

const loadPointAccount = async () => {
	try {
		const point = await getPointAccountApi()
		availablePoints.value = Number(point?.availablePoints || 0)
	} catch (_) {
		availablePoints.value = 0
	}
}

const loadWalletAccount = async () => {
	try {
		const wallet = await getWalletAccountApi()
		availableBalance.value = normalizeMoney(wallet?.availableBalance)
	} catch (_) {
		availableBalance.value = '0.00'
	}
}

const sanitizeUsePoints = () => {
	const raw = String(usePoints.value || '').replace(/\D/g, '')
	if (!raw) {
		usePoints.value = '0'
		return
	}
	let value = Number(raw)
	if (!Number.isFinite(value) || value < 0) value = 0
	if (value > availablePoints.value) value = availablePoints.value
	usePoints.value = String(value)
	if (orderItems.value.length === 1) {
		pendingOrders.value = []
		pendingBatchId.value = null
		prepareOrderPricing()
	}
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const isAppPlus = () => {
	// #ifdef APP-PLUS
	return true
	// #endif
	// #ifndef APP-PLUS
	return false
	// #endif
}

const normalizeProviderIds = (providers) => {
	if (!Array.isArray(providers)) return []
	return providers
		.map((item) => {
			if (typeof item === 'string') return item
			if (item && typeof item === 'object') {
				return item.id || item.name || item.provider || ''
			}
			return ''
		})
		.filter(Boolean)
}

const getPaymentProviders = () => {
	return new Promise((resolve) => {
		// #ifdef APP-PLUS
		if (typeof plus !== 'undefined' && plus?.payment?.getChannels) {
			plus.payment.getChannels(
				(channels) => resolve(normalizeProviderIds(channels)),
				() => resolve([])
			)
			return
		}
		// #endif

		uni.getProvider({
			service: 'payment',
			success: (res) => resolve(normalizeProviderIds(res?.providers || [])),
			fail: () => resolve([])
		})
	})
}

const ensureAlipayAvailable = async () => {
	if (!isAppPlus()) {
		uni.showToast({ title: 'H5不支持App支付宝拉起，请在App端支付', icon: 'none' })
		return false
	}
	const providers = await getPaymentProviders()
	if (!providers.includes('alipay')) {
		const channelsText = providers.length ? providers.join(',') : '无'
		uni.showToast({ title: `未检测到支付宝通道(当前:${channelsText})`, icon: 'none' })
		return false
	}
	return true
}

const withSandboxOrderInfo = (orderInfo) => {
	const raw = String(orderInfo || '').trim()
	if (!raw) return raw
	if (/([?&])bizcontext=/.test(raw)) {
		return raw
	}
	const bizContext = encodeURIComponent(JSON.stringify({ appenv: 'system' }))
	return `${raw}&bizcontext=${bizContext}`
}

const ensureAlipaySandboxEnv = () => {
	// #ifdef APP-PLUS
	if (typeof plus === 'undefined') return false
	if (plus?.os?.name !== 'Android') return true
	try {
		const EnvUtils = plus.android.importClass('com.alipay.sdk.app.EnvUtils')
		EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
		return true
	} catch (error) {
		console.error('设置支付宝沙箱环境失败:', error)
		return false
	}
	// #endif
	// #ifndef APP-PLUS
	return false
	// #endif
}

const pollPayStatus = async (orderId, maxRetry = 6) => {
	for (let i = 0; i < maxRetry; i += 1) {
		try {
			const status = await getOrderPayStatusApi(orderId)
			if (status?.payStatus === 1 || status?.orderStatus === 1) {
				return true
			}
		} catch (error) {
			// 忽略单次轮询异常，继续重试
		}
		await sleep(2000)
	}
	return false
}

const pollBatchPayStatus = async (batchId, maxRetry = 8) => {
	for (let i = 0; i < maxRetry; i += 1) {
		try {
			const status = await getBatchPayStatusApi(batchId)
			if (status?.payStatus === 1 || status?.batchStatus === 1) {
				return true
			}
		} catch (error) {
			// 忽略单次轮询异常，继续重试
		}
		await sleep(2000)
	}
	return false
}

const removePaidItemsFromCart = () => {
	if (checkoutFrom.value !== 'cart') return
	const currentCart = uni.getStorageSync('cart_list') || []
	const selectedSignatures = new Set(
		orderItems.value.map(item => `${item.modelId || ''}|${item.name || ''}|${item.params || ''}|${item.price || ''}`)
	)
	const nextCart = currentCart.filter(item => {
		const signature = `${item.modelId || ''}|${item.name || ''}|${item.params || ''}|${item.price || ''}`
		return !selectedSignatures.has(signature)
	})
	uni.setStorageSync('cart_list', nextCart)
}

const normalizeMoney = (value) => {
	const num = Number(value)
	if (!Number.isFinite(num) || num < 0) return '0.00'
	return num.toFixed(2)
}

const applyPricingFromOrder = (orderResult = {}) => {
	basePrice.value = normalizeMoney(orderResult?.basePrice)
	materialCost.value = normalizeMoney(orderResult?.materialCost)
	goodsTotal.value = normalizeMoney(orderResult?.goodsAmount ?? orderResult?.orderPrice)
	shippingFee.value = normalizeMoney(orderResult?.shippingFee ?? orderResult?.freightFee)
	discount.value = normalizeMoney(orderResult?.discountAmount ?? orderResult?.couponDiscount)
	pointDiscountAmount.value = normalizeMoney(orderResult?.pointDiscountAmount)
	payAmount.value = normalizeMoney(orderResult?.payAmount ?? orderResult?.orderPrice)
}

const applyPricingFromItems = () => {
	const goodsAmount = orderItems.value.reduce((sum, item) => {
		const price = Number(item?.price || 0)
		const quantity = Number(item?.num || 1)
		if (!Number.isFinite(price) || !Number.isFinite(quantity)) return sum
		return sum + Math.max(0, price) * Math.max(1, quantity)
	}, 0)
	basePrice.value = normalizeMoney(goodsAmount)
	materialCost.value = '0.00'
	goodsTotal.value = normalizeMoney(goodsAmount)
	shippingFee.value = '0.00'
	discount.value = '0.00'
	pointDiscountAmount.value = '0.00'
	payAmount.value = normalizeMoney(goodsAmount)
}

const buildPendingOrderMeta = (orderResult = {}) => ({
	orderId: orderResult?.orderId,
	orderSn: orderResult?.orderSn,
	orderPrice: normalizeMoney(orderResult?.orderPrice)
})

const createOrdersForItems = async () => {
	const created = []
	for (const item of orderItems.value) {
		const quantity = Math.max(1, Number(item?.num || 1))
		for (let i = 0; i < quantity; i += 1) {
			const orderRequest = buildOrderRequest(item)
			if (!orderRequest.modelId) {
				throw new Error('缺少模型信息，无法下单')
			}
			const orderResult = await createOrderApi(orderRequest)
			created.push(buildPendingOrderMeta(orderResult))
		}
	}
	return created
}

const buildOrderRequest = (item) => {
	const payload = item?.orderPayload || {}
	const modelId = payload?.modelId || item?.modelId
	let customParamsObj = {}
	const rawCustomParams = payload?.custom_params || payload?.customParams || null
	if (rawCustomParams && typeof rawCustomParams === 'object') {
		customParamsObj = { ...rawCustomParams }
	} else if (typeof rawCustomParams === 'string' && rawCustomParams.trim()) {
		try {
			customParamsObj = JSON.parse(rawCustomParams)
		} catch (error) {
			customParamsObj = { rawCustomParams }
		}
	}
	if (!customParamsObj.paramsText && item?.params) {
		customParamsObj.paramsText = item.params
	}
	if (address.value) {
		customParamsObj.receiverName = address.value.name
		customParamsObj.receiverPhone = address.value.phone
		customParamsObj.receiverAddress = `${address.value.province}${address.value.city}${address.value.district}${address.value.detail}`
		customParamsObj.shippingAddress = {
			name: address.value.name,
			phone: address.value.phone,
			province: address.value.province,
			city: address.value.city,
			district: address.value.district,
			detail: address.value.detail
		}
	}
	const serializedCustomParams = Object.keys(customParamsObj).length > 0 ? JSON.stringify(customParamsObj) : ''
	const pointsToUse = orderItems.value.length === 1 ? Number(usePoints.value || 0) : 0
	return {
		modelId,
		materialId: payload?.materialId || null,
		scale: payload?.scale || 1,
		fillPercent: payload?.fillPercent || 100,
		color: payload?.color || '',
		note: payload?.note || '',
		usePoints: pointsToUse,
		customParams: serializedCustomParams,
		custom_params: serializedCustomParams
	}
}

const prepareOrderPricing = async () => {
	if (preparingOrder.value) return
	if (!orderItems.value.length) return
	if (orderItems.value.length > 1) {
		pendingOrders.value = []
		pendingBatchId.value = null
		applyPricingFromItems()
		return
	}

	const currentItem = orderItems.value[0]
	const orderRequest = buildOrderRequest(currentItem)
	if (!orderRequest.modelId) {
		uni.showToast({ title: '缺少模型信息，无法获取订单金额', icon: 'none' })
		return
	}

	preparingOrder.value = true
	uni.showLoading({ title: '正在获取订单金额...' })
	try {
		const orderResult = await createOrderApi(orderRequest)
		pendingOrders.value = [buildPendingOrderMeta(orderResult)]
		pendingBatchId.value = null
		applyPricingFromOrder(orderResult)
	} catch (error) {
		uni.showToast({ title: error?.message || '获取订单金额失败', icon: 'none' })
	} finally {
		uni.hideLoading()
		preparingOrder.value = false
	}
}

const handlePay = async () => {
	if (paying.value) {
		return
	}
	if (preparingOrder.value) {
		uni.showToast({ title: '订单金额确认中，请稍候', icon: 'none' })
		return
	}
	if (payMethod.value !== 'ali' && payMethod.value !== 'wallet') {
		uni.showToast({ title: '当前支付方式不支持', icon: 'none' })
		return
	}
	if (!orderItems.value.length) {
		uni.showToast({ title: '结算商品为空', icon: 'none' })
		return
	}
	if (!address.value) {
		uni.showToast({ title: '请选择收货地址', icon: 'none' })
		return
	}
	paying.value = true
	uni.showLoading({ title: '正在创建支付...' })
	try {
		let createdOrders = [...pendingOrders.value].filter(item => item?.orderId)
		if (!createdOrders.length) {
			createdOrders = await createOrdersForItems()
			pendingOrders.value = createdOrders
			if (createdOrders.length === 1) {
				applyPricingFromOrder({ orderPrice: createdOrders[0].orderPrice })
			} else {
				applyPricingFromItems()
			}
		}

		if (!createdOrders.length) {
			throw new Error('订单创建失败，请重试')
		}

		const orderIds = createdOrders.map(item => item.orderId)
		const useBatchPay = orderIds.length > 1
		const executeAlipayPay = async () => {
			const alipayReady = await ensureAlipayAvailable()
			if (!alipayReady) {
				uni.hideLoading()
				return false
			}
			if (!ensureAlipaySandboxEnv()) {
				uni.hideLoading()
				return false
			}

			const payResult = useBatchPay
				? await createAlipayBatchPayApi({ orderIds })
				: await createAlipayAppPayApi({ orderId: orderIds[0] })

			if (useBatchPay) {
				pendingBatchId.value = payResult?.batchId || null
			}
			uni.hideLoading()

			await new Promise((resolve, reject) => {
				uni.requestPayment({
					provider: 'alipay',
					orderInfo: withSandboxOrderInfo(payResult.orderString),
					success: () => resolve(true),
					fail: (err) => reject(err)
				})
			})

			if (useBatchPay) {
				if (!pendingBatchId.value) {
					throw new Error('批量支付单创建失败')
				}
				try {
					await syncBatchPayStatusApi(pendingBatchId.value)
				} catch (syncError) {
					// 无回调场景下主动查单可能短暂失败，后续轮询兜底
				}
			} else {
				try {
					await syncOrderPayStatusApi(orderIds[0])
				} catch (syncError) {
					// 无回调场景下主动查单可能短暂失败，后续轮询兜底
				}
			}

			uni.showLoading({ title: '支付结果确认中...' })
			const paid = useBatchPay
				? await pollBatchPayStatus(pendingBatchId.value)
				: await pollPayStatus(orderIds[0])
			uni.hideLoading()
			if (!paid) {
				uni.showToast({ title: '支付结果确认超时，请稍后在订单页查看', icon: 'none' })
				return false
			}
			return true
		}

		let currentPayMethod = payMethod.value
		if (currentPayMethod === 'wallet') {
			await loadWalletAccount()
			const payable = Number(payAmount.value || 0)
			const balance = Number(availableBalance.value || 0)
			if (payable > balance) {
				payMethod.value = 'ali'
				currentPayMethod = 'ali'
				uni.showToast({ title: '余额不足，已自动切换支付宝', icon: 'none' })
			}
		}

		if (currentPayMethod === 'wallet') {
			try {
				if (useBatchPay) {
					await payBatchByWalletApi({ orderIds })
				} else {
					await payOrderByWalletApi({ orderId: orderIds[0] })
				}
				uni.hideLoading()
			} catch (walletError) {
				const walletMsg = walletError?.errMsg || walletError?.message || ''
				if (String(walletMsg).includes('余额不足')) {
					payMethod.value = 'ali'
					uni.showToast({ title: '余额不足，已自动切换支付宝', icon: 'none' })
					const paid = await executeAlipayPay()
					if (!paid) {
						return
					}
				} else {
					throw walletError
				}
			}
		} else {
			const paid = await executeAlipayPay()
			if (!paid) {
				return
			}
		}

		removePaidItemsFromCart()
		uni.removeStorageSync('checkout_items')
		uni.removeStorageSync('checkout_from')
		pendingOrders.value = []
		pendingBatchId.value = null
		await loadWalletAccount()
		uni.showToast({ title: '支付成功', icon: 'success' })
		setTimeout(() => {
			goOrderList()
		}, 600)
	} catch (error) {
		uni.hideLoading()
		console.error('支付完整错误对象:', JSON.stringify(error || {}))
		const message = error?.errMsg || error?.message || '支付失败'
		if (String(message).includes('cancel')) {
			uni.showToast({ title: '您已取消支付', icon: 'none' })
		} else if (error?.code === -100 && String(message).includes('62009')) {
			uni.showToast({ title: '支付宝SDK异常(62009)，请确认安装并登录支付宝沙箱版，且使用云打包安装包', icon: 'none' })
		} else if (String(message).includes('requestPayment:fail')) {
			uni.showToast({ title: '支付通道不可用，请确认使用自定义基座/云打包并启用支付宝支付', icon: 'none' })
		} else {
			uni.showToast({ title: message, icon: 'none' })
		}
	} finally {
		paying.value = false
	}
}

const goOrderList = () => {
	uni.redirectTo({ url: '/pages/user/orders' })
}

const selectAddress = () => {
	uni.navigateTo({ url: '/pages/user/address?select=1' })
}
</script>

<style scoped lang="scss">
.checkout-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: #f8fafc;
}

.checkout-scroll {
	flex: 1;
}

.section-title {
	font-size: 30rpx;
	font-weight: 700;
	color: #1e293b;
	margin-bottom: 24rpx;
}

.address-section {
	background-color: #ffffff;
	padding: 30rpx;
	display: flex;
	align-items: center;
	margin-bottom: 20rpx;
	.address-icon { font-size: 40rpx; }
	.arrow { font-size: 32rpx; color: #94a3b8; }
	.address-info {
		flex: 1;
		margin-left: 20rpx;
		.user-info {
			display: flex;
			align-items: center;
			.name { font-size: 32rpx; font-weight: 700; color: #1e293b; }
			.phone { font-size: 26rpx; color: #64748b; margin-left: 20rpx; }
		}
		.detail { font-size: 26rpx; color: #64748b; margin-top: 10rpx; display: block; }
	}
	.no-address { flex: 1; margin-left: 20rpx; font-size: 28rpx; color: #94a3b8; }
}

.goods-section {
	background-color: #ffffff;
	padding: 30rpx;
	margin-bottom: 20rpx;
	.goods-item {
		display: flex;
		margin-bottom: 30rpx;
		&:last-child { margin-bottom: 0; }
		.goods-img { width: 140rpx; height: 140rpx; border-radius: 12rpx; background-color: #f1f5f9; }
		.goods-info {
			flex: 1;
			margin-left: 24rpx;
			.name { font-size: 28rpx; font-weight: 600; color: #1e293b; display: block; }
			.params { font-size: 22rpx; color: #94a3b8; margin-top: 8rpx; display: block; }
			.price-row {
				margin-top: 16rpx;
				display: flex;
				justify-content: space-between;
				.price { font-size: 30rpx; font-weight: 700; color: #1e293b; }
				.num { font-size: 26rpx; color: #94a3b8; }
			}
		}
	}
}

.fee-section {
	background-color: #ffffff;
	padding: 30rpx;
	margin-bottom: 20rpx;
	.fee-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 20rpx;
		&:last-child { margin-bottom: 0; }
		.label { font-size: 26rpx; color: #64748b; }
		.val { font-size: 26rpx; color: #1e293b; }
		.discount { color: #ef4444; }
	}
	.points-row {
		.points-input {
			width: 220rpx;
			height: 62rpx;
			padding: 0 18rpx;
			border-radius: 12rpx;
			background: #f8fafc;
			font-size: 24rpx;
			text-align: right;
			color: #1e293b;
		}
	}
}

.pay-section {
	background-color: #ffffff;
	padding: 30rpx;
	.pay-item {
		display: flex;
		align-items: center;
		padding: 30rpx 0;
		border-bottom: 2rpx solid #f8fafc;
		&:last-child { border-bottom: 0; }
		.pay-icon { font-size: 40rpx; }
		text { flex: 1; font-size: 28rpx; color: #1e293b; margin-left: 24rpx; }
	}
}

.footer-bar {
	height: 110rpx;
	background-color: #ffffff;
	border-top: 2rpx solid #f1f5f9;
	display: flex;
	align-items: center;
	padding: 0 30rpx;
	padding-bottom: env(safe-area-inset-bottom);
	.total-price {
		flex: 1;
		display: flex;
		align-items: baseline;
		.label { font-size: 24rpx; color: #64748b; }
		.symbol { font-size: 24rpx; color: #ef4444; font-weight: 700; margin-left: 8rpx; }
		.val { font-size: 40rpx; color: #ef4444; font-weight: 700; }
	}
	.submit-btn {
		width: 260rpx;
		height: 80rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30rpx;
		font-weight: 600;
	}
}

.success-modal {
	width: 560rpx;
	background-color: #ffffff;
	border-radius: 32rpx;
	padding: 60rpx 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	.t { font-size: 36rpx; font-weight: 700; color: #1e293b; margin-top: 30rpx; }
	.d { font-size: 26rpx; color: #64748b; margin-top: 20rpx; text-align: center; }
	.btn {
		margin-top: 50rpx;
		width: 100%;
		height: 88rpx;
		background-color: #4f46e5;
		color: #ffffff;
		border-radius: 44rpx;
		font-size: 30rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}
}
</style>
