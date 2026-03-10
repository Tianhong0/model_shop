<template>
  <view class="page" v-if="detail">
    <view class="card">
      <view class="row-between">
        <text class="sn">{{ detail.orderSn }}</text>
        <text class="status">{{ statusText(detail.status) }}</text>
      </view>
      <view class="title">{{ detail.listingTitle }}</view>
      <view class="meta">订单金额：￥{{ Number(detail.orderAmount || 0).toFixed(2) }}</view>
      <view class="meta">收货人：{{ detail.receiverName }} {{ detail.receiverPhone }}</view>
      <view class="meta">收货地址：{{ detail.receiverAddress }}</view>
      <view class="meta">物流信息：{{ detail.deliveryCompany || '-' }} {{ detail.deliverySn || '' }}</view>
    </view>

    <view v-if="role === 'buy' && detail.status === 0" class="card">
      <view class="section-title">支付方式</view>
      <view class="pay-method-list">
        <view class="pay-method-item" :class="{ active: payMethod === 'wallet' }" @click="payMethod = 'wallet'">
          <view>
            <view class="pay-method-title">余额支付</view>
            <view class="pay-method-desc">当前余额 ￥{{ walletBalance }}</view>
          </view>
          <view class="pay-method-check">{{ payMethod === 'wallet' ? '✓' : '' }}</view>
        </view>
        <view class="pay-method-item" :class="{ active: payMethod === 'alipay' }" @click="payMethod = 'alipay'">
          <view>
            <view class="pay-method-title">支付宝支付</view>
            <view class="pay-method-desc">拉起支付宝完成真实支付</view>
          </view>
          <view class="pay-method-check">{{ payMethod === 'alipay' ? '✓' : '' }}</view>
        </view>
      </view>
    </view>

    <view class="card" v-if="detail.afterSale">
      <view class="section-title">售后信息</view>
      <view class="meta">售后单号：{{ detail.afterSale.afterSaleSn }}</view>
      <view class="meta">售后状态：{{ afterSaleText(detail.afterSale.status) }}</view>
      <view class="meta">原因：{{ detail.afterSale.reason }}</view>
      <view class="meta">卖家备注：{{ detail.afterSale.sellerRemark || '-' }}</view>
    </view>

    <view v-if="shipPopupVisible" class="popup-mask" @click="closeShipPopup">
      <view class="popup-card" @click.stop>
        <view class="popup-title">填写发货信息</view>
        <input v-model="shipForm.deliveryCompany" class="popup-input" placeholder="请输入物流公司，如顺丰速运" maxlength="50" />
        <input v-model="shipForm.deliverySn" class="popup-input" placeholder="请输入快递单号" maxlength="50" />
        <view class="popup-actions">
          <button class="ghost-btn popup-btn" @click="closeShipPopup">取消</button>
          <button class="primary-btn popup-btn" :loading="shipSubmitting" @click="submitShipOrder">确认发货</button>
        </view>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="ghost-btn" @click="goReport">举报</button>
      <button v-if="role === 'buy' && detail.status === 0" class="primary-btn" @click="payOrder">立即支付</button>
      <button v-if="role === 'buy' && detail.status === 0" class="ghost-btn" @click="cancelOrder">取消订单</button>
      <button v-if="role === 'buy' && detail.status === 2" class="primary-btn" @click="confirmReceive">确认收货</button>
      <button v-if="role === 'buy' && (detail.status === 2 || detail.status === 3) && !detail.afterSale" class="primary-btn" @click="goAfterSale">申请售后</button>
      <button v-if="role === 'sell' && detail.status === 1" class="primary-btn" @click="openShipPopup">填写发货</button>
      <button v-if="role === 'sell' && detail.afterSale && (detail.afterSale.status === 0 || detail.afterSale.status === 3)" class="primary-btn" @click="auditAfterSale(true)">同意退款</button>
      <button v-if="role === 'sell' && detail.afterSale && (detail.afterSale.status === 0 || detail.afterSale.status === 3)" class="ghost-btn" @click="auditAfterSale(false)">拒绝售后</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getWalletAccountApi } from '../../api/wallet'
import { auditUsedAfterSaleBySellerApi, cancelUsedOrderApi, confirmUsedOrderApi, getUsedOrderDetailApi, getUsedOrderPayStatusApi, payUsedOrderApi, payUsedOrderByWalletApi, shipUsedOrderApi, syncUsedOrderPayStatusApi } from '../../api/used'

