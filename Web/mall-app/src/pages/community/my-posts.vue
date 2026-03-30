<template>
  <view class="page-container">
    <!-- Glass Header -->
    <view class="page-header">
      <text class="header-title">我的帖子</text>
      <text class="header-subtitle">管理你发布的内容</text>
    </view>

    <!-- Filter Section -->
    <view class="filter-section">
      <view class="filter-row">
        <picker :range="statusOptions" @change="onStatusChange" :value="statusIndex">
          <view class="status-picker">
            <text>{{ statusOptions[statusIndex] }}</text>
            <uni-icons type="arrowdown" size="14" color="#57534e"></uni-icons>
          </view>
        </picker>
        <view class="search-box">
          <uni-icons type="search" size="18" color="#a8a29e"></uni-icons>
          <input
            v-model="query.keyword"
            placeholder="搜索我的帖子..."
            placeholder-class="search-placeholder"
            class="search-input"
            @confirm="refresh"
          />
        </view>
      </view>
    </view>

    <!-- Posts List -->
    <view class="posts-list">
      <view
        class="post-item"
        v-for="(post, index) in records"
        :key="post.id"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <view class="post-header">
          <text class="post-title" @click="goDetail(post.id)">{{ post.title }}</text>
          <view class="status-badge" :class="statusClass(post.status)">
            <text>{{ statusText(post.status) }}</text>
          </view>
        </view>

        <text class="post-summary">{{ post.summary || '暂无摘要' }}</text>

        <view class="post-meta">
          <view class="meta-item">
            <uni-icons type="calendar" size="16" color="#a8a29e"></uni-icons>
            <text>{{ post.createTime || '' }}</text>
          </view>
          <view class="meta-item">
            <uni-icons type="chat" size="16" color="#a8a29e"></uni-icons>
            <text>{{ post.replyCount || 0 }} 回复</text>
          </view>
        </view>

        <view class="post-actions">
          <view class="action-btn edit" @click="goEdit(post.id)">
            <uni-icons type="compose" size="18" color="#0099cc"></uni-icons>
            <text>编辑</text>
          </view>
          <view class="action-btn delete" @click="removePost(post)">
            <uni-icons type="trash" size="18" color="#ff4d6d"></uni-icons>
            <text>删除</text>
          </view>
        </view>
      </view>

      <!-- Empty State -->
      <view class="empty-state" v-if="!loading && !records.length">
        <view class="empty-visual">
          <uni-icons type="compose" size="56" color="#e7e5e4"></uni-icons>
        </view>
        <text class="empty-title">暂无帖子</text>
        <text class="empty-hint">发布你的第一个帖子吧</text>
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
import { deletePostApi, getMyPostPageApi } from '../../api/community'

const loading = ref(false)
const hasMore = ref(true)
const records = ref([])
const statusOptions = ['全部状态', '草稿', '已发布', '已下架']
const statusIndex = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  keyword: ''
})

const statusText = (status) => {
  if (status === 0) return '草稿'
  if (status === 1) return '已发布'
  if (status === 2) return '已下架'
  return '未知'
}

const statusClass = (status) => {
  if (status === 1) return 'published'
  if (status === 2) return 'offline'
  return 'draft'
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const payload = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.status !== null) payload.status = query.status
    if (query.keyword && query.keyword.trim()) payload.keyword = query.keyword.trim()

    const res = await getMyPostPageApi(payload)
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

const onStatusChange = async (e) => {
  const idx = Number(e?.detail?.value || 0)
  statusIndex.value = idx
  query.status = idx === 0 ? null : idx - 1
  await refresh()
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

const goEdit = (id) => {
  uni.navigateTo({ url: `/pages/community/post-edit?id=${id}` })
}

const removePost = async (post) => {
  const confirm = await new Promise((resolve) => {
    uni.showModal({
      title: '确认删除',
      content: `删除后无法恢复，确定要删除【${post.title}】吗？`,
      success: (res) => resolve(!!res.confirm)
    })
  })
  if (!confirm) return

  try {
    await deletePostApi(post.id)
    uni.showToast({ title: '删除成功', icon: 'success' })
    await refresh()
  } catch (error) {
    uni.showToast({ title: error.message || '删除失败', icon: 'none' })
  }
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

/* Filter Section */
.filter-section {
  padding: 20rpx 32rpx;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.status-picker {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: $bg;
  border-radius: $radius-capsule;
  padding: 16rpx 24rpx;
  min-width: 160rpx;
  box-shadow: $shadow-card;

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
  }
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: $bg;
  border-radius: $radius-capsule;
  padding: 0 24rpx;
  height: 72rpx;
  box-shadow: $shadow-card;
}

.search-input {
  flex: 1;
  margin-left: 12rpx;
  font-size: 26rpx;
  color: $text-primary;
}

.search-placeholder {
  color: $text-muted;
}

/* Posts List */
.posts-list {
  padding: 24rpx 28rpx;
}

.post-item {
  background: $card;
  border-radius: $radius-card;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: $shadow-card;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;

  &:active {
    transform: scale(0.99);
  }
}

.post-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.post-title {
  flex: 1;
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 600;
  line-height: 1.4;
  margin-right: 16rpx;
}

.status-badge {
  font-size: 20rpx;
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
  white-space: nowrap;

  &.draft {
    background: $bg;
    color: $text-muted;
  }

  &.published {
    background: rgba(16, 185, 129, 0.1);
    color: $success;
  }

  &.offline {
    background: rgba(255, 77, 109, 0.08);
    color: $danger;
  }
}

.post-summary {
  display: -webkit-box;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.6;
  margin-bottom: 16rpx;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  gap: 28rpx;
  margin-bottom: 24rpx;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8rpx;

  text {
    font-size: 24rpx;
    color: $text-muted;
  }
}

.post-actions {
  display: flex;
  gap: 16rpx;
  padding-top: 24rpx;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 28rpx;
  border-radius: $radius-capsule;
  font-size: 26rpx;
  font-weight: 500;
  transition: all 0.2s ease;

  &.edit {
    background: rgba(0, 191, 255, 0.08);
    color: $deep;
  }

  &.delete {
    background: rgba(255, 77, 109, 0.06);
    color: $danger;
  }

  &:active {
    transform: scale(0.96);
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
