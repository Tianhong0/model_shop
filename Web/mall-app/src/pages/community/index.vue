<template>
  <view class="community-container">
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
            placeholder="探索帖子、话题..."
            placeholder-class="search-placeholder"
            @confirm="refreshList"
          />
        </view>
        <view class="manage-btn" @click="goMyPosts">
          <text>我的帖子</text>
        </view>
      </view>

      <view class="top-actions">
        <view class="order-tabs">
          <view class="tab" :class="{ active: query.orderBy === 'latest' }" @click="changeOrder('latest')">
            <text>最新</text>
            <view class="tab-indicator" v-if="query.orderBy === 'latest'"></view>
          </view>
          <view class="tab" :class="{ active: query.orderBy === 'hot' }" @click="changeOrder('hot')">
            <text>热门</text>
            <view class="tab-indicator" v-if="query.orderBy === 'hot'"></view>
          </view>
        </view>
        <view class="my-interaction" @click="goMyInteractions">
          <view class="interaction-icon">
            <uni-icons type="heart" size="16" color="#0099cc"></uni-icons>
          </view>
          <text>我的互动</text>
        </view>
      </view>

      <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
        <view
          v-for="item in categoryTabs"
          :key="item.id"
          class="category-item"
          :class="{ active: query.categoryId === item.id }"
          @click="switchCategory(item.id)"
        >
          <text>{{ item.name }}</text>
          <view class="category-dot" v-if="query.categoryId === item.id"></view>
        </view>
      </scroll-view>
    </view>

    <!-- Posts List -->
    <scroll-view
      scroll-y
      class="post-scroll"
      enhanced
      :show-scrollbar="false"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onPullDown"
      @scrolltolower="loadMore"
    >
      <view class="posts-wrapper">
        <view
          class="post-card"
          v-for="(post, index) in records"
          :key="post.id"
          @click="goDetail(post.id)"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <view class="post-head">
            <view class="avatar-ring">
              <image :src="post.userAvatar || defaultAvatar" class="avatar" mode="aspectFill"></image>
            </view>
            <view class="meta">
              <text class="nickname">{{ post.userNickname || '创作者' }}</text>
              <text class="time">{{ post.createTime || '' }}</text>
            </view>
            <view class="category-label">
              <text>{{ post.categoryName || '未分类' }}</text>
            </view>
          </view>

          <view class="post-title">{{ post.title }}</view>
          <view class="post-summary">{{ post.summary || '' }}</view>

          <view class="media-preview" v-if="post.mediaList && post.mediaList.length" @click.stop="previewPostMedia(post)">
            <image v-if="post.mediaList[0].mediaType === 1" :src="post.mediaList[0].mediaUrl" mode="aspectFill"></image>
            <view v-else class="video-thumb-box">
              <image
                v-if="!coverErrors[post.mediaList[0].mediaUrl]"
                :src="getVideoCoverUrl(post.mediaList[0].mediaUrl)"
                mode="aspectFill"
                class="video-thumb-img"
                @error="coverErrors[post.mediaList[0].mediaUrl] = true"
              ></image>
              <view v-else class="video-box">
                <view class="video-overlay">
                  <uni-icons type="videocam" size="34" color="#ffffff"></uni-icons>
                </view>
              </view>
              <view class="video-play-icon">
                <uni-icons type="play-filled" size="40" color="#ffffff"></uni-icons>
              </view>
            </view>
            <view class="media-count" v-if="post.mediaList.length > 1">
              <uni-icons type="image" size="14" color="#fff"></uni-icons>
              <text>+{{ post.mediaList.length - 1 }}</text>
            </view>
          </view>

          <view class="post-footer">
            <view class="action-item" :class="{ liked: post.liked }" @click.stop="toggleInteraction(post, 1)">
              <view class="action-icon">
                <uni-icons :type="post.liked ? 'heart-filled' : 'heart'" size="20" :color="post.liked ? '#ff4d6d' : '#8a9aaa'"></uni-icons>
              </view>
              <text>{{ post.likeCount || 0 }}</text>
            </view>
            <view class="action-item" :class="{ collected: post.collected }" @click.stop="toggleInteraction(post, 2)">
              <view class="action-icon">
                <uni-icons :type="post.collected ? 'star-filled' : 'star'" size="20" :color="post.collected ? '#00bfff' : '#8a9aaa'"></uni-icons>
              </view>
              <text>{{ post.collectCount || 0 }}</text>
            </view>
            <view class="action-item" @click.stop="goDetail(post.id)">
              <view class="action-icon">
                <uni-icons type="chat" size="20" color="#8a9aaa"></uni-icons>
              </view>
              <text>{{ post.replyCount || 0 }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!loading && !records.length">
        <view class="empty-icon">
          <uni-icons type="info" size="48" color="#8a9aaa"></uni-icons>
        </view>
        <text class="empty-text">暂无帖子</text>
        <text class="empty-hint">成为第一个发帖的人吧</text>
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

    <!-- Floating Publish Button -->
    <view class="publish-btn" @click="goPublish" hover-class="publish-btn-hover">
      <view class="btn-inner">
        <uni-icons type="plusempty" size="28" color="#fff"></uni-icons>
      </view>
      <view class="btn-ripple"></view>
    </view>

    <!-- #ifdef APP-PLUS -->
    <AppTabbar />
    <!-- #endif -->
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ensureLoginOrRedirect } from '../../utils/auth'
import {
  getPostCategoryListApi,
  getPostPageApi,
  togglePostInteractionApi
} from '../../api/community'
// #ifdef APP-PLUS
import AppTabbar from '../../components/AppTabbar.vue'
// #endif

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=community'
const coverErrors = reactive({})