const orderId = ref('')
const role = ref('buy')
const detail = ref(null)
const walletBalance = ref('0.00')
const payMethod = ref('wallet')
const shipPopupVisible = ref(false)
const shipSubmitting = ref(false)
const shipForm = ref({
  deliveryCompany: '',
  deliverySn: ''
})

const statusText = (status) => ({ 0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '售后中' }[Number(status)] || '未知')
const afterSaleText = (status) => ({ 0: '待处理', 1: '已同意', 2: '已拒绝', 3: '平台介入', 4: '已退款', 5: '已关闭' }[Number(status)] || '未知')

const loadDetail = async () => {
  detail.value = await getUsedOrderDetailApi(orderId.value)
  if (role.value === 'buy' && Number(detail.value?.status) === 0) {
    await loadWalletBalance()
  }
}

const loadWalletBalance = async () => {
  try {
    const wallet = await getWalletAccountApi()
    walletBalance.value = Number(wallet?.availableBalance || 0).toFixed(2)
    const orderAmount = Number(detail.value?.orderAmount || 0)
    payMethod.value = Number(walletBalance.value) >= orderAmount ? 'wallet' : 'alipay'
  } catch (_) {
    walletBalance.value = '0.00'
    payMethod.value = 'alipay'
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
    uni.showToast({ title: '支付宝SDK沙箱环境初始化失败', icon: 'none' })
    return false
  }
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
}

const pollPayStatus = async (id, maxRetry = 6) => {
  for (let i = 0; i < maxRetry; i += 1) {
    try {
      const status = await getUsedOrderPayStatusApi(id)
      if (status?.payStatus === 1 || status?.orderStatus === 1) {
        return true
      }
    } catch (_) {
      // ignore and retry
    }
    await sleep(2000)
  }
  return false
}

const payOrder = async () => {
  if (payMethod.value === 'wallet') {
    try {
      uni.showLoading({ title: '余额支付中...' })
      await payUsedOrderByWalletApi(orderId.value)
      uni.hideLoading()
      uni.showToast({ title: '支付成功', icon: 'success' })
      await Promise.all([loadDetail(), loadWalletBalance()])
    } catch (error) {
      uni.hideLoading()
      uni.showToast({ title: error.message || '余额支付失败', icon: 'none' })
    }
    return
  }

  try {
    const alipayReady = await ensureAlipayAvailable()
    if (!alipayReady) return
    if (!ensureAlipaySandboxEnv()) return

    uni.showLoading({ title: '拉起支付中...' })
    const payResult = await payUsedOrderApi(orderId.value)
    uni.hideLoading()

    await new Promise((resolve, reject) => {
      uni.requestPayment({
        provider: 'alipay',
        orderInfo: withSandboxOrderInfo(payResult.orderString),
        success: () => resolve(true),
        fail: (err) => reject(err)
      })
    })

    try {
      await syncUsedOrderPayStatusApi(orderId.value)
    } catch (_) {
      // ignore
    }

    uni.showLoading({ title: '确认支付结果...' })
    const paid = await pollPayStatus(orderId.value)
    uni.hideLoading()
    if (!paid) {
      uni.showToast({ title: '支付结果确认超时，请稍后刷新', icon: 'none' })
      return
    }

    uni.showToast({ title: '支付成功', icon: 'success' })
    await loadDetail()
    await loadWalletBalance()
  } catch (error) {
    uni.hideLoading()
    const message = error?.errMsg || error?.message || '支付失败'
    if (String(message).includes('cancel')) {
      uni.showToast({ title: '您已取消支付', icon: 'none' })
    } else if (error?.code === -100 && String(message).includes('62009')) {
      uni.showToast({ title: '支付宝SDK异常(62009)，请确认安装并登录支付宝沙箱版', icon: 'none' })
    } else if (String(message).includes('requestPayment:fail')) {
      uni.showToast({ title: '支付通道不可用，请确认使用自定义基座/云打包并启用支付宝支付', icon: 'none' })
    } else {
      uni.showToast({ title: message, icon: 'none' })
    }
  }
}

const cancelOrder = async () => {
  try {
    await cancelUsedOrderApi(orderId.value)
    uni.showToast({ title: '订单已取消', icon: 'success' })
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '取消失败', icon: 'none' })
  }
}

