<template>
  <view class="page">
    <view class="status-hero">
      <view>
        <text class="status-title">{{ order.statusText }}</text>
        <text class="status-desc">{{ statusDescText }}</text>
      </view>
      <uni-icons :type="statusIcon" size="44" color="#ffffff" />
    </view>

    <view class="card logistics" v-if="order.status >= 2" @click="goLogistics">
      <view class="card-head">
        <text class="head-title">物流信息</text>
        <view class="head-right">
          <text class="head-link">查看详情</text>
          <uni-icons type="right" size="14" color="#8a9aaa" />
        </view>
      </view>
      <view v-if="deliverySummary">
        <text class="log-main">{{ deliverySummary.deliveryCompany || '快递公司' }} · {{ deliverySummary.deliverySn || '-' }}</text>
        <text class="log-sub">{{ latestTrackText }}</text>
        <text class="log-sub">收货人：{{ receiverText }}</text>
      </view>
      <text v-else class="log-sub">物流信息生成中，点击后可重试加载</text>
    </view>

    <view class="card progress" v-if="order.status === 1 || printProgress.hasValue">
      <view class="card-head">
        <text class="head-title">打印进度</text>
        <text class="chip">{{ printProgress.statusDesc || '生产中' }}</text>
      </view>
      <view class="progress-row">
        <view class="bar">
          <view class="bar-inner" :style="{ width: `${printPercent}%` }" />
        </view>
        <text class="percent">{{ printPercent }}%</text>
      </view>
      <view class="meta-grid">
        <text>剩余时间：{{ remainText }}</text>
        <text>喷头温度：{{ tempText(printProgress.toolTempActual, printProgress.toolTempTarget) }}</text>
        <text>热床温度：{{ tempText(printProgress.bedTempActual, printProgress.bedTempTarget) }}</text>
      </view>
    </view>

    <view class="card">
      <view class="address-row">
        <uni-icons type="location-filled" size="18" color="#00bfff" />
        <view class="addr-content">
          <text class="addr-name">{{ parsedAddress.name || '收货人' }} {{ parsedAddress.phone || '' }}</text>
          <text class="addr-detail">{{ parsedAddress.address || '暂无收货地址信息' }}</text>
        </view>
      </view>
    </view>

    <view class="card goods">
      <view class="goods-item" v-for="(item, idx) in order.items" :key="idx">
        <image :src="item.image" mode="aspectFill" class="goods-img" />
        <view class="goods-info">
          <text class="name">{{ item.name }}</text>
          <text class="params">{{ item.params }}</text>
          <view class="price-line">
            <text class="price">￥{{ item.price }}</text>
            <text class="count">x{{ item.num }}</text>
          </view>
        </view>
      </view>
      <view class="sum-row"><text>商品总额</text><text>￥{{ order.totalPrice }}</text></view>
      <view class="sum-row"><text>运费</text><text>￥0.00</text></view>
      <view class="sum-row total"><text>实付款</text><text class="amount">￥{{ order.totalPrice }}</text></view>
    </view>

    <view class="card info">
      <view class="info-row"><text>订单编号</text><view class="inline"><text>{{ order.no }}</text><text class="copy" @click="copyNo">复制</text></view></view>
      <view class="info-row"><text>下单时间</text><text>{{ order.time }}</text></view>
      <view class="info-row"><text>更新时间</text><text>{{ order.updateTimeText }}</text></view>
      <view class="info-row"><text>订单备注</text><text>{{ order.remark || '无' }}</text></view>
    </view>

    <view class="bottom-bar">
      <template v-if="order.status === 0">
        <button class="btn" @click="cancelOrder">取消订单</button>
        <button class="btn primary" @click="payOrder">立即支付</button>
      </template>
      <template v-else-if="order.status === 2">
        <button class="btn" @click="goLogistics">查看物流</button>
        <button class="btn primary" @click="confirmOrder">确认收货</button>
      </template>
      <template v-else-if="order.status === 3">
        <button class="btn" @click="afterSaleEntry ? goAfterSaleDetail() : goAfterSaleCreate()">{{ afterSaleEntry ? '查看售后' : '申请售后' }}</button>
        <button class="btn primary" @click="goComment">立即评价</button>
      </template>
      <template v-else-if="order.status === 1 || order.status === 4">
        <button class="btn" @click="goAfterSaleList">售后记录</button>
      </template>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { cancelOrderApi, confirmOrderReceiveApi, createAlipayAppPayApi, getMyAfterSaleListApi, getMyDeliveryDetailApi, getMyOrderCommentsApi, getOrderDetailApi, getOrderDetailBySnApi, getOrderPayStatusApi, payOrderByWalletApi, syncOrderPayStatusApi } from '../../api/order'
