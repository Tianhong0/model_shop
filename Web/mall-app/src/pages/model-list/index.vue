<template>
  <view class="list-plaza">
    <!-- Glass Header -->
    <view class="header-sticky">
      <view class="search-row">
        <view class="search-bar">
          <view class="search-icon">
            <uni-icons type="search" size="18" color="#ffffff"></uni-icons>
          </view>
          <input
            v-model="query.keyword"
            type="text"
            placeholder="搜索清单..."
            placeholder-class="search-placeholder"
            @confirm="refreshList"
          />
        </view>
        <view class="manage-btn" @click="goMyLists">
          <text>我的清单</text>
        </view>
      </view>

      <view class="top-actions">
        <view class="order-tabs">
          <view class="tab" :class="{ active: query.orderBy === 'latest' }" @click="changeOrder('latest')">
            <text>最新</text>
          </view>
          <view class="tab" :class="{ active: query.orderBy === 'hot' }" @click="changeOrder('hot')">
            <text>热门</text>
          </view>
        </view>
        <view class="my-interaction" @click="goMyInteractions">
          <view class="interaction-icon">
            <uni-icons type="heart" size="16" color="#0099cc"></uni-icons>
          </view>
          <text>我的互动</text>
        </view>
      </view>
    </view>

    <!-- List -->
    <scroll-view
      scroll-y
      class="list-scroll"
      enhanced
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onPullDown"
      @scrolltolower="loadMore"
    >
      <view class="list-wrapper">
        <view
          class="list-card"
          v-for="(item, index) in records"
          :key="item.id"
          @click="goDetail(item.id)"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <image
            v-if="item.coverImage"
            :src="item.coverImage"
            class="card-cover"
            mode="aspectFill"
          ></image>
          <view class="card-cover card-cover-empty" v-else>
            <uni-icons type="image" size="36" color="#8a9aaa"></uni-icons>
          </view>

          <view class="card-body">
            <view class="card-head">
              <image :src="item.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
              <view class="meta">
                <text class="nickname">{{ item.userNickname || '用户' }}</text>
                <text class="time">{{ item.createTime || '' }}</text>
              </view>
              <view class="model-count-badge">
                <uni-icons type="compose" size="14" color="#0099cc"></uni-icons>
                <text>{{ item.modelCount || 0 }}件</text>
              </view>
            </view>

            <view class="card-title">{{ item.title }}</view>
            <view class="card-desc" v-if="item.description">{{ item.description }}</view>

            <view class="card-footer">
              <view class="action-item" :class="{ liked: item.liked }" @click.stop="toggleInteraction(item, 1)">
                <view class="action-icon">
                  <uni-icons :type="item.liked ? 'heart-filled' : 'heart'" size="20" :color="item.liked ? '#ff4d6d' : '#8a9aaa'"></uni-icons>
                </view>
                <text>{{ item.likeCount || 0 }}</text>
              </view>
              <view class="action-item" :class="{ collected: item.collected }" @click.stop="toggleInteraction(item, 2)">
                <view class="action-icon">
                  <uni-icons :type="item.collected ? 'star-filled' : 'star'" size="20" :color="item.collected ? '#00bfff' : '#8a9aaa'"></uni-icons>
                </view>
                <text>{{ item.collectCount || 0 }}</text>
              </view>
              <view class="action-item">
                <view class="action-icon">
                  <uni-icons type="eye" size="20" color="#8a9aaa"></uni-icons>
                </view>
                <text>{{ item.viewCount || 0 }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!loading && !records.length">
        <view class="empty-icon">
          <uni-icons type="info" size="48" color="#8a9aaa"></uni-icons>
        </view>
        <text class="empty-text">暂无清单</text>
        <text class="empty-hint">快去创建并分享你的第一个清单吧</text>
      </view>

      <view class="loading-state" v-if="loading">
        <view class="loading-spinner"></view>
        <text>加载中...</text>
      </view>

      <view class="load-more-hint" v-if="!loading && records.length">
        <view class="hint-line"></view>
        <text>{{ hasMore ? '上拉加载更多' : '已经到底了' }}</text>
        <view class="hint-line"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  getModelListPageApi,
  toggleModelListInteractionApi
} from '../../api/modelList'

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=list'

const records = ref([])
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  orderBy: 'latest'
})

