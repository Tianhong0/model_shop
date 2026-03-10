import {
  ackPopupNotificationsApi,
  getNotificationUnreadSummaryApi,
  getPendingPopupNotificationsApi
} from '../api/notification'
import { hasValidLogin } from './auth'

export const NOTIFICATION_SUMMARY_EVENT = 'notification-summary-change'
export const NOTIFICATION_REFRESH_EVENT = 'notification-refresh'

const EMPTY_SUMMARY = {
  totalUnread: 0,
  tradeUnread: 0,
  likeUnread: 0,
  logisticsUnread: 0
}

let pollTimer = null
let polling = false
let popupShowing = false
let popupQueue = []
let started = false

const safeNumber = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num : 0
}

const normalizeSummary = (summary) => ({
  totalUnread: safeNumber(summary?.totalUnread),
  tradeUnread: safeNumber(summary?.tradeUnread),
  likeUnread: safeNumber(summary?.likeUnread),
  logisticsUnread: safeNumber(summary?.logisticsUnread)
})

const buildPopupContent = (item) => {
  const title = String(item?.title || '').trim()
  const content = String(item?.content || '').trim()
  if (title && content) return `${title}\n${content}`
  return title || content || '你有一条新的重要消息'
}

const resolveNavigationMethod = (url) => {
  if (!url) return null
  if (url.startsWith('/pages/index/index') || url.startsWith('/pages/community/index') || url.startsWith('/pages/cart/cart') || url.startsWith('/pages/user/user')) {
    return 'switchTab'
  }
  return 'navigateTo'
}

export const openNotificationTarget = (item) => {
  const url = String(item?.redirectUrl || '').trim()
  if (!url) return
  const method = resolveNavigationMethod(url)
  if (!method || typeof uni?.[method] !== 'function') return
  uni[method]({ url })
}

export const emitNotificationSummary = (summary = EMPTY_SUMMARY) => {
  const normalized = normalizeSummary(summary)
  uni.$emit(NOTIFICATION_SUMMARY_EVENT, normalized)
  return normalized
}

export const refreshNotificationSummary = async () => {
  if (!hasValidLogin()) {
    return emitNotificationSummary(EMPTY_SUMMARY)
  }
  try {
    const summary = await getNotificationUnreadSummaryApi()
    return emitNotificationSummary(summary)
  } catch (_) {
    return emitNotificationSummary(EMPTY_SUMMARY)
  }
}

const ackPopupItem = async (item) => {
  const id = item?.id
  if (!id) return
  try {
    await ackPopupNotificationsApi([id])
  } catch (_) {
    // ignore ack failure, next poll will retry if needed
  }
}

const showNextPopup = async () => {
  if (popupShowing || !popupQueue.length) return
  const item = popupQueue.shift()
  if (!item?.id) {
    showNextPopup()
    return
  }
  popupShowing = true
  uni.showModal({
    title: '消息提醒',
    content: buildPopupContent(item),
    confirmText: '查看',
    cancelText: '知道了',
    success: async (res) => {
      await ackPopupItem(item)
      await refreshNotificationSummary()
      popupShowing = false
      if (res?.confirm) {
        openNotificationTarget(item)
      }
      showNextPopup()
    },
    fail: async () => {
      await ackPopupItem(item)
      await refreshNotificationSummary()
      popupShowing = false
      showNextPopup()
    }
  })
}

const mergePopupQueue = (items = []) => {
  const existed = new Set(popupQueue.map(item => String(item?.id || '')))
  items.forEach((item) => {
    const key = String(item?.id || '')
    if (!key || existed.has(key)) return
    popupQueue.push(item)
    existed.add(key)
  })
}

const pollNotifications = async () => {
  if (polling || !hasValidLogin()) {
    if (!hasValidLogin()) {
      emitNotificationSummary(EMPTY_SUMMARY)
      popupQueue = []
    }
    return
  }
  polling = true
  try {
    const [summary, popups] = await Promise.all([
      getNotificationUnreadSummaryApi(),
      getPendingPopupNotificationsApi(10)
    ])
    emitNotificationSummary(summary)
    mergePopupQueue(Array.isArray(popups) ? popups : [])
    showNextPopup()
  } catch (_) {
    // ignore polling errors
  } finally {
    polling = false
  }
}

export const startNotificationRuntime = () => {
  if (started) {
    pollNotifications()
    return
  }
  started = true
  pollNotifications()
  pollTimer = setInterval(pollNotifications, 15000)
}

export const stopNotificationRuntime = () => {
  started = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  polling = false
  popupShowing = false
  popupQueue = []
  emitNotificationSummary(EMPTY_SUMMARY)
}
