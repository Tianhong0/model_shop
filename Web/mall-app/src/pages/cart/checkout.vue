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
				<view class="arrow">
					<uni-icons type="right" size="16" color="#94a3b8"></uni-icons>
				</view>
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
				<view class="fee-row coupon-row" @click="showCouponPicker" v-if="orderItems.length === 1">
					<text class="label">优惠券</text>
					<view class="coupon-picker">
						<text v-if="selectedCoupon" class="selected-coupon">{{ selectedCoupon.name }} (-￥{{couponDiscountAmount}})</text>
						<text v-else class="no-coupon">{{ availableCoupons.length > 0 ? availableCoupons.length + '张可用' : '暂无可用' }}</text>
						<uni-icons type="right" size="14" color="#94a3b8"></uni-icons>
					</view>
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

			<!-- 优惠券选择弹窗 -->
			<view class="coupon-popup" v-if="showCouponPopup" @click="showCouponPopup = false">
				<view class="coupon-popup-content" @click.stop>
					<view class="popup-header">
						<text class="popup-title">选择优惠券</text>
						<text class="popup-close" @click="showCouponPopup = false">✕</text>
					</view>
					<scroll-view scroll-y class="coupon-list">
						<view v-if="availableCoupons.length === 0" class="empty-coupon">暂无可用优惠券</view>
						<view
							class="coupon-item"
							v-for="item in availableCoupons"
							:key="item.id"
							:class="{ selected: selectedCoupon?.id === item.id }"
							@click="selectCoupon(item)"
						>
							<view class="coupon-left">
								<text class="coupon-value">¥{{ item.value }}</text>
								<text class="coupon-condition">满¥{{ item.minAmount || 0 }}可用</text>
							</view>
							<view class="coupon-right">
								<text class="coupon-name">{{ item.name }}</text>
								<text class="coupon-expire">{{ item.endTime?.slice(0, 10) }}到期</text>
							</view>
						</view>
					</scroll-view>
					<view class="popup-footer">
						<text class="clear-btn" @click="clearCoupon">不使用优惠券</text>
					</view>
				</view>
			</view>

			<!-- 支付方式 -->
			<view class="pay-section">
				<view class="section-title">支付方式</view>
				<radio-group @change="onPayChange">
					<!-- <label class="pay-item">
						<view class="pay-icon">🟢</view>
						<text>微信支付</text>
						<radio value="wx" checked color="#00bfff" />
					</label> -->
					<label class="pay-item">
						<view class="pay-icon">🔵</view>
						<text>支付宝</text>
						<radio value="ali" color="#00bfff" />
					</label>
					<label class="pay-item">
						<view class="pay-icon">💰</view>
						<text>余额支付 (可用￥{{ availableBalance }})</text>
						<radio value="wallet" color="#00bfff" />
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
import { getAvailableCouponsForOrderApi, calculateCouponDiscountApi } from '../../api/coupon'

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
const selectedCoupon = ref(null)
const couponDiscountAmount = ref('0.00')
const availableCoupons = ref([])
const showCouponPopup = ref(false)
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

const showCouponPicker = async () => {
	// 如果订单正在准备中，等待完成
	if (preparingOrder.value) {
		uni.showLoading({ title: '正在加载...' })
		// 等待订单准备完成
		while (preparingOrder.value) {
			await sleep(100)
		}
		uni.hideLoading()
	}
	await loadAvailableCoupons()
	showCouponPopup.value = true
}

const loadAvailableCoupons = async () => {
	try {
		// 优先使用后端返回的金额，如果没有则根据商品列表计算
		let amount = Number(goodsTotal.value) || 0
		if (amount <= 0) {
			// 根据商品列表计算预估金额
			amount = orderItems.value.reduce((sum, item) => {
				const price = Number(item?.price || 0)
				const quantity = Number(item?.num || 1)
				return sum + price * quantity
			}, 0)
		}
		if (amount <= 0) {
			availableCoupons.value = []
			return
		}
		const data = await getAvailableCouponsForOrderApi(amount)
		availableCoupons.value = Array.isArray(data) ? data : []
	} catch (error) {
		console.error('加载优惠券失败', error)
		availableCoupons.value = []
	}
}