const buildPayload = () => {
  const payload = { pageNum: query.pageNum, pageSize: query.pageSize, orderBy: query.orderBy }
  if (query.keyword && query.keyword.trim()) payload.keyword = query.keyword.trim()
  return payload
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getModelListPageApi(buildPayload())
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    hasMore.value = records.value.length < Number(res?.total || 0)
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const refreshList = async () => {
  query.pageNum = 1
  hasMore.value = true
  await fetchList(false)
}

const loadMore = async () => {
  if (!hasMore.value || loading.value) return
  query.pageNum += 1
  await fetchList(true)
}

const onPullDown = async () => {
  refreshing.value = true
  await refreshList()
}

const changeOrder = async (orderBy) => {
  if (query.orderBy === orderBy) return
  query.orderBy = orderBy
  await refreshList()
}

const toggleInteraction = async (item, interactType) => {
  try {
    const res = await toggleModelListInteractionApi({ listId: item.id, interactType })
    const active = !!res?.active
    if (interactType === 1) {
      item.liked = active
      item.likeCount = Math.max(0, Number(item.likeCount || 0) + (active ? 1 : -1))
    } else {
      item.collected = active
      item.collectCount = Math.max(0, Number(item.collectCount || 0) + (active ? 1 : -1))
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/model-list/detail?id=${id}` })
}

const goMyLists = () => {
  uni.navigateTo({ url: '/pages/user/params' })
}

const goMyInteractions = () => {
  uni.navigateTo({ url: '/pages/model-list/detail?tab=interactions' })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
})

onShow(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
})
</script>

<style scoped lang="scss">
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$danger: #ff4d6d;
$bg: #f8f8f8;
$card: #ffffff;
$text-primary: #1a2030;
$text-secondary: #5a6a7a;
$text-muted: #8a9aaa;
$gradient: linear-gradient(135deg, #00bfff 0%, #5ce1ff 100%);
$shadow-card: 0 8rpx 40rpx rgba(0, 0, 0, 0.04);
$radius-card: 24rpx;
$radius-capsule: 999rpx;

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes jellyPop {
  0% { transform: scale(1); }
  30% { transform: scale(1.25); }
  50% { transform: scale(0.9); }
  70% { transform: scale(1.08); }
  100% { transform: scale(1); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.list-plaza {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg;
  overflow: hidden;
}

.header-sticky {
  position: relative;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  padding-top: calc(env(safe-area-inset-top) + 16rpx);
  z-index: 10;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 0 32rpx;
}

.search-bar {
  flex: 1;
  height: 80rpx;
  background: $bg;
  border-radius: $radius-capsule;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  box-shadow: $shadow-card;

  .search-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40rpx;
    height: 40rpx;
    background: $gradient;
    border-radius: 12rpx;
  }

  input {
    flex: 1;
    margin-left: 16rpx;
    font-size: 28rpx;
    color: $text-primary;
  }
}

.search-placeholder {
  color: $text-muted;
}

.manage-btn {
  font-size: 26rpx;
  color: $primary;
  font-weight: 600;
  padding: 16rpx 28rpx;
  background: rgba(0, 191, 255, 0.08);
  border-radius: $radius-capsule;
  &:active { transform: scale(0.96); }
}

.top-actions {
  margin-top: 20rpx;
  padding: 0 32rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-tabs {
  display: flex;
  gap: 8rpx;
  background: $bg;
  padding: 6rpx;
  border-radius: $radius-capsule;
  box-shadow: $shadow-card;
}

.tab {
  font-size: 28rpx;
  color: $text-secondary;
  padding: 12rpx 28rpx;
  font-weight: 500;
  border-radius: $radius-capsule;
  transition: all 0.25s ease;

  &.active {
    color: #ffffff;
    background: $gradient;
    font-weight: 600;
  }
  &:active { transform: scale(0.96); }
}

.my-interaction {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 26rpx;
  color: $deep;
  font-weight: 600;
  padding: 12rpx 24rpx;
  background: rgba(0, 191, 255, 0.08);
  border-radius: $radius-capsule;

  .interaction-icon { display: flex; align-items: center; }
  &:active { background: rgba(0, 191, 255, 0.15); transform: scale(0.96); }
}

.list-scroll {
  flex: 1;
  min-height: 0;
}

.list-wrapper {
  padding: 24rpx 28rpx;
}

.list-card {
  background: $card;
  margin-bottom: 28rpx;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  overflow: hidden;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
  &:active { transform: scale(0.99); }
}

.card-cover {
  width: 100%;
  height: 300rpx;
}

.card-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4fd 0%, #dbeafe 100%);
}

.card-body {
  padding: 28rpx 32rpx;
}

.card-head {
  display: flex;
  align-items: center;

  .avatar {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    border: 3rpx solid rgba(0, 191, 255, 0.3);
  }

  .meta {
    flex: 1;
    margin-left: 16rpx;
  }

  .nickname {
    font-size: 26rpx;
    color: $text-primary;
    font-weight: 600;
    display: block;
  }

  .time {
    font-size: 22rpx;
    color: $text-muted;
    display: block;
    margin-top: 2rpx;
  }
}

.model-count-badge {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

.card-title {
  margin-top: 20rpx;
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 700;
  line-height: 1.4;
}

.card-desc {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-top: 24rpx;
  display: flex;
  gap: 32rpx;
  padding-top: 20rpx;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 16rpx;
  border-radius: $radius-capsule;

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
  }

  &.liked text { color: $danger; }
  &.collected text { color: $primary; }

  .action-icon {
    display: flex;
    align-items: center;
  }

  &:active .action-icon {
    animation: jellyPop 0.4s ease;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  animation: fadeInUp 0.5s ease;

  .empty-icon {
    width: 120rpx;
    height: 120rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $card;
    border-radius: 50%;
    box-shadow: $shadow-card;
    margin-bottom: 28rpx;
  }

  .empty-text {
    font-size: 36rpx;
    color: $text-primary;
    font-weight: 700;
    margin-bottom: 8rpx;
  }

  .empty-hint {
    font-size: 28rpx;
    color: $text-muted;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
  gap: 16rpx;

  text { font-size: 26rpx; color: $text-muted; }
}

.loading-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid $bg;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.load-more-hint {
  display: flex;
  align-items: center;
  padding: 36rpx 40rpx;
  gap: 20rpx;

  text { font-size: 24rpx; color: $text-muted; white-space: nowrap; }
  .hint-line {
    flex: 1;
    height: 2rpx;
    background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.06) 50%, transparent 100%);
  }
}
</style>