const getVideoCoverUrl = (videoUrl) => {
  return videoUrl ? videoUrl.replace(/\.\w+$/, '-cover.jpg') : ''
}

const categoryOptions = ref([])
const records = ref([])
const loading = ref(false)
const refreshing = ref(false)
const hasMore = ref(true)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: 0,
  orderBy: 'latest'
})

const categoryTabs = computed(() => [{ id: 0, name: '全部' }, ...categoryOptions.value])

const loadCategories = async () => {
  const list = await getPostCategoryListApi()
  categoryOptions.value = Array.isArray(list) ? list : []
}

const buildPayload = () => {
  const payload = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    orderBy: query.orderBy
  }
  if (query.keyword && query.keyword.trim()) payload.keyword = query.keyword.trim()
  if (query.categoryId && query.categoryId > 0) payload.categoryId = query.categoryId
  return payload
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getPostPageApi(buildPayload())
    const list = Array.isArray(res?.records) ? res.records : []
    records.value = append ? [...records.value, ...list] : list
    const total = Number(res?.total || 0)
    hasMore.value = records.value.length < total
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

const switchCategory = async (categoryId) => {
  query.categoryId = categoryId
  await refreshList()
}

const changeOrder = async (orderBy) => {
  if (query.orderBy === orderBy) return
  query.orderBy = orderBy
  await refreshList()
}

const toggleInteraction = async (post, interactType) => {
  try {
    const res = await togglePostInteractionApi({ postId: post.id, interactType })
    const active = !!res?.active
    if (interactType === 1) {
      post.liked = active
      post.likeCount = Math.max(0, Number(post.likeCount || 0) + (active ? 1 : -1))
    } else {
      post.collected = active
      post.collectCount = Math.max(0, Number(post.collectCount || 0) + (active ? 1 : -1))
    }
  } catch (error) {
    uni.showToast({ title: error.message || '操作失败', icon: 'none' })
  }
}