const selectCoupon = async (coupon) => {
	selectedCoupon.value = coupon
	showCouponPopup.value = false

	try {
		const amount = Number(goodsTotal.value) || 0
		const discount = await calculateCouponDiscountApi(coupon.id, amount)
		couponDiscountAmount.value = normalizeMoney(discount || 0)
	} catch (error) {
		couponDiscountAmount.value = '0.00'
	}

	if (orderItems.value.length === 1) {
		pendingOrders.value = []
		pendingBatchId.value = null
		prepareOrderPricing()
	}
}

const clearCoupon = () => {
	selectedCoupon.value = null
	couponDiscountAmount.value = '0.00'
	showCouponPopup.value = false

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
	const couponIdToUse = orderItems.value.length === 1 && selectedCoupon.value ? selectedCoupon.value.id : null
	return {
		modelId,
		materialId: payload?.materialId || null,
		scale: payload?.scale || 1,
		fillPercent: payload?.fillPercent || 100,
		color: payload?.color || '',
		note: payload?.note || '',
		usePoints: pointsToUse,
		couponId: couponIdToUse,
		customParams: serializedCustomParams,
		custom_params: serializedCustomParams
	}
}

const prepareOrderPricing = async () => {
	if (preparingOrder.value) return
	if (!orderItems.value.length) return

	preparingOrder.value = true
	pendingOrders.value = []
	pendingBatchId.value = null
	uni.showLoading({ title: '正在获取订单金额...' })

	try {
		const orderResults = []
		let totalBasePrice = 0
		let totalMaterialCost = 0
		let totalGoodsAmount = 0
		let totalShippingFee = 0
		let totalDiscount = 0
		let totalPayAmount = 0

		for (const item of orderItems.value) {
			const quantity = Math.max(1, Number(item?.num || 1))
			for (let i = 0; i < quantity; i += 1) {
				const orderRequest = buildOrderRequest(item)
				if (!orderRequest.modelId) {
					throw new Error('缺少模型信息，无法下单')
				}
				const orderResult = await createOrderApi(orderRequest)
				orderResults.push(buildPendingOrderMeta(orderResult))

				totalBasePrice += Number(orderResult?.basePrice || 0)
				totalMaterialCost += Number(orderResult?.materialCost || 0)
				totalGoodsAmount += Number(orderResult?.goodsAmount || orderResult?.orderPrice || 0)
				totalShippingFee += Number(orderResult?.shippingFee || orderResult?.freightFee || 0)
				totalDiscount += Number(orderResult?.discountAmount || orderResult?.couponDiscount || 0)
				totalPayAmount += Number(orderResult?.payAmount || orderResult?.orderPrice || 0)
			}
		}

		pendingOrders.value = orderResults
		basePrice.value = normalizeMoney(totalBasePrice)
		materialCost.value = normalizeMoney(totalMaterialCost)
		goodsTotal.value = normalizeMoney(totalGoodsAmount)
		shippingFee.value = normalizeMoney(totalShippingFee)
		discount.value = normalizeMoney(totalDiscount)
		payAmount.value = normalizeMoney(totalPayAmount)
	} catch (error) {
		uni.showToast({ title: error?.message || '获取订单金额失败', icon: 'none' })
		applyPricingFromItems()
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

		// 支付宝支付
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

		// 抖音支付
		const executeDouyinPay = async () => {
			const payResult = useBatchPay
				? await createDouyinBatchPayApi({ orderIds })
				: await createDouyinAppPayApi({ orderId: orderIds[0] })

			if (useBatchPay) {
				pendingBatchId.value = payResult?.batchId || null
			}
			uni.hideLoading()

			// 调用抖音支付
			await new Promise((resolve, reject) => {
				// #ifdef APP-PLUS
				// 尝试使用原生插件
				const douyinPay = uni.requireNativePlugin('DouyinPay')
				if (douyinPay) {
					douyinPay.pay({
						orderInfo: payResult.orderInfo
					}, (res) => {
						if (res.code === 0 || res.code === '0') {
							resolve(true)
						} else {
							reject(new Error(res.message || '抖音支付失败'))
						}
					})
				} else {
					// 使用uni.requestPayment
					uni.requestPayment({
						provider: 'toutiao',
						orderInfo: payResult.orderInfo,
						success: () => resolve(true),
						fail: (err) => reject(err)
					})
				}
				// #endif

				// #ifndef APP-PLUS
				uni.requestPayment({
					provider: 'toutiao',
					orderInfo: payResult.orderInfo,
					success: () => resolve(true),
					fail: (err) => reject(err)
				})
				// #endif
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
		} else if (currentPayMethod === 'douyin') {
			const paid = await executeDouyinPay()
			if (!paid) {
				return
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
$sky-blue: #00bfff;
$sky-light: #5ce1ff;
$sky-deep: #0099cc;
$surface: #f8f8f8;
$surface-raised: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #94a3b8;
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$danger: #ff4d6d;
$gradient-primary: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);

@keyframes fadeInUp {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.checkout-container {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $surface;
}

.checkout-scroll {
	flex: 1;
}

.section-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
	margin-bottom: 24rpx;
}

.address-section {
	background-color: $surface-raised;
	padding: 32rpx;
	display: flex;
	align-items: center;
	margin: 24rpx;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out both;

	.address-icon {
		font-size: 40rpx;
	}
	.arrow {
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.address-info {
		flex: 1;
		margin-left: 20rpx;

		.user-info {
			display: flex;
			align-items: center;
			.name {
				font-size: 32rpx;
				font-weight: 700;
				color: $text-primary;
			}
			.phone {
				font-size: 26rpx;
				color: $text-secondary;
				margin-left: 20rpx;
			}
		}
		.detail {
			font-size: 26rpx;
			color: $text-secondary;
			margin-top: 10rpx;
			display: block;
		}
	}
	.no-address {
		flex: 1;
		margin-left: 20rpx;
		font-size: 28rpx;
		color: $text-muted;
	}
}

.goods-section {
	background-color: $surface-raised;
	padding: 32rpx;
	margin: 0 24rpx 24rpx;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out 0.05s both;

	.goods-item {
		display: flex;
		margin-bottom: 30rpx;
		&:last-child {
			margin-bottom: 0;
		}
		.goods-img {
			width: 140rpx;
			height: 140rpx;
			border-radius: 20rpx;
			background-color: $surface;
		}
		.goods-info {
			flex: 1;
			margin-left: 24rpx;

			.name {
				font-size: 28rpx;
				font-weight: 600;
				color: $text-primary;
				display: block;
			}
			.params {
				font-size: 24rpx;
				color: $text-muted;
				margin-top: 8rpx;
				display: block;
			}
			.price-row {
				margin-top: 16rpx;
				display: flex;
				justify-content: space-between;

				.price {
					font-size: 30rpx;
					font-weight: 700;
					color: $text-primary;
				}
				.num {
					font-size: 26rpx;
					color: $text-muted;
				}
			}
		}
	}
}

.fee-section {
	background-color: $surface-raised;
	padding: 32rpx;
	margin: 0 24rpx 24rpx;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out 0.1s both;

	.fee-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 20rpx;
		&:last-child {
			margin-bottom: 0;
		}
		.label {
			font-size: 28rpx;
			color: $text-secondary;
		}
		.val {
			font-size: 28rpx;
			color: $text-primary;
		}
		.discount {
			color: $danger;
		}
	}
	.points-row {
		.points-input {
			width: 220rpx;
			height: 62rpx;
			padding: 0 18rpx;
			border-radius: 999rpx;
			background: $surface;
			font-size: 24rpx;
			text-align: right;
			color: $text-primary;
		}
	}
}

.pay-section {
	background-color: $surface-raised;
	padding: 32rpx;
	margin: 0 24rpx 24rpx;
	border-radius: 24rpx;
	box-shadow: $shadow-card;
	animation: fadeInUp 0.35s ease-out 0.15s both;

	.pay-item {
		display: flex;
		align-items: center;
		padding: 28rpx 0;
		position: relative;

		&:not(:last-child)::after {
			content: '';
			position: absolute;
			left: 0;
			right: 0;
			bottom: 0;
			height: 1rpx;
			background: rgba(0, 0, 0, 0.03);
		}

		.pay-icon {
			font-size: 40rpx;
		}
		text {
			flex: 1;
			font-size: 28rpx;
			color: $text-primary;
			margin-left: 24rpx;
		}
	}
}

.footer-bar {
	height: 110rpx;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	display: flex;
	align-items: center;
	padding: 0 30rpx;
	padding-bottom: env(safe-area-inset-bottom);
	box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);

	.total-price {
		flex: 1;
		display: flex;
		align-items: baseline;

		.label {
			font-size: 24rpx;
			color: $text-secondary;
		}
		.symbol {
			font-size: 24rpx;
			color: $danger;
			font-weight: 700;
			margin-left: 8rpx;
		}
		.val {
			font-size: 40rpx;
			color: $danger;
			font-weight: 700;
		}
	}
	.submit-btn {
		width: 260rpx;
		height: 80rpx;
		background: $gradient-primary;
		color: $surface-raised;
		border-radius: 999rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30rpx;
		font-weight: 600;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);

		&:active {
			transform: scale(0.96);
		}
	}
}

.success-modal {
	width: 560rpx;
	background-color: $surface-raised;
	border-radius: 32rpx;
	padding: 60rpx 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-shadow: $shadow-card;

	.t {
		font-size: 36rpx;
		font-weight: 700;
		color: $text-primary;
		margin-top: 30rpx;
	}
	.d {
		font-size: 26rpx;
		color: $text-secondary;
		margin-top: 20rpx;
		text-align: center;
	}
	.btn {
		margin-top: 50rpx;
		width: 100%;
		height: 88rpx;
		background: $gradient-primary;
		color: $surface-raised;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.35);

		&:active {
			transform: scale(0.96);
		}
	}
}

.coupon-row {
	.coupon-picker {
		display: flex;
		align-items: center;

		.selected-coupon {
			font-size: 26rpx;
			color: $sky-blue;
		}

		.no-coupon {
			font-size: 26rpx;
			color: $text-muted;
		}
	}
}

.coupon-popup {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	z-index: 1000;
	display: flex;
	align-items: flex-end;

	.coupon-popup-content {
		width: 100%;
		max-height: 70vh;
		background: $surface-raised;
		border-radius: 32rpx 32rpx 0 0;
		display: flex;
		flex-direction: column;
	}

	.popup-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 32rpx;
		border-bottom: 1rpx solid rgba(0, 0, 0, 0.05);

		.popup-title {
			font-size: 32rpx;
			font-weight: 700;
			color: $text-primary;
		}

		.popup-close {
			font-size: 32rpx;
			color: $text-muted;
		}
	}

	.coupon-list {
		flex: 1;
		padding: 24rpx;
		max-height: 50vh;
	}

	.empty-coupon {
		text-align: center;
		padding: 60rpx;
		font-size: 28rpx;
		color: $text-muted;
	}

	.coupon-item {
		display: flex;
		background: $surface;
		border-radius: 16rpx;
		margin-bottom: 20rpx;
		overflow: hidden;
		border: 2rpx solid transparent;

		&.selected {
			border-color: $sky-blue;
		}

		.coupon-left {
			width: 160rpx;
			background: $gradient-primary;
			padding: 24rpx;
			display: flex;
			flex-direction: column;
			align-items: center;
			justify-content: center;
			color: #fff;

			.coupon-value {
				font-size: 40rpx;
				font-weight: 700;
			}

			.coupon-condition {
				font-size: 20rpx;
				opacity: 0.9;
				margin-top: 8rpx;
			}
		}

		.coupon-right {
			flex: 1;
			padding: 20rpx;
			display: flex;
			flex-direction: column;
			justify-content: center;

			.coupon-name {
				font-size: 28rpx;
				color: $text-primary;
				font-weight: 500;
			}

			.coupon-expire {
				font-size: 22rpx;
				color: $text-muted;
				margin-top: 8rpx;
			}
		}
	}

	.popup-footer {
		padding: 24rpx 32rpx;
		padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
		border-top: 1rpx solid rgba(0, 0, 0, 0.05);

		.clear-btn {
			display: block;
			text-align: center;
			font-size: 28rpx;
			color: $text-secondary;
			padding: 20rpx 0;
		}
	}
}
</style>
