<template>
  <view class="page">
    <view class="hero-card">
      <view>
        <text class="hero-kicker">MESSAGE CENTER</text>
        <text class="hero-title">消息中心</text>
        <text class="hero-sub">点赞、物流、二手沟通与议价提醒都会汇总到这里。</text>
      </view>
      <view class="hero-badge">
        <text class="hero-badge-value">{{ summary.totalUnread || 0 }}</text>
        <text class="hero-badge-label">未读消息</text>
      </view>
    </view>

    <view class="toolbar-card">
      <scroll-view scroll-x class="tab-scroll" show-scrollbar="false">
        <view class="tab-row">
          <view
            v-for="item in tabs"
            :key="item.value"
            class="tab-item"
            :class="{ active: currentTab === item.value }"
            @click="switchTab(item.value)"
          >
            <text class="tab-label">{{ item.label }}</text>
            <text v-if="item.badge > 0" class="tab-badge">{{ item.badge > 99 ? '99+' : item.badge }}</text>
          </view>
        </view>
      </scroll-view>

      <view class="action-row">
        <view class="toggle-chip" :class="{ active: unreadOnly }" @click="toggleUnreadOnly">仅看未读</view>
        <view class="mark-btn" @click="markAllRead">全部已读</view>
      </view>
    </view>

    <scroll-view scroll-y class="list-scroll" @scrolltolower="loadMore">
      <view v-if="records.length === 0" class="empty-card">
        <view class="empty-dot"></view>
        <text class="empty-title">暂时没有消息</text>
        <text class="empty-sub">新的点赞、物流或交易提醒出现后，会第一时间展示在这里。</text>
      </view>

      <view
        v-for="item in records"
        :key="item.id"
        class="notice-card"
        :class="{ unread: Number(item.isRead) === 0 }"
        @click="handleOpen(item)"
      >
        <image v-if="item.coverUrl" :src="item.coverUrl" class="notice-cover" mode="aspectFill" />
        <view v-else class="notice-cover placeholder">{{ categoryIcon(item.category) }}</view>
        <view class="notice-main">
          <view class="notice-top">
            <view class="notice-title-row">
              <text class="notice-title">{{ item.title || '消息提醒' }}</text>
              <view v-if="Number(item.isRead) === 0" class="notice-dot"></view>
            </view>
            <text class="notice-time">{{ formatTime(item.createTime) }}</text>
          </view>
          <view class="notice-content">{{ item.content || '点击查看详情' }}</view>
          <view class="notice-bottom">
            <text class="notice-tag">{{ categoryText(item.category) }}</text>
            <text v-if="item.senderName" class="notice-sender">来自 {{ item.senderName }}</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="bottom-tip">加载中...</view>
      <view v-else-if="finished && records.length" class="bottom-tip">没有更多消息了</view>
      <view class="safe-space"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getNotificationPageApi,
  getNotificationUnreadSummaryApi,
  readAllNotificationsApi,
  readNotificationApi
} from '../../api/notification'
import { emitNotificationSummary, openNotificationTarget } from '../../utils/notificationRuntime'

const currentTab = ref('ALL')
const unreadOnly = ref(false)
const loading = ref(false)
const finished = ref(false)
const records = ref([])
const summary = reactive({
  totalUnread: 0,
  tradeUnread: 0,
  likeUnread: 0,
  logisticsUnread: 0
})
const pager = reactive({
  pageNum: 1,
  pageSize: 20
})

const tabs = computed(() => ([
  { label: '全部', value: 'ALL', badge: Number(summary.totalUnread || 0) },
  { label: '交易', value: 'TRADE', badge: Number(summary.tradeUnread || 0) },
  { label: '点赞', value: 'LIKE', badge: Number(summary.likeUnread || 0) },
  { label: '物流', value: 'LOGISTICS', badge: Number(summary.logisticsUnread || 0) }
]))

const formatTime = (value) => {
  if (!value) return '刚刚'
  const text = String(value).replace('T', ' ')
  return text.length > 16 ? text.slice(0, 16) : text
}

const categoryText = (category) => ({
  TRADE: '交易消息',
  LIKE: '点赞提醒',
  LOGISTICS: '物流提醒'
}[String(category || '').toUpperCase()] || '系统消息')

const categoryIcon = (category) => ({
  TRADE: '聊',
  LIKE: '赞',
  LOGISTICS: '运'
}[String(category || '').toUpperCase()] || '信')

const loadSummary = async () => {
  try {
    const result = await getNotificationUnreadSummaryApi()
    Object.assign(summary, {
      totalUnread: Number(result?.totalUnread || 0),
      tradeUnread: Number(result?.tradeUnread || 0),
      likeUnread: Number(result?.likeUnread || 0),
      logisticsUnread: Number(result?.logisticsUnread || 0)
    })
    emitNotificationSummary(summary)
  } catch (_) {
    Object.assign(summary, {
      totalUnread: 0,
      tradeUnread: 0,
      likeUnread: 0,
      logisticsUnread: 0
    })
    emitNotificationSummary(summary)
  }
}

