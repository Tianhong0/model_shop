/**
 * 统一支付工具
 * 支持支付宝和抖音支付
 */

/**
 * 执行APP支付
 * @param {Object} payResult - 支付下单结果
 * @param {string} payResult.orderString - 支付宝支付参数
 * @param {string} payResult.orderInfo - 抖音支付参数
 * @param {string} channel - 支付渠道 'alipay' | 'douyin'
 * @returns {Promise<boolean>}
 */
export const doAppPayment = async (payResult, channel = 'alipay') => {
  if (channel === 'douyin') {
    return await doDouyinPayment(payResult)
  }
  return await doAlipayPayment(payResult)
}

/**
 * 支付宝APP支付
 */
const doAlipayPayment = async (payResult) => {
  // #ifdef APP-PLUS
  // 检测支付宝支付能力
  const alipayReady = await ensureAlipayAvailable()
  if (!alipayReady) {
    throw new Error('当前设备未检测到支付宝支付能力')
  }

  // 设置沙箱环境
  if (!ensureAlipaySandboxEnv()) {
    throw new Error('支付宝沙箱环境初始化失败')
  }

  return new Promise((resolve, reject) => {
    uni.requestPayment({
      provider: 'alipay',
      orderInfo: withSandboxOrderInfo(payResult.orderString),
      success: () => resolve(true),
      fail: (err) => reject(err)
    })
  })
  // #endif

  // #ifndef APP-PLUS
  throw new Error('支付宝支付仅支持APP端')
  // #endif
}

/**
 * 抖音APP支付
 * 注意：抖音APP支付需要集成原生插件
 */
const doDouyinPayment = async (payResult) => {
  // #ifdef APP-PLUS
  // 尝试获取抖音支付原生插件
  const douyinPay = uni.requireNativePlugin('DouyinPay')

  if (!douyinPay) {
    // 如果没有原生插件，尝试使用uni.requestPayment
    return new Promise((resolve, reject) => {
      uni.requestPayment({
        provider: 'toutiao',
        orderInfo: payResult.orderInfo,
        success: () => resolve(true),
        fail: (err) => {
          console.error('抖音支付失败:', err)
          reject(new Error(err.errMsg || '抖音支付失败'))
        }
      })
    })
  }

  // 使用原生插件
  return new Promise((resolve, reject) => {
    douyinPay.pay({
      orderInfo: payResult.orderInfo
    }, (res) => {
      if (res.code === 0 || res.code === '0') {
        resolve(true)
      } else {
        reject(new Error(res.message || '抖音支付失败'))
      }
    })
  })
  // #endif

  // #ifndef APP-PLUS
  throw new Error('抖音支付仅支持APP端')
  // #endif
}

/**
 * 检测支付宝支付能力
 */
const ensureAlipayAvailable = () => {
  // #ifdef APP-PLUS
  if (typeof uni.getProvider !== 'function') return Promise.resolve(false)
  return new Promise((resolve) => {
    uni.getProvider({
      service: 'payment',
      success: (res) => {
        const providers = Array.isArray(res?.provider) ? res.provider : []
        resolve(providers.includes('alipay'))
      },
      fail: () => resolve(false)
    })
  })
  // #endif
  // #ifndef APP-PLUS
  return Promise.resolve(false)
  // #endif
}

/**
 * 设置支付宝沙箱环境
 */
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

/**
 * 为沙箱订单添加bizcontext
 */
const withSandboxOrderInfo = (orderInfo) => {
  const raw = String(orderInfo || '').trim()
  if (!raw) return raw
  if (/([?&])bizcontext=/.test(raw)) {
    return raw
  }
  const bizContext = encodeURIComponent(JSON.stringify({ appenv: 'system' }))
  return `${raw}&bizcontext=${bizContext}`
}

/**
 * 判断是否支持抖音支付
 */
export const isDouyinPaySupported = () => {
  // #ifdef APP-PLUS
  const douyinPay = uni.requireNativePlugin('DouyinPay')
  if (douyinPay) return true

  // 检查是否支持 toutiao provider
  return new Promise((resolve) => {
    uni.getProvider({
      service: 'payment',
      success: (res) => {
        const providers = Array.isArray(res?.provider) ? res.provider : []
        resolve(providers.includes('toutiao'))
      },
      fail: () => resolve(false)
    })
  })
  // #endif
  // #ifndef APP-PLUS
  return Promise.resolve(false)
  // #endif
}

/**
 * 获取可用的支付渠道列表
 */
export const getAvailablePayChannels = async () => {
  const channels = []

  // #ifdef APP-PLUS
  // 检测支付宝
  const alipayReady = await ensureAlipayAvailable()
  if (alipayReady) {
    channels.push({
      key: 'alipay',
      name: '支付宝',
      icon: '/static/icons/alipay.png'
    })
  }

  // 检测抖音支付
  const douyinSupported = await isDouyinPaySupported()
  if (douyinSupported) {
    channels.push({
      key: 'douyin',
      name: '抖音支付',
      icon: '/static/icons/douyin.png'
    })
  }
  // #endif

  return channels
}

export default {
  doAppPayment,
  isDouyinPaySupported,
  getAvailablePayChannels
}
