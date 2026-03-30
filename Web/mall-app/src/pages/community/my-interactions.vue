<template>
  <view class="page-container">
    <!-- Glass Header -->
    <view class="page-header">
      <text class="header-title">我的互动</text>
      <text class="header-subtitle">查看你的点赞和收藏</text>
    </view>

    <!-- Tabs -->
    <view class="tabs-section">
      <view class="tabs-container">
        <view
          class="tab-item"
          :class="{ active: interactType === 1 }"
          @click="switchType(1)"
        >
          <view class="tab-icon">
            <uni-icons :type="interactType === 1 ? 'heart-filled' : 'heart'" size="20" :color="interactType === 1 ? '#ff4d6d' : '#8a9aaa'"></uni-icons>
          </view>
          <text>我的点赞</text>
          <view class="tab-indicator" v-if="interactType === 1"></view>
        </view>
        <view
          class="tab-item"
          :class="{ active: interactType === 2 }"
          @click="switchType(2)"
        >
          <view class="tab-icon">
            <uni-icons :type="interactType === 2 ? 'star-filled' : 'star'" size="20" :color="interactType === 2 ? '#00bfff' : '#8a9aaa'"></uni-icons>
          </view>
          <text>我的收藏</text>
          <view class="tab-indicator" v-if="interactType === 2"></view>
        </view>
      </view>
    </view>

    <!-- Posts List -->
    <view class="posts-list">
      <view
        class="post-item"
        v-for="(post, index) in records"
        :key="post.id"
        @click="goDetail(post.id)"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <view class="post-content">
          <text class="post-title">{{ post.title }}</text>
          <text class="post-summary">{{ post.summary || '暂无摘要' }}</text>
        </view>

        <view class="post-footer">
          <view class="category-tag">
            <text>{{ post.categoryName || '未分类' }}</text>
          </view>
          <view class="stats-row">
            <view class="stat-item">
              <uni-icons type="heart" size="16" color="#a8a29e"></uni-icons>
              <text>{{ post.likeCount || 0 }}</text>
            </view>
            <view class="stat-item">
              <uni-icons type="star" size="16" color="#a8a29e"></uni-icons>
              <text>{{ post.collectCount || 0 }}</text>
            </view>
          </view>
        </view>

        <view class="interaction-badge" :class="interactType === 1 ? 'like' : 'collect'">
          <uni-icons :type="interactType === 1 ? 'heart-filled' : 'star-filled'" size="14" color="#fff"></uni-icons>
        </view>
      </view>

      <!-- Empty State -->
      <view class="empty-state" v-if="!loading && !records.length">
        <view class="empty-visual">
          <uni-icons :type="interactType === 1 ? 'heart' : 'star'" size="56" color="#e7e5e4"></uni-icons>
        </view>
        <text class="empty-title">{{ interactType === 1 ? '暂无点赞' : '暂无收藏' }}</text>
        <text class="empty-hint">{{ interactType === 1 ? '去发现感兴趣的内容吧' : '收藏喜欢的内容方便查看' }}</text>
      </view>

      <!-- Loading -->
      <view class="loading-state" v-if="loading">
        <view class="loading-spinner"></view>
        <text>加载中...</text>
      </view>

      <!-- Load More -->
      <view class="load-hint" v-if="!loading && records.length">
        <view class="hint-line"></view>
        <text>{{ hasMore ? '上拉加载更多' : '已经到底了' }}</text>
        <view class="hint-line"></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad, onReachBottom } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import { getMyInteractionPageApi } from '../../api/community'

const interactType = ref(1)
const loading = ref(false)
const hasMore = ref(true)
const records = ref([])
const query = reactive({ pageNum: 1, pageSize: 10 })

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getMyInteractionPageApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      interactType: interactType.value
    })
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    const total = Number(res?.total || 0)
    hasMore.value = records.value.length < total
  } catch (error) {
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  query.pageNum = 1
  hasMore.value = true
  await fetchList(false)
}

const switchType = async (type) => {
  if (interactType.value === type) return
  interactType.value = type
  await refresh()
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await refresh()
})

onReachBottom(async () => {
  if (!hasMore.value || loading.value) return
  query.pageNum += 1
  await fetchList(true)
})
</script>

<style scoped lang="scss">
/* Design System Tokens */
$primary: #00bfff;
$deep: #0099cc;
$light: #5ce1ff;
$success: #10b981;
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
  from {
    opacity: 0;
    transform: translateY(24rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.page-container {
  min-height: 100vh;
  background: $bg;
}

/* Glass Header */
.page-header {
  padding: 36rpx 32rpx 28rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $text-primary;
  display: block;
}

.header-subtitle {
  font-size: 24rpx;
  color: $text-muted;
  margin-top: 6rpx;
  display: block;
}

/* Tabs Section */
.tabs-section {
  padding: 20rpx 28rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
}

.tabs-container {
  display: flex;
  background: $bg;
  border-radius: $radius-capsule;
  padding: 8rpx;
  box-shadow: $shadow-card;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  padding: 20rpx;
  border-radius: $radius-capsule;
  position: relative;
  transition: all 0.3s ease;

  .tab-icon {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: all 0.3s ease;
  }

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
    transition: all 0.3s ease;
  }

  &.active {
    background: $card;
    box-shadow: $shadow-card;

    .tab-icon {
      animation: jellyPop 0.4s ease;
    }

    text {
      color: $text-primary;
      font-weight: 600;
    }
  }

  &:active {
    transform: scale(0.96);
  }
}

.tab-indicator {
  position: absolute;
  bottom: 8rpx;
  width: 32rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: $gradient;
}

/* Posts List */
.posts-list {
  padding: 24rpx 28rpx;
}

.post-item {
  position: relative;
  background: $card;
  border-radius: $radius-card;
  padding: 32rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;
  box-shadow: $shadow-card;

  &:active {
    transform: scale(0.98);
  }
}

.post-content {
  margin-bottom: 24rpx;
}

.post-title {
  display: block;
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 12rpx;
}

.post-summary {
  display: -webkit-box;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.6;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.category-tag {
  font-size: 22rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

.stats-row {
  display: flex;
  gap: 20rpx;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6rpx;

  text {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.interaction-badge {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;

  &.like {
    background: linear-gradient(135deg, #ff6b8a 0%, $danger 100%);
    box-shadow: 0 4rpx 16rpx rgba(255, 77, 109, 0.3);
  }

  &.collect {
    background: $gradient;
    box-shadow: 0 4rpx 16rpx rgba(0, 191, 255, 0.3);
  }
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  animation: fadeInUp 0.5s ease;

  .empty-visual {
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

  .empty-title {
    font-size: 30rpx;
    color: $text-primary;
    font-weight: 600;
    margin-bottom: 8rpx;
  }

  .empty-hint {
    font-size: 28rpx;
    color: $text-muted;
    font-weight: 400;
  }
}

/* Loading */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx;
  gap: 16rpx;

  text {
    font-size: 26rpx;
    color: $text-muted;
  }
}

.loading-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid $bg;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Load Hint */
.load-hint {
  display: flex;
  align-items: center;
  padding: 36rpx 40rpx;
  gap: 20rpx;

  text {
    font-size: 24rpx;
    color: $text-muted;
    white-space: nowrap;
  }

  .hint-line {
    flex: 1;
    height: 2rpx;
    background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.06) 50%, transparent 100%);
  }
}
</style>