const confirmReceive = async () => {
  try {
    await confirmUsedOrderApi(orderId.value)
    uni.showToast({ title: '收货成功', icon: 'success' })
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '确认失败', icon: 'none' })
  }
}

const openShipPopup = () => {
  shipForm.value = {
    deliveryCompany: detail.value?.deliveryCompany || '',
    deliverySn: detail.value?.deliverySn || ''
  }
  shipPopupVisible.value = true
}

const closeShipPopup = () => {
  if (shipSubmitting.value) return
  shipPopupVisible.value = false
}

const submitShipOrder = async () => {
  const deliveryCompany = String(shipForm.value.deliveryCompany || '').trim()
  const deliverySn = String(shipForm.value.deliverySn || '').trim()
  if (!deliveryCompany) {
    uni.showToast({ title: '请填写物流公司', icon: 'none' })
    return
  }
  if (!deliverySn) {
    uni.showToast({ title: '请填写快递单号', icon: 'none' })
    return
  }
  try {
    shipSubmitting.value = true
    await shipUsedOrderApi({ orderId: orderId.value, deliveryCompany, deliverySn })
    shipPopupVisible.value = false
    uni.showToast({ title: '已发货', icon: 'success' })
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '发货失败', icon: 'none' })
  } finally {
    shipSubmitting.value = false
  }
}

const auditAfterSale = async (approved) => {
  try {
    await auditUsedAfterSaleBySellerApi({ afterSaleId: detail.value.afterSale.id, approved, refundAmount: detail.value.orderAmount, remark: approved ? '卖家同意退款' : '卖家拒绝售后' })
    uni.showToast({ title: approved ? '已同意退款' : '已拒绝售后', icon: 'success' })
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '处理失败', icon: 'none' })
  }
}

const goAfterSale = () => uni.navigateTo({ url: `/pages/used/after-sale-create?orderId=${orderId.value}` })
const goReport = () => uni.navigateTo({ url: `/pages/used/report?targetType=ORDER&targetId=${orderId.value}` })

onLoad(async (options) => {
  orderId.value = options?.id || ''
  role.value = options?.role || 'buy'
  if (!orderId.value) return
  try {
    await loadDetail()
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  }
})
</script>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f8fafc; padding: 20rpx 20rpx 140rpx; }
.card { background: #fff; border-radius: 24rpx; padding: 24rpx; margin-bottom: 20rpx; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.sn, .status, .meta { font-size: 24rpx; color: #64748b; }
.title { margin-top: 16rpx; font-size: 32rpx; color: #0f172a; font-weight: 700; }
.meta { margin-top: 12rpx; line-height: 1.6; }
.section-title { font-size: 28rpx; font-weight: 600; color: #0f172a; }
.pay-method-list { margin-top: 20rpx; display: flex; flex-direction: column; gap: 16rpx; }
.pay-method-item { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 24rpx; border-radius: 22rpx; background: #f8fafc; border: 2rpx solid transparent; }
.pay-method-item.active { border-color: #111827; background: #eff6ff; }
.pay-method-title { font-size: 28rpx; font-weight: 700; color: #0f172a; }
.pay-method-desc { margin-top: 8rpx; font-size: 24rpx; color: #64748b; }
.pay-method-check { width: 44rpx; height: 44rpx; border-radius: 50%; background: #111827; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 20rpx; display: flex; gap: 16rpx; box-shadow: 0 -8rpx 24rpx rgba(15, 23, 42, 0.08); flex-wrap: wrap; }
.ghost-btn, .primary-btn { flex: 1; min-width: 220rpx; border-radius: 999rpx; }
.ghost-btn { background: #eef2ff; color: #4338ca; }
.primary-btn { background: #111827; color: #fff; }
.popup-mask { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; padding: 32rpx; z-index: 20; }
.popup-card { width: 100%; background: #fff; border-radius: 28rpx; padding: 28rpx; box-sizing: border-box; }
.popup-title { font-size: 30rpx; font-weight: 700; color: #0f172a; margin-bottom: 20rpx; }
.popup-input { width: 100%; height: 88rpx; background: #f8fafc; border-radius: 20rpx; padding: 0 24rpx; box-sizing: border-box; font-size: 28rpx; color: #0f172a; margin-bottom: 16rpx; }
.popup-actions { display: flex; gap: 16rpx; margin-top: 8rpx; }
.popup-btn { min-width: 0; }
</style>