const loadList = async (reset = true) => {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) {
      pager.pageNum = 1
      finished.value = false
    }
    const result = await getNotificationPageApi({
      pageNum: pager.pageNum,
      pageSize: pager.pageSize,
      category: currentTab.value,
      unreadOnly: unreadOnly.value
    })
    const list = Array.isArray(result?.records) ? result.records : []
    records.value = reset ? list : [...records.value, ...list]
    const pages = Number(result?.pages || 1)
    finished.value = pager.pageNum >= pages
  } catch (error) {
    uni.showToast({ title: error.message || '加载消息失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const refreshPage = async () => {
  await Promise.all([loadSummary(), loadList(true)])
}

const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  loadList(true)
}

const toggleUnreadOnly = () => {
  unreadOnly.value = !unreadOnly.value
  loadList(true)
}

const markAllRead = async () => {
  try {
    await readAllNotificationsApi(currentTab.value === 'ALL' ? '' : currentTab.value)
    uni.showToast({ title: '已全部标记已读', icon: 'success' })
    await refreshPage()
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const handleOpen = async (item) => {
  try {
    if (Number(item?.isRead) === 0) {
      await readNotificationApi(item.id)
    }
  } catch (_) {
    // ignore read failure and continue navigation
  }
  await loadSummary()
  records.value = records.value.map(record => (
    String(record.id) === String(item.id)
      ? { ...record, isRead: 1 }
      : record
  ))
  openNotificationTarget(item)
}

const loadMore = () => {
  if (loading.value || finished.value) return
  pager.pageNum += 1
  loadList(false)
}

onShow(() => {
  refreshPage()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 24rpx;
  background:
    radial-gradient(circle at top right, rgba(224, 231, 255, 0.72), transparent 26%),
    radial-gradient(circle at left top, rgba(253, 230, 138, 0.35), transparent 20%),
    linear-gradient(180deg, #f8fafc 0%, #fefefe 34%, #f1f5f9 100%);
}

.hero-card,
.toolbar-card,
.notice-card,
.empty-card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 28rpx;
  border: 1px solid rgba(148, 163, 184, 0.12);
  box-shadow: 0 16rpx 36rpx rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  padding: 28rpx;
}

.hero-kicker {
  display: block;
  color: #6366f1;
  font-size: 20rpx;
  letter-spacing: 4rpx;
}

.hero-title {
  display: block;
  margin-top: 12rpx;
  font-size: 42rpx;
  font-weight: 700;
  color: #0f172a;
}

.hero-sub {
  display: block;
  margin-top: 12rpx;
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.7;
}

.hero-badge {
  width: 180rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #312e81 0%, #4f46e5 100%);
}

.hero-badge-value {
  display: block;
  color: #fff;
  font-size: 42rpx;
  font-weight: 700;
}

.hero-badge-label {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 22rpx;
}

.toolbar-card {
  margin-top: 20rpx;
  padding: 20rpx;
}

.tab-scroll {
  white-space: nowrap;
}

.tab-row {
  display: inline-flex;
  gap: 14rpx;
  padding-right: 10rpx;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 16rpx 24rpx;
  border-radius: 999rpx;
  background: #f8fafc;
  color: #64748b;
}

.tab-item.active {
  background: linear-gradient(135deg, #111827 0%, #374151 100%);
  color: #fff;
}

.tab-label {
  font-size: 24rpx;
}

.tab-badge {
  min-width: 34rpx;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(239, 68, 68, 0.16);
  color: #ef4444;
  font-size: 20rpx;
  text-align: center;
}

.tab-item.active .tab-badge {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
}

.toggle-chip,
.mark-btn {
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.toggle-chip {
  background: #f8fafc;
  color: #64748b;
}

.toggle-chip.active {
  background: #eef2ff;
  color: #4338ca;
}

.mark-btn {
  background: #111827;
  color: #fff;
}

.list-scroll {
  height: calc(100vh - 310rpx);
  margin-top: 20rpx;
}

.notice-card {
  display: flex;
  gap: 18rpx;
  padding: 20rpx;
  margin-bottom: 18rpx;
}

.notice-card.unread {
  border-color: rgba(79, 70, 229, 0.2);
  box-shadow: 0 18rpx 38rpx rgba(79, 70, 229, 0.08);
}

.notice-cover {
  width: 120rpx;
  height: 120rpx;
  border-radius: 22rpx;
  background: #e2e8f0;
  flex-shrink: 0;
}

.notice-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4338ca;
  font-size: 34rpx;
  font-weight: 700;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-top {
  display: flex;
  justify-content: space-between;
  gap: 14rpx;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.notice-title {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.4;
}

.notice-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
}

.notice-time,
.notice-content,
.notice-sender,
.notice-tag,
.empty-sub,
.bottom-tip {
  color: #64748b;
  font-size: 22rpx;
}

.notice-time {
  flex-shrink: 0;
}

.notice-content {
  margin-top: 12rpx;
  line-height: 1.7;
}

.notice-bottom {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 16rpx;
}

.notice-tag {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #eef2ff;
  color: #4338ca;
}

.empty-card {
  padding: 120rpx 40rpx;
  text-align: center;
}

.empty-dot {
  width: 108rpx;
  height: 108rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #c7d2fe 0%, #818cf8 55%, #4f46e5 100%);
  box-shadow: 0 16rpx 32rpx rgba(79, 70, 229, 0.24);
}

.empty-title {
  display: block;
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  line-height: 1.7;
}

.bottom-tip {
  text-align: center;
  padding: 20rpx 0;
}

.safe-space {
  height: 30rpx;
}
</style>