const previewPostMedia = (post) => {
  const mediaList = Array.isArray(post?.mediaList) ? post.mediaList : []
  const imageUrls = mediaList.filter(item => item.mediaType === 1).map(item => item.mediaUrl)
  if (imageUrls.length) {
    uni.previewImage({ urls: imageUrls, current: imageUrls[0] })
    return
  }
  if (mediaList.length && mediaList[0].mediaType === 2) {
    uni.navigateTo({ url: `/pages/community/post-detail?id=${post.id}` })
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/community/post-detail?id=${id}` })
}

const goPublish = () => {
  uni.navigateTo({ url: '/pages/community/post-edit' })
}

const goMyInteractions = () => {
  uni.navigateTo({ url: '/pages/community/my-interactions' })
}

const goMyPosts = () => {
  uni.navigateTo({ url: '/pages/community/my-posts' })
}

onLoad(async () => {
  if (!ensureLoginOrRedirect()) return
  await loadCategories()
  await refreshList()
})

onShow(async () => {
  if (!ensureLoginOrRedirect()) return
  await refreshList()
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

/* Animations */
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

@keyframes ripple {
  0% {
    transform: scale(1);
    opacity: 0.4;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

@keyframes publishBtnIn {
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes imageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.community-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg;
  overflow: hidden;
}

/* Glass Header */
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
    font-weight: 400;
  }
}

.search-placeholder {
  color: $text-muted;
  font-weight: 400;
}

.manage-btn {
  font-size: 26rpx;
  color: $primary;
  font-weight: 600;
  padding: 16rpx 28rpx;
  background: rgba(0, 191, 255, 0.08);
  border-radius: $radius-capsule;
  transition: all 0.2s ease;

  &:active {
    transform: scale(0.96);
    background: rgba(0, 191, 255, 0.15);
  }
}

.top-actions {
  margin-top: 20rpx;
  padding: 0 32rpx;
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
  position: relative;
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

  &:active {
    transform: scale(0.96);
  }
}

.tab-indicator {
  display: none;
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
  transition: all 0.2s ease;

  .interaction-icon {
    display: flex;
    align-items: center;
  }

  &:active {
    background: rgba(0, 191, 255, 0.15);
    transform: scale(0.96);
  }
}

.category-scroll {
  white-space: nowrap;
  padding: 20rpx 32rpx 24rpx;
}

.category-item {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-right: 16rpx;
  font-size: 26rpx;
  color: $text-secondary;
  padding: 12rpx 28rpx;
  background: $card;
  border-radius: $radius-capsule;
  box-shadow: $shadow-card;
  transition: all 0.25s ease;

  &.active {
    color: #ffffff;
    font-weight: 600;
    background: $gradient;
  }

  &:active {
    transform: scale(0.96);
  }
}

.category-dot {
  width: 8rpx;
  height: 8rpx;
  background: #ffffff;
  border-radius: 50%;
}

/* Posts List */
.post-scroll {
  flex: 1;
  min-height: 0;
}

.posts-wrapper {
  padding: 24rpx 28rpx;
}

.post-card {
  position: relative;
  background: $card;
  margin-bottom: 28rpx;
  padding: 32rpx;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
  overflow: hidden;
  animation: fadeInUp 0.4s ease forwards;
  opacity: 0;

  &:active {
    transform: scale(0.99);
  }
}

.post-head {
  display: flex;
  align-items: center;
}

.avatar-ring {
  width: 80rpx;
  height: 80rpx;
  padding: 4rpx;
  background: $gradient;
  border-radius: 50%;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  animation: imageFadeIn 0.4s ease;
}

.meta {
  flex: 1;
  margin-left: 20rpx;
}

.nickname {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 600;
  display: block;
}

.time {
  font-size: 24rpx;
  color: $text-muted;
  display: block;
  margin-top: 4rpx;
}

.category-label {
  font-size: 22rpx;
  color: $deep;
  background: rgba(0, 191, 255, 0.08);
  padding: 8rpx 18rpx;
  border-radius: $radius-capsule;
  font-weight: 500;
}

.post-title {
  margin-top: 24rpx;
  font-size: 36rpx;
  color: $text-primary;
  font-weight: 700;
  line-height: 1.4;
}

.post-summary {
  margin-top: 12rpx;
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.media-preview {
  margin-top: 24rpx;
  position: relative;
  border-radius: 20rpx;
  overflow: hidden;

  image {
    width: 100%;
    height: 360rpx;
    border-radius: 20rpx;
    animation: imageFadeIn 0.5s ease;
  }
}

.video-thumb-box {
  position: relative;
  width: 100%;
  height: 360rpx;
  border-radius: 20rpx;
  overflow: hidden;
}

.video-thumb-img {
  width: 100%;
  height: 360rpx;
  border-radius: 20rpx;
  animation: imageFadeIn 0.5s ease;
}

.video-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 50%;
  backdrop-filter: blur(4px);
}

.video-box {
  width: 100%;
  height: 360rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #e0f0f8 0%, #d0e8f2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;

  text {
    color: $text-secondary;
    font-size: 26rpx;
    font-weight: 500;
  }
}

.media-count {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  display: flex;
  align-items: center;
  gap: 6rpx;
  color: #ffffff;
  background: rgba(0, 153, 204, 0.8);
  backdrop-filter: blur(8px);
  border-radius: $radius-capsule;
  padding: 8rpx 18rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.post-footer {
  margin-top: 28rpx;
  display: flex;
  gap: 32rpx;
  padding-top: 24rpx;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 8rpx 16rpx;
  border-radius: $radius-capsule;
  transition: all 0.2s ease;

  text {
    font-size: 26rpx;
    color: $text-secondary;
    font-weight: 500;
  }

  &.liked text {
    color: $danger;
  }

  &.collected text {
    color: $primary;
  }

  .action-icon {
    display: flex;
    align-items: center;
    transition: transform 0.2s ease;
  }

  &:active .action-icon {
    animation: jellyPop 0.4s ease;
  }
}

/* Empty & Loading States */
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
    font-weight: 400;
  }
}

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

.load-more-hint {
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

/* Publish Button */
.publish-btn {
  position: fixed;
  right: 40rpx;
  bottom: calc(140rpx + env(safe-area-inset-bottom));
  width: 108rpx;
  height: 108rpx;
  border-radius: $radius-card;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 100;
  animation: publishBtnIn 0.5s ease forwards;
  opacity: 0;
  transform: scale(0.5) translateY(40rpx);

  .btn-inner {
    width: 100%;
    height: 100%;
    background: $gradient;
    border-radius: $radius-card;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow:
      0 8rpx 28rpx rgba(0, 191, 255, 0.35),
      0 4rpx 12rpx rgba(0, 191, 255, 0.25);
  }

  .btn-ripple {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: $radius-card;
    border: 4rpx solid $primary;
    opacity: 0;
    animation: ripple 2s infinite;
  }
}

.publish-btn-hover {
  transform: scale(0.92);
}
</style>