import { getWalletAccountApi } from '../../api/wallet'
import { getApiBaseUrl } from '../../utils/apiBase'

const pageOrderId = ref('')
const pageOrderSn = ref('')

const order = ref({
  id: null,
  no: '',
  status: -1,
  statusText: '加载中',
  totalPrice: '0.00',
  time: '-',
  updateTimeText: '-',
  remark: '',
  customParams: '',
  items: [{ name: '模型订单', params: '-', price: '0.00', num: 1, image: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200' }]
})

const deliverySummary = ref(null)
const afterSaleEntry = ref(null)
const printProgress = reactive({
  hasValue: false,
  statusDesc: '',
  progress: 0,
  estimatedSecondsLeft: null,
  toolTempActual: null,
  toolTempTarget: null,
  bedTempActual: null,
  bedTempTarget: null
})

const socketTask = ref(null)
const socketManualClose = ref(false)
const reconnectTimer = ref(null)

const statusTextMap = {
  0: '待付款',
  1: '生产中',
  2: '待收货',
  3: '已完成',
  4: '已取消'
}

const statusDescText = computed(() => {
  if (order.value.status === 0) return '订单待支付，请尽快完成付款'
  if (order.value.status === 1) return '打印任务执行中，可实时查看进度'
  if (order.value.status === 2) return '包裹运输中，请留意签收'
  if (order.value.status === 3) return '交易已完成，欢迎评价本次订单'
  return '订单已关闭，如有疑问可联系售后'
})

const statusIcon = computed(() => ({
  0: 'wallet-filled',
  1: 'gear-filled',
  2: 'paperplane-filled',
  3: 'checkbox-filled',
  4: 'closeempty'
}[order.value.status] || 'info-filled'))

const printPercent = computed(() => {
  const value = Number(printProgress.progress || 0)
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
})

const remainText = computed(() => {
  const seconds = Number(printProgress.estimatedSecondsLeft || 0)
  if (!seconds || seconds <= 0) return '-'
  const hours = Math.floor(seconds / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${mins}分钟`
  return `${Math.max(1, mins)}分钟`
})

const tempText = (actual, target) => {
  if (actual == null && target == null) return '-'
  const now = actual == null ? '-' : `${Number(actual).toFixed(1)}℃`
  const tar = target == null ? '-' : `${Number(target).toFixed(1)}℃`
  return `${now} / ${tar}`
}

const parsedAddress = computed(() => {
  const defaults = { name: '', phone: '', address: '' }
  const raw = order.value.customParams
  if (!raw) return defaults
  try {
    const obj = JSON.parse(raw)
    const shipping = obj?.shippingAddress || obj?.address || {}
    const name = shipping?.name || shipping?.receiverName || obj?.receiverName || obj?.consigneeName || ''
    const phone = shipping?.phone || shipping?.mobile || obj?.receiverPhone || obj?.consigneePhone || ''
    const address = shipping?.fullAddress
      || shipping?.address
      || shipping?.detailAddress
      || [shipping?.province, shipping?.city, shipping?.district, shipping?.detail].filter(Boolean).join('')
      || obj?.receiverAddress
      || obj?.consigneeAddress
      || ''
    return { name, phone, address }
  } catch (_) {
    return defaults
  }
})

const receiverText = computed(() => {
  if (!deliverySummary.value) return '-'
  return `${deliverySummary.value.receiverName || ''} ${deliverySummary.value.receiverPhone || ''}`.trim() || '-'
})

const latestTrackText = computed(() => {
  const tracks = deliverySummary.value?.tracks
  if (!Array.isArray(tracks) || !tracks.length) return '暂无物流轨迹'
  const latest = tracks.slice().sort((a, b) => new Date(b.trackTime || 0).getTime() - new Date(a.trackTime || 0).getTime())[0]
  return latest?.trackContent || '暂无物流轨迹'
})

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const toTimestamp = (value) => {
  if (!value) return NaN
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : NaN
}

const mapOrderDetail = (record) => {
  const status = Number(record?.orderStatus ?? -1)
  const totalPrice = Number(record?.orderPrice || 0).toFixed(2)
  let remark = ''
  let paramsText = record?.orderSn || ''
  if (record?.customParams) {
    try {
      const paramsObj = JSON.parse(record.customParams)
      remark = paramsObj?.note || ''
      const pieces = []
      if (record?.materialName) pieces.push(record.materialName)
      if (record?.materialColor || paramsObj?.color) pieces.push(record.materialColor || paramsObj?.color)
      if (paramsObj?.scale) pieces.push(`倍率${paramsObj.scale}`)
      if (paramsObj?.fillPercent) pieces.push(`填充${paramsObj.fillPercent}%`)
      if (pieces.length) paramsText = pieces.join(' / ')
    } catch (_) {
      paramsText = record?.orderSn || ''
    }
  }

  const createTimeRaw = record?.createTime || ''
  const updateTimeRaw = record?.updateTime || ''
  const createTimestamp = toTimestamp(createTimeRaw)
  const updateTimestamp = toTimestamp(updateTimeRaw)
  const safeUpdateTime = Number.isFinite(createTimestamp)
    && Number.isFinite(updateTimestamp)
    && updateTimestamp < createTimestamp
    ? createTimeRaw
    : updateTimeRaw

  order.value = {
    id: record?.id,
    no: record?.orderSn || '',
    status,
    statusText: statusTextMap[status] || '未知状态',
    totalPrice,
    time: formatDateTime(createTimeRaw),
    updateTimeText: formatDateTime(safeUpdateTime),
    remark,
    customParams: record?.customParams || '',
    items: [{
      name: record?.modelName || '模型订单',
      params: paramsText,
      price: totalPrice,
      num: 1,
      image: record?.mainImageUrl || 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=200'
    }]
  }
}

const loadOrderDetail = async () => {
  let detail = null
  if (pageOrderSn.value) {
    detail = await getOrderDetailBySnApi(pageOrderSn.value)
  } else if (pageOrderId.value) {
    detail = await getOrderDetailApi(pageOrderId.value)
  } else {
    uni.showToast({ title: '订单参数无效', icon: 'none' })
    return
  }
  mapOrderDetail(detail)
}

const loadDeliverySummary = async () => {
  if (!order.value.no || order.value.status < 2) {
    deliverySummary.value = null
    return
  }
  try {
    deliverySummary.value = await getMyDeliveryDetailApi(order.value.no)
  } catch (_) {
    deliverySummary.value = null
  }
}

const loadAfterSaleEntry = async () => {
  if (!order.value?.no) {
    afterSaleEntry.value = null
    return
  }
  try {
    const data = await getMyAfterSaleListApi({ orderSn: order.value.no, pageNum: 1, pageSize: 20 })
    const records = Array.isArray(data?.records) ? data.records : []
    const record = records.find(item => Number(item?.status) !== 6) || null
    afterSaleEntry.value = record ? { id: String(record.id || ''), afterSaleSn: String(record.afterSaleSn || '') } : null
  } catch (_) {
    afterSaleEntry.value = null
  }
}

const closePrintSocket = () => {
  socketManualClose.value = true
  if (reconnectTimer.value) {
    clearTimeout(reconnectTimer.value)
    reconnectTimer.value = null
  }
  if (socketTask.value) {
    try {
      socketTask.value.close({ code: 1000, reason: 'manual-close' })
    } catch (_) {
      // ignore
    }
  }
  socketTask.value = null
}

const buildPrintWsUrl = () => {
  const token = uni.getStorageSync('token') || ''
  const base = String(getApiBaseUrl() || '').trim().replace(/\/+$/, '')
  const wsBase = base.startsWith('https://') ? base.replace('https://', 'wss://') : base.replace('http://', 'ws://')
  return `${wsBase}/ws/print/progress?token=${encodeURIComponent(token)}`
}

const connectPrintSocket = () => {
  if (!order.value.id || order.value.status !== 1) return
  closePrintSocket()
  socketManualClose.value = false

  const task = uni.connectSocket({ url: buildPrintWsUrl(), complete: () => {} })
  task.onMessage((res) => {
    if (!res?.data) return
    try {
      const payload = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
      if (Number(payload?.orderId) !== Number(order.value.id)) return
      printProgress.hasValue = true
      printProgress.statusDesc = payload?.statusDesc || ''
      printProgress.progress = Number(payload?.progress || 0)
      printProgress.estimatedSecondsLeft = payload?.estimatedSecondsLeft ?? null
      printProgress.toolTempActual = payload?.toolTempActual ?? null
      printProgress.toolTempTarget = payload?.toolTempTarget ?? null
      printProgress.bedTempActual = payload?.bedTempActual ?? null
      printProgress.bedTempTarget = payload?.bedTempTarget ?? null
    } catch (_) {
      // ignore
    }
  })

  task.onClose(() => {
    if (!socketManualClose.value) {
      reconnectTimer.value = setTimeout(() => {
        connectPrintSocket()
      }, 2500)
    }
  })

  task.onError(() => {
    // noop
  })

  socketTask.value = task
}

const copyNo = () => {
  uni.setClipboardData({
    data: order.value.no,
    success: () => uni.showToast({ title: '已复制' })
  })
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

const pollPayStatus = async (orderId, maxRetry = 6) => {
  for (let i = 0; i < maxRetry; i += 1) {
    try {
      const status = await getOrderPayStatusApi(orderId)
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

const executeAlipayPay = async () => {
  const alipayReady = await ensureAlipayAvailable()
  if (!alipayReady) return false
  if (!ensureAlipaySandboxEnv()) return false

  uni.showLoading({ title: '拉起支付中...' })
  const payResult = await createAlipayAppPayApi({ orderId: order.value.id })
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
    await syncOrderPayStatusApi(order.value.id)
  } catch (_) {
    // 无回调场景下主动查单可能短暂失败，后续轮询兜底
  }

  uni.showLoading({ title: '确认支付结果...' })
  const paid = await pollPayStatus(order.value.id)
  uni.hideLoading()
  if (!paid) {
    uni.showToast({ title: '支付结果确认超时，请稍后刷新', icon: 'none' })
    return false
  }
  return true
}

const askSwitchToAlipay = () => new Promise((resolve) => {
  uni.showModal({
    title: '余额不足',
    content: '余额不足，是否切换支付宝支付？',
    success: (res) => resolve(!!res.confirm),
    fail: () => resolve(false)
  })
})

const executeWalletPay = async () => {
  try {
    uni.showLoading({ title: '余额支付中...' })
    await payOrderByWalletApi({ orderId: order.value.id })
    uni.hideLoading()
    return true
  } catch (error) {
    uni.hideLoading()
    const message = error?.errMsg || error?.message || ''
    if (String(message).includes('余额不足')) {
      const shouldSwitch = await askSwitchToAlipay()
      if (!shouldSwitch) return false
      return executeAlipayPay()
    }
    throw error
  }
}

const choosePayMethod = async () => {
  let walletLabel = '余额支付'
  try {
    const wallet = await getWalletAccountApi()
    const balance = Number(wallet?.availableBalance || 0)
    if (Number.isFinite(balance)) {
      walletLabel = `余额支付(可用¥${balance.toFixed(2)})`
    }
  } catch (_) {
    // ignore and fallback label
  }

  return new Promise((resolve, reject) => {
    uni.showActionSheet({
      itemList: [walletLabel, '支付宝支付'],
      success: (res) => resolve(Number(res.tapIndex || 0)),
      fail: (err) => reject(err)
    })
  })
}

const cancelOrder = () => {
  uni.showModal({
    title: '提示',
    content: '确定取消该订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrderApi(order.value.id)
        uni.showToast({ title: '订单已取消', icon: 'success' })
        await loadOrderDetail()
      } catch (error) {
        uni.showToast({ title: error.message || '取消失败', icon: 'none' })
      }
    }
  })
}

const payOrder = async () => {
  if (order.value.status !== 0 || !order.value.id) {
    uni.showToast({ title: '当前订单不可支付', icon: 'none' })
    return
  }
  try {
    const payMethod = await choosePayMethod()
    const paid = payMethod === 0 ? await executeWalletPay() : await executeAlipayPay()
    if (!paid) return
    uni.showToast({ title: '支付成功', icon: 'success' })
    await Promise.all([loadOrderDetail(), loadDeliverySummary(), loadAfterSaleEntry()])
    connectPrintSocket()
  } catch (error) {
    uni.hideLoading()
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
  }
}

const confirmOrder = () => {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到货物并完成本次订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await confirmOrderReceiveApi(order.value.no)
        uni.showToast({ title: '已确认收货', icon: 'success' })
        await Promise.all([loadOrderDetail(), loadDeliverySummary()])
      } catch (error) {
        uni.showToast({ title: error.message || '确认收货失败', icon: 'none' })
      }
    }
  })
}

const goAfterSaleCreate = () => {
  if (!order.value.id) {
    uni.showToast({ title: '订单信息无效', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/user/after-sale-create?orderId=${order.value.id}&orderSn=${order.value.no}` })
}

const goAfterSaleList = () => {
  uni.navigateTo({ url: '/pages/user/after-sale-list' })
}

const goAfterSaleDetail = () => {
  if (!afterSaleEntry.value) {
    uni.showToast({ title: '暂无售后记录', icon: 'none' })
    return
  }
  const sn = encodeURIComponent(afterSaleEntry.value.afterSaleSn || '')
  const id = encodeURIComponent(afterSaleEntry.value.id || '')
  uni.navigateTo({ url: `/pages/user/after-sale-detail?afterSaleSn=${sn}&id=${id}` })
}

const goLogistics = () => {
  if (!order.value.no) {
    uni.showToast({ title: '订单号无效', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/user/logistics-detail?orderSn=${encodeURIComponent(order.value.no)}` })
}

const goComment = () => {
  if (!order.value.id) {
    uni.showToast({ title: '订单信息无效', icon: 'none' })
    return
  }
  checkCommentAndNavigate()
}

const checkCommentAndNavigate = async () => {
  try {
    const data = await getMyOrderCommentsApi({ orderId: order.value.id, pageNum: 1, pageSize: 1 })
    const records = Array.isArray(data?.records) ? data.records : []
    if (records.length > 0) {
      uni.showToast({ title: '该订单已评价', icon: 'none' })
      return
    }
  } catch (_) {
    // ignore and allow navigation fallback
  }
  uni.navigateTo({ url: `/pages/user/order-comment-create?orderId=${order.value.id}` })
}

onLoad(async (options) => {
  pageOrderId.value = options?.id ? String(options.id) : ''
  pageOrderSn.value = options?.orderSn ? decodeURIComponent(String(options.orderSn)) : ''
  try {
    await loadOrderDetail()
    await Promise.all([loadDeliverySummary(), loadAfterSaleEntry()])
    connectPrintSocket()
  } catch (error) {
    uni.showToast({ title: error.message || '加载订单详情失败', icon: 'none' })
  }
})

onUnload(() => {
  closePrintSocket()
})
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

.page {
	min-height: 100vh;
	background: $bg;
	padding-bottom: 140rpx;
}

.status-hero {
	background: $gradient;
	border-bottom-left-radius: 32rpx;
	border-bottom-right-radius: 32rpx;
	padding: 48rpx 32rpx 56rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.status-title { color: #fff; display: block; font-size: 36rpx; font-weight: 700; }
.status-desc { color: rgba(255, 255, 255, 0.9); display: block; margin-top: 10rpx; font-size: 24rpx; }

.card {
	margin: 20rpx 28rpx 0;
	background: $card;
	border-radius: 24rpx;
	padding: 28rpx;
	box-shadow: $shadow;
}

.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.head-title { color: $text1; font-size: 30rpx; font-weight: 700; }
.head-right { display: flex; align-items: center; gap: 6rpx; }
.head-link { color: $text3; font-size: 22rpx; }
.log-main { display: block; color: $text1; font-size: 26rpx; font-weight: 600; }
.log-sub { display: block; color: $text2; font-size: 24rpx; margin-top: 8rpx; line-height: 1.5; }
.chip { color: $primary; background: rgba(0, 191, 255, 0.08); font-size: 22rpx; border-radius: 999rpx; padding: 6rpx 16rpx; font-weight: 600; }
.progress-row { display: flex; align-items: center; gap: 16rpx; }
.bar { height: 16rpx; flex: 1; border-radius: 999rpx; background: $bg; overflow: hidden; }
.bar-inner { height: 100%; background: $gradient; border-radius: 999rpx; }
.percent { color: $text1; font-size: 24rpx; font-weight: 600; }
.meta-grid { display: flex; flex-direction: column; gap: 10rpx; margin-top: 16rpx; color: $text2; font-size: 24rpx; }
.address-row { display: flex; align-items: flex-start; }
.addr-content { margin-left: 14rpx; flex: 1; }
.addr-name { display: block; color: $text1; font-size: 28rpx; font-weight: 600; }
.addr-detail { display: block; color: $text2; font-size: 24rpx; margin-top: 8rpx; line-height: 1.6; }
.goods-item { display: flex; margin-bottom: 18rpx; }
.goods-img { width: 142rpx; height: 142rpx; border-radius: 16rpx; background: $bg; }
.goods-info { margin-left: 16rpx; flex: 1; }
.name { display: block; color: $text1; font-size: 28rpx; font-weight: 700; }
.params { display: block; color: $text3; font-size: 22rpx; margin-top: 8rpx; }
.price-line { display: flex; justify-content: space-between; margin-top: 12rpx; }
.price { color: $text1; font-size: 30rpx; font-weight: 700; }
.count { color: $text3; font-size: 24rpx; }
.sum-row { display: flex; justify-content: space-between; color: $text2; font-size: 24rpx; margin-top: 10rpx; }
.sum-row.total { color: $text1; margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx solid rgba(0,0,0,0.04); }
.amount { color: $danger; font-size: 34rpx; font-weight: 700; }
.info-row { display: flex; justify-content: space-between; color: $text2; font-size: 24rpx; margin-bottom: 16rpx; }
.info-row:last-child { margin-bottom: 0; }
.inline { display: flex; align-items: center; gap: 12rpx; }
.copy { color: $primary; font-weight: 500; }

.bottom-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(255,255,255,0.72);
	backdrop-filter: blur(24px);
	-webkit-backdrop-filter: blur(24px);
	padding: 18rpx 28rpx calc(env(safe-area-inset-bottom) + 18rpx);
	display: flex;
	justify-content: flex-end;
	gap: 14rpx;
}

.btn {
	margin: 0;
	height: 72rpx;
	line-height: 72rpx;
	border-radius: 999rpx;
	background: $card;
	color: $text2;
	padding: 0 36rpx;
	font-size: 26rpx;
	box-shadow: $shadow;
	&:active { transform: scale(0.96); }
}

.btn.primary {
	background: $gradient;
	color: #fff;
	box-shadow: 0 6rpx 20rpx rgba(0, 191, 255, 0.25);
}
</style>
